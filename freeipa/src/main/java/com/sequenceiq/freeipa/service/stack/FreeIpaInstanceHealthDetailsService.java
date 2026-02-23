package com.sequenceiq.freeipa.service.stack;

import jakarta.inject.Inject;

import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.client.RPCResponse;
import com.sequenceiq.cloudbreak.service.Retry;
import com.sequenceiq.cloudbreak.service.RetryService;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.health.NodeHealthDetails;
import com.sequenceiq.freeipa.client.FreeIpaClientException;
import com.sequenceiq.freeipa.client.RetryableFreeIpaClientException;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.Stack;

@Service
public class FreeIpaInstanceHealthDetailsService {

    @Inject
    private RetryService retryService;

    @Inject
    private FreeIpaInstanceHealthDetailsClientService freeIpaInstanceHealthDetailsClientService;

    @Retryable(value = RetryableFreeIpaClientException.class,
            // Having 3 retries exceeds the RPC timeout for the CDP CLI in the worst case scenario with FreeIPA HA.
            maxAttempts = 2
    )
    public NodeHealthDetails getInstanceHealthDetails(Stack stack, InstanceMetaData instance) throws FreeIpaClientException {
        return freeIpaInstanceHealthDetailsClientService.getInstanceHealthDetails(stack, instance);
    }

    public RPCResponse<Boolean> checkFreeIpaHealth(Stack stack, InstanceMetaData instance) throws Retry.ActionFailedException {
        return retryService.testWith1SecDelayMax5TimesAndMultiplier2WithCheckRetriable(() ->
                freeIpaInstanceHealthDetailsClientService.checkFreeIpaHealth(stack, instance));
    }
}
