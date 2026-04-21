package com.sequenceiq.cloudbreak.service.upgrade;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus.findLastStatusIndexFromListByMultipleStatuses;

import java.util.List;
import java.util.Optional;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.upgrade.UpgradeReinitiableV4Response;
import com.sequenceiq.cloudbreak.common.event.Payload;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.core.flow2.chain.UpgradeDistroxFlowEventChainFactory;
import com.sequenceiq.cloudbreak.core.flow2.event.DistroXUpgradeFlowChainTriggerEvent;
import com.sequenceiq.cloudbreak.domain.stack.StackStatus;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.service.image.ImageChangeDto;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.service.stackstatus.StackStatusService;
import com.sequenceiq.cloudbreak.util.CodUtil;
import com.sequenceiq.distrox.api.v1.distrox.model.upgrade.DistroXUpgradeReplaceVms;
import com.sequenceiq.distrox.api.v1.distrox.model.upgrade.DistroXUpgradeV1Request;
import com.sequenceiq.distrox.api.v1.distrox.model.upgrade.reinit.UpgradeReinitiateStatus;
import com.sequenceiq.flow.api.model.FlowCheckResponse;
import com.sequenceiq.flow.domain.FlowChainLog;
import com.sequenceiq.flow.service.FlowService;
import com.sequenceiq.flow.service.flowlog.FlowChainLogService;

@Service
public class UpgradeReinitiateService {

    private static final Logger LOGGER = LoggerFactory.getLogger(UpgradeReinitiateService.class);

    @Inject
    private StackDtoService stackDtoService;

    @Inject
    private FlowChainLogService flowChainLogService;

    @Inject
    private UpgradeDistroxFlowEventChainFactory upgradeDistroxFlowEventChainFactory;

    @Inject
    private FlowService flowService;

    @Inject
    private StackStatusService stackStatusService;

    public UpgradeReinitiableV4Response checkClusterUpgradeReinitiable(Long stackId) {
        StackDto stack = stackDtoService.getById(stackId);
        if (CodUtil.isCodCluster(stack)) {
            return new UpgradeReinitiableV4Response(
                    UpgradeReinitiateStatus.NON_REINITIABLE,
                    "The cluster is not eligible for upgrade reinitiation: " +
                            "Please note that COD cluster upgrades are supported only through the Operational Database UI or CLI!"
            );
        }

        Optional<FlowChainLog> lastUpgradeFlowChain =
                flowChainLogService.findLastByResourceIdAndFlowChainTypeOrderByCreatedDesc(stackId, upgradeDistroxFlowEventChainFactory.getName());
        return lastUpgradeFlowChain
                .map(this::getUpgradeReinitiableV4ResponseBasedOnFlows)
                .orElseGet(() -> {
                    LOGGER.info("Couldn't find any upgrade flow chain logs for stack with id: {}", stackId);
                    return getUpgradeReinitiableV4ResponseBasedOnStatuses(stackId);
                });
    }

    private UpgradeReinitiableV4Response getUpgradeReinitiableV4ResponseBasedOnFlows(FlowChainLog flowChainLog) {
        FlowCheckResponse flowCheckResponse = flowService.getFlowChainState(flowChainLog.getFlowChainId());
        if (flowCheckResponse.getLatestFlowFinalizedAndFailed()) {
            return new UpgradeReinitiableV4Response(
                    UpgradeReinitiateStatus.REINITIABLE,
                    "The last upgrade for this cluster finished with a failure, therefore the cluster is eligible for upgrade reinitiation."
            );
        } else if (flowCheckResponse.getHasActiveFlow()) {
            return new UpgradeReinitiableV4Response(
                    UpgradeReinitiateStatus.NON_REINITIABLE,
                    "The last upgrade for this cluster is still in progress, therefore there is no reason to reinitiate the upgrade."
            );
        } else {
            return new UpgradeReinitiableV4Response(
                    UpgradeReinitiateStatus.NON_REINITIABLE,
                    "The last upgrade for this cluster finished successfully, therefore there is no reason to reinitiate the upgrade."
            );
        }
    }

