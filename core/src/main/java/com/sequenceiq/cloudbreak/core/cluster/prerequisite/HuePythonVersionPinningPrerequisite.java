package com.sequenceiq.cloudbreak.core.cluster.prerequisite;

import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.isVersionNewerOrEqualThanLimited;
import static java.lang.String.format;
import static java.util.stream.Collectors.toCollection;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.cloud.model.component.StackType;
import com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessorFactory;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.hue.HueRoles;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.core.CloudbreakImageCatalogException;
import com.sequenceiq.cloudbreak.core.CloudbreakImageNotFoundException;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;
import com.sequenceiq.cloudbreak.service.image.ClusterUpgradeTargetImageService;
import com.sequenceiq.cloudbreak.service.image.ImageCatalogService;
import com.sequenceiq.cloudbreak.service.parcel.ClouderaManagerProductTransformer;
import com.sequenceiq.cloudbreak.service.retry.RetryType;
import com.sequenceiq.cloudbreak.util.CdhVersionProvider;
import com.sequenceiq.cloudbreak.view.InstanceMetadataView;

/**
 * Temporary workaround for CB-34712: affected images carry a Python 3.12 that cannot import psycopg2, and Hue's
 * launcher picks the highest discoverable Python without checking whether it works, so Hue fails to start after an
 * upgrade to runtime 7.3.2.10000 or newer. Pins Hue to Python 3.11 via {@code HUE_PYTHON_VERSION=3.11} in
 * {@code /etc/default/cloudera-scm-agent} on the affected Hue hosts.
 * <p>
 * The pin takes effect on the full Cloudera Manager agent restart that follows in
 * {@code ClusterManagerUpgradeManagementService#upgradeClusterManager}; affected images always require a Cloudera
 * Manager upgrade, so that restart is reached.
 * <p>
 * The pin survives on hosts the upgrade does not replace: whoever fixes Python 3.12 must also drop the
 * {@code HUE_PYTHON_VERSION} entry and retire this prerequisite.
 */
@Component
public class HuePythonVersionPinningPrerequisite implements ClouderaManagerUpgradePrerequisite {

    /**
     * Mirrors Hue's own interpreter discovery: {@code _python_bin_path} scans SEARCH_DIRS instead of PATH and takes the
     * FIRST executable {@code pythonX.Y} it finds, without a health check. Only that interpreter can reach Hue, so the
     * loop evaluates exactly that one and prints {@code broken} when it cannot import psycopg2. Prints nothing when no
     * Python 3.12 is discoverable or the one Hue would pick is healthy.
     */
    static final String DETECT_BROKEN_PYTHON312_COMMAND =
            "for d in /usr/local/bin /bin /usr/bin /opt/rh/rh-python312/root/usr/bin; do "
                    + "[ -x \"$d/python3.12\" ] || continue; "
                    + "\"$d/python3.12\" -c 'import psycopg2' >/dev/null 2>&1 || echo broken; "
                    + "break; "
                    + "done";

    /**
     * Verifies the pin target before the pin is written. With {@code HUE_PYTHON_VERSION=3.11} set, Hue resolves the
     * interpreter with the same first-match, no-health-check scan, so this command evaluates exactly that one python3.11
     * and prints {@code missing} when there is none or it cannot import psycopg2. Accepting any healthy 3.11 would pass
     * a host whose first one is broken, the very outcome this check prevents.
     * <p>
     * Must not start with a {@code name=value} token: it travels as a positional salt-api {@code arg} of
     * {@code cmd.run}, and salt would turn that into a keyword argument, leaving {@code cmd.run} with no command. Hence
     * the shell variable is assigned inside the loop only.
     */
    static final String DETECT_MISSING_PYTHON311_COMMAND =
            "for d in /usr/local/bin /bin /usr/bin /opt/rh/rh-python311/root/usr/bin; do "
                    + "[ -x \"$d/python3.11\" ] || continue; "
                    + "\"$d/python3.11\" -c 'import psycopg2' >/dev/null 2>&1 && ok=1; "
                    + "break; "
                    + "done; [ -n \"$ok\" ] || echo missing";

    /**
     * Writes {@code HUE_PYTHON_VERSION=3.11} into {@code /etc/default/cloudera-scm-agent}, the systemd
     * {@code EnvironmentFile} of the agent and supervisord that Hue's launcher reads. Drops any existing entry first to
     * stay idempotent, then prints {@code failed} if the line is missing afterwards, because {@code cmd.run} does not
     * fail on a non-zero exit code. The leading {@code \n} guards against a file without a trailing newline.
     */
    static final String PIN_HUE_PYTHON311_COMMAND =
            "sed -i '/^HUE_PYTHON_VERSION=/d' /etc/default/cloudera-scm-agent 2>/dev/null; "
                    + "printf '\\nHUE_PYTHON_VERSION=3.11\\n' >> /etc/default/cloudera-scm-agent; "
                    + "grep -qx 'HUE_PYTHON_VERSION=3.11' /etc/default/cloudera-scm-agent || echo failed";

    private static final String BROKEN = "broken";

