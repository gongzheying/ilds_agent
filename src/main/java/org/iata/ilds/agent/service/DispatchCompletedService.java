package org.iata.ilds.agent.service;

import org.iata.ilds.agent.domain.message.DispatchCompletedMessage;

public interface DispatchCompletedService {

    void setCompletionStatus(DispatchCompletedMessage message, String... fileWithErrors);
}
