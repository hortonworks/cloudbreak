package com.sequenceiq.cloudbreak.core.flow2.event;

import java.util.Objects;
import java.util.StringJoiner;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.common.event.AcceptResult;
import com.sequenceiq.cloudbreak.common.json.JsonIgnoreDeserialization;
import com.sequenceiq.cloudbreak.eventbus.Promise;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class DataLakeUpgradeFlowChainTriggerEvent extends StackEvent {

    private final String imageId;

    private final boolean rollingUpgradeEnabled;

    public DataLakeUpgradeFlowChainTriggerEvent(String selector, Long stackId, String imageId, boolean rollingUpgradeEnabled) {
        super(selector, stackId);
        this.imageId = imageId;
        this.rollingUpgradeEnabled = rollingUpgradeEnabled;
    }

    @JsonCreator
    public DataLakeUpgradeFlowChainTriggerEvent(
            @JsonProperty("selector") String event,
            @JsonProperty("resourceId") Long resourceId,
            @JsonIgnoreDeserialization @JsonProperty("accepted") Promise<AcceptResult> accepted,
            @JsonProperty("imageId") String imageId,
            @JsonProperty("rollingUpgradeEnabled") boolean rollingUpgradeEnabled) {
        super(event, resourceId, accepted);
        this.imageId = imageId;
        this.rollingUpgradeEnabled = rollingUpgradeEnabled;
    }

    public String getImageId() {
        return imageId;
    }

    public boolean isRollingUpgradeEnabled() {
        return rollingUpgradeEnabled;
    }

    @Override
    public boolean equalsEvent(StackEvent other) {
        return isClassAndEqualsEvent(DataLakeUpgradeFlowChainTriggerEvent.class, other,
                event -> Objects.equals(imageId, event.imageId) && Objects.equals(rollingUpgradeEnabled, event.rollingUpgradeEnabled));
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", DataLakeUpgradeFlowChainTriggerEvent.class.getSimpleName() + "[", "]")
                .add("imageId='" + imageId + "'")
                .add("rollingUpgradeEnabled=" + rollingUpgradeEnabled)
                .toString();
    }
}
