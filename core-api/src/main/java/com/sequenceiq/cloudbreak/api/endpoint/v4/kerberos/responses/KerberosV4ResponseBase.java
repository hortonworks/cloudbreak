package com.sequenceiq.cloudbreak.api.endpoint.v4.kerberos.responses;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.cloudbreak.api.endpoint.v4.JsonEntity;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.StackModelDescription;
import com.sequenceiq.cloudbreak.type.KerberosType;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public abstract class KerberosV4ResponseBase implements JsonEntity {

    private Long id;

    private String name;

    @ApiModelProperty(StackModelDescription.KERBEROS_URL)
    private String url;

    @ApiModelProperty(StackModelDescription.KERBEROS_ADMIN_URL)
    private String adminUrl;

    private String realm;

    private String ldapUrl;

    private String containerDn;

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

    @Size(max = 1000)
    @ApiModelProperty(ModelDescriptions.DESCRIPTION)
    private String description;

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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

}
