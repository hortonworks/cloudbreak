package com.sequenceiq.environment.environment.service;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.consumption.api.v1.consumption.model.request.StorageConsumptionRequest;
import com.sequenceiq.consumption.client.ConsumptionInternalCrnClient;

@Service
public class ConsumptionCommunicator {

    @Inject
    private ConsumptionInternalCrnClient consumptionInternalCrnClient;

    public void asdasd(String accountId, StorageConsumptionRequest request) {
        consumptionInternalCrnClient.withInternalCrn().consumptionEndpoint()
                .scheduleStorageConsumptionCollection(accountId, request);
    }
}
