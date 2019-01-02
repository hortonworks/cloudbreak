package com.sequenceiq.cloudbreak.api.endpoint.v4.ldaps.responses;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.api.endpoint.v4.JsonEntity;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.ClusterModelDescription;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
public class LdapTestV4Response implements JsonEntity {

    @ApiModelProperty(value = ClusterModelDescription.LDAP_CONNECTION_RESULT, required = true)
    private String result;

    public LdapTestV4Response() {

    }

    public LdapTestV4Response(String result) {
        this.result = result;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }
}
