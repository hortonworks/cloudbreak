package com.sequenceiq.environment.environment.flow.encryptionprofile.event;

import static com.sequenceiq.environment.environment.flow.encryptionprofile.event.EnableEncryptionProfileStateSelectors.FAILED_ENABLE_ENCRYPTION_PROFILE_EVENT;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.flow.reactor.api.event.BaseFailedFlowEvent;

public class EnableEncryptionProfileFailedEvent extends BaseFailedFlowEvent implements Selectable {

    @JsonCreator
    public EnableEncryptionProfileFailedEvent(
            @JsonProperty("resourceId") Long resourceId,
            @JsonProperty("resourceName") String resourceName,
            @JsonProperty("resourceCrn") String resourceCrn,
            @JsonProperty("exception") Exception exception) {
        super(FAILED_ENABLE_ENCRYPTION_PROFILE_EVENT.name(), resourceId, resourceName, resourceCrn, exception);
    }

    @Override
    public String selector() {
        return FAILED_ENABLE_ENCRYPTION_PROFILE_EVENT.name();
    }
}
