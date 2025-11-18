package com.sequenceiq.cloudbreak.service.upgrade.validation.service;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateService;
import com.sequenceiq.cloudbreak.common.exception.UpgradeValidationFailedException;
import com.sequenceiq.cloudbreak.dto.StackDto;

@Component
public class Spark2UpgradeValidator implements ServiceUpgradeValidator {

    private static final Logger LOGGER = LoggerFactory.getLogger(Spark2UpgradeValidator.class);

    private static final String SPARK_ON_YARN_SERVICE_TYPE = "SPARK_ON_YARN";

    @Inject
    private CmTemplateService cmTemplateService;

    @Override
    public void validate(ServiceUpgradeValidationRequest validationRequest) {
        if (spark2PresentedInTheOriginalBlueprint(validationRequest)) {
            if (upgradeTo730WhereSpark2Deprecated(validationRequest)) {
                throw new UpgradeValidationFailedException(
                        "Your current cluster configuration includes Spark2, " +
                                        "which will be deprecated in the upcoming 7.3.x release. " +
                                        "As a result, your cluster will only support the 7.2.x line " +
                                        "and you will not be able to upgrade to the 7.3.x line. " +
                                        "To ensure a smooth transition and continued support, " +
                                        "please start planning to migrate to Spark3 by recreating " +
                                        "your DH cluster (This will involve setting up a new cluster with Spark3 " +
                                        "alongside your existing cluster) or remove Spark 2 and install Spark 3 on Cloudera Manager UI. " +
                                        "This will involve setting up a new cluster with Spark3 " +
                                        "(This will be automatically synced into CDP Control Plane).");
            }
        } else {
            LOGGER.debug("Skipping Spark2 service validation because it's OS upgrade.");
        }
    }

    private boolean upgradeTo730WhereSpark2Deprecated(ServiceUpgradeValidationRequest validationRequest) {
        String targetRuntime = getTargetVersion(validationRequest);
        return StringUtils.hasText(targetRuntime) && targetRuntimeHigherOrEqualThan730(targetRuntime);
    }

    private String getTargetVersion(ServiceUpgradeValidationRequest validationRequest) {
        return validationRequest.upgradeImageInfo().targetStatedImage().getImage().getVersion();
    }

    private boolean spark2PresentedInTheOriginalBlueprint(ServiceUpgradeValidationRequest validationRequest) {
        return !validationRequest.lockComponents() && isSparkOnYarnServicePresent(validationRequest.stack());
    }

    private boolean isSparkOnYarnServicePresent(StackDto stack) {
        return cmTemplateService.isServiceTypePresent(SPARK_ON_YARN_SERVICE_TYPE, getBlueprintText(stack));
    }

    private String getBlueprintText(StackDto stack) {
        return stack.getCluster().getExtendedBlueprintText();
    }

    private boolean targetRuntimeHigherOrEqualThan730(String targetRuntime) {
        return CMRepositoryVersionUtil
                .isVersionNewerOrEqualThanLimited(targetRuntime, CMRepositoryVersionUtil.CLOUDERA_STACK_VERSION_7_3_0);
    }
}
