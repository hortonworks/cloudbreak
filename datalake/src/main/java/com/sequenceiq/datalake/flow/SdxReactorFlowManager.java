package com.sequenceiq.datalake.flow;

import static com.sequenceiq.datalake.flow.create.SdxCreateEvent.RDS_WAIT_EVENT;
import static com.sequenceiq.datalake.flow.datalake.upgrade.DatalakeUpgradeEvent.DATALAKE_UPGRADE_EVENT;
import static com.sequenceiq.datalake.flow.delete.SdxDeleteEvent.SDX_DELETE_EVENT;
import static com.sequenceiq.datalake.flow.repair.SdxRepairEvent.SDX_REPAIR_EVENT;
import static com.sequenceiq.datalake.flow.start.SdxStartEvent.SDX_START_EVENT;
import static com.sequenceiq.datalake.flow.stop.SdxStopEvent.SDX_STOP_EVENT;
import static com.sequenceiq.datalake.flow.upgrade.SdxOsUpgradeEvent.SDX_UPGRADE_EVENT;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.upgrade.UpgradeOptionV4Response;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.common.event.Acceptable;
import com.sequenceiq.cloudbreak.exception.CloudbreakApiException;
import com.sequenceiq.cloudbreak.exception.FlowNotAcceptedException;
import com.sequenceiq.cloudbreak.exception.FlowsAlreadyRunningException;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.flow.datalake.upgrade.event.DatalakeUpgradeStartEvent;
import com.sequenceiq.datalake.flow.delete.event.SdxDeleteStartEvent;
import com.sequenceiq.datalake.flow.repair.event.SdxRepairStartEvent;
import com.sequenceiq.datalake.flow.start.event.SdxStartStartEvent;
import com.sequenceiq.datalake.flow.stop.event.SdxStartStopEvent;
import com.sequenceiq.datalake.flow.upgrade.event.SdxUpgradeStartEvent;
import com.sequenceiq.datalake.settings.SdxRepairSettings;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.api.model.FlowType;
import com.sequenceiq.flow.core.FlowConstants;
import com.sequenceiq.flow.core.model.FlowAcceptResult;
import com.sequenceiq.flow.reactor.ErrorHandlerAwareReactorEventFactory;
import com.sequenceiq.sdx.api.model.SdxRepairRequest;

import reactor.bus.Event;
import reactor.bus.EventBus;

@Service
public class SdxReactorFlowManager {

    private static final long WAIT_FOR_ACCEPT = 10L;

    private static final Logger LOGGER = LoggerFactory.getLogger(SdxReactorFlowManager.class);

    @Inject
    private EventBus reactor;

    @Inject
    private ErrorHandlerAwareReactorEventFactory eventFactory;

    public FlowIdentifier triggerSdxCreation(SdxCluster cluster) {
        LOGGER.info("Trigger Datalake creation for: {}", cluster);
        String selector = RDS_WAIT_EVENT.event();
        String userId = ThreadBasedUserCrnProvider.getUserCrn();
        return notify(selector, new SdxEvent(selector, cluster.getId(), userId));
    }

    public FlowIdentifier triggerSdxDeletion(SdxCluster cluster, boolean forced) {
        LOGGER.info("Trigger Datalake deletion for: {} forced: ", cluster, forced);
        String selector = SDX_DELETE_EVENT.event();
        String userId = ThreadBasedUserCrnProvider.getUserCrn();
        return notify(selector, new SdxDeleteStartEvent(selector, cluster.getId(), userId, forced));
    }

    public FlowIdentifier triggerSdxRepairFlow(SdxCluster cluster, SdxRepairRequest repairRequest) {
        LOGGER.info("Trigger Datalake repair for: {} with settings: {}", cluster, repairRequest);
        SdxRepairSettings settings = SdxRepairSettings.from(repairRequest);
        String selector = SDX_REPAIR_EVENT.event();
        String userId = ThreadBasedUserCrnProvider.getUserCrn();
        return notify(selector, new SdxRepairStartEvent(selector, cluster.getId(), userId, settings));
    }

    public FlowIdentifier triggerDatalakeOsUpgradeFlow(SdxCluster cluster, UpgradeOptionV4Response upgradeOption) {
        LOGGER.info("Trigger Datalake osUpgrade for: {} with settings: {}", cluster, upgradeOption);
        String selector = SDX_UPGRADE_EVENT.event();
        String userId = ThreadBasedUserCrnProvider.getUserCrn();
        return notify(selector, new SdxUpgradeStartEvent(selector, cluster.getId(), userId, upgradeOption));
    }

    public FlowIdentifier triggerDatalakeRuntimeUpgradeFlow(SdxCluster cluster, String imageId) {
        LOGGER.info("Trigger Datalake runtimeUpgrade for: {} with imageId: {}", cluster, imageId);
        String selector = DATALAKE_UPGRADE_EVENT.event();
        String userId = ThreadBasedUserCrnProvider.getUserCrn();
        return notify(selector, new DatalakeUpgradeStartEvent(selector, cluster.getId(), userId, imageId));
    }

    public FlowIdentifier triggerSdxStartFlow(SdxCluster cluster) {
        LOGGER.info("Trigger Datalake start for: {}", cluster);
        String selector = SDX_START_EVENT.event();
        String userId = ThreadBasedUserCrnProvider.getUserCrn();
        return notify(selector, new SdxStartStartEvent(selector, cluster.getId(), userId));
    }

    public FlowIdentifier triggerSdxStopFlow(SdxCluster cluster) {
        LOGGER.info("Trigger Datalake start for: {}", cluster);
        String selector = SDX_STOP_EVENT.event();
        String userId = ThreadBasedUserCrnProvider.getUserCrn();
        return notify(selector, new SdxStartStopEvent(selector, cluster.getId(), userId));
    }

    private FlowIdentifier notify(String selector, SdxEvent acceptable) {
        Map<String, Object> flowTriggerUserCrnHeader = Map.of(FlowConstants.FLOW_TRIGGER_USERCRN, acceptable.getUserId());
        Event<Acceptable> event = eventFactory.createEventWithErrHandler(flowTriggerUserCrnHeader, acceptable);

        reactor.notify(selector, event);
        try {
            FlowAcceptResult accepted = (FlowAcceptResult) event.getData().accepted().await(WAIT_FOR_ACCEPT, TimeUnit.SECONDS);
            if (accepted == null) {
                throw new FlowNotAcceptedException(String.format("Timeout happened when trying to start the flow for sdx cluster %s.",
                        event.getData().getResourceId()));
            } else {
                switch (accepted.getResultType()) {
                    case ALREADY_EXISTING_FLOW:
                        throw new FlowsAlreadyRunningException(String.format("Sdx cluster %s has flows under operation, request not allowed.",
                                event.getData().getResourceId()));
                    case RUNNING_IN_FLOW:
                        return new FlowIdentifier(FlowType.FLOW, accepted.getAsFlowId());
                    case RUNNING_IN_FLOW_CHAIN:
                        return new FlowIdentifier(FlowType.FLOW_CHAIN, accepted.getAsFlowChainId());
                    default:
                        throw new IllegalStateException("Unsupported accept result type: " + accepted.getClass());
                }
            }
        } catch (InterruptedException e) {
            throw new CloudbreakApiException(e.getMessage());
        }

    }
}
