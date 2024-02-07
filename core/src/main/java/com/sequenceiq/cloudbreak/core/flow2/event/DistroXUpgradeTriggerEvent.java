package com.sequenceiq.cloudbreak.core.flow2.event;

import java.util.StringJoiner;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.common.event.AcceptResult;
import com.sequenceiq.cloudbreak.common.json.JsonIgnoreDeserialization;
import com.sequenceiq.cloudbreak.eventbus.Promise;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.service.image.ImageChangeDto;

public class DistroXUpgradeTriggerEvent extends StackEvent {

    private final ImageChangeDto imageChangeDto;

    private final boolean replaceVms;

    private final boolean lockComponents;

    private final String triggeredStackVariant;

    private final boolean rollingUpgradeEnabled;

    @JsonCreator
    public DistroXUpgradeTriggerEvent(
            @JsonProperty("selector") String selector,
            @JsonProperty("resourceId") Long stackId,
            @JsonIgnoreDeserialization @JsonProperty("accepted") Promise<AcceptResult> accepted,
            @JsonProperty("imageChangeDto") ImageChangeDto imageChangeDto,
            @JsonProperty("replaceVms") boolean replaceVms,
            @JsonProperty("lockComponents") boolean lockComponents,
            @JsonProperty("triggeredStackVariant") String triggeredStackVariant,
            @JsonProperty("rollingUpgradeEnabled") boolean rollingUpgradeEnabled) {
        super(selector, stackId, accepted);
        this.imageChangeDto = imageChangeDto;
        this.replaceVms = replaceVms;
        this.lockComponents = lockComponents;
        this.triggeredStackVariant = triggeredStackVariant;
        this.rollingUpgradeEnabled = rollingUpgradeEnabled;
    }

    public ImageChangeDto getImageChangeDto() {
        return imageChangeDto;
    }

    public boolean isReplaceVms() {
        return replaceVms;
    }

    public boolean isLockComponents() {
        return lockComponents;
    }

    public String getTriggeredStackVariant() {
        return triggeredStackVariant;
    }

    public boolean isRollingUpgradeEnabled() {
        return rollingUpgradeEnabled;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", DistroXUpgradeTriggerEvent.class.getSimpleName() + "[", "]")
                .add("imageChangeDto=" + imageChangeDto)
                .add("replaceVms=" + replaceVms)
                .add("lockComponents=" + lockComponents)
                .add("rollingUpgradeEnabled=" + rollingUpgradeEnabled)
                .toString();
    }
}
