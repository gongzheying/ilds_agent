package org.iata.ilds.agent.spring.integration;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.FileUtils;
import org.awaitility.Awaitility;
import org.iata.ilds.agent.activemq.ActivemqConfigProperties;
import org.iata.ilds.agent.domain.entity.FileType;
import org.iata.ilds.agent.domain.entity.TransferFile;
import org.iata.ilds.agent.domain.entity.TransferPackage;
import org.iata.ilds.agent.domain.entity.TransferStatus;
import org.iata.ilds.agent.domain.message.DispatchCompletedMessage;
import org.iata.ilds.agent.domain.message.outbound.Address;
import org.iata.ilds.agent.domain.message.outbound.Channel;
import org.iata.ilds.agent.domain.message.outbound.OutboundDispatchMessage;
import org.iata.ilds.agent.domain.message.outbound.RoutingFileInfo;
import org.iata.ilds.agent.exception.OutboundDispatchException;
import org.iata.ilds.agent.spring.data.TransferPackageRepository;
import org.iata.ilds.agent.util.FileTrackingUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandlingException;
import org.springframework.messaging.support.ErrorMessage;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import javax.jms.ConnectionFactory;
import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.*;
import java.util.stream.Stream;

@Log4j2
@SpringBootTest
@ActiveProfiles(value = "test")
public class OutboundDispatchConfigTests {

    @Autowired
    private TransferPackageRepository transferPackageRepository;


    @Autowired
    private MessageChannel errorChannel;

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
    public void testOutboundDispatchExceptionFlow(TransferPackage transferPackage) {

        prepareTransferPackageForTesting(transferPackage);

        DispatchCompletedMessage dispatchCompletedMessage = new DispatchCompletedMessage();
        dispatchCompletedMessage.setProcessingStartTime((int) System.currentTimeMillis());
        dispatchCompletedMessage.setTrackingId(transferPackage.getPackageName());
        dispatchCompletedMessage.setSuccessful(false);

        dispatchCompletedMessage.setProcessedLocalFilePaths(transferPackage.getTransferFiles().stream()
                .filter(f -> FileType.Normal.equals(f.getFileType()))
                .map(TransferFile::getFileName)
                .toList());

        dispatchCompletedMessage.setLocalFilePath(transferPackage.getTransferFiles().stream()
                .filter(f -> FileType.TDF.equals(f.getFileType()))
                .map(TransferFile::getFileName)
                .findFirst().orElse(null));



        Message<DispatchCompletedMessage> message = MessageBuilder.withPayload(dispatchCompletedMessage).build();
        MessageHandlingException e = new MessageHandlingException(message,new OutboundDispatchException("Custom Error!", dispatchCompletedMessage));

        errorChannel.send(new ErrorMessage(e, message));

        Awaitility.await().atMost(Duration.ofSeconds(30)).until(() ->
                TransferStatus.Failed.equals(
                        transferPackageRepository.findByPackageName(transferPackage.getPackageName()).get().getStatus()
                ));

    }

    private static Stream<TransferPackage> testOutboundDispatchExceptionFlow() {

        return Stream.of(createTransferPackage());

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
        OutboundDispatchMessage dispatchMessage = createOutboundDispatchMessage(transferPackage);

        TransferPackage transferPackage2 = createTransferPackage();
        OutboundDispatchMessage dispatchMessage2 = createWrongOutboundDispatchMessage(transferPackage2);

        return Stream.of(
                Arguments.of(TransferStatus.Sent, dispatchMessage, transferPackage),
                Arguments.of(TransferStatus.Failed, dispatchMessage2, transferPackage2)
        );

    }


    private static OutboundDispatchMessage createOutboundDispatchMessage(TransferPackage transferPackage) {
        OutboundDispatchMessage dispatchMessage = new OutboundDispatchMessage();
        dispatchMessage.setProcessingStartTime((int) System.currentTimeMillis());
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

    private static OutboundDispatchMessage createWrongOutboundDispatchMessage(TransferPackage transferPackage) {
        OutboundDispatchMessage dispatchMessage = new OutboundDispatchMessage();
        dispatchMessage.setProcessingStartTime((int) System.currentTimeMillis());
        dispatchMessage.setTrackingId(transferPackage.getPackageName());
        dispatchMessage.setLocalFilePath(transferPackage.getLocalFilePath());

        RoutingFileInfo routingFileInfo = new RoutingFileInfo();
        Channel channel = new Channel();
        Address address = new Address();
        address.setIp("172.18.0.4");
        address.setPort(22);
        address.setPath("/upload_err");
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
