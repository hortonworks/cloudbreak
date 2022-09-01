package com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.embeddeddb;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.common.database.TargetMajorVersion;

public class UpgradeEmbeddedDBPreparationRequest extends AbstractUpgradeEmbeddedDBPreparationEvent {
    @JsonCreator
    public UpgradeEmbeddedDBPreparationRequest(
            @JsonProperty("resourceId") Long stackId,
            @JsonProperty("version") TargetMajorVersion version) {
        super(stackId, version);
    }
}
