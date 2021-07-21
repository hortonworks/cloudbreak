package com.sequenceiq.cloudbreak.service.upgrade.validation.service;

import java.util.Optional;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cluster.api.ClusterApi;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateService;
import com.sequenceiq.cloudbreak.common.exception.UpgradeValidationFailedException;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;
import com.sequenceiq.cloudbreak.template.VolumeUtils;

@Component
public class NifiUpgradeValidator implements ServiceUpgradeValidator {

    private static final Logger LOGGER = LoggerFactory.getLogger(NifiUpgradeValidator.class);

    private static final String NIFI_SERVICE_TYPE = "NIFI";

    private static final String NIFI_WORKING_DIRECTORY = "nifi.working.directory";

    private static final String ROLE_TYPE = "NIFI_NODE";

    @Inject
    private CmTemplateService cmTemplateService;

    @Inject
    private ClusterApiConnectors clusterApiConnectors;

    @Override
    public void validate(ServiceUpgradeValidationRequest validationRequest) {
        if (validationRequest.isLockComponents() && isNifiServicePresent(validationRequest.getStack())) {
            validateNifiWorkingDirectory(validationRequest.getStack());
        } else {
            LOGGER.debug("Skipping Nifi service validation because  it's not OS upgrade or Nifi not present in the cluster.");
        }
    }

    private boolean isNifiServicePresent(Stack stack) {
        return cmTemplateService.isServiceTypePresent(NIFI_SERVICE_TYPE, getBlueprintText(stack));
    }

    private String getBlueprintText(Stack stack) {
        return stack.getCluster().getBlueprint().getBlueprintText();
    }

    private void validateNifiWorkingDirectory(Stack stack) {
        ClusterApi connector = clusterApiConnectors.getConnector(stack);
        Optional<String> nifiWorkingDirectory = connector.getRoleConfigValueByServiceType(stack.getCluster().getName(), ROLE_TYPE, NIFI_SERVICE_TYPE,
                NIFI_WORKING_DIRECTORY);
        LOGGER.debug("Validating Nifi working directory: {}", nifiWorkingDirectory);
        if (nifiWorkingDirectory.isPresent() && nifiWorkingDirectory.get().startsWith(VolumeUtils.VOLUME_PREFIX)) {
            LOGGER.debug("Nifi working directory validation was successful");
        } else {
            throw new UpgradeValidationFailedException(
                    String.format("Nifi working directory validation failed. The current directory %s is not eligible for upgrade.",
                            nifiWorkingDirectory.get()));
        }
    }
}
