package com.sequenceiq.freeipa.service.stack;

import static com.sequenceiq.freeipa.flow.stack.termination.StackTerminationEvent.TERMINATION_EVENT;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.ws.rs.BadRequestException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.common.service.Clock;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.flow.core.ApplicationFlowInformation;
import com.sequenceiq.flow.core.FlowLogService;
import com.sequenceiq.flow.domain.ClassValue;
import com.sequenceiq.flow.domain.FlowLog;
import com.sequenceiq.flow.service.FlowCancelService;
import com.sequenceiq.freeipa.entity.ChildEnvironment;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.stack.termination.StackTerminationState;
import com.sequenceiq.freeipa.flow.stack.termination.event.TerminationEvent;
import com.sequenceiq.freeipa.nodestatus.NodeStatusJobService;
import com.sequenceiq.freeipa.service.freeipa.flow.FreeIpaFlowManager;
import com.sequenceiq.freeipa.service.rotation.FreeIpaSecretRotationService;
import com.sequenceiq.freeipa.sync.FreeipaJobService;

@Service
public class FreeIpaDeletionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIpaDeletionService.class);

    @Inject
    private StackService stackService;

    @Inject
    private ChildEnvironmentService childEnvironmentService;

    @Inject
    private FreeIpaFlowManager flowManager;

    @Inject
    private FlowCancelService flowCancelService;

    @Inject
    private FreeipaJobService freeipaJobService;

    @Inject
    private NodeStatusJobService nodeStatusJobService;

    @Inject
    private FlowLogService flowLogService;

    @Inject
    private ApplicationFlowInformation applicationFlowInformation;

    @Inject
    private Clock clock;

    @Inject
    private FreeIpaSecretRotationService freeIpaSecretRotationService;

    public void delete(String environmentCrn, String accountId, boolean forced) {
        List<Stack> stacks = stackService.findAllByEnvironmentCrnAndAccountId(environmentCrn, accountId);
        if (stacks.isEmpty()) {
            throw new NotFoundException("No FreeIpa found in environment");
        }
        stacks.forEach(stack -> validateDeletion(stack, accountId));
        stacks.forEach(stack -> unscheduleAndTriggerTerminate(stack, forced));
        freeIpaSecretRotationService.cleanupSecretRotationEntries(environmentCrn);
    }

    private void unscheduleAndTriggerTerminate(Stack stack, boolean forced) {
        MDCBuilder.buildMdcContext(stack);
        flowCancelService.cancelTooOldTerminationFlowForResource(stack.getId(), stack.getName());
        freeipaJobService.unschedule(stack);
        nodeStatusJobService.unschedule(stack);
        if (!stack.isDeleteCompleted()) {
            handleIfStackIsNotTerminated(stack, forced);
        } else {
            LOGGER.debug("Stack is already deleted.");
        }
    }

    private void handleIfStackIsNotTerminated(Stack stack, boolean forced) {
        LOGGER.info("Stack {} in environment {} is not deleted.", stack.getName(), stack.getEnvironmentCrn());
        Optional<FlowLog> optionalFlowLog = findLatestTerminationFlowLogWithInitState(stack);
        if (optionalFlowLog.isPresent()) {
            FlowLog flowLog = optionalFlowLog.get();
            LOGGER.debug("Found termination flowlog with id [{}] and payload [{}]", flowLog.getFlowId(), flowLog.getPayloadJackson());
            if (!isRunningFlowForced(flowLog) && forced) {
                LOGGER.info("Cancelling running termination flow as it's not forced, but the requested termination is forced");
                flowCancelService.cancelFlowSilently(flowLog);
                fireTerminationEvent(stack, forced);
            } else if (flowLog.getCreated() != null && flowLog.getCreated() < clock.nowMinus(Duration.ofHours(1L)).toEpochMilli()) {
                LOGGER.info("Cancelling running termination flow before triggering a new one as it's older than 1 hour");
                flowCancelService.cancelFlowSilently(flowLog);
                fireTerminationEvent(stack, forced);
            }
        } else {
            fireTerminationEvent(stack, forced);
        }
    }

    private void fireTerminationEvent(Stack stack, boolean forced) {
        LOGGER.debug("Couldn't find termination FlowLog with 'INIT_STATE'. Triggering termination");
        flowManager.notify(TERMINATION_EVENT.event(), new TerminationEvent(TERMINATION_EVENT.event(), stack.getId(), forced));
        flowCancelService.cancelRunningFlows(stack.getId());
    }

    private Optional<FlowLog> findLatestTerminationFlowLogWithInitState(Stack stack) {
        List<FlowLog> flowLogs = flowLogService.findAllByResourceIdAndFinalizedIsFalseOrderByCreatedDesc(stack.getId());
        return flowLogs.stream()
                .filter(flowLog -> applicationFlowInformation.getTerminationFlow().stream()
                        .anyMatch(flowLog::isFlowType))
                .filter(fl -> StackTerminationState.INIT_STATE.name().equalsIgnoreCase(fl.getCurrentState()))
                .findFirst();
    }

    private void validateDeletion(Stack stack, String accountId) {
        MDCBuilder.buildMdcContext(stack);
        List<ChildEnvironment> childEnvironments = childEnvironmentService.findChildEnvironments(stack, accountId);
        if (!childEnvironments.isEmpty()) {
            String childEnvironmentCrns = childEnvironments.stream()
                    .map(ChildEnvironment::getEnvironmentCrn)
                    .collect(Collectors.joining(", "));
            throw new BadRequestException(String.format("FreeIpa can not be deleted while it has the following child environment(s) attached [%s]",
                    childEnvironmentCrns));
        }
    }

    private boolean isRunningFlowForced(FlowLog fl) {
        ClassValue payloadType = fl.getPayloadType();
        if (payloadType != null && payloadType.isOnClassPath() && TerminationEvent.class.equals(payloadType.getClassValue())) {
            TerminationEvent payload = JsonUtil.readValueUnchecked(fl.getPayloadJackson(), TerminationEvent.class);
            return payload.getForced();
        } else {
            LOGGER.warn("Payloadtype [{}] is not 'TerminationEvent' for flow [{}]", fl.getPayloadType(), fl.getFlowId());
            return false;
        }
    }
}
