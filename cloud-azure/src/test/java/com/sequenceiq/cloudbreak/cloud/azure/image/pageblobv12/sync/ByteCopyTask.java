package com.sequenceiq.cloudbreak.cloud.azure.image.pageblobv12.sync;

import com.azure.storage.blob.models.PageBlobRequestConditions;
import com.azure.storage.blob.specialized.PageBlobClient;

public class ByteCopyTask implements Runnable {

    private final long startByte;
    private final long endByte;
    private final PageBlobClient destinationPageBlobClient;
    private final String sourceBlobUrl;
    private final PageBlobRequestConditions destinationRequestConditions;
    private final PageBlobCopy pageBlobCopy = new PageBlobCopy();

    public ByteCopyTask(PageBlobClient destinationPageBlobClient, String sourceBlobUrl, long startByte, long endByte, PageBlobRequestConditions destinationRequestConditions) {
        this.startByte = startByte;
        this.endByte = endByte;
        this.destinationPageBlobClient = destinationPageBlobClient;
        this.sourceBlobUrl = sourceBlobUrl;
        this.destinationRequestConditions = destinationRequestConditions;
    }

    @Override
    public void run() {
        pageBlobCopy.copyChunkWithRetry(destinationPageBlobClient, startByte, endByte, sourceBlobUrl, destinationRequestConditions);
    }
}
