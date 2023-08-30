package org.iata.ilds.agent.spring.data;

import org.iata.ilds.agent.domain.entity.TransferSite;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransferSiteRepository extends JpaRepository<TransferSite, Long> {

    TransferSite findByUsernameAndIpAndPortAndRemotePath(String username, String ip, int port, String remotePath);
}
