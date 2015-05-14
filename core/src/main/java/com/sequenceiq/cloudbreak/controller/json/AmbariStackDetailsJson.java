package com.sequenceiq.cloudbreak.controller.json;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.controller.doc.ModelDescriptions.AmbariStackDetailsDescription;
import com.wordnik.swagger.annotations.ApiModel;
import com.wordnik.swagger.annotations.ApiModelProperty;

@ApiModel("AmbariStackDetails")
@JsonIgnoreProperties(ignoreUnknown = true)
public class AmbariStackDetailsJson {

    @NotNull
    @ApiModelProperty(value = AmbariStackDetailsDescription.STACK, required = true)
    private String stack;
    @NotNull
    @ApiModelProperty(value = AmbariStackDetailsDescription.VERSION, required = true)
    private String version;
    @NotNull
    @ApiModelProperty(value = AmbariStackDetailsDescription.OS, required = true)
    private String os;
    @NotNull
    @ApiModelProperty(value = AmbariStackDetailsDescription.REPO_ID, required = true)
    private String repoId;
    @NotNull
    @ApiModelProperty(value = AmbariStackDetailsDescription.BASE_URL, required = true)
    private String baseURL;
    @NotNull
    @ApiModelProperty(value = AmbariStackDetailsDescription.VERIFY, required = true)
    private Boolean verify;

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

    public String getRepoId() {
        return repoId;
    }

    public void setRepoId(String repoId) {
        this.repoId = repoId;
    }

    public String getBaseURL() {
        return baseURL;
    }

    public void setBaseURL(String baseURL) {
        this.baseURL = baseURL;
    }

    public void setVerify(Boolean verify) {
        this.verify = verify;
    }

    public Boolean getVerify() {
        return verify;
    }
}
