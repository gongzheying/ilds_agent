package org.iata.ilds.agent.domain.message.eventlog;

import org.iata.ilds.agent.domain.entity.Direction;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;


/**
 * Include Calling Process information
 */
public enum CallingProcess {
    PROBLEM_RESOLUTION_BACKUP("Problem Resolution - Backup", "PRB"),
    PROBLEM_RESOLUTION_QUARANTINE("Problem Resolution - Quarantine", "PRQ"),
    INBOUND_RECEIPT("Inbound Receipt", "IRE", Direction.INBOUND),
    INBOUND_BACKUP("Inbound Backup", "IBA", Direction.INBOUND),
    INBOUND_VERIFICATION("Inbound Verification", "IVE", Direction.INBOUND),
    INBOUND_SUBSTITUTION("Inbound Substitution", "ISU", Direction.INBOUND),
    INBOUND_DISPATCH_PREPARATION("Inbound Dispatch Preparation", "IDP", Direction.INBOUND),
    INBOUND_DISPATCH("Inbound Dispatch", "IDS", Direction.INBOUND),
    OUTBOUND_RECEIPT("Outbound Receipt", "ORE"),
    OUTBOUND_VERIFICATION("Outbound Verification", "OVE", Direction.OUTBOUND),
    OUTBOUND_RESTORATION("Outbound Restoration", "ORS", Direction.OUTBOUND),
    OUTBOUND_DISPATCH_PREPARATION("Outbound Dispatch Preparation", "ODP", Direction.OUTBOUND),
    OUTBOUND_DISPATCH("Outbound Dispatch", "ODS", Direction.OUTBOUND),
    QUARANTINE("Quarantine", "QUA"),
    INBOUND_BACKUP_DELETION("Inbound Backup Deletion", "IBD"),
    OFFLINE_SCANNING("Offline Scanning", "OFF"),
    ONLINE_TRANSACTION("Online Transaction", "ONL"),
    ONLINE_FILE("Online File Inbound", "ONI"),
    ONLINE_FILE_OUTBOUND("Online File Outbound", "ONO");

    private String name;
    private String code;
    private Optional<Direction> direction;

    private CallingProcess(final String name, final String code) {
        this(name, code, Optional.<Direction>empty());
    }

    private CallingProcess(final String name, final String code, Direction direction) {
        this(name, code, Optional.of(direction));
    }

    private CallingProcess(final String name, final String code, final Optional<Direction> direction) {
        this.name = name;
        this.code = code;
        this.direction = direction;
    }

    public static List<CallingProcess> filterBy(Direction direction) {
        final List<CallingProcess> cps = new ArrayList<>();
        for (CallingProcess cp : values()) {
            if (cp.getDirection().isPresent() && cp.getDirection().get() == direction) {
                cps.add(cp);
            }
        }
        return cps;
    }

    public String getName() {
        return name;
    }

    public String getCode() {
        return code;
    }

    public Optional<Direction> getDirection() {
        return direction;
    }

    @Override
    public String toString() {
        return this.code + " - " + this.name;
    }
}