package org.iata.ilds.agent.config.inbound;


import com.fasterxml.jackson.databind.ObjectMapper;
import org.iata.ilds.agent.domain.message.inbound.HostingSystemInfo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import java.io.IOException;

@Configuration
public class HostingSystemConfig {

    @Bean
    public HostingSystemLookup hostingSystemLookup(@Value("${inbound.hostingSystemInfo.path}") Resource template,
                                                   ObjectMapper objectMapper) throws IOException {

        HostingSystemInfo hostingSystemInfo = objectMapper.readValue(template.getInputStream(), HostingSystemInfo.class);
        return new HostingSystemLookup(hostingSystemInfo);

    }


}
