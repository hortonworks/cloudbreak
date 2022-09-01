package com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.embeddeddb;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.common.database.TargetMajorVersion;

public class UpgradeEmbeddedDBPreparationResult extends AbstractUpgradeEmbeddedDBPreparationEvent {

    @JsonCreator
    public UpgradeEmbeddedDBPreparationResult(
            @JsonProperty("resourceId") Long stackId,
            @JsonProperty("version") TargetMajorVersion version) {
        super(stackId, version);
    }
}
