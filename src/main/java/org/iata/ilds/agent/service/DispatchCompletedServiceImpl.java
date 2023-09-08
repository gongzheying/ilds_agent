package org.iata.ilds.agent.service;


import org.iata.ilds.agent.domain.entity.TransferPackage;
import org.iata.ilds.agent.domain.entity.TransferStatus;
import org.iata.ilds.agent.domain.message.DispatchCompletedMessage;
import org.iata.ilds.agent.spring.data.TransferPackageRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

@Service
public class DispatchCompletedServiceImpl implements DispatchCompletedService {

    private final TransferPackageRepository transferPackageRepository;

    public DispatchCompletedServiceImpl(TransferPackageRepository transferPackageRepository) {
        this.transferPackageRepository = transferPackageRepository;
    }

    @Override
    @Transactional
    public void setCompletionStatus(DispatchCompletedMessage message, String... fileWithErrors) {

        Optional<TransferPackage> transferPackageOptional = transferPackageRepository.findByPackageName(message.getTrackingId());
        if (transferPackageOptional.isPresent()) {
            TransferPackage transferPackage = transferPackageOptional.get();
            if (message.isSuccessful()) {

                Optional.ofNullable(transferPackage.getTransferFiles())
                        .orElseGet(Collections::emptyList)
                        .forEach(transferFile -> transferFile.setStatus(TransferStatus.Sent));

                transferPackage.setStatus(TransferStatus.Sent);

            } else {

                Optional.ofNullable(transferPackage.getTransferFiles())
                        .orElseGet(Collections::emptyList)
                        .stream()
                        .filter(file -> message.getLocalFilePath().contains(file.getFileName()))
                        .forEach(file -> file.setStatus(TransferStatus.Sent));

                Optional.ofNullable(transferPackage.getTransferFiles())
                        .orElseGet(Collections::emptyList)
                        .stream()
                        .filter(file ->  Arrays.stream(fileWithErrors).anyMatch(item -> item.equals(file.getFileName())) )
                        .forEach(file -> file.setStatus(TransferStatus.Failed));


            }

            transferPackageRepository.save(transferPackage);
        }
    }
}
