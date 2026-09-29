package com.sequenceiq.cloudbreak.service.upgrade.validation.service;

import static com.sequenceiq.cloudbreak.cloud.model.catalog.ImagePackageVersion.PSQL11;
import static com.sequenceiq.cloudbreak.cloud.model.catalog.ImagePackageVersion.PYTHON312;
import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.isVersionNewerOrEqualThanLimited;
import static java.lang.String.format;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil;
import com.sequenceiq.cloudbreak.common.exception.UpgradeValidationFailedException;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;
import com.sequenceiq.cloudbreak.service.retry.RetryType;
import com.sequenceiq.cloudbreak.service.upgrade.ClusterUpgradeProperties;
import com.sequenceiq.cloudbreak.util.CdhVersionProvider;

@Component
public class HuePsycopg2UpgradeValidator implements ServiceUpgradeValidator {

    /**
     * When Python 3.12 is installed, prints {@code broken} if its {@code pip3.12} wrapper is missing or {@code psycopg2}
     * cannot be imported through it. Prints nothing when Python 3.12 is absent (cannot be used by Hue) or healthy.
     */
    static final String DETECT_BROKEN_PYTHON312_COMMAND =
            "command -v python3.12 >/dev/null 2>&1 "
                    + "&& { command -v pip3.12 >/dev/null 2>&1 && python3.12 -c \"import psycopg2\" >/dev/null 2>&1 || echo broken; }";

    /**
     * Removes the Python 3.12 package, then prints {@code failed} if {@code python3.12} is still resolvable afterwards
     * (the removal did not take effect). Prints nothing when the package is gone. {@code cmd.run} does not fail on a
     * non-zero exit code, so the command must report the failure itself.
     */
    static final String REMOVE_BROKEN_PYTHON312_COMMAND =
            "yum -y remove python3.12 >/dev/null 2>&1; command -v python3.12 >/dev/null 2>&1 && echo failed";

    private static final Logger LOGGER = LoggerFactory.getLogger(HuePsycopg2UpgradeValidator.class);

    @Inject
    private GatewayConfigService gatewayConfigService;

    @Inject
    private HostOrchestrator hostOrchestrator;

    @Override
    public void validate(ServiceUpgradeValidationRequest request) {
        if (!isTargetRuntimeAffected(request)) {
            LOGGER.debug("Skipping Hue psycopg2 validation because the target runtime is older than {}.",
                    CMRepositoryVersionUtil.CLOUDERA_STACK_VERSION_7_3_2_10000.getVersion());
            return;
        }
        if (!isCurrentImageAtRisk(request)) {
            LOGGER.debug("Skipping Hue psycopg2 validation because the current image is proven safe "
                    + "(Python 3.12 burnt into the image, or PostgreSQL 11 absent from a populated manifest).");
            return;
        }
        StackDto stack = request.stack();
        Set<String> brokenHosts = findHostsWithBrokenPython312(stack);
        if (brokenHosts.isEmpty()) {
            LOGGER.debug("Hue psycopg2 validation passed, no host has a broken Python 3.12 pip/psycopg2 environment.");
            return;
        }
        LOGGER.info("Broken Python 3.12 (missing pip3.12/psycopg2) detected on host(s): {}. Attempting to remove the python3.12 package.", brokenHosts);
        Set<String> removalFailedHosts = removeBrokenPython312(stack, brokenHosts);
        if (!removalFailedHosts.isEmpty()) {
            String msg = format("You are not eligible to upgrade to the selected runtime because Python 3.12 is present on the cluster nodes without a working "
                    + "pip3.12/psycopg2 module (required by the Hue service), and the automatic removal of the Python 3.12 package failed. Affected "
                    + "host(s): %s. Please contact Cloudera support.", String.join(", ", removalFailedHosts));
            LOGGER.warn(msg);
            throw new UpgradeValidationFailedException(msg);
        }
        LOGGER.info("Removed the broken Python 3.12 package from host(s): {}. The upgrade can proceed.", brokenHosts);
    }

    private boolean isTargetRuntimeAffected(ServiceUpgradeValidationRequest request) {
        ClusterUpgradeProperties clusterUpgradeProperties = request.clusterUpgradeProperties();
        Optional<String> targetFullCdhVersion = Optional.ofNullable(clusterUpgradeProperties)
                .map(ClusterUpgradeProperties::getCdhParcel)
                .map(ClouderaManagerProduct::getVersion)
                .map(CdhVersionProvider::getCdhFullVersionFromVersionString);
        return targetFullCdhVersion.isEmpty() ||
                StringUtils.isNotBlank(targetFullCdhVersion.get())
                        && isVersionNewerOrEqualThanLimited(targetFullCdhVersion.get(), CMRepositoryVersionUtil.CLOUDERA_STACK_VERSION_7_3_2_10000);
    }

    private boolean isCurrentImageAtRisk(ServiceUpgradeValidationRequest request) {
        Image currentImage = request.clusterUpgradeProperties().toCurrentCloudImage();
        Map<String, String> packageVersions = currentImage == null ? null : currentImage.getPackageVersions();
        boolean result = true;
        if (MapUtils.isNotEmpty(packageVersions)) {
            result = packageVersions.containsKey(PSQL11.getKey()) && !packageVersions.containsKey(PYTHON312.getKey());
        }
        return result;
    }

    private Set<String> findHostsWithBrokenPython312(StackDto stack) {
        try {
            GatewayConfig primaryGatewayConfig = gatewayConfigService.getPrimaryGatewayConfig(stack);
            Map<String, String> output = hostOrchestrator.runCommandOnAllHosts(primaryGatewayConfig, DETECT_BROKEN_PYTHON312_COMMAND,
                    RetryType.WITH_1_SEC_DELAY_MAX_3_TIMES);
            return output.entrySet().stream()
                    .filter(entry -> StringUtils.isNotBlank(StringUtils.trimToEmpty(entry.getValue())))
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
        } catch (CloudbreakOrchestratorFailedException e) {
            String msg = format("You are not eligible to upgrade to the selected runtime because the Hue psycopg2 requirement could not be verified "
                    + "on all hosts. All instances must be running and healthy for this upgrade. Cause: %s", e.getMessage());
            LOGGER.warn(msg, e);
            throw new UpgradeValidationFailedException(msg, e);
        }
    }

    private Set<String> removeBrokenPython312(StackDto stack, Set<String> brokenHosts) {
        try {
            List<GatewayConfig> gatewayConfigs = gatewayConfigService.getAllGatewayConfigs(stack);
            Map<String, String> output = hostOrchestrator.runCommandOnHosts(gatewayConfigs, brokenHosts, REMOVE_BROKEN_PYTHON312_COMMAND,
                    RetryType.WITH_1_SEC_DELAY_MAX_3_TIMES);
            return output.entrySet().stream()
                    .filter(entry -> StringUtils.isNotBlank(StringUtils.trimToEmpty(entry.getValue())))
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
        } catch (CloudbreakOrchestratorFailedException e) {
            String msg = format("You are not eligible to upgrade to the selected runtime because the broken Python 3.12 package could not be removed on all "
                    + "affected hosts. All instances must be running and healthy for this upgrade. Cause: %s", e.getMessage());
            LOGGER.warn(msg, e);
            throw new UpgradeValidationFailedException(msg, e);
        }
    }
}
