package org.iata.ilds.agent.spring.integration;

import lombok.extern.log4j.Log4j2;
import org.awaitility.Awaitility;
import org.iata.ilds.agent.domain.entity.TransferStatus;
import org.iata.ilds.agent.domain.message.DispatchCompletedMessage;
import org.iata.ilds.agent.exception.OutboundDispatchException;
import org.iata.ilds.agent.spring.data.TransferPackageRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.MessageChannel;

import java.time.Duration;
import java.util.List;

@Log4j2
@SpringBootTest
public class OutboundDispatchConfigTests {

    @Autowired
    private MessageChannel handleException;

    @Autowired
    private TransferPackageRepository transferPackageRepository;

    @Test
    public void testHandleExceptionFlow() {
        DispatchCompletedMessage dispatchCompletedMessage = new DispatchCompletedMessage();
        dispatchCompletedMessage.setProcessingStartTime((int) System.currentTimeMillis());
        dispatchCompletedMessage.setTrackingId("1");
        dispatchCompletedMessage.setSuccessful(false);
        dispatchCompletedMessage.setLocalFilePath(List.of("file1.txt", "file2.txt"));
        dispatchCompletedMessage.setLocalFilePathWithErrors(List.of("wrong_file.txt"));
        RuntimeException customException = new RuntimeException("Custom Error!");
        OutboundDispatchException e = new OutboundDispatchException(dispatchCompletedMessage, customException);
        handleException.send(MessageBuilder.withPayload(e).build());

        Awaitility.await().atMost(Duration.ofSeconds(10)).until(() ->
                TransferStatus.Failed.equals(transferPackageRepository.findByPackageName("1").orElseThrow().getStatus())
        );

    }

    @Test
    public void testHandleExceptionFlowWithRuntimeException() {
        RuntimeException customException = new RuntimeException("Custom Error!");
        handleException.send(MessageBuilder.withPayload(customException).build());

        Awaitility.await().atMost(Duration.ofSeconds(10)).until(() -> {
            log.info("There should be an ErrorMessage appearing on the console");
            return true;
        });

    }

    @Test
    public void testHandleOutboundDispatchFlow() {

    }

}
