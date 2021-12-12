package com.sequenceiq.it.cloudbreak.util.aws;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.apache.commons.lang3.NotImplementedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.microsoft.azure.management.resources.ResourceGroup;
import com.sequenceiq.it.cloudbreak.util.CloudFunctionality;
import com.sequenceiq.it.cloudbreak.util.aws.amazonec2.AmazonEC2Util;
import com.sequenceiq.it.cloudbreak.util.aws.amazons3.AmazonS3Util;

@Component
public class AwsCloudFunctionality implements CloudFunctionality {

    private static final Logger LOGGER = LoggerFactory.getLogger(AwsCloudFunctionality.class);

    @Inject
    private AmazonEC2Util amazonEC2Util;

    @Inject
    private AmazonS3Util amazonS3Util;

    @Override
    public List<String> listInstanceVolumeIds(String clusterName, List<String> instanceIds) {
        return amazonEC2Util.listInstanceVolumeIds(instanceIds);
    }

    @Override
    public List<String> listVolumeEncryptionKeyIds(String clusterName, List<String> instanceIds) {
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
    public ResourceGroup createResourceGroup(String resourceGroupName) {
        LOGGER.debug("createResourceGroup: nothing to do for AWS");
        throw new NotImplementedException("Resource group creation is not applicable for AWS!");
    }

    @Override
    public void deleteResourceGroup(String resourceGroupName) {
        LOGGER.debug("deleteResourceGroup: nothing to do for AWS");
        throw new NotImplementedException("Resource group deletion is not applicable for AWS!");
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
}
