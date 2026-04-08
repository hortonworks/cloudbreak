package com.sequenceiq.cloudbreak.service.upgrade.validation.service;

import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.CLOUDERA_STACK_VERSION_7_3_1;
import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.CLOUDERA_STACK_VERSION_7_3_2;
import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.isVersionNewerOrEqualThanLimited;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.exception.UpgradeValidationFailedException;

@Component
public class JavaVersionUpgradeValidator implements ServiceUpgradeValidator {

    public static final Integer JAVA_8 = 8;

    public static final Integer JAVA_11 = 11;

    public static final Integer JAVA_17 = 17;

    private static final Logger LOGGER = LoggerFactory.getLogger(JavaVersionUpgradeValidator.class);

    @Override
    public void validate(ServiceUpgradeValidationRequest request) {
        String targetRuntime = request.upgradeImageInfo().getTargetStatedImage().getImage().getVersion();
        Integer currentJavaVersion = request.stack().getStack().getJavaVersion();
        if (isVersionNewerOrEqualThan732(targetRuntime)) {
            if (currentJavaVersion < JAVA_17) {
                String message = String.format("You cannot upgrade to %s because your current cluster uses JDK %d, and upgrading to %s with " +
                                "JDK %d is not supported. Please upgrade to JDK 17 or higher before upgrading the cluster.",
                        targetRuntime, currentJavaVersion, targetRuntime, currentJavaVersion);
                LOGGER.error("Cluster upgrade validation failed for 7.3.2+ cluster. {}", message);
                throw new UpgradeValidationFailedException(message);
            }
        } else if (isVersionNewerOrEqualThan731(targetRuntime)) {
            if (JAVA_11.equals(currentJavaVersion)) {
                String message = String.format("You cannot upgrade to %s because your current cluster uses JDK %d, and upgrading to %s with "
                                + "JDK %d is not supported. Please downgrade to JDK 8 before upgrading the cluster.",
                        targetRuntime, JAVA_11, targetRuntime, JAVA_11);
                LOGGER.error("Cluster upgrade validation failed because of JDK 11. {}", message);
                throw new UpgradeValidationFailedException(message);
            }
        }
    }

    private boolean isVersionNewerOrEqualThan731(String targetRuntime) {
        return isVersionNewerOrEqualThanLimited(targetRuntime, CLOUDERA_STACK_VERSION_7_3_1);
    }

    private boolean isVersionNewerOrEqualThan732(String targetRuntime) {
        return isVersionNewerOrEqualThanLimited(targetRuntime, CLOUDERA_STACK_VERSION_7_3_2);
    }
}
