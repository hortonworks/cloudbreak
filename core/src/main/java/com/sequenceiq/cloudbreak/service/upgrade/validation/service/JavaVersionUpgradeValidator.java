package com.sequenceiq.cloudbreak.service.upgrade.validation.service;

import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.CLOUDERA_STACK_VERSION_7_3_1;
import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.isVersionNewerOrEqualThanLimited;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.exception.UpgradeValidationFailedException;

@Component
public class JavaVersionUpgradeValidator implements ServiceUpgradeValidator {

    private static final Logger LOGGER = LoggerFactory.getLogger(JavaVersionUpgradeValidator.class);

    private static final Integer NOT_SUPPORTED_JAVA_VERSION = 11;

    @Override
    public void validate(ServiceUpgradeValidationRequest request) {
        String targetRuntime = request.upgradeImageInfo().getTargetStatedImage().getImage().getVersion();
        Integer currentJavaVersion = request.stack().getStack().getJavaVersion();
        if (isVersionNewerOrEqualThan731(targetRuntime) && NOT_SUPPORTED_JAVA_VERSION.equals(currentJavaVersion)) {
            String message = String.format("You cannot upgrade to %s because your current cluster uses JDK %d, and upgrading to %s with "
                    + "JDK %d is not supported. Please downgrade to JDK 8 before upgrading the cluster.",
                    targetRuntime, NOT_SUPPORTED_JAVA_VERSION, targetRuntime, NOT_SUPPORTED_JAVA_VERSION);
            LOGGER.error("Cluster upgrade validation failed. {}", message);
            throw new UpgradeValidationFailedException(message);
        }
    }

    private boolean isVersionNewerOrEqualThan731(String targetRuntime) {
        return isVersionNewerOrEqualThanLimited(targetRuntime, CLOUDERA_STACK_VERSION_7_3_1);
    }
}
