package org.iata.ilds.agent.exception;

import org.iata.ilds.agent.domain.message.DispatchCompletedMessage;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessagingException;

import java.io.Serial;

public class DispatchException extends MessagingException {

    @Serial
    private static final long serialVersionUID = -6208357434845628394L;

    public DispatchException(Message<DispatchCompletedMessage> failedMessage, Throwable cause) {
        super(failedMessage, cause);
    }

}
