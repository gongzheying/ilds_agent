package org.iata.ilds.agent.config.inbound;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Map;


@Configuration
@ConfigurationProperties("hosting")
@Getter
@Setter
public class HostingSystemProperties {

    private Map<String, HostingSystem> system;

    @Getter
    @Setter
    public static class HostingSystem {
        private String host;
        private int port;
        private String accountName;
    }

}
