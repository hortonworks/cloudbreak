package com.sequenceiq.cloudbreak.api.endpoint.requests;

import java.util.Set;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.sequenceiq.cloudbreak.api.model.CustomConfigPropertyParameters;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CustomConfigsV4Request {

    @ApiModelProperty(value = ModelDescriptions.CustomConfigsModelDescription.CUSTOM_CONFIGS_NAME)
    @Size(min = 1, max = 100, message = "Length of custom configs name must be from 1 to 100 characters and shouldn't contain semicolon and percentage symbol")
    @Pattern(regexp = "^[^;\\/%]*$")
    @NotNull(message = "Custom Configs name cannot be missing or empty")
    @NotEmpty
    private String name;

    @ApiModelProperty(value = ModelDescriptions.CustomConfigsModelDescription.CUSTOM_CONFIGS_SET)
    @NotNull(message = "Config Properties cannot be missing or empty")
    @NotEmpty
    @Valid
    private Set<CustomConfigPropertyParameters> configs;

    @ApiModelProperty(value = ModelDescriptions.CustomConfigsModelDescription.VERSION)
    private String platformVersion;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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
}
