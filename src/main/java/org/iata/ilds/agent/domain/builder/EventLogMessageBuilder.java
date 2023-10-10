package org.iata.ilds.agent.domain.builder;

import org.iata.ilds.agent.domain.message.BaseMessage;
import org.iata.ilds.agent.domain.message.DispatchCompletedMessage;
import org.iata.ilds.agent.domain.message.eventlog.AbstractEventLogMessage;
import org.iata.ilds.agent.domain.message.eventlog.AbstractEventLogMessage.CallingProcess;
import org.iata.ilds.agent.domain.message.eventlog.CallingProcessStatus;
import org.iata.ilds.agent.domain.message.eventlog.ProcessingBaseEventLogMessage;
import org.iata.ilds.agent.domain.message.eventlog.ProcessingTimeEventLogMessage;
import org.iata.ilds.agent.domain.message.inbound.InboundDispatchMessage;
import org.iata.ilds.agent.domain.message.outbound.OutboundDispatchMessage;
import org.iata.ilds.agent.util.FileTrackingUtils;

import java.nio.file.Paths;

import static org.iata.ilds.agent.domain.message.eventlog.AbstractEventLogMessage.CallingProcess.INBOUND_DISPATCH;
import static org.iata.ilds.agent.domain.message.eventlog.AbstractEventLogMessage.CallingProcess.OUTBOUND_DISPATCH;
import static org.iata.ilds.agent.domain.message.eventlog.AbstractEventLogMessage.EventSeverity.CRITICAL;
import static org.iata.ilds.agent.domain.message.eventlog.AbstractEventLogMessage.EventSeverity.INFO;
import static org.iata.ilds.agent.domain.message.eventlog.LogType.*;

public final class EventLogMessageBuilder {

    private AbstractEventLogMessage eventLogMessage;
    private EventLogMessageBuilder(AbstractEventLogMessage eventLogMessage) {
        this.eventLogMessage = eventLogMessage;
    }

    private static ProcessingBaseEventLogMessage basicEventLogMessage(BaseMessage message, CallingProcess callingProcess) {
        ProcessingBaseEventLogMessage eventLogMessage = new ProcessingBaseEventLogMessage();
        eventLogMessage.setOriginalPackageName(message.getOriginalFileName());
        eventLogMessage.setOriginalPackageSize(message.getOriginalFileSize());
        eventLogMessage.setSender(message.getSender());
        eventLogMessage.setDestination(message.getDestination());
        eventLogMessage.setTrackingId(message.getTrackingId());
        eventLogMessage.setBsp(message.getBsp());
        if (message.getOriginalFilePath() != null) {
            eventLogMessage.setInboundFolder(Paths.get(message.getOriginalFilePath()).getParent().toString());
        }
        eventLogMessage.setCallingProcess(callingProcess);
        eventLogMessage.setClassName("ProcessingBaseEventLogMessage");
        return eventLogMessage;
    }


    public static EventLogMessageBuilder eventLog(InboundDispatchMessage message) {
        return new EventLogMessageBuilder( basicEventLogMessage(message, INBOUND_DISPATCH));
    }

    public static EventLogMessageBuilder eventLog(OutboundDispatchMessage message) {
        return new EventLogMessageBuilder( basicEventLogMessage(message, OUTBOUND_DISPATCH));
    }

    public static EventLogMessageBuilder eventLog(DispatchCompletedMessage message) {

        ProcessingTimeEventLogMessage eventLogMessage = new ProcessingTimeEventLogMessage();
        eventLogMessage.setOriginalPackageName(message.getOriginalFileName());
        eventLogMessage.setOriginalPackageSize(message.getOriginalFileSize());
        eventLogMessage.setSender(message.getSender());
        eventLogMessage.setDestination(message.getDestination());
        eventLogMessage.setTrackingId(message.getTrackingId());
        eventLogMessage.setBsp(message.getBsp());
        if (message.getOriginalFilePath() != null) {
            eventLogMessage.setInboundFolder(Paths.get(message.getOriginalFilePath()).getParent().toString());
        }

        boolean isInbound = FileTrackingUtils.isInboundDirection(message.getTrackingId());
        eventLogMessage.setCallingProcess(isInbound ? INBOUND_DISPATCH : OUTBOUND_DISPATCH);

        eventLogMessage.setProcessingStartTime(message.getProcessingStartTime());
        eventLogMessage.setProcessingInterval(System.currentTimeMillis() - eventLogMessage.getProcessingStartTime() );
        eventLogMessage.setClassName("ProcessingTimeEventLogMessage");

        return new EventLogMessageBuilder(eventLogMessage);
    }

    public AbstractEventLogMessage started() {
        eventLogMessage.setEventSeverity(INFO);
        eventLogMessage.setCallingProcessStatusId(CallingProcessStatus.getId(Started, eventLogMessage.getCallingProcess()));
        return eventLogMessage;
    }

    public AbstractEventLogMessage completed() {
        eventLogMessage.setEventSeverity(INFO);
        eventLogMessage.setCallingProcessStatusId(CallingProcessStatus.getId(Completed, eventLogMessage.getCallingProcess()));
        return eventLogMessage;
    }

    public AbstractEventLogMessage failedBySystem(String errorDescription) {
        eventLogMessage.setEventSeverity(CRITICAL);
        eventLogMessage.setCallingProcessStatusId(CallingProcessStatus.getId(Failed, eventLogMessage.getCallingProcess()));
        eventLogMessage.setDescription(errorDescription);
        return eventLogMessage;
    }

}
