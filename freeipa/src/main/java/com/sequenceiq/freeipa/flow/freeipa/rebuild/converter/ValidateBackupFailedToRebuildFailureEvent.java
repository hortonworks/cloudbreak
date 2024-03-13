package com.sequenceiq.freeipa.flow.freeipa.rebuild.converter;

import com.sequenceiq.flow.core.PayloadConverter;
import com.sequenceiq.freeipa.flow.freeipa.rebuild.event.RebuildFailureEvent;
import com.sequenceiq.freeipa.flow.freeipa.rebuild.event.backup.ValidateBackupFailed;

public class ValidateBackupFailedToRebuildFailureEvent implements PayloadConverter<RebuildFailureEvent> {
    @Override
    public boolean canConvert(Class<?> sourceClass) {
        return ValidateBackupFailed.class.isAssignableFrom(sourceClass);
    }

    @Override
    public RebuildFailureEvent convert(Object payload) {
        ValidateBackupFailed result = (ValidateBackupFailed) payload;
        return new RebuildFailureEvent(result.getResourceId(), result.getException());
    }
}
