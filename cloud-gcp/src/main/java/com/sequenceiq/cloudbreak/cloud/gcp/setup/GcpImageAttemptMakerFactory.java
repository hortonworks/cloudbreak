package com.sequenceiq.cloudbreak.cloud.gcp.setup;

import org.springframework.stereotype.Service;

import com.google.api.services.storage.Storage;

@Service
public class GcpImageAttemptMakerFactory {

    public GcpImageAttemptMaker create(String rewriteToken, String sourceBucket, String sourceKey, String destBucket, String destKey, Storage storage) {
        return new GcpImageAttemptMaker(rewriteToken, sourceBucket, sourceKey, destBucket, destKey, storage);
    }

}
