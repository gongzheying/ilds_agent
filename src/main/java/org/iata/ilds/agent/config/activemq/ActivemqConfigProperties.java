package org.iata.ilds.agent.config.activemq;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;


@Configuration
@ConfigurationProperties("activemq")
@Getter
@Setter
public class ActivemqConfigProperties {

    private CONTEXT ctx;
    private JNDI jndi;

    @Getter
    @Setter
    public static class CONTEXT {
        private String provideUrl;
        private String initialContextFactory;
        private String securityPrincipal;
        private String securityCredentials;
    }

    @Getter
    @Setter
    public static class JNDI {

        private String connectionFactory;
        private String queueOutboundDispatch;
        private String queueQuarantine;
        private String queueEventLog;
        private String queueInboundDispatch;

    }

}
