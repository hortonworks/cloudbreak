package com.sequenceiq.cloudbreak.api.endpoint.v4.kerberos.requests;

import static com.sequenceiq.cloudbreak.doc.ModelDescriptions.StackModelDescription.KERBEROS_ADMIN;
import static com.sequenceiq.cloudbreak.doc.ModelDescriptions.StackModelDescription.KERBEROS_DOMAIN;
import static com.sequenceiq.cloudbreak.doc.ModelDescriptions.StackModelDescription.KERBEROS_KDC_VERIFY_KDC_TRUST;
import static com.sequenceiq.cloudbreak.doc.ModelDescriptions.StackModelDescription.KERBEROS_NAMESERVERS;
import static com.sequenceiq.cloudbreak.doc.ModelDescriptions.StackModelDescription.KERBEROS_PASSWORD;
import static com.sequenceiq.cloudbreak.doc.ModelDescriptions.StackModelDescription.KERBEROS_PRINCIPAL;
import static com.sequenceiq.cloudbreak.doc.ModelDescriptions.StackModelDescription.KERBEROS_TCP_ALLOW;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.api.endpoint.v4.JsonEntity;
import com.sequenceiq.cloudbreak.type.KerberosType;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true, value = "type")
public abstract class KerberosTypeBase implements JsonEntity {

    @ApiModelProperty(KERBEROS_ADMIN)
    private String admin;

    @ApiModelProperty(value = KERBEROS_PASSWORD, required = true)
    @Size(max = 50, min = 5, message = "The length of the Kerberos password has to be in range of 5 to 50")
    @NotNull
    @NotEmpty
    private String password;

    @ApiModelProperty(value = KERBEROS_TCP_ALLOW, required = true)
    private Boolean tcpAllowed = false;

    @ApiModelProperty(KERBEROS_KDC_VERIFY_KDC_TRUST)
    private Boolean verifyKdcTrust = true;

    @ApiModelProperty(KERBEROS_DOMAIN)
    private String domain;

    @Pattern(regexp = "(^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)(,((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}"
            + "(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?))*$)")
    @ApiModelProperty(KERBEROS_NAMESERVERS)
    private String nameServers;

    @ApiModelProperty(value = KERBEROS_PRINCIPAL, required = true)
    @NotNull
    @NotEmpty
    private String principal;

    abstract KerberosType getType();

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

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Boolean getTcpAllowed() {
        return tcpAllowed;
    }

    public void setTcpAllowed(Boolean tcpAllowed) {
        this.tcpAllowed = tcpAllowed;
    }

    public String getAdmin() {
        return admin;
    }

    public void setAdmin(String admin) {
        this.admin = admin;
    }

    public String getPrincipal() {
        return principal;
    }

    public void setPrincipal(String principal) {
        this.principal = principal;
    }

}
