package com.sequenceiq.it.cloudbreak.util.azurecloudblob;

import java.net.URISyntaxException;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.microsoft.azure.storage.StorageException;
import com.sequenceiq.it.cloudbreak.SdxClient;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxTestDto;
import com.sequenceiq.it.cloudbreak.util.azurecloudblob.action.AzureCloudBlobClientActions;

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

    public SdxTestDto cleanupContainer(TestContext testContext, SdxTestDto sdxTestDto, SdxClient sdxClient) {
        return azureCloudBlobClientActions.cleanupContainer(testContext, sdxTestDto, sdxClient);
    }

    public void createContainerIfNotExist(TestContext testContext) {
        azureCloudBlobClientActions.createCloudBlobContainer();
    }

    public SdxTestDto listAllFoldersInAContaier(TestContext testContext, SdxTestDto sdxTestDto, SdxClient sdxClient) {
        return azureCloudBlobClientActions.listAllFolders(testContext, sdxTestDto, sdxClient);
    }
}
