package com.sequenceiq.cloudbreak.core.flow2.service;

import java.util.List;
import java.util.Optional;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.cedarsoftware.util.io.JsonReader;
import com.sequenceiq.cloudbreak.core.flow2.chain.FlowChainTriggers;
import com.sequenceiq.cloudbreak.core.flow2.cluster.termination.ClusterTerminationState;
import com.sequenceiq.cloudbreak.core.flow2.stack.termination.StackTerminationState;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.kerberos.KerberosConfigService;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.TerminationEvent;
import com.sequenceiq.flow.core.ApplicationFlowInformation;
import com.sequenceiq.flow.core.FlowLogService;
import com.sequenceiq.flow.domain.FlowLog;

@Service
public class TerminationTriggerService {

    private static final Logger LOGGER = LoggerFactory.getLogger(TerminationTriggerService.class);

    @Inject
    private KerberosConfigService kerberosConfigService;

    @Inject
    private FlowCancelService flowCancelService;

    @Inject
    private ReactorNotifier reactorNotifier;

    @Inject
    private FlowLogService flowLogService;

    @Inject
    private ApplicationFlowInformation applicationFlowInformation;

    public void triggerTermination(Stack stack, boolean forced) {
        flowCancelService.cancelTooOldTerminationFlowForResource(stack.getId(), stack.getName());
        if (!stack.isDeleteCompleted()) {
            handleIfStackIsAlreadyTerminating(stack, forced);
        } else {
            LOGGER.debug("Stack is already deleted.");
        }
    }

    private void handleIfStackIsAlreadyTerminating(Stack stack, boolean forced) {
        LOGGER.info("stack {} in environment {} is already delete in progress. Current termination request is {}", stack.getName(), stack.getEnvironmentCrn(),
                getForcedOrNotString(forced));
        Optional<FlowLog> optionalFlowLog = findLatestTerminationFlowLogWithInitState(stack);
        if (optionalFlowLog.isPresent()) {
            FlowLog flowLog = optionalFlowLog.get();
            LOGGER.debug("Found termination flowlog with id [{}] and payload [{}]", flowLog.getFlowId(), flowLog.getPayload());
            handleIfFlowLogExistsForTermination(stack, forced, flowLog);
        } else {
            LOGGER.warn("Couldn't find FlowLog with 'PRE_TERMINATION_STATE'. Triggering termination");
            fireTerminationEvent(stack, forced);
        }
    }

    private Optional<FlowLog> findLatestTerminationFlowLogWithInitState(Stack stack) {
        List<FlowLog> flowLogs = flowLogService.findAllByResourceIdAndFinalizedIsFalseOrderByCreatedDesc(stack.getId());
        return flowLogs.stream()
                .filter(flowLog -> applicationFlowInformation.getTerminationFlow().stream()
                        .anyMatch(terminationFlowClass -> terminationFlowClass.equals(flowLog.getFlowType())))
                .filter(fl -> StackTerminationState.INIT_STATE.name().equalsIgnoreCase(fl.getCurrentState())
                        || ClusterTerminationState.INIT_STATE.name().equalsIgnoreCase(fl.getCurrentState()))
                .findFirst();
    }

    private void handleIfFlowLogExistsForTermination(Stack stack, boolean forced, FlowLog flowLog) {
        boolean runningFlowForced = isRunningFlowForced(flowLog);
        if (!runningFlowForced && forced) {
            handleRunningFlowIsNotForced(stack, flowLog);
        } else {
            LOGGER.info("Not triggering termination because currently running flow is {} and current termination request is {}",
                    getForcedOrNotString(runningFlowForced), getForcedOrNotString(forced));
        }
    }

    private boolean isRunningFlowForced(FlowLog fl) {
        Class<?> payloadType = fl.getPayloadType();
        if (TerminationEvent.class.equals(payloadType)) {
            TerminationEvent payload = (TerminationEvent) JsonReader.jsonToJava(fl.getPayload());
            return Boolean.TRUE.equals(payload.getForced());
        } else {
            LOGGER.warn("Payloadtype [{}] is not 'TerminationEvent' for flow [{}]", fl.getPayloadType(), fl.getFlowId());
            return false;
        }
    }

    private void handleRunningFlowIsNotForced(Stack stack, FlowLog fl) {
        LOGGER.info("Terminate stack {} in environment {} because the current flow is not force termination.",
                stack.getName(), stack.getEnvironmentCrn());
        flowCancelService.cancelFlowSilently(fl);
        fireTerminationEvent(stack, true);
    }

    private String getForcedOrNotString(boolean forced) {
        return forced ? "forced" : "not forced";
    }

    private void fireTerminationEvent(Stack stack, boolean forced) {
        Long stackId = stack.getId();
        boolean secure = isKerberosConfigAvailableForCluster(stack);
        String selector = secure ? FlowChainTriggers.PROPER_TERMINATION_TRIGGER_EVENT : FlowChainTriggers.TERMINATION_TRIGGER_EVENT;
        reactorNotifier.notify(stackId, selector, new TerminationEvent(selector, stackId, forced));
        flowCancelService.cancelRunningFlows(stackId);
    }

    private boolean isKerberosConfigAvailableForCluster(Stack stack) {
        boolean result = false;
        try {
            result = kerberosConfigService.isKerberosConfigExistsForEnvironment(stack.getEnvironmentCrn(), stack.getName());
        } catch (Exception ex) {
            LOGGER.warn("Failed to get Kerberos config from FreeIPA service.", ex);
        }
        return result;
    }
}
