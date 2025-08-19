package com.sequenceiq.freeipa.service.validation;

import java.util.Map;
import java.util.Optional;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.common.model.SeLinux;
import com.sequenceiq.freeipa.entity.SecurityConfig;
import com.sequenceiq.freeipa.entity.Stack;

@Service
public class SeLinuxValidationService {

    public static final String SELINUX_SUPPORTED_TAG = "selinux-supported";

    private static final Logger LOGGER = LoggerFactory.getLogger(SeLinuxValidationService.class);

    @Inject
    private EntitlementService entitlementService;

    public void validateSeLinuxEntitlementGranted(Stack stack) {
        SeLinux selinuxModeOfStack = getSelinuxModeOfStack(stack);
        validateSeLinuxEntitlementGranted(selinuxModeOfStack);
    }

    public void validateSeLinuxEntitlementGranted(SeLinux targetSelinuxMode) {
        if (SeLinux.ENFORCING.equals(targetSelinuxMode) && !entitlementService.isCdpSecurityEnforcingSELinux(ThreadBasedUserCrnProvider.getAccountId())) {
            throw new CloudbreakServiceException("You are not entitled to use SELinux enforcing mode. " +
                    "Please contact your CDP administrator about the enablement of this feature!");
        }
    }

    public void validateSeLinuxSupportedOnTargetImage(Stack stack, Image targetImage) {
        validateSeLinuxSupportedOnTargetImage(stack, targetImage.getTags(), targetImage.getImageId());
    }

    public void validateSeLinuxSupportedOnTargetImage(Stack stack, com.sequenceiq.freeipa.api.v1.freeipa.stack.model.image.Image targetImage) {
        validateSeLinuxSupportedOnTargetImage(stack, targetImage.getTags(), targetImage.getUuid());
    }

    private static void validateSeLinuxSupportedOnTargetImage(Stack stack, Map<String, String> imageTags, String imageId) {
        SeLinux selinuxModeOfStack = getSelinuxModeOfStack(stack);
        if (SeLinux.ENFORCING.equals(selinuxModeOfStack)) {
            Boolean selinuxSupportedOnTargetImage = Boolean.valueOf(imageTags.getOrDefault(SELINUX_SUPPORTED_TAG, Boolean.TRUE.toString()));
            LOGGER.debug("SELinux supported on target image '{}': '{}'", imageId, selinuxSupportedOnTargetImage);
            if (!selinuxSupportedOnTargetImage) {
                throw new CloudbreakServiceException("SELinux enforcing mode is not supported on target image. " +
                        "Please select another image!");
            }
        }
    }

    private static SeLinux getSelinuxModeOfStack(Stack stack) {
        SeLinux selinuxModeOfStack = Optional.ofNullable(stack.getSecurityConfig())
                .map(SecurityConfig::getSeLinux)
                .orElse(SeLinux.PERMISSIVE);
        LOGGER.debug("SELinux mode of stack '{}': '{}'", stack.getId(), selinuxModeOfStack);
        return selinuxModeOfStack;
    }
}
