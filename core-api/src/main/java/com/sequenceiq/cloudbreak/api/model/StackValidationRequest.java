package com.sequenceiq.cloudbreak.api.model;

import java.util.HashSet;
import java.util.Set;

import javax.validation.constraints.NotNull;

import com.sequenceiq.cloudbreak.doc.ModelDescriptions;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.StackModelDescription;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
public class StackValidationRequest implements JsonEntity {
    @ApiModelProperty(value = ModelDescriptions.ClusterModelDescription.HOSTGROUPS, required = true)
    private Set<HostGroupRequest> hostGroups = new HashSet<>();

    @ApiModelProperty(value = StackModelDescription.INSTANCE_GROUPS, required = true)
    private Set<InstanceGroupRequest> instanceGroups = new HashSet<>();

    @ApiModelProperty(value = ModelDescriptions.ClusterModelDescription.BLUEPRINT_ID)
    private Long blueprintId;

    @ApiModelProperty(value = ModelDescriptions.ClusterModelDescription.BLUEPRINT)
    private BlueprintRequest blueprint;

    @ApiModelProperty(value = StackModelDescription.NETWORK_ID)
    private Long networkId;

    @ApiModelProperty(value = StackModelDescription.NETWORK)
    private NetworkRequest network;

    @NotNull
    @ApiModelProperty(value = ModelDescriptions.CLOUD_PLATFORM, required = true)
    private String platform;

    @ApiModelProperty(value = StackModelDescription.CREDENTIAL_ID)
    private Long credentialId;

    @ApiModelProperty(value = StackModelDescription.CREDENTIAL_NAME)
    private Long credentialName;

    @ApiModelProperty(value = StackModelDescription.CREDENTIAL)
    private CredentialRequest credential;

    private FileSystemRequest fileSystem;

    public Set<HostGroupRequest> getHostGroups() {
        return hostGroups;
    }

    public void setHostGroups(Set<HostGroupRequest> hostGroups) {
        this.hostGroups = hostGroups;
    }

    public Set<InstanceGroupRequest> getInstanceGroups() {
        return instanceGroups;
    }

    public void setInstanceGroups(Set<InstanceGroupRequest> instanceGroups) {
        this.instanceGroups = instanceGroups;
    }

    public Long getBlueprintId() {
        return blueprintId;
    }

    public void setBlueprintId(Long blueprintId) {
        this.blueprintId = blueprintId;
    }

    public Long getNetworkId() {
        return networkId;
    }

    public void setNetworkId(Long netWorkId) {
        this.networkId = netWorkId;
    }

    public FileSystemRequest getFileSystem() {
        return fileSystem;
    }

    public void setFileSystem(FileSystemRequest fileSystem) {
        this.fileSystem = fileSystem;
    }

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }

    public BlueprintRequest getBlueprint() {
        return blueprint;
    }

    public void setBlueprint(BlueprintRequest blueprint) {
        this.blueprint = blueprint;
    }

    public NetworkRequest getNetwork() {
        return network;
    }

    public void setNetwork(NetworkRequest network) {
        this.network = network;
    }

    public Long getCredentialId() {
        return credentialId;
    }

    public void setCredentialId(Long credentialId) {
        this.credentialId = credentialId;
    }

    public CredentialRequest getCredential() {
        return credential;
    }

    public void setCredential(CredentialRequest credential) {
        this.credential = credential;
    }

    public Long getCredentialName() {
        return credentialName;
    }

    public void setCredentialName(Long credentialName) {
        this.credentialName = credentialName;
    }
}
