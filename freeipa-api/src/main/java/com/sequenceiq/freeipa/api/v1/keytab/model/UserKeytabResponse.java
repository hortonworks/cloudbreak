package com.sequenceiq.freeipa.api.v1.keytab.model;

import com.sequenceiq.freeipa.api.v1.keytab.doc.KeytabModelDescription;

import io.swagger.annotations.ApiModelProperty;

public class UserKeytabResponse {

    @ApiModelProperty(KeytabModelDescription.ID)
    private int id;

    @ApiModelProperty (KeytabModelDescription.PRICIPAL)
    private String userPrincial;

    @ApiModelProperty (KeytabModelDescription.KEYTAB)
    private String keytab;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getUserPrincial() {
        return userPrincial;
    }

    public void setUserPrincial(String servicePrincial) {
        this.userPrincial = servicePrincial;
    }

    public String getKeytab() {
        return keytab;
    }

    public void setKeytab(String keytab) {
        this.keytab = keytab;
    }
}
