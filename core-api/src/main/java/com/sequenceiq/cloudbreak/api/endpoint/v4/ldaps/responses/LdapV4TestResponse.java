package com.sequenceiq.cloudbreak.api.endpoint.v4.ldaps.responses;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.api.model.JsonEntity;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.ClusterModelDescription;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
public class LdapV4TestResponse implements JsonEntity {

    @ApiModelProperty(value = ClusterModelDescription.LDAP_CONNECTION_RESULT, required = true)
    private String connectionResult;

    public LdapV4TestResponse() {

    }

    public LdapV4TestResponse(String connectionResult) {
        this.connectionResult = connectionResult;
    }

    public String getConnectionResult() {
        return connectionResult;
    }

    public void setConnectionResult(String connectionResult) {
        this.connectionResult = connectionResult;
    }
}
