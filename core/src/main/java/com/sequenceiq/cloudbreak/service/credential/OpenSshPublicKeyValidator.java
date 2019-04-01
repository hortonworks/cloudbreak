package com.sequenceiq.cloudbreak.service.credential;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.aspect.Measure;
import com.sequenceiq.cloudbreak.controller.exception.BadRequestException;

@Component
public class OpenSshPublicKeyValidator {

    private static final Logger LOGGER = LoggerFactory.getLogger(OpenSshPublicKeyValidator.class);

    @Measure(OpenSshPublicKeyValidator.class)
    public void validate(String publicKey) {
        try {
            PublicKeyReaderUtil.load(publicKey);
        } catch (Exception e) {
            String errorMessage = String.format("Could not validate publickey certificate [certificate: '%s'], detailed message: %s",
                    publicKey, e.getMessage());
            LOGGER.info(errorMessage, e);
            throw new BadRequestException(errorMessage, e);
        }
    }
}