    private UpgradeReinitiableV4Response getUpgradeReinitiableV4ResponseBasedOnStatuses(Long stackId) {
        List<DetailedStackStatus> detailedStackStatusList = stackStatusService.findAllStackStatusesById(stackId).stream()
                .map(StackStatus::getDetailedStackStatus)
                .toList();
        int lastUpgradeSuccessIndex = findLastStatusIndexFromListByMultipleStatuses(detailedStackStatusList, DetailedStackStatus.getUpgradeSuccessStatuses());
        int lastUpgradeFailureIndex = findLastStatusIndexFromListByMultipleStatuses(detailedStackStatusList, DetailedStackStatus.getUpgradeFailureStatuses());
        LOGGER.info("Last upgrade success index: {}, last upgrade failure index: {}", lastUpgradeSuccessIndex, lastUpgradeFailureIndex);

        int lastRelevantStatusIndex = Math.max(lastUpgradeSuccessIndex, lastUpgradeFailureIndex);
        if (lastRelevantStatusIndex == -1) {
            return new UpgradeReinitiableV4Response(
                    UpgradeReinitiateStatus.NON_REINITIABLE,
                    "There were no upgrades for this cluster based on the past statuses, therefore upgrade reinitiation is not needed."
            );
        } else if (lastRelevantStatusIndex == lastUpgradeSuccessIndex) {
            return new UpgradeReinitiableV4Response(
                    UpgradeReinitiateStatus.NON_REINITIABLE,
                    "The last upgrade for this cluster finished successfully based on the past statuses, therefore upgrade reinitiation is not needed."
            );
        } else if (lastRelevantStatusIndex == lastUpgradeFailureIndex) {
            return new UpgradeReinitiableV4Response(
                    UpgradeReinitiateStatus.REINITIABLE,
                    "The last upgrade for this cluster finished with a failure based on the past statuses," +
                            " therefore the cluster is eligible for upgrade reinitiation."
            );
        } else {
            return new UpgradeReinitiableV4Response(UpgradeReinitiateStatus.NON_REINITIABLE);
        }
    }

    public Optional<DistroXUpgradeV1Request> tryRetrieveLastDistroxUpgradeV1Request(Long stackId) {
        return flowChainLogService.findLastByResourceIdAndFlowChainTypeOrderByCreatedDesc(stackId, upgradeDistroxFlowEventChainFactory.getName())
                .map(UpgradeReinitiateService::tryDeserializeFlowChainLogTriggerEvent)
                .map(Optional::get)
                .filter(DistroXUpgradeFlowChainTriggerEvent.class::isInstance)
                .map(DistroXUpgradeFlowChainTriggerEvent.class::cast)
                .map(triggerEvent -> {
                    LOGGER.info("Last DistroX upgrade flow chain trigger event: {}", triggerEvent);
                    DistroXUpgradeV1Request distroXUpgradeV1Request = new DistroXUpgradeV1Request();
                    distroXUpgradeV1Request.setImageId(Optional.ofNullable(triggerEvent.getImageChangeDto()).map(ImageChangeDto::getImageId).orElse(null));
                    distroXUpgradeV1Request.setRuntime(triggerEvent.getRuntimeVersion());
                    distroXUpgradeV1Request.setLockComponents(triggerEvent.isLockComponents());
                    distroXUpgradeV1Request.setRollingUpgradeEnabled(triggerEvent.isRollingUpgradeEnabled());
                    distroXUpgradeV1Request.setReplaceVms(DistroXUpgradeReplaceVms.fromBoolean(triggerEvent.isReplaceVms()));
                    return Optional.of(distroXUpgradeV1Request);
                }).orElseGet(() -> {
                    LOGGER.info("Could not retrieve the parameters of the last failed upgrade for stack with id '{}'. ", stackId);
                    return Optional.empty();
                });
    }

    private static Optional<Payload> tryDeserializeFlowChainLogTriggerEvent(FlowChainLog flowChainLog) {
        try {
            return Optional.of(JsonUtil.readValueUnchecked(flowChainLog.getTriggerEventJackson(), Payload.class));
        } catch (IllegalStateException e) {
            LOGGER.debug("Failed to deserialize the trigger event of flow chain log with id '{}'", flowChainLog.getId(), e);
            return Optional.empty();
        }
    }
}
