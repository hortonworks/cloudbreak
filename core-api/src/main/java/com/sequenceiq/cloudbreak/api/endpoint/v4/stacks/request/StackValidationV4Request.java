package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request;

import java.util.HashSet;
import java.util.Set;

import com.sequenceiq.cloudbreak.api.endpoint.v4.JsonEntity;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.InstanceGroupV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.network.NetworkV4Request;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.ClusterModelDescription;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.StackModelDescription;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
public class StackValidationV4Request implements JsonEntity {

    @ApiModelProperty(value = ClusterModelDescription.HOSTGROUPS, required = true)
    private Set<HostGroupV4Request> hostGroups = new HashSet<>();

    @ApiModelProperty(value = StackModelDescription.INSTANCE_GROUPS, required = true)
    private Set<InstanceGroupV4Request> instanceGroups = new HashSet<>();

    @ApiModelProperty(ClusterModelDescription.CLUSTER_DEFINITION_NAME)
    private String clusterDefinitionName;

    @ApiModelProperty(StackModelDescription.NETWORK_ID)
    private Long networkId;

    @ApiModelProperty(StackModelDescription.NETWORK)
    private NetworkV4Request network;

    @ApiModelProperty(StackModelDescription.ENVIRONMENT)
    private String environmentName;

    @ApiModelProperty(StackModelDescription.CREDENTIAL_NAME)
    private String credentialName;

    @ApiModelProperty(StackModelDescription.FILESYSTEM)
    private FileSystemValidationV4Request fileSystem;

    public Set<HostGroupV4Request> getHostGroups() {
        return hostGroups;
    }

    public void setHostGroups(Set<HostGroupV4Request> hostGroups) {
        this.hostGroups = hostGroups;
    }

    public Set<InstanceGroupV4Request> getInstanceGroups() {
        return instanceGroups;
    }

    public void setInstanceGroups(Set<InstanceGroupV4Request> instanceGroups) {
        this.instanceGroups = instanceGroups;
    }

    public String getClusterDefinitionName() {
        return clusterDefinitionName;
    }

    public void setClusterDefinitionName(String clusterDefinitionName) {
        this.clusterDefinitionName = clusterDefinitionName;
    }

    public Long getNetworkId() {
        return networkId;
    }

    public void setNetworkId(Long networkId) {
        this.networkId = networkId;
    }

    public NetworkV4Request getNetwork() {
        return network;
    }

    public void setNetwork(NetworkV4Request network) {
        this.network = network;
    }

    public String getEnvironmentName() {
        return environmentName;
    }

    public void setEnvironmentName(String environmentName) {
        this.environmentName = environmentName;
    }

    public String getCredentialName() {
        return credentialName;
    }

    public void setCredentialName(String credentialName) {
        this.credentialName = credentialName;
    }

    public FileSystemValidationV4Request getFileSystem() {
        return fileSystem;
    }

    public void setFileSystem(FileSystemValidationV4Request fileSystem) {
        this.fileSystem = fileSystem;
    }
}