    private static final String MISSING = "missing";

    private static final String FAILED = "failed";

    private static final int MAX_REPORTED_OUTPUT_LENGTH = 200;

    private static final String BROKEN_PYTHON312_DETECTION = "The broken Python 3.12 detection";

    private static final String PYTHON311_AVAILABILITY_CHECK = "The Python 3.11 availability check";

    private static final String HUE_PYTHON311_PINNING = "The Hue Python 3.11 pinning";

    private static final Logger LOGGER = LoggerFactory.getLogger(HuePythonVersionPinningPrerequisite.class);

    @Inject
    private GatewayConfigService gatewayConfigService;

    @Inject
    private HostOrchestrator hostOrchestrator;

    @Inject
    private CmTemplateProcessorFactory cmTemplateProcessorFactory;

    @Inject
    private ClusterUpgradeTargetImageService clusterUpgradeTargetImageService;

    @Inject
    private ImageCatalogService imageCatalogService;

    @Inject
    private ClouderaManagerProductTransformer clouderaManagerProductTransformer;

    @Override
    public void execute(StackDto stackDto) {
        if (!isTargetRuntimeAffectedOrUnknown(stackDto)) {
            LOGGER.debug("Skipping Hue Python pinning because the target runtime is older than {}.",
                    CMRepositoryVersionUtil.CLOUDERA_STACK_VERSION_7_3_2_10000.getVersion());
            return;
        }
        Set<String> hueHostGroups = getHueHostGroups(stackDto);
        if (hueHostGroups.isEmpty()) {
            LOGGER.debug("Skipping Hue Python pinning because there is no host group containing the {} component.", HueRoles.HUE_SERVER);
            return;
        }
        Set<String> hueFqdns = reachableFqdnsIn(stackDto, hueHostGroups);
        if (hueFqdns.isEmpty()) {
            throw logAndCreateException(format("There is no reachable host in the Hue host group(s): %s. All instances must be running and healthy for "
                    + "this upgrade.", String.join(", ", hueHostGroups)));
        }
        Set<String> brokenHosts = findHostsWithBrokenPython312(stackDto, hueFqdns);
        if (brokenHosts.isEmpty()) {
            LOGGER.debug("No Hue host would select a Python 3.12 that cannot import psycopg2, Hue does not have to be pinned to Python 3.11.");
            return;
        }
        LOGGER.info("A Python 3.12 that cannot import psycopg2 was detected on host(s): {}. Pinning Hue to Python 3.11.", brokenHosts);
        validatePinTargetAvailable(stackDto, brokenHosts);
        Set<String> pinFailedHosts = pinHueToPython311(stackDto, brokenHosts);
        if (!pinFailedHosts.isEmpty()) {
            throw logAndCreateException(format("Pinning Hue to Python 3.11 failed on host(s): %s. The Python 3.12 that the Hue service would select "
                    + "cannot import psycopg2. Please contact Cloudera support.", String.join(", ", pinFailedHosts)));
        }
        LOGGER.info("Pinned Hue to Python 3.11 on host(s): {}. The upgrade can proceed.", brokenHosts);
    }

    private boolean isTargetRuntimeAffectedOrUnknown(StackDto stackDto) {
        Optional<String> targetFullCdhVersion = findTargetCatalogImage(stackDto)
                .map(image -> clouderaManagerProductTransformer.transform(image, true, false))
                .flatMap(products -> products.stream().filter(product -> StackType.CDH.name().equals(product.getName())).findFirst())
                .map(ClouderaManagerProduct::getVersion)
                .map(CdhVersionProvider::getCdhFullVersionFromVersionString);
        return targetFullCdhVersion.isEmpty()
                || StringUtils.isNotBlank(targetFullCdhVersion.get())
                && isVersionNewerOrEqualThanLimited(targetFullCdhVersion.get(), CMRepositoryVersionUtil.CLOUDERA_STACK_VERSION_7_3_2_10000);
    }

    private Optional<Image> findTargetCatalogImage(StackDto stackDto) {
        Optional<com.sequenceiq.cloudbreak.cloud.model.Image> targetImage = clusterUpgradeTargetImageService.findTargetImage(stackDto.getId());
        if (targetImage.isEmpty()) {
            LOGGER.warn("The target image is not available for stack {}, the target runtime version cannot be determined.", stackDto.getId());
            return Optional.empty();
        }
        try {
            return Optional.of(imageCatalogService.getImage(stackDto.getWorkspaceId(), targetImage.get().getImageCatalogUrl(),
                    targetImage.get().getImageCatalogName(), targetImage.get().getImageId()).getImage());
        } catch (CloudbreakImageNotFoundException | CloudbreakImageCatalogException e) {
            LOGGER.warn("The target image {} is not found in catalog {}, the target runtime version cannot be determined.",
                    targetImage.get().getImageId(), targetImage.get().getImageCatalogUrl(), e);
            return Optional.empty();
        }
    }

    private Set<String> getHueHostGroups(StackDto stackDto) {
        return cmTemplateProcessorFactory.get(stackDto.getBlueprintJsonText()).getHostGroupsWithComponent(HueRoles.HUE_SERVER);
    }

