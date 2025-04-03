package com.sequenceiq.flow.domain;

import java.util.Date;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;

@Entity
public class FlowCancel {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "flowcancel_generator")
    @SequenceGenerator(name = "flowcancel_generator", sequenceName = "flowcancel_id_seq", allocationSize = 1)
    private Long id;

    @Column(nullable = false)
    private Long resourceId;

    private Long created = new Date().getTime();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getResourceId() {
        return resourceId;
    }

    public void setResourceId(Long resourceId) {
        this.resourceId = resourceId;
    }

    public Long getCreated() {
        return created;
    }

    public void setCreated(Long created) {
        this.created = created;
    }
}
