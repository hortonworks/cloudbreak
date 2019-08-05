package com.sequenceiq.cloudbreak.telemetry.metering;

import org.springframework.stereotype.Service;

@Service
public class MeteringConfigService {

    public MeteringConfigView createMeteringConfigs(boolean enabled, String platform, String clusterCrn, String serviceType,
            String serviceVersion) {
        MeteringConfigView.Builder builder = new MeteringConfigView.Builder();
        if (enabled) {
            builder.withPlatform(platform)
                    .withClusterCrn(clusterCrn)
                    .withServiceType(serviceType)
                    .withServiceVersion(serviceVersion);
        }
        return builder
                .withEnabled(enabled)
                .build();
    }
}
