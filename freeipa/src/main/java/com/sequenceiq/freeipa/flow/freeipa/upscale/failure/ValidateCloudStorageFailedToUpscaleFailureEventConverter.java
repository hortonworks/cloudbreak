package com.sequenceiq.freeipa.flow.freeipa.upscale.failure;

import static com.sequenceiq.freeipa.flow.freeipa.common.FailureType.VALIDATION;

import java.util.Map;
import java.util.Set;

import com.sequenceiq.flow.core.PayloadConverter;
import com.sequenceiq.freeipa.flow.freeipa.provision.event.cloudstorage.ValidateCloudStorageFailed;
import com.sequenceiq.freeipa.flow.freeipa.upscale.event.UpscaleFailureEvent;

public class ValidateCloudStorageFailedToUpscaleFailureEventConverter implements PayloadConverter<UpscaleFailureEvent> {
    @Override
    public boolean canConvert(Class<?> sourceClass) {
        return ValidateCloudStorageFailed.class.isAssignableFrom(sourceClass);
    }

    @Override
    public UpscaleFailureEvent convert(Object payload) {
        ValidateCloudStorageFailed result = (ValidateCloudStorageFailed) payload;
        return new UpscaleFailureEvent(
                result.getResourceId(),
                "Cloud storage validation",
                Set.of(),
                VALIDATION,
                result.getException() != null ? Map.of("statusReason", result.getException().getMessage()) : Map.of(),
                result.getException()
        );
    }
}
