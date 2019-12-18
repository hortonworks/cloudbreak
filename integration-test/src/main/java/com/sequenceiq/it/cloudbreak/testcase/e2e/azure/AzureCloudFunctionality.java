package com.sequenceiq.it.cloudbreak.testcase.e2e.azure;

import java.util.List;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.it.cloudbreak.testcase.e2e.CloudFunctionality;
import com.sequenceiq.it.cloudbreak.util.azure.azurecloudblob.AzureCloudBlobUtil;
import com.sequenceiq.it.cloudbreak.util.azure.azurevm.action.AzureClientActions;

@Component
public class AzureCloudFunctionality implements CloudFunctionality {

    @Inject
    private AzureClientActions azureClientActions;

    @Inject
    private AzureCloudBlobUtil azureCloudBlobUtil;

    @Override
    public List<String> listInstanceVolumeIds(List<String> instanceIds) {
        return azureClientActions.listInstanceVolumeIds(instanceIds);
    }

    @Override
    public void deleteInstances(List<String> instanceIds) {
        azureClientActions.deleteInstances(instanceIds);
    }

    @Override
    public void stopInstances(List<String> instanceIds) {
        azureClientActions.stopInstances(instanceIds);
    }

    @Override
    public void cloudStorageInitialize() {
        azureCloudBlobUtil.createContainerIfNotExist();
    }

    @Override
    public void cloudStorageListContainer(String baseLocation) {
        azureCloudBlobUtil.listAllFoldersInAContaier(baseLocation);
    }

    @Override
    public void cloudStorageDeleteContainer(String baseLocation) {
        azureCloudBlobUtil.cleanupContainer(baseLocation);
    }
}
