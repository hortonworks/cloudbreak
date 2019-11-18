package com.sequenceiq.datalake.service.sdx;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.datalake.configuration.PlatformConfig;
import com.sequenceiq.datalake.controller.exception.BadRequestException;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.sdx.api.model.SdxDatabaseRequest;

@Component
public class SdxExternalDatabaseConfigurer {

    private static final Logger LOGGER = LoggerFactory.getLogger(SdxExternalDatabaseConfigurer.class);

    @Inject
    private PlatformConfig platformConfig;

    public void configure(CloudPlatform cloudPlatform, SdxDatabaseRequest sdxExternalDbRequest, SdxCluster sdxCluster) {
        setPlatformDefaultForCreateDatabaseIfNeeded(sdxExternalDbRequest, sdxCluster, cloudPlatform);
        setExperimentalForCreateDatabaseIfNeeded(sdxExternalDbRequest, sdxCluster, cloudPlatform);
        validate(cloudPlatform, sdxCluster);
    }

    private void validate(CloudPlatform cloudPlatform, SdxCluster sdxCluster) {
        if (sdxCluster.isCreateDatabase() && !platformConfig.isExternalDatabaseSupportedOrExperimental(cloudPlatform)) {
            String message = String.format("Cannot create external database for sdx: %s, for now only %s is/are supported", sdxCluster.getClusterName(),
                    platformConfig.getSupportedExternalDatabasePlatforms());
            LOGGER.debug(message);
            throw new BadRequestException(message);
        }
    }

    void setPlatformDefaultForCreateDatabaseIfNeeded(SdxDatabaseRequest sdxDatabaseRequest, SdxCluster sdxCluster, CloudPlatform cloudPlatform) {
        if (platformConfig.isExternalDatabaseSupportedFor(cloudPlatform) && !isExternalDbSkipped(sdxDatabaseRequest)) {
            sdxCluster.setCreateDatabase(true);
        }
    }

    public boolean isExternalDbSkipped(SdxDatabaseRequest request) {
        return request != null && request.getCreate() != null && !request.getCreate();
    }

    private void setExperimentalForCreateDatabaseIfNeeded(SdxDatabaseRequest sdxDatabaseRequest, SdxCluster sdxCluster, CloudPlatform cloudPlatform) {
        if (sdxDatabaseRequest != null && sdxDatabaseRequest.getCreate() != null && sdxDatabaseRequest.getCreate()) {
            sdxCluster.setCreateDatabase(true);
        }
    }
}
