package org.iata.ilds.agent.domain.entity;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.util.List;

@Getter
@Setter
@Entity
@Table(
        name = "tbl_ilds_transfer_site",
        uniqueConstraints = {@UniqueConstraint(columnNames = {"username", "ip", "port", "remote_path"})}
)
public class TransferSite extends BaseEntity {
    @Column(name = "mft_transfer_site_id")
    private String mftTransferSiteId;

    @Column(name = "ip")
    private String ip;

    @Column(name = "port")
    private int port;

    @Column(name = "username")
    private String username;

    @Column(name = "remote_path")
    private String remotePath;

    @Column(name = "compression_password")
    private String compressionPassword;

    @Column(name = "credential_type")
    private TransferSiteCredentialType credentialType;


    @Enumerated
    @Column(name = "status")
    private TransferSiteStatus status;

    @Column(name = "encryption_key_name")
    private String encryptionKeyName;

    @Column(name = "destination_type")
    private DestinationType destinationType;

    @Column(name = "file_rename_suffix")
    private String fileRenameSuffix;

    @OneToMany(mappedBy = "transferSite")
    private List<PGPEncryptionKey> encryptionKeys;

    @Column(name = "trigger_required")
    private boolean triggerRequired;

    @Column(name = "dispatcher")
    private String dispatcher;

    @ManyToOne
    @JoinColumn(name = "credential_id")
    private TransferCredentials credential;

}
