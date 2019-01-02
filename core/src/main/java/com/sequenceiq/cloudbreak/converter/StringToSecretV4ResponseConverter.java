package com.sequenceiq.cloudbreak.converter;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.responses.SecretV4Response;
import com.sequenceiq.cloudbreak.service.secret.SecretService;

@Component
public class StringToSecretV4ResponseConverter extends AbstractConversionServiceAwareConverter<String, SecretV4Response> {

    @Inject
    private SecretService secretService;

    @Override
    public SecretV4Response convert(String source) {
        return secretService.convertToExternal(source);
    }
}
