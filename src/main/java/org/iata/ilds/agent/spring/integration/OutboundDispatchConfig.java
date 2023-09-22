package org.iata.ilds.agent.spring.integration;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.SftpException;
import lombok.extern.log4j.Log4j2;
import org.iata.ilds.agent.activemq.ActivemqConfigProperties;
import org.iata.ilds.agent.domain.builder.DispatchCompletedMessageBuilder;
import org.iata.ilds.agent.domain.builder.QuarantineMessageBuilder;
import org.iata.ilds.agent.domain.entity.FileType;
import org.iata.ilds.agent.domain.entity.TransferPackage;
import org.iata.ilds.agent.domain.entity.TransferSite;
import org.iata.ilds.agent.domain.entity.TransferStatus;
import org.iata.ilds.agent.domain.message.DispatchCompletedMessage;
import org.iata.ilds.agent.domain.message.outbound.OutboundDispatchMessage;
import org.iata.ilds.agent.domain.message.outbound.RoutingFileInfo;
import org.iata.ilds.agent.exception.OutboundDispatchException;
import org.iata.ilds.agent.service.DispatchCompletedService;
import org.iata.ilds.agent.service.FileService;
import org.iata.ilds.agent.spring.data.TransferPackageRepository;
import org.iata.ilds.agent.spring.data.TransferSiteRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.integration.core.MessageSelector;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.dsl.MessageChannels;
import org.springframework.integration.dsl.Transformers;
import org.springframework.integration.file.remote.ClientCallback;
import org.springframework.integration.file.remote.session.DelegatingSessionFactory;
import org.springframework.integration.handler.GenericHandler;
import org.springframework.integration.handler.advice.RequestHandlerRetryAdvice;
import org.springframework.integration.jms.dsl.Jms;
import org.springframework.integration.sftp.session.SftpRemoteFileTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.retry.support.RetryTemplate;

import javax.jms.ConnectionFactory;
import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;


@Log4j2
@Configuration
public class OutboundDispatchConfig {


    @Bean
    public MessageChannel errorChannel() {
        return MessageChannels.publishSubscribe().get();
    }


    @Bean
    public DelegatingSessionFactory<ChannelSftp.LsEntry> delegatingSessionFactory(DBSessionFactoryLocator dBSessionFactoryLocator) {
        return new DelegatingSessionFactory<>(dBSessionFactoryLocator);
    }

    @Bean
    public RetryTemplate retryTemplate() {
        return RetryTemplate.builder()
                .maxAttempts(3)
                .exponentialBackoff(1000, 2.0, 10*1000)
                .build();
    }

    @Bean
    public RequestHandlerRetryAdvice retryAdvice(RetryTemplate retryTemplate) {
        RequestHandlerRetryAdvice retryAdvice = new RequestHandlerRetryAdvice();
        retryAdvice.setRetryTemplate(retryTemplate);
        return retryAdvice;
    }

    @Bean
    public IntegrationFlow handleExceptionFlow(MessageChannel errorChannel,
                                               ConnectionFactory connectionFactory,
                                               ActivemqConfigProperties config,
                                               DispatchCompletedService dispatchCompletedService,
                                               RequestHandlerRetryAdvice retryAdvice) {
        return IntegrationFlows.from(errorChannel)
                .routeByException(spec -> {
                        spec.subFlowMapping(
                            OutboundDispatchException.class,
                            sf -> sf.handle(handleOutboundDispatchException(dispatchCompletedService))
                                    .transform(Transformers.toJson())
                                    .handle(Jms.outboundAdapter(connectionFactory).destination(config.getJndi().getQueueQuarantine()),
                                            spec1 -> spec1.advice(retryAdvice))
                        )
                        .defaultOutputToParentFlow();
                })
                .log()
                .get();
    }

    @Bean
    public IntegrationFlow handleOutboundDispatchFlow(ConnectionFactory connectionFactory,
                                                      ActivemqConfigProperties config,
                                                      TransferPackageRepository transferPackageRepository,
                                                      TransferSiteRepository transferSiteRepository,
                                                      DelegatingSessionFactory<ChannelSftp.LsEntry> delegatingSessionFactory,
                                                      RetryTemplate retryTemplate,
                                                      FileService fileService,
                                                      DispatchCompletedService dispatchCompletedService) {
        return IntegrationFlows.from(
                        Jms.inboundAdapter(connectionFactory).destination(config.getJndi().getQueueOutboundDispatch()),
                        spec -> spec.poller(poller -> poller.cron("0 0/1 * * * *").maxMessagesPerPoll(10)))
                .transform(Transformers.fromJson(OutboundDispatchMessage.class))
                .enrichHeaders(
                        spec -> spec.headerFunction("TransferSite", headerValueOfTransferSite(transferSiteRepository))
                                    .headerFunction("TransferPackage", headerValueOfTransferPackage(transferPackageRepository))
                )
                .filter(filterOutboundDispatchMessage())
                .handle(dispatchOutboundDataFiles(delegatingSessionFactory, retryTemplate, fileService))
                .handle(dispatchOutboundCompleted(dispatchCompletedService))
                .get();
    }


