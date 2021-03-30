package com.sequenceiq.cloudbreak.api.endpoint.responses;


import java.util.Set;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import com.sequenceiq.cloudbreak.api.model.CustomConfigPropertyParameters;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.CustomConfigsModelDescription;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
public class CustomConfigsV4Response {

    @ApiModelProperty(value = CustomConfigsModelDescription.CUSTOM_CONFIGS_NAME)
    @NotNull
    @Size(min = 1, max = 100, message = "Length of custom configs name must be from 1 to 100 characters and shouldn't contain semicolon and percentage symbol")
    @Pattern(regexp = "^[^;\\/%]*$")
    private String name;

    @ApiModelProperty(value = CustomConfigsModelDescription.CRN)
    @NotNull
    private String resourceCrn;

    @ApiModelProperty(value = CustomConfigsModelDescription.CUSTOM_CONFIGS_SET)
    @NotNull
    private Set<CustomConfigPropertyParameters> configs;

    @ApiModelProperty(value = CustomConfigsModelDescription.VERSION)
    private String platformVersion;

    @ApiModelProperty
    @NotNull
    private String account;

    @NotNull
    private Long created;

    private Long lastModified;

    public Long getLastModified() {
        return lastModified;
    }

    public void setLastModified(Long lastModified) {
        this.lastModified = lastModified;
    }

    public Long getCreated() {
        return created;
    }

    public void setCreated(Long created) {
        this.created = created;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getResourceCrn() {
        return resourceCrn;
    }

    public void setResourceCrn(String resourceCrn) {
        this.resourceCrn = resourceCrn;
    }

    public Set<CustomConfigPropertyParameters> getConfigs() {
        return configs;
    }

    public void setConfigs(Set<CustomConfigPropertyParameters> configs) {
        this.configs = configs;
    }

    public String getPlatformVersion() {
        return platformVersion;
    }

    public void setPlatformVersion(String platformVersion) {
        this.platformVersion = platformVersion;
    }

    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
    }
}
