package org.iata.ilds.agent.domain.entity;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;

@Getter
@Setter
@Entity
@Table(
        name = "tbl_ilds_transfer_file",
        uniqueConstraints = {@UniqueConstraint(name = "idx_transfer_package_id_file_name", columnNames = {"transfer_package_id", "file_name"})}
)
public class TransferFile extends BaseEntity {


    @Column(name = "file_name")
    private String fileName;

    @Enumerated
    @Column(name = "file_type")
    private FileType fileType;

    @Enumerated
    @Column(name = "status")
    private TransferStatus status;

    @ManyToOne
    @JoinColumn(name = "transfer_package_id")
    private TransferPackage transferPackage;

}
