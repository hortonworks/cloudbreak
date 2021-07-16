package com.sequenceiq.datalake.service.upgrade.recovery;

import static com.sequenceiq.cloudbreak.util.Benchmark.measure;
import static com.sequenceiq.datalake.entity.DatalakeStatusEnum.DATALAKE_UPGRADE_FAILED;
import static com.sequenceiq.datalake.entity.DatalakeStatusEnum.STOPPED;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.dto.NameOrCrn;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.message.CloudbreakMessagesService;
import com.sequenceiq.datalake.entity.DatalakeStatusEnum;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.entity.SdxStatusEntity;
import com.sequenceiq.datalake.flow.SdxReactorFlowManager;
import com.sequenceiq.datalake.service.sdx.SdxService;
import com.sequenceiq.datalake.service.sdx.status.SdxStatusService;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.sdx.api.model.SdxRecoveryRequest;
import com.sequenceiq.sdx.api.model.SdxRecoveryResponse;
import com.sequenceiq.sdx.api.model.SdxRecoveryType;

@Component
public class SdxUpgradeRecoveryService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SdxUpgradeRecoveryService.class);

    private static final Set<DatalakeStatusEnum> RECOVERABLE_STATUSES =
            new HashSet<>(Arrays.asList(DATALAKE_UPGRADE_FAILED,
                    STOPPED));

    @Inject
    private SdxService sdxService;

    @Inject
    private SdxStatusService sdxStatusService;

    @Inject
    private SdxReactorFlowManager sdxReactorFlowManager;

    @Inject
    private CloudbreakMessagesService messagesService;

    @Inject
    private EntitlementService entitlementService;

    public SdxRecoveryResponse triggerRecovery(String userCrn, NameOrCrn clusterNameOrCrn, SdxRecoveryRequest recoverRequest) {
        SdxCluster cluster;
        if (clusterNameOrCrn.hasName()) {
            cluster = sdxService.getByNameInAccount(userCrn, clusterNameOrCrn.getName());
        } else {
            cluster = sdxService.getByCrn(userCrn, clusterNameOrCrn.getCrn());
        }
        return initSdxRecovery(userCrn, recoverRequest, cluster);
    }

    private SdxRecoveryResponse initSdxRecovery(String userCrn, SdxRecoveryRequest request, SdxCluster cluster) {

        // Request time validations here
        validateDatalakeStatus(cluster);
        // Fetch latest backup id for given runtime from datalake-dr service
        String backupId = "";

        FlowIdentifier flowIdentifier = triggerDatalakeUpgradeRecoveryFlow(request.getType(), cluster);
        String message = getMessage(backupId);
        return new SdxRecoveryResponse(message, flowIdentifier);
    }

    private void validateDatalakeStatus(SdxCluster cluster) {
        String clusterName = cluster.getClusterName();
        SdxStatusEntity actualStatusForSdx = measure(() -> sdxStatusService.getActualStatusForSdx(cluster), LOGGER,
                "Fetching SDX status took {}ms from DB. Name: [{}]", clusterName);
        if (Objects.isNull(actualStatusForSdx)) {
            throw new BadRequestException(String.format("Datalake cluster status with name %s could not be determined", clusterName));
        } else if (actualStatusForSdx.getStatus() != DATALAKE_UPGRADE_FAILED) {
            Optional<SdxStatusEntity> lastRecoverableStatus = sdxService.findByIdAndStatuses(cluster.getId(), RECOVERABLE_STATUSES);
            if (lastRecoverableStatus.isEmpty()) {
                throw new BadRequestException(String.format("Current datalake cluster status is %s, it should have been in either of %s"
                        + " statuses now or previously to be able to start the recovery."
                        , actualStatusForSdx.getStatus().name()
                        , RECOVERABLE_STATUSES));
            }
        }
    }

    private FlowIdentifier triggerDatalakeUpgradeRecoveryFlow(SdxRecoveryType recoveryType, SdxCluster cluster) {
        MDCBuilder.buildMdcContext(cluster);
        return sdxReactorFlowManager.triggerDatalakeRuntimeUpgradeRecoveryFlow(cluster, recoveryType);
    }

    private String getMessage(String imageId) {
        return messagesService.getMessage(ResourceEvent.DATALAKE_UPGRADE.getMessage(), Collections.singletonList(imageId));
    }

}
