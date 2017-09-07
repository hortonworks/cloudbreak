package com.sequenceiq.cloudbreak.api.model;

import com.sequenceiq.cloudbreak.doc.ModelDescriptions.ClusterModelDescription;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel("RdsTestResult")
public class LdapTestResult implements JsonEntity {

    @ApiModelProperty(value = ClusterModelDescription.LDAP_CONNECTION_RESULT, required = true)
    private String connectionResult;

    public LdapTestResult() {

    }

    public LdapTestResult(String connectionResult) {
        this.connectionResult = connectionResult;
    }

    public String getConnectionResult() {
        return connectionResult;
    }

    public void setConnectionResult(String connectionResult) {
        this.connectionResult = connectionResult;
    }
}
