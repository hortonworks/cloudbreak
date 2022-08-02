package com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.embeddeddb;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.common.database.TargetMajorVersion;

public class UpgradeEmbeddedDBPreparationTriggerRequest extends AbstractUpgradeEmbeddedDBPreparationEvent {

    @JsonCreator
    public UpgradeEmbeddedDBPreparationTriggerRequest(
            @JsonProperty("selector") String selector,
            @JsonProperty("resourceId") Long stackId,
            @JsonProperty("version") TargetMajorVersion version) {
        super(selector, stackId, version);
    }
}
