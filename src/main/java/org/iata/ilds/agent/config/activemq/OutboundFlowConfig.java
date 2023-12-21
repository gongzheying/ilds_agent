package org.iata.ilds.agent.config.activemq;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OutboundFlowConfig {
    public final int concurrentConsumers;
    public final int maxConcurrentConsumers;
    public final int maxMessagesPerTask;

    public OutboundFlowConfig(@Value("${outboundFlow.concurrentConsumers:20}") int concurrentConsumers,
                              @Value("${outboundFlow.maxConcurrentConsumers:20}") int maxConcurrentConsumers,
                              @Value("${outboundFlow.maxMessagesPerTask:10}") int maxMessagesPerTask){
        this.maxConcurrentConsumers = maxConcurrentConsumers;
        this.concurrentConsumers = concurrentConsumers;
        this.maxMessagesPerTask = maxMessagesPerTask;
    }

}
