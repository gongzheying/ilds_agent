package org.iata.ilds.agent.config.outbound;


import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties("outbound.flow")
@Getter
@Setter
public class OutboundFlowConfig {
    private int concurrentConsumers = 1;
    private int maxConcurrentConsumers = 1;
    private int maxMessagesPerTask = 1;
    private ProxyConfig proxy;


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

}
