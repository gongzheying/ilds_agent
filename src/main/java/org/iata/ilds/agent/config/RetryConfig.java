package org.iata.ilds.agent.config;


import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties("retry")
@Getter
@Setter
public class RetryConfig {
    private long initialInterval = 1000L;
    private double multiplier = 2;
    private long maxInterval = 30000L;


}
