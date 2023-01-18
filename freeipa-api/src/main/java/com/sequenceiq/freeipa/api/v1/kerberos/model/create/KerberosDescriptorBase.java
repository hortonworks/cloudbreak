package com.sequenceiq.freeipa.api.v1.kerberos.model.create;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.freeipa.api.v1.kerberos.doc.KerberosConfigModelDescription;
import com.sequenceiq.freeipa.api.v1.kerberos.model.KerberosType;

import io.swagger.v3.oas.annotations.media.Schema;

@JsonIgnoreProperties(ignoreUnknown = true, value = "type")
public abstract class KerberosDescriptorBase {

    @Schema(description = KerberosConfigModelDescription.KERBEROS_PASSWORD, required = true)
    @Size(max = 50, min = 5, message = "The length of the Kerberos password has to be in range of 5 to 50")
    @NotNull
    @NotEmpty
    private String password;

    @Schema(description = KerberosConfigModelDescription.KERBEROS_TCP_ALLOW, required = true)
    private Boolean tcpAllowed = false;

    @Schema(description = KerberosConfigModelDescription.KERBEROS_KDC_VERIFY_KDC_TRUST)
    private Boolean verifyKdcTrust = true;

    @Schema(description = KerberosConfigModelDescription.KERBEROS_DOMAIN)
    private String domain;

    @Pattern(regexp = "(^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)(,((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}"
            + "(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?))*$)")
    @Schema(description = KerberosConfigModelDescription.KERBEROS_NAMESERVERS)
    private String nameServers;

    @Schema(description = KerberosConfigModelDescription.KERBEROS_PRINCIPAL, required = true)
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

    public String getPrincipal() {
        return principal;
    }

    public void setPrincipal(String principal) {
        this.principal = principal;
    }
}
