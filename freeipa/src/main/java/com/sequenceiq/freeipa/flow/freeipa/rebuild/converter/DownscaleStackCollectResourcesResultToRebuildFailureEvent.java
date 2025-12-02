package com.sequenceiq.freeipa.flow.freeipa.rebuild.converter;

import static com.sequenceiq.freeipa.flow.freeipa.common.FailureType.ERROR;

import com.sequenceiq.cloudbreak.cloud.event.resource.DownscaleStackCollectResourcesResult;
import com.sequenceiq.flow.core.PayloadConverter;
import com.sequenceiq.freeipa.flow.freeipa.rebuild.event.RebuildFailureEvent;

public class DownscaleStackCollectResourcesResultToRebuildFailureEvent implements PayloadConverter<RebuildFailureEvent> {
    @Override
    public boolean canConvert(Class<?> sourceClass) {
        return DownscaleStackCollectResourcesResult.class.isAssignableFrom(sourceClass);
    }

    @Override
    public RebuildFailureEvent convert(Object payload) {
        DownscaleStackCollectResourcesResult result = (DownscaleStackCollectResourcesResult) payload;
        return new RebuildFailureEvent(result.getResourceId(), ERROR, result.getErrorDetails());
    }
}
