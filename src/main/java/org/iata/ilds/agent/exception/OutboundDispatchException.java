package org.iata.ilds.agent.exception;

import lombok.Getter;
import org.iata.ilds.agent.domain.message.DispatchCompletedMessage;

@Getter
public class OutboundDispatchException extends RuntimeException {

    private DispatchCompletedMessage message;
    private String fileWithErrors;

    public OutboundDispatchException(DispatchCompletedMessage message, String fileWithErrors, Throwable cause) {
        super(cause);
        this.message = message;
        this.fileWithErrors = fileWithErrors;
    }
}
