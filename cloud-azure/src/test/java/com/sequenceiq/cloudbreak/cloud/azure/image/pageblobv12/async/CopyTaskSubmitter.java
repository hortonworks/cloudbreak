package com.sequenceiq.cloudbreak.cloud.azure.image.pageblobv12.async;

import java.util.ArrayList;
import java.util.List;

import com.azure.storage.blob.models.PageBlobRequestConditions;
import com.azure.storage.blob.models.PageRange;
import com.azure.storage.blob.specialized.PageBlobAsyncClient;

public class CopyTaskSubmitter {

    private PageBlobAsyncClient destinationClient;

    private String sourceBlobUrl;

    private PageBlobRequestConditions destinationRequestConditions;

    public void submitAll(List<PageRange> copyRanges) {
        List<PageRange> failedRanges = new ArrayList<>();

//        List<Completable> responseList = copyRanges.stream().map(r -> {
//
//                    Mono<Response<PageBlobItem>> response = destinationClient.uploadPagesFromUrlWithResponse(
//                            r,
//                            sourceBlobUrl,
//                            r.getStart(),
//                            null,
//                            destinationRequestConditions,
//                            null
//                    ).doOnError(t -> failedRanges.add(r))
//                            .subscribeOn(Scheduler.Worker));
//
//            return RxJava2Adapter.monoToCompletable(response);
//                }
//        ).collect(Collectors.toList());
    }
}

