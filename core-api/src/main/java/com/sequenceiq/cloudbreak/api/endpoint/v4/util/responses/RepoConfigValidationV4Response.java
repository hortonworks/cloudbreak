package com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.RepositoryConfigValidationDescription;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class RepoConfigValidationV4Response {

    @Schema(description = RepositoryConfigValidationDescription.FIELDS, requiredMode = Schema.RequiredMode.REQUIRED)
    private Boolean ambariBaseUrl = Boolean.FALSE;

    @Schema(description = RepositoryConfigValidationDescription.FIELDS, requiredMode = Schema.RequiredMode.REQUIRED)
    private Boolean ambariGpgKeyUrl = Boolean.FALSE;

    @Schema(description = RepositoryConfigValidationDescription.FIELDS, requiredMode = Schema.RequiredMode.REQUIRED)
    private Boolean stackBaseURL = Boolean.FALSE;

    @Schema(description = RepositoryConfigValidationDescription.FIELDS, requiredMode = Schema.RequiredMode.REQUIRED)
    private Boolean utilsBaseURL = Boolean.FALSE;

    @Schema(description = RepositoryConfigValidationDescription.FIELDS, requiredMode = Schema.RequiredMode.REQUIRED)
    private Boolean versionDefinitionFileUrl = Boolean.FALSE;

    @Schema(description = RepositoryConfigValidationDescription.FIELDS, requiredMode = Schema.RequiredMode.REQUIRED)
    private Boolean mpackUrl = Boolean.FALSE;

    public Boolean getAmbariBaseUrl() {
        return ambariBaseUrl;
    }

    public void setAmbariBaseUrl(Boolean ambariBaseUrl) {
        this.ambariBaseUrl = ambariBaseUrl;
    }

    public Boolean getAmbariGpgKeyUrl() {
        return ambariGpgKeyUrl;
    }

    public void setAmbariGpgKeyUrl(Boolean ambariGpgKeyUrl) {
        this.ambariGpgKeyUrl = ambariGpgKeyUrl;
    }

    public Boolean getStackBaseURL() {
        return stackBaseURL;
    }

    public void setStackBaseURL(Boolean stackBaseURL) {
        this.stackBaseURL = stackBaseURL;
    }

    public Boolean getUtilsBaseURL() {
        return utilsBaseURL;
    }

    public void setUtilsBaseURL(Boolean utilsBaseURL) {
        this.utilsBaseURL = utilsBaseURL;
    }

    public Boolean getVersionDefinitionFileUrl() {
        return versionDefinitionFileUrl;
    }

    public void setVersionDefinitionFileUrl(Boolean versionDefinitionFileUrl) {
        this.versionDefinitionFileUrl = versionDefinitionFileUrl;
    }

    public Boolean getMpackUrl() {
        return mpackUrl;
    }

    public void setMpackUrl(Boolean mpackUrl) {
        this.mpackUrl = mpackUrl;
    }
}
