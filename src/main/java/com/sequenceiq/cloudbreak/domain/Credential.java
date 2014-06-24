package com.sequenceiq.cloudbreak.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;

@Entity
public abstract class Credential {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "credential_generator")
    @SequenceGenerator(name = "credential_generator", sequenceName = "credential_table")
    private Long id;

    @Enumerated(EnumType.STRING)
    private CloudPlatform cloudPlatform;

    @Column(unique = true, nullable = false)
    private String name;

    private String description;

    public Credential() {

    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public CloudPlatform getCloudPlatform() {
        return cloudPlatform;
    }

    public void setCloudPlatform(CloudPlatform cloudPlatform) {
        this.cloudPlatform = cloudPlatform;
    }

    public abstract CloudPlatform cloudPlatform();

    public abstract User getOwner();

}
