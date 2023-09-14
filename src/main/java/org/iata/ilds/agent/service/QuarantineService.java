package org.iata.ilds.agent.service;

import org.iata.ilds.agent.domain.message.QuarantineMessage;

public interface QuarantineService {

    void quarantineForFailure(QuarantineMessage quarantineMessage);

}
