package org.iata.ilds.agent.config.inbound;

import org.iata.ilds.agent.domain.message.inbound.HostingSystem;
import org.iata.ilds.agent.domain.message.inbound.HostingSystemInfo;

import java.util.Optional;

public class HostingSystemLookup {

    private final HostingSystemInfo hostingSystemInfo;

    public HostingSystemLookup(HostingSystemInfo hostingSystemInfo) {
        this.hostingSystemInfo = hostingSystemInfo;
    }


    public HostingSystem getHostingSystem(String hostingName) {
        Optional<HostingSystem> hostingSystemOptional = hostingSystemInfo.getHostingSystems().stream().filter(hostingSystem -> hostingSystem.getHostingName().equals(hostingName)).findFirst();
        return hostingSystemOptional.orElse(null);
    }


}
