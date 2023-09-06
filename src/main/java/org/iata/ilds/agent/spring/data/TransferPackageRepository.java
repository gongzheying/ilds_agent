package org.iata.ilds.agent.spring.data;

import org.iata.ilds.agent.domain.entity.TransferPackage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TransferPackageRepository extends JpaRepository<TransferPackage, Long> {

    Optional<TransferPackage> findByPackageName(String trackingId);

}
