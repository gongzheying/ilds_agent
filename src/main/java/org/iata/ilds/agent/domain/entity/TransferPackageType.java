package org.iata.ilds.agent.domain.entity;

public enum TransferPackageType {
    ZIP("zip"), GZ("gz"), FILE("file");

    private final String fileTypeName;

    TransferPackageType(String fileTypeName) {
        this.fileTypeName = fileTypeName;
    }

    @Override
    public String toString() {
        return this.fileTypeName;
    }


}
