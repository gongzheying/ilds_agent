package org.iata.ilds.agent.domain.entity;


public enum TransferSiteStatus {
    UNHOLD("unhold"), HOLD("hold");

    private final String label;

    TransferSiteStatus(String label) {
        this.label = label;
    }


    @Override
    public String toString() {
        return this.label;
    }
}
