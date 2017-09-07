package com.sequenceiq.cloudbreak.api.model;

import java.util.HashSet;
import java.util.Set;

import javax.validation.constraints.NotNull;

import com.sequenceiq.cloudbreak.doc.ModelDescriptions;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.ClusterModelDescription;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.StackModelDescription;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
public class StackValidationRequest implements JsonEntity {
    @ApiModelProperty(value = ClusterModelDescription.HOSTGROUPS, required = true)
    private Set<HostGroupRequest> hostGroups = new HashSet<>();

    @ApiModelProperty(value = StackModelDescription.INSTANCE_GROUPS, required = true)
    private Set<InstanceGroupRequest> instanceGroups = new HashSet<>();

    @ApiModelProperty(ClusterModelDescription.BLUEPRINT_ID)
    private Long blueprintId;

    @ApiModelProperty(ClusterModelDescription.BLUEPRINT)
    private BlueprintRequest blueprint;

    @ApiModelProperty(StackModelDescription.NETWORK_ID)
    private Long networkId;

    @ApiModelProperty(StackModelDescription.NETWORK)
    private NetworkRequest network;

    @NotNull
    @ApiModelProperty(value = ModelDescriptions.CLOUD_PLATFORM, required = true)
    private String platform;

    @ApiModelProperty(StackModelDescription.CREDENTIAL_ID)
    private Long credentialId;

    @ApiModelProperty(StackModelDescription.CREDENTIAL_NAME)
    private String credentialName;

    @ApiModelProperty(StackModelDescription.CREDENTIAL)
    private CredentialRequest credential;

    @ApiModelProperty(StackModelDescription.FILESYSTEM)
    private FileSystemRequest fileSystem;

    @ApiModelProperty(hidden = true)
    private String owner;

    @ApiModelProperty(hidden = true)
    private String account;

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
        networkId = netWorkId;
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

    public String getCredentialName() {
        return credentialName;
    }

    public void setCredentialName(String credentialName) {
        this.credentialName = credentialName;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
    }
}
