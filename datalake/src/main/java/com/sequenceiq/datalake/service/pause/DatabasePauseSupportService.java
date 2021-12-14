package com.sequenceiq.datalake.service.pause;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.datalake.configuration.PlatformConfig;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.service.EnvironmentClientService;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;

@Component
public class DatabasePauseSupportService {

    @Inject
    private EnvironmentClientService environmentClientService;

    @Inject
    private PlatformConfig platformConfig;

    public boolean isDatabasePauseSupported(SdxCluster sdxCluster) {
        if (sdxCluster.hasExternalDatabase() && StringUtils.isNotEmpty(sdxCluster.getDatabaseCrn())) {
            DetailedEnvironmentResponse environment = environmentClientService.getByCrn(sdxCluster.getEnvCrn());

            return platformConfig.isExternalDatabasePauseSupportedFor(CloudPlatform.valueOf(environment.getCloudPlatform()));
        }
        return false;
    }
}
