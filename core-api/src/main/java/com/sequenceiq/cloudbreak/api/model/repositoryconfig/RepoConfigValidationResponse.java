package com.sequenceiq.cloudbreak.api.model.repositoryconfig;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel("RepoConfigValidationResponse")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RepoConfigValidationResponse {

    @ApiModelProperty(value = ModelDescriptions.RepositoryConfigValidationDescription.FIELDS)
    private Boolean ambariBaseUrl;

    @ApiModelProperty(value = ModelDescriptions.RepositoryConfigValidationDescription.FIELDS)
    private Boolean ambariGpgKeyUrl;

    @ApiModelProperty(value = ModelDescriptions.RepositoryConfigValidationDescription.FIELDS)
    private Boolean stackBaseURL;

    @ApiModelProperty(value = ModelDescriptions.RepositoryConfigValidationDescription.FIELDS)
    private Boolean utilsBaseURL;

    @ApiModelProperty(value = ModelDescriptions.RepositoryConfigValidationDescription.FIELDS)
    private Boolean versionDefinitionFileUrl;

    @ApiModelProperty(value = ModelDescriptions.RepositoryConfigValidationDescription.FIELDS)
    private Boolean mpackUrl;

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
