package com.sequenceiq.cloudbreak.service.upgrade.validation.service;

import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.CLOUDERA_STACK_VERSION_7_3_2;
import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.isVersionNewerOrEqualThanLimited;

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
import com.sequenceiq.cloudbreak.util.DocumentationLinkProvider;

@Component
public class NifiUpgradeValidator implements ServiceUpgradeValidator {

    private static final Logger LOGGER = LoggerFactory.getLogger(NifiUpgradeValidator.class);

    private static final String NIFI_SERVICE_TYPE = "NIFI";

    private static final String NIFI_WORKING_DIRECTORY = "nifi.working.directory";

    private static final String ROLE_TYPE = "NIFI_NODE";

    private static final String NIFI_LIGHT_DUTY_BLUEPRINT_NAME = "Flow Management Light Duty with Apache NiFi, Apache NiFi Registry, Schema Registry";

    private static final String NIFI_HEAVY_DUTY_BLUEPRINT_VERSION = "Flow Management Heavy Duty with Apache NiFi, Apache NiFi Registry, Schema Registry";

    private static final String NIFI_2 = "NiFi 2";

    @Inject
    private CmTemplateService cmTemplateService;

    @Inject
    private ClusterApiConnectors clusterApiConnectors;

    @Override
    public void validate(ServiceUpgradeValidationRequest validationRequest) {
        if ((validationRequest.lockComponents() || validationRequest.replaceVms()) && isNifiServicePresent(validationRequest.stack())) {
            validateNifiBlueprintVersion(validationRequest);
            validateNifiWorkingDirectory(validationRequest.stack());
        } else {
            LOGGER.debug("Skipping Nifi service validation because it's not OS upgrade or Nifi not present in the cluster.");
        }
    }

    private void validateNifiBlueprintVersion(ServiceUpgradeValidationRequest validationRequest) {
        String targetRuntime = validationRequest.upgradeImageInfo().targetStatedImage().getImage().getVersion();
        if (isVersionNewerOrEqualThanLimited(targetRuntime, CLOUDERA_STACK_VERSION_7_3_2)) {
            String blueprintName = validationRequest.stack().getBlueprint().getName();
            if (blueprintName.contains(NIFI_LIGHT_DUTY_BLUEPRINT_NAME)
                    || blueprintName.contains(NIFI_HEAVY_DUTY_BLUEPRINT_VERSION)) {
                if (!blueprintName.contains(NIFI_2)) {
                    throw new UpgradeValidationFailedException(String.format("Action Required: Upgrade to NiFi 2.x" + System.lineSeparator()
                            + "The selected CDP Runtime version (%s) does not support NiFi 1.x. A direct, in-place upgrade is not possible. "
                            + "To proceed, you must manually migrate your workflows to a new NiFi 2.x Data Hub cluster "
                            + "before upgrading this environment. Refer to Cloudera Documentation at: %s", targetRuntime,
                            DocumentationLinkProvider.nifiMigrationLink()));
                }
            }
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
