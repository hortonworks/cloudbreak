package com.sequenceiq.freeipa.api.v1.kerberosmgmt.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.sequenceiq.freeipa.api.v1.kerberosmgmt.doc.KeytabModelDescription;
import com.sequenceiq.cloudbreak.service.secret.model.SecretResponse;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel("ServiceKeytabV1Response")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ServiceKeytabResponse {

    @ApiModelProperty (KeytabModelDescription.PRINCIPAL)
    private SecretResponse servicePrincial;

    @ApiModelProperty (KeytabModelDescription.KEYTAB)
    private SecretResponse keytab;

    public SecretResponse getServicePrincial() {
        return servicePrincial;
    }

    public void setServicePrincial(SecretResponse servicePrincial) {
        this.servicePrincial = servicePrincial;
    }

    public SecretResponse getKeytab() {
        return keytab;
    }

    public void setKeytab(SecretResponse keytab) {
        this.keytab = keytab;
    }
}
