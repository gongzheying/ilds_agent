package org.iata.ilds.agent.spring.integration;

import com.jcraft.jsch.ChannelSftp;
import lombok.extern.log4j.Log4j2;
import org.iata.ilds.agent.config.activemq.ActivemqConfigProperties;
import org.iata.ilds.agent.domain.builder.EventLogMessageBuilder;
import org.iata.ilds.agent.domain.builder.QuarantineMessageBuilder;
import org.iata.ilds.agent.domain.message.DispatchCompletedMessage;
import org.iata.ilds.agent.domain.message.eventlog.AbstractEventLogMessage;
import org.iata.ilds.agent.domain.message.inbound.InboundDispatchMessage;
import org.iata.ilds.agent.domain.message.outbound.OutboundDispatchMessage;
import org.iata.ilds.agent.exception.DispatchException;
import org.iata.ilds.agent.service.DispatchCompletedService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.dsl.MessageChannels;
import org.springframework.integration.dsl.Transformers;
import org.springframework.integration.file.remote.session.DelegatingSessionFactory;
import org.springframework.integration.handler.GenericHandler;
import org.springframework.integration.handler.advice.RequestHandlerRetryAdvice;
import org.springframework.integration.jms.dsl.Jms;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.retry.support.RetryTemplate;

import javax.jms.ConnectionFactory;
import java.io.PrintWriter;
import java.io.StringWriter;

import static org.springframework.integration.handler.LoggingHandler.Level;

@Log4j2
@Configuration
public class BasicFlowConfig {

    @Bean
    public MessageChannel errorChannel() {
        return MessageChannels.publishSubscribe().get();
    }

    @Bean
    public MessageChannel dispatchExceptionChannel() {
        return MessageChannels.direct().get();
    }

    @Bean
    public MessageChannel eventLogChannel() {
        return MessageChannels.direct().get();
    }

    @Bean
    public RetryTemplate retryTemplate() {
        return RetryTemplate.builder()
                .maxAttempts(3)
                .exponentialBackoff(1000, 2.0, 10 * 1000)
                .build();
    }

    @Bean
    public RequestHandlerRetryAdvice retryAdvice(RetryTemplate retryTemplate) {
        RequestHandlerRetryAdvice retryAdvice = new RequestHandlerRetryAdvice();
        retryAdvice.setRetryTemplate(retryTemplate);
        return retryAdvice;
    }


    @Bean
    public DelegatingSessionFactory<ChannelSftp.LsEntry> delegatingSessionFactory(DBSessionFactoryLocator dBSessionFactoryLocator) {
        return new DelegatingSessionFactory<>(dBSessionFactoryLocator);
    }


    @Bean
    public IntegrationFlow exceptionTypeRouteFlow() {
        return IntegrationFlows.from("errorChannel")
                .routeByException(
                        r -> r.channelMapping(DispatchException.class, "dispatchExceptionChannel")
                                .defaultOutputToParentFlow()
                )
                .log(Level.ERROR, BasicFlowConfig.class.getName())
                .get();
    }


    @Bean
    public IntegrationFlow dispatchExceptionFlow(ConnectionFactory connectionFactory,
                                                 ActivemqConfigProperties activemqConfig,
                                                 DispatchCompletedService dispatchCompletedService,
                                                 RequestHandlerRetryAdvice retryAdvice) {
        return IntegrationFlows.from("dispatchExceptionChannel")
                .wireTap("eventLogChannel")
                .handle(handleDispatchException(dispatchCompletedService))
                .transform(Transformers.toJson())
                .handle(Jms.outboundAdapter(connectionFactory).destination(activemqConfig.getJndi().getQueueQuarantine()),
                        s -> s.advice(retryAdvice))
                .get();
    }

    private GenericHandler<DispatchException> handleDispatchException(DispatchCompletedService dispatchCompletedService) {
        return (payload, headers) -> {
            Message<DispatchCompletedMessage> failedMessage = (Message<DispatchCompletedMessage>) payload.getFailedMessage();
            Throwable cause = payload.getRootCause();

            dispatchCompletedService.setCompletionStatus(failedMessage.getPayload());

            return QuarantineMessageBuilder.quarantineMessage(failedMessage.getPayload())
                    .errorDescriptionOfParentProcess(cause.getMessage())
                    .build();

        };
    }

    @Bean
    public IntegrationFlow eventLogFlow(ConnectionFactory connectionFactory,
                                        ActivemqConfigProperties activemqConfig,
                                        RequestHandlerRetryAdvice retryAdvice) {
        return IntegrationFlows.from("eventLogChannel")
                .<Object, Class<?>>route(Object::getClass,
                        r -> r.subFlowMapping(
                                        InboundDispatchMessage.class,
                                        f -> f.<InboundDispatchMessage, AbstractEventLogMessage>transform(
                                                payload -> EventLogMessageBuilder.eventLog(payload).started())
                                )
                                .subFlowMapping(
                                        OutboundDispatchMessage.class,
                                        f -> f.<OutboundDispatchMessage, AbstractEventLogMessage>transform(
                                                payload -> EventLogMessageBuilder.eventLog(payload).started())
                                )
                                .subFlowMapping(
                                        DispatchCompletedMessage.class,
                                        f -> f.<DispatchCompletedMessage, AbstractEventLogMessage>transform(
                                                payload -> EventLogMessageBuilder.eventLog(payload).completed())
                                )
                                .subFlowMapping(
                                        DispatchException.class,
                                        f -> f.<DispatchException, AbstractEventLogMessage>transform(
                                                payload -> {
                                                    Message<DispatchCompletedMessage> failedMessage = (Message<DispatchCompletedMessage>) payload.getFailedMessage();
                                                    Throwable cause = payload.getRootCause();
                                                    return EventLogMessageBuilder.eventLog(failedMessage.getPayload()).failedBySystem(cause.getMessage());
                                                })

                                )

                )
                .transform(Transformers.toJson())
                .log(Level.INFO, BasicFlowConfig.class.getName())
                .handle(Jms.outboundAdapter(connectionFactory).destination(activemqConfig.getJndi().getQueueEventLog()),
                        s -> s.advice(retryAdvice))
                .get();
    }

}
