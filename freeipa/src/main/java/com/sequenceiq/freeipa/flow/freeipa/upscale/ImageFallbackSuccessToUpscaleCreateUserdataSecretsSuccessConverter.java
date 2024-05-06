package com.sequenceiq.freeipa.flow.freeipa.upscale;

import java.util.List;

import com.sequenceiq.cloudbreak.common.event.Payload;
import com.sequenceiq.flow.core.PayloadConverter;
import com.sequenceiq.freeipa.flow.freeipa.upscale.event.UpscaleCreateUserdataSecretsSuccess;
import com.sequenceiq.freeipa.flow.stack.provision.event.imagefallback.ImageFallbackSuccess;

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
