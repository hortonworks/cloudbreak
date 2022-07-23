package com.sequenceiq.consumption.flow.consumption.storage.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.common.event.AcceptResult;
import com.sequenceiq.cloudbreak.common.json.JsonIgnoreDeserialization;
import com.sequenceiq.consumption.dto.StorageConsumptionResult;

import reactor.rx.Promise;

public class SendStorageConsumptionEvent extends StorageConsumptionCollectionEvent {

    private final StorageConsumptionResult storageConsumptionResult;

    public SendStorageConsumptionEvent(String selector, Long resourceId, String resourceCrn, StorageConsumptionResult storageConsumptionResult) {
        super(selector, resourceId, resourceCrn);
        this.storageConsumptionResult = storageConsumptionResult;
    }

    @JsonCreator
    public SendStorageConsumptionEvent(
            @JsonProperty("selector") String selector,
            @JsonProperty("resourceId") Long resourceId,
            @JsonProperty("resourceCrn") String resourceCrn,
            @JsonProperty("storageConsumptionResult") StorageConsumptionResult storageConsumptionResult,
            @JsonIgnoreDeserialization @JsonProperty("accepted") Promise<AcceptResult> accepted) {

        super(selector, resourceId, resourceCrn, accepted);
        this.storageConsumptionResult = storageConsumptionResult;
    }

    public StorageConsumptionResult getStorageConsumptionResult() {
        return storageConsumptionResult;
    }

    @Override
    public String toString() {
        return "SendStorageConsumptionEvent{" +
                "storageConsumptionResult=" + storageConsumptionResult +
                "} " + super.toString();
    }
}
