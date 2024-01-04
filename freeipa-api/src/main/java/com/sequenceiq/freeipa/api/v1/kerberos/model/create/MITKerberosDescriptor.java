package com.sequenceiq.freeipa.api.v1.kerberos.model.create;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import com.sequenceiq.freeipa.api.v1.kerberos.doc.KerberosConfigModelDescription;
import com.sequenceiq.freeipa.api.v1.kerberos.model.KerberosType;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "MITKerberosV1Descriptor")
public class MITKerberosDescriptor extends KerberosDescriptorBase {

    @Schema(description = KerberosConfigModelDescription.KERBEROS_URL, required = true)
    @NotNull
    @NotEmpty
    private String url;

    @Schema(description = KerberosConfigModelDescription.KERBEROS_ADMIN_URL, required = true)
    @NotNull
    @NotEmpty
    private String adminUrl;

    @Schema(description = KerberosConfigModelDescription.KERBEROS_REALM, required = true)
    @NotNull
    @NotEmpty
    private String realm;

    @Schema(hidden = true)
    @Override
    public KerberosType getType() {
        return KerberosType.MIT;
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

}
