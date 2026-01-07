package com.sequenceiq.cloudbreak.service.upgrade.validation.service;

import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.CLOUDERA_STACK_VERSION_7_3_1;
import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.CLOUDERA_STACK_VERSION_7_3_2;
import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.isVersionNewerOrEqualThanLimited;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.common.exception.UpgradeValidationFailedException;

@Component
public class JavaVersionUpgradeValidator implements ServiceUpgradeValidator {

    private static final Logger LOGGER = LoggerFactory.getLogger(JavaVersionUpgradeValidator.class);

    private static final Integer NOT_SUPPORTED_JAVA_VERSION = 11;

    private static final Integer POST_732_MIN_JAVA_VERSION = 17;

    @Inject
    private EntitlementService entitlementService;

    @Override
    public void validate(ServiceUpgradeValidationRequest request) {
        String targetRuntime = request.upgradeImageInfo().getTargetStatedImage().getImage().getVersion();
        Integer currentJavaVersion = request.stack().getStack().getJavaVersion();
        if (isVersionNewerOrEqualThan732(targetRuntime)) {
            if (!entitlementService.isAutoJavaUpgaradeEnabled(request.stack().getAccountId()) && currentJavaVersion < POST_732_MIN_JAVA_VERSION) {
                String message = String.format("You cannot upgrade to %s because your current cluster uses JDK %d, and upgrading to %s with " +
                                "JDK %d is not supported. Please upgrade to JDK 17 or higher before upgrading the cluster.",
                        targetRuntime, currentJavaVersion, targetRuntime, currentJavaVersion);
                LOGGER.error("Cluster upgrade validation failed for 7.3.2+ cluster. {}", message);
                throw new UpgradeValidationFailedException(message);
            }
        } else if (isVersionNewerOrEqualThan731(targetRuntime) && NOT_SUPPORTED_JAVA_VERSION.equals(currentJavaVersion)) {
            String message = String.format("You cannot upgrade to %s because your current cluster uses JDK %d, and upgrading to %s with "
                    + "JDK %d is not supported. Please downgrade to JDK 8 before upgrading the cluster.",
                    targetRuntime, NOT_SUPPORTED_JAVA_VERSION, targetRuntime, NOT_SUPPORTED_JAVA_VERSION);
            LOGGER.error("Cluster upgrade validation failed because of JDK 11. {}", message);
            throw new UpgradeValidationFailedException(message);
        }

    }

    private boolean isVersionNewerOrEqualThan731(String targetRuntime) {
        return isVersionNewerOrEqualThanLimited(targetRuntime, CLOUDERA_STACK_VERSION_7_3_1);
    }

    private boolean isVersionNewerOrEqualThan732(String targetRuntime) {
        return isVersionNewerOrEqualThanLimited(targetRuntime, CLOUDERA_STACK_VERSION_7_3_2);
    }
}