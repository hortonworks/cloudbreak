package com.sequenceiq.freeipa.flow.freeipa.prepareupgrade.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.freeipa.flow.stack.StackEvent;

public class PrepareUpgradeMetadataCollectionSuccess extends StackEvent {

    @JsonCreator
    public PrepareUpgradeMetadataCollectionSuccess(@JsonProperty("resourceId") Long stackId) {
        super(stackId);
    }
}
