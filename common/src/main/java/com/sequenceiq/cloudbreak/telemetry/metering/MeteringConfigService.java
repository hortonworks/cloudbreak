package com.sequenceiq.cloudbreak.telemetry.metering;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.telemetry.TelemetryClusterDetails;
import com.sequenceiq.cloudbreak.telemetry.TelemetryPillarConfigGenerator;
import com.sequenceiq.cloudbreak.telemetry.TelemetryUpgradeConfiguration;
import com.sequenceiq.cloudbreak.telemetry.context.MeteringContext;
import com.sequenceiq.cloudbreak.telemetry.context.TelemetryContext;
import com.sequenceiq.cloudbreak.telemetry.fluent.FluentClusterType;

@Service
public class MeteringConfigService implements TelemetryPillarConfigGenerator<MeteringConfigView> {

    private static final String SALT_STATE = "metering";

    private final MeteringConfiguration meteringConfiguration;

    private final TelemetryUpgradeConfiguration telemetryUpgradeConfiguration;

    public MeteringConfigService(MeteringConfiguration meteringConfiguration, TelemetryUpgradeConfiguration telemetryUpgradeConfiguration) {
        this.meteringConfiguration = meteringConfiguration;
        this.telemetryUpgradeConfiguration = telemetryUpgradeConfiguration;
    }

    @Override
    public MeteringConfigView createConfigs(TelemetryContext context) {
        MeteringContext meteringContext = context.getMeteringContext();
        TelemetryClusterDetails clusterDetails = context.getClusterDetails();
        MeteringConfigView.Builder builder = new MeteringConfigView.Builder();
        if (telemetryUpgradeConfiguration.isEnabled() && telemetryUpgradeConfiguration.getMeteringAgent() != null) {
            builder.withDesiredMeteringAgentDate(telemetryUpgradeConfiguration.getMeteringAgent().getDesiredDate());
        }
        return builder
                .withEnabled(meteringContext.isEnabled())
                .withPlatform(clusterDetails.getPlatform())
                .withClusterCrn(clusterDetails.getCrn())
                .withClusterName(clusterDetails.getName())
                .withServiceType(StringUtils.upperCase(meteringContext.getServiceType()))
                .withServiceVersion(meteringContext.getVersion())
                .withStreamName(meteringConfiguration.getDbusStreamName())
                .build();
    }

    @Override
    public boolean isEnabled(TelemetryContext context) {
        return context != null && context.getTelemetry() != null && context.getTelemetry().isMeteringFeatureEnabled()
                && context.getClusterDetails() != null && FluentClusterType.DATAHUB.equals(context.getClusterType());
    }

    @Override
    public String saltStateName() {
        return SALT_STATE;
    }
}
