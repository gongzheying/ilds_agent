package org.iata.ilds.agent.domain.entity;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;

import static javax.persistence.FetchType.LAZY;

@Getter
@Setter
@Entity
@Table(name = "tbl_ilds_transfer_credentials")
public class TransferCredentials extends BaseEntity {

    @Column(name = "title")
    private String title;

    @Column(name = "password")
    private String password;

    @Column(name = "private_key_name")
    private String privateKeyName;

    @Column(name = "private_key_passphrase")
    private String privateKeyPassphrase;

    @Lob
    @Basic(fetch = LAZY)
    @Column(name = "private_key_content")
    private byte[] privateKeyContent;

    @Column(name = "private_key_content_type")
    private String privateKeyContentType;

    @Enumerated(EnumType.ORDINAL)
    @Column(name = "type")
    private TransferSiteCredentialType type;

}
