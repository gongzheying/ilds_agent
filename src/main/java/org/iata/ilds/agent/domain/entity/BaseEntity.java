package org.iata.ilds.agent.domain.entity;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.util.Date;


/**
 * Base entity, to generate the ID
 */
@Getter
@Setter
@MappedSuperclass
public abstract class BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private long id;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    @Column(name = "created_at")
    private Date createdAt;

    @Column(name = "last_modified_at")
    private Date lastModifiedAt;

    @PrePersist
    public void onCreate() {
        createdAt = new Date();
        lastModifiedAt = new Date();
    }

    @PreUpdate
    public void onUpdate() {
        lastModifiedAt = new Date();
    }

}
