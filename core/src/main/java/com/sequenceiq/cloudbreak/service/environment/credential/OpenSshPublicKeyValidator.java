package com.sequenceiq.cloudbreak.service.environment.credential;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.aspect.Measure;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.util.PublicKeyReaderUtil;
import com.sequenceiq.common.model.OsType;

@Component
public class OpenSshPublicKeyValidator {

    private static final Logger LOGGER = LoggerFactory.getLogger(OpenSshPublicKeyValidator.class);

    @Measure(OpenSshPublicKeyValidator.class)
    public void validate(String publicKey, boolean fipsEnabled, OsType targetOsType) {
        try {
            PublicKeyReaderUtil.load(publicKey, fipsEnabled);
        } catch (Exception e) {
            if (e instanceof PublicKeyReaderUtil.PublicKeyParseException parseException
                    && parseException.getErrorCode() == PublicKeyReaderUtil.PublicKeyParseException.ErrorCode.RSA_KEY_TOO_SHORT) {
                if (OsType.RHEL9 != targetOsType) {
                    LOGGER.info("The provided RSA public key is too short, but only RHEL 9 images reject it, target OS is {}", targetOsType, e);
                    return;
                }
                String errorMessage = String.format(
                        "The provided public key ['%s'] is not valid. Cause: %s. " +
                        "Please create new SSH keys for the environment by editing 'Root SSH' on the environment's Summary page " +
                        "or with this command: 'cdp environments update-ssh-key'", publicKey, e.getMessage());
                LOGGER.info(errorMessage, e);
                throw new BadRequestException(errorMessage, e);
            } else if (e.getCause() instanceof IllegalArgumentException) {
                if (e.getCause().getMessage().contains("RSA modulus has a small prime factor")) {
                    LOGGER.info("The provided key is not FIPS compliant but ignoring the error to be backward compatible", e);
                } else {
                    String errorMessage = String.format(
                            "The provided public key ['%s'] is not valid, possibly due to insufficient strength. Cause: %s. " +
                            "Please create new SSH keys for the environment by editing 'Root SSH' on the environment's Summary page " +
                            "or with this command: 'cdp environments update-ssh-key'", publicKey, e.getCause().getMessage()
                    );
                    LOGGER.info(errorMessage, e);
                    throw new BadRequestException(errorMessage, e);
                }
            } else {
                String errorMessage = String.format("Could not validate publickey certificate [certificate: '%s'], detailed message: %s",
                        publicKey, e.getMessage());
                LOGGER.info(errorMessage, e);
                throw new BadRequestException(errorMessage, e);
            }

        }
    }

}
