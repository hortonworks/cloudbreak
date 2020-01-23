package com.sequenceiq.it.cloudbreak.util.azure.azurecloudblob;

import java.net.URISyntaxException;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.microsoft.azure.storage.StorageException;
import com.sequenceiq.it.cloudbreak.SdxClient;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxTestDto;
import com.sequenceiq.it.cloudbreak.util.azure.azurecloudblob.action.AzureCloudBlobClientActions;

@Component
public class AzureCloudBlobUtil {
    @Inject
    private AzureCloudBlobClientActions azureCloudBlobClientActions;

    private AzureCloudBlobUtil() {
    }

    public SdxTestDto deleteAllFoldersInAContainer(TestContext testContext, SdxTestDto sdxTestDto, SdxClient sdxClient) {
        return azureCloudBlobClientActions.deleteAllFolders(testContext, sdxTestDto, sdxClient);
    }

    public void deleteAllFoldersInAContainer() throws StorageException, URISyntaxException {
        azureCloudBlobClientActions.deleteAllFolders();
    }

    public void cleanupContainer(String baseLocation) {
        azureCloudBlobClientActions.cleanupContainer(baseLocation);
    }

    public void createContainerIfNotExist() {
        azureCloudBlobClientActions.createCloudBlobContainer();
    }

    public void listAllFoldersInAContaier(String baseLocation) {
        azureCloudBlobClientActions.listAllFolders(baseLocation);
    }

    public void listDataLakeFoldersInAContaier(String baseLocation) {
        azureCloudBlobClientActions.listSelectedDirectory(baseLocation, "datalake", false);
    }

    public void listFreeIPAFoldersInAContaier(String baseLocation) {
        azureCloudBlobClientActions.listSelectedDirectory(baseLocation, "freeipa", false);
    }
}
