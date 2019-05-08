package com.sequenceiq.environment.api.ldap.model.response;

import static com.sequenceiq.environment.api.ldap.doc.LdapConfigModelDescription.LDAP_CONNECTION_RESULT;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
public class LdapTestV1Response implements Serializable {

    @ApiModelProperty(value = LDAP_CONNECTION_RESULT, required = true)
    private String result;

    public LdapTestV1Response() {

    }

    public LdapTestV1Response(String result) {
        this.result = result;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }
}
