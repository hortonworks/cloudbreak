package com.sequenceiq.cloudbreak.telemetry.metering;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

@Service
public class MeteringConfigService {

    private final MeteringConfiguration meteringConfiguration;

    public MeteringConfigService(MeteringConfiguration meteringConfiguration) {
        this.meteringConfiguration = meteringConfiguration;
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
        return builder
                .withEnabled(enabled)
                .build();
    }
}
