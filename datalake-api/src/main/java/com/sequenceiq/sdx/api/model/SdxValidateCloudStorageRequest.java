package com.sequenceiq.sdx.api.model;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SdxValidateCloudStorageRequest {

    @ApiModelProperty(ModelDescriptions.DATA_ACCESS_ROLE)
    @NotNull
    private String dataAccessRole;

    @ApiModelProperty(ModelDescriptions.RANGER_AUDIT_ROLE)
    @NotNull
    private String rangerAuditRole;

    @ApiModelProperty(ModelDescriptions.CREDENTIAL_CRN)
    @NotNull
    private String credentialCrn;

    @ApiModelProperty(ModelDescriptions.CLOUD_STORAGE_DETAILS)
    @NotNull
    private SdxCloudStorageRequest sdxCloudStorageRequest;

    @ApiModelProperty(ModelDescriptions.CLUSTER_TEMPLATE_NAME)
    @NotNull
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
