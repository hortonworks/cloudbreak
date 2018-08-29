package com.sequenceiq.cloudbreak.template.model;

import java.util.Optional;

import com.sequenceiq.cloudbreak.api.model.ExecutorType;
import com.sequenceiq.cloudbreak.common.model.OrchestratorType;

public class GeneralClusterConfigs {

    private String identityUserEmail;

    private boolean gatewayInstanceMetadataPresented;

    private boolean instanceGroupsPresented;

    private String clusterName;

    private String stackName;

    private String uuid;

    private String userName;

    private String password;

    private ExecutorType executorType;

    private String ambariIp;

    private OrchestratorType orchestratorType = OrchestratorType.HOST;

    private int nodeCount;

    private Optional<String> primaryGatewayInstanceDiscoveryFQDN = Optional.empty();

    private int kafkaReplicationFactor;

    public GeneralClusterConfigs() {
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

    public void setPrimaryGatewayInstanceDiscoveryFQDN(Optional<String> primaryGatewayInstanceDiscoveryFQDN) {
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

    public Optional<String> getPrimaryGatewayInstanceDiscoveryFQDN() {
        return primaryGatewayInstanceDiscoveryFQDN;
    }

    public int getNodeCount() {
        return nodeCount;
    }

    public void setNodeCount(int nodeCount) {
        this.nodeCount = nodeCount;
    }

    public String getIdentityUserEmail() {
        return identityUserEmail;
    }

    public void setIdentityUserEmail(String identityUserEmail) {
        this.identityUserEmail = identityUserEmail;
    }

    public int getKafkaReplicationFactor() {
        return kafkaReplicationFactor;
    }

    public void setKafkaReplicationFactor(int kafkaReplicationFactor) {
        this.kafkaReplicationFactor = kafkaReplicationFactor;
    }
}
