package com.sequenceiq.freeipa.flow.freeipa.rebuild.converter;

import static com.sequenceiq.freeipa.flow.freeipa.common.FailureType.ERROR;

import com.sequenceiq.cloudbreak.cloud.event.resource.DownscaleStackResult;
import com.sequenceiq.flow.core.PayloadConverter;
import com.sequenceiq.freeipa.flow.freeipa.rebuild.event.RebuildFailureEvent;

public class DownscaleStackResultToRebuildFailureEvent implements PayloadConverter<RebuildFailureEvent> {
    @Override
    public boolean canConvert(Class<?> sourceClass) {
        return DownscaleStackResult.class.isAssignableFrom(sourceClass);
    }

    @Override
    public RebuildFailureEvent convert(Object payload) {
        DownscaleStackResult result = (DownscaleStackResult) payload;
        return new RebuildFailureEvent(result.getResourceId(), ERROR, result.getErrorDetails());
    }
}
