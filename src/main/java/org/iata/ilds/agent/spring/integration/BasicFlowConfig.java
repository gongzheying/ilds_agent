package org.iata.ilds.agent.spring.integration;

import lombok.extern.log4j.Log4j2;
import org.iata.ilds.agent.activemq.ActivemqConfigProperties;
import org.iata.ilds.agent.domain.builder.EventLogMessageBuilder;
import org.iata.ilds.agent.domain.builder.QuarantineMessageBuilder;
import org.iata.ilds.agent.domain.message.DispatchCompletedMessage;
import org.iata.ilds.agent.domain.message.eventlog.AbstractEventLogMessage;
import org.iata.ilds.agent.domain.message.outbound.OutboundDispatchMessage;
import org.iata.ilds.agent.exception.DispatchException;
import org.iata.ilds.agent.service.DispatchCompletedService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.dsl.MessageChannels;
import org.springframework.integration.dsl.Transformers;
import org.springframework.integration.handler.GenericHandler;
import org.springframework.integration.handler.advice.RequestHandlerRetryAdvice;
import org.springframework.integration.jms.dsl.Jms;
import org.springframework.integration.transformer.GenericTransformer;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandlingException;
import org.springframework.retry.support.RetryTemplate;

import javax.jms.ConnectionFactory;

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
    public IntegrationFlow exceptionTypeRouteFlow() {
        return IntegrationFlows.from("errorChannel")
                .routeByException(
                        r -> r.channelMapping(DispatchException.class, "dispatchExceptionChannel")
                                .defaultOutputToParentFlow()
                )
                .log()
                .get();
    }


    @Bean
    public IntegrationFlow dispatchExceptionFlow(ConnectionFactory connectionFactory,
                                                 ActivemqConfigProperties config,
                                                 DispatchCompletedService dispatchCompletedService,
                                                 RequestHandlerRetryAdvice retryAdvice) {
        return IntegrationFlows.from("dispatchExceptionChannel")
                .log()
                .transform(toDispatchException())
                .wireTap("eventLogChannel")
                .handle(handleDispatchException(dispatchCompletedService))
                .transform(Transformers.toJson())
                .handle(Jms.outboundAdapter(connectionFactory).destination(config.getJndi().getQueueQuarantine()),
                        spec1 -> spec1.advice(retryAdvice))
                .get();
    }


    private GenericTransformer<MessageHandlingException, DispatchException> toDispatchException() {
        return payload -> (DispatchException) payload.getMostSpecificCause();
    }

    private GenericHandler<DispatchException> handleDispatchException(DispatchCompletedService dispatchCompletedService) {
        return (payload, headers) -> {
            dispatchCompletedService.setCompletionStatus(payload.getDispatchCompletedMessage());

            return QuarantineMessageBuilder.quarantineMessage(payload.getDispatchCompletedMessage())
                    .errorDescriptionOfParentProcess(payload.getMessage())
                    .build();

        };
    }

    @Bean
    public IntegrationFlow eventLogFlow(ConnectionFactory connectionFactory,
                                        ActivemqConfigProperties config,
                                        RequestHandlerRetryAdvice retryAdvice) {
        return IntegrationFlows.from("eventLogChannel")
                .<Object, Class<?>>route(Object::getClass,
                        r -> r.subFlowMapping(
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
                                                payload -> EventLogMessageBuilder.eventLog(payload.getDispatchCompletedMessage())
                                                        .failedBySystem(payload.getMessage()))
                                )

                )
                .transform(Transformers.toJson())
                .log()
//                .handle(Jms.outboundAdapter(connectionFactory).destination(config.getJndi().getQueueEventLog()),
//                        spec1 -> spec1.advice(retryAdvice))
                .get();
    }

}
