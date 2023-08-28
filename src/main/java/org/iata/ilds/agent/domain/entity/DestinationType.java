package org.iata.ilds.agent.domain.entity;

public enum DestinationType {
    Inbound("Inbound"), Outbound("Outbound");

    private final String label;

    DestinationType(String label) {
        this.label = label;
    }


    @Override
    public String toString() {
        return this.label;
    }
}
