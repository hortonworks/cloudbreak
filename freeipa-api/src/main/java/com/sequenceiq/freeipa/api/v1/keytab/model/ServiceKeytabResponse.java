package com.sequenceiq.freeipa.api.v1.keytab.model;

import com.sequenceiq.freeipa.api.v1.keytab.doc.KeytabModelDescription;

import io.swagger.annotations.ApiModelProperty;

public class ServiceKeytabResponse {

    @ApiModelProperty(KeytabModelDescription.ID)
    private int id;

    @ApiModelProperty (KeytabModelDescription.PRICIPAL)
    private String servicePrincial;

    @ApiModelProperty (KeytabModelDescription.KEYTAB)
    private String keytab;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getServicePrincial() {
        return servicePrincial;
    }

    public void setServicePrincial(String servicePrincial) {
        this.servicePrincial = servicePrincial;
    }

    public String getKeytab() {
        return keytab;
    }

    public void setKeytab(String keytab) {
        this.keytab = keytab;
    }
}
