package org.iata.ilds.agent.spring.integration;

import lombok.extern.log4j.Log4j2;
import org.iata.ilds.agent.activemq.ActivemqConfigProperties;
import org.iata.ilds.agent.domain.entity.TransferPackage;
import org.iata.ilds.agent.domain.entity.TransferSite;
import org.iata.ilds.agent.domain.entity.TransferStatus;
import org.iata.ilds.agent.domain.message.outbound.OutboundDispatchMessage;
import org.iata.ilds.agent.domain.message.outbound.RoutingFileInfo;
import org.iata.ilds.agent.spring.data.TransferPackageRepository;
import org.iata.ilds.agent.spring.data.TransferSiteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.*;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.core.MessageSource;
import org.springframework.integration.handler.MessageProcessor;
import org.springframework.integration.handler.ServiceActivatingHandler;
import org.springframework.integration.jms.JmsDestinationPollingSource;
import org.springframework.integration.json.JsonToObjectTransformer;
import org.springframework.integration.router.AbstractMappingMessageRouter;
import org.springframework.integration.router.AbstractMessageRouter;
import org.springframework.integration.scheduling.PollerMetadata;
import org.springframework.integration.transaction.TransactionHandleMessageAdvice;
import org.springframework.integration.transaction.TransactionInterceptorBuilder;
import org.springframework.integration.transformer.HeaderEnricher;
import org.springframework.integration.transformer.support.HeaderValueMessageProcessor;
import org.springframework.integration.transformer.support.MessageProcessingHeaderValueMessageProcessor;
import org.springframework.integration.transformer.support.StaticHeaderValueMessageProcessor;
import org.springframework.jms.connection.JmsTransactionManager;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;

import javax.jms.ConnectionFactory;
import java.util.*;

import static java.lang.String.format;


@Log4j2
@Configuration
public class SendAgentConfig {


    @Bean
    public JmsTransactionManager jmsTransactionManager(ConnectionFactory connectionFactory) {
        return new JmsTransactionManager(connectionFactory);
    }

    @Bean
    public TransactionHandleMessageAdvice jmsTransactionInterceptor(JmsTransactionManager jmsTransactionManager) {
        return (TransactionHandleMessageAdvice) new TransactionInterceptorBuilder(true)
                .transactionManager(jmsTransactionManager)
                .isolation(Isolation.READ_COMMITTED)
                .propagation(Propagation.REQUIRES_NEW)
                .build();
    }

    @Bean
    public PollerMetadata jmsQueuePoller(TransactionHandleMessageAdvice jmsTransactionInterceptor) {
        PollerMetadata pollerMetadata = new PollerMetadata();
        pollerMetadata.setTrigger(new CronTrigger("0 0/1 * * * *"));
        pollerMetadata.setMaxMessagesPerPoll(10L);

        /*If you want the entire flow to be transactional, you must use a transactional poller with a JmsTransactionManager. */
        pollerMetadata.setAdviceChain(List.of(jmsTransactionInterceptor));

        return pollerMetadata;
    }




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
    public MessageChannel dispatchOutboundDataFiles() {
        return new DirectChannel();
    }

    @Bean
    public MessageChannel handleException() {
        return new DirectChannel();
    }


    @Bean
    @InboundChannelAdapter(value = "pollJmsQueueForOutbound", poller = @Poller("jmsQueuePoller"))
    public MessageSource<Object> pollJmsQueueForOutboundInboundChannelAdapter(ConnectionFactory connectionFactory, ActivemqConfigProperties config) {
        JmsDestinationPollingSource source = new JmsDestinationPollingSource(new JmsTemplate(connectionFactory));

        //A JMS TextMessage produces a string-based payload
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
            return transferPackageRepository.findByPackageName(outboundDispatchMessage.getTrackingId());
        });
    }

    @Bean
    public MessageProcessingHeaderValueMessageProcessor headerValueOfTransferSite(TransferSiteRepository transferSiteRepository) {
        return new MessageProcessingHeaderValueMessageProcessor(message -> {
            OutboundDispatchMessage outboundDispatchMessage = (OutboundDispatchMessage) message.getPayload();
            RoutingFileInfo routingFileInfo =  outboundDispatchMessage.getRoutingFileInfo();
            return  transferSiteRepository.findByUsernameAndIpAndPortAndRemotePath(
                    routingFileInfo.getChannel().getAddress().getUser(),
                    routingFileInfo.getChannel().getAddress().getIp(),
                    routingFileInfo.getChannel().getAddress().getPort(),
                    routingFileInfo.getChannel().getAddress().getPath());
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
    @Router(inputChannel = "verifyOutboundMessage", channelMappings = {"error=handleException","success=dispatchOutboundDataFiles"})
    public AbstractMessageRouter verifyOutboundMessageRouter() {
        return new AbstractMappingMessageRouter() {

            @Override
            protected List<Object> getChannelKeys(Message<?> message) {

                OutboundDispatchMessage outboundDispatchMessage = (OutboundDispatchMessage) message.getPayload();
                TransferPackage transferPackage = (TransferPackage) message.getHeaders().get("TransferPackage");
                TransferSite transferSite = (TransferSite) message.getHeaders().get("TransferSite");

                if (transferPackage == null) {
                    log.error("System Log: invalid outbound package_{} : no according transfer package in ilds db.", outboundDispatchMessage.getTrackingId());
                    return List.of("error");
                } else if (transferPackage.getStatus() == TransferStatus.Abandoned) {
                    log.info("System Log: package_{} abandoned ,so we filter it before sending it to MFT. ", outboundDispatchMessage.getTrackingId());
                    return List.of("error");
                }

                if (transferSite == null) {
                    log.error("System Log: invalid outbound destination in package_{} : no according transfer site in ilds db.",outboundDispatchMessage.getTrackingId());
                    return List.of("error");
                }

                return List.of("success");
            }
        };

    }

    @Bean
    @ServiceActivator(inputChannel = "dispatchOutboundDataFiles")
    public ServiceActivatingHandler dispatchOutboundDataFilesServiceActivator() {
        return new ServiceActivatingHandler((MessageProcessor<String>) message -> {

            OutboundDispatchMessage outboundDispatchMessage = (OutboundDispatchMessage) message.getPayload();
            TransferPackage transferPackage = (TransferPackage) message.getHeaders().get("TransferPackage");
            TransferSite transferSite = (TransferSite) message.getHeaders().get("TransferSite");

            return null;
        });
    }


    @Bean
    @ServiceActivator(inputChannel = "handleException")
    public ServiceActivatingHandler handleExceptionServiceActivator() {
        return new ServiceActivatingHandler((MessageProcessor<String>) message -> {

            OutboundDispatchMessage outboundDispatchMessage = (OutboundDispatchMessage) message.getPayload();

            return null;
        });
    }

}
