package org.iata.ilds.agent.config.activemq;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RetryConfig {
    public final long initialInterval;
    public final double multiplier;
    public final long maxInterval;

    public RetryConfig(@Value("${retry.initialInterval:1000}") long initialInterval,
                       @Value("${retry.multiplier:2.0}") double multiplier,
                       @Value("${retry.maxInterval:10}") long maxInterval){
        this.initialInterval = initialInterval;
        this.multiplier = multiplier;
        this.maxInterval = maxInterval;
    }

}
