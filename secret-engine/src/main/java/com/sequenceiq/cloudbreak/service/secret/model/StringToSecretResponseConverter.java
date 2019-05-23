package com.sequenceiq.cloudbreak.service.secret.model;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.service.secret.service.SecretService;

@Component
public class StringToSecretResponseConverter extends AbstractConversionServiceAwareConverter<String, SecretResponse> {
    @Inject
    private SecretService secretService;

    public SecretResponse convert(String source) {
        return source != null ? secretService.convertToExternal(source) : null;
    }
}
