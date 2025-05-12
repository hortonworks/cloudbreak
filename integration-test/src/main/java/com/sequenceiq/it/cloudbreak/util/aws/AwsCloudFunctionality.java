package com.sequenceiq.it.cloudbreak.util.aws;

import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.InstanceGroupV4Response;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceGroupResponse;
import com.sequenceiq.it.cloudbreak.microservice.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.util.CloudFunctionality;
import com.sequenceiq.it.cloudbreak.util.aws.amazonec2.AmazonEC2Util;
import com.sequenceiq.it.cloudbreak.util.aws.amazons3.AmazonS3Util;
import com.sequenceiq.it.cloudbreak.util.ssh.SshJUtil;
import com.sequenceiq.it.cloudbreak.util.ssh.action.SshEnaDriverCheckActions;

@Component
public class AwsCloudFunctionality implements CloudFunctionality {

    private static final Logger LOGGER = LoggerFactory.getLogger(AwsCloudFunctionality.class);

    @Inject
    private AmazonEC2Util amazonEC2Util;

    @Inject
    private AmazonS3Util amazonS3Util;

    @Inject
    private SshEnaDriverCheckActions sshEnaDriverCheckActions;

    @Inject
    private SshJUtil sshJUtil;

    @Override
    public List<String> listInstancesVolumeIds(String clusterName, List<String> instanceIds) {
        return amazonEC2Util.listInstancesVolumeIds(instanceIds);
    }

    @Override
    public List<String> listInstancesRootVolumeIds(String clusterName, List<String> instanceIds) {
        return amazonEC2Util.listInstancesRootVolumeIds(instanceIds);
    }

    @Override
    public Map<String, Set<String>> listInstanceVolumeIds(String clusterName, String instanceId) {
        return amazonEC2Util.listInstanceVolumeIds(instanceId);
    }

    @Override
    public List<String> listInstanceTypes(String clusterName, List<String> instanceIds) {
        return amazonEC2Util.listInstanceTypes(instanceIds);
    }

    @Override
    public List<String> listVolumeEncryptionKeyIds(String clusterName, String resourceGroupName, List<String> instanceIds) {
        return amazonEC2Util.listVolumeKmsKeyIds(instanceIds);
    }

    @Override
    public Map<String, Map<String, String>> listTagsByInstanceId(String clusterName, List<String> instanceIds) {
        return amazonEC2Util.listTagsByInstanceId(instanceIds);
    }

    @Override
    public void deleteInstances(String clusterName, List<String> instanceIds) {
        amazonEC2Util.deleteHostGroupInstances(instanceIds);
    }

    @Override
    public void stopInstances(String clusterName, List<String> instanceIds) {
        amazonEC2Util.stopHostGroupInstances(instanceIds);
    }

    @Override
    public void cloudStorageInitialize() {
        LOGGER.debug("cloudStorageInitialize: nothing to do for AWS");
    }

    @Override
    public void cloudStorageListContainer(String baseLocation, String selectedObject, boolean zeroContent) {
        amazonS3Util.listBucketSelectedObject(baseLocation, selectedObject, zeroContent);
    }

    @Override
    public void cloudStorageListContainerFreeIpa(String baseLocation, String clusterName, String crn) {
        amazonS3Util.listFreeIpaObject(baseLocation);
    }

    @Override
    public void cloudStorageListContainerDataLake(String baseLocation, String clusterName, String crn) {
        amazonS3Util.listDataLakeObject(baseLocation);
    }

    @Override
    public void cloudStorageDeleteContainer(String baseLocation) {
        amazonS3Util.deleteNonVersionedBucket(baseLocation);
    }

    @Override
    public Map<String, Boolean> enaSupport(List<String> instanceIds) {
        return amazonEC2Util.enaSupport(instanceIds);
    }

    @Override
    public Map<String, String> getInstanceSubnetMap(List<String> instanceIds) {
        return amazonEC2Util.instanceSubnet(instanceIds);
    }

    @Override
    public String getFreeIpaLogsUrl(String clusterName, String crn, String baseLocation) {
        return amazonS3Util.getFreeIpaLogsUrl(clusterName, crn, baseLocation);
    }

    @Override
    public String getDataLakeLogsUrl(String clusterName, String crn, String baseLocation) {
        return amazonS3Util.getDataLakeLogsUrl(clusterName, crn, baseLocation);
    }

    @Override
    public String getDataHubLogsUrl(String clusterName, String crn, String baseLocation) {
        return amazonS3Util.getDataHubLogsUrl(clusterName, crn, baseLocation);
    }

    @Override
    public void verifyEnaDriver(StackV4Response stackV4Response, CloudbreakClient cloudbreakClient) {
        sshEnaDriverCheckActions.checkEnaDriverOnAws(stackV4Response, cloudbreakClient);
    }

    @Override
    public void checkMountedDisks(List<InstanceGroupV4Response> instanceGroups, List<String> hostGroupNames) {
        sshJUtil.checkAwsMountedDisks(instanceGroups, hostGroupNames);
    }

    @Override
    public Set<String> getVolumeMountPoints(List<InstanceGroupV4Response> instanceGroups, List<String> hostGroupNames) {
        return sshJUtil.getAwsVolumeMountPoints(instanceGroups, hostGroupNames);
    }

    @Override
    public Map<String, String> getLaunchTemplateUserData(String name) {
        return amazonEC2Util.listLaunchTemplatesUserData(name);
    }

    @Override
    public Boolean isCloudFormationExistForStack(String name) {
        return amazonEC2Util.isCloudFormationExistForStack(name);
    }

    @Override
    public Boolean isFreeipaCfStackExistForEnvironment(String environmentCrn) {
        return !amazonEC2Util.listStacksByEnvironmentCrn(environmentCrn).isEmpty();
    }

    @Override
    public List<com.sequenceiq.cloudbreak.cloud.model.Volume> describeVolumes(List<String> volumeIds) {
        return amazonEC2Util.describeVolumes(volumeIds);
    }

    @Override
    public List<String> executeSshCommandsOnInstances(List<InstanceGroupResponse> instanceGroups, List<String> hostGroupNames, String privateKeyFilePath,
            String command) {
        return sshJUtil.executeSshCommandsOnInstances(instanceGroups, hostGroupNames, privateKeyFilePath, command);
    }

    @Override
    public Map<String, String> listAvailabilityZonesForVms(String clusterName, Map<String, String> instanceZoneMap) {
        return amazonEC2Util.listAvailabilityZonesForVms(instanceZoneMap);
    }
}
