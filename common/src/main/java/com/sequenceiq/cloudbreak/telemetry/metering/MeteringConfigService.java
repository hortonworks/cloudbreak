package com.sequenceiq.cloudbreak.telemetry.metering;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.telemetry.TelemetryUpgradeConfiguration;

@Service
public class MeteringConfigService {

    private final MeteringConfiguration meteringConfiguration;

    private final TelemetryUpgradeConfiguration telemetryUpgradeConfiguration;

    public MeteringConfigService(MeteringConfiguration meteringConfiguration, TelemetryUpgradeConfiguration telemetryUpgradeConfiguration) {
        this.meteringConfiguration = meteringConfiguration;
        this.telemetryUpgradeConfiguration = telemetryUpgradeConfiguration;
    }

    public MeteringConfigView createMeteringConfigs(boolean enabled, String platform, String clusterCrn, String clusterName, String serviceType,
            String serviceVersion) {
        MeteringConfigView.Builder builder = new MeteringConfigView.Builder();
        if (enabled) {
            builder.withPlatform(platform)
                    .withClusterCrn(clusterCrn)
                    .withClusterName(clusterName)
                    .withServiceType(StringUtils.upperCase(serviceType))
                    .withServiceVersion(serviceVersion)
                    .withStreamName(meteringConfiguration.getDbusStreamName());
        }
        if (telemetryUpgradeConfiguration.isEnabled() && telemetryUpgradeConfiguration.getMeteringAgent() != null) {
            builder.withDesiredMeteringAgentDate(telemetryUpgradeConfiguration.getMeteringAgent().getDesiredDate());
        }
        return builder
                .withEnabled(enabled)
                .build();
    }
}
