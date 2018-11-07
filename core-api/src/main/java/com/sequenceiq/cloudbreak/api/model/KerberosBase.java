package com.sequenceiq.cloudbreak.api.model;

import static com.sequenceiq.cloudbreak.type.KerberosType.CB_MANAGED;
import static com.sequenceiq.cloudbreak.type.KerberosType.CUSTOM;
import static com.sequenceiq.cloudbreak.type.KerberosType.EXISTING_AD;
import static com.sequenceiq.cloudbreak.type.KerberosType.EXISTING_MIT;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.StackModelDescription;
import com.sequenceiq.cloudbreak.type.KerberosType;
import com.sequenceiq.cloudbreak.validation.RequiredKerberosField;
import com.sequenceiq.cloudbreak.validation.ValidJson;
import com.sequenceiq.cloudbreak.validation.ValidKerberosDescriptor;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public abstract class KerberosBase implements JsonEntity {

    @RequiredKerberosField(types = CB_MANAGED)
    @ApiModelProperty(StackModelDescription.KERBEROS_ADMIN)
    @Size(max = 15, min = 5, message = "The length of the Kerberos admin has to be in range of 5 to 15")
    private String admin;

    @RequiredKerberosField(types = {EXISTING_AD, EXISTING_MIT})
    @ApiModelProperty(StackModelDescription.KERBEROS_KDC_URL)
    private String url;

    @RequiredKerberosField(types = {EXISTING_AD, EXISTING_MIT})
    @ApiModelProperty(StackModelDescription.KERBEROS_ADMIN_URL)
    private String adminUrl;

    @RequiredKerberosField(types = {EXISTING_AD, EXISTING_MIT})
    private String realm;

    @RequiredKerberosField(types = EXISTING_AD)
    private String ldapUrl;

    @RequiredKerberosField(types = EXISTING_AD)
    private String containerDn;

    @RequiredKerberosField
    private Boolean tcpAllowed = false;

    @RequiredKerberosField(types = CUSTOM)
    @ValidKerberosDescriptor
    private String descriptor;

    @RequiredKerberosField(types = CUSTOM)
    @ValidJson(message = "The krb5 configuration must be a valid JSON")
    private String krb5Conf;

    @NotNull
    private KerberosType type;

    public KerberosType getType() {
        return type;
    }

    public void setType(KerberosType type) {
        this.type = type;
    }

    public String getAdmin() {
        return admin;
    }

    public void setAdmin(String admin) {
        this.admin = admin;
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

    public String getDescriptor() {
        return descriptor;
    }

    public void setDescriptor(String descriptor) {
        this.descriptor = descriptor;
    }

    public String getKrb5Conf() {
        return krb5Conf;
    }

    public void setKrb5Conf(String krb5Conf) {
        this.krb5Conf = krb5Conf;
    }
}
