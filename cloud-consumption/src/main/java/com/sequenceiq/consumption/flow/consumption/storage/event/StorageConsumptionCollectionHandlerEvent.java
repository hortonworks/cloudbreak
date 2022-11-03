package com.sequenceiq.consumption.flow.consumption.storage.event;

import java.util.Set;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.common.event.AcceptResult;
import com.sequenceiq.cloudbreak.common.json.JsonIgnoreDeserialization;
import com.sequenceiq.cloudbreak.eventbus.Promise;
import com.sequenceiq.consumption.dto.StorageConsumptionResult;
import com.sequenceiq.consumption.flow.consumption.ConsumptionContext;

public class StorageConsumptionCollectionHandlerEvent extends StorageConsumptionCollectionEvent {

    private final ConsumptionContext context;

    private final Set<StorageConsumptionResult> storageConsumptionResults;

    public StorageConsumptionCollectionHandlerEvent(String selector, Long resourceId, String resourceCrn, ConsumptionContext context,
            Set<StorageConsumptionResult> storageConsumptionResults) {
        super(selector, resourceId, resourceCrn);
        this.context = context;
        this.storageConsumptionResults = storageConsumptionResults;
    }

    @JsonCreator
    public StorageConsumptionCollectionHandlerEvent(
            @JsonProperty("selector") String selector,
            @JsonProperty("resourceId") Long resourceId,
            @JsonProperty("resourceCrn") String resourceCrn,
            @JsonProperty("context") ConsumptionContext context,
            @JsonProperty("storageConsumptionResults") Set<StorageConsumptionResult> storageConsumptionResults,
            @JsonIgnoreDeserialization @JsonProperty("accepted") Promise<AcceptResult> accepted) {

        super(selector, resourceId, resourceCrn, accepted);
        this.context = context;
        this.storageConsumptionResults = storageConsumptionResults;
    }

    public ConsumptionContext getContext() {
        return context;
    }

    public Set<StorageConsumptionResult> getStorageConsumptionResults() {
        return storageConsumptionResults;
    }

    @Override
    public String toString() {
        return "StorageConsumptionCollectionHandlerEvent{" +
                "context=" + context +
                ", storageConsumptionResults=" + storageConsumptionResults +
                "} " + super.toString();
    }
}
