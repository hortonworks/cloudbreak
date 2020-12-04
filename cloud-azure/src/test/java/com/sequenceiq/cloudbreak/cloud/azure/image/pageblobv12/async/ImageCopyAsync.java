package com.sequenceiq.cloudbreak.cloud.azure.image.pageblobv12.async;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.junit.Ignore;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobClientBuilder;
import com.azure.storage.blob.BlobContainerAsyncClient;
import com.azure.storage.blob.BlobServiceAsyncClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.blob.models.BlobProperties;
import com.azure.storage.blob.models.PageRange;
import com.azure.storage.blob.specialized.PageBlobAsyncClient;
import com.sequenceiq.cloudbreak.cloud.azure.image.AzureTestCredentials;
import com.sequenceiq.cloudbreak.cloud.azure.image.pageblobv12.ChunkCalculator;

public class ImageCopyAsync {

    public static final int INFINITE_LEASE = -1;
    public static final int MAX_LEASE_DURATION = 60;
    private static int not201 = 0;
    private static int foundException = 0;
    private final AzureTestCredentials azureTestCredentials = new AzureTestCredentials();

    @Ignore("The rx version does not yet work")
//    @Test
    public void copyAsync() {
        String sourceBlob = "https://cldrwestus.blob.core.windows.net/images/freeipa-cdh--2008121423.vhd";
//        String sourceBlob = "https://cldrwestus.blob.core.windows.net/images/cb-cdh-72-2011180019.vhd";
        long chunkSize = 4194304L;
        String destinationFilename = "kiscica-datalake.vhd";
        int parallelism = 320;
        Duration taskTimeout = Duration.of(20, ChronoUnit.MINUTES);

        BlobProperties sourceBlobProperties = getPublicBlobProperties(sourceBlob);
        long fileSize = sourceBlobProperties.getBlobSize();


        BlobServiceAsyncClient bsac = new BlobServiceClientBuilder().connectionString(azureTestCredentials.getStorageAccountConnectionString()).buildAsyncClient();
        BlobContainerAsyncClient bccasync = bsac.getBlobContainerAsyncClient("images");
        PageBlobAsyncClient pageBlobAsyncClient = new BlobServiceClientBuilder().connectionString(azureTestCredentials.getStorageAccountConnectionString()).buildAsyncClient()
                .getBlobContainerAsyncClient("images")
                .getBlobAsyncClient(destinationFilename)
                .getPageBlobAsyncClient();

        ChunkCalculator chunkCalculator = new ChunkCalculator(fileSize, chunkSize);
        PageRangeCalculator pageRangeCalculator = new PageRangeCalculator();
        List<PageRange> pageRanges = pageRangeCalculator.getAll(chunkCalculator);
        CopyTaskSubmitter copyTaskSubmitter =  new CopyTaskSubmitter();
        copyTaskSubmitter.submitAll(pageRanges);
    }

    private BlobProperties getPublicBlobProperties(String sourceBlobUrl) {
        BlobClient blobClient = new BlobClientBuilder().endpoint(sourceBlobUrl).buildClient();
        return blobClient.getProperties();
    }

}
