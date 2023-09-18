package org.iata.ilds.agent.domain.builder;

import org.iata.ilds.agent.domain.message.DispatchCompletedMessage;
import org.iata.ilds.agent.domain.message.outbound.OutboundDispatchMessage;
import org.iata.ilds.agent.util.FileTrackingUtils;

import java.nio.file.Path;
import java.nio.file.Paths;

public final class DispatchCompletedMessageBuilder {

    static final String N_A = "N/A";
    static final String HOSTING = "hosting";


    private DispatchCompletedMessage dispatchCompletedMessage;

    private DispatchCompletedMessageBuilder() {
        dispatchCompletedMessage = new DispatchCompletedMessage();
    }


    public static DispatchCompletedMessageBuilder dispatchCompletedMessage(OutboundDispatchMessage message) {
        DispatchCompletedMessageBuilder instance = new DispatchCompletedMessageBuilder();
        instance.dispatchCompletedMessage.setTrackingId(message.getTrackingId());
        instance.dispatchCompletedMessage.setProcessingStartTime(message.getProcessingStartTime());

        instance.dispatchCompletedMessage.setLocalFilePath(message.getLocalFilePath());
        instance.dispatchCompletedMessage.setOriginalFileName(message.getOriginalFileName());
        instance.dispatchCompletedMessage.setOriginalFilePath(message.getOriginalFilePath());
        instance.dispatchCompletedMessage.setOriginalFileSize(message.getOriginalFileSize());
//        instance.dispatchCompletedMessage.setSender(message.getSender());
//        instance.dispatchCompletedMessage.setDestination(message.getDestination());
        instance.dispatchCompletedMessage.setBsp(message.getBsp());

        instance.dispatchCompletedMessage.setSuccessful(true);
        return instance;
    }


    public void addProcessedDataFile(String dataFilePath) {
        dispatchCompletedMessage.getProcessedLocalFilePaths().add(dataFilePath);
    }

    public void addFailedDataFile(String dataFilePath) {
        dispatchCompletedMessage.setLocalFilePath(dataFilePath);
        dispatchCompletedMessage.setSuccessful(false);
    }

    public DispatchCompletedMessage build() {

        boolean isInbound = FileTrackingUtils.isInboundDirection(dispatchCompletedMessage.getTrackingId());
        dispatchCompletedMessage.setSender(isInbound ? N_A : HOSTING);

        //getGrandParentName
        Path dataFilePath = Paths.get(dispatchCompletedMessage.getProcessedLocalFilePaths().stream().findFirst().orElse(dispatchCompletedMessage.getLocalFilePath()));
        dispatchCompletedMessage.setDestination(isInbound ? HOSTING : dataFilePath.getName(dataFilePath.getNameCount() - 3).toString());

        return dispatchCompletedMessage;
    }

}
