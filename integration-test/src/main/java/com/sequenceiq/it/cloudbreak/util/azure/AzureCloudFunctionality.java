package com.sequenceiq.it.cloudbreak.util.azure;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.azure.resourcemanager.resources.models.ResourceGroup;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.InstanceGroupV4Response;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceGroupResponse;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.microservice.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.util.CloudFunctionality;
import com.sequenceiq.it.cloudbreak.util.azure.azurecloudblob.AzureCloudBlobUtil;
import com.sequenceiq.it.cloudbreak.util.azure.azurevm.action.AzureClientActions;
import com.sequenceiq.it.cloudbreak.util.ssh.SshJUtil;

@Component
public class AzureCloudFunctionality implements CloudFunctionality {

    private static final Logger LOGGER = LoggerFactory.getLogger(AzureCloudFunctionality.class);

    @Inject
    private AzureClientActions azureClientActions;

    @Inject
    private AzureCloudBlobUtil azureCloudBlobUtil;

    @Inject
    private SshJUtil sshJUtil;

    @Override
    public List<String> listInstancesVolumeIds(String clusterName, List<String> instanceIds) {
        return azureClientActions.getSelectedInstancesVolumeIds(clusterName, instanceIds, false);
    }

    @Override
    public List<String> listInstancesRootVolumeIds(String clusterName, List<String> instanceIds) {
        return azureClientActions.getSelectedInstancesVolumeIds(clusterName, instanceIds, true);
    }

    @Override
    public Map<String, Set<String>> listInstanceVolumeIds(String clusterName, String instanceId) {
        return azureClientActions.getInstanceVolumeIds(clusterName, instanceId);
    }

    @Override
    public List<String> listInstanceTypes(String clusterName, List<String> instanceIds) {
        return azureClientActions.listInstanceTypes(clusterName, instanceIds);
    }

    @Override
    public List<String> listVolumeEncryptionKeyIds(String clusterName, String resourceGroupName, List<String> instanceIds) {
        return azureClientActions.getVolumesDesId(clusterName, resourceGroupName, instanceIds);
    }

    @Override
    public Map<String, Map<String, String>> listTagsByInstanceId(String clusterName, List<String> instanceIds) {
        return azureClientActions.listTagsByInstanceId(clusterName, instanceIds);
    }

    @Override
    public void deleteInstances(String clusterName, List<String> instanceIds) {
        azureClientActions.deleteInstances(clusterName, instanceIds);
    }

    @Override
    public void stopInstances(String clusterName, List<String> instanceIds) {
        azureClientActions.stopInstances(clusterName, instanceIds);
    }

    @Override
    public void cloudStorageInitialize() {
        azureCloudBlobUtil.createContainerIfNotExist();
    }

    @Override
    public void cloudStorageListContainer(String baseLocation, String selectedDirectory, boolean zeroContent) {
        azureCloudBlobUtil.listSelectedFoldersInAContainer(baseLocation, selectedDirectory, zeroContent);
    }

    @Override
    public void cloudStorageListContainerFreeIpa(String baseLocation, String clusterName, String crn) {
        azureCloudBlobUtil.listFreeIpaFoldersInAContainer(baseLocation, clusterName, crn);
    }

    @Override
    public void cloudStorageListContainerDataLake(String baseLocation, String clusterName, String crn) {
        azureCloudBlobUtil.listDataLakeFoldersInAContainer(baseLocation, clusterName, crn);
    }

    @Override
    public void cloudStorageDeleteContainer(String baseLocation) {
        azureCloudBlobUtil.cleanupContainer(baseLocation);
    }

    @Override
    public Map<String, Boolean> enaSupport(List<String> instanceIds) {
        LOGGER.warn("enaSupport ::: ENA driver is only available for AWS!");
        Log.log(LOGGER, " enaSupport ::: ENA driver is only available at AWS! ");
        return Collections.emptyMap();
    }

    @Override
    public Map<String, String> getInstanceSubnetMap(List<String> instanceIds) {
        LOGGER.warn("getInstanceSubnetMap ::: Not implemented for AZURE!");
        Log.log(LOGGER, " getInstanceSubnetMap ::: Not implemented for AZURE! ");
        return Collections.emptyMap();
    }

    @Override
    public String getFreeIpaLogsUrl(String clusterName, String crn, String baseLocation) {
        return azureCloudBlobUtil.getFreeIpaLogsUrl(clusterName, crn, baseLocation);
    }

    @Override
    public String getDataLakeLogsUrl(String clusterName, String crn, String baseLocation) {
        return azureCloudBlobUtil.getDataLakeLogsUrl(clusterName, crn, baseLocation);
    }

    @Override
    public String getDataHubLogsUrl(String clusterName, String crn, String baseLocation) {
        return azureCloudBlobUtil.getDataHubLogsUrl(clusterName, crn, baseLocation);
    }

    @Override
    public void verifyEnaDriver(StackV4Response stackV4Response, CloudbreakClient cloudbreakClient, TestContext testContext) {
        LOGGER.warn("ENA driver is only available at AWS. So validation on AZURE is not possible!");
        Log.then(LOGGER, " ENA driver is only available at AWS. So validation on AZURE is not possible! ");
    }

    public ResourceGroup createResourceGroup(String resourceGroupName, Map<String, String> tags) {
        return azureClientActions.createResourceGroup(resourceGroupName, tags);
    }

    public void deleteResourceGroup(String resourceGroupName) {
        azureClientActions.deleteResourceGroup(resourceGroupName);
    }

    @Override
    public void checkMountedDisks(List<InstanceGroupV4Response> instanceGroups, List<String> hostGroupNames) {
        sshJUtil.checkAzureMountedDisks(instanceGroups, hostGroupNames);
    }

    @Override
    public Set<String> getVolumeMountPoints(List<InstanceGroupV4Response> instanceGroups, List<String> hostGroupNames) {
        return Set.of("/mnt/resource", "/hadoopfs/ephfs1");
    }

    @Override
    public Map<String, String> getLaunchTemplateUserData(String stack) {
        LOGGER.info("not implemented for azure");
        return Collections.emptyMap();
    }

    @Override
    public Boolean isCloudFormationExistForStack(String name) {
        LOGGER.warn("CloudFormation is only available at AWS. So validation on AZURE is not possible!");
        Log.then(LOGGER, "CloudFormation is only available at AWS. So validation on AZURE is not possible! ");
        return true;
    }

    @Override
    public Boolean isFreeipaCfStackExistForEnvironment(String environmentCrn) {
        LOGGER.warn("CloudFormation is only available at AWS. So validation on AZURE is not possible!");
        Log.then(LOGGER, "CloudFormation is only available at AWS. So validation on AZURE is not possible! ");
        return true;    }

    @Override
    public Map<String, Set<String>> listAvailabilityZonesForVms(String clusterName, List<String> instanceIds) {
        return azureClientActions.listAvailabilityZonesForVms(clusterName, instanceIds);
    }

    @Override
    public List<com.sequenceiq.cloudbreak.cloud.model.Volume> describeVolumes(List<String> volumeIds) {
        return azureClientActions.describeVolumes(volumeIds);
    }

    @Override
    public List<String> executeSshCommandsOnInstances(List<InstanceGroupResponse> instanceGroups, List<String> hostGroupNames, String privateKeyFilePath,
            String command) {
        return sshJUtil.executeSshCommandsOnInstances(instanceGroups, hostGroupNames, privateKeyFilePath, command);
    }
}
