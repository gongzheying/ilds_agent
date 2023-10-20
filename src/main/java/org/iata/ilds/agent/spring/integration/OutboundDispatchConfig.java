package org.iata.ilds.agent.spring.integration;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.SftpException;
import lombok.extern.log4j.Log4j2;
import org.iata.ilds.agent.config.activemq.ActivemqConfigProperties;
import org.iata.ilds.agent.domain.builder.DispatchCompletedMessageBuilder;
import org.iata.ilds.agent.domain.entity.FileType;
import org.iata.ilds.agent.domain.entity.TransferPackage;
import org.iata.ilds.agent.domain.entity.TransferSite;
import org.iata.ilds.agent.domain.entity.TransferStatus;
import org.iata.ilds.agent.domain.message.DispatchCompletedMessage;
import org.iata.ilds.agent.domain.message.outbound.OutboundDispatchMessage;
import org.iata.ilds.agent.domain.message.outbound.RoutingFileInfo;
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
import org.springframework.integration.jms.dsl.Jms;
import org.springframework.integration.sftp.session.SftpRemoteFileTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.util.StringUtils;

import javax.jms.ConnectionFactory;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;


@Log4j2
@Configuration
public class OutboundDispatchConfig {


    @Bean
    public IntegrationFlow outboundDispatchFlow(ConnectionFactory connectionFactory,
                                                ActivemqConfigProperties activemqConfig,
                                                TransferPackageRepository transferPackageRepository,
                                                TransferSiteRepository transferSiteRepository,
                                                DelegatingSessionFactory<ChannelSftp.LsEntry> delegatingSessionFactory,
                                                RetryTemplate retryTemplate,
                                                FileService fileService,
                                                DispatchCompletedService dispatchCompletedService) {
        return IntegrationFlows.from(
                        Jms.inboundAdapter(connectionFactory).destination(activemqConfig.getJndi().getQueueOutboundDispatch()),
                        s -> s.poller(p -> p.cron("0 0/1 * * * *").maxMessagesPerPoll(10)))
                .transform(Transformers.fromJson(OutboundDispatchMessage.class))
                .wireTap("eventLogChannel")
                .enrichHeaders(
                        s -> s.headerFunction("TransferSite", headerValueOfTransferSite(transferSiteRepository))
                                .headerFunction("TransferPackage", headerValueOfTransferPackage(transferPackageRepository))
                )
                .filter(filterOutboundDispatchMessage())
                .handle(dispatchOutboundDataFiles(delegatingSessionFactory, retryTemplate, fileService))
                .handle(dispatchOutboundCompleted(dispatchCompletedService))
                .wireTap("eventLogChannel")
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
                log.error("The TransferPackage \"{}\" is not registered in the system.", payload.getTrackingId());
                return false;
            } else if (transferPackage.getStatus() == TransferStatus.Abandoned) {
                log.warn("The TransferPackage \"{}\" is abandoned.", payload.getTrackingId());
                return false;
            }

            if (transferSite == null) {
                log.error("The TransferSite \"{}\" is not registered in the system.",
                        String.format("%s@%s:%d:%s",
                                payload.getRoutingFileInfo().getChannel().getAddress().getUser(),
                                payload.getRoutingFileInfo().getChannel().getAddress().getIp(),
                                payload.getRoutingFileInfo().getChannel().getAddress().getPort(),
                                payload.getRoutingFileInfo().getChannel().getAddress().getPath()
                        )
                );
                return false;
            } else if (transferSite.getCredential() == null) {
                log.error("The TransferSite \"{}\" is not configured with login credential.", transferSite.getId());
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
            List<File> dataFileGroupWithoutTDF = selectDataFileGroup(
                    dataFileGroups,
                    entry -> !FileType.Routing.equals(entry.getKey()) && !FileType.TDF.equals(entry.getKey()));

            List<File> dataFileGroupWithTDF = selectDataFileGroup(
                    dataFileGroups,
                    entry -> FileType.TDF.equals(entry.getKey()));

            List<File> dataFiles = Stream.of(dataFileGroupWithoutTDF, dataFileGroupWithTDF).flatMap(Collection::stream).toList();

            TransferSite transferSite = headers.get("TransferSite", TransferSite.class);

            DispatchCompletedMessageBuilder completedMessageBuilder = DispatchCompletedMessageBuilder.dispatchCompletedMessage(payload);


            try {
                delegatingSessionFactory.setThreadKey(transferSite.getId());
                SftpRemoteFileTemplate remoteFileTemplate = new SftpRemoteFileTemplate(delegatingSessionFactory);

                List<String> processedDataFiles = retryTemplate.<List<String>, MessagingException>execute(ctx -> remoteFileTemplate.executeWithClient(
                        (ClientCallback<ChannelSftp, List<String>>) client -> {
                            List<String> fileSentOutList = new ArrayList<>();
                            dataFiles.forEach(file -> {
                                String remotePath = transferSite.getRemotePath();
                                try {
                                    client.put(file.getAbsolutePath(), String.format("%s/%s", remotePath, file.getName()));
                                } catch (SftpException e) {
                                    throw new MessagingException(String.format("An error occurred while uploading file %s", file.getAbsolutePath()), e);
                                }

                                fileSentOutList.add(file.getAbsolutePath());
                            });
                            return fileSentOutList;
                        }
                ));

                processedDataFiles.forEach(completedMessageBuilder::addProcessedDataFile);

                return completedMessageBuilder.build();
            } catch (MessagingException e) {
                if (e.getCause() instanceof SftpException) {
                    completedMessageBuilder.addFailedDataFile(StringUtils.replace(e.getMessage(), "An error occurred while uploading file ", ""));
                } else { //failed to create SFTP Session
                    completedMessageBuilder.addFailedDataFile(dataFiles.get(0).getAbsolutePath());
                }
                throw new DispatchException(MessageBuilder.withPayload(completedMessageBuilder.build()).build(), e.getCause());
            }


        };
    }


    private List<File> selectDataFileGroup(Map<FileType, List<File>> dataFileGroups, Predicate<Map.Entry<FileType, List<File>>> p) {
        return dataFileGroups.entrySet().stream().
                filter(p).
                flatMap(entry -> entry.getValue().stream()).toList();
    }

    private GenericHandler<DispatchCompletedMessage> dispatchOutboundCompleted(DispatchCompletedService dispatchCompletedService) {
        return (payload, headers) -> {
            dispatchCompletedService.setCompletionStatus(payload);
            return payload;
        };
    }

}