    private Function<Message<OutboundDispatchMessage>, TransferPackage> headerValueOfTransferPackage(TransferPackageRepository transferPackageRepository) {
        return message -> {
            OutboundDispatchMessage outboundDispatchMessage = message.getPayload();
            return transferPackageRepository.findByPackageName(outboundDispatchMessage.getTrackingId()).orElse(null);
        };
    }

    private Function<Message<OutboundDispatchMessage>, TransferSite> headerValueOfTransferSite(TransferSiteRepository transferSiteRepository) {
        return message -> {
            OutboundDispatchMessage outboundDispatchMessage = message.getPayload();
            RoutingFileInfo routingFileInfo = outboundDispatchMessage.getRoutingFileInfo();
            return transferSiteRepository.findByUsernameAndIpAndPortAndRemotePath(
                    routingFileInfo.getChannel().getAddress().getUser(),
                    routingFileInfo.getChannel().getAddress().getIp(),
                    routingFileInfo.getChannel().getAddress().getPort(),
                    routingFileInfo.getChannel().getAddress().getPath()).orElse(null);
        };
    }

    private MessageSelector filterOutboundDispatchMessage() {
        return message -> {
            OutboundDispatchMessage payload = (OutboundDispatchMessage) message.getPayload();
            TransferPackage transferPackage = message.getHeaders().get("TransferPackage", TransferPackage.class);
            TransferSite transferSite = message.getHeaders().get("TransferSite", TransferSite.class);

            if (transferPackage == null) {
                log.error("No TransferPackage \"{}\" were found in the Database.", payload.getTrackingId());
                return false;
            } else if (transferPackage.getStatus() == TransferStatus.Abandoned) {
                log.warn("The TransferPackage \"{}\" was abandoned.", payload.getTrackingId());
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

    private GenericHandler<OutboundDispatchMessage> dispatchOutboundDataFiles(DelegatingSessionFactory<ChannelSftp.LsEntry> delegatingSessionFactory,
                                                                              RetryTemplate retryTemplate,
                                                                              FileService fileService) {
        return (payload, headers) -> {
            Map<FileType, List<File>> dataFileGroups = fileService.fetchOutboundTransferFiles(payload.getLocalFilePath());
            Stream<File> dataFileGroupWithoutTDF = selectDataFileGroup(
                    dataFileGroups,
                    entry -> !FileType.Routing.equals(entry.getKey()) && !FileType.TDF.equals(entry.getKey()));

            Stream<File> dataFileGroupWithTDF = selectDataFileGroup(
                    dataFileGroups,
                    entry -> FileType.TDF.equals(entry.getKey()));


            TransferSite transferSite = headers.get("TransferSite", TransferSite.class);
            delegatingSessionFactory.setThreadKey(transferSite.getId());

            SftpRemoteFileTemplate remoteFileTemplate = new SftpRemoteFileTemplate(delegatingSessionFactory);
            return remoteFileTemplate.executeWithClient((ClientCallback<ChannelSftp, DispatchCompletedMessage>) client -> {

                DispatchCompletedMessageBuilder completedMessageBuilder = DispatchCompletedMessageBuilder.dispatchCompletedMessage(payload);

                Stream.concat(dataFileGroupWithoutTDF, dataFileGroupWithTDF).forEach(file -> {
                    try {
                        String fileSentOut = dispatchDataFile(file, transferSite.getRemotePath(), retryTemplate, client);
                        completedMessageBuilder.addProcessedDataFile(fileSentOut);
                    } catch (SftpException e) {
                        completedMessageBuilder.addFailedDataFile(file.getAbsolutePath());
                        throw new OutboundDispatchException(completedMessageBuilder.build(),  e);
                    }
                });

                return completedMessageBuilder.build();
            });
        };
    }


    private Stream<File> selectDataFileGroup(Map<FileType, List<File>> dataFileGroups, Predicate<Map.Entry<FileType, List<File>>> p) {
        return dataFileGroups.entrySet().stream().
                filter(p).
                flatMap(entry -> entry.getValue().stream());
    }

    private String dispatchDataFile(File file, String remotePath, RetryTemplate retryTemplate, ChannelSftp client) throws SftpException {
        return retryTemplate.execute(context -> {

            log.info("put {} to {}", file.getAbsolutePath(), String.format("%s/%s", remotePath, file.getName()));

            client.put(file.getAbsolutePath(), String.format("%s/%s", remotePath, file.getName()));
            return file.getAbsolutePath();
        });
    }


    private MessageHandler dispatchOutboundCompleted(DispatchCompletedService dispatchCompletedService) {
        return message -> {
            DispatchCompletedMessage payload = (DispatchCompletedMessage) message.getPayload();
            dispatchCompletedService.setCompletionStatus(payload);
        };
    }

    private GenericHandler<OutboundDispatchException> handleOutboundDispatchException(DispatchCompletedService dispatchCompletedService) {
        return (payload, headers) -> {

            dispatchCompletedService.setCompletionStatus(payload.getDispatchCompletedMessage());

            return QuarantineMessageBuilder.quarantineMessage(payload.getDispatchCompletedMessage())
                        .errorDescriptionOfParentProcess(NestedExceptionUtils.getMostSpecificCause(payload).getMessage())
                        .build();

        };
    }


}
