package org.iata.ilds.agent.domain.builder;

import org.iata.ilds.agent.domain.message.DispatchCompletedMessage;
import org.iata.ilds.agent.util.FileTrackingUtils;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.stream.Stream;

public final class DispatchCompletedMessageBuilder {

    static final String N_A = "N/A";
    static final String HOSTING = "hosting";


    private DispatchCompletedMessage dispatchCompletedMessage;

    private DispatchCompletedMessageBuilder() {
        dispatchCompletedMessage = new DispatchCompletedMessage();
    }

    public static DispatchCompletedMessageBuilder dispatchCompletedMessage(String trackingId) {
        DispatchCompletedMessageBuilder instance = new DispatchCompletedMessageBuilder();
        instance.dispatchCompletedMessage.setTrackingId(trackingId);
        return instance;
    }


    public DispatchCompletedMessageBuilder processingStartTime(int processingStartTime) {
        dispatchCompletedMessage.setProcessingStartTime(processingStartTime);
        return this;
    }

    public void addCompletedDataFile(String dataFilePath) {
        dispatchCompletedMessage.getLocalFilePath().add(dataFilePath);
    }

    public void addFailedDataFile(String dataFilePath) {
        dispatchCompletedMessage.getLocalFilePathWithErrors().add(dataFilePath);
    }

    public DispatchCompletedMessage build() {

        boolean isInbound = FileTrackingUtils.isInboundDirection(dispatchCompletedMessage.getTrackingId());
        dispatchCompletedMessage.setSender(isInbound ? N_A : HOSTING);

        //getGrandParentName
        Optional<String> dataFilePathOptional = Stream.concat(
                dispatchCompletedMessage.getLocalFilePath().stream(),
                dispatchCompletedMessage.getLocalFilePathWithErrors().stream()).findFirst();
        Path dataFilePath = Paths.get(dataFilePathOptional.get());
        dispatchCompletedMessage.setDestination(isInbound ? HOSTING : dataFilePath.getName(dataFilePath.getNameCount() - 3).toString());

        dispatchCompletedMessage.setSuccessful(dispatchCompletedMessage.getLocalFilePathWithErrors().isEmpty());

        return dispatchCompletedMessage;
    }

}
