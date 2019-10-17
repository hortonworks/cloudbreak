package com.sequenceiq.freeipa.flow.freeipa.cleanup.event.failure;

import com.sequenceiq.flow.core.PayloadConverter;
import com.sequenceiq.freeipa.flow.freeipa.cleanup.event.vault.RemoveVaultEntriesResponse;

public class RemoveVaultEntriesResponseToCleanupFailureEventConverter implements PayloadConverter<CleanupFailureEvent> {
    @Override
    public boolean canConvert(Class<?> sourceClass) {
        return RemoveVaultEntriesResponse.class.isAssignableFrom(sourceClass);
    }

    @Override
    public CleanupFailureEvent convert(Object payload) {
        RemoveVaultEntriesResponse removeVaultEntriesResponse = (RemoveVaultEntriesResponse) payload;
        CleanupFailureEvent event = new CleanupFailureEvent(removeVaultEntriesResponse, "DNS record removal",
                removeVaultEntriesResponse.getVaultCleanupFailed(), removeVaultEntriesResponse.getVaultCleanupSuccess());
        return event;
    }
}
