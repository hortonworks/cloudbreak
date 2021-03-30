package com.sequenceiq.cloudbreak.api.endpoint.v4.responses;


import java.util.Set;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.sequenceiq.cloudbreak.api.model.CustomConfigurationPropertyParameters;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.CustomConfigurationsModelDescription;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CustomConfigurationsV4Response {

    @ApiModelProperty(value = CustomConfigurationsModelDescription.CUSTOM_CONFIGURATIONS_NAME)
    @NotNull
    @Size(min = 1, max = 100, message = "Length of custom configs name must be from 1 to 100 characters and shouldn't contain semicolon and percentage symbol")
    @Pattern(regexp = "^[^;\\/%]*$")
    private String name;

    @ApiModelProperty(value = CustomConfigurationsModelDescription.CRN)
    @NotNull
    private String crn;

    @ApiModelProperty(value = CustomConfigurationsModelDescription.CONFIGURATION_PROPERTIES)
    @NotNull
    private Set<CustomConfigurationPropertyParameters> configurations;

    @ApiModelProperty(value = CustomConfigurationsModelDescription.RUNTIME_VERSION)
    private String runtimeVersion;

    @ApiModelProperty
    @NotNull
    private String account;

    @NotNull
    private Long created;

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

    public String getCrn() {
        return crn;
    }

    public void setCrn(String crn) {
        this.crn = crn;
    }

    public Set<CustomConfigurationPropertyParameters> getConfigurations() {
        return configurations;
    }

    public void setConfigurations(Set<CustomConfigurationPropertyParameters> configurations) {
        this.configurations = configurations;
    }

    public String getRuntimeVersion() {
        return runtimeVersion;
    }

    public void setRuntimeVersion(String runtimeVersion) {
        this.runtimeVersion = runtimeVersion;
    }

    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
    }
}
