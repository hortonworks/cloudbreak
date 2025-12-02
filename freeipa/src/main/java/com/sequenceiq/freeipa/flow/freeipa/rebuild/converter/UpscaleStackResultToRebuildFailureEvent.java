package com.sequenceiq.freeipa.flow.freeipa.rebuild.converter;

import static com.sequenceiq.freeipa.flow.freeipa.common.FailureType.ERROR;

import com.sequenceiq.flow.core.PayloadConverter;
import com.sequenceiq.freeipa.flow.freeipa.rebuild.event.RebuildFailureEvent;
import com.sequenceiq.freeipa.flow.freeipa.upscale.event.UpscaleStackResult;

public class UpscaleStackResultToRebuildFailureEvent implements PayloadConverter<RebuildFailureEvent> {
    @Override
    public boolean canConvert(Class<?> sourceClass) {
        return UpscaleStackResult.class.isAssignableFrom(sourceClass);
    }

    @Override
    public RebuildFailureEvent convert(Object payload) {
        UpscaleStackResult upscaleStackResult = (UpscaleStackResult) payload;
        return new RebuildFailureEvent(upscaleStackResult.getResourceId(), ERROR, upscaleStackResult.getErrorDetails());
    }
}
