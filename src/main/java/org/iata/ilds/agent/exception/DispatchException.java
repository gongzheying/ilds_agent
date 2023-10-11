package org.iata.ilds.agent.exception;

import lombok.Getter;
import org.iata.ilds.agent.domain.message.DispatchCompletedMessage;

@Getter
public class DispatchException extends RuntimeException {

    private DispatchCompletedMessage dispatchCompletedMessage;

    public DispatchException(String errorMessage, DispatchCompletedMessage completeMessage) {
        super(errorMessage);
        this.dispatchCompletedMessage = completeMessage;
    }
}
