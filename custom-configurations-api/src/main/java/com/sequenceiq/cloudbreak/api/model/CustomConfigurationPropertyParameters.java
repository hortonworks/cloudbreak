package com.sequenceiq.cloudbreak.api.model;

import java.util.Objects;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CustomConfigurationPropertyParameters {

    @ApiModelProperty(value = ModelDescriptions.CustomConfigurationsModelDescription.NAME)
    @NotNull
    private String name;

    @ApiModelProperty(value = ModelDescriptions.CustomConfigurationsModelDescription.VALUE)
    @NotNull
    private String value;

    @ApiModelProperty
    private String roleType;

    @ApiModelProperty
    @NotNull
    private String serviceType;

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
