package org.iata.ilds.agent.spring.integration;

import com.jcraft.jsch.ChannelSftp;
import lombok.extern.log4j.Log4j2;
import org.iata.ilds.agent.domain.entity.TransferSite;
import org.iata.ilds.agent.spring.data.TransferSiteRepository;
import org.springframework.core.io.FileSystemResource;
import org.springframework.integration.file.remote.session.SessionFactory;
import org.springframework.integration.file.remote.session.SessionFactoryLocator;
import org.springframework.integration.sftp.session.DefaultSftpSessionFactory;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Log4j2
@Component
public class TransferSiteSessionFactoryLocator implements SessionFactoryLocator<ChannelSftp.LsEntry> {

    private final Map<Object, DefaultSftpSessionFactory> sessionFactoryMap  = new ConcurrentHashMap<>();
    private final TransferSiteRepository transferSiteRepository;

    public TransferSiteSessionFactoryLocator(TransferSiteRepository transferSiteRepository) {
        this.transferSiteRepository = transferSiteRepository;
    }

    @Override
    public SessionFactory<ChannelSftp.LsEntry> getSessionFactory(Object key) {
        return sessionFactoryMap.computeIfAbsent(key, this::generateSessionFactory);
    }

    private DefaultSftpSessionFactory generateSessionFactory(Object key){
        TransferSite transferSite = null;
        if (transferSite == null) {
            log.warn("Cannot find a TransferSite by key {}", key);
            return null;
        }

        DefaultSftpSessionFactory sftpSessionFactory = new DefaultSftpSessionFactory();
        sftpSessionFactory.setHost(transferSite.getIp());
        sftpSessionFactory.setPort(transferSite.getPort());
        sftpSessionFactory.setUser(transferSite.getUsername());
        sftpSessionFactory.setPassword(transferSite.getDispatcher().getDispatcherPassword());
        sftpSessionFactory.setPrivateKey(new FileSystemResource(transferSite.getDispatcher().getDispatcherKeyName()));
        sftpSessionFactory.setPrivateKeyPassphrase(transferSite.getDispatcher().getDispatcherKeyPassphrase());
        return sftpSessionFactory;
    }

}
