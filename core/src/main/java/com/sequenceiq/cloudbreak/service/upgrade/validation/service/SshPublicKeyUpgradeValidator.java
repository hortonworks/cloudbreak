package com.sequenceiq.cloudbreak.service.upgrade.validation.service;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.exception.UpgradeValidationFailedException;
import com.sequenceiq.cloudbreak.domain.StackAuthentication;
import com.sequenceiq.cloudbreak.dto.StackDtoDelegate;
import com.sequenceiq.cloudbreak.util.PublicKeyReaderUtil;
import com.sequenceiq.common.model.OsType;

/**
 * Upgrades and image changes to a RHEL 9 image are validated here, because RHEL 9 rejects RSA keys shorter than 2048 bits over SSH. Fail the upgrade
 * early instead of leaving unreachable nodes. Upgrades that keep the nodes on an older OS are not affected.
 * Only the too-short RSA case is blocking, other parse problems are ignored to keep existing upgrades working.
 */
@Component
public class SshPublicKeyUpgradeValidator implements ServiceUpgradeValidator {

    private static final Logger LOGGER = LoggerFactory.getLogger(SshPublicKeyUpgradeValidator.class);

    @Override
    public void validate(ServiceUpgradeValidationRequest validationRequest) {
        validate(validationRequest.stack(), validationRequest.clusterUpgradeProperties().getTargetOsType());
    }

    public void validate(StackDtoDelegate stack, OsType targetOsType) {
        if (OsType.RHEL9 == targetOsType) {
            StackAuthentication authentication = stack.getStackAuthentication();
            if (authentication != null && !authentication.passwordAuthenticationRequired() && StringUtils.isNotBlank(authentication.getPublicKey())) {
                PublicKeyReaderUtil.findTooShortRsaKeyMessage(authentication.getPublicKey()).ifPresent(message -> {
                    LOGGER.info("Refusing upgrade of stack {} to {} as the SSH public key is not acceptable: {}", stack.getName(), targetOsType, message);
                    throw new UpgradeValidationFailedException(String.format("%s. Please create new SSH keys for the environment by editing 'Root SSH' " +
                            "on the environment's Summary page or with this command: 'cdp environments update-ssh-key'", message));
                });
            }
        } else {
            LOGGER.debug("SSH public key length check is skipped for stack {}, target OS is {}", stack.getName(), targetOsType);
        }
    }
}
