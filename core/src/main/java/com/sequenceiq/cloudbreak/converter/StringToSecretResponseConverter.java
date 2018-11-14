package com.sequenceiq.cloudbreak.converter;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.SecretResponse;
import com.sequenceiq.cloudbreak.service.secret.SecretService;

@Component
public class StringToSecretResponseConverter extends AbstractConversionServiceAwareConverter<String, SecretResponse> {

    @Inject
    private SecretService secretService;

    @Override
    public SecretResponse convert(String source) {
        return secretService.convertToExternal(source);
    }
}
