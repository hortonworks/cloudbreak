package com.sequenceiq.datalake.service.sdx;

import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.StackV4Endpoint;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.common.exception.WebApplicationExceptionMessageExtractor;
import com.sequenceiq.datalake.entity.DatalakeStatusEnum;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.service.sdx.flowcheck.CloudbreakFlowService;
import com.sequenceiq.datalake.service.sdx.status.SdxStatusService;
import com.sequenceiq.flow.api.model.FlowIdentifier;

@Service
public class SdxUpgradePrepareService {
    private static final Logger LOGGER = LoggerFactory.getLogger(SdxUpgradePrepareService.class);

    @Inject
    private SdxService sdxService;

    @Inject
    private SdxStatusService sdxStatusService;

    @Inject
    private StackV4Endpoint stackV4Endpoint;

    @Inject
    private CloudbreakFlowService cloudbreakFlowService;

    @Inject
    private WebApplicationExceptionMessageExtractor exceptionMessageExtractor;

    public void prepareUpgrade(Long id, String imageId) {
        SdxCluster sdxCluster = sdxService.getById(id);
        sdxStatusService.setStatusForDatalakeAndNotify(DatalakeStatusEnum.DATALAKE_UPGRADE_PREPARATION_IN_PROGRESS, "Preparing Data Lake for upgrade",
                sdxCluster);
        try {
            String initiatorUserCrn = ThreadBasedUserCrnProvider.getUserCrn();
            LOGGER.debug("Initiating upgrade prepare for {} with image {}", sdxCluster, imageId);
            FlowIdentifier flowIdentifier = ThreadBasedUserCrnProvider.doAsInternalActor(
                    () -> stackV4Endpoint.prepareClusterUpgradeByCrnInternal(0L, sdxCluster.getCrn(), imageId, initiatorUserCrn));
            LOGGER.debug("Upgrade prepare initiated with flow {}", flowIdentifier);
            cloudbreakFlowService.saveLastCloudbreakFlowChainId(sdxCluster, flowIdentifier);
        } catch (WebApplicationException e) {
            String exceptionMessage = exceptionMessageExtractor.getErrorMessage(e);
            String message = String.format("Data Lake upgrade preparation failed on cluster: [%s]. Message: [%s]",
                    sdxCluster.getClusterName(), exceptionMessage);
            throw new CloudbreakServiceException(message, e);
        }
    }
}
