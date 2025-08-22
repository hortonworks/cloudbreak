package com.sequenceiq.cloudbreak.service.validation;

import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.CLOUDERA_STACK_VERSION_7_2_18;
import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.isVersionNewerOrEqualThanLimited;

import java.util.Map;
import java.util.Optional;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.cloud.model.catalog.ImagePackageVersion;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.domain.SecurityConfig;
import com.sequenceiq.cloudbreak.dto.StackDtoDelegate;
import com.sequenceiq.common.model.SeLinux;

@Service
public class SeLinuxValidationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SeLinuxValidationService.class);

    private static final String SELINUX_SUPPORTED_TAG = "selinux-supported";

    @Inject
    private EntitlementService entitlementService;

    public void validateSeLinuxEntitlementGranted(StackDtoDelegate stack) {
        SeLinux selinuxModeOfStack = getSelinuxModeOfStack(stack);
        validateSeLinuxEntitlementGranted(selinuxModeOfStack);
    }

    public void validateSeLinuxEntitlementGranted(SeLinux targetSelinuxMode) {
        if (SeLinux.ENFORCING.equals(targetSelinuxMode)) {
            boolean entitled = entitlementService.isCdpSecurityEnforcingSELinux(ThreadBasedUserCrnProvider.getAccountId());
            LOGGER.debug("SELinux entitlement granted for account '{}': '{}'", ThreadBasedUserCrnProvider.getAccountId(), entitled);
            if (!entitled) {
                throw new CloudbreakServiceException("You are not entitled to use SELinux enforcing mode. " +
                        "Please contact your CDP administrator about the enablement of this feature!");
            }
        }
    }

    public void validateSeLinuxSupportedOnTargetImage(StackDtoDelegate stack, Image targetImage) {
        SeLinux selinuxModeOfStack = getSelinuxModeOfStack(stack);
        validateSeLinuxSupportedOnTargetImage(selinuxModeOfStack, targetImage.getTags(), targetImage.getUuid(), targetImage.getStackDetails().getVersion());
    }

    public void validateSeLinuxSupportedOnTargetImage(SeLinux selinuxModeOfStack, com.sequenceiq.cloudbreak.cloud.model.Image targetImage) {
        String stackVersion = Optional.ofNullable(targetImage.getPackageVersions().get(ImagePackageVersion.STACK.getKey())).orElseThrow(() ->
                new CloudbreakServiceException(String.format("Could not determine the stack version of image '%s'", targetImage.toString())));
        validateSeLinuxSupportedOnTargetImage(selinuxModeOfStack, targetImage.getTags(), targetImage.getImageId(), stackVersion);
    }

    private void validateSeLinuxSupportedOnTargetImage(SeLinux selinuxModeOfStack, Map<String, String> imageTags, String imageId, String stackVersion) {
        if (SeLinux.ENFORCING.equals(selinuxModeOfStack)) {
            Boolean selinuxSupportedOnTargetImage = Boolean.valueOf(imageTags.getOrDefault(SELINUX_SUPPORTED_TAG, Boolean.TRUE.toString()));
            LOGGER.debug("SELinux supported on target image '{}': '{}'", imageId, selinuxSupportedOnTargetImage);
            if (!selinuxSupportedOnTargetImage) {
                throw new CloudbreakServiceException("SELinux enforcing mode is not supported on target image. " +
                        "Please select another image!");
            }

            LOGGER.debug("Stack version of target image '{}': '{}'", imageId, stackVersion);
            if (!isVersionNewerOrEqualThanLimited(stackVersion, CLOUDERA_STACK_VERSION_7_2_18)) {
                throw new CloudbreakServiceException("SELinux enforcing mode is only supported on Cloudera Stack Version 7.2.18 or newer. " +
                        "Please select another image!");
            }
        }
    }

    private static SeLinux getSelinuxModeOfStack(StackDtoDelegate stack) {
        SeLinux selinuxModeOfStack = Optional.ofNullable(stack.getSecurityConfig())
                .map(SecurityConfig::getSeLinux)
                .orElse(SeLinux.PERMISSIVE);
        LOGGER.debug("SELinux mode of stack '{}': '{}'", stack.getId(), selinuxModeOfStack);
        return selinuxModeOfStack;
    }
}
