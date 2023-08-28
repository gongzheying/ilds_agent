package org.iata.ilds.agent.domain.entity;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.util.Date;
import java.util.List;

@Getter
@Setter
@Entity
@Table(
        name = "tbl_ilds_transfer_package",
        uniqueConstraints = {@UniqueConstraint(columnNames = {"package_name"})}
)
public class TransferPackage extends BaseEntity {

    @Column(name = "package_name")
    private String packageName;

    @Column(name = "original_package_name")
    private String originalPackageName;

    @Column(name = "final_package_name")
    private String finalPackageName;

    @Column(name = "original_package_size")
    private long originalPackageSize;

    @Column(name = "local_file_path")
    private String localFilePath;

    @Enumerated
    @Column(name = "status")
    private TransferStatus status;

    @Enumerated
    @Column(name = "direction")
    private Direction direction;

    @Column(name = "sender")
    private String sender;

    @Column(name = "destination")
    private String destination;

    @Column(name = "original_file_path")
    private String originalFilePath;

    @Column(name = "transfer_protocol")
    private String transferProtocol;

    @Enumerated
    @Column(name = "transfer_package_type")
    private TransferPackageType transferPackageType;

    @OneToMany(mappedBy = "transferPackage")
    private List<TransferFile> transferFiles;

    @Column(name = "received_date")
    private Date receivedDate;

    //The start time which start tokenize/detokenize process
    @Column(name = "start_tokenize_timestamp")
    private long startTokenizeTimestamp;

    //The start time which finish tokenize/detokenize process
    @Column(name = "end_tokenize_timestamp")
    private long endTokenizeTimestamp;

    @Column(name = "bsp")
    private String bsp;

    @Column(name = "trigger_required")
    private boolean triggerRequired;

    @Column(name = "need_retadd")
    private boolean needRETADD;

    @Column(name = "need_hotadd")
    private boolean needHOTADD;


    @Column(name = "output_category")
    private String outputCategory;


    @Column(name = "critical")
    private String critical;


}
