package com.sequenceiq.cloudbreak.api.model;

import static com.sequenceiq.cloudbreak.type.KerberosType.EXISTING_AD;
import static com.sequenceiq.cloudbreak.type.KerberosType.EXISTING_FREEIPA;
import static com.sequenceiq.cloudbreak.type.KerberosType.EXISTING_MIT;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.StackModelDescription;
import com.sequenceiq.cloudbreak.type.KerberosType;
import com.sequenceiq.cloudbreak.validation.RequiredKerberosField;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public abstract class KerberosBase implements JsonEntity {

    @RequiredKerberosField(types = {EXISTING_AD, EXISTING_MIT, EXISTING_FREEIPA})
    @ApiModelProperty(StackModelDescription.KERBEROS_KDC_URL)
    private String url;

    @RequiredKerberosField(types = {EXISTING_AD, EXISTING_MIT, EXISTING_FREEIPA})
    @ApiModelProperty(StackModelDescription.KERBEROS_ADMIN_URL)
    private String adminUrl;

    @RequiredKerberosField(types = {EXISTING_AD, EXISTING_MIT, EXISTING_FREEIPA})
    private String realm;

    @RequiredKerberosField(types = EXISTING_AD)
    private String ldapUrl;

    @RequiredKerberosField(types = EXISTING_AD)
    private String containerDn;

    @RequiredKerberosField
    private Boolean tcpAllowed = false;

    @NotNull(message = "Kerberos type can not be null")
    private KerberosType type;

    @ApiModelProperty(StackModelDescription.KERBEROS_KDC_VERIFY_KDC_TRUST)
    private Boolean verifyKdcTrust = true;

    @ApiModelProperty(StackModelDescription.KERBEROS_DOMAIN)
    private String domain;

    @Pattern(regexp = "(^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)(,((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}"
            + "(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?))*$)")
    @ApiModelProperty(StackModelDescription.KERBEROS_NAMESERVERS)
    private String nameServers;

    public KerberosType getType() {
        return type;
    }

    public void setType(KerberosType type) {
        this.type = type;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getAdminUrl() {
        return adminUrl;
    }

    public void setAdminUrl(String adminUrl) {
        this.adminUrl = adminUrl;
    }

    public String getRealm() {
        return realm;
    }

    public void setRealm(String realm) {
        this.realm = realm;
    }

    public String getLdapUrl() {
        return ldapUrl;
    }

    public void setLdapUrl(String ldapUrl) {
        this.ldapUrl = ldapUrl;
    }

    public String getContainerDn() {
        return containerDn;
    }

    public void setContainerDn(String containerDn) {
        this.containerDn = containerDn;
    }

    public Boolean getTcpAllowed() {
        return tcpAllowed;
    }

    public void setTcpAllowed(Boolean tcpAllowed) {
        this.tcpAllowed = tcpAllowed;
    }

    public Boolean getVerifyKdcTrust() {
        return verifyKdcTrust;
    }

    public void setVerifyKdcTrust(Boolean verifyKdcTrust) {
        this.verifyKdcTrust = verifyKdcTrust;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public String getNameServers() {
        return nameServers;
    }

    public void setNameServers(String nameServers) {
        this.nameServers = nameServers;
    }
}
