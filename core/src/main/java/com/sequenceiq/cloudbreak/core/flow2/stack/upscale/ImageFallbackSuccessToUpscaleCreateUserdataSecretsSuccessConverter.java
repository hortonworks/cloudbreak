package com.sequenceiq.cloudbreak.core.flow2.stack.upscale;

import java.util.List;

import com.sequenceiq.cloudbreak.common.event.Payload;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.ImageFallbackSuccess;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.userdata.UpscaleCreateUserdataSecretsSuccess;
import com.sequenceiq.flow.core.PayloadConverter;

public class ImageFallbackSuccessToUpscaleCreateUserdataSecretsSuccessConverter implements PayloadConverter<UpscaleCreateUserdataSecretsSuccess> {
    @Override
    public boolean canConvert(Class<?> sourceClass) {
        return ImageFallbackSuccess.class.isAssignableFrom(sourceClass);
    }

    @Override
    public UpscaleCreateUserdataSecretsSuccess convert(Object payload) {
        return new UpscaleCreateUserdataSecretsSuccess(((Payload) payload).getResourceId(), List.of());
    }
}
