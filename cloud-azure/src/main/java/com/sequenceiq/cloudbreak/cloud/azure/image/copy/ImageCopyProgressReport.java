package com.sequenceiq.cloudbreak.cloud.azure.image.copy;

import java.util.Optional;

import com.microsoft.azure.storage.blob.CloudPageBlob;
import com.microsoft.azure.storage.blob.CopyState;
import com.sequenceiq.cloudbreak.cloud.azure.image.copy.parallel.ParallelCopyProgressInfo;

public class ImageCopyProgressReport {

    private CloudPageBlob cloudPageBlob;

    public ImageCopyProgressReport(CloudPageBlob cloudPageBlob) {
        this.cloudPageBlob = cloudPageBlob;
    }

    public CopyState getSequentialCopyState() {
        return cloudPageBlob.getCopyState();
    }

    public Optional<ParallelCopyProgressInfo> getParallelCopyState() {
        return ParallelCopyProgressInfo.fromMap(cloudPageBlob.getMetadata());
    }
}
