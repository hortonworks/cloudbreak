package com.sequenceiq.cloudbreak.template.model;

import java.util.Optional;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.ExecutorType;
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

    private String cloudbreakAmbariUser;

    private String cloudbreakAmbariPassword;

    private ExecutorType executorType;

    private String clusterManagerIp;

    private String externalFQDN;

    private OrchestratorType orchestratorType = OrchestratorType.HOST;

    private int nodeCount;

    private Optional<String> primaryGatewayInstanceDiscoveryFQDN = Optional.empty();

    private String variant;

    private boolean autoTlsEnabled;

    private boolean knoxUserFacingCertConfigured;

    private boolean enableRangerRaz;

    private Optional<String> loadBalancerGatewayFqdn = Optional.empty();

    private Optional<String> accountId = Optional.empty();

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

    public void setCloudbreakAmbariUser(String cloudbreakAmbariUser) {
        this.cloudbreakAmbariUser = cloudbreakAmbariUser;
    }

    public void setCloudbreakAmbariPassword(String cloudbreakAmbariPassword) {
        this.cloudbreakAmbariPassword = cloudbreakAmbariPassword;
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

    public void setClusterManagerIp(String clusterManagerIp) {
        this.clusterManagerIp = clusterManagerIp;
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

    public String getCloudbreakAmbariUser() {
        return cloudbreakAmbariUser;
    }

    public String getCloudbreakAmbariPassword() {
        return cloudbreakAmbariPassword;
    }

    public ExecutorType getExecutorType() {
        return executorType;
    }

    public String getClusterManagerIp() {
        return clusterManagerIp;
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

    public String getVariant() {
        return variant;
    }

    public void setVariant(String variant) {
        this.variant = variant;
    }

    public boolean getAutoTlsEnabled() {
        return autoTlsEnabled;
    }

    public void setAutoTlsEnabled(boolean autoTlsEnabled) {
        this.autoTlsEnabled = autoTlsEnabled;
    }

    public boolean getKnoxUserFacingCertConfigured() {
        return knoxUserFacingCertConfigured;
    }

    public void setKnoxUserFacingCertConfigured(boolean knoxUserFacingCertConfigured) {
        this.knoxUserFacingCertConfigured = knoxUserFacingCertConfigured;
    }

    public String getExternalFQDN() {
        return externalFQDN;
    }

    public void setExternalFQDN(String externalFQDN) {
        this.externalFQDN = externalFQDN;
    }

    public boolean isEnableRangerRaz() {
        return enableRangerRaz;
    }

    public void setEnableRangerRaz(boolean enableRangerRaz) {
        this.enableRangerRaz = enableRangerRaz;
    }

    public Optional<String> getLoadBalancerGatewayFqdn() {
        return loadBalancerGatewayFqdn;
    }

    public void setLoadBalancerGatewayFqdn(Optional<String> loadBalancerGatewayFqdn) {
        this.loadBalancerGatewayFqdn = loadBalancerGatewayFqdn;
    }

    public Optional<String> getAccountId() {
        return accountId;
    }

    public void setAccountId(Optional<String> accountId) {
        this.accountId = accountId;
    }
}
