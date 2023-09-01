package org.iata.ilds.agent.domain.entity;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Embeddable;

@Getter
@Setter
@Embeddable
public class TransferSiteDispatcher {

    @Column(name = "dispatcher")
    private String dispatcher;

    @Column(name = "dispatcher_password")
    private String dispatcherPassword;

    @Column(name = "dispatcher_key_name")
    private String dispatcherKeyName;

    @Column(name = "dispatcher_key_passphrase")
    private String dispatcherKeyPassphrase;
}
