package com.sequenceiq.secret.model;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.secret.service.SecretService;

@Component
public class StringToSecretResponseConverter {

    @Inject
    private SecretService secretService;

    public SecretResponse convert(String source) {
        return secretService.convertToExternal(source);
    }
}
