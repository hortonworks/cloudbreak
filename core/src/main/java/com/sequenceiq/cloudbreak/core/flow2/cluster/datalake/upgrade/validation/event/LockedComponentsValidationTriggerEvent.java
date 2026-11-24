package com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.common.event.AcceptResult;
import com.sequenceiq.cloudbreak.common.json.JsonIgnoreDeserialization;
import com.sequenceiq.cloudbreak.eventbus.Promise;
import com.sequenceiq.cloudbreak.service.image.ImageChangeDto;

public class LockedComponentsValidationTriggerEvent extends ClusterUpgradeValidationTriggerEvent {

    @JsonCreator
    public LockedComponentsValidationTriggerEvent(@JsonProperty("resourceId") Long resourceId,
            @JsonIgnoreDeserialization @JsonProperty("accepted") Promise<AcceptResult> accepted, @JsonProperty("imageChangeDto") ImageChangeDto imageChangeDto) {
        super(resourceId, accepted, imageChangeDto, true, false, true);
    }
}
