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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.test.context.ActiveProfiles;

import javax.jms.ConnectionFactory;
import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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

    //@Test
    public void testInvalidOutboundDispatchFlow() {

        TransferPackage transferPackage = createTransferPackage(1);
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


    @Test
    public void testOutboundDispatchFlow() throws JsonProcessingException {
        int capacity = 100;
        List<TransferPackage> transferPackages = new ArrayList<>(capacity);
        List<String> dispatchMessages = new ArrayList<>(capacity);
        for (long i = 0; i < capacity; i++) {
            TransferPackage transferPackage = createTransferPackage(i);
            String dispatchMessage;
            if (i % 2 == 0) {
                dispatchMessage = objectMapper.writeValueAsString(createOutboundDispatchMessageByPassword(transferPackage));
            } else {
                dispatchMessage = objectMapper.writeValueAsString(createOutboundDispatchMessageByKeyAndPassphrase(transferPackage));
            }

            transferPackages.add(transferPackage);
            dispatchMessages.add(dispatchMessage);
        }

        transferPackages.forEach(this::prepareTransferPackageForTesting);

        JmsTemplate jmsTemplate = new JmsTemplate(connectionFactory);
        dispatchMessages.forEach(dispatchMessage -> {
            jmsTemplate.convertAndSend(config.getJndi().getQueueOutboundDispatch(), dispatchMessage);
        });

        log.info("{} outboundDispatchMessage have been sent", capacity);


        Awaitility.await().pollInterval(Duration.ofSeconds(5)).atMost(Duration.ofSeconds(3 * 60)).until(() -> {

            List<Long> packageIds = transferPackages.stream().map(TransferPackage::getId).collect(Collectors.toList());
            return transferPackageRepository.findAllById(packageIds).stream().allMatch(transferPackage -> TransferStatus.Sent.equals(transferPackage.getStatus()));

        });

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


    private static TransferPackage createTransferPackage(long packageId) {
        String trackingId = FileTrackingUtils.generateTrackingId(false);
        TransferPackage transferPackage = new TransferPackage();
        transferPackage.setId(packageId);
        transferPackage.setPackageName(trackingId);
        transferPackage.setTransferFiles(new ArrayList<>());
        transferPackage.setStatus(TransferStatus.Processing);
        transferPackage.setLocalFilePath(String.format("target/test-files-%d", packageId));
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
