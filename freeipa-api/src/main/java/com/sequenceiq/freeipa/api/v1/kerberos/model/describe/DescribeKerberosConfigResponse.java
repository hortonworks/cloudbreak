package com.sequenceiq.freeipa.api.v1.kerberos.model.describe;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.cloudbreak.service.secret.model.SecretResponse;
import com.sequenceiq.freeipa.api.v1.kerberos.doc.KerberosConfigModelDescription;
import com.sequenceiq.freeipa.api.v1.kerberos.model.KerberosType;
import com.sequenceiq.service.api.doc.ModelDescriptions;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "DescribeKerberosConfigV1Response")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class DescribeKerberosConfigResponse {
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private String crn;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @NotNull
    @Schema(description = ModelDescriptions.ENVIRONMENT_CRN, requiredMode = Schema.RequiredMode.REQUIRED)
    private String environmentCrn;

    @Schema(description = KerberosConfigModelDescription.KERBEROS_URL)
    private String url;

    @Schema(description = KerberosConfigModelDescription.KERBEROS_ADMIN_URL)
    private String adminUrl;

    private String realm;

    private String ldapUrl;

    private String containerDn;

    private Boolean tcpAllowed = false;

    @NotNull(message = "Kerberos type can not be null")
    private KerberosType type;

    @Schema(description = KerberosConfigModelDescription.KERBEROS_KDC_VERIFY_KDC_TRUST, requiredMode = Schema.RequiredMode.REQUIRED)
    private Boolean verifyKdcTrust = true;

    @Schema(description = KerberosConfigModelDescription.KERBEROS_DOMAIN)
    private String domain;

    @Pattern(regexp = "(^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)(,((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}"
            + "(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?))*$)")
    @Schema(description = KerberosConfigModelDescription.KERBEROS_NAMESERVERS)
    private String nameServers;

    @Size(max = 1000)
    @Schema(description = ModelDescriptions.DESCRIPTION)
    private String description;

    @Schema(description = KerberosConfigModelDescription.KERBEROS_PASSWORD)
    private SecretResponse password;

    @Schema(description = KerberosConfigModelDescription.KERBEROS_PRINCIPAL)
    private SecretResponse principal;

    @Schema(description = KerberosConfigModelDescription.DESCRIPTOR)
    private SecretResponse descriptor;

    @Schema(description = KerberosConfigModelDescription.KRB_5_CONF)
    private SecretResponse krb5Conf;

    public String getEnvironmentCrn() {
        return environmentCrn;
    }

    public void setEnvironmentCrn(String environmentCrn) {
        this.environmentCrn = environmentCrn;
    }

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

    public String getCrn() {
        return crn;
    }

    public void setCrn(String crn) {
        this.crn = crn;
    }

    public SecretResponse getPassword() {
        return password;
    }

    public void setPassword(SecretResponse password) {
        this.password = password;
    }

    public SecretResponse getPrincipal() {
        return principal;
    }

    public void setPrincipal(SecretResponse principal) {
        this.principal = principal;
    }

    public SecretResponse getDescriptor() {
        return descriptor;
    }

    public void setDescriptor(SecretResponse descriptor) {
        this.descriptor = descriptor;
    }

    public SecretResponse getKrb5Conf() {
        return krb5Conf;
    }

    public void setKrb5Conf(SecretResponse krb5Conf) {
        this.krb5Conf = krb5Conf;
    }
}
