package com.sequenceiq.it.cloudbreak.util.azure;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.microsoft.azure.management.resources.ResourceGroup;
import com.sequenceiq.it.cloudbreak.util.CloudFunctionality;
import com.sequenceiq.it.cloudbreak.util.azure.azurecloudblob.AzureCloudBlobUtil;
import com.sequenceiq.it.cloudbreak.util.azure.azurevm.action.AzureClientActions;

@Component
public class AzureCloudFunctionality implements CloudFunctionality {

    @Inject
    private AzureClientActions azureClientActions;

    @Inject
    private AzureCloudBlobUtil azureCloudBlobUtil;

    @Override
    public List<String> listInstanceVolumeIds(String clusterName, List<String> instanceIds) {
        return azureClientActions.listInstanceVolumeIds(clusterName, instanceIds);
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
    public ResourceGroup createResourceGroup(String resourceGroupName, Map<String, String> tags) {
        return azureClientActions.createResourceGroup(resourceGroupName, tags);
    }

    @Override
    public void deleteResourceGroup(String resourceGroupName) {
        azureClientActions.deleteResourceGroup(resourceGroupName);
    }

    @Override
    public void cloudStorageInitialize() {
        azureCloudBlobUtil.createContainerIfNotExist();
    }

    @Override
    public void cloudStorageListContainer(String baseLocation, String selectedDirectory, boolean zeroContent) {
        azureCloudBlobUtil.listSelectedFoldersInAContaier(baseLocation, selectedDirectory, zeroContent);
    }

    @Override
    public void cloudStorageListContainerFreeIpa(String baseLocation, String clusterName, String crn) {
        azureCloudBlobUtil.listFreeIpaFoldersInAContaier(baseLocation, clusterName, crn);
    }

    @Override
    public void cloudStorageListContainerDataLake(String baseLocation, String clusterName, String crn) {
        azureCloudBlobUtil.listDataLakeFoldersInAContaier(baseLocation, clusterName, crn);
    }

    @Override
    public void cloudStorageDeleteContainer(String baseLocation) {
        azureCloudBlobUtil.cleanupContainer(baseLocation);
    }

    @Override
    public Map<String, Boolean> enaSupport(List<String> instanceIds) {
        return Collections.emptyMap();
    }

    @Override
    public Map<String, String> getInstanceSubnetMap(List<String> instanceIds) {
        //TODO
        return null;
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
}
