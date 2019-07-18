package com.sequenceiq.freeipa.api.v1.kerberosmgmt.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.sequenceiq.freeipa.api.v1.kerberosmgmt.doc.KeytabModelDescription;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel("ServiceKeytabV1Response")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ServiceKeytabResponse {

    @ApiModelProperty (KeytabModelDescription.PRINCIPAL)
    private String servicePrincipal;

    @ApiModelProperty (KeytabModelDescription.KEYTAB)
    private String keytab;

    public String getServicePrincipal() {
        return servicePrincipal;
    }

    public void setServicePrincipal(String servicePrincipal) {
        this.servicePrincipal = servicePrincipal;
    }

    public String getKeytab() {
        return keytab;
    }

    public void setKeytab(String keytab) {
        this.keytab = keytab;
    }
}
