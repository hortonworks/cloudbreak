package com.sequenceiq.datalake.service.sdx;

import static com.sequenceiq.datalake.service.sdx.flowcheck.FlowState.FAILED;
import static com.sequenceiq.datalake.service.sdx.flowcheck.FlowState.FINISHED;
import static com.sequenceiq.datalake.service.sdx.flowcheck.FlowState.UNKNOWN;

import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.dyngr.Polling;
import com.dyngr.core.AttemptResult;
import com.dyngr.core.AttemptResults;
import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.cloudbreak.cloud.scheduler.PollGroup;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.datalake.flow.statestore.DatalakeInMemoryStateStore;
import com.sequenceiq.datalake.service.sdx.flowcheck.FlowCheckResponseToFlowStateConverter;
import com.sequenceiq.datalake.service.sdx.flowcheck.FlowState;
import com.sequenceiq.flow.api.FlowEndpoint;
import com.sequenceiq.flow.api.model.FlowCheckResponse;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.api.model.FlowType;

public abstract class AbstractFlowPoller {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractFlowPoller.class);

    @Inject
    private FlowCheckResponseToFlowStateConverter flowCheckResponseToFlowStateConverter;

    @Inject
    private RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    protected abstract FlowEndpoint flowEndpoint();

    public void pollFlowStateByFlowIdentifierUntilComplete(String process, FlowIdentifier flowIdentifier, Long sdxId, PollingConfig pollingConfig) {
        waitForFlowChainStateByFlowIdentifier(process, flowIdentifier, sdxId, pollingConfig, Sets.immutableEnumSet(FINISHED),
                Sets.immutableEnumSet(FAILED, UNKNOWN));
    }

    private void waitForFlowChainStateByFlowIdentifier(String process, FlowIdentifier flowIdentifier, Long sdxId, PollingConfig pollingConfig,
            Set<FlowState> targetStates, Set<FlowState> failedStates) {
        Polling.waitPeriodly(pollingConfig.getSleepTime(), pollingConfig.getSleepTimeUnit())
                .stopIfException(pollingConfig.getStopPollingIfExceptionOccurred())
                .stopAfterDelay(pollingConfig.getDuration(), pollingConfig.getDurationTimeUnit())
                .run(() -> checkFlowStateByFlowIdIdentifier(process, flowIdentifier, sdxId, targetStates, failedStates));
    }

    private AttemptResult<FlowState> checkFlowStateByFlowIdIdentifier(
            String process,
            FlowIdentifier flowIdentifier,
            Long sdxId,
            Set<FlowState> targetStates,
            Set<FlowState> failedStates) {
        LOGGER.info("Polling {} for {} state of process '{}' with pollable id '{}'.",
                getClass().getSimpleName(), flowIdentifier.getType(), process, flowIdentifier.getPollableId());
        if (PollGroup.CANCELLED.equals(DatalakeInMemoryStateStore.get(sdxId))) {
            LOGGER.info("{} polling cancelled in inmemory store, id: {}", process, sdxId);
            return AttemptResults.breakFor(String.format("%s polling cancelled for %s: %s",
                    process, flowIdentifier.getType(), flowIdentifier.getPollableId()));
        }
        FlowCheckResponse flowCheckResponse;
        if (flowIdentifier.getType() == FlowType.FLOW) {
            flowCheckResponse = ThreadBasedUserCrnProvider.doAsInternalActor(
                    regionAwareInternalCrnGeneratorFactory.iam().getInternalCrnForServiceAsString(),
                    () -> flowEndpoint().hasFlowRunningByFlowId(flowIdentifier.getPollableId())
            );
        } else if (flowIdentifier.getType() == FlowType.FLOW_CHAIN) {
            flowCheckResponse = ThreadBasedUserCrnProvider.doAsInternalActor(
                    regionAwareInternalCrnGeneratorFactory.iam().getInternalCrnForServiceAsString(),
                    () -> flowEndpoint().hasFlowRunningByChainId(flowIdentifier.getPollableId())
            );
        } else if (flowIdentifier.getType() == FlowType.NOT_TRIGGERED) {
            return AttemptResults.breakFor(String.format("Flow %s not triggered", flowIdentifier.getPollableId()));
        } else {
            String message = String.format("Unknown flow identifier type %s for flow: %s", flowIdentifier.getType(), flowIdentifier);
            LOGGER.error(message);
            throw new CloudbreakServiceException(message);
        }

        FlowState flowState = flowCheckResponseToFlowStateConverter.convert(flowCheckResponse);
        if (failedStates.contains(flowState)) {
            String message = String.format("%s process had %s with pollable id %s fail with flow state: %s, reason: %s",
                    process, flowIdentifier.getType(), flowIdentifier.getPollableId(), flowState.name(), flowCheckResponse.getReason());
            return AttemptResults.breakFor(message);
        } else if (targetStates.contains(flowState)) {
            return AttemptResults.finishWith(flowState);
        } else {
            return AttemptResults.justContinue();
        }
    }
}
