package com.sequenceiq.consumption.flow.consumption.storage.event;

import com.sequenceiq.cloudbreak.common.event.AcceptResult;
import com.sequenceiq.flow.reactor.api.event.BaseFlowEvent;

import reactor.rx.Promise;

public class StorageConsumptionCollectionEvent extends BaseFlowEvent {

    private final String environmentCrn;

    private final String storageLocation;

    public StorageConsumptionCollectionEvent(String selector, Long resourceId, String resourceCrn, String environmentCrn, String storageLocation) {
        super(selector, resourceId, resourceCrn);
        this.environmentCrn = environmentCrn;
        this.storageLocation = storageLocation;
    }

    public StorageConsumptionCollectionEvent(String selector, Long resourceId, String resourceCrn, String environmentCrn, String storageLocation,
            Promise<AcceptResult> accepted) {
        super(selector, resourceId, resourceCrn, accepted);
        this.environmentCrn = environmentCrn;
        this.storageLocation = storageLocation;
    }

    public String getEnvironmentCrn() {
        return environmentCrn;
    }

    public String getStorageLocation() {
        return storageLocation;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private String resourceCrn;

        private String environmentCrn;

        private String storageLocation;

        private String selector;

        private Long resourceId;

        private Promise<AcceptResult> accepted;

        private Builder() {
        }

        public Builder withSelector(String selector) {
            this.selector = selector;
            return this;
        }

        public Builder withResourceId(Long resourceId) {
            this.resourceId = resourceId;
            return this;
        }

        public Builder withAccepted(Promise<AcceptResult> accepted) {
            this.accepted = accepted;
            return this;
        }

        public Builder withResourceCrn(String resourceCrn) {
            this.resourceCrn = resourceCrn;
            return this;
        }

        public Builder withEnvironmentCrn(String environmentCrn) {
            this.environmentCrn = environmentCrn;
            return this;
        }

        public Builder withStorageLocation(String storageLocation) {
            this.storageLocation = storageLocation;
            return this;
        }

        public StorageConsumptionCollectionEvent build() {
            return new StorageConsumptionCollectionEvent(selector, resourceId, resourceCrn, environmentCrn, storageLocation, accepted);
        }
    }
}
