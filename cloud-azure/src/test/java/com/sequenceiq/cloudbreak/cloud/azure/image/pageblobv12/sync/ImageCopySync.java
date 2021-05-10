package com.sequenceiq.cloudbreak.cloud.azure.image.pageblobv12.sync;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.junit.Test;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobClientBuilder;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.blob.models.BlobProperties;
import com.sequenceiq.cloudbreak.cloud.azure.image.AzureTestCredentials;
import com.sequenceiq.cloudbreak.cloud.azure.image.ImageNameCombinator;

public class ImageCopySync {

    private final AzureTestCredentials azureTestCredentials = new AzureTestCredentials();

    private List<String> freeipaSourceBlobs = List.of(
            "https://cldrwestus.blob.core.windows.net/images/freeipa-cdh--2008121423.vhd",
            "https://cldrwestus.blob.core.windows.net/images/freeipa-cdh--2010061458.vhd"
    );

    private List<String> cdhSourceBlobs = List.of(
            "https://cldrwestus.blob.core.windows.net/images/cb-cdh--2012021538.vhd",
            "https://cldrwestus.blob.core.windows.net/images/cb-cdh--2012010839.vhd",
            "https://cldrwestus.blob.core.windows.net/images/cb-cdh--2011300854.vhd",
            "https://cldrwestus.blob.core.windows.net/images/cb-cdh--2011261322.vhd",
            "https://cldrwestus.blob.core.windows.net/images/cb-cdh--2011251004.vhd",
            "https://cldrwestus.blob.core.windows.net/images/cb-cdh--2011240904.vhd",
            "https://cldrwestus.blob.core.windows.net/images/cb-cdh--2011191715.vhd",
            "https://cldrwestus.blob.core.windows.net/images/cb-cdh--2011191044.vhd",
            "https://cldrwestus.blob.core.windows.net/images/cb-cdh--2011171355.vhd",
            "https://cldrwestus.blob.core.windows.net/images/cb-cdh--2011161934.vhd"
    );

    private static final int THREAD_POOL_SIZE = 320;

    private static final int THREAD_POOL_COUNT = 8;

    private static final int NUMBER_OF_PARALLEL_COPIES = 8;

    @Test
    public void newStorageBlobSdk() {
        ImageNameCombinator imageNameCombinator = new ImageNameCombinator(true, "copyTask_8c_", NUMBER_OF_PARALLEL_COPIES);
        ThreadPoolManager threadPoolManager = new ThreadPoolManager(THREAD_POOL_COUNT, THREAD_POOL_SIZE);

        List<ImageCopyTask> imageCopyTasks = new ArrayList<>();
        for (int i = 0; i < NUMBER_OF_PARALLEL_COPIES; i++) {
            String sourceBlob = imageNameCombinator.getSource(i);
            String destinationFileName = imageNameCombinator.getDestinationFilename(i);
            imageCopyTasks.add(new ImageCopyTask(azureTestCredentials, sourceBlob, destinationFileName, threadPoolManager.get(i)));
        }

        CompletableFuture copyTasks = CompletableFuture.allOf(
                imageCopyTasks.stream()
                        .map(imageCopyTask -> CompletableFuture.runAsync(imageCopyTask))
                        .toArray(CompletableFuture[]::new)
        );
        copyTasks.join();
        System.out.println("Finished");
    }

    @Test
    public void getPropertiesofSourceAndDestinationBlobs() {
        String sourceBlob = "https://cldrwestus.blob.core.windows.net/images/freeipa-cdh--2008121423.vhd";
        String filenameDest = "kiscica2.vhd";

        BlobProperties propertiesOriginal = getPublicBlobProperties(sourceBlob);
        BlobProperties propertiesDestination = getBlobClient(filenameDest);

        System.out.println(propertiesOriginal);
        System.out.println(propertiesDestination);
    }

    private BlobProperties getPublicBlobProperties(String sourceBlobUrl) {
        BlobClient blobClient = new BlobClientBuilder().endpoint(sourceBlobUrl).buildClient();
        return blobClient.getProperties();
    }

    private BlobProperties getBlobClient(String filenameOrig) {
        BlobServiceClient blobServiceClient = new BlobServiceClientBuilder().connectionString(azureTestCredentials.getStorageAccountConnectionString()).buildClient();
        BlobContainerClient blobContainerClient = blobServiceClient.getBlobContainerClient("images");
        BlobClient blobClient = blobContainerClient.getBlobClient(filenameOrig);
        return blobClient.getProperties();
    }
}
