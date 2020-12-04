package com.sequenceiq.cloudbreak.cloud.azure.image.pageblobv12.sync;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;

import com.azure.core.util.Context;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobClientBuilder;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.blob.models.BlobProperties;
import com.azure.storage.blob.models.PageBlobRequestConditions;
import com.azure.storage.blob.specialized.BlobLeaseClient;
import com.azure.storage.blob.specialized.BlobLeaseClientBuilder;
import com.azure.storage.blob.specialized.PageBlobClient;
import com.sequenceiq.cloudbreak.cloud.azure.image.AzureTestCredentials;
import com.sequenceiq.cloudbreak.cloud.azure.image.pageblobv12.ChunkCalculator;

public class ImageCopyTask implements Runnable {

    public static final int MAX_LEASE_DURATION = 60;

    private static final long MAX_CHUNK_SIZE = 4194304L;

    private static final Duration TASK_TIMEOUT = Duration.of(20, ChronoUnit.MINUTES);

    private final AzureTestCredentials azureTestCredentials;

    private final String sourceBlobUrl;

    private final String destinationFilename;

    private final ThreadPoolExecutor threadPoolExecutor;

    private TaskState taskState;

    private Exception exception;

    public ImageCopyTask(AzureTestCredentials azureTestCredentials, String sourceBlobUrl, String destinationFilename, ThreadPoolExecutor threadPoolExecutor) {
        this.azureTestCredentials = azureTestCredentials;
        this.sourceBlobUrl = sourceBlobUrl;
        this.destinationFilename = destinationFilename;
        this.threadPoolExecutor = threadPoolExecutor;
        this.taskState = TaskState.REQUESTED;
    }

    public TaskState getTaskState() {
        return taskState;
    }

    public Exception getException() {
        return exception;
    }

    @Override
    public void run() {
        try {
            BlobProperties sourceBlobProperties = getPublicBlobProperties(sourceBlobUrl);
            long fileSize = sourceBlobProperties.getBlobSize();
            ChunkCalculator chunkCalculator = new ChunkCalculator(fileSize, MAX_CHUNK_SIZE);

            PageBlobClient destinationPageBlobClient = new BlobServiceClientBuilder().connectionString(azureTestCredentials.getStorageAccountConnectionString()).buildClient()
                    .getBlobContainerClient("images")
                    .getBlobClient(destinationFilename)
                    .getPageBlobClient();

            destinationPageBlobClient.create(fileSize, true);
            try (BlobLeaser blobLeaser = new BlobLeaser(destinationPageBlobClient)) {
                PageBlobRequestConditions destinationRequestConditions = new PageBlobRequestConditions().setLeaseId(blobLeaser.getLeaseId());
                destinationPageBlobClient.setMetadataWithResponse(sourceBlobProperties.getMetadata(), destinationRequestConditions, Duration.of(120, ChronoUnit.SECONDS), Context.NONE);

                List<Runnable> copyTaskList = new CopyTaskFactory().createCopyTasks(destinationPageBlobClient, sourceBlobUrl, destinationRequestConditions, chunkCalculator);
                CompletableFuture<Void>[] completableFutures = copyTaskList.stream()
                        .map(task -> CompletableFuture.runAsync(task, threadPoolExecutor))
                        .toArray(CompletableFuture[]::new);
                CompletableFuture copyTasks = CompletableFuture.allOf(completableFutures);
                taskState = TaskState.STARTED;
                pollWithTimeout(copyTasks, blobLeaser, TASK_TIMEOUT);
            }
        } catch (Exception e) {
            System.out.println("Finished with exception: " + e);
            exception = e;
            taskState = TaskState.FAILED;
        }
    }

    private BlobProperties getPublicBlobProperties(String sourceBlobUrl) {
        BlobClient blobClient = new BlobClientBuilder().endpoint(sourceBlobUrl).buildClient();
        return blobClient.getProperties();
    }

    private void pollWithTimeout(CompletableFuture copyTasks, BlobLeaser blobLeaser, Duration timeout) {
        Instant instant = Instant.now();
        Instant timeoutAt = instant.plus(timeout);

        do {
            if (copyTasks.isDone()) {
                return;
            }
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            blobLeaser.renew();
        } while (Instant.now().isBefore(timeoutAt));

        if (copyTasks.isCompletedExceptionally()) {
            taskState = TaskState.FAILED;
        } else if (copyTasks.isDone()) {
            taskState = TaskState.READY;
        } else {
            taskState = TaskState.TIMEOUT;
        }
    }

    private static final class BlobLeaser implements AutoCloseable {

        private final BlobLeaseClient destinationBlobLeaseClient;

        private final String leaseId;

        public BlobLeaser(PageBlobClient destinationPageBlobClient) {
            this.destinationBlobLeaseClient = new BlobLeaseClientBuilder().blobClient(destinationPageBlobClient).buildClient();
            leaseId = destinationBlobLeaseClient.acquireLease(MAX_LEASE_DURATION);
        }

        public void renew() {
            destinationBlobLeaseClient.renewLease();
        }

        @Override
        public void close() {
            destinationBlobLeaseClient.releaseLease();
        }

        public String getLeaseId() {
            return leaseId;
        }
    }
}
