package com.sequenceiq.cloudbreak.template.views;

import com.sequenceiq.cloudbreak.common.model.OrchestratorType;
import com.sequenceiq.cloudbreak.template.model.GeneralClusterConfigs;

public class GeneralClusterConfigsView {

    private String email;

    private boolean gatewayInstanceMetadataPresented;

    private boolean instanceGroupsPresented;

    private String clusterName;

    private String stackName;

    private String uuid;

    private String userName;

    private String password;

    private String cmUserName;

    private String cmPassword;

    private String clusterManagerIp;

    private OrchestratorType orchestratorType;

    private Integer nodeCount;

    private String primaryGatewayInstanceDiscoveryFQDN;

    public GeneralClusterConfigsView(GeneralClusterConfigs generalClusterConfigs) {
        email = generalClusterConfigs.getIdentityUserEmail();
        gatewayInstanceMetadataPresented = generalClusterConfigs.isGatewayInstanceMetadataPresented();
        instanceGroupsPresented = generalClusterConfigs.getInstanceGroupsPresented();
        clusterName = generalClusterConfigs.getClusterName();
        stackName = generalClusterConfigs.getStackName();
        uuid = generalClusterConfigs.getUuid();
        userName = generalClusterConfigs.getUserName();
        password = generalClusterConfigs.getPassword();
        cmUserName = generalClusterConfigs.getCloudbreakClusterManagerUser();
        cmPassword = generalClusterConfigs.getCloudbreakClusterManagerPassword();
        clusterManagerIp = generalClusterConfigs.getClusterManagerIp();
        orchestratorType = generalClusterConfigs.getOrchestratorType();
        nodeCount = generalClusterConfigs.getNodeCount();
        primaryGatewayInstanceDiscoveryFQDN = generalClusterConfigs.getPrimaryGatewayInstanceDiscoveryFQDN().orElse(null);
    }

    public OrchestratorType getOrchestratorType() {
        return orchestratorType;
    }

    public void setOrchestratorType(OrchestratorType orchestratorType) {
        this.orchestratorType = orchestratorType;
    }

    public void setGatewayInstanceMetadataPresented(boolean gatewayInstanceMetadataPresented) {
        this.gatewayInstanceMetadataPresented = gatewayInstanceMetadataPresented;
    }

    public void setInstanceGroupsPresented(boolean instanceGroupsPresented) {
        this.instanceGroupsPresented = instanceGroupsPresented;
    }

    public void setClusterName(String clusterName) {
        this.clusterName = clusterName;
    }

    public void setStackName(String stackName) {
        this.stackName = stackName;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setCmUserName(String cmUserName) {
        this.cmUserName = cmUserName;
    }

    public void setCmPassword(String cmPassword) {
        this.cmPassword = cmPassword;
    }

    public void setClusterManagerIp(String clusterManagerIp) {
        this.clusterManagerIp = clusterManagerIp;
    }

    public void setPrimaryGatewayInstanceDiscoveryFQDN(String primaryGatewayInstanceDiscoveryFQDN) {
        this.primaryGatewayInstanceDiscoveryFQDN = primaryGatewayInstanceDiscoveryFQDN;
    }

    public boolean getInstanceGroupsPresented() {
        return instanceGroupsPresented;
    }

    public boolean isGatewayInstanceMetadataPresented() {
        return gatewayInstanceMetadataPresented;
    }

    public String getClusterName() {
        return clusterName;
    }

    public String getStackName() {
        return stackName;
    }

    public String getUuid() {
        return uuid;
    }

    public String getUserName() {
        return userName;
    }

    public String getPassword() {
        return password;
    }

    public String getCmUserName() {
        return cmUserName;
    }

    public String getCmPassword() {
        return cmPassword;
    }

    public String getClusterManagerIp() {
        return clusterManagerIp;
    }

    public String getPrimaryGatewayInstanceDiscoveryFQDN() {
        return primaryGatewayInstanceDiscoveryFQDN;
    }

    public Integer getNodeCount() {
        return nodeCount;
    }

    public void setNodeCount(Integer nodeCount) {
        this.nodeCount = nodeCount;
    }

    public boolean isInstanceGroupsPresented() {
        return instanceGroupsPresented;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
