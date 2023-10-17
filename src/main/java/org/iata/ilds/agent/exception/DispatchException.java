package org.iata.ilds.agent.exception;

import org.iata.ilds.agent.domain.message.DispatchCompletedMessage;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessagingException;

public class DispatchException extends MessagingException {

    public DispatchException(Message<DispatchCompletedMessage> failedMessage, Throwable cause) {
        super(failedMessage, cause);
    }

}
