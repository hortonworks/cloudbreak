package com.sequenceiq.cloudbreak.api.model;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.cloudbreak.api.model.mpack.ManagementPackDetails;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.AmbariStackDetailsDescription;
import com.sequenceiq.cloudbreak.validation.ValidAmbariStack;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ValidAmbariStack
@ApiModel("AmbariStackDetails")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class AmbariStackDetailsJson implements JsonEntity {
    @ApiModelProperty(AmbariStackDetailsDescription.STACK)
    private String stack;

    @ApiModelProperty(AmbariStackDetailsDescription.VERSION)
    private String version;

    @ApiModelProperty(AmbariStackDetailsDescription.OS)
    private String os;

    @ApiModelProperty(AmbariStackDetailsDescription.OS_TYPE)
    private String osType;

    @ApiModelProperty(AmbariStackDetailsDescription.STACK_REPO_ID)
    private String stackRepoId;

    @ApiModelProperty(AmbariStackDetailsDescription.STACK_BASE_URL)
    private String stackBaseURL;

    @ApiModelProperty(AmbariStackDetailsDescription.UTILS_REPO_ID)
    private String utilsRepoId;

    @ApiModelProperty(AmbariStackDetailsDescription.UTILS_BASE_URL)
    private String utilsBaseURL;

    @ApiModelProperty(AmbariStackDetailsDescription.ENABLE_GPL_REPO)
    private boolean enableGplRepo;

    @ApiModelProperty(AmbariStackDetailsDescription.VERIFY)
    private Boolean verify;

    @ApiModelProperty(AmbariStackDetailsDescription.REPOSITORY_VERSION)
    private String repositoryVersion;

    @ApiModelProperty(AmbariStackDetailsDescription.VDF_URL)
    private String versionDefinitionFileUrl;

    @ApiModelProperty(AmbariStackDetailsDescription.MPACK_URL)
    private String mpackUrl;

    @ApiModelProperty(AmbariStackDetailsDescription.MPACKS)
    private List<ManagementPackDetails> mpacks = new ArrayList<>();

    @ApiModelProperty(ModelDescriptions.AmbariRepoDetailsDescription.AMBARI_REPO_GPG_KEY)
    private String gpgKeyUrl;

    public String getStack() {
        return stack;
    }

    public void setStack(String stack) {
        this.stack = stack;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getOs() {
        return os;
    }

    public void setOs(String os) {
        this.os = os;
    }

    public String getOsType() {
        return osType;
    }

    public void setOsType(String osType) {
        this.osType = osType;
    }

    public String getStackRepoId() {
        return stackRepoId;
    }

    public void setStackRepoId(String stackRepoId) {
        this.stackRepoId = stackRepoId;
    }

    public String getStackBaseURL() {
        return stackBaseURL;
    }

    public void setStackBaseURL(String stackBaseURL) {
        this.stackBaseURL = stackBaseURL;
    }

    public void setVerify(Boolean verify) {
        this.verify = verify;
    }

    public Boolean getVerify() {
        return verify;
    }

    public String getUtilsRepoId() {
        return utilsRepoId;
    }

    public void setUtilsRepoId(String utilsRepoId) {
        this.utilsRepoId = utilsRepoId;
    }

    public String getUtilsBaseURL() {
        return utilsBaseURL;
    }

    public void setUtilsBaseURL(String utilsBaseURL) {
        this.utilsBaseURL = utilsBaseURL;
    }

    public String getRepositoryVersion() {
        return repositoryVersion;
    }

    public void setRepositoryVersion(String repositoryVersion) {
        this.repositoryVersion = repositoryVersion;
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

    public List<ManagementPackDetails> getMpacks() {
        return mpacks;
    }

    public void setMpacks(List<ManagementPackDetails> mpacks) {
        this.mpacks = mpacks != null ? mpacks : new ArrayList<>();
    }

    public boolean isEnableGplRepo() {
        return enableGplRepo;
    }

    public void setEnableGplRepo(boolean enableGplRepo) {
        this.enableGplRepo = enableGplRepo;
    }

    public String getGpgKeyUrl() {
        return gpgKeyUrl;
    }

    public void setGpgKeyUrl(String gpgKeyUrl) {
        this.gpgKeyUrl = gpgKeyUrl;
    }
}
