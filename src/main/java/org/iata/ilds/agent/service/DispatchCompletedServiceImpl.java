package org.iata.ilds.agent.service;


import org.apache.commons.io.FilenameUtils;
import org.iata.ilds.agent.domain.entity.TransferPackage;
import org.iata.ilds.agent.domain.entity.TransferStatus;
import org.iata.ilds.agent.domain.message.DispatchCompletedMessage;
import org.iata.ilds.agent.spring.data.TransferPackageRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class DispatchCompletedServiceImpl implements DispatchCompletedService {

    private final TransferPackageRepository transferPackageRepository;

    public DispatchCompletedServiceImpl(TransferPackageRepository transferPackageRepository) {
        this.transferPackageRepository = transferPackageRepository;
    }

    @Override
    @Transactional
    public void setCompletionStatus(DispatchCompletedMessage message) {

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
                        .filter(file -> message.getProcessedLocalFilePaths().stream().map(FilenameUtils::getName).toList().contains(file.getFileName()))
                        .forEach(file -> file.setStatus(TransferStatus.Sent));

                Optional.ofNullable(transferPackage.getTransferFiles())
                        .orElseGet(Collections::emptyList)
                        .stream()
                        .filter(file -> FilenameUtils.getName(message.getLocalFilePath()).equals(file.getFileName()))
                        .forEach(file -> file.setStatus(TransferStatus.Failed));

                transferPackage.setStatus(TransferStatus.Failed);
            }

            transferPackageRepository.save(transferPackage);
        }
    }
}
