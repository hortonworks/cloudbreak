package com.sequenceiq.cloudbreak.api.endpoint.v4.responses;

import java.util.Set;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.sequenceiq.cloudbreak.api.model.CustomConfigurationPropertyParameters;
import com.sequenceiq.cloudbreak.doc.ApiDescription.CustomConfigurationsJsonProperties;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CustomConfigurationsV4Response {

    @Schema(description = CustomConfigurationsJsonProperties.CUSTOM_CONFIGURATIONS_NAME)
    @NotNull
    @Size(min = 1, max = 100, message = "Length of custom configs name must be from 1 to 100 characters and shouldn't contain semicolon and percentage symbol")
    @Pattern(regexp = "^[^;\\/%]*$")
    private String name;

    @Schema(description = CustomConfigurationsJsonProperties.CRN)
    @NotNull
    private String crn;

    @Schema(description = CustomConfigurationsJsonProperties.CONFIGURATION_PROPERTIES)
    @NotNull
    private Set<CustomConfigurationPropertyParameters> configurations;

    @Schema(description = CustomConfigurationsJsonProperties.RUNTIME_VERSION)
    private String runtimeVersion;

    @Schema
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
