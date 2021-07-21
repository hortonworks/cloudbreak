package com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event.ClusterUpgradeValidationStateSelectors.START_CLUSTER_UPGRADE_VALIDATION_INIT_EVENT;

import com.sequenceiq.cloudbreak.common.event.AcceptResult;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

import reactor.rx.Promise;

public class ClusterUpgradeValidationTriggerEvent extends StackEvent {

    private final String imageId;

    private final boolean lockComponents;

    public ClusterUpgradeValidationTriggerEvent(Long resourceId, Promise<AcceptResult> accepted, String imageId, boolean lockComponents) {
        super(START_CLUSTER_UPGRADE_VALIDATION_INIT_EVENT.event(), resourceId, accepted);
        this.imageId = imageId;
        this.lockComponents = lockComponents;
    }

    public String getImageId() {
        return imageId;
    }

    public boolean isLockComponents() {
        return lockComponents;
    }
}
