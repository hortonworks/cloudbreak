package com.sequenceiq.cloudbreak.service.upgrade.validation.service;

import java.util.Optional;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cluster.api.ClusterApi;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateService;
import com.sequenceiq.cloudbreak.common.exception.UpgradeValidationFailedException;
import com.sequenceiq.cloudbreak.dto.StackDto;
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
        if ((validationRequest.lockComponents() || validationRequest.replaceVms()) && isNifiServicePresent(validationRequest.stack())) {
            validateNifiWorkingDirectory(validationRequest.stack());
        } else {
            LOGGER.debug("Skipping Nifi service validation because it's not OS upgrade or Nifi not present in the cluster.");
        }
    }

    private boolean isNifiServicePresent(StackDto stack) {
        return cmTemplateService.isServiceTypePresent(NIFI_SERVICE_TYPE, stack.getBlueprintJsonText());
    }

    private void validateNifiWorkingDirectory(StackDto stack) {
        ClusterApi connector = clusterApiConnectors.getConnector(stack);
        Optional<String> nifiWorkingDirectory = connector.getRoleConfigValueByServiceType(stack.getCluster().getName(), ROLE_TYPE, NIFI_SERVICE_TYPE,
                NIFI_WORKING_DIRECTORY);
        LOGGER.debug("Validating Nifi working directory: {}", nifiWorkingDirectory);
        if (nifiWorkingDirectory.isPresent() && nifiWorkingDirectory.get().startsWith(VolumeUtils.VOLUME_PREFIX)) {
            LOGGER.debug("Nifi working directory validation was successful");
        } else {
            throw new UpgradeValidationFailedException(
                    String.format("Nifi working directory validation failed. The current directory %s is not eligible for upgrade because it is located "
                                    + "on the root disk. The Nifi working directory should be under the %s path. During upgrade or repair "
                                    + "the Nifi directory would get deleted as the root disk is not kept during these operations.",
                            nifiWorkingDirectory.get(), VolumeUtils.VOLUME_PREFIX));
        }
    }
}
