# JBoss EAP 7.3 Setup

1. Add an ***application user*** to jboss, whose role belongs to guest

```
$ ~/jboss-eap-7.3/bin/add-user.sh  -a -u 'jmsuser' -p 'password1!' -g 'guest'
```

2. Add a connection-factory to the ***standalone configuration file*** as follows

```
$ ~/jboss-eap-7.3/bin/jboss-cli.sh   
You are disconnected at the moment. Type 'connect' to connect to the server or 'help' for the list of supported commands.
[disconnected /] ==connect==
[standalone@localhost:9990 /] ==/subsystem=messaging-activemq/server=default/connection-factory=RemoteHttpConnectionFactory/:add(connectors=["http-connector"],entries=["java:jboss/exported/jms/RemoteConnectionFactory"],consumer-window-size=0)==
```

# Database Setup

1. Running DDL scripts as follows to update the ILDS database

```mysql

CREATE TABLE `tbl_ilds_transfer_credentials`
(
    `id`                       bigint NOT NULL AUTO_INCREMENT,
    `created_at`               datetime     DEFAULT NULL,
    `last_modified_at`         datetime     DEFAULT NULL,
    `version`                  bigint NOT NULL,
    `private_key_name`         varchar(255) DEFAULT NULL,
    `private_key_passphrase`   varchar(255) DEFAULT NULL,
    `password`                 varchar(255) DEFAULT NULL,
    `title`                    varchar(255) DEFAULT NULL,
    `private_key_content`      blob,
    `private_key_content_type` varchar(255) DEFAULT NULL,
    `type`                     int          DEFAULT NULL,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB;

ALTER TABLE `tbl_ilds_transfer_site`
    ADD `new_flag` TINYINT(1) DEFAULT 0;

ALTER TABLE `tbl_ilds_transfer_site`
    ADD `credential_id` BIGINT;

```

# Application Setup

### Activemq Properties

| Name                                | Description                                                                         | Default Value                                          |
|-------------------------------------|-------------------------------------------------------------------------------------|--------------------------------------------------------|
| activemq.ctx.initialContextFactory  | Fully qualified class name of the factory class that will create an initial context | org.wildfly.naming.client.WildFlyInitialContextFactory |
| activemq.ctx.provideUrl             | URL string that the service provider to use                                         | http-remoting://127.0.0.1:8080                         |
| activemq.ctx.securityPrincipal      | Credentials of the principal for authenticating the caller to the service           | jmsuser                                                |
| activemq.ctx.securityCredentials    | Identity of the principal for authenticating the caller to the service              | password1!                                             |
| activemq.jndi.connectionFactory     | JNDI name that the ConnectionFactory is bound to                                    | jms/RemoteConnectionFactory                            |
| activemq.jndi.queueEventLog         | JNDI name that the EventLog queue is bound to                                       | queue/ilds/eventLog                                    |
| activemq.jndi.queueInboundDispatch  | JNDI name that the InboundDispatch queue is bound to                                | queue/ilds/inbound/inboundDispatchNew                  |
| activemq.jndi.queueOutboundDispatch | JNDI name that the OutboundDispatch queue is bound to                               | queue/ilds/outbound/outboundDispatchNew                |
| activemq.jndi.queueQuarantine       | JNDI name that the Quarantine queue is bound to                                     | queue/ilds/quarantine                                  |

### Data Properties

| Name                                       | Description                                                                | Default Value                      |
|--------------------------------------------|----------------------------------------------------------------------------|------------------------------------|
| spring.datasource.driver-class-name        | Fully qualified name of the JDBC driver                                    | com.mysql.cj.jdbc.Driver           |
| spring.datasource.url                      | JDBC URL of the database                                                   | jdbc:mysql://127.0.0.1:3306/ildsdb |
| spring.datasource.username                 | Login username of the database                                             | root                               |
| spring.datasource.password                 | Login password of the database                                             | 123456                             |
| spring.datasource.hikari.minimum-idle      | Min number of idle connections maintained by HikariCP in a connection pool | 5                                  |
| spring.datasource.hikari.maximum-pool-size | Max pool size                                                              | 30                                 |

### Retry Properties

| Name                  | Description                                                           | Default Value |
|-----------------------|-----------------------------------------------------------------------|---------------|
| retry.initialInterval | Initial period to sleep on the first backoff                          | 3000          |
| retry.multiplier      | Multiplier to use to generate the next backoff interval from the last | 2.0           |
| retry.maxInterval     | Max interval to sleep for                                             | 20000         |

### Outbound Properties

| Name                                 | Description                                          | Default Value |
|--------------------------------------|------------------------------------------------------|---------------|
| outbound.flow.concurrentConsumers    | Concurrent consumers number to use                   | 20            |
| outbound.flow.maxConcurrentConsumers | Max for concurrent consumers number to use           | 20            |
| outbound.flow.maxMessagesPerTask     | Max messages per task                                | 10            |
| outbound.flow.proxy.type             | the proxie's type, only support HTTP or SOCKS5       |               |
| outbound.flow.proxy.host             | the proxie's host name                               |               |
| outbound.flow.proxy.port             | the proxie's port                                    |               |
| outbound.flow.proxy.user             | the user name needed for authentication to the proxy |               |
| outbound.flow.proxy.password         | the password needed for authentication to the proxy  |               |
| outbound.flow.jsch.config            | see also com.jcraft.jsch.JSch.config                 |               |

### Inbound Properties

| Name                           | Description                          | Default Value                                         |
|--------------------------------|--------------------------------------|-------------------------------------------------------|
| inbound.hostingSystemInfo.path | File URL of hosting-system-info.json | file:/etc/ilds/configuration/hosting-system-info.json |
