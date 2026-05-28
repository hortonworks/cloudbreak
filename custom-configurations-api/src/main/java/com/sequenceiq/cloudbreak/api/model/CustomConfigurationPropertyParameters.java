package com.sequenceiq.cloudbreak.api.model;

import java.util.Objects;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.sequenceiq.cloudbreak.doc.ApiDescription.CustomConfigurationsJsonProperties;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CustomConfigurationPropertyParameters {

    @Schema(description = CustomConfigurationsJsonProperties.NAME)
    @NotNull
    @Size(max = 256, message = "Configuration property name must be at most 256 characters")
    @Pattern(regexp = "^[a-zA-Z][a-zA-Z0-9._-]*$",
            message = "Configuration property name must start with a letter and contain only alphanumeric, dot, underscore, or hyphen characters")
    private String name;

    @Schema(description = CustomConfigurationsJsonProperties.VALUE)
    @NotNull
    @Size(max = 4096, message = "Configuration property value must be at most 4096 characters")
    @Pattern(regexp = "^[^\\p{Cntrl}<>]*$",
            message = "Configuration property value must not contain control characters or angle brackets")
    private String value;

    @Schema
    @Size(max = 128, message = "Role type must be at most 128 characters")
    @Pattern(regexp = "^[A-Z][A-Z0-9_]*$",
            message = "Role type must start with an uppercase letter and contain only uppercase alphanumeric and underscore characters")
    private String roleType;

    @Schema
    @NotNull
    @Size(max = 128, message = "Service type must be at most 128 characters")
    @Pattern(regexp = "^[A-Z][A-Z0-9_]*$",
            message = "Service type must start with an uppercase letter and contain only uppercase alphanumeric and underscore characters")
    private String serviceType;

    public CustomConfigurationPropertyParameters() {
    }

    public CustomConfigurationPropertyParameters(String name, String value, String roleType, String serviceType) {
        this.name = name;
        this.value = value;
        this.roleType = roleType;
        this.serviceType = serviceType;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getRoleType() {
        return roleType;
    }

    public void setRoleType(String roleType) {
        this.roleType = roleType;
    }

    public String getServiceType() {
        return serviceType;
    }

    public void setServiceType(String serviceType) {
        this.serviceType = serviceType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof CustomConfigurationPropertyParameters)) {
            return false;
        }
        CustomConfigurationPropertyParameters that = (CustomConfigurationPropertyParameters) o;
        return getName().equals(that.getName()) && getValue().equals(that.getValue()) &&
                Objects.equals(getRoleType(), that.getRoleType()) && getServiceType().equals(that.getServiceType());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getName(), getValue(), getRoleType(), getServiceType());
    }
}
