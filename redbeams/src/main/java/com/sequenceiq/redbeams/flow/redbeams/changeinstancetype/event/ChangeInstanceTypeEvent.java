package com.sequenceiq.redbeams.flow.redbeams.changeinstancetype.event;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.cloud.model.database.DatabaseServerInstanceTypeState;
import com.sequenceiq.cloudbreak.common.event.AcceptResult;
import com.sequenceiq.cloudbreak.common.json.JsonIgnoreDeserialization;
import com.sequenceiq.cloudbreak.eventbus.Promise;
import com.sequenceiq.redbeams.flow.redbeams.common.RedbeamsEvent;

public class ChangeInstanceTypeEvent extends RedbeamsEvent {

    private final String targetInstanceType;

    private final DatabaseServerInstanceTypeState liveState;

    public ChangeInstanceTypeEvent(String selector, Long resourceId, String targetInstanceType, DatabaseServerInstanceTypeState liveState) {
        this(selector, resourceId, new Promise<>(), targetInstanceType, liveState);
    }

    @JsonCreator
    public ChangeInstanceTypeEvent(
            @JsonProperty("selector") String selector,
            @JsonProperty("resourceId") Long resourceId,
            @JsonIgnoreDeserialization @JsonProperty("accepted") Promise<AcceptResult> accepted,
            @JsonProperty("targetInstanceType") String targetInstanceType,
            @JsonProperty("liveState") DatabaseServerInstanceTypeState liveState) {
        super(selector, resourceId, accepted, false);
        this.targetInstanceType = targetInstanceType;
        this.liveState = liveState;
    }

    public String getTargetInstanceType() {
        return targetInstanceType;
    }

    /**
     * The instance type state read from the cloud provider by the latest sync or poll, {@code null} before the first sync.
     */
    public DatabaseServerInstanceTypeState getLiveState() {
        return liveState;
    }

    @Override
    public boolean equalsEvent(RedbeamsEvent other) {
        return isClassAndEqualsEvent(ChangeInstanceTypeEvent.class, other,
                event -> Objects.equals(targetInstanceType, event.targetInstanceType));
    }

    @Override
    public String toString() {
        return "ChangeInstanceTypeEvent{" +
                "targetInstanceType='" + targetInstanceType + '\'' +
                ", liveState=" + liveState +
                "} " + super.toString();
    }
}
