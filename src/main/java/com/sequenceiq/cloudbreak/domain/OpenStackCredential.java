package com.sequenceiq.cloudbreak.domain;

import javax.persistence.Entity;

@Entity
public class OpenStackCredential extends Credential implements ProvisionEntity {

    private String userName;
    private String password;
    private String tenantName;
    private String endpoint;

    public OpenStackCredential() {
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getTenantName() {
        return tenantName;
    }

    public void setTenantName(String tenantName) {
        this.tenantName = tenantName;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    @Override
    public CloudPlatform cloudPlatform() {
        return CloudPlatform.OPENSTACK;
    }

}
