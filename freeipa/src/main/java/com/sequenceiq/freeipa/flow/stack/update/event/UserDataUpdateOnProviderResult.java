package com.sequenceiq.freeipa.flow.stack.update.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformResult;
import com.sequenceiq.cloudbreak.common.event.FlowPayload;
import com.sequenceiq.cloudbreak.common.event.Selectable;

public class UserDataUpdateOnProviderResult extends CloudPlatformResult implements Selectable, FlowPayload {

    @JsonCreator
    public UserDataUpdateOnProviderResult(@JsonProperty("resourceId") Long resourceId) {
        super(resourceId);
    }
}
