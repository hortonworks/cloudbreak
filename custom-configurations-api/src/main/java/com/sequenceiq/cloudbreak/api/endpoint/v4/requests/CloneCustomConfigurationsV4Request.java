package com.sequenceiq.cloudbreak.api.endpoint.v4.requests;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CloneCustomConfigurationsV4Request {

    @ApiModelProperty(value = ModelDescriptions.CustomConfigurationsModelDescription.CUSTOM_CONFIGURATIONS_NAME)
    @Size(min = 1, max = 100,
            message = "Length of custom configurations name must be from 1 to 100 characters and shouldn't contain semicolon and percentage symbol")
    @Pattern(regexp = "^[^;\\/%]*$")
    @NotNull
    private String name;

    @ApiModelProperty(value = ModelDescriptions.CustomConfigurationsModelDescription.RUNTIME_VERSION)
    private String runtimeVersion;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRuntimeVersion() {
        return runtimeVersion;
    }

    public void setRuntimeVersion(String runtimeVersion) {
        this.runtimeVersion = runtimeVersion;
    }
}
