package org.iata.ilds.agent.spring.data;

import org.iata.ilds.agent.domain.entity.TransferSite;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TransferSiteRepository extends JpaRepository<TransferSite, Long> {

    Optional<TransferSite> findByUsernameAndIpAndPortAndRemotePath(String username, String ip, int port, String remotePath);
}
