package com.sequenceiq.cloudbreak.cloud.azure.image.pageblobv12.async;

import static com.azure.core.util.Configuration.PROPERTY_AZURE_LOG_LEVEL;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.junit.Test;

import com.azure.core.http.policy.HttpLogDetailLevel;
import com.azure.core.http.policy.HttpLogOptions;
import com.azure.core.util.Configuration;
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

    @Test
    public void makeItWork() {

        BlobServiceAsyncClient bsac = new BlobServiceClientBuilder()
                .connectionString(azureTestCredentials.getStorageAccountConnectionString())
                .httpLogOptions(new HttpLogOptions().setLogLevel(HttpLogDetailLevel.BODY_AND_HEADERS).setPrettyPrintBody(true))
                .configuration(new Configuration().put(PROPERTY_AZURE_LOG_LEVEL, "DEBUG"))
                .buildAsyncClient();

        BlobServiceAsyncClient bsacSas = new BlobServiceClientBuilder()
                .endpoint("https://cbimgwu9d62091440e606d4.blob.core.windows.net/?sv=2019-12-12&ss=bfqt&srt=sco&sp=rwdlacupx&se=2021-01-22T04:50:04Z&st=2021-01-21T20:50:04Z&spr=https&sig=kIWiqUfstLRVjKRt0AfFZ8difSAduhC%2F9%2BNsPYYZ%2BVA%3D")
                .httpLogOptions(new HttpLogOptions().setLogLevel(HttpLogDetailLevel.BODY_AND_HEADERS).setPrettyPrintBody(true))
                .configuration(new Configuration().put(PROPERTY_AZURE_LOG_LEVEL, "DEBUG"))
                .buildAsyncClient();

        bsacSas.createBlobContainer("vicekvacak")
                .doOnError(e -> System.out.println("Container exists?"))
                .subscribe();

//        bsacSas.getAccountInfo()
//                .doOnSuccess(sa -> System.out.println("SA is: " + sa))
//                .doOnError(t -> System.out.println("Error: " + t))
//                .onErrorResume(t -> Mono.just(new StorageAccountInfo(SkuName.STANDARD_LRS, AccountKind.BLOB_STORAGE)))
//                .subscribe(
//                acc -> System.out.println("What we have here is: " + acc.getAccountKind())
//        );

        System.out.println("Finished");

    }

//    @Ignore("The rx version does not yet work")
    @Test
    public void copyAsync() {
        String sourceBlob = "https://cldrwestus.blob.core.windows.net/images/freeipa-cdh--2008121423.vhd";
//        String sourceBlob = "https://cldrwestus.blob.core.windows.net/images/cb-cdh-72-2011180019.vhd";
        long chunkSize = 4194304L;
        String destinationFilename = "async1-freeipa.vhd";
        Duration taskTimeout = Duration.of(20, ChronoUnit.MINUTES);

        BlobProperties sourceBlobProperties = getPublicBlobProperties(sourceBlob);
        long fileSize = sourceBlobProperties.getBlobSize();

        BlobServiceAsyncClient bsac = new BlobServiceClientBuilder()
                .connectionString(azureTestCredentials.getStorageAccountConnectionString())
                .httpLogOptions(new HttpLogOptions().setLogLevel(HttpLogDetailLevel.BODY_AND_HEADERS).setPrettyPrintBody(true))
                .configuration(new Configuration().put(PROPERTY_AZURE_LOG_LEVEL, "DEBUG"))
                .buildAsyncClient();

        bsac.getAccountInfo().subscribe(
                acc -> System.out.println("What we have here is: " + acc.getAccountKind())
        );

        BlobContainerAsyncClient bccasync = bsac.getBlobContainerAsyncClient("images");
        PageBlobAsyncClient pageBlobAsyncClient = new BlobServiceClientBuilder()
                .connectionString(azureTestCredentials.getStorageAccountConnectionString())
                .httpLogOptions(new HttpLogOptions().setLogLevel(HttpLogDetailLevel.BODY_AND_HEADERS).setPrettyPrintBody(true))
                .configuration(new Configuration().put(PROPERTY_AZURE_LOG_LEVEL, "DEBUG"))
                .buildAsyncClient()
                .getBlobContainerAsyncClient("images")
                .getBlobAsyncClient(destinationFilename)
                .getPageBlobAsyncClient();

        ChunkCalculator chunkCalculator = new ChunkCalculator(fileSize, chunkSize);
        PageRangeCalculator pageRangeCalculator = new PageRangeCalculator();
        List<PageRange> pageRanges = pageRangeCalculator.getAll(chunkCalculator);
        CopyTaskSubmitter copyTaskSubmitter =  new CopyTaskSubmitter(pageBlobAsyncClient, sourceBlob, null);
        copyTaskSubmitter.submitAll(pageRanges, fileSize);
    }

    private BlobProperties getPublicBlobProperties(String sourceBlobUrl) {
        BlobClient blobClient = new BlobClientBuilder().endpoint(sourceBlobUrl).buildClient();
        return blobClient.getProperties();
    }

}
