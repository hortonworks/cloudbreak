package com.sequenceiq.provisioning.domain;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;

@Entity
public abstract class CloudInstance {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "cloudinstance_generator")
    @SequenceGenerator(name = "cloudinstance_generator", sequenceName = "cloudsequence_table")
    private Long id;

    public CloudInstance() {

    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public abstract CloudPlatform cloudPlatform();
}
