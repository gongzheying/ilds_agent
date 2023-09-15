package org.iata.ilds.agent.domain.builder;

import org.iata.ilds.agent.domain.message.DispatchCompletedMessage;
import org.iata.ilds.agent.domain.message.QuarantineMessage;
import org.iata.ilds.agent.domain.message.eventlog.CallingProcess;
import org.iata.ilds.agent.domain.message.eventlog.CallingProcessStatus;
import org.iata.ilds.agent.domain.message.eventlog.LogType;
import org.iata.ilds.agent.util.FileTrackingUtils;

public final class QuarantineMessageBuilder {

    private QuarantineMessage quarantineMessage;
    private QuarantineMessageBuilder() {
        quarantineMessage = new QuarantineMessage();
    }

    public static QuarantineMessageBuilder quarantineMessage(DispatchCompletedMessage dispatchCompletedMessage) {
        QuarantineMessageBuilder instance = new QuarantineMessageBuilder();


        instance.quarantineMessage.setTrackingId(dispatchCompletedMessage.getTrackingId());
        instance.quarantineMessage.setProcessingStartTime(dispatchCompletedMessage.getProcessingStartTime());

        boolean isInbound = FileTrackingUtils.isInboundDirection(dispatchCompletedMessage.getTrackingId());
        CallingProcess callingProcess = isInbound ? CallingProcess.INBOUND_DISPATCH : CallingProcess.OUTBOUND_DISPATCH;
        instance.quarantineMessage.setCallingProcessStatusId(CallingProcessStatus.getId(LogType.Failed, callingProcess));

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
