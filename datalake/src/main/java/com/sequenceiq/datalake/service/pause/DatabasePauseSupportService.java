package com.sequenceiq.datalake.service.pause;

import javax.inject.Inject;

import org.apache.logging.log4j.util.Strings;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.platform.ExternalDatabasePlatformConfig;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.service.EnvironmentClientService;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;

@Component
public class DatabasePauseSupportService {

    @Inject
    private EnvironmentClientService environmentClientService;

    @Inject
    private ExternalDatabasePlatformConfig externalDatabasePlatformConfig;

    public boolean isDatabasePauseSupported(SdxCluster sdxCluster) {
        if (sdxCluster.hasExternalDatabase() && Strings.isNotEmpty(sdxCluster.getDatabaseCrn())) {
            DetailedEnvironmentResponse environment = environmentClientService.getByCrn(sdxCluster.getEnvCrn());

            return externalDatabasePlatformConfig.isPauseSupportedForExternalDatabase(CloudPlatform.valueOf(environment.getCloudPlatform()));
        }
        return false;
    }
}
