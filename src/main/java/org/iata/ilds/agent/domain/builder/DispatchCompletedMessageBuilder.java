package org.iata.ilds.agent.domain.builder;

import org.iata.ilds.agent.domain.message.DispatchCompletedMessage;
import org.iata.ilds.agent.domain.message.inbound.InboundDispatchMessage;
import org.iata.ilds.agent.domain.message.outbound.OutboundDispatchMessage;

public final class DispatchCompletedMessageBuilder {

    static final String N_A = "N/A";
    static final String HOSTING = "hosting";


    private final DispatchCompletedMessage dispatchCompletedMessage;

    private DispatchCompletedMessageBuilder() {
        dispatchCompletedMessage = new DispatchCompletedMessage();
    }



    public static DispatchCompletedMessageBuilder dispatchCompletedMessage(InboundDispatchMessage message) {
        DispatchCompletedMessageBuilder instance = new DispatchCompletedMessageBuilder();
        instance.dispatchCompletedMessage.setTrackingId(message.getTrackingId());
        instance.dispatchCompletedMessage.setProcessingStartTime(message.getProcessingStartTime());

        instance.dispatchCompletedMessage.setLocalFilePath(message.getLocalFilePath());
        instance.dispatchCompletedMessage.setOriginalFileName(message.getOriginalFileName());
        instance.dispatchCompletedMessage.setOriginalFilePath(message.getOriginalFilePath());
        instance.dispatchCompletedMessage.setOriginalFileSize(message.getOriginalFileSize());
        instance.dispatchCompletedMessage.setBsp(message.getBsp());
        instance.dispatchCompletedMessage.setSender(message.getSender());
        instance.dispatchCompletedMessage.setDestination(message.getDestination());
        instance.dispatchCompletedMessage.setSuccessful(true);
        return instance;
    }

    public static DispatchCompletedMessageBuilder dispatchCompletedMessage(OutboundDispatchMessage message) {
        DispatchCompletedMessageBuilder instance = new DispatchCompletedMessageBuilder();
        instance.dispatchCompletedMessage.setTrackingId(message.getTrackingId());
        instance.dispatchCompletedMessage.setProcessingStartTime(message.getProcessingStartTime());

        instance.dispatchCompletedMessage.setLocalFilePath(message.getLocalFilePath());
        instance.dispatchCompletedMessage.setOriginalFileName(message.getOriginalFileName());
        instance.dispatchCompletedMessage.setOriginalFilePath(message.getOriginalFilePath());
        instance.dispatchCompletedMessage.setOriginalFileSize(message.getOriginalFileSize());
        instance.dispatchCompletedMessage.setSender(message.getSender());
        instance.dispatchCompletedMessage.setDestination(message.getDestination());
        instance.dispatchCompletedMessage.setBsp(message.getBsp());
        instance.dispatchCompletedMessage.setSuccessful(true);
        return instance;
    }


    public void addProcessedDataFile(String dataFilePath) {
        dispatchCompletedMessage.getProcessedDataFilePaths().add(dataFilePath);
    }

    public void addFailedDataFile(String dataFilePath) {
        dispatchCompletedMessage.setFailedDataFilePath(dataFilePath);
        dispatchCompletedMessage.setSuccessful(false);
    }

    public DispatchCompletedMessage build() {
        return dispatchCompletedMessage;
    }

}
