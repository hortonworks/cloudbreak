package com.sequenceiq.cloudbreak.telemetry.common;

import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.PropertyPlaceholderHelper;

import com.sequenceiq.cloudbreak.altus.AltusDatabusConnectionConfiguration;
import com.sequenceiq.cloudbreak.telemetry.TelemetryClusterDetails;
import com.sequenceiq.cloudbreak.telemetry.TelemetryRepoConfiguration;
import com.sequenceiq.cloudbreak.telemetry.TelemetryUpgradeConfiguration;
import com.sequenceiq.common.api.telemetry.model.Telemetry;
import com.sequenceiq.common.api.telemetry.model.VmLog;

@Service
public class TelemetryCommonConfigService {

    static final String SERVICE_LOG_FOLDER_PREFIX = "serviceLogFolderPrefix";

    static final String AGENT_LOG_FOLDER_PREFIX = "agentLogFolderPrefix";

    static final String SERVER_LOG_FOLDER_PREFIX = "serverLogFolderPrefix";

    private static final String LOG_FOLDER_DEFAULT = "/var/log";

    private final AnonymizationRuleResolver anonymizationRuleResolver;

    private final TelemetryUpgradeConfiguration telemetryUpgradeConfiguration;

    private final TelemetryRepoConfiguration telemetryRepoConfiguration;

    private final AltusDatabusConnectionConfiguration altusDatabusConnectionConfiguration;

    public TelemetryCommonConfigService(AnonymizationRuleResolver anonymizationRuleResolver, TelemetryUpgradeConfiguration telemetryUpgradeConfiguration,
            TelemetryRepoConfiguration telemetryRepoConfiguration, AltusDatabusConnectionConfiguration altusDatabusConnectionConfiguration) {
        this.anonymizationRuleResolver = anonymizationRuleResolver;
        this.telemetryUpgradeConfiguration = telemetryUpgradeConfiguration;
        this.altusDatabusConnectionConfiguration = altusDatabusConnectionConfiguration;
        this.telemetryRepoConfiguration = telemetryRepoConfiguration;
    }

    public TelemetryCommonConfigView createTelemetryCommonConfigs(Telemetry telemetry, List<VmLog> logs,
            TelemetryClusterDetails clusterDetails) {
        resolveLogPathReferences(telemetry, logs);
        TelemetryCommonConfigView.Builder builder = new TelemetryCommonConfigView.Builder();
        if (telemetryUpgradeConfiguration.isEnabled()) {
            if (telemetryUpgradeConfiguration.getCdpTelemetry() != null) {
                builder.withDesiredCdpTelemetryVersion(telemetryUpgradeConfiguration.getCdpTelemetry().getDesiredVersion());
            }
            if (telemetryUpgradeConfiguration.getCdpLoggingAgent() != null) {
                builder.withDesiredCdpLoggingAgentVersion(telemetryUpgradeConfiguration.getCdpLoggingAgent().getDesiredVersion());
            }
        }
        return builder
                .withClusterDetails(clusterDetails)
                .withRules(anonymizationRuleResolver.decodeRules(telemetry.getRules()))
                .withVmLogs(logs)
                .withDatabusConnectMaxTimeSeconds(altusDatabusConnectionConfiguration.getMaxTimeSeconds())
                .withDatabusConnectRetryTimes(altusDatabusConnectionConfiguration.getRetryTimes())
                .withDatabusConnectRetryDelay(altusDatabusConnectionConfiguration.getRetryDelaySeconds())
                .withDatabusConnectRetryMaxTime(altusDatabusConnectionConfiguration.getRetryMaxTimeSeconds())
                .withRepoName(telemetryRepoConfiguration.getName())
                .withRepoBaseUrl(telemetryRepoConfiguration.getBaseUrl())
                .withRepoGpgKey(telemetryRepoConfiguration.getGpgKey())
                .withRepoGpgCheck(telemetryRepoConfiguration.getGpgCheck())
                .build();
    }

    private void resolveLogPathReferences(Telemetry telemetry, List<VmLog> logs) {
        Map<String, Object> fluentAttributes = telemetry.getFluentAttributes();
        if (CollectionUtils.isNotEmpty(logs)) {
            Properties props = new Properties();
            props.setProperty(SERVER_LOG_FOLDER_PREFIX, fluentAttributes.getOrDefault(SERVER_LOG_FOLDER_PREFIX, LOG_FOLDER_DEFAULT).toString());
            props.setProperty(AGENT_LOG_FOLDER_PREFIX, fluentAttributes.getOrDefault(AGENT_LOG_FOLDER_PREFIX, LOG_FOLDER_DEFAULT).toString());
            props.setProperty(SERVICE_LOG_FOLDER_PREFIX, fluentAttributes.getOrDefault(SERVICE_LOG_FOLDER_PREFIX, LOG_FOLDER_DEFAULT).toString());
            PropertyPlaceholderHelper propertyPlaceholderHelper = new PropertyPlaceholderHelper("${", "}");
            for (VmLog log : logs) {
                String resolvedPath = propertyPlaceholderHelper.replacePlaceholders(log.getPath(), props);
                log.setPath(resolvedPath);
            }
        }
    }
}
