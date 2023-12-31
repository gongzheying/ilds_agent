package org.iata.ilds.agent.domain.builder;

import org.iata.ilds.agent.domain.message.DispatchCompletedMessage;
import org.iata.ilds.agent.domain.message.QuarantineMessage;
import org.iata.ilds.agent.domain.message.eventlog.AbstractEventLogMessage.CallingProcess;
import org.iata.ilds.agent.domain.message.eventlog.CallingProcessStatus;
import org.iata.ilds.agent.util.FileTrackingUtils;

import static org.iata.ilds.agent.domain.message.eventlog.AbstractEventLogMessage.CallingProcess.INBOUND_DISPATCH;
import static org.iata.ilds.agent.domain.message.eventlog.AbstractEventLogMessage.CallingProcess.OUTBOUND_DISPATCH;
import static org.iata.ilds.agent.domain.message.eventlog.LogType.Failed;

public final class QuarantineMessageBuilder {

    private final QuarantineMessage quarantineMessage;
    private QuarantineMessageBuilder() {
        quarantineMessage = new QuarantineMessage();
    }

    public static QuarantineMessageBuilder quarantineMessage(DispatchCompletedMessage message) {
        QuarantineMessageBuilder instance = new QuarantineMessageBuilder();

        instance.quarantineMessage.setTrackingId(message.getTrackingId());
        instance.quarantineMessage.setProcessingStartTime(message.getProcessingStartTime());

        instance.quarantineMessage.setLocalFilePath(message.getLocalFilePath());
        instance.quarantineMessage.setOriginalFileName(message.getOriginalFileName());
        instance.quarantineMessage.setOriginalFilePath(message.getOriginalFilePath());
        instance.quarantineMessage.setOriginalFileSize(message.getOriginalFileSize());
        instance.quarantineMessage.setSender(message.getSender());
        instance.quarantineMessage.setDestination(message.getDestination());
        instance.quarantineMessage.setBsp(message.getBsp());

        boolean isInbound = FileTrackingUtils.isInboundDirection(message.getTrackingId());
        CallingProcess callingProcess = isInbound ? INBOUND_DISPATCH : OUTBOUND_DISPATCH;
        instance.quarantineMessage.setCallingProcessStatusId(CallingProcessStatus.getId(Failed, callingProcess));

        return instance;
    }

    public QuarantineMessageBuilder errorDescriptionOfParentProcess(String errorDescriptionOfParentProcess) {
        quarantineMessage.setErrorDescriptionOfParentProcess(errorDescriptionOfParentProcess);
        return this;
    }

    public QuarantineMessage build() {
        return quarantineMessage;
    }
}
