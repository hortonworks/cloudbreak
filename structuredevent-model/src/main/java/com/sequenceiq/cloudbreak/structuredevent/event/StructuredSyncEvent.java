package com.sequenceiq.cloudbreak.structuredevent.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.sequenceiq.cloudbreak.structuredevent.event.legacy.OperationDetails;
import com.sequenceiq.cloudbreak.structuredevent.json.Base64Deserializer;
import com.sequenceiq.cloudbreak.structuredevent.json.Base64Serializer;

@JsonIgnoreProperties(ignoreUnknown = true)
public class StructuredSyncEvent extends StructuredEvent {

    private SyncDetails syncDetails;

    @JsonSerialize(using = Base64Serializer.class)
    @JsonDeserialize(using = Base64Deserializer.class)
    private String exception;

    public StructuredSyncEvent() {
        super(StructuredSyncEvent.class.getSimpleName());
    }

    public StructuredSyncEvent(OperationDetails operationDetails, SyncDetails syncDetails) {
        super(StructuredSyncEvent.class.getSimpleName(), operationDetails);
        this.syncDetails = syncDetails;
    }

    @Override
    public String getStatus() {
        return SENT;
    }

    @Override
    public Long getDuration() {
        return ZERO;
    }

    public SyncDetails getsyncDetails() {
        return syncDetails;
    }

    public void setsyncDetails(SyncDetails syncDetails) {
        this.syncDetails = syncDetails;
    }

    public String getException() {
        return exception;
    }

    public void setException(String exception) {
        this.exception = exception;
    }
}
