package com.sequenceiq.sdx.api.model;

import javax.validation.constraints.NotNull;

public class SdxValidateCloudStorageRequest {

    @NotNull
    private String dataAccessRole;

    @NotNull
    private String rangerAuditRole;

    @NotNull
    private String credentialCrn;

    @NotNull
    private SdxCloudStorageRequest sdxCloudStorageRequest;

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
