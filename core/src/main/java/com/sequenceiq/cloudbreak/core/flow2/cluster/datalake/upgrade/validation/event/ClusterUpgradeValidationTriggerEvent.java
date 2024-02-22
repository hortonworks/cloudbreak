package com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event.ClusterUpgradeValidationStateSelectors.START_CLUSTER_UPGRADE_VALIDATION_INIT_EVENT;

import java.util.StringJoiner;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.common.event.AcceptResult;
import com.sequenceiq.cloudbreak.common.json.JsonIgnoreDeserialization;
import com.sequenceiq.cloudbreak.eventbus.Promise;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class ClusterUpgradeValidationTriggerEvent extends StackEvent {

    private final String imageId;

    private final boolean lockComponents;

    private final boolean rollingUpgradeEnabled;

    private final boolean replaceVms;

    @JsonCreator
    public ClusterUpgradeValidationTriggerEvent(
            @JsonProperty("resourceId") Long resourceId,
            @JsonIgnoreDeserialization @JsonProperty("accepted") Promise<AcceptResult> accepted,
            @JsonProperty("imageId") String imageId,
            @JsonProperty("lockComponents") boolean lockComponents,
            @JsonProperty("rollingUpgradeEnabled") boolean rollingUpgradeEnabled,
            @JsonProperty("replaceVms") boolean replaceVms) {
        super(START_CLUSTER_UPGRADE_VALIDATION_INIT_EVENT.event(), resourceId, accepted);
        this.imageId = imageId;
        this.lockComponents = lockComponents;
        this.rollingUpgradeEnabled = rollingUpgradeEnabled;
        this.replaceVms = replaceVms;
    }

    public String getImageId() {
        return imageId;
    }

    public boolean isLockComponents() {
        return lockComponents;
    }

    public boolean isRollingUpgradeEnabled() {
        return rollingUpgradeEnabled;
    }

    public boolean isReplaceVms() {
        return replaceVms;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", ClusterUpgradeValidationTriggerEvent.class.getSimpleName() + "[", "]")
                .add("imageId='" + imageId + "'")
                .add("lockComponents=" + lockComponents)
                .add("rollingUpgradeEnabled=" + rollingUpgradeEnabled)
                .add("replaceVms=" + replaceVms)
                .add(super.toString())
                .toString();
    }
}
