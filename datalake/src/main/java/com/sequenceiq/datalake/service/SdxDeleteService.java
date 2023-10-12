package com.sequenceiq.datalake.service;

import static com.sequenceiq.cloudbreak.common.exception.NotFoundException.notFound;
import static com.sequenceiq.datalake.entity.DatalakeStatusEnum.PROVISIONING_FAILED;

import java.util.Collection;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackViewV4Response;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.datalake.entity.DatalakeStatusEnum;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.entity.SdxStatusEntity;
import com.sequenceiq.datalake.flow.SdxReactorFlowManager;
import com.sequenceiq.datalake.repository.SdxClusterRepository;
import com.sequenceiq.datalake.service.rotation.SdxRotationService;
import com.sequenceiq.datalake.service.sdx.DistroxService;
import com.sequenceiq.datalake.service.sdx.status.SdxStatusService;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.service.FlowCancelService;

@Service
public class SdxDeleteService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SdxDeleteService.class);

    @Inject
    private FlowCancelService flowCancelService;

    @Inject
    private SdxClusterRepository sdxClusterRepository;

    @Inject
    private SdxStatusService sdxStatusService;

    @Inject
    private SdxReactorFlowManager sdxReactorFlowManager;

    @Inject
    private DistroxService distroxService;

    @Inject
    private SdxRotationService sdxRotationService;

    public FlowIdentifier deleteSdxByClusterCrn(String accountId, String clusterCrn, boolean forced) {
        LOGGER.info("Deleting SDX {}", clusterCrn);
        return sdxClusterRepository.findByAccountIdAndCrnAndDeletedIsNull(accountId, clusterCrn)
                .map(sdxCluster -> deleteSdxCluster(sdxCluster, forced))
                .orElseThrow(() -> notFound("SDX cluster", clusterCrn).get());
    }

    public FlowIdentifier deleteSdx(String accountId, String name, boolean forced) {
        LOGGER.info("Deleting SDX {}", name);
        return sdxClusterRepository.findByAccountIdAndClusterNameAndDeletedIsNull(accountId, name)
                .map(sdxCluster -> deleteSdxCluster(sdxCluster, forced))
                .orElseThrow(() -> notFound("SDX cluster", name).get());
    }

    private FlowIdentifier deleteSdxCluster(SdxCluster sdxCluster, boolean forced) {
        checkIfSdxIsDeletable(sdxCluster, forced);
        MDCBuilder.buildMdcContext(sdxCluster);
        sdxClusterRepository.save(sdxCluster);
        sdxStatusService.setStatusForDatalakeAndNotify(DatalakeStatusEnum.DELETE_REQUESTED, "Datalake deletion requested", sdxCluster);
        FlowIdentifier flowIdentifier = sdxReactorFlowManager.triggerSdxDeletion(sdxCluster, forced);
        flowCancelService.cancelRunningFlows(sdxCluster.getId());
        sdxRotationService.cleanupSecretRotationEntries(sdxCluster.getCrn());
        return flowIdentifier;
    }

    private void checkIfSdxIsDeletable(SdxCluster sdxCluster, boolean forced) {
        Optional<SdxCluster> detachedCluster = sdxClusterRepository
                .findByAccountIdAndEnvCrnAndDeletedIsNullAndDetachedIsTrue(sdxCluster.getAccountId(), sdxCluster.getEnvCrn());
        if (forced && detachedCluster.isPresent() && !detachedCluster.get().getCrn().equals(sdxCluster.getCrn())) {
            return;
        }

        SdxStatusEntity actualStatusForSdx = sdxStatusService.getActualStatusForSdx(sdxCluster);
        if (!forced && actualStatusForSdx.getStatus() != PROVISIONING_FAILED &&
                sdxCluster.hasExternalDatabase() && StringUtils.isEmpty(sdxCluster.getDatabaseCrn())) {
            throw new BadRequestException(String.format("Can not find external database for Data Lake, but it was requested: %s. Please use force delete.",
                    sdxCluster.getClusterName()));
        }
        Collection<StackViewV4Response> attachedDistroXClusters = distroxService.getAttachedDistroXClusters(sdxCluster.getEnvCrn());
        if (!attachedDistroXClusters.isEmpty()) {
            throw new BadRequestException(String.format("The following Data Hub(s) cluster(s) must be terminated " +
                            "before deletion of SDX cluster: [%s].",
                    attachedDistroXClusters.stream().map(StackViewV4Response::getName).collect(Collectors.joining(", "))));
        }
    }
}
