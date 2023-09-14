package org.iata.ilds.agent.service;

import org.iata.ilds.agent.domain.message.QuarantineMessage;
import org.springframework.stereotype.Service;

@Service
public class QuarantineServiceImpl implements   QuarantineService {

    @Override
    public void quarantineForFailure(QuarantineMessage quarantineMessage) {

    }
}
