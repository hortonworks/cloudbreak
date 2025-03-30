package com.sequenceiq.datalake.service.sdx.refresh;

import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.exception.WebApplicationExceptionMessageExtractor;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.service.sdx.CloudbreakPoller;
import com.sequenceiq.datalake.service.sdx.DistroxService;
import com.sequenceiq.datalake.service.sdx.PollingConfig;
import com.sequenceiq.datalake.service.sdx.SdxService;

@Component
public class SdxRefreshService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SdxRefreshService.class);

    @Inject
    private SdxService sdxService;

    @Inject
    private CloudbreakPoller cloudbreakPoller;

    @Inject
    private DistroxService distroxService;

    @Inject
    private WebApplicationExceptionMessageExtractor webApplicationExceptionMessageExtractor;

    public void refreshAllDatahubsServices(Long sdxId) {
        SdxCluster sdxCluster = sdxService.getById(sdxId);

        try {
            distroxService.restartAttachedDistroxClustersServices(sdxCluster.getEnvCrn());
        } catch (WebApplicationException e) {
            String errorMessage = webApplicationExceptionMessageExtractor.getErrorMessage(e);
            LOGGER.info("Can not restart datahubs {} from cloudbreak: {}", sdxCluster.getStackId(), errorMessage, e);
            throw new RuntimeException("Can not restart datahubs, error happened during operation: " + errorMessage);
        }
    }

    public void waitCloudbreakCluster(Long sdxId, PollingConfig pollingConfig) {
        SdxCluster sdxCluster = sdxService.getById(sdxId);
        LOGGER.info("starting refresh polling for {}", sdxCluster.getClusterName());
        cloudbreakPoller.pollUpdateUntilAvailable("Datahub Refresh", sdxCluster, pollingConfig);

    }
}
