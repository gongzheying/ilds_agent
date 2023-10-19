package org.iata.ilds.agent.spring.integration;

import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.FileUtils;
import org.awaitility.Awaitility;
import org.iata.ilds.agent.domain.entity.FileType;
import org.iata.ilds.agent.domain.entity.TransferFile;
import org.iata.ilds.agent.domain.entity.TransferPackage;
import org.iata.ilds.agent.domain.entity.TransferStatus;
import org.iata.ilds.agent.domain.message.DispatchCompletedMessage;
import org.iata.ilds.agent.exception.DispatchException;
import org.iata.ilds.agent.spring.data.TransferPackageRepository;
import org.iata.ilds.agent.util.FileTrackingUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.ErrorMessage;
import org.springframework.test.context.ActiveProfiles;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;

@Log4j2
@SpringBootTest
@ActiveProfiles(value = "test")
public class BasicFlowConfigTests {

    @Autowired
    private TransferPackageRepository transferPackageRepository;


    @Autowired
    private MessageChannel errorChannel;

    private void prepareTransferPackageForTesting(TransferPackage transferPackage) {

        if (transferPackageRepository.existsById(transferPackage.getId())) {
            transferPackageRepository.deleteById(transferPackage.getId());
        }
        transferPackageRepository.save(transferPackage);

    }


    @Test
    public void testDispatchExceptionFlow() {

        TransferPackage transferPackage = createTransferPackage();

        prepareTransferPackageForTesting(transferPackage);

        DispatchCompletedMessage dispatchCompletedMessage = new DispatchCompletedMessage();
        dispatchCompletedMessage.setProcessingStartTime(System.currentTimeMillis());
        dispatchCompletedMessage.setTrackingId(transferPackage.getPackageName());
        dispatchCompletedMessage.setSuccessful(false);

        dispatchCompletedMessage.setProcessedDataFilePaths(transferPackage.getTransferFiles().stream()
                .filter(f -> FileType.Normal.equals(f.getFileType()))
                .map(TransferFile::getFileName)
                .toList());

        dispatchCompletedMessage.setFailedDataFilePath(transferPackage.getTransferFiles().stream()
                .filter(f -> FileType.TDF.equals(f.getFileType()))
                .map(TransferFile::getFileName)
                .findFirst().orElse(null));


        Message<DispatchCompletedMessage> message = MessageBuilder.withPayload(dispatchCompletedMessage).build();
        DispatchException dispatchException = new DispatchException(message, new RuntimeException("Custom Error!"));

        errorChannel.send(new ErrorMessage(dispatchException));

        Awaitility.await().atMost(Duration.ofSeconds(30)).until(() ->
                TransferStatus.Failed.equals(
                        transferPackageRepository.findByPackageName(transferPackage.getPackageName()).get().getStatus()
                ));

    }


    private static TransferPackage createTransferPackage() {
        String trackingId = FileTrackingUtils.generateTrackingId(false);
        TransferPackage transferPackage = new TransferPackage();
        transferPackage.setId(1L);
        transferPackage.setPackageName(trackingId);
        transferPackage.setTransferFiles(new ArrayList<>());
        transferPackage.setStatus(TransferStatus.Processing);
        transferPackage.setLocalFilePath("target/test-files");
        for (long id = 1; id <= 3; id++) {
            TransferFile transferFile = new TransferFile();
            transferFile.setFileName(String.format("file%d.%s", id, id == 3 ? "tdf" : "txt"));
            transferFile.setFileType(id == 3 ? FileType.TDF : FileType.Normal);
            transferFile.setStatus(TransferStatus.Processing);
            try {
                createTestFile(transferPackage.getLocalFilePath(), transferFile.getFileName());
                transferFile.setTransferPackage(transferPackage);
                transferPackage.getTransferFiles().add(transferFile);
            } catch (IOException e) {
                log.error("Failed : {}", e.getMessage());
            }
        }
        return transferPackage;
    }

    private static void createTestFile(String localFilePath, String filename) throws IOException {
        File dir = new File(localFilePath);
        if (!dir.exists()) {
            FileUtils.forceMkdir(dir);
        }
        File file = new File(dir, filename);
        FileUtils.writeStringToFile(file, "It works", "utf-8");

    }
}
