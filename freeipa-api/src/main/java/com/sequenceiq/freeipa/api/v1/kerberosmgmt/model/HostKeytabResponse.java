package com.sequenceiq.freeipa.api.v1.kerberosmgmt.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.sequenceiq.cloudbreak.service.secret.model.SecretResponse;
import com.sequenceiq.freeipa.api.v1.kerberosmgmt.doc.KeytabModelDescription;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "HostKeytabV1Response")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class HostKeytabResponse {

    @Schema(description = KeytabModelDescription.PRINCIPAL)
    private SecretResponse hostPrincipal;

    @Schema(description = KeytabModelDescription.KEYTAB)
    private SecretResponse keytab;

    public SecretResponse getHostPrincipal() {
        return hostPrincipal;
    }

    public void setHostPrincipal(SecretResponse hostPrincipal) {
        this.hostPrincipal = hostPrincipal;
    }

    public SecretResponse getKeytab() {
        return keytab;
    }

    public void setKeytab(SecretResponse keytab) {
        this.keytab = keytab;
    }
}
