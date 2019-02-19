package com.sequenceiq.it.cloudbreak.newway.cloud;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.RecoveryMode;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.ambari.AmbariV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.environment.EnvironmentSettingsV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.environment.placement.PlacementSettingsV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.InstanceGroupV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.template.InstanceTemplateV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.network.NetworkV4Request;
import com.sequenceiq.it.cloudbreak.newway.Cluster;
import com.sequenceiq.it.cloudbreak.newway.entity.credential.CredentialTestDto;
import com.sequenceiq.it.cloudbreak.newway.Stack;
import com.sequenceiq.it.cloudbreak.newway.entity.stack.StackTestDto;
import com.sequenceiq.it.cloudbreak.newway.entity.EnvironmentSettingsV4Entity;
import com.sequenceiq.it.cloudbreak.newway.entity.PlacementSettingsEntity;

public abstract class CloudProvider {

    public static final String CREDENTIAL_DEFAULT_DESCRIPTION = "test credential";

    public abstract StackTestDto aValidStackRequest();

    public abstract CredentialTestDto aValidCredential();

    public abstract CredentialTestDto aValidCredential(boolean create);

    public abstract Stack aValidStackCreated();

    public abstract StackTestDto aValidAttachedStackRequest(String datalakeName);

    public abstract AmbariV4Request ambariRequestWithBlueprintName(String bluePrintName);

    public abstract AmbariV4Request ambariRequestWithBlueprintNameAndCustomAmbari(String bluePrintName, String customAmbariVersion,
            String customAmbariRepoUrl, String customAmbariRepoGpgKey);

    public abstract String getClusterName();

    public abstract String getPlatform();

    public abstract String getCredentialName();

    public abstract String getBlueprintName();

    public abstract String getNetworkName();

    public abstract String getSubnetCIDR();

    public abstract Map<String, Object> newNetworkProperties();

    public abstract Map<String, Object> networkProperties();

    public abstract Map<String, Object> subnetProperties();

    public abstract String getVpcId();

    public abstract String getSubnetId();

    public EnvironmentSettingsV4Request getEnvironmentSettings() {
        return getEnvironmentSettings(getCredentialName(), getPlacementSettings());
    }

    public EnvironmentSettingsV4Request getEnvironmentSettings(String credentialName, PlacementSettingsV4Request placementSettingsV4Request) {
        EnvironmentSettingsV4Entity settingsV4Entity = new EnvironmentSettingsV4Entity().valid();
        settingsV4Entity.getRequest().setCredentialName(credentialName);
        return settingsV4Entity.getRequest();
    }

    protected PlacementSettingsV4Request getPlacementSettings() {
        return getPlacementSettings(region(), availabilityZone());
    }

    public PlacementSettingsV4Request getPlacementSettings(String region, String availabilityZone) {
        PlacementSettingsEntity placementSettingsEntity = new PlacementSettingsEntity().valid();
        placementSettingsEntity.getRequest().setRegion(region);
        placementSettingsEntity.getRequest().setAvailabilityZone(availabilityZone);
        return placementSettingsEntity.getRequest();
    }

    public abstract String region();

    public abstract NetworkV4Request newNetwork();

    public abstract NetworkV4Request existingNetwork();

    public abstract NetworkV4Request existingSubnet();

    public abstract String availabilityZone();

    public abstract List<InstanceGroupV4Request> instanceGroups(String securityGroupId);

    public abstract List<InstanceGroupV4Request> instanceGroups(Set<String> recipes);

    public abstract Stack aValidAttachedClusterStackCreated(HostGroupType... groupTypes);

    public abstract AmbariV4Request getAmbariRequestWithNoConfigStrategyAndEmptyMpacks(String blueprintName);

    public abstract ResourceHelper<?> getResourceHelper();

    public abstract Cluster aValidDatalakeCluster();

    public abstract Cluster aValidAttachedCluster();

    public abstract InstanceTemplateV4Request template();

    public abstract List<InstanceGroupV4Request> instanceGroups();

    public abstract List<InstanceGroupV4Request> instanceGroups(HostGroupType... groupTypes);

    public abstract List<InstanceGroupV4Request> instanceGroups(String securityGroupId, HostGroupType... groupTypes);

    public abstract List<InstanceGroupV4Request> instanceGroups(Set<String> recipes, HostGroupType... groupTypes);

    public abstract RecoveryMode getRecoveryModeParam(String hostgroupName);
}