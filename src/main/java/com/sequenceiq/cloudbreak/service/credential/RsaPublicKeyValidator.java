package com.sequenceiq.cloudbreak.service.credential;

import java.security.PublicKey;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.controller.BadRequestException;
import com.sequenceiq.cloudbreak.domain.Credential;

@Component
public class RsaPublicKeyValidator {

    private static final Logger LOGGER = LoggerFactory.getLogger(RsaPublicKeyValidator.class);

    public void validate(Credential credential) {
        try {
            PublicKey load = PublicKeyReaderUtil.load(credential.getPublicKey());
        } catch (Exception e) {
            String errorMessage = String.format("Could not validate publickey certificate [certificate: '%s'], detailed message: %s",
                    credential.getPublicKey(), e.getMessage());
            LOGGER.error(errorMessage, e);
            throw new BadRequestException(errorMessage, e);
        }
    }
}
