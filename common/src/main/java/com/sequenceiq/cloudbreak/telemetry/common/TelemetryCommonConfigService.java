package com.sequenceiq.cloudbreak.telemetry.common;

import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.PropertyPlaceholderHelper;

import com.sequenceiq.cloudbreak.altus.AltusDatabusConnectionConfiguration;
import com.sequenceiq.cloudbreak.telemetry.TelemetryClusterDetails;
import com.sequenceiq.cloudbreak.telemetry.TelemetryConfigView;
import com.sequenceiq.cloudbreak.telemetry.TelemetryPillarConfigGenerator;
import com.sequenceiq.cloudbreak.telemetry.TelemetryRepoConfiguration;
import com.sequenceiq.cloudbreak.telemetry.TelemetryRepoConfigurationHolder;
import com.sequenceiq.cloudbreak.telemetry.TelemetryUpgradeConfiguration;
import com.sequenceiq.cloudbreak.telemetry.context.LogShipperContext;
import com.sequenceiq.cloudbreak.telemetry.context.TelemetryContext;
import com.sequenceiq.cloudbreak.telemetry.fluent.FluentClusterType;
import com.sequenceiq.common.api.telemetry.model.Telemetry;
import com.sequenceiq.common.api.telemetry.model.VmLog;

@Service
public class TelemetryCommonConfigService implements TelemetryPillarConfigGenerator<TelemetryCommonConfigView> {

    static final String SERVICE_LOG_FOLDER_PREFIX = "serviceLogFolderPrefix";

    static final String AGENT_LOG_FOLDER_PREFIX = "agentLogFolderPrefix";

    static final String SERVER_LOG_FOLDER_PREFIX = "serverLogFolderPrefix";

    private static final String LOG_FOLDER_DEFAULT = "/var/log";

    private static final String SALT_STATE = "telemetry";

    private final AnonymizationRuleResolver anonymizationRuleResolver;

    private final TelemetryUpgradeConfiguration telemetryUpgradeConfiguration;

    private final TelemetryRepoConfigurationHolder telemetryRepoConfigurationHolder;

    private final AltusDatabusConnectionConfiguration altusDatabusConnectionConfiguration;

    public TelemetryCommonConfigService(AnonymizationRuleResolver anonymizationRuleResolver, TelemetryUpgradeConfiguration telemetryUpgradeConfiguration,
            TelemetryRepoConfigurationHolder telemetryRepoConfigurationHolder, AltusDatabusConnectionConfiguration altusDatabusConnectionConfiguration) {
        this.anonymizationRuleResolver = anonymizationRuleResolver;
        this.telemetryUpgradeConfiguration = telemetryUpgradeConfiguration;
        this.altusDatabusConnectionConfiguration = altusDatabusConnectionConfiguration;
        this.telemetryRepoConfigurationHolder = telemetryRepoConfigurationHolder;
    }

    @Override
    public TelemetryCommonConfigView createConfigs(TelemetryContext context) {
        Telemetry telemetry = context.getTelemetry();
        TelemetryClusterDetails clusterDetails = context.getClusterDetails();
        LogShipperContext logShipperContext = context.getLogShipperContext();
        List<VmLog> vmLogs = logShipperContext.getVmLogs();
        resolveLogPathReferences(telemetry, vmLogs);
        TelemetryCommonConfigView.Builder builder = new TelemetryCommonConfigView.Builder();
        if (telemetryUpgradeConfiguration.isEnabled()) {
            if (telemetryUpgradeConfiguration.getCdpTelemetry() != null) {
                builder.withDesiredCdpTelemetryVersion(telemetryUpgradeConfiguration.getCdpTelemetry().getDesiredVersion());
            }
            if (telemetryUpgradeConfiguration.getCdpLoggingAgent() != null) {
                builder.withDesiredCdpLoggingAgentVersion(telemetryUpgradeConfiguration.getCdpLoggingAgent().getDesiredVersion());
            }
            if (telemetryUpgradeConfiguration.getCdpRequestSigner() != null) {
                builder.withDesiredCdpRequestSignerVersion(telemetryUpgradeConfiguration.getCdpRequestSigner().getDesiredVersion());
            }
        }

        TelemetryRepoConfiguration telemetryRepoConfiguration = telemetryRepoConfigurationHolder.selectCorrectRepoConfig(context);

        return builder
                .withClusterDetails(clusterDetails)
                .withRules(anonymizationRuleResolver.decodeRules(telemetry.getRules()))
                .withVmLogs(vmLogs)
                .withDatabusConnectMaxTimeSeconds(altusDatabusConnectionConfiguration.getMaxTimeSeconds())
                .withDatabusConnectRetryTimes(altusDatabusConnectionConfiguration.getRetryTimes())
                .withDatabusConnectRetryDelay(altusDatabusConnectionConfiguration.getRetryDelaySeconds())
                .withDatabusConnectRetryMaxTime(altusDatabusConnectionConfiguration.getRetryMaxTimeSeconds())
                .withRepoName(telemetryRepoConfiguration.name())
                .withRepoBaseUrl(telemetryRepoConfiguration.baseUrl())
                .withRepoGpgKey(telemetryRepoConfiguration.gpgKey())
                .withRepoGpgCheck(telemetryRepoConfiguration.gpgCheck())
                .build();
    }

    @Override
    public boolean isEnabled(TelemetryContext context) {
        return context != null && context.getTelemetry() != null && context.getLogShipperContext() != null && context.getClusterDetails() != null;
    }

    @Override
    public String saltStateName() {
        return SALT_STATE;
    }

    @Override
    public Map<String, Map<String, Map<String, Object>>> getSaltPillars(TelemetryConfigView configView, TelemetryContext context) {
        Map<String, Object> configMap;
        if (FluentClusterType.FREEIPA.equals(context.getClusterType())) {
            configMap = Map.of(saltStateName(), configView.toMap(), "cloudera-manager", context.getPaywallConfigs());
        } else {
            configMap = Map.of(saltStateName(), configView.toMap());
        }
        return Map.of(saltStateName(), Map.of(String.format("/%s/init.sls", saltStateName()), configMap));
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
