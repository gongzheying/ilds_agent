package org.iata.ilds.agent.spring.integration;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.SftpException;
import lombok.extern.log4j.Log4j2;
import org.iata.ilds.agent.activemq.ActivemqConfigProperties;
import org.iata.ilds.agent.domain.entity.FileType;
import org.iata.ilds.agent.domain.entity.TransferPackage;
import org.iata.ilds.agent.domain.entity.TransferSite;
import org.iata.ilds.agent.domain.entity.TransferStatus;
import org.iata.ilds.agent.domain.message.DispatchCompletedMessage;
import org.iata.ilds.agent.domain.message.outbound.OutboundDispatchMessage;
import org.iata.ilds.agent.domain.message.outbound.RoutingFileInfo;
import org.iata.ilds.agent.service.FileService;
import org.iata.ilds.agent.spring.data.TransferPackageRepository;
import org.iata.ilds.agent.spring.data.TransferSiteRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.*;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.core.MessageSource;
import org.springframework.integration.file.remote.ClientCallback;
import org.springframework.integration.file.remote.session.DelegatingSessionFactory;
import org.springframework.integration.handler.MessageProcessor;
import org.springframework.integration.handler.ServiceActivatingHandler;
import org.springframework.integration.jms.JmsDestinationPollingSource;
import org.springframework.integration.json.JsonToObjectTransformer;
import org.springframework.integration.router.AbstractMappingMessageRouter;
import org.springframework.integration.router.AbstractMessageRouter;
import org.springframework.integration.sftp.session.SftpRemoteFileTemplate;
import org.springframework.integration.transformer.HeaderEnricher;
import org.springframework.integration.transformer.support.HeaderValueMessageProcessor;
import org.springframework.integration.transformer.support.MessageProcessingHeaderValueMessageProcessor;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.retry.support.RetryTemplate;

import javax.jms.ConnectionFactory;
import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;


@Log4j2
@Configuration
public class SendAgentConfig {
//
//
//    @Bean
//    public TransactionHandleMessageAdvice jmsTransactionInterceptor(JmsTransactionManager jmsTransactionManager) {
//        return (TransactionHandleMessageAdvice) new TransactionInterceptorBuilder(true)
//                .transactionManager(jmsTransactionManager)
//                .isolation(Isolation.READ_COMMITTED)
//                .propagation(Propagation.REQUIRES_NEW)
//                .build();
//    }
//


    @Bean
    public MessageChannel pollJmsQueueForOutbound() {
        return new DirectChannel();
    }


    @Bean
    public MessageChannel enrichOutboundMessage() {
        return new DirectChannel();
    }

    @Bean
    public MessageChannel verifyOutboundMessage() {
        return new DirectChannel();
    }


    @Bean
    public MessageChannel prepareOutboundSessionFactory() {
        return new DirectChannel();
    }

    @Bean
    public MessageChannel dispatchOutboundDataFiles() {
        return new DirectChannel();
    }

    @Bean
    public MessageChannel dispatchOutboundCompleted() {
        return new DirectChannel();
    }

    @Bean
    public MessageChannel handleException() {
        return new DirectChannel();
    }


    @Bean
    @InboundChannelAdapter(value = "pollJmsQueueForOutbound", poller = @Poller(cron = "0 0/1 * * * *", maxMessagesPerPoll = "10"))
    public MessageSource<Object> pollJmsQueueForOutboundInboundChannelAdapter(ConnectionFactory connectionFactory, ActivemqConfigProperties config) {
        JmsDestinationPollingSource source = new JmsDestinationPollingSource(new JmsTemplate(connectionFactory));
        source.setDestinationName(config.getJndi().getQueueOutboundDispatch());
        return source;
    }


    @Bean
    @Transformer(inputChannel = "pollJmsQueueForOutbound", outputChannel = "enrichOutboundMessage")
    public JsonToObjectTransformer convertTextToOutboundMessageTransformer() {
        return new JsonToObjectTransformer(OutboundDispatchMessage.class);
    }


