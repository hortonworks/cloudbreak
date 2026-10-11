package com.sequenceiq.freeipa.api.v1.freeipa.stack.model.crossrealm;

import jakarta.validation.constraints.NotEmpty;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.sequenceiq.cloudbreak.auth.crn.CrnResourceDescriptor;
import com.sequenceiq.cloudbreak.auth.security.internal.ResourceCrn;
import com.sequenceiq.cloudbreak.validation.ValidCrn;
import com.sequenceiq.service.api.doc.ModelDescriptions;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PrepareCrossRealmTrustBase {
    public static final String REALM_MATCHER = "(?i)[a-z0-9_](?:[a-z0-9_-]{0,61}[a-z0-9_])?(?:\\.[a-z0-9_](?:[a-z0-9_-]{0,61}[a-z0-9_])?)*";

    public static final String INVALID_REALM_MESSAGE = "Invalid realm format, only letters, digits, '-', '_' and '.' separated labels are allowed";

    public static final String TRUST_SECRET_MATCHER = "[A-Za-z0-9@#$*+,./:;=?_~-]{1,128}";

    public static final String INVALID_TRUST_SECRET_MESSAGE = "Invalid trust secret, only letters, digits and @#$*+,./:;=?_~- are allowed (max 128)";

    @ValidCrn(resource = CrnResourceDescriptor.ENVIRONMENT)
    @NotEmpty
    @Schema(description = ModelDescriptions.ENVIRONMENT_CRN, requiredMode = Schema.RequiredMode.REQUIRED)
    private String environmentCrn;

    @ResourceCrn
    public String getEnvironmentCrn() {
        return environmentCrn;
    }

    public void setEnvironmentCrn(String environmentCrn) {
        this.environmentCrn = environmentCrn;
    }

    @Override
    public String toString() {
        return "PrepareCrossRealmTrustBase{" +
                "environmentCrn='" + environmentCrn + '\'' +
                '}';
    }
}
