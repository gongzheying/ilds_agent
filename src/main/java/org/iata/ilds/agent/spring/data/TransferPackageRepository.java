package org.iata.ilds.agent.spring.data;

import org.iata.ilds.agent.domain.entity.TransferPackage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransferPackageRepository extends JpaRepository<TransferPackage, Long> {

    TransferPackage findByPackageName(String trackingId);

}
