package com.sequenceiq.cloudbreak.api.model.repositoryconfig;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel("RepoConfigValidationRequest")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RepoConfigValidationRequest {

    @ApiModelProperty(value = ModelDescriptions.AmbariRepoDetailsDescription.AMBARI_BASE_URL)
    private String ambariBaseUrl;

    @ApiModelProperty(value = ModelDescriptions.AmbariRepoDetailsDescription.AMBARI_REPO_GPG_KEY)
    private String ambariGpgKeyUrl;

    @ApiModelProperty(value = ModelDescriptions.AmbariStackDetailsDescription.STACK_BASE_URL)
    private String stackBaseURL;

    @ApiModelProperty(value = ModelDescriptions.AmbariStackDetailsDescription.UTILS_BASE_URL)
    private String utilsBaseURL;

    @ApiModelProperty(value = ModelDescriptions.AmbariStackDetailsDescription.VDF_URL)
    private String versionDefinitionFileUrl;

    @ApiModelProperty(value = ModelDescriptions.AmbariStackDetailsDescription.MPACK_URL)
    private String mpackUrl;

    public String getAmbariBaseUrl() {
        return ambariBaseUrl;
    }

    public void setAmbariBaseUrl(String ambariBaseUrl) {
        this.ambariBaseUrl = ambariBaseUrl;
    }

    public String getAmbariGpgKeyUrl() {
        return ambariGpgKeyUrl;
    }

    public void setAmbariGpgKeyUrl(String ambariGpgKeyUrl) {
        this.ambariGpgKeyUrl = ambariGpgKeyUrl;
    }

    public String getStackBaseURL() {
        return stackBaseURL;
    }

    public void setStackBaseURL(String stackBaseURL) {
        this.stackBaseURL = stackBaseURL;
    }

    public String getUtilsBaseURL() {
        return utilsBaseURL;
    }

    public void setUtilsBaseURL(String utilsBaseURL) {
        this.utilsBaseURL = utilsBaseURL;
    }

    public String getVersionDefinitionFileUrl() {
        return versionDefinitionFileUrl;
    }

    public void setVersionDefinitionFileUrl(String versionDefinitionFileUrl) {
        this.versionDefinitionFileUrl = versionDefinitionFileUrl;
    }

    public String getMpackUrl() {
        return mpackUrl;
    }

    public void setMpackUrl(String mpackUrl) {
        this.mpackUrl = mpackUrl;
    }
}
