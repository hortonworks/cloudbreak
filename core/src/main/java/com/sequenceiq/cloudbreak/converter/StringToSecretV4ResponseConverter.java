package com.sequenceiq.cloudbreak.converter;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.secret.model.SecretResponse;
import com.sequenceiq.secret.service.SecretService;

@Component
public class StringToSecretV4ResponseConverter extends AbstractConversionServiceAwareConverter<String, SecretResponse> {

    @Inject
    private SecretService secretService;

    @Override
    public SecretResponse convert(String source) {
        return secretService.convertToExternal(source);
    }
}
