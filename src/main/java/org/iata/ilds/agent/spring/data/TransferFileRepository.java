package org.iata.ilds.agent.spring.data;

import org.iata.ilds.agent.domain.entity.TransferFile;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransferFileRepository  extends JpaRepository<TransferFile, Long> {
}
