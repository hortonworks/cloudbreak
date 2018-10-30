package com.sequenceiq.cloudbreak.template.views;

import com.sequenceiq.cloudbreak.api.model.ExecutorType;
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

    private ExecutorType executorType;

    private String ambariIp;

    private OrchestratorType orchestratorType;

    private boolean containerExecutor;

    private Integer nodeCount;

    private String primaryGatewayInstanceDiscoveryFQDN;

    private int kafkaReplicationFactor;

    public GeneralClusterConfigsView(GeneralClusterConfigs generalClusterConfigs) {
        email = generalClusterConfigs.getIdentityUserEmail();
        gatewayInstanceMetadataPresented = generalClusterConfigs.isGatewayInstanceMetadataPresented();
        instanceGroupsPresented = generalClusterConfigs.getInstanceGroupsPresented();
        clusterName = generalClusterConfigs.getClusterName();
        stackName = generalClusterConfigs.getStackName();
        uuid = generalClusterConfigs.getUuid();
        userName = generalClusterConfigs.getUserName();
        password = generalClusterConfigs.getPassword();
        executorType = generalClusterConfigs.getExecutorType();
        ambariIp = generalClusterConfigs.getAmbariIp();
        orchestratorType = generalClusterConfigs.getOrchestratorType();
        nodeCount = generalClusterConfigs.getNodeCount();
        containerExecutor = ExecutorType.CONTAINER.equals(generalClusterConfigs.getExecutorType());
        primaryGatewayInstanceDiscoveryFQDN = generalClusterConfigs.getPrimaryGatewayInstanceDiscoveryFQDN().orElse(null);
        kafkaReplicationFactor = generalClusterConfigs.getKafkaReplicationFactor();
        gatewayInstanceMetadataPresented = generalClusterConfigs.isGatewayInstanceMetadataPresented();
        instanceGroupsPresented = generalClusterConfigs.getInstanceGroupsPresented();
        clusterName = generalClusterConfigs.getClusterName();
        stackName = generalClusterConfigs.getStackName();
        uuid = generalClusterConfigs.getUuid();
        userName = generalClusterConfigs.getUserName();
        password = generalClusterConfigs.getPassword();
        executorType = generalClusterConfigs.getExecutorType();
        ambariIp = generalClusterConfigs.getAmbariIp();
        orchestratorType = generalClusterConfigs.getOrchestratorType();
        nodeCount = generalClusterConfigs.getNodeCount();
        containerExecutor = ExecutorType.CONTAINER.equals(generalClusterConfigs.getExecutorType());
        primaryGatewayInstanceDiscoveryFQDN = generalClusterConfigs.getPrimaryGatewayInstanceDiscoveryFQDN().orElse(null);
        kafkaReplicationFactor = generalClusterConfigs.getKafkaReplicationFactor();
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

    public void setExecutorType(ExecutorType executorType) {
        this.executorType = executorType;
    }

    public void setAmbariIp(String ambariIp) {
        this.ambariIp = ambariIp;
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

    public ExecutorType getExecutorType() {
        return executorType;
    }

    public String getAmbariIp() {
        return ambariIp;
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

    public boolean getContainerExecutor() {
        return containerExecutor;
    }

    public void setContainerExecutor(boolean containerExecutor) {
        this.containerExecutor = containerExecutor;
    }

    public int getKafkaReplicationFactor() {
        return kafkaReplicationFactor;
    }

    public void setKafkaReplicationFactor(int kafkaReplicationFactor) {
        this.kafkaReplicationFactor = kafkaReplicationFactor;
    }
}
