package com.sequenceiq.cloudbreak.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

@Entity
public class GccCredential  extends Credential implements ProvisionEntity {
    @ManyToOne
    @JoinColumn(name = "gccCredential_gccCredentialOwner")
    private User gccCredentialOwner;
    @Column(nullable = false)
    private String name;
    private String serviceAccountId;
    @Column(columnDefinition = "TEXT")
    private String serviceAccountPrivateKey;
    private String projectId;

    public GccCredential() {

    }

    public String getServiceAccountId() {
        return serviceAccountId;
    }

    public void setServiceAccountId(String serviceAccountId) {
        this.serviceAccountId = serviceAccountId;
    }

    public String getServiceAccountPrivateKey() {
        return serviceAccountPrivateKey;
    }

    public void setServiceAccountPrivateKey(String serviceAccountPrivateKey) {
        this.serviceAccountPrivateKey = serviceAccountPrivateKey;
    }

    public User getGccCredentialOwner() {
        return gccCredentialOwner;
    }

    public void setGccCredentialOwner(User gccCredentialOwner) {
        this.gccCredentialOwner = gccCredentialOwner;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    @Override
    public CloudPlatform cloudPlatform() {
        return CloudPlatform.GCC;
    }

    @Override
    public User getOwner() {
        return gccCredentialOwner;
    }

    @Override
    public String getCredentialName() {
        return name;
    }
}
