package com.sequenceiq.freeipa.api.v1.kerberosmgmt.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.sequenceiq.cloudbreak.service.secret.model.SecretResponse;
import com.sequenceiq.freeipa.api.v1.kerberosmgmt.doc.KeytabModelDescription;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "ServiceKeytabV1Response")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ServiceKeytabResponse {

    @Schema(description = KeytabModelDescription.PRINCIPAL)
    private SecretResponse servicePrincipal;

    @Schema(description = KeytabModelDescription.KEYTAB)
    private SecretResponse keytab;

    public SecretResponse getServicePrincipal() {
        return servicePrincipal;
    }

    public void setServicePrincipal(SecretResponse servicePrincipal) {
        this.servicePrincipal = servicePrincipal;
    }

    public SecretResponse getKeytab() {
        return keytab;
    }

    public void setKeytab(SecretResponse keytab) {
        this.keytab = keytab;
    }
}
