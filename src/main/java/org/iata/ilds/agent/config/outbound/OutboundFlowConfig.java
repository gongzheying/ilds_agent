package org.iata.ilds.agent.config.outbound;


import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
@ConfigurationProperties("outbound.flow")
@Getter
@Setter
public class OutboundFlowConfig {
    private int concurrentConsumers = 1;
    private int maxConcurrentConsumers = 1;
    private int maxMessagesPerTask = 1;
    private ProxyConfig proxy;
    private JSchConfig jsch;


    @Getter
    @Setter
    public static class ProxyConfig {
        public enum Type {
            HTTP, SOCKS5
        }

        private Type type;
        private String host;
        private int port;
        private String user;
        private String password;
    }

    @Getter
    @Setter
    public static class JSchConfig {
        private Map<String,String> config;
    }

}
