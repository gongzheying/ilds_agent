package org.iata.ilds.agent.spring.integration;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.SftpException;
import lombok.extern.log4j.Log4j2;
import org.iata.ilds.agent.config.activemq.ActivemqConfigProperties;
import org.iata.ilds.agent.config.inbound.HostingSystemLookup;
import org.iata.ilds.agent.domain.builder.DispatchCompletedMessageBuilder;
import org.iata.ilds.agent.domain.entity.TransferPackage;
import org.iata.ilds.agent.domain.entity.TransferSite;
import org.iata.ilds.agent.domain.message.DispatchCompletedMessage;
import org.iata.ilds.agent.domain.message.inbound.HostingSystem;
import org.iata.ilds.agent.domain.message.inbound.InboundDispatchMessage;

import org.iata.ilds.agent.exception.DispatchException;
import org.iata.ilds.agent.service.DispatchCompletedService;
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
import org.springframework.integration.jms.dsl.Jms;
import org.springframework.integration.sftp.session.SftpRemoteFileTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.retry.support.RetryTemplate;

import javax.jms.ConnectionFactory;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Function;


@Log4j2
@Configuration
public class InboundDispatchConfig {


    @Bean
    public IntegrationFlow inboundDispatchFlow(ConnectionFactory connectionFactory,
                                               ActivemqConfigProperties activemqConfig,
                                               HostingSystemLookup hostingSystemLookup,
                                               TransferPackageRepository transferPackageRepository,
                                               TransferSiteRepository transferSiteRepository,
                                               DelegatingSessionFactory<ChannelSftp.LsEntry> delegatingSessionFactory,
                                               RetryTemplate retryTemplate,
                                               DispatchCompletedService dispatchCompletedService) {
        return IntegrationFlows.from(
                        Jms.inboundAdapter(connectionFactory).destination(activemqConfig.getJndi().getQueueInboundDispatch()),
                        s -> s.poller(p -> p.cron("0 0/1 * * * *").maxMessagesPerPoll(10)))
                .transform(Transformers.fromJson(InboundDispatchMessage.class))
                .wireTap("eventLogChannel")
                .enrichHeaders(
                        s -> s.headerFunction("TransferSite", headerValueOfTransferSite(transferSiteRepository, hostingSystemLookup))
                                .headerFunction("TransferPackage", headerValueOfTransferPackage(transferPackageRepository))
                )
                .filter(filterInboundDispatchMessage(hostingSystemLookup))
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
                                                                                              HostingSystemLookup hostingSystemLookup) {
        return message -> {
            InboundDispatchMessage inboundDispatchMessage = message.getPayload();
            String destination = inboundDispatchMessage.getDestination();
            HostingSystem hostingSystem = hostingSystemLookup.getHostingSystem(destination);
            if (hostingSystem != null) {
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

    private MessageSelector filterInboundDispatchMessage(HostingSystemLookup hostingSystemLookup) {
        return message -> {
            InboundDispatchMessage payload = (InboundDispatchMessage) message.getPayload();
            TransferPackage transferPackage = message.getHeaders().get("TransferPackage", TransferPackage.class);
            TransferSite transferSite = message.getHeaders().get("TransferSite", TransferSite.class);

            if (transferPackage == null) {
                log.error("The TransferPackage \"{}\" is not registered in the system.", payload.getTrackingId());
                return false;
            }

            if (transferSite == null) {
                HostingSystem hostingSystem = hostingSystemLookup.getHostingSystem(payload.getDestination());
                if (hostingSystem != null) {
                    Path originalFilePath = Paths.get(payload.getOriginalFilePath());
                    log.error("The TransferSite \"{}\" is not registered in the system.",
                            String.format("%s@%s:%d:%s",
                                    hostingSystem.getAccountName(),
                                    hostingSystem.getHost(),
                                    hostingSystem.getPort(),
                                    originalFilePath.getParent().toString()

                            )
                    );
                } else {
                    log.error("The TransferSite \"{}\" is not registered in the system.", payload.getDestination());
                }


                return false;
            } else if (transferSite.getCredential() == null) {
                log.error("The TransferSite \"{}\" is not configured with login credential.", transferSite.getId());
                return false;
            }

            return true;
        };
    }

    private GenericHandler<InboundDispatchMessage> dispatchInboundDataFiles(DelegatingSessionFactory<ChannelSftp.LsEntry> delegatingSessionFactory,
                                                                            RetryTemplate retryTemplate) {
        return (payload, headers) -> {
            TransferSite transferSite = headers.get("TransferSite", TransferSite.class);

            DispatchCompletedMessageBuilder completedMessageBuilder = DispatchCompletedMessageBuilder.dispatchCompletedMessage(payload);

            try {
                delegatingSessionFactory.setThreadKey(transferSite.getId());
                SftpRemoteFileTemplate remoteFileTemplate = new SftpRemoteFileTemplate(delegatingSessionFactory);
                String fileSentOut = retryTemplate.<String, MessagingException>execute(ctx -> remoteFileTemplate.executeWithClient(
                        (ClientCallback<ChannelSftp, String>) client -> {
                            File file = new File(payload.getLocalFilePath());
                            String remotePath = transferSite.getRemotePath();
                            try {
                                client.put(file.getAbsolutePath(), String.format("%s/%s", remotePath, file.getName()));
                            } catch (SftpException e) {
                                throw new MessagingException(String.format("An error occurred while uploading file %s", file.getAbsolutePath()), e);
                            }
                            return file.getAbsolutePath();
                        }
                ));

                completedMessageBuilder.addProcessedDataFile(fileSentOut);
                return completedMessageBuilder.build();

            } catch (MessagingException e) {
                completedMessageBuilder.addFailedDataFile(payload.getLocalFilePath());
                throw new DispatchException(MessageBuilder.withPayload(completedMessageBuilder.build()).build(), e.getCause());
            }

        };
    }


    private GenericHandler<DispatchCompletedMessage> dispatchInboundCompleted(DispatchCompletedService dispatchCompletedService) {
        return (payload, headers) -> {
            dispatchCompletedService.setCompletionStatus(payload);
            return payload;
        };
    }

}
