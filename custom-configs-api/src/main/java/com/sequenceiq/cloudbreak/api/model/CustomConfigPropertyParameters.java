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
public class CustomConfigPropertyParameters {

    @ApiModelProperty(value = ModelDescriptions.CustomConfigsModelDescription.CUSTOM_CONFIG_NAME)
    @NotNull
    private String configName;

    @ApiModelProperty(value = ModelDescriptions.CustomConfigsModelDescription.CUSTOM_CONFIG_VALUE)
    @NotNull
    private String configValue;

    @ApiModelProperty
    private String roleType;

    @ApiModelProperty
    @NotNull
    private String serviceType;

    public String getConfigName() {
        return configName;
    }

    public void setConfigName(String configName) {
        this.configName = configName;
    }

    public String getConfigValue() {
        return configValue;
    }

    public void setConfigValue(String configValue) {
        this.configValue = configValue;
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
        if (!(o instanceof CustomConfigPropertyParameters)) {
            return false;
        }
        CustomConfigPropertyParameters that = (CustomConfigPropertyParameters) o;
        return getConfigName().equals(that.getConfigName()) && getConfigValue().equals(that.getConfigValue()) &&
                Objects.equals(getRoleType(), that.getRoleType()) && getServiceType().equals(that.getServiceType());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getConfigName(), getConfigValue(), getRoleType(), getServiceType());
    }
}
