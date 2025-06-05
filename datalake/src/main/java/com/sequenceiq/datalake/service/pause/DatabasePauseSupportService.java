package com.sequenceiq.datalake.service.pause;

import java.util.Map;
import java.util.Optional;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.common.model.DatabaseType;
import com.sequenceiq.datalake.configuration.PlatformConfig;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.entity.SdxDatabase;
import com.sequenceiq.datalake.service.sdx.EnvironmentService;
import com.sequenceiq.datalake.service.sdx.database.DatabaseServerParameterSetter;

@Component
public class DatabasePauseSupportService {
    @Inject
    private EnvironmentService environmentClientService;

    @Inject
    private PlatformConfig platformConfig;

    @Inject
    private Map<CloudPlatform, DatabaseServerParameterSetter> databaseServerParameterSetters;

    public boolean isDatabasePauseSupported(SdxCluster sdxCluster) {
        if (sdxCluster.hasExternalDatabase() && StringUtils.isNotEmpty(sdxCluster.getDatabaseCrn())) {
            CloudPlatform cloudPlatform = CloudPlatform.valueOf(environmentClientService.getByCrn(sdxCluster.getEnvCrn()).getCloudPlatform());
            return platformConfig.isExternalDatabasePauseSupportedFor(cloudPlatform) && isDatabaseTypeSupported(cloudPlatform, sdxCluster.getSdxDatabase());
        } else {
            return false;
        }
    }

    private boolean isDatabaseTypeSupported(CloudPlatform cloudPlatform, SdxDatabase sdxDatabase) {
        return Optional.ofNullable(databaseServerParameterSetters.get(cloudPlatform))
                .flatMap(parameterSetter -> parameterSetter.getDatabaseType(sdxDatabase))
                .map(DatabaseType::isDatabasePauseSupported)
                .orElse(true);
    }
}
