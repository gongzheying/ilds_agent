package org.iata.ilds.agent.spring.integration;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.Proxy;
import com.jcraft.jsch.ProxyHTTP;
import com.jcraft.jsch.ProxySOCKS5;
import lombok.extern.log4j.Log4j2;
import org.iata.ilds.agent.config.outbound.OutboundFlowConfig;
import org.iata.ilds.agent.domain.entity.DestinationType;
import org.iata.ilds.agent.domain.entity.TransferSite;
import org.iata.ilds.agent.domain.entity.TransferSiteCredentialType;
import org.iata.ilds.agent.spring.data.TransferSiteRepository;
import org.iata.ilds.agent.util.AESUtility;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.integration.file.remote.session.SessionFactory;
import org.springframework.integration.file.remote.session.SessionFactoryLocator;
import org.springframework.integration.sftp.session.DefaultSftpSessionFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

@Log4j2
@Component
public class DBSessionFactoryLocator implements SessionFactoryLocator<ChannelSftp.LsEntry> {

    private final Map<Object, DefaultSftpSessionFactory> sessionFactoryMap = new ConcurrentHashMap<>();
    private final TransferSiteRepository transferSiteRepository;
    private final OutboundFlowConfig outboundFlowConfig;

    public DBSessionFactoryLocator(TransferSiteRepository transferSiteRepository, OutboundFlowConfig outboundFlowConfig) {
        this.transferSiteRepository = transferSiteRepository;
        this.outboundFlowConfig = outboundFlowConfig;
    }

    @Override
    public SessionFactory<ChannelSftp.LsEntry> getSessionFactory(Object key) {
        return sessionFactoryMap.computeIfAbsent(key, this::generateSessionFactory);
    }

    private DefaultSftpSessionFactory generateSessionFactory(Object key) {
        Optional<TransferSite> transferSiteOptional = transferSiteRepository.findById((Long) key);
        if (transferSiteOptional.isEmpty()) {
            log.error("Cannot find a TransferSite by key \"{}\"", key);
            return null;
        }

        TransferSite transferSite = transferSiteOptional.get();
        if (transferSite.getCredential() == null) {
            log.error("No login credentials are set for the TransferSite \"{}\"", transferSite.getId());
            return null;
        }

        DefaultSftpSessionFactory sftpSessionFactory = new DefaultSftpSessionFactory();
        sftpSessionFactory.setAllowUnknownKeys(true);
        sftpSessionFactory.setHost(transferSite.getIp());
        sftpSessionFactory.setPort(transferSite.getPort());
        sftpSessionFactory.setUser(transferSite.getUsername());

        if (DestinationType.Outbound.equals(transferSite.getDestinationType())) {
            Proxy proxy = getProxy();
            if (proxy != null) {
                sftpSessionFactory.setProxy(proxy);
                log.info("Using proxy \"{}://{}:{}\" for site {}@{}:{}:{}",
                        outboundFlowConfig.getProxy().getType(),
                        outboundFlowConfig.getProxy().getHost(),
                        outboundFlowConfig.getProxy().getPort(),
                        transferSite.getUsername(),
                        transferSite.getIp(),
                        transferSite.getPort(),
                        transferSite.getRemotePath());
            }
            Properties jschConfig = getJschConfig();
            sftpSessionFactory.setSessionConfig(jschConfig);
            log.info("Using jsch jschConfig \"{}\" for site {}@{}:{}:{}",
                    jschConfig,
                    transferSite.getUsername(),
                    transferSite.getIp(),
                    transferSite.getPort(),
                    transferSite.getRemotePath());

        }


        if (TransferSiteCredentialType.PasswordOnly.equals(transferSite.getCredential().getType())) {
            sftpSessionFactory.setPassword(AESUtility.decrypt(transferSite.getCredential().getPassword()));
        } else if (TransferSiteCredentialType.SSHKeyOnly.equals(transferSite.getCredential().getType())) {
            sftpSessionFactory.setPrivateKey(new ByteArrayResource(transferSite.getCredential().getPrivateKeyContent()));
        } else if (TransferSiteCredentialType.PasswordAndSSHKey.equals(transferSite.getCredential().getType())) {
            sftpSessionFactory.setPrivateKey(new ByteArrayResource(transferSite.getCredential().getPrivateKeyContent()));
            sftpSessionFactory.setPrivateKeyPassphrase(AESUtility.decrypt(transferSite.getCredential().getPrivateKeyPassphrase()));
        }

        log.info("Using credential(id={},type={}) for site {}@{}:{}:{}",
                transferSite.getCredential().getId(),
                transferSite.getCredentialType(),
                transferSite.getUsername(),
                transferSite.getIp(),
                transferSite.getPort(),
                transferSite.getRemotePath());

        return sftpSessionFactory;
    }

    private Proxy getProxy() {
        Proxy proxy = null;
        if (outboundFlowConfig.getProxy() != null) {
            switch (outboundFlowConfig.getProxy().getType()) {
                case SOCKS5 -> {
                    ProxySOCKS5 socks5proxy = new ProxySOCKS5(outboundFlowConfig.getProxy().getHost(), outboundFlowConfig.getProxy().getPort());
                    if (StringUtils.hasText(outboundFlowConfig.getProxy().getUser())) {
                        socks5proxy.setUserPasswd(outboundFlowConfig.getProxy().getUser(), outboundFlowConfig.getProxy().getPassword());
                    }
                    proxy = socks5proxy;
                }
                case HTTP -> {
                    ProxyHTTP httpProxy = new ProxyHTTP(outboundFlowConfig.getProxy().getHost(), outboundFlowConfig.getProxy().getPort());
                    if (StringUtils.hasText(outboundFlowConfig.getProxy().getUser())) {
                        httpProxy.setUserPasswd(outboundFlowConfig.getProxy().getUser(), outboundFlowConfig.getProxy().getPassword());
                    }
                    proxy = httpProxy;
                }

            }
        }
        return proxy;
    }

    private Properties getJschConfig() {
        Properties config = new Properties();
        if (outboundFlowConfig.getJsch() != null && outboundFlowConfig.getJsch().getConfig() != null) {
            config.putAll(outboundFlowConfig.getJsch().getConfig());
        }
        config.put("StrictHostKeyChecking", "no");
        return config;
    }

}
