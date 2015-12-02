package com.sequenceiq.cloudbreak.domain;

import javax.persistence.Entity;

import com.sequenceiq.cloudbreak.common.type.CloudPlatform;

//@Entity
public class OpenStackCredential extends Credential implements ProvisionEntity {

    @Encrypted
    private String userName;
    @Encrypted
    private String password;
    private String tenantName;
    private String endpoint;
    private String userDomain;
    private String keystoneVersion;
    private String keystoneAuthScope;
    private String projectName;
    private String projectDomainName;
    private String domainName;

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

    public String getUserDomain() {
        return userDomain;
    }

    public void setUserDomain(String userDomain) {
        this.userDomain = userDomain;
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

    public String getKeystoneVersion() {
        return keystoneVersion;
    }

    public void setKeystoneVersion(String keystoneVersion) {
        this.keystoneVersion = keystoneVersion;
    }

    public String getKeystoneAuthScope() {
        return keystoneAuthScope;
    }

    public void setKeystoneAuthScope(String keystoneAuthScope) {
        this.keystoneAuthScope = keystoneAuthScope;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public String getProjectDomainName() {
        return projectDomainName;
    }

    public void setProjectDomainName(String projectDomainName) {
        this.projectDomainName = projectDomainName;
    }

    public String getDomainName() {
        return domainName;
    }

    public void setDomainName(String domainName) {
        this.domainName = domainName;
    }

    @Override
    public CloudPlatform cloudPlatform() {
        return CloudPlatform.OPENSTACK;
    }

}
