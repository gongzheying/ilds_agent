package org.iata.ilds.agent.domain.entity;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;

@Getter
@Setter
@Entity
@Table(
        name = "tbl_ilds_pgp_encryption_key",
        uniqueConstraints = {@UniqueConstraint(name = "idx_transfer_site_id_key_name", columnNames = {"transfer_site_id", "key_name"})}
)
public class PGPEncryptionKey extends BaseEntity {
    @Column(name = "key_name")
    private String keyName;

    @Lob
    @Column(name = "key_content")
    private String keyContent;

    @ManyToOne
    @JoinColumn(name = "transfer_site_id")
    private TransferSite transferSite;

}
