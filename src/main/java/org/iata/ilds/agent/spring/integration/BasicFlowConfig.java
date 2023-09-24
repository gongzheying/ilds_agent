package org.iata.ilds.agent.spring.integration;

import lombok.extern.log4j.Log4j2;
import org.iata.ilds.agent.exception.OutboundDispatchException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.dsl.MessageChannels;
import org.springframework.integration.handler.advice.RequestHandlerRetryAdvice;
import org.springframework.messaging.MessageChannel;
import org.springframework.retry.support.RetryTemplate;

@Log4j2
@Configuration
public class BasicFlowConfig {

    @Bean
    public MessageChannel errorChannel() {
        return MessageChannels.publishSubscribe().get();
    }

    @Bean
    public MessageChannel outboundDispatchExceptionChannel() {
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
                        r -> r.channelMapping(OutboundDispatchException.class, "outboundDispatchExceptionChannel")
                                .defaultOutputToParentFlow()
                )
                .log()
                .get();
    }

}
