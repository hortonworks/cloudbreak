package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster;

import java.util.HashSet;
import java.util.Set;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.ExecutorType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.cm.ClouderaManagerV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.customcontainer.CustomContainerV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.gateway.GatewayV4Request;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.ClusterModelDescription;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.StackModelDescription;
import com.sequenceiq.common.api.cloudstorage.CloudStorageRequest;
import com.sequenceiq.common.model.JsonEntity;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class ClusterV4Request implements JsonEntity {

    @Schema(hidden = true)
    private String name;

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

    @Schema(description = ClusterModelDescription.REDBEAMS_DB_SERVER_CRN)
    private String databaseServerCrn;

    @Schema(description = ClusterModelDescription.PROXY_CRN)
    private String proxyConfigCrn;

    @Valid
    @Schema(description = StackModelDescription.CLOUD_STORAGE)
    private CloudStorageRequest cloudStorage;

    @Valid
    @Schema(description = ClusterModelDescription.CM_REQUEST)
    private ClouderaManagerV4Request cm;

    private GatewayV4Request gateway;

    @Schema(description = ClusterModelDescription.CUSTOM_CONTAINERS)
    private CustomContainerV4Request customContainer;

    @Schema(description = ClusterModelDescription.CUSTOM_QUEUE)
    private String customQueue;

    @Schema(description = ClusterModelDescription.EXECUTOR_TYPE)
    @Deprecated
    private ExecutorType executorType = ExecutorType.DEFAULT;

    @Schema(description = ClusterModelDescription.BLUEPRINT_NAME)
    private String blueprintName;

    @Schema(description = ClusterModelDescription.VALIDATE_BLUEPRINT)
    private Boolean validateBlueprint = Boolean.TRUE;

    @Schema(description = StackModelDescription.CUSTOM_CONFIGURATIONS_NAME)
    private String customConfigurationsName;

    @Schema(description = ClusterModelDescription.ENABLE_RANGER_RAZ)
    private boolean rangerRazEnabled;

    @Schema(description = ClusterModelDescription.ENABLE_RANGER_RMS)
    private boolean rangerRmsEnabled;

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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDatabaseServerCrn() {
        return databaseServerCrn;
    }

    public void setDatabaseServerCrn(String databaseServerCrn) {
        this.databaseServerCrn = databaseServerCrn;
    }

    public Set<String> getDatabases() {
        return databases;
    }

    public void setDatabases(Set<String> databases) {
        this.databases = databases;
    }

    public String getProxyConfigCrn() {
        return proxyConfigCrn;
    }

    public void setProxyConfigCrn(String proxyConfigCrn) {
        this.proxyConfigCrn = proxyConfigCrn;
    }

    public CloudStorageRequest getCloudStorage() {
        return cloudStorage;
    }

    public void setCloudStorage(CloudStorageRequest cloudStorage) {
        this.cloudStorage = cloudStorage;
    }

    public ClouderaManagerV4Request getCm() {
        return cm;
    }

    public void setCm(ClouderaManagerV4Request cm) {
        this.cm = cm;
    }

    public GatewayV4Request getGateway() {
        return gateway;
    }

    public void setGateway(GatewayV4Request gateway) {
        this.gateway = gateway;
    }

    public String getCustomQueue() {
        return customQueue;
    }

    public void setCustomQueue(String customQueue) {
        this.customQueue = customQueue;
    }

    public CustomContainerV4Request getCustomContainer() {
        return customContainer;
    }

    public void setCustomContainer(CustomContainerV4Request customContainer) {
        this.customContainer = customContainer;
    }

    public String getBlueprintName() {
        return blueprintName;
    }

    public void setBlueprintName(String blueprintName) {
        this.blueprintName = blueprintName;
    }

    public String getCustomConfigurationsName() {
        return customConfigurationsName;
    }

    public void setCustomConfigurationsName(String customConfigurationsName) {
        this.customConfigurationsName = customConfigurationsName;
    }

    public Boolean getValidateBlueprint() {
        return validateBlueprint;
    }

    public void setValidateBlueprint(Boolean validateBlueprint) {
        this.validateBlueprint = validateBlueprint;
    }

    public boolean isRangerRazEnabled() {
        return rangerRazEnabled;
    }

    public void setRangerRazEnabled(boolean rangerRazEnabled) {
        this.rangerRazEnabled = rangerRazEnabled;
    }

    public boolean isRangerRmsEnabled() {
        return rangerRmsEnabled;
    }

    public void setRangerRmsEnabled(boolean rangerRmsEnabled) {
        this.rangerRmsEnabled = rangerRmsEnabled;
    }

    public String getEncryptionProfileName() {
        return encryptionProfileName;
    }

    public void setEncryptionProfileName(String encryptionProfileName) {
        this.encryptionProfileName = encryptionProfileName;
    }

    @Override
    public String toString() {
        return "ClusterV4Request{" +
                "name='" + name + '\'' +
                ", userName='" + userName + '\'' +
                ", databases=" + databases +
                ", databaseServerCrn='" + databaseServerCrn + '\'' +
                ", proxyConfigCrn='" + proxyConfigCrn + '\'' +
                ", cloudStorage=" + cloudStorage +
                ", cm=" + cm +
                ", gateway=" + gateway +
                ", customContainer=" + customContainer +
                ", customQueue='" + customQueue + '\'' +
                ", blueprintName='" + blueprintName + '\'' +
                ", validateBlueprint=" + validateBlueprint +
                ", customConfigurationsName='" + customConfigurationsName + '\'' +
                ", rangerRazEnabled=" + rangerRazEnabled +
                ", rangerRmsEnabled=" + rangerRmsEnabled +
                ", encryptionProfileName=" + encryptionProfileName +
                '}';
    }
}
