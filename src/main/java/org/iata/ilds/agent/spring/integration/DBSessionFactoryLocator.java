package org.iata.ilds.agent.spring.integration;

import com.jcraft.jsch.ChannelSftp;
import lombok.extern.log4j.Log4j2;
import org.iata.ilds.agent.domain.entity.TransferSite;
import org.iata.ilds.agent.domain.entity.TransferSiteCredentialType;
import org.iata.ilds.agent.spring.data.TransferSiteRepository;
import org.springframework.core.io.FileSystemResource;
import org.springframework.integration.file.remote.session.SessionFactory;
import org.springframework.integration.file.remote.session.SessionFactoryLocator;
import org.springframework.integration.sftp.session.DefaultSftpSessionFactory;
import org.springframework.stereotype.Component;
import org.wildfly.security.auth.realm.AggregateSecurityRealm;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Log4j2
@Component
public class DBSessionFactoryLocator implements SessionFactoryLocator<ChannelSftp.LsEntry> {

    private final Map<Object, DefaultSftpSessionFactory> sessionFactoryMap  = new ConcurrentHashMap<>();
    private final TransferSiteRepository transferSiteRepository;

    public DBSessionFactoryLocator(TransferSiteRepository transferSiteRepository) {
        this.transferSiteRepository = transferSiteRepository;
    }

    @Override
    public SessionFactory<ChannelSftp.LsEntry> getSessionFactory(Object key) {
        return sessionFactoryMap.computeIfAbsent(key, this::generateSessionFactory);
    }

    private DefaultSftpSessionFactory generateSessionFactory(Object key){
        Optional<TransferSite> transferSiteOptional = transferSiteRepository.findById((Long)key);
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
        sftpSessionFactory.setHost(transferSite.getIp());
        sftpSessionFactory.setPort(transferSite.getPort());
        sftpSessionFactory.setUser(transferSite.getUsername());

        if (TransferSiteCredentialType.PasswordOnly.equals(transferSite.getCredentialType())) {
            sftpSessionFactory.setPassword(transferSite.getCredential().getPassword());
        } else if (TransferSiteCredentialType.SSHKeyOnly.equals(transferSite.getCredentialType()) ){
            sftpSessionFactory.setPrivateKey(new FileSystemResource(transferSite.getCredential().getPrivateKeyName()));
        }
        else if (TransferSiteCredentialType.PasswordAndSSHKey.equals(transferSite.getCredentialType())) {
            sftpSessionFactory.setPrivateKey(new FileSystemResource(transferSite.getCredential().getPrivateKeyName()));
            sftpSessionFactory.setPrivateKeyPassphrase(transferSite.getCredential().getPrivateKeyPassphrase());
        }

        return sftpSessionFactory;
    }

}
