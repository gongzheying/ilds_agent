package org.iata.ilds.agent.spring.integration;

import org.iata.ilds.agent.activemq.ActivemqConfigProperties;
import org.iata.ilds.agent.domain.entity.TransferPackage;
import org.iata.ilds.agent.domain.entity.TransferSite;
import org.iata.ilds.agent.domain.message.outbound.OutboundDispatchMessage;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.InboundChannelAdapter;
import org.springframework.integration.annotation.Poller;
import org.springframework.integration.annotation.Transformer;
import org.springframework.integration.core.MessageSource;
import org.springframework.integration.jms.JmsDestinationPollingSource;
import org.springframework.integration.json.JsonToObjectTransformer;
import org.springframework.integration.scheduling.PollerMetadata;
import org.springframework.integration.transaction.TransactionHandleMessageAdvice;
import org.springframework.integration.transaction.TransactionInterceptorBuilder;
import org.springframework.integration.transformer.HeaderEnricher;
import org.springframework.integration.transformer.support.HeaderValueMessageProcessor;
import org.springframework.integration.transformer.support.StaticHeaderValueMessageProcessor;
import org.springframework.jms.connection.JmsTransactionManager;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;

import javax.jms.ConnectionFactory;
import java.util.*;

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
    @InboundChannelAdapter(value = "pollingJmsQueueForOutboundDispatch", poller = @Poller("jmsQueuePoller"))
    public MessageSource<Object> pollingJmsQueueForOutboundDispatch(ConnectionFactory connectionFactory, ActivemqConfigProperties config) {
        JmsDestinationPollingSource source = new JmsDestinationPollingSource(new JmsTemplate(connectionFactory));

        //A JMS TextMessage produces a string-based payload
        source.setDestinationName(config.getJndi().getQueueOutboundDispatch());

        return source;
    }


    @Bean
    @Transformer(inputChannel = "pollingJmsQueueForOutboundDispatch", outputChannel = "convertTextToOutboundDispatchMessage")
    public JsonToObjectTransformer convertTextToOutboundDispatchMessage() {
        return new JsonToObjectTransformer(OutboundDispatchMessage.class);
    }


    @Bean
    @Transformer(inputChannel = "convertTextToOutboundDispatchMessage", outputChannel = "enrichOutboundDispatchMessage")
    public HeaderEnricher enrichOutboundDispatchMessage() {

        Optional<TransferPackage> transferPackage = Optional.empty();
        Optional<TransferSite> transferSite = Optional.empty();

        Map<String, ? extends HeaderValueMessageProcessor<?>> headersToAdd = Map.of(
                "transferPackage", new StaticHeaderValueMessageProcessor<>(transferPackage),
                "transferSite", new StaticHeaderValueMessageProcessor<>(transferSite));


        return new HeaderEnricher(headersToAdd);
    }



}
