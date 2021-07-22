package com.sequenceiq.it.cloudbreak.util.azure;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

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
}
