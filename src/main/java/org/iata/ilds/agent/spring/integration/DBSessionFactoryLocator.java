package org.iata.ilds.agent.spring.integration;

import com.jcraft.jsch.ChannelSftp;
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
             if (outboundFlowConfig.getProxy() != null) {
                 ProxySOCKS5 socks5proxy = new ProxySOCKS5(outboundFlowConfig.getProxy().getHost(), outboundFlowConfig.getProxy().getPort());
                 socks5proxy.setUserPasswd(outboundFlowConfig.getProxy().getUser(), outboundFlowConfig.getProxy().getPassword());
                 sftpSessionFactory.setProxy(socks5proxy);
             }
        }

        Properties config = new Properties();
        config.put("PubkeyAcceptedAlgorithms", "ecdsa-sha2-nistp256,ecdsa-sha2-nistp384,ecdsa-sha2-nistp521,rsa-sha2-512,rsa-sha2-256");
        config.put("StrictHostKeyChecking", "no");
        sftpSessionFactory.setSessionConfig(config);

        if (TransferSiteCredentialType.PasswordOnly.equals(transferSite.getCredential().getType())) {
            sftpSessionFactory.setPassword(AESUtility.decrypt(transferSite.getCredential().getPassword()));
        } else if (TransferSiteCredentialType.SSHKeyOnly.equals(transferSite.getCredential().getType())) {
            sftpSessionFactory.setPrivateKey(new ByteArrayResource(transferSite.getCredential().getPrivateKeyContent()));
        } else if (TransferSiteCredentialType.PasswordAndSSHKey.equals(transferSite.getCredential().getType())) {
            sftpSessionFactory.setPrivateKey(new ByteArrayResource(transferSite.getCredential().getPrivateKeyContent()));
            sftpSessionFactory.setPrivateKeyPassphrase(AESUtility.decrypt(transferSite.getCredential().getPrivateKeyPassphrase()));
        }

        log.info("Using the credential(id={},type={}) for site {}@{}:{}:{}",
                transferSite.getCredential().getId(),
                transferSite.getCredentialType(),
                transferSite.getUsername(),
                transferSite.getIp(),
                transferSite.getPort(),
                transferSite.getRemotePath());

        return sftpSessionFactory;
    }

}
