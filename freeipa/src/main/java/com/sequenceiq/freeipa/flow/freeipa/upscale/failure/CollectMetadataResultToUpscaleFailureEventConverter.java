package com.sequenceiq.freeipa.flow.freeipa.upscale.failure;

import static com.sequenceiq.freeipa.flow.freeipa.common.FailureType.ERROR;

import java.util.Map;
import java.util.Set;

import com.sequenceiq.cloudbreak.cloud.event.instance.CollectMetadataResult;
import com.sequenceiq.flow.core.PayloadConverter;
import com.sequenceiq.freeipa.flow.freeipa.upscale.event.UpscaleFailureEvent;

public class CollectMetadataResultToUpscaleFailureEventConverter implements PayloadConverter<UpscaleFailureEvent> {
    @Override
    public boolean canConvert(Class<?> sourceClass) {
        return CollectMetadataResult.class.isAssignableFrom(sourceClass);
    }

    @Override
    public UpscaleFailureEvent convert(Object payload) {
        CollectMetadataResult result = (CollectMetadataResult) payload;
        return new UpscaleFailureEvent(
                result.getResourceId(),
                "Collecting metadata",
                Set.of(),
                ERROR,
                Map.of(),
                result.getException()
        );
    }
}