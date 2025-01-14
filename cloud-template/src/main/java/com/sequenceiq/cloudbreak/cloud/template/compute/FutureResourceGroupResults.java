package com.sequenceiq.cloudbreak.cloud.template.compute;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.Future;

import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;

public class FutureResourceGroupResults {

    private final CloudInstancesGroupProcessingBatch batch;

    private final Collection<Future<ResourceRequestResult<List<CloudResourceStatus>>>> futures;

    public FutureResourceGroupResults(CloudInstancesGroupProcessingBatch batch, Collection<Future<ResourceRequestResult<List<CloudResourceStatus>>>> futures) {
        this.batch = batch;
        this.futures = futures;
    }

    public CloudInstancesGroupProcessingBatch getBatch() {
        return batch;
    }

    public Collection<Future<ResourceRequestResult<List<CloudResourceStatus>>>> getFutures() {
        return futures;
    }
}