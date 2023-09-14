package org.iata.ilds.agent.domain.builder;

import org.iata.ilds.agent.domain.message.DispatchCompletedMessage;
import org.iata.ilds.agent.domain.message.QuarantineMessage;

public final class QuarantineMessageBuilder {

    private QuarantineMessage quarantineMessage;
    private QuarantineMessageBuilder() {
        quarantineMessage = new QuarantineMessage();
    }

    public static QuarantineMessageBuilder quarantineMessage(DispatchCompletedMessage dispatchCompletedMessage) {
        QuarantineMessageBuilder instance = new QuarantineMessageBuilder();


        instance.quarantineMessage.setTrackingId(dispatchCompletedMessage.getTrackingId());
        instance.quarantineMessage.setProcessingStartTime(dispatchCompletedMessage.getProcessingStartTime());

        return instance;
    }

    public QuarantineMessageBuilder callingProcessStatusId(int callingProcessStatusId) {
        this.quarantineMessage.setCallingProcessStatusId(callingProcessStatusId);
        return this;
    }

    public QuarantineMessageBuilder errorDescriptionOfParentProcess(String errorDescriptionOfParentProcess) {
        quarantineMessage.setErrorDescriptionOfParentProcess(errorDescriptionOfParentProcess);
        return this;
    }

    public QuarantineMessage build() {
        return quarantineMessage;
    }
}
