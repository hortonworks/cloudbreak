package com.sequenceiq.freeipa.flow.freeipa.rebuild.converter;

import static com.sequenceiq.freeipa.flow.freeipa.common.FailureType.ERROR;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.freeipa.flow.freeipa.rebuild.event.RebuildFailureEvent;
import com.sequenceiq.freeipa.flow.freeipa.rebuild.event.backup.ValidateBackupFailed;

class ValidateBackupFailedToRebuildFailureEventTest {

    private final ValidateBackupFailedToRebuildFailureEvent underTest = new ValidateBackupFailedToRebuildFailureEvent();

    @Test
    void canConvert() {
        assertTrue(underTest.canConvert(ValidateBackupFailed.class));
    }

    @Test
    void convert() {
        ValidateBackupFailed payload = new ValidateBackupFailed(6L,  new CloudbreakException("fda"), ERROR);

        RebuildFailureEvent result = underTest.convert(payload);

        assertEquals(payload.getResourceId(), result.getResourceId());
        assertEquals(payload.getException(), result.getException());
    }
}