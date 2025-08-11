package com.sequenceiq.cloudbreak.service.upgrade.validation.service;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.sequenceiq.cloudbreak.cluster.api.ClusterApi;
import com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateService;
import com.sequenceiq.cloudbreak.common.exception.UpgradeValidationFailedException;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;

@Component
public class FlinkUpgradeValidator implements ServiceUpgradeValidator {

    private static final Logger LOGGER = LoggerFactory.getLogger(FlinkUpgradeValidator.class);

    private static final String FLINK_SERVICE_TYPE = "SQL_STREAM_BUILDER";

    private static final String ROLE_TYPE = "STREAMING_SQL_CONSOLE";

    @Inject
    private CmTemplateService cmTemplateService;

    @Inject
    private ClusterApiConnectors clusterApiConnectors;

    @Override
    public void validate(ServiceUpgradeValidationRequest validationRequest) {
        String targetRuntime = validationRequest.upgradeImageInfo().targetStatedImage().getImage().getVersion();
        StackDto stack = validationRequest.stack();
        if (!StringUtils.hasText(targetRuntime)) {
            LOGGER.debug("Skipping Flink service validation due to missing or invalid: target runtime version: [{}]", targetRuntime);
        } else if (!CMRepositoryVersionUtil.isVersionNewerOrEqualThanLimited(targetRuntime, CMRepositoryVersionUtil.CLOUDERA_STACK_VERSION_7_2_16)) {
            LOGGER.debug("Skipping Flink service validation, because target runtime ({}) is lower than 7.2.16", targetRuntime);
        } else if (!isFLinkServicePresent(stack)) {
            LOGGER.debug("Skipping Flink service validation, because Flink service not found in blueprint");
        } else {
            ClusterApi connector = clusterApiConnectors.getConnector(stack);
            LOGGER.debug("Validating Flink service role : {}", ROLE_TYPE);
            if (!connector.isRolePresent(stack.getCluster().getName(), ROLE_TYPE, FLINK_SERVICE_TYPE)) {
                LOGGER.debug("Flink upgrade validation was successful");
            } else {
                throw new UpgradeValidationFailedException(
                        String.format("Version change to CDH %s would lose access to role STREAMING_SQL_CONSOLE. " +
                                "To continue, remove this role and any dependent services.", targetRuntime));
            }
        }
    }

    private boolean isFLinkServicePresent(StackDto stack) {
        return cmTemplateService.isServiceTypePresent(FLINK_SERVICE_TYPE, getBlueprintText(stack));
    }

    private String getBlueprintText(StackDto stack) {
        return stack.getBlueprintJsonText();
    }
}
