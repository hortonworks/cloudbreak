package com.sequenceiq.freeipa.api.v1.kerberosmgmt.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.sequenceiq.freeipa.api.v1.kerberosmgmt.doc.KeytabModelDescription;
import com.sequenceiq.cloudbreak.service.secret.model.SecretResponse;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel("HostKeytabV1Response")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class HostKeytabResponse {

    @ApiModelProperty (KeytabModelDescription.PRINCIPAL)
    private SecretResponse hostPrincipal;

    @ApiModelProperty (KeytabModelDescription.KEYTAB)
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