    @Bean
    public MessageProcessingHeaderValueMessageProcessor headerValueOfTransferPackage(TransferPackageRepository transferPackageRepository) {
        return new MessageProcessingHeaderValueMessageProcessor(message -> {
            OutboundDispatchMessage outboundDispatchMessage = (OutboundDispatchMessage) message.getPayload();
            return transferPackageRepository.findByPackageName(outboundDispatchMessage.getTrackingId()).orElse(null);
        });
    }

    @Bean
    public MessageProcessingHeaderValueMessageProcessor headerValueOfTransferSite(TransferSiteRepository transferSiteRepository) {
        return new MessageProcessingHeaderValueMessageProcessor(message -> {
            OutboundDispatchMessage outboundDispatchMessage = (OutboundDispatchMessage) message.getPayload();
            RoutingFileInfo routingFileInfo = outboundDispatchMessage.getRoutingFileInfo();
            return transferSiteRepository.findByUsernameAndIpAndPortAndRemotePath(
                    routingFileInfo.getChannel().getAddress().getUser(),
                    routingFileInfo.getChannel().getAddress().getIp(),
                    routingFileInfo.getChannel().getAddress().getPort(),
                    routingFileInfo.getChannel().getAddress().getPath()).orElse(null);
        });
    }

    @Bean
    @Transformer(inputChannel = "enrichOutboundMessage", outputChannel = "verifyOutboundMessage")
    public HeaderEnricher enrichOutboundMessageHeaderEnricher(
            MessageProcessingHeaderValueMessageProcessor headerValueOfTransferPackage,
            MessageProcessingHeaderValueMessageProcessor headerValueOfTransferSite) {

        Map<String, ? extends HeaderValueMessageProcessor<?>> headersToAdd = Map.of(
                "TransferPackage", headerValueOfTransferPackage,
                "TransferSite", headerValueOfTransferSite);
        return new HeaderEnricher(headersToAdd);
    }

    @Bean
    @Router(inputChannel = "verifyOutboundMessage", channelMappings = {"error=handleException", "success=dispatchOutboundDataFiles"})
    public AbstractMessageRouter verifyOutboundMessageRouter() {
        return new AbstractMappingMessageRouter() {

            @Override
            protected List<Object> getChannelKeys(Message<?> message) {

                OutboundDispatchMessage outboundDispatchMessage = (OutboundDispatchMessage) message.getPayload();
                TransferPackage transferPackage = (TransferPackage) message.getHeaders().get("TransferPackage");
                TransferSite transferSite = (TransferSite) message.getHeaders().get("TransferSite");

                if (transferPackage == null) {
                    log.error("No TransferPackage \"{}\" were found in the Database.", outboundDispatchMessage.getTrackingId());
                    return List.of("error");
                } else if (transferPackage.getStatus() == TransferStatus.Abandoned) {
                    log.warn("The TransferPackage \"{}\" was abandoned.", outboundDispatchMessage.getTrackingId());
                    return List.of("error");
                }

                if (transferSite == null) {
                    log.error("No TransferSite \"{}\" were found in the Database.", outboundDispatchMessage.getTrackingId());
                    return List.of("error");
                } else if (transferSite.getCredential() == null) {
                    log.error("No login credentials were set for the TransferSite \"{}\"", transferSite.getId());
                    return null;

                }

                return List.of("success");
            }
        };

    }


    @Bean
    public DelegatingSessionFactory<ChannelSftp.LsEntry> delegatingSessionFactory(DBSessionFactoryLocator dBSessionFactoryLocator) {
        return new DelegatingSessionFactory<>(dBSessionFactoryLocator);
    }

