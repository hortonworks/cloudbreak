package com.sequenceiq.datalake.flow.dr.backup.event;

import static com.sequenceiq.datalake.flow.dr.backup.DatalakeBackupEvent.DATALAKE_BACKUP_FAILURE_HANDLED_EVENT;

import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.datalake.flow.SdxContext;
import com.sequenceiq.datalake.flow.SdxEvent;

public class DatalakeBackupFailureHandledEvent extends SdxEvent {
    @JsonCreator
    public DatalakeBackupFailureHandledEvent(
            @JsonProperty("resourceId") Long sdxId,
            @JsonProperty("userId") String userId) {
        super(DATALAKE_BACKUP_FAILURE_HANDLED_EVENT.event(), sdxId, userId);
    }

    public static DatalakeBackupFailureHandledEvent from(Optional<SdxContext> context, SdxEvent event) {
        return new DatalakeBackupFailureHandledEvent(context.map(SdxContext::getSdxId).orElse(event.getResourceId()), event.getUserId());
    }
}