    private Set<String> reachableFqdnsIn(StackDto stackDto, Set<String> hostGroups) {
        return stackDto.getInstanceGroupDtos().stream()
                .filter(instanceGroup -> hostGroups.contains(instanceGroup.getInstanceGroup().getGroupName()))
                .flatMap(instanceGroup -> instanceGroup.getReachableInstanceMetaData().stream())
                .map(InstanceMetadataView::getDiscoveryFQDN)
                .filter(StringUtils::isNotBlank)
                .collect(toCollection(LinkedHashSet::new));
    }

    private Set<String> findHostsWithBrokenPython312(StackDto stackDto, Set<String> hueFqdns) {
        return hostsReporting(runCommandOnHosts(stackDto, hueFqdns, DETECT_BROKEN_PYTHON312_COMMAND, BROKEN_PYTHON312_DETECTION), BROKEN,
                BROKEN_PYTHON312_DETECTION);
    }

    private void validatePinTargetAvailable(StackDto stackDto, Set<String> brokenHosts) {
        Set<String> hostsWithoutPython311 = hostsReporting(
                runCommandOnHosts(stackDto, brokenHosts, DETECT_MISSING_PYTHON311_COMMAND, PYTHON311_AVAILABILITY_CHECK), MISSING,
                PYTHON311_AVAILABILITY_CHECK);
        if (!hostsWithoutPython311.isEmpty()) {
            throw logAndCreateException(format("Hue cannot be pinned to Python 3.11 because there is no Python 3.11 with a working psycopg2 module on "
                    + "host(s): %s. Please contact Cloudera support.", String.join(", ", hostsWithoutPython311)));
        }
    }

    private Set<String> pinHueToPython311(StackDto stackDto, Set<String> brokenHosts) {
        return hostsReporting(runCommandOnHosts(stackDto, brokenHosts, PIN_HUE_PYTHON311_COMMAND, HUE_PYTHON311_PINNING), FAILED, HUE_PYTHON311_PINNING);
    }

    private Map<String, String> runCommandOnHosts(StackDto stackDto, Set<String> targetFqdns, String command, String operation) {
        try {
            List<GatewayConfig> gatewayConfigs = gatewayConfigService.getAllGatewayConfigs(stackDto);
            Map<String, String> output = hostOrchestrator.runCommandOnHosts(gatewayConfigs, targetFqdns, command, RetryType.WITH_2_SEC_DELAY_MAX_15_TIMES);
            validateAllHostsResponded(targetFqdns, output, operation);
            return output;
        } catch (CloudbreakOrchestratorFailedException e) {
            throw logAndCreateException(format("%s could not be executed on all affected hosts. All instances must be running and healthy for this upgrade. "
                    + "Cause: %s", operation, e.getMessage()), e);
        }
    }

    /**
     * {@code cmd.run} returns only the minions that responded, and a quiet one is absent instead of causing an error,
     * so it would be read as a healthy host while detecting and as a pinned host while pinning.
     */
    private void validateAllHostsResponded(Set<String> expectedFqdns, Map<String, String> output, String operation) {
        Set<String> silentHosts = new LinkedHashSet<>(expectedFqdns);
        silentHosts.removeAll(output.keySet());
        if (!silentHosts.isEmpty()) {
            throw logAndCreateException(format("%s returned no result from host(s): %s. All instances must be running and healthy for this upgrade.",
                    operation, String.join(", ", silentHosts)));
        }
    }

    /**
     * Collects the hosts that printed the expected token. Blank means the host is fine, the token means it is not, and
     * anything else is reported as a failure with the output: treating every non-blank output as the token would turn
     * an error of the command itself into a verdict about the host.
     */
    private Set<String> hostsReporting(Map<String, String> output, String expectedToken, String operation) {
        Set<String> hostsWithUnexpectedOutput = new LinkedHashSet<>();
        Set<String> reportingHosts = new LinkedHashSet<>();
        for (Map.Entry<String, String> entry : output.entrySet()) {
            String hostOutput = StringUtils.trimToEmpty(entry.getValue());
            if (expectedToken.equals(hostOutput)) {
                reportingHosts.add(entry.getKey());
            } else if (StringUtils.isNotBlank(hostOutput)) {
                hostsWithUnexpectedOutput.add(entry.getKey() + ": " + StringUtils.abbreviate(hostOutput, MAX_REPORTED_OUTPUT_LENGTH));
            }
        }
        if (!hostsWithUnexpectedOutput.isEmpty()) {
            throw logAndCreateException(format("%s returned an unexpected result, expected either an empty output or '%s'. Output by host: %s",
                    operation, expectedToken, String.join(", ", hostsWithUnexpectedOutput)));
        }
        return reportingHosts;
    }

    private CloudbreakServiceException logAndCreateException(String message) {
        LOGGER.warn(message);
        return new CloudbreakServiceException(message);
    }

    private CloudbreakServiceException logAndCreateException(String message, Exception e) {
        LOGGER.warn(message, e);
        return new CloudbreakServiceException(message, e);
    }
}
