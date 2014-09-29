package com.sequenceiq.cloudbreak.domain;

import javax.persistence.Column;
import javax.persistence.Entity;

@Entity
public class GccCredential  extends Credential implements ProvisionEntity {
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

}
