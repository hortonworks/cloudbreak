package com.sequenceiq.cloudbreak.cloud.gcp.polling;

import java.math.BigInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.dyngr.core.AttemptMaker;
import com.dyngr.core.AttemptResult;
import com.dyngr.core.AttemptResults;
import com.google.api.services.storage.Storage;
import com.google.api.services.storage.model.RewriteResponse;
import com.google.api.services.storage.model.StorageObject;
import com.google.common.base.Strings;

public class GcpImageAttemptMaker implements AttemptMaker {

    private static final Logger LOGGER = LoggerFactory.getLogger(GcpImageAttemptMaker.class);

    private String rewriteToken;

    private final String sourceBucket;

    private final String sourceKey;

    private final String destBucket;

    private final String destKey;

    private final Storage storage;

    public GcpImageAttemptMaker(String rewriteToken, String sourceBucket, String sourceKey, String destBucket, String destKey, Storage storage) {
        this.sourceBucket = sourceBucket;
        this.sourceKey = sourceKey;
        this.destBucket = destBucket;
        this.destKey = destKey;
        this.storage = storage;
        this.rewriteToken = rewriteToken;
    }

    public String getRewriteToken() {
        return rewriteToken;
    }

    public void setRewriteToken(String rewriteToken) {
        this.rewriteToken = rewriteToken;
    }

    @Override
    public AttemptResult process() throws Exception {
        Storage.Objects.Rewrite rewrite = storage.objects().rewrite(sourceBucket, sourceKey, destBucket, destKey, new StorageObject());
        rewrite.setRewriteToken(rewriteToken);
        RewriteResponse rewriteResponse = rewrite.execute();
        String newRewriteToken = rewriteResponse.getRewriteToken();
        BigInteger totalBytesRewritten = rewriteResponse.getTotalBytesRewritten();
        BigInteger currentSize = rewriteResponse.getObjectSize();
        LOGGER.debug("Rewriting not finished, bytes completed: {}/{}. Calling rewrite again with token {}.",
                currentSize,
                totalBytesRewritten,
                newRewriteToken);
        this.rewriteToken = Strings.isNullOrEmpty(newRewriteToken) ? this.rewriteToken : newRewriteToken;
        if (!rewriteResponse.getDone()) {
            return AttemptResults.justContinue();
        } else {
            return AttemptResults.finishWith(null);
        }
    }
}
