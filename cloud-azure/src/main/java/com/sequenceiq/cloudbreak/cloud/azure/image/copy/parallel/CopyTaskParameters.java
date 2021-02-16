package com.sequenceiq.cloudbreak.cloud.azure.image.copy.parallel;

import java.util.List;

import com.azure.storage.blob.models.PageBlobRequestConditions;
import com.azure.storage.blob.models.PageRange;
import com.azure.storage.blob.specialized.BlobLeaseAsyncClient;
import com.azure.storage.blob.specialized.PageBlobAsyncClient;

public class CopyTaskParameters {

    private final PageBlobAsyncClient pageBlobAsyncClient;

    private final String sourceBlobUrl;

    private final PageBlobRequestConditions destinationRequestConditions;

    private final List<PageRange> copyRanges;

    private final BlobLeaseAsyncClient blobLeaseAsyncClient;

    public CopyTaskParameters(PageBlobAsyncClient pageBlobAsyncClient, String sourceBlobUrl, PageBlobRequestConditions destinationRequestConditions,
            List<PageRange> copyRanges, BlobLeaseAsyncClient blobLeaseAsyncClient) {
        this.pageBlobAsyncClient = pageBlobAsyncClient;
        this.sourceBlobUrl = sourceBlobUrl;
        this.destinationRequestConditions = destinationRequestConditions;
        this.copyRanges = copyRanges;
        this.blobLeaseAsyncClient = blobLeaseAsyncClient;
    }

    public PageBlobAsyncClient getPageBlobAsyncClient() {
        return pageBlobAsyncClient;
    }

    public String getSourceBlobUrl() {
        return sourceBlobUrl;
    }

    public PageBlobRequestConditions getDestinationRequestConditions() {
        return destinationRequestConditions;
    }

    public List<PageRange> getCopyRanges() {
        return copyRanges;
    }

    public BlobLeaseAsyncClient getBlobLeaseAsyncClient() {
        return blobLeaseAsyncClient;
    }
}
