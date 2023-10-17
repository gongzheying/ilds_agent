package org.iata.ilds.agent.config.activemq;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jndi.JndiObjectFactoryBean;

import javax.jms.ConnectionFactory;
import javax.naming.Context;
import java.util.Properties;

@Configuration
public class ActivemqConfig {

    private final ActivemqConfigProperties config;

    public ActivemqConfig(ActivemqConfigProperties config) {
        this.config = config;
    }

    @Bean
    public JndiObjectFactoryBean connectionFactory(){
        JndiObjectFactoryBean jndiObjectFactoryBean =new JndiObjectFactoryBean();
        jndiObjectFactoryBean.setJndiName(config.getJndi().getConnectionFactory());
        Properties env = new Properties();
        env.put(Context.INITIAL_CONTEXT_FACTORY, config.getCtx().getInitialContextFactory());
        env.put(Context.PROVIDER_URL, config.getCtx().getProvideUrl());
        env.put(Context.SECURITY_PRINCIPAL, config.getCtx().getSecurityPrincipal());
        env.put(Context.SECURITY_CREDENTIALS, config.getCtx().getSecurityCredentials());
        jndiObjectFactoryBean.setJndiEnvironment(env);
        jndiObjectFactoryBean.setResourceRef(true);
        jndiObjectFactoryBean.setProxyInterface(ConnectionFactory.class);
        return jndiObjectFactoryBean;
    }


}
