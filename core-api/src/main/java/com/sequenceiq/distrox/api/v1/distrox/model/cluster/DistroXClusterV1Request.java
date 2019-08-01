package com.sequenceiq.distrox.api.v1.distrox.model.cluster;

import java.io.Serializable;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.ClusterModelDescription;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.StackModelDescription;
import com.sequenceiq.common.api.cloudstorage.StorageLocationBase;
import com.sequenceiq.distrox.api.v1.distrox.model.cluster.cm.ClouderaManagerV1Request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class DistroXClusterV1Request implements Serializable {

    @Size(max = 15, min = 5, message = "The length of the username has to be in range of 5 to 15")
    @Pattern(regexp = "(^[a-z][-a-z0-9]*[a-z0-9]$)",
            message = "The username can only contain lowercase alphanumeric characters and hyphens and has start with an alphanumeric character")
    @NotNull
    @ApiModelProperty(value = StackModelDescription.USERNAME, required = true)
    private String userName;

    @NotNull
    @Pattern.List({
            @Pattern(regexp = "^.*[a-zA-Z].*$", message = "The password should contain at least one letter."),
            @Pattern(regexp = "^.*[0-9].*$", message = "The password should contain at least one number.")
    })
    @Size(max = 100, min = 8, message = "The length of the password has to be in range of 8 to 100")
    @ApiModelProperty(value = StackModelDescription.PASSWORD, required = true)
    private String password;

    @ApiModelProperty(ClusterModelDescription.RDSCONFIG_NAMES)
    private Set<String> databases = new HashSet<>();

    @ApiModelProperty(ClusterModelDescription.PROXY_NAME)
    private String proxy;

    @ApiModelProperty(ClusterModelDescription.CLOUD_STORAGE_LOCATIONS)
    private Set<StorageLocationBase> cloudStorageLocations = new HashSet<>();

    @Valid
    @ApiModelProperty(ClusterModelDescription.CM_REQUEST)
    private ClouderaManagerV1Request cm;

    private List<String> exposedServices;

    @ApiModelProperty(ClusterModelDescription.BLUEPRINT_NAME)
    private String blueprintName;

    @ApiModelProperty(ClusterModelDescription.VALIDATE_BLUEPRINT)
    private Boolean validateBlueprint = Boolean.FALSE;

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Set<String> getDatabases() {
        return databases;
    }

    public void setDatabases(Set<String> databases) {
        this.databases = databases;
    }

    public Set<StorageLocationBase> getCloudStorageLocations() {
        return cloudStorageLocations;
    }

    public void setCloudStorageLocations(Set<StorageLocationBase> cloudStorageLocations) {
        this.cloudStorageLocations = cloudStorageLocations;
    }

    public ClouderaManagerV1Request getCm() {
        return cm;
    }

    public void setCm(ClouderaManagerV1Request cm) {
        this.cm = cm;
    }

    public List<String> getExposedServices() {
        return exposedServices;
    }

    public void setExposedServices(List<String> exposedServices) {
        this.exposedServices = exposedServices;
    }

    public String getBlueprintName() {
        return blueprintName;
    }

    public void setBlueprintName(String blueprintName) {
        this.blueprintName = blueprintName;
    }

    public String getProxy() {
        return proxy;
    }

    public void setProxy(String proxy) {
        this.proxy = proxy;
    }

    public Boolean getValidateBlueprint() {
        return validateBlueprint;
    }

    public void setValidateBlueprint(Boolean validateBlueprint) {
        this.validateBlueprint = validateBlueprint;
    }
}
