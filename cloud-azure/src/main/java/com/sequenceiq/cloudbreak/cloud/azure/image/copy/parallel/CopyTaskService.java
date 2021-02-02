package com.sequenceiq.cloudbreak.cloud.azure.image.copy.parallel;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.azure.core.http.rest.Response;
import com.azure.storage.blob.models.PageBlobItem;
import com.azure.storage.blob.models.PageRange;
import com.azure.storage.blob.options.BlobRenewLeaseOptions;
import com.azure.storage.blob.specialized.BlobLeaseAsyncClient;
import com.sequenceiq.cloudbreak.common.service.Clock;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class CopyTaskService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CopyTaskService.class);

    @Inject
    private ParallelImageCopyParametersService imageCopyParameters;

    @Inject
    private ResponseCodeHandlerService responseCodeHandlerService;

    @Inject
    private Clock clock;

    public Mono<Void> createAll(CopyTaskParameters copyTaskParameters) {

        AtomicLong successCounter = new AtomicLong();
        AtomicBoolean imageCopyRunning = new AtomicBoolean(true);
        return Flux.just(
                getLeaseRenewFlux(copyTaskParameters, imageCopyRunning),
                getCopyFlux(copyTaskParameters, successCounter, imageCopyRunning))
                .flatMap(x -> x, 2, 2)
                .then();
    }

    private Mono<Void> getLeaseRenewFlux(CopyTaskParameters copyTaskParameters, AtomicBoolean imageCopyRunning) {
        BlobLeaseAsyncClient blobLeaseAsyncClient = copyTaskParameters.getBlobLeaseAsyncClient();
        return Mono.just(blobLeaseAsyncClient)
                .delayElement(imageCopyParameters.getBlobLeaseRenewInterval())
                .flatMap(client -> client.renewLeaseWithResponse((BlobRenewLeaseOptions) null))
                .doOnSuccess(resp -> responseCodeHandlerService.handleResponse(resp.getStatusCode()))
                .retryBackoff(imageCopyParameters.getRetryCount(), imageCopyParameters.getBackoffMinimum(), imageCopyParameters.getBackoffMaximum())
                .repeat(imageCopyRunning::get)
                .then(Mono.just(blobLeaseAsyncClient))
                .flatMap(x -> blobLeaseAsyncClient.releaseLeaseWithResponse(copyTaskParameters.getDestinationRequestConditions()))
                .doOnSuccess(resp -> responseCodeHandlerService.handleResponse(resp.getStatusCode()))
                .retryBackoff(imageCopyParameters.getRetryCount(), imageCopyParameters.getBackoffMinimum(), imageCopyParameters.getBackoffMaximum())
                .then();
    }

    private Mono<Void> getCopyFlux(CopyTaskParameters copyTaskParameters, AtomicLong successCounter, AtomicBoolean imageCopyRunning) {
        return Flux.fromIterable(copyTaskParameters.getCopyRanges())
                .flatMap(r -> Mono.just(r)
                                .flatMap(pageRange -> sendToServer(copyTaskParameters, pageRange))
                                .doOnSuccess(resp -> responseCodeHandlerService.handleResponse(resp.getStatusCode()))
                                .retryBackoff(imageCopyParameters.getRetryCount(), imageCopyParameters.getBackoffMinimum(), imageCopyParameters.getBackoffMaximum())
                                .flatMap(copyResp -> evaluateProgress(copyTaskParameters, successCounter))
                                .then()
                        , imageCopyParameters.getConcurrency(), imageCopyParameters.getPrefetch())
                .doOnError(e -> {
                    LOGGER.warn("Image copy failed {} with exception {}", copyTaskParameters.getSourceBlobUrl(), e);
                    reportProgress(copyTaskParameters,
                            ParallelCopyProgressInfo.failed(successCounter.get(), copyTaskParameters.getCopyRanges().size(), e.getMessage(), clock.getCurrentTimeMillis()));
                })
                .doOnComplete(() -> LOGGER.info("Image copy completed successfully"))
                .doFinally(s -> imageCopyRunning.set(false))
                .then();
    }


    private Mono<Response<Void>> evaluateProgress(CopyTaskParameters copyTaskParameters, AtomicLong successCounter) {
        long copiedCounter = successCounter.incrementAndGet();
        int totalTasks = copyTaskParameters.getCopyRanges().size();
        if (copiedCounter % 100 == 0) {
            return reportProgress(copyTaskParameters, ParallelCopyProgressInfo.inProgress(copiedCounter, totalTasks, clock.getCurrentTimeMillis()));
        } else if(copiedCounter == totalTasks) {
            return reportProgress(copyTaskParameters, ParallelCopyProgressInfo.finished(totalTasks, clock.getCurrentTimeMillis()));
        }
        return Mono.empty();
    }

    private Mono<Response<Void>> reportProgress(CopyTaskParameters copyTaskParameters, ParallelCopyProgressInfo parallelCopyProgressInfo) {
        return Mono.just(parallelCopyProgressInfo)
                .flatMap(p -> copyTaskParameters.getPageBlobAsyncClient()
                        .setMetadataWithResponse(parallelCopyProgressInfo.toMap(), copyTaskParameters.getDestinationRequestConditions()))
                .doOnSuccess(resp -> responseCodeHandlerService.handleResponse(resp.getStatusCode()))
                .retryBackoff(imageCopyParameters.getRetryCount(), imageCopyParameters.getBackoffMinimum(), imageCopyParameters.getBackoffMaximum());
    }

    private Mono<Response<PageBlobItem>> sendToServer(CopyTaskParameters copyTaskParameters, PageRange r) {
        return copyTaskParameters.getPageBlobAsyncClient().uploadPagesFromUrlWithResponse(
                r,
                copyTaskParameters.getSourceBlobUrl(),
                r.getStart(),
                null,
                copyTaskParameters.getDestinationRequestConditions(),
                null
        );
    }
}

