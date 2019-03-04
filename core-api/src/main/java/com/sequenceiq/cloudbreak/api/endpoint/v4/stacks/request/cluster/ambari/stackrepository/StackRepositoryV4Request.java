package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.ambari.stackrepository;

import java.util.ArrayList;
import java.util.List;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.cloudbreak.api.endpoint.v4.JsonEntity;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.repository.RepositoryV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.ambari.stackrepository.mpack.ManagementPackDetailsV4Request;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.StackRepositoryDescription;
import com.sequenceiq.cloudbreak.validation.ValidAmbariStack;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ValidAmbariStack
@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class StackRepositoryV4Request implements JsonEntity {

    @ApiModelProperty
    private RepositoryV4Request repository;

    @NotNull
    @ApiModelProperty(value = StackRepositoryDescription.STACK, required = true)
    private String stack;

    @NotNull
    @ApiModelProperty(value = StackRepositoryDescription.VERSION, required = true)
    @Pattern(regexp = "(^[0-9]+\\.[0-9]+$)",
            message = "Valid stack version is in MAJOR.MINOR format eg.: 2.6")
    private String version;

    @ApiModelProperty(StackRepositoryDescription.OS)
    private String os;

    @ApiModelProperty(StackRepositoryDescription.OS_TYPE)
    private String osType;

    @ApiModelProperty(StackRepositoryDescription.STACK_REPO_ID)
    private String repoId;

    @ApiModelProperty(StackRepositoryDescription.UTILS_REPO_ID)
    private String utilsRepoId;

    @ApiModelProperty(StackRepositoryDescription.UTILS_BASE_URL)
    private String utilsBaseURL;

    @ApiModelProperty(StackRepositoryDescription.ENABLE_GPL_REPO)
    private boolean enableGplRepo;

    @ApiModelProperty(StackRepositoryDescription.VERIFY)
    private Boolean verify;

    @ApiModelProperty(StackRepositoryDescription.VDF_URL)
    private String versionDefinitionFileUrl;

    @ApiModelProperty(StackRepositoryDescription.MPACK_URL)
    @Pattern(regexp = "^http[s]?://.*", message = "The URL should start with the protocol (http, https)")
    private String mpackUrl;

    @ApiModelProperty(StackRepositoryDescription.MPACKS)
    private List<ManagementPackDetailsV4Request> mpacks = new ArrayList<>();

    public RepositoryV4Request getRepository() {
        return repository;
    }

    public void setRepository(RepositoryV4Request repository) {
        this.repository = repository;
    }

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

    public String getRepoId() {
        return repoId;
    }

    public void setRepoId(String repoId) {
        this.repoId = repoId;
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

    public boolean isEnableGplRepo() {
        return enableGplRepo;
    }

    public void setEnableGplRepo(boolean enableGplRepo) {
        this.enableGplRepo = enableGplRepo;
    }

    public Boolean getVerify() {
        return verify;
    }

    public void setVerify(Boolean verify) {
        this.verify = verify;
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

    public List<ManagementPackDetailsV4Request> getMpacks() {
        return mpacks;
    }

    public void setMpacks(List<ManagementPackDetailsV4Request> mpacks) {
        this.mpacks = mpacks;
    }

    public boolean customRepoSpecified() {
        boolean vdfFileSpecified = StringUtils.isNoneEmpty(versionDefinitionFileUrl);
        boolean repositoriesSpecified = repository != null && StringUtils.isNoneEmpty(repository.getBaseUrl())
                && StringUtils.isNoneEmpty(utilsBaseURL)
                && StringUtils.isNoneEmpty(utilsRepoId);

        return vdfFileSpecified || repositoriesSpecified;
    }
}
