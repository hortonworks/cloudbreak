package com.sequenceiq.consumption.flow.consumption.storage.event;

import java.util.Set;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.common.event.AcceptResult;
import com.sequenceiq.cloudbreak.common.json.JsonIgnoreDeserialization;
import com.sequenceiq.cloudbreak.eventbus.Promise;
import com.sequenceiq.consumption.dto.StorageConsumptionResult;

public class SendStorageConsumptionEvent extends StorageConsumptionCollectionEvent {

    private final Set<StorageConsumptionResult> storageConsumptionResults;

    public SendStorageConsumptionEvent(String selector, Long resourceId, String resourceCrn,
        Set<StorageConsumptionResult> storageConsumptionResults) {
        super(selector, resourceId, resourceCrn);
        this.storageConsumptionResults = storageConsumptionResults;
    }

    @JsonCreator
    public SendStorageConsumptionEvent(
            @JsonProperty("selector") String selector,
            @JsonProperty("resourceId") Long resourceId,
            @JsonProperty("resourceCrn") String resourceCrn,
            @JsonProperty("storageConsumptionResults") Set<StorageConsumptionResult> storageConsumptionResults,
            @JsonIgnoreDeserialization @JsonProperty("accepted") Promise<AcceptResult> accepted) {

        super(selector, resourceId, resourceCrn, accepted);
        this.storageConsumptionResults = storageConsumptionResults;
    }

    public Set<StorageConsumptionResult> getStorageConsumptionResults() {
        return storageConsumptionResults;
    }

    @Override
    public String toString() {
        return "SendStorageConsumptionEvent{" +
                "storageConsumptionResults=" + storageConsumptionResults +
                "} " + super.toString();
    }
}
