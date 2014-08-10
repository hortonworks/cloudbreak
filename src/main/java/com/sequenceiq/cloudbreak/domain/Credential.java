package com.sequenceiq.cloudbreak.domain;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

@Entity
@Table(uniqueConstraints = {
        @UniqueConstraint(columnNames = { "azureCredential_azureCredentialOwner", "name" }),
        @UniqueConstraint(columnNames = { "awsCredential_awsCredentialOwner", "name" })
})
public abstract class Credential {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "credential_generator")
    @SequenceGenerator(name = "credential_generator", sequenceName = "credential_table")
    private Long id;

    @Enumerated(EnumType.STRING)
    private CloudPlatform cloudPlatform;

    private String description;

    @Column(columnDefinition = "TEXT")
    private String publicKey;

    @ElementCollection(fetch = FetchType.EAGER)
    @Enumerated(EnumType.STRING)
    private List<UserRole> userRoles = new ArrayList<>();

    public Credential() {

    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(String publicKey) {
        this.publicKey = publicKey;
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

    public abstract String getCredentialName();

    public List<UserRole> getUserRoles() {
        return userRoles;
    }

    public void setUserRoles(List<UserRole> userRoles) {
        this.userRoles = userRoles;
    }
}
