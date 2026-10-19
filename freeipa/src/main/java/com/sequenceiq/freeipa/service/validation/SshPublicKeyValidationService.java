package com.sequenceiq.freeipa.service.validation;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.util.PublicKeyReaderUtil;
import com.sequenceiq.common.model.OsType;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.entity.StackAuthentication;

/**
 * RHEL 9 rejects RSA keys shorter than 2048 bits over SSH, so a FreeIPA that is created, rebuilt, upgraded or changed to a RHEL 9 image with such a key
 * would be unreachable. Other OS versions keep accepting short keys.
 */
@Service
public class SshPublicKeyValidationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SshPublicKeyValidationService.class);

    public void validateForTargetOs(Stack stack, OsType targetOsType) {
        validate(stack, targetOsType, "Please create new SSH keys for the environment by editing 'Root SSH' " +
                "on the environment's Summary page or with this command: 'cdp environments update-ssh-key'");
    }

    // used by both environment creation and FreeIPA rebuild, so the advice must not assume either of them
    public void validateForCreation(Stack stack, OsType targetOsType) {
        validate(stack, targetOsType, "The SSH public key of the environment must be an RSA key of at least 2048 bits or an Ed25519 key");
    }

    private void validate(Stack stack, OsType targetOsType, String advice) {
        if (OsType.RHEL9 == targetOsType) {
            StackAuthentication authentication = stack.getStackAuthentication();
            if (authentication != null && StringUtils.isNotBlank(authentication.getPublicKey())) {
                PublicKeyReaderUtil.findTooShortRsaKeyMessage(authentication.getPublicKey()).ifPresent(message -> {
                    LOGGER.warn("Refusing to use {} image for FreeIPA {} as the SSH public key is not acceptable: {}", targetOsType, stack.getName(), message);
                    throw new BadRequestException(String.format("%s. %s", message, advice));
                });
            }
        } else {
            LOGGER.debug("SSH public key length check is skipped for FreeIPA {}, target OS is {}", stack.getName(), targetOsType);
        }
    }
}
