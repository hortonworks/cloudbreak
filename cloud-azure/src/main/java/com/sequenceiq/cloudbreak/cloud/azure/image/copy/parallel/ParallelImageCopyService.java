package com.sequenceiq.cloudbreak.cloud.azure.image.copy.parallel;

import java.util.List;
import java.util.Optional;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.azure.core.http.rest.Response;
import com.azure.storage.blob.BlobAsyncClient;
import com.azure.storage.blob.BlobClientBuilder;
import com.azure.storage.blob.models.BlobProperties;
import com.azure.storage.blob.models.PageBlobItem;
import com.azure.storage.blob.models.PageBlobRequestConditions;
import com.azure.storage.blob.models.PageRange;
import com.azure.storage.blob.specialized.BlobLeaseAsyncClient;
import com.azure.storage.blob.specialized.BlobLeaseClientBuilder;
import com.azure.storage.blob.specialized.PageBlobAsyncClient;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.cloud.azure.image.AzureImageInfo;
import com.sequenceiq.cloudbreak.cloud.azure.image.copy.parallel.exception.ParallelImageCopyException;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.common.service.Clock;

import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

@Service
public class ParallelImageCopyService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ParallelImageCopyService.class);

    private static final long CHUNK_SIZE = 4194304L;

    @Inject
    private ParallelImageCopyParametersService imageCopyParameters;

    @Inject
    private ResponseCodeHandlerService responseCodeHandlerService;

    @Inject
    private CopyTaskService copyTaskService;

    @Inject
    private Clock clock;

    public void copyImage(Image image, AzureClient client, String imageStorageName, String imageResourceGroupName, AzureImageInfo azureImageInfo) {
        try {
            PageBlobAsyncClient pageBlobAsyncClient
                    = client.getPageBlobAsyncClient(imageResourceGroupName, imageStorageName, "images", azureImageInfo.getImageName());
            BlobLeaseAsyncClient blobLeaseAsyncClient = new BlobLeaseClientBuilder().blobAsyncClient(pageBlobAsyncClient).buildAsyncClient();
            String sourceBlob = image.getImageName();

            ImageCopyContext imageCopyContext = new ImageCopyContext();
            Optional<Response<String>> leaseResponse = getPublicBlobPropertiesAsync(Mono.just(sourceBlob))
                    .flatMap(blobPropertiesResponse -> createImage(pageBlobAsyncClient, imageCopyContext, blobPropertiesResponse))
                    .flatMap(pageBlobItemResponse -> startLeaseImage(blobLeaseAsyncClient))
                    .blockOptional();
            if (leaseResponse.isEmpty()) {
                throw new ParallelImageCopyException("Leasing the blob was not successful");
            }

            Mono.just(leaseResponse.get())
                    .flatMap(lr -> copyImageChunks(sourceBlob, pageBlobAsyncClient, blobLeaseAsyncClient, imageCopyContext, lr.getValue()))
                    .timeout(imageCopyParameters.getImageCopyTimeout())
                    .subscribe();
        } catch (Exception e) {
            throw new ParallelImageCopyException("Error during image copy", e);
        }
    }

    private Mono<Void> copyImageChunks(String sourceBlob, PageBlobAsyncClient pageBlobAsyncClient, BlobLeaseAsyncClient blobLeaseAsyncClient, ImageCopyContext imageCopyContext, String leaseId) {
        ChunkCalculator chunkCalculator = new ChunkCalculator(imageCopyContext.getFilesize(), CHUNK_SIZE);
        List<PageRange> pageRanges = new PageRangeCalculator().getAll(chunkCalculator);
        PageBlobRequestConditions pageBlobRequestConditions = new PageBlobRequestConditions().setLeaseId(leaseId);
        return Mono.just(new CopyTaskParameters(pageBlobAsyncClient, sourceBlob, pageBlobRequestConditions, pageRanges, blobLeaseAsyncClient))
                .flatMap(copyTasks -> copyTaskService.createAll(copyTasks))
                .doOnError(e -> LOGGER.warn("Copy tasks finished with error: "))
                .doFinally(s -> LOGGER.info("Copy task finished, signal type: {}", s));
    }

    private Mono<Response<String>> startLeaseImage(BlobLeaseAsyncClient blobLeaseAsyncClient) {
        return blobLeaseAsyncClient.acquireLeaseWithResponse(imageCopyParameters.getBlobLeaseDurationSeconds(), null)
                .doOnSuccess(resp -> responseCodeHandlerService.handleResponse(resp.getStatusCode()))
                .retryBackoff(imageCopyParameters.getRetryCount(), imageCopyParameters.getBackoffMinimum(), imageCopyParameters.getBackoffMaximum());
    }

    private Mono<Response<PageBlobItem>> createImage(PageBlobAsyncClient pageBlobAsyncClient, ImageCopyContext imageCopyContext, Response<BlobProperties> blobPropertiesResponse) {
        long fileSize = blobPropertiesResponse.getValue().getBlobSize();
        imageCopyContext.setFilesize(fileSize);
        ParallelCopyProgressInfo parallelCopyProgressInfo = ParallelCopyProgressInfo.init(clock.getCurrentTimeMillis());
        return pageBlobAsyncClient
                .createWithResponse(fileSize, 0L, null, parallelCopyProgressInfo.toMap(), null)
                .doOnSuccess(resp -> responseCodeHandlerService.handleResponse(resp.getStatusCode()))
                .retryWhen(Retry.backoff(imageCopyParameters.getRetryCount(), imageCopyParameters.getBackoffMinimum()));
//        Retry.from()
    }

    private Mono<Response<BlobProperties>> getPublicBlobPropertiesAsync(Mono<String> sourceBlobUrl) {
        return sourceBlobUrl.flatMap(this::getPublicBlobProperties)
                .doOnSuccess(resp -> responseCodeHandlerService.handleResponse(resp.getStatusCode()))
                .retryBackoff(imageCopyParameters.getRetryCount(), imageCopyParameters.getBackoffMinimum(), imageCopyParameters.getBackoffMaximum());
    }

    private Mono<Response<BlobProperties>> getPublicBlobProperties(String sourceBlobUrl) {
        BlobAsyncClient blobClient = new BlobClientBuilder().endpoint(sourceBlobUrl).buildAsyncClient();
        return blobClient.getPropertiesWithResponse(null)
                .doOnSuccess(resp -> responseCodeHandlerService.handleResponse(resp.getStatusCode()))
                .retryBackoff(imageCopyParameters.getRetryCount(), imageCopyParameters.getBackoffMinimum(), imageCopyParameters.getBackoffMaximum());
    }

}
