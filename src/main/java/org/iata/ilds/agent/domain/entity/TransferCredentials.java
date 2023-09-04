package org.iata.ilds.agent.domain.entity;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

@Getter
@Setter
@Entity
@Table(name = "tbl_ilds_transfer_credentials")
public class TransferCredentials  extends BaseEntity {

    @Column(name = "password")
    private String password;

    @Column(name = "private_key_name")
    private String privateKeyName;

    @Column(name = "private_key_passphrase")
    private String privateKeyPassphrase;
}
