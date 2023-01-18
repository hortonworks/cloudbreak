package com.sequenceiq.cloudbreak.api.endpoint.v4.util.requests;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.ClusterManagerRepositoryDescription;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.StackRepositoryDescription;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class RepoConfigValidationV4Request {

    @Schema(description = ClusterManagerRepositoryDescription.BASE_URL)
    private String ambariBaseUrl;

    @Schema(description = ClusterManagerRepositoryDescription.REPO_GPG_KEY)
    private String ambariGpgKeyUrl;

    @Schema(description = StackRepositoryDescription.STACK_BASE_URL)
    private String stackBaseURL;

    @Schema(description = StackRepositoryDescription.UTILS_BASE_URL)
    private String utilsBaseURL;

    @Schema(description = StackRepositoryDescription.VDF_URL)
    private String versionDefinitionFileUrl;

    @Schema(description = StackRepositoryDescription.MPACK_URL)
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
