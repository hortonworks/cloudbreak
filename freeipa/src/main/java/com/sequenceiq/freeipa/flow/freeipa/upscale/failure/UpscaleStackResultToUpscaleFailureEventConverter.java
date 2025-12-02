package com.sequenceiq.freeipa.flow.freeipa.upscale.failure;

import static com.sequenceiq.freeipa.flow.freeipa.common.FailureType.ERROR;

import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import com.sequenceiq.flow.core.PayloadConverter;
import com.sequenceiq.freeipa.flow.freeipa.upscale.event.UpscaleFailureEvent;
import com.sequenceiq.freeipa.flow.freeipa.upscale.event.UpscaleStackResult;

public class UpscaleStackResultToUpscaleFailureEventConverter implements PayloadConverter<UpscaleFailureEvent> {
    @Override
    public boolean canConvert(Class<?> sourceClass) {
        return UpscaleStackResult.class.isAssignableFrom(sourceClass);
    }

    @Override
    public UpscaleFailureEvent convert(Object payload) {
        UpscaleStackResult result = (UpscaleStackResult) payload;
        return new UpscaleFailureEvent(
                result.getResourceId(),
                "Adding instances",
                Set.of(),
                ERROR,
                StringUtils.isNotEmpty(result.getStatusReason()) ? Map.of("statusReason", result.getStatusReason()) : Map.of(),
                result.getException()
        );
    }
}