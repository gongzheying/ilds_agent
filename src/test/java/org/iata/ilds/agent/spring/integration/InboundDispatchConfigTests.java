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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.test.context.ActiveProfiles;

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

    private void prepareTransferPackageForTesting(TransferPackage transferPackage) {

        if (transferPackageRepository.existsById(transferPackage.getId())) {
            transferPackageRepository.deleteById(transferPackage.getId());
        }
        transferPackageRepository.save(transferPackage);

    }

    @Test
    public void testInvalidInboundDispatchFlow() {

        TransferPackage transferPackage = createTransferPackage();
        InboundDispatchMessage inboundDispatchMessage = createInvaildInboundDispatchMessage(transferPackage);
        prepareTransferPackageForTesting(transferPackage);
        try {
            JmsTemplate jmsTemplate = new JmsTemplate(connectionFactory);
            jmsTemplate.convertAndSend(config.getJndi().getQueueInboundDispatch(), objectMapper.writeValueAsString(inboundDispatchMessage));
        } catch (JsonProcessingException e) {
            Assertions.fail("JSON serialization failed", e);
        }

        Awaitility.await().timeout(Duration.ofSeconds(60)).pollDelay(Duration.ofSeconds(30)).until(() ->
                TransferStatus.Processing.equals(
                        transferPackageRepository.findByPackageName(transferPackage.getPackageName()).get().getStatus()
                ));


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

        Awaitility.await().atMost(Duration.ofSeconds(3 * 60)).until(() ->
                expectedStatus.equals(
                        transferPackageRepository.findByPackageName(transferPackage.getPackageName()).get().getStatus()
                ));

    }

    private static Stream<Arguments> testInboundDispatchFlow() {

        TransferPackage transferPackage = createTransferPackage();
        InboundDispatchMessage dispatchMessage = createInboundDispatchMessageByPassword(transferPackage);

        TransferPackage transferPackage2 = createTransferPackage();
        InboundDispatchMessage dispatchMessage2 = createInboundDispatchMessageByKeyAndPassphrase(transferPackage2);

        return Stream.of(
                Arguments.of(TransferStatus.Sent, dispatchMessage, transferPackage),
                Arguments.of(TransferStatus.Sent, dispatchMessage2, transferPackage2)
        );

    }


    private static InboundDispatchMessage createInboundDispatchMessageByPassword(TransferPackage transferPackage) {
        InboundDispatchMessage dispatchMessage = new InboundDispatchMessage();
        dispatchMessage.setProcessingStartTime(System.currentTimeMillis());
        dispatchMessage.setTrackingId(transferPackage.getPackageName());
        dispatchMessage.setLocalFilePath(transferPackage.getLocalFilePath());
        dispatchMessage.setOriginalFilePath(transferPackage.getOriginalFilePath());
        dispatchMessage.setDestination("isis3");


        return dispatchMessage;
    }

    private static InboundDispatchMessage createInboundDispatchMessageByKeyAndPassphrase(TransferPackage transferPackage) {
        InboundDispatchMessage dispatchMessage = new InboundDispatchMessage();
        dispatchMessage.setProcessingStartTime(System.currentTimeMillis());
        dispatchMessage.setTrackingId(transferPackage.getPackageName());
        dispatchMessage.setLocalFilePath(transferPackage.getLocalFilePath());
        dispatchMessage.setOriginalFilePath(transferPackage.getOriginalFilePath());
        dispatchMessage.setDestination("isis4");

        return dispatchMessage;
    }

    private static InboundDispatchMessage createInvaildInboundDispatchMessage(TransferPackage transferPackage) {
        InboundDispatchMessage dispatchMessage = new InboundDispatchMessage();
        dispatchMessage.setProcessingStartTime(System.currentTimeMillis());
        dispatchMessage.setTrackingId(transferPackage.getPackageName());
        dispatchMessage.setLocalFilePath(transferPackage.getLocalFilePath());
        dispatchMessage.setOriginalFilePath(transferPackage.getOriginalFilePath());
        dispatchMessage.setDestination("isis5");

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
            createTestFile(transferPackage.getLocalFilePath());
            transferFile.setTransferPackage(transferPackage);
            transferPackage.getTransferFiles().add(transferFile);
        } catch (IOException e) {
            log.error("Failed : {}", e.getMessage());
        }
        return transferPackage;
    }

    private static void createTestFile(String localFilePath) throws IOException {
        File file = new File(localFilePath);
        if (!file.getParentFile().exists()) {
            FileUtils.forceMkdir(file.getParentFile());
        }

        FileUtils.writeStringToFile(file, "It works", "utf-8");

    }

}
