package com.sequenceiq.it.cloudbreak.util.gcp;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.InstanceGroupV4Response;
import com.sequenceiq.cloudbreak.cloud.gcp.util.GcpLabelUtil;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceGroupResponse;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.microservice.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.util.CloudFunctionality;
import com.sequenceiq.it.cloudbreak.util.ssh.SshJUtil;

@Component
public class GcpCloudFunctionality implements CloudFunctionality {

    private static final Logger LOGGER = LoggerFactory.getLogger(GcpCloudFunctionality.class);

    private static final String GCP_IMPLEMENTATION_MISSING = "GCP implementation missing";

    @Inject
    private GcpUtil gcpUtil;

    @Inject
    private GcpLabelUtil gcpLabelUtil;

    @Inject
    private SshJUtil sshJUtil;

    @Override
    public List<String> listInstancesVolumeIds(String clusterName, List<String> instanceIds) {
        return gcpUtil.listInstancesDiskNames(instanceIds);
    }

    @Override
    public Map<String, Set<String>> listInstanceVolumeIds(String clusterName, String instanceId) {
        return gcpUtil.listInstanceVolumeIds(instanceId);
    }

    @Override
    public List<String> listInstanceTypes(String clusterName, List<String> instanceIds) {
        return gcpUtil.listInstanceTypes(instanceIds);
    }

    @Override
    public List<String> listVolumeEncryptionKeyIds(String clusterName, String resourceGroupName, List<String> instanceIds) {
        return gcpUtil.listVolumeEncryptionKey(instanceIds);
    }

    @Override
    public Map<String, Map<String, String>> listTagsByInstanceId(String clusterName, List<String> instanceIds) {
        return gcpUtil.listTagsByInstanceId(instanceIds);
    }

    @Override
    public Map<String, String> getInstanceSubnetMap(List<String> instanceIds) {
        LOGGER.warn("getInstanceSubnetMap ::: Not implemented for GCP!");
        Log.log(LOGGER, " getInstanceSubnetMap ::: Not implemented for GCP! ");
        return Collections.emptyMap();
    }

    @Override
    public void deleteInstances(String clusterName, List<String> instanceIds) {
        gcpUtil.deleteHostGroupInstances(instanceIds);
    }

    @Override
    public void deleteInstances(String clusterName, Map<String, String> instances) {
        gcpUtil.deleteHostGroupInstances(instances);
    }

    @Override
    public void stopInstances(String clusterName, List<String> instanceIds) {
        gcpUtil.stopHostGroupInstances(instanceIds);
    }

    @Override
    public void cloudStorageInitialize() {
        LOGGER.debug("cloudStorageInitialize: nothing to do for GCP");
    }

    @Override
    public void cloudStorageListContainer(String baseLocation, String selectedObject, boolean zeroContent) {
        gcpUtil.cloudStorageListContainer(baseLocation, selectedObject, zeroContent);
    }

    @Override
    public void cloudStorageListContainerFreeIpa(String baseLocation, String clusterName, String crn) {
        gcpUtil.cloudStorageListContainerFreeIpa(baseLocation, clusterName, crn);
    }

    @Override
    public void cloudStorageListContainerDataLake(String baseLocation, String clusterName, String crn) {
        gcpUtil.cloudStorageListContainerDataLake(baseLocation, clusterName, crn);
    }

    @Override
    public void cloudStorageDeleteContainer(String baseLocation) {
        gcpUtil.cloudStorageDeleteContainer(baseLocation);
    }

    @Override
    public Map<String, Boolean> enaSupport(List<String> instanceIds) {
        LOGGER.warn("enaSupport ::: ENA driver is only available for AWS!");
        Log.log(LOGGER, " enaSupport ::: ENA driver is only available at AWS! ");
        return Collections.emptyMap();
    }

    @Override
    public String transformTagKeyOrValue(String originalValue) {
        return gcpLabelUtil.transformLabelKeyOrValue(originalValue);
    }

    @Override
    public String getFreeIpaLogsUrl(String clusterName, String crn, String baseLocation) {
        return gcpUtil.getFreeIpaLogsUrl(clusterName, crn, baseLocation);
    }

    @Override
    public String getDataLakeLogsUrl(String clusterName, String crn, String baseLocation) {
        return gcpUtil.getDataLakeLogsUrl(clusterName, crn, baseLocation);
    }

    @Override
    public String getDataHubLogsUrl(String clusterName, String crn, String baseLocation) {
        return gcpUtil.getDataHubLogsUrl(clusterName, crn, baseLocation);
    }

    @Override
    public void verifyEnaDriver(StackV4Response stackV4Response, CloudbreakClient cloudbreakClient, TestContext testContext) {
        LOGGER.warn("ENA driver is only available at AWS. So validation on GCP is not possible!");
        Log.then(LOGGER, " ENA driver is only available at AWS. So validation on GCP is not possible! ");
    }

    @Override
    public void checkMountedDisks(List<InstanceGroupV4Response> instanceGroups, List<String> hostGroupNames) {
        LOGGER.debug("Currently not implemented for GCP!");
    }

    @Override
    public Set<String> getVolumeMountPoints(List<InstanceGroupV4Response> instanceGroups, List<String> hostGroupNames) {
        LOGGER.debug("Currently not implemented for GCP!");
        return Collections.emptySet();
    }

    @Override
    public Map<String, String> getLaunchTemplateUserData(String stack) {
        LOGGER.debug("Currently not implemented for GCP!");
        return Collections.emptyMap();
    }

    @Override
    public Boolean isCloudFormationExistForStack(String name) {
        LOGGER.warn("CloudFormation is only available at AWS. So validation on GCP is not possible!");
        Log.then(LOGGER, "CloudFormation is only available at AWS. So validation on GCP is not possible! ");
        return true;
    }

    @Override
    public Boolean isFreeipaCfStackExistForEnvironment(String environmentCrn) {
        LOGGER.warn("CloudFormation is only available at AWS. So validation on GCP is not possible!");
        Log.then(LOGGER, "CloudFormation is only available at AWS. So validation on GCP is not possible! ");
        return true;
    }

    @Override
    public Map<String, String> listAvailabilityZonesForVms(String clusterName, Map<String, String> instanceZoneMap) {
        return gcpUtil.listAvailabilityZonesForVms(instanceZoneMap);
    }

    @Override
    public List<String> executeSshCommandsOnInstances(List<InstanceGroupResponse> instanceGroups, List<String> hostGroupNames, String privateKeyFilePath,
            String command) {
        return sshJUtil.executeSshCommandsOnInstances(instanceGroups, hostGroupNames, privateKeyFilePath, command);
    }
}
