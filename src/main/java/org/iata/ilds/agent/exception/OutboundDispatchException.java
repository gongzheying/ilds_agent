package org.iata.ilds.agent.exception;

import lombok.Getter;
import org.iata.ilds.agent.domain.message.DispatchCompletedMessage;

@Getter
public class OutboundDispatchException extends RuntimeException {

    private DispatchCompletedMessage dispatchCompletedMessage;

    public OutboundDispatchException(DispatchCompletedMessage message, Throwable cause) {
        super(cause);
        this.dispatchCompletedMessage = message;
    }
}
