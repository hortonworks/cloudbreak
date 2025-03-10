package com.sequenceiq.freeipa.api.v1.kerberos.model.create;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import com.sequenceiq.freeipa.api.v1.kerberos.doc.KerberosConfigModelDescription;
import com.sequenceiq.freeipa.api.v1.kerberos.model.KerberosType;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "ActiveDirectoryKerberosV1Descriptor")
public class ActiveDirectoryKerberosDescriptor extends KerberosDescriptorBase {
    @Schema(description = KerberosConfigModelDescription.KERBEROS_URL, requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull
    @NotEmpty
    private String url;

    @Schema(description = KerberosConfigModelDescription.KERBEROS_ADMIN_URL, requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull
    @NotEmpty
    private String adminUrl;

    @Schema(description = KerberosConfigModelDescription.KERBEROS_REALM, requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull
    @NotEmpty
    private String realm;

    @Schema(description = KerberosConfigModelDescription.KERBEROS_LDAP_URL, requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull
    @NotEmpty
    private String ldapUrl;

    @Schema(description = KerberosConfigModelDescription.KERBEROS_CONTAINER_DN, requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull
    @NotEmpty
    private String containerDn;

    @Schema(hidden = true)
    @Override
    public KerberosType getType() {
        return KerberosType.ACTIVE_DIRECTORY;
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
}
