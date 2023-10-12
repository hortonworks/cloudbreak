package com.sequenceiq.datalake.flow.dr.restore.event;

import static com.sequenceiq.datalake.flow.dr.restore.DatalakeRestoreEvent.DATALAKE_RESTORE_FAILURE_HANDLED_EVENT;

import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.datalake.flow.SdxContext;
import com.sequenceiq.datalake.flow.SdxEvent;

public class DatalakeRestoreFailureHandledEvent extends SdxEvent {
    @JsonCreator
    public DatalakeRestoreFailureHandledEvent(
            @JsonProperty("resourceId") Long sdxId,
            @JsonProperty("userId") String userId) {
        super(DATALAKE_RESTORE_FAILURE_HANDLED_EVENT.event(), sdxId, userId);
    }

    public static DatalakeRestoreFailureHandledEvent from(Optional<SdxContext> context, SdxEvent event) {
        return new DatalakeRestoreFailureHandledEvent(context.map(SdxContext::getSdxId).orElse(event.getResourceId()), event.getUserId());
    }
}
