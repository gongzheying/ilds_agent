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
import org.iata.ilds.agent.domain.message.outbound.Address;
import org.iata.ilds.agent.domain.message.outbound.Channel;
import org.iata.ilds.agent.domain.message.outbound.OutboundDispatchMessage;
import org.iata.ilds.agent.domain.message.outbound.RoutingFileInfo;
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
public class OutboundDispatchConfigTests {

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
    public void testInvalidOutboundDispatchFlow() {

        TransferPackage transferPackage = createTransferPackage();
        OutboundDispatchMessage outboundDispatchMessage = createInvalidOutboundDispatchMessage(transferPackage);
        prepareTransferPackageForTesting(transferPackage);
        try {
            JmsTemplate jmsTemplate = new JmsTemplate(connectionFactory);
            jmsTemplate.convertAndSend(config.getJndi().getQueueOutboundDispatch(), objectMapper.writeValueAsString(outboundDispatchMessage));
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
    public void testOutboundDispatchFlow(TransferStatus expectedStatus,
                                         OutboundDispatchMessage outboundDispatchMessage,
                                         TransferPackage transferPackage) {

        prepareTransferPackageForTesting(transferPackage);

        try {
            JmsTemplate jmsTemplate = new JmsTemplate(connectionFactory);
            jmsTemplate.convertAndSend(config.getJndi().getQueueOutboundDispatch(), objectMapper.writeValueAsString(outboundDispatchMessage));
            log.info("An outboundDispatchMessage has been sent");
        } catch (JsonProcessingException e) {
            Assertions.fail("JSON serialization failed", e);
        }

        Awaitility.await().atMost(Duration.ofSeconds(3 * 60)).until(() ->
                expectedStatus.equals(
                        transferPackageRepository.findByPackageName(transferPackage.getPackageName()).get().getStatus()
                ));

    }

    private static Stream<Arguments> testOutboundDispatchFlow() {

        TransferPackage transferPackage = createTransferPackage();
        OutboundDispatchMessage dispatchMessage = createOutboundDispatchMessageByPassword(transferPackage);

        TransferPackage transferPackage2 = createTransferPackage();
        OutboundDispatchMessage dispatchMessage2 = createOutboundDispatchMessageByKeyAndPassphrase(transferPackage2);

        return Stream.of(
                Arguments.of(TransferStatus.Sent, dispatchMessage, transferPackage),
                Arguments.of(TransferStatus.Sent, dispatchMessage2, transferPackage2)
        );

    }


    private static OutboundDispatchMessage createOutboundDispatchMessageByPassword(TransferPackage transferPackage) {
        OutboundDispatchMessage dispatchMessage = new OutboundDispatchMessage();
        dispatchMessage.setProcessingStartTime(System.currentTimeMillis());
        dispatchMessage.setTrackingId(transferPackage.getPackageName());
        dispatchMessage.setLocalFilePath(transferPackage.getLocalFilePath());

        RoutingFileInfo routingFileInfo = new RoutingFileInfo();
        Channel channel = new Channel();
        Address address = new Address();
        address.setIp("172.18.0.3");
        address.setPort(22);
        address.setPath("/upload");
        address.setUser("foo");
        channel.setAddress(address);
        routingFileInfo.setChannel(channel);

        dispatchMessage.setRoutingFileInfo(routingFileInfo);
        return dispatchMessage;
    }

    private static OutboundDispatchMessage createOutboundDispatchMessageByKeyAndPassphrase(TransferPackage transferPackage) {
        OutboundDispatchMessage dispatchMessage = new OutboundDispatchMessage();
        dispatchMessage.setProcessingStartTime(System.currentTimeMillis());
        dispatchMessage.setTrackingId(transferPackage.getPackageName());
        dispatchMessage.setLocalFilePath(transferPackage.getLocalFilePath());

        RoutingFileInfo routingFileInfo = new RoutingFileInfo();
        Channel channel = new Channel();
        Address address = new Address();
        address.setIp("172.18.0.4");
        address.setPort(22);
        address.setPath("/upload");
        address.setUser("foo");
        channel.setAddress(address);
        routingFileInfo.setChannel(channel);

        dispatchMessage.setRoutingFileInfo(routingFileInfo);
        return dispatchMessage;
    }

    private static OutboundDispatchMessage createInvalidOutboundDispatchMessage(TransferPackage transferPackage) {
        OutboundDispatchMessage dispatchMessage = new OutboundDispatchMessage();
        dispatchMessage.setProcessingStartTime(System.currentTimeMillis());
        dispatchMessage.setTrackingId(transferPackage.getPackageName());
        dispatchMessage.setLocalFilePath(transferPackage.getLocalFilePath());

        RoutingFileInfo routingFileInfo = new RoutingFileInfo();
        Channel channel = new Channel();
        Address address = new Address();
        address.setIp("172.18.0.5");
        address.setPort(22);
        address.setPath("/upload");
        address.setUser("foo");
        channel.setAddress(address);
        routingFileInfo.setChannel(channel);

        dispatchMessage.setRoutingFileInfo(routingFileInfo);
        return dispatchMessage;
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
