package com.sequenceiq.cloudbreak.core.flow2.event;

import java.util.Objects;
import java.util.StringJoiner;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.common.event.AcceptResult;
import com.sequenceiq.cloudbreak.common.json.JsonIgnoreDeserialization;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.eventbus.Promise;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.service.image.ImageChangeDto;

public class DataLakeUpgradeFlowChainTriggerEvent extends StackEvent {

    private final String imageId;

    private final boolean rollingUpgradeEnabled;

    private final ImageChangeDto imageChangeDto;

    public DataLakeUpgradeFlowChainTriggerEvent(String selector, Long stackId, String imageId, boolean rollingUpgradeEnabled) {
        this(selector, stackId, new Promise<>(), imageId, rollingUpgradeEnabled, null);
    }

    @JsonCreator
    public DataLakeUpgradeFlowChainTriggerEvent(
            @JsonProperty("selector") String event,
            @JsonProperty("resourceId") Long resourceId,
            @JsonIgnoreDeserialization @JsonProperty("accepted") Promise<AcceptResult> accepted,
            @JsonProperty("imageId") String imageId,
            @JsonProperty("rollingUpgradeEnabled") boolean rollingUpgradeEnabled,
            @JsonProperty("imageChangeDto") ImageChangeDto imageChangeDto) {
        super(event, resourceId, accepted);
        this.imageId = imageId;
        this.rollingUpgradeEnabled = rollingUpgradeEnabled;
        this.imageChangeDto = imageChangeDto;
    }

    public ImageChangeDto getImageChangeDto() {
        return imageChangeDto;
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
                event -> Objects.equals(imageId, event.imageId) && rollingUpgradeEnabled == event.rollingUpgradeEnabled
                        && Objects.equals(JsonUtil.convertToTree(imageChangeDto), JsonUtil.convertToTree(event.imageChangeDto)));
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", DataLakeUpgradeFlowChainTriggerEvent.class.getSimpleName() + "[", "]")
                .add("imageId='" + imageId + "'")
                .add("rollingUpgradeEnabled=" + rollingUpgradeEnabled)
                .add("imageChangeDto=" + imageChangeDto)
                .add("super=" + super.toString())
                .toString();
    }
}