    @Bean
    @ServiceActivator(inputChannel = "prepareOutboundSessionFactory")
    public MessageHandler prepareOutboundSessionFactoryServiceActivator(DelegatingSessionFactory<ChannelSftp.LsEntry> delegatingSessionFactory) {
        ServiceActivatingHandler messageHandler = new ServiceActivatingHandler(message -> {

            TransferSite transferSite = (TransferSite) message.getHeaders().get("TransferSite");
            delegatingSessionFactory.setThreadKey(transferSite.getId());

            return message;
        });
        messageHandler.setOutputChannelName("dispatchOutboundDataFiles");
        return messageHandler;
    }

    @Bean
    public RetryTemplate retryTemplate() {
        return RetryTemplate.builder()
                .maxAttempts(3)
                .exponentialBackoff(1000, 2, 10000)
                .retryOn(JSchException.class)
                .traversingCauses()
                .build();
    }


    private List<File> selectDataFileGroup(Map<FileType, List<File>> dataFileGroups, Predicate<Map.Entry<FileType, List<File>>> p) {
        return dataFileGroups.entrySet().stream().
                filter(p).
                flatMap(entry -> entry.getValue().stream()).
                collect(Collectors.toList());
    }

    private String dispatchDataFile(File file, String remotePath, RetryTemplate retryTemplate, ChannelSftp client) throws SftpException {
        return retryTemplate.execute(context -> {
            client.put(file.getAbsolutePath(), String.format("%s/%s", remotePath, file.getName()));
            return file.getAbsolutePath();
        });
    }

    @Bean
    @ServiceActivator(inputChannel = "dispatchOutboundDataFiles")
    public MessageHandler dispatchOutboundDataFilesServiceActivator(
            DelegatingSessionFactory<ChannelSftp.LsEntry> delegatingSessionFactory,
            RetryTemplate retryTemplate,
            FileService fileService) {
        ServiceActivatingHandler messageHandler =  new ServiceActivatingHandler(message -> {

            OutboundDispatchMessage outboundDispatchMessage = (OutboundDispatchMessage) message.getPayload();


            Map<FileType, List<File>> dataFileGroups = fileService.fetchOutboundTransferFiles(outboundDispatchMessage.getLocalFilePath());
            List<File> dataFileGroupWithoutTDF = selectDataFileGroup(
                    dataFileGroups,
                    entry -> !FileType.Routing.equals(entry.getKey()) && !FileType.TDF.equals(entry.getKey()));

            List<File> dataFileGroupWithTDF = selectDataFileGroup(
                    dataFileGroups,
                    entry -> FileType.TDF.equals(entry.getKey()));


            TransferSite transferSite = (TransferSite) message.getHeaders().get("TransferSite");



            SftpRemoteFileTemplate remoteFileTemplate = new SftpRemoteFileTemplate(delegatingSessionFactory);
            return remoteFileTemplate.executeWithClient((ClientCallback<ChannelSftp, DispatchCompletedMessage>) client -> {
                DispatchCompletedMessage completedMessage = new DispatchCompletedMessage();
                completedMessage.setTrackingId(outboundDispatchMessage.getTrackingId());
                completedMessage.setProcessingStartTime(outboundDispatchMessage.getProcessingStartTime());

                try {
                    for (File file1 : dataFileGroupWithoutTDF) {
                        completedMessage.getLocalFilePath().add(dispatchDataFile(file1, transferSite.getRemotePath(), retryTemplate, client));
                    }
                    for (File file : dataFileGroupWithTDF) {
                        completedMessage.getLocalFilePath().add(dispatchDataFile(file, transferSite.getRemotePath(), retryTemplate, client));
                    }

                }catch (SftpException e) {
                    //TODO:
                    throw new RuntimeException(e);
                }

                completedMessage.setSuccessful(true);
                return completedMessage;
            });


        });
        messageHandler.setOutputChannelName("dispatchOutboundCompleted");
        return messageHandler;
    }


    @Bean
    @ServiceActivator(inputChannel = "handleException")
    public ServiceActivatingHandler handleExceptionServiceActivator() {
        return new ServiceActivatingHandler(message -> {

            OutboundDispatchMessage outboundDispatchMessage = (OutboundDispatchMessage) message.getPayload();

            return message;
        });
    }

}
