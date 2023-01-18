package com.sequenceiq.sdx.api.model;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SdxValidateCloudStorageRequest {

    @NotNull
    @Schema(description = ModelDescriptions.DATA_ACCESS_ROLE)
    private String dataAccessRole;

    @NotNull
    @Schema(description = ModelDescriptions.RANGER_AUDIT_ROLE)
    private String rangerAuditRole;

    @NotNull
    @Schema(description = ModelDescriptions.CREDENTIAL_CRN)
    private String credentialCrn;

    @NotNull
    @Schema(description = ModelDescriptions.CLOUD_STORAGE_DETAILS)
    private SdxCloudStorageRequest sdxCloudStorageRequest;

    @NotNull
    @Schema(description = ModelDescriptions.CLUSTER_TEMPLATE_NAME)
    private String blueprintName;

    public SdxValidateCloudStorageRequest() {
    }

    public String getCredentialCrn() {
        return credentialCrn;
    }

    public SdxCloudStorageRequest getSdxCloudStorageRequest() {
        return sdxCloudStorageRequest;
    }

    public String getBlueprintName() {
        return blueprintName;
    }

    public String getDataAccessRole() {
        return dataAccessRole;
    }

    public String getRangerAuditRole() {
        return rangerAuditRole;
    }
}
