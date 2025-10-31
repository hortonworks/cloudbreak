package com.sequenceiq.distrox.api.v1.distrox.model.cluster;

import java.io.Serializable;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.ClusterModelDescription;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.StackModelDescription;
import com.sequenceiq.common.api.cloudstorage.CloudStorageRequest;
import com.sequenceiq.distrox.api.v1.distrox.model.cluster.cm.ClouderaManagerV1Request;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class DistroXClusterV1Request implements Serializable {

    @Size(max = 15, min = 5, message = "The length of the username has to be in the range of 5 to 15 characters")
    @Pattern(regexp = "(^[a-z][-a-z0-9]*[a-z0-9]$)",
            message = "The username may only contain lowercase alphanumeric characters and hyphens, has to start with a letter and end with an " +
                    "alphanumeric character")
    @Schema(description = StackModelDescription.USERNAME)
    private String userName;

    @Pattern.List({
            @Pattern(regexp = "^.*[a-zA-Z].*$", message = "The password should contain at least one letter."),
            @Pattern(regexp = "^.*[0-9].*$", message = "The password should contain at least one number.")
    })
    @Size(max = 100, min = 8, message = "The length of the password has to be in the range of 8 to 100 characters")
    @Schema(description = StackModelDescription.PASSWORD)
    private String password;

    @Schema(description = ClusterModelDescription.RDSCONFIG_NAMES)
    private Set<String> databases = new HashSet<>();

    @Schema(description = ClusterModelDescription.PROXY_NAME)
    private String proxy;

    @Valid
    @Schema(description = StackModelDescription.CLOUD_STORAGE)
    private CloudStorageRequest cloudStorage;

    @Valid
    @Schema(description = ClusterModelDescription.CM_REQUEST)
    private ClouderaManagerV1Request cm;

    private List<String> exposedServices;

    @NotNull
    @NotEmpty
    @Schema(description = ClusterModelDescription.BLUEPRINT_NAME)
    private String blueprintName;

    @Schema(description = ClusterModelDescription.VALIDATE_BLUEPRINT)
    private Boolean validateBlueprint = Boolean.FALSE;

    @Schema(description = StackModelDescription.CUSTOM_CONFIGURATIONS_NAME)
    private String customConfigurationsName;

    @Schema(description = ClusterModelDescription.ENCRYPTION_PROFILE_NAME)
    private String encryptionProfileName;

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

    public CloudStorageRequest getCloudStorage() {
        return cloudStorage;
    }

    public void setCloudStorage(CloudStorageRequest cloudStorage) {
        this.cloudStorage = cloudStorage;
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

    public String getCustomConfigurationsName() {
        return customConfigurationsName;
    }

    public void setCustomConfigurationsName(String customConfigurationsName) {
        this.customConfigurationsName = customConfigurationsName;
    }

    public String getEncryptionProfileName() {
        return encryptionProfileName;
    }

    public void setEncryptionProfileName(String encryptionProfileName) {
        this.encryptionProfileName = encryptionProfileName;
    }
}
