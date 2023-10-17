package org.iata.ilds.agent.spring.integration;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.SftpException;
import lombok.extern.log4j.Log4j2;
import org.iata.ilds.agent.config.activemq.ActivemqConfigProperties;
import org.iata.ilds.agent.config.inbound.HostingSystemProperties;
import org.iata.ilds.agent.domain.builder.DispatchCompletedMessageBuilder;
import org.iata.ilds.agent.domain.entity.TransferPackage;
import org.iata.ilds.agent.domain.entity.TransferSite;
import org.iata.ilds.agent.domain.message.DispatchCompletedMessage;
import org.iata.ilds.agent.domain.message.inbound.InboundDispatchMessage;

import org.iata.ilds.agent.exception.DispatchException;
import org.iata.ilds.agent.service.DispatchCompletedService;
import org.iata.ilds.agent.service.FileService;
import org.iata.ilds.agent.spring.data.TransferPackageRepository;
import org.iata.ilds.agent.spring.data.TransferSiteRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.core.MessageSelector;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.dsl.Transformers;
import org.springframework.integration.file.remote.ClientCallback;
import org.springframework.integration.file.remote.session.DelegatingSessionFactory;
import org.springframework.integration.handler.GenericHandler;
import org.springframework.integration.handler.advice.RequestHandlerRetryAdvice;
import org.springframework.integration.jms.dsl.Jms;
import org.springframework.integration.sftp.session.SftpRemoteFileTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.retry.support.RetryTemplate;

import javax.jms.ConnectionFactory;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Function;

import static org.iata.ilds.agent.config.inbound.HostingSystemProperties.HostingSystem;

@Log4j2
@Configuration
public class InboundDispatchConfig {


    @Bean
    public IntegrationFlow inboundDispatchFlow(ConnectionFactory connectionFactory,
                                                ActivemqConfigProperties activemqConfig,
                                                HostingSystemProperties hostingConfig,
                                                TransferPackageRepository transferPackageRepository,
                                                TransferSiteRepository transferSiteRepository,
                                                DelegatingSessionFactory<ChannelSftp.LsEntry> delegatingSessionFactory,
                                                RetryTemplate retryTemplate,
                                                RequestHandlerRetryAdvice retryAdvice,
                                                FileService fileService,
                                                DispatchCompletedService dispatchCompletedService) {
        return IntegrationFlows.from(
                        Jms.inboundAdapter(connectionFactory).destination(activemqConfig.getJndi().getQueueInboundDispatch()),
                        spec -> spec.poller(poller -> poller.cron("0 0/1 * * * *").maxMessagesPerPoll(10)))
                .transform(Transformers.fromJson(InboundDispatchMessage.class))
                .wireTap("eventLogChannel")
                .enrichHeaders(
                        spec -> spec.headerFunction("TransferSite", headerValueOfTransferSite(transferSiteRepository, hostingConfig))
                                .headerFunction("TransferPackage", headerValueOfTransferPackage(transferPackageRepository))
                )
                .filter(filterInboundDispatchMessage())
                .handle(dispatchInboundDataFiles(delegatingSessionFactory, retryTemplate))
                .handle(dispatchInboundCompleted(dispatchCompletedService))
                .wireTap("eventLogChannel")
                .get();
    }

    private Function<Message<InboundDispatchMessage>, TransferPackage> headerValueOfTransferPackage(TransferPackageRepository transferPackageRepository) {
        return message -> {
            InboundDispatchMessage inboundDispatchMessage = message.getPayload();
            return transferPackageRepository.findByPackageName(inboundDispatchMessage.getTrackingId()).orElse(null);
        };
    }

    private Function<Message<InboundDispatchMessage>, TransferSite> headerValueOfTransferSite(TransferSiteRepository transferSiteRepository,
                                                                                              HostingSystemProperties hostingConfig) {
        return message -> {
            InboundDispatchMessage inboundDispatchMessage = message.getPayload();
            String destination = inboundDispatchMessage.getDestination();
            if (hostingConfig.getSystem().containsKey(destination)) {
                HostingSystem hostingSystem = hostingConfig.getSystem().get(destination);
                Path originalFilePath = Paths.get(inboundDispatchMessage.getOriginalFilePath());
                return transferSiteRepository.findByUsernameAndIpAndPortAndRemotePath(
                        hostingSystem.getAccountName(),
                        hostingSystem.getHost(),
                        hostingSystem.getPort(),
                        originalFilePath.getParent().toString()).orElse(null);

            }
            return null;
        };
    }

    private MessageSelector filterInboundDispatchMessage() {
        return message -> {
            InboundDispatchMessage payload = (InboundDispatchMessage) message.getPayload();
            TransferPackage transferPackage = message.getHeaders().get("TransferPackage", TransferPackage.class);
            TransferSite transferSite = message.getHeaders().get("TransferSite", TransferSite.class);

            if (transferPackage == null) {
                log.error("No TransferPackage \"{}\" were found in the Database.", payload.getTrackingId());
                return false;
            }

            if (transferSite == null) {
                log.error("No TransferSite \"{}\" were found in the Database.", payload.getTrackingId());
                return false;
            } else if (transferSite.getCredential() == null) {
                log.error("No login credentials were set for the TransferSite \"{}\"", transferSite.getId());
                return false;
            }

            return true;
        };
    }

    private GenericHandler<InboundDispatchMessage> dispatchInboundDataFiles(DelegatingSessionFactory<ChannelSftp.LsEntry> delegatingSessionFactory,
                                                                            RetryTemplate retryTemplate) {
        return (payload, headers) -> {


            TransferSite transferSite = headers.get("TransferSite", TransferSite.class);
            delegatingSessionFactory.setThreadKey(transferSite.getId());

            SftpRemoteFileTemplate remoteFileTemplate = new SftpRemoteFileTemplate(delegatingSessionFactory);
            return remoteFileTemplate.executeWithClient((ClientCallback<ChannelSftp, DispatchCompletedMessage>) client -> {

                DispatchCompletedMessageBuilder completedMessageBuilder = DispatchCompletedMessageBuilder.dispatchCompletedMessage(payload);
                File file = new File(payload.getLocalFilePath());
                try {
                    String fileSentOut = dispatchDataFile(file, transferSite.getRemotePath(), retryTemplate, client);
                    completedMessageBuilder.addProcessedDataFile(fileSentOut);
                } catch (SftpException e) {
                    completedMessageBuilder.addFailedDataFile(file.getAbsolutePath());
                    throw new DispatchException(MessageBuilder.withPayload(completedMessageBuilder.build()).build(), e);
                }

                return completedMessageBuilder.build();
            });
        };
    }

    private String dispatchDataFile(File file, String remotePath, RetryTemplate retryTemplate, ChannelSftp client) throws SftpException {
        return retryTemplate.execute(context -> {

            log.debug("put {} to {}", file.getAbsolutePath(), String.format("%s/%s", remotePath, file.getName()));

            client.put(file.getAbsolutePath(), String.format("%s/%s", remotePath, file.getName()));
            return file.getAbsolutePath();
        });
    }

    private GenericHandler<DispatchCompletedMessage> dispatchInboundCompleted(DispatchCompletedService dispatchCompletedService) {
        return (payload, headers) -> {
            dispatchCompletedService.setCompletionStatus(payload);
            return payload;
        };
    }

}
