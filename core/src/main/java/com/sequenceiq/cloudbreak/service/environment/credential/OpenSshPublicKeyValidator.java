package com.sequenceiq.cloudbreak.service.environment.credential;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.aspect.Measure;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.util.PublicKeyReaderUtil;

@Component
public class OpenSshPublicKeyValidator {

    private static final Logger LOGGER = LoggerFactory.getLogger(OpenSshPublicKeyValidator.class);

    @Measure(OpenSshPublicKeyValidator.class)
    public void validate(String publicKey, boolean fipsEnabled) {
        try {
            PublicKeyReaderUtil.load(publicKey, fipsEnabled);
        } catch (Exception e) {
            String errorMessage = String.format("Could not validate publickey certificate [certificate: '%s'], detailed message: %s",
                    publicKey, e.getMessage());
            if (e.getCause() instanceof IllegalArgumentException) {
                errorMessage = String.format("The provided public key ['%s'] is not valid, possibly due to insufficient strength. Cause: %s. " +
                                "Please create new SSH keys for the environment by editing 'Root SSH' on the environment's Summary page " +
                                "or with this command: 'cdp environments update-ssh-key'",
                        publicKey, e.getCause().getMessage());
            }
            LOGGER.info(errorMessage, e);
            throw new BadRequestException(errorMessage, e);
        }
    }

}
