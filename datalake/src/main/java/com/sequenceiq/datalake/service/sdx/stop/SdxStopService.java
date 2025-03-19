package com.sequenceiq.datalake.service.sdx.stop;

import java.util.Optional;
import java.util.Set;

import jakarta.inject.Inject;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.WebApplicationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.common.annotations.VisibleForTesting;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.StackV4Endpoint;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.common.exception.WebApplicationExceptionMessageExtractor;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.datalake.entity.DatalakeStatusEnum;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.flow.SdxReactorFlowManager;
import com.sequenceiq.datalake.flow.chain.DatalakeResizeFlowEventChainFactory;
import com.sequenceiq.datalake.service.FreeipaService;
import com.sequenceiq.datalake.service.sdx.CloudbreakPoller;
import com.sequenceiq.datalake.service.sdx.DistroxService;
import com.sequenceiq.datalake.service.sdx.PollingConfig;
import com.sequenceiq.datalake.service.sdx.SdxService;
import com.sequenceiq.datalake.service.sdx.flowcheck.CloudbreakFlowService;
import com.sequenceiq.datalake.service.sdx.status.SdxStatusService;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.core.FlowLogService;
import com.sequenceiq.flow.domain.FlowLog;
import com.sequenceiq.flow.service.flowlog.FlowChainLogService;

@Component
public class SdxStopService {

    @VisibleForTesting
    static final Set<String> UNSTOPPABLE_FLOWS = Set.of(
            DatalakeResizeFlowEventChainFactory.class.getSimpleName()
    );

    private static final Logger LOGGER = LoggerFactory.getLogger(SdxStopService.class);

    @Inject
    private SdxReactorFlowManager sdxReactorFlowManager;

    @Inject
    private SdxService sdxService;

    @Inject
    private StackV4Endpoint stackV4Endpoint;

    @Inject
    private SdxStatusService sdxStatusService;

    @Inject
    private DistroxService distroxService;

    @Inject
    private FreeipaService freeipaService;

    @Inject
    private CloudbreakFlowService cloudbreakFlowService;

    @Inject
    private WebApplicationExceptionMessageExtractor webApplicationExceptionMessageExtractor;

    @Inject
    private CloudbreakPoller cloudbreakPoller;

    @Inject
    private FlowLogService flowLogService;

    @Inject
    private FlowChainLogService flowChainLogService;

    public FlowIdentifier triggerStopIfClusterNotStopped(SdxCluster cluster) {
        MDCBuilder.buildMdcContext(cluster);
        freeipaService.checkFreeipaRunning(cluster.getEnvCrn());
        Optional<String> reasonUnstoppable = checkIfStoppable(cluster);
        if (reasonUnstoppable.isPresent()) {
            throw new CloudbreakServiceException("Can't stop datalake! Reason: " + reasonUnstoppable.get());
        }
        return sdxReactorFlowManager.triggerSdxStopFlow(cluster);
    }

    public void stop(Long sdxId) {
        SdxCluster sdxCluster = sdxService.getById(sdxId);
        try {
            LOGGER.info("Triggering stop flow for cluster {}", sdxCluster.getClusterName());
            String initiatorUserCrn = ThreadBasedUserCrnProvider.getUserCrn();
            FlowIdentifier flowIdentifier = ThreadBasedUserCrnProvider.doAsInternalActor(
                    () -> stackV4Endpoint.putStopInternal(0L, sdxCluster.getClusterName(), initiatorUserCrn));
            sdxStatusService.setStatusForDatalakeAndNotify(DatalakeStatusEnum.STOP_IN_PROGRESS, "Datalake stop in progress", sdxCluster);
            cloudbreakFlowService.saveLastCloudbreakFlowChainId(sdxCluster, flowIdentifier);
        } catch (NotFoundException e) {
            LOGGER.error("Can not find stack on cloudbreak side {}", sdxCluster.getClusterName());
        } catch (WebApplicationException e) {
            String errorMessage = webApplicationExceptionMessageExtractor.getErrorMessage(e);
            LOGGER.error("Can not stop stack {} from cloudbreak: {}", sdxCluster.getStackId(), errorMessage, e);
            throw new RuntimeException("Cannot stop cluster, error happened during operation: " + errorMessage);
        }
    }

    public void waitCloudbreakCluster(Long sdxId, PollingConfig pollingConfig) {
        SdxCluster sdxCluster = sdxService.getById(sdxId);
        cloudbreakPoller.pollStopUntilStopped(sdxCluster, pollingConfig);
    }

    public void stopAllDatahub(Long sdxId) {
        SdxCluster sdxCluster = sdxService.getById(sdxId);
        distroxService.stopAttachedDistrox(sdxCluster.getEnvCrn());
    }

    public Optional<String> checkIfStoppable(SdxCluster cluster) {
        Optional<String> reasonUnstoppable = Optional.empty();
        Optional<FlowLog> lastFlowLog = flowLogService.getLastFlowLog(cluster.getId());
        if (lastFlowLog.isPresent() && !lastFlowLog.get().getFinalized()) {
            String flowChainType = flowChainLogService.getFlowChainType(
                    lastFlowLog.get().getFlowChainId()
            );
            if (flowChainType != null && UNSTOPPABLE_FLOWS.contains(flowChainType)) {
                reasonUnstoppable = Optional.of(
                        "Datalake " + cluster.getClusterName() + " can not be stopped while flow chain " +
                        flowChainType + " is running."
                );
                LOGGER.info(reasonUnstoppable.get());
            }
        }
        return reasonUnstoppable;
    }
}
