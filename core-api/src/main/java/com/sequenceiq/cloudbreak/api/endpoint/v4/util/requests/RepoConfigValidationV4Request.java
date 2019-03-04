package com.sequenceiq.cloudbreak.api.endpoint.v4.util.requests;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.ClusterManagerRepositoryDescription;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.StackRepositoryDescription;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class RepoConfigValidationV4Request {

    @ApiModelProperty(ClusterManagerRepositoryDescription.BASE_URL)
    private String ambariBaseUrl;

    @ApiModelProperty(ClusterManagerRepositoryDescription.REPO_GPG_KEY)
    private String ambariGpgKeyUrl;

    @ApiModelProperty(StackRepositoryDescription.STACK_BASE_URL)
    private String stackBaseURL;

    @ApiModelProperty(StackRepositoryDescription.UTILS_BASE_URL)
    private String utilsBaseURL;

    @ApiModelProperty(StackRepositoryDescription.VDF_URL)
    private String versionDefinitionFileUrl;

    @ApiModelProperty(StackRepositoryDescription.MPACK_URL)
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
