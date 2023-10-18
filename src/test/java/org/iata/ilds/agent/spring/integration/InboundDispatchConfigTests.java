package org.iata.ilds.agent.spring.integration;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.FileUtils;
import org.awaitility.Awaitility;
import org.iata.ilds.agent.config.activemq.ActivemqConfigProperties;
import org.iata.ilds.agent.domain.entity.FileType;
import org.iata.ilds.agent.domain.entity.TransferFile;
import org.iata.ilds.agent.domain.entity.TransferPackage;
import org.iata.ilds.agent.domain.entity.TransferStatus;
import org.iata.ilds.agent.domain.message.inbound.InboundDispatchMessage;
import org.iata.ilds.agent.spring.data.TransferPackageRepository;
import org.iata.ilds.agent.util.FileTrackingUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import javax.jms.ConnectionFactory;
import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.stream.Stream;

@Log4j2
@SpringBootTest
@ActiveProfiles(value = "test")
public class InboundDispatchConfigTests {

    @Autowired
    private TransferPackageRepository transferPackageRepository;

    @Autowired
    private ObjectMapper objectMapper;


    @Autowired
    private ConnectionFactory connectionFactory;

    @Autowired
    private ActivemqConfigProperties config;

    @Transactional
    public void prepareTransferPackageForTesting(TransferPackage transferPackage) {

        if (transferPackageRepository.existsById(transferPackage.getId())) {
            transferPackageRepository.deleteById(transferPackage.getId());
        }
        transferPackageRepository.save(transferPackage);

    }

    @ParameterizedTest
    @MethodSource
    public void testInboundDispatchFlow(TransferStatus expectedStatus,
                                        InboundDispatchMessage inboundDispatchMessage,
                                        TransferPackage transferPackage) {

        prepareTransferPackageForTesting(transferPackage);

        try {
            JmsTemplate jmsTemplate = new JmsTemplate(connectionFactory);
            jmsTemplate.convertAndSend(config.getJndi().getQueueInboundDispatch(), objectMapper.writeValueAsString(inboundDispatchMessage));
        } catch (JsonProcessingException e) {
            Assertions.fail("JSON serialization failed", e);
        }

        Awaitility.await().atMost(Duration.ofSeconds(1 * 60)).until(() ->
                expectedStatus.equals(
                        transferPackageRepository.findByPackageName(transferPackage.getPackageName()).get().getStatus()
                ));

    }

    private static Stream<Arguments> testInboundDispatchFlow() {

        TransferPackage transferPackage = createTransferPackage();
        InboundDispatchMessage dispatchMessage = createInboundDispatchMessage(transferPackage);

        TransferPackage transferPackage2 = createTransferPackage();
        InboundDispatchMessage dispatchMessage2 = createWrongInboundDispatchMessage(transferPackage2);

        return Stream.of(
                Arguments.of(TransferStatus.Sent, dispatchMessage, transferPackage),
                Arguments.of(TransferStatus.Failed, dispatchMessage2, transferPackage2)
        );

    }


    private static InboundDispatchMessage createInboundDispatchMessage(TransferPackage transferPackage) {
        InboundDispatchMessage dispatchMessage = new InboundDispatchMessage();
        dispatchMessage.setProcessingStartTime(System.currentTimeMillis());
        dispatchMessage.setTrackingId(transferPackage.getPackageName());
        dispatchMessage.setLocalFilePath(transferPackage.getLocalFilePath());
        dispatchMessage.setOriginalFilePath(transferPackage.getOriginalFilePath());
        dispatchMessage.setDestination("isis");


        return dispatchMessage;
    }

    private static InboundDispatchMessage createWrongInboundDispatchMessage(TransferPackage transferPackage) {
        InboundDispatchMessage dispatchMessage = new InboundDispatchMessage();
        dispatchMessage.setProcessingStartTime(System.currentTimeMillis());
        dispatchMessage.setTrackingId(transferPackage.getPackageName());
        dispatchMessage.setLocalFilePath(transferPackage.getLocalFilePath());
        dispatchMessage.setOriginalFilePath(transferPackage.getOriginalFilePath());
        dispatchMessage.setDestination("wrong-isis");

        return dispatchMessage;
    }

    private static TransferPackage createTransferPackage() {
        String trackingId = FileTrackingUtils.generateTrackingId(true);
        TransferPackage transferPackage = new TransferPackage();
        transferPackage.setId(1L);
        transferPackage.setPackageName(trackingId);
        transferPackage.setTransferFiles(new ArrayList<>());
        transferPackage.setStatus(TransferStatus.Processing);
        transferPackage.setLocalFilePath("target/test-files/file0.txt");
        transferPackage.setOriginalFilePath("/upload/file0.txt");

        TransferFile transferFile = new TransferFile();
        transferFile.setFileName("file0.txt");
        transferFile.setFileType(FileType.Normal);
        transferFile.setStatus(TransferStatus.Processing);
        try {
            createTestFile(transferPackage.getLocalFilePath(), transferFile.getFileName());
            transferFile.setTransferPackage(transferPackage);
            transferPackage.getTransferFiles().add(transferFile);
        } catch (IOException e) {
            log.error("Failed : {}", e.getMessage());
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
