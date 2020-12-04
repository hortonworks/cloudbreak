package com.sequenceiq.cloudbreak.cloud.azure.image.pageblobv12.sync;

import static com.azure.storage.blob.models.BlobErrorCode.SERVER_BUSY;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

import com.azure.core.http.rest.Response;
import com.azure.core.util.Context;
import com.azure.storage.blob.models.BlobStorageException;
import com.azure.storage.blob.models.PageBlobItem;
import com.azure.storage.blob.models.PageBlobRequestConditions;
import com.azure.storage.blob.models.PageRange;
import com.azure.storage.blob.specialized.PageBlobClient;

public class PageBlobCopy {

    public static final int SERVER_BUSY_WAIT_TIME = 100;

    public void copyChunkWithRetry(PageBlobClient pageBlobClient, long from, long to, String sourceBlobUrl, PageBlobRequestConditions destinationRequestConditions) {
        PageRange pageRange = new PageRange().setStart(from).setEnd(to);
        copyChunkWithRetry(pageBlobClient, pageRange, sourceBlobUrl, destinationRequestConditions);
    }

    public void copyChunkWithRetry(PageBlobClient pageBlobClient, PageRange pageRange, String sourceBlobUrl, PageBlobRequestConditions destinationRequestConditions) {
        int maxRetry = 5;
        int retryCount = 0;
        boolean retry = false;
        Exception originalException = null;
        do {
            try {
                retry = false;
                Response<PageBlobItem> copyResult = pageBlobClient.uploadPagesFromUrlWithResponse(pageRange, sourceBlobUrl, pageRange.getStart(), null, destinationRequestConditions, null, Duration.of(120, ChronoUnit.SECONDS), Context.NONE);
                if (copyResult.getStatusCode() != 201) {
                    System.out.println("It is not 201");
                    ++retryCount;
                }
            } catch (BlobStorageException e) {
                if (e.getStatusCode() == 503 && SERVER_BUSY.equals(e.getErrorCode())) {
                    sleep(SERVER_BUSY_WAIT_TIME);
                }
            } catch (Exception e) {
                System.out.println(e);
                originalException = e;
                ++retryCount;
            }
        } while (retry && retryCount < maxRetry);
        if (retry) {
            throw new RuntimeException("Too many retries, with original exception.", originalException);
        }
    }

    private void sleep(long millisecs) {
        try {
            Thread.sleep(millisecs);
        } catch (InterruptedException interruptedException) {
            interruptedException.printStackTrace();
        }
    }
}
