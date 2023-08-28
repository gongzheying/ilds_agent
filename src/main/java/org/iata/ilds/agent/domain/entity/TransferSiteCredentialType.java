package org.iata.ilds.agent.domain.entity;

public enum TransferSiteCredentialType {
    PasswordAndSSHKey("Key and Password"), SSHKeyOnly("Key Only"), PasswordOnly("Password Only");

    private final String label;

    TransferSiteCredentialType(String label) {
        this.label = label;
    }


    @Override
    public String toString() {
        return this.label;
    }
}
