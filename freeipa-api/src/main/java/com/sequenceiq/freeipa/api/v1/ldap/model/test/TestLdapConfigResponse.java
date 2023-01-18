package com.sequenceiq.freeipa.api.v1.ldap.model.test;

import static com.sequenceiq.freeipa.api.v1.ldap.doc.LdapConfigModelDescription.LDAP_CONNECTION_RESULT;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "TestLdapConfigV1Response")
@JsonIgnoreProperties(ignoreUnknown = true)
public class TestLdapConfigResponse {
    @Schema(description = LDAP_CONNECTION_RESULT, required = true)
    private String result;

    public TestLdapConfigResponse() {

    }

    public TestLdapConfigResponse(String result) {
        this.result = result;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }
}
