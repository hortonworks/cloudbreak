package com.sequenceiq.datalake.service.sdx.start;

import javax.inject.Inject;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.ClientErrorException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.WebApplicationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.StackV4Endpoint;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.common.exception.WebApplicationExceptionMessageExtractor;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.datalake.entity.DatalakeStatusEnum;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.flow.SdxReactorFlowManager;
import com.sequenceiq.datalake.service.FreeipaService;
import com.sequenceiq.datalake.service.sdx.flowcheck.CloudbreakFlowService;
import com.sequenceiq.datalake.service.sdx.PollingConfig;
import com.sequenceiq.datalake.service.sdx.SdxService;
import com.sequenceiq.datalake.service.sdx.cert.CloudbreakPoller;
import com.sequenceiq.datalake.service.sdx.status.SdxStatusService;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.describe.DescribeFreeIpaResponse;

@Component
public class SdxStartService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SdxStartService.class);

    @Inject
    private SdxReactorFlowManager sdxReactorFlowManager;

    @Inject
    private SdxService sdxService;

    @Inject
    private StackV4Endpoint stackV4Endpoint;

    @Inject
    private SdxStatusService sdxStatusService;

    @Inject
    private WebApplicationExceptionMessageExtractor webApplicationExceptionMessageExtractor;

    @Inject
    private FreeipaService freeipaService;

    @Inject
    private CloudbreakFlowService cloudbreakFlowService;

    @Inject
    private CloudbreakPoller cloudbreakPoller;

    public FlowIdentifier triggerStartIfClusterNotRunning(SdxCluster cluster) {
        MDCBuilder.buildMdcContext(cluster);
        checkFreeipaRunning(cluster.getEnvCrn());
        return sdxReactorFlowManager.triggerSdxStartFlow(cluster);
    }

    public void start(Long sdxId) {
        SdxCluster sdxCluster = sdxService.getById(sdxId);
        try {
            LOGGER.info("Triggering start flow for cluster {}", sdxCluster.getClusterName());
            String initiatorUserCrn = ThreadBasedUserCrnProvider.getUserCrn();
            FlowIdentifier flowIdentifier = ThreadBasedUserCrnProvider.doAsInternalActor(() ->
                    stackV4Endpoint.putStartInternal(0L, sdxCluster.getClusterName(), initiatorUserCrn));
            cloudbreakFlowService.saveLastCloudbreakFlowChainId(sdxCluster, flowIdentifier);
            sdxStatusService.setStatusForDatalakeAndNotify(DatalakeStatusEnum.START_IN_PROGRESS, "Datalake start in progress", sdxCluster);
        } catch (NotFoundException e) {
            LOGGER.info("Can not find stack on cloudbreak side {}", sdxCluster.getClusterName());
        } catch (ClientErrorException e) {
            String errorMessage = webApplicationExceptionMessageExtractor.getErrorMessage(e);
            LOGGER.info("Can not start stack {} from cloudbreak: {}", sdxCluster.getStackId(), errorMessage, e);
            throw new RuntimeException("Cannot start cluster, error happened during operation: " + errorMessage);
        } catch (WebApplicationException e) {
            String errorMessage = webApplicationExceptionMessageExtractor.getErrorMessage(e);
            LOGGER.info("Can not start stack {} from cloudbreak: {}", sdxCluster.getStackId(), errorMessage, e);
            throw new RuntimeException("Can not start cluster, error happened during operation: " + errorMessage);
        }
    }

    public void waitCloudbreakCluster(Long sdxId, PollingConfig pollingConfig) {
        SdxCluster sdxCluster = sdxService.getById(sdxId);
        cloudbreakPoller.pollStartUntilAvailable(sdxCluster, pollingConfig);
    }

    private void checkFreeipaRunning(String envCrn) {
        DescribeFreeIpaResponse freeipa = freeipaService.describe(envCrn);
        if (freeipa != null && freeipa.getAvailabilityStatus() != null && !freeipa.getAvailabilityStatus().isAvailable()) {
            throw new BadRequestException("Freeipa should be in Available state but currently is " + freeipa.getStatus().name());
        }

    }
}
