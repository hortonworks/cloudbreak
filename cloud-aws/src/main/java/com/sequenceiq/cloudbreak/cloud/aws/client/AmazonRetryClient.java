package com.sequenceiq.cloudbreak.cloud.aws.client;

import java.util.function.Supplier;

import com.amazonaws.SdkClientException;
import com.sequenceiq.cloudbreak.service.Retry.ActionFailedException;

public abstract class AmazonRetryClient {

    protected <T> T mapThrottlingError(Supplier<T> supplier) {
        try {
            return supplier.get();
        } catch (SdkClientException e) {
            if (e.getMessage().contains("Rate exceeded")) {
                throw new ActionFailedException(e.getMessage());
            }
            throw e;
        }
    }
}
