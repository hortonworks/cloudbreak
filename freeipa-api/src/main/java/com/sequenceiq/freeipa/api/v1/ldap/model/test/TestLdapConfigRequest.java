package com.sequenceiq.freeipa.api.v1.ldap.model.test;

import javax.validation.Valid;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.freeipa.api.v1.ldap.doc.LdapConfigModelDescription;
import com.sequenceiq.service.api.doc.ModelDescriptions;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "TestLdapConfigV1Request")
@JsonIgnoreProperties(ignoreUnknown = true)
public class TestLdapConfigRequest {
    @Schema(description = ModelDescriptions.ENVIRONMENT_CRN)
    private String environmentCrn;

    @Valid
    @Schema(description = LdapConfigModelDescription.VALIDATION_REQUEST)
    private MinimalLdapConfigRequest ldap;

    public String getEnvironmentCrn() {
        return environmentCrn;
    }

    public void setEnvironmentCrn(String environmentCrn) {
        this.environmentCrn = environmentCrn;
    }

    public MinimalLdapConfigRequest getLdap() {
        return ldap;
    }

    public void setLdap(MinimalLdapConfigRequest ldap) {
        this.ldap = ldap;
    }
}
