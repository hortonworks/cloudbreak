package com.sequenceiq.cloudbreak.cloud.azure.image;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

import org.junit.Test;

import com.azure.core.util.polling.SyncPoller;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobClientBuilder;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.blob.models.BlobProperties;
import com.azure.storage.blob.models.RehydratePriority;
import com.azure.storage.blob.specialized.PageBlobClient;

public class ImageCopyPageBlobCopy {
    public static final Duration TIMEOUT = Duration.of(20, ChronoUnit.MINUTES);
    private final String connectionString2 = "DefaultEndpointsProtocol=https;AccountName=cbimgwu9d62091440e606d4;AccountKey=tYEGBt9rjo2Ql5ZV7vcnndbE+/5MBV9PZbRY5wU50nzuRsg6TphTCniVSbVvPXe6MLct5a66t6gaLryBlpw8Fw==;EndpointSuffix=core.windows.net";

    @Test
    public void copyWithv12CopyMethod() {
        String sourceBlob = "https://cldrwestus.blob.core.windows.net/images/cb-cdh-72-2011180019.vhd";
        long chunkSize = 4194304L;
        String destinationFilename = "kiscica-datalake3.vhd";
        int parallelism = 320;
        Duration taskTimeout = Duration.of(20, ChronoUnit.MINUTES);

        BlobProperties sourceBlobProperties = getPublicBlobProperties(sourceBlob);
        BlobServiceClient blobServiceClient = new BlobServiceClientBuilder().connectionString(connectionString2).buildClient();
        BlobContainerClient blobContainerClient = blobServiceClient.getBlobContainerClient("images");
        BlobClient blobClient = blobContainerClient.getBlobClient(destinationFilename);
        PageBlobClient destinationPageBlobClient = blobClient.getPageBlobClient();

        SyncPoller poller = destinationPageBlobClient.beginCopy(sourceBlob, sourceBlobProperties.getMetadata(), null, RehydratePriority.HIGH, null, null, TIMEOUT);
        poller.waitForCompletion();

    }

    private BlobProperties getPublicBlobProperties(String sourceBlobUrl) {
        BlobClient blobClient = new BlobClientBuilder().endpoint(sourceBlobUrl).buildClient();
        return blobClient.getProperties();
    }

}
