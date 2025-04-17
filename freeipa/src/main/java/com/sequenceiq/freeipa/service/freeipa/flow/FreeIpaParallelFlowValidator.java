package com.sequenceiq.freeipa.service.freeipa.flow;

import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.exception.FlowsAlreadyRunningException;
import com.sequenceiq.cloudbreak.util.Benchmark;
import com.sequenceiq.flow.core.FlowLogService;
import com.sequenceiq.flow.domain.FlowLogIdWithTypeAndTimestamp;
import com.sequenceiq.flow.service.FlowNameFormatService;
import com.sequenceiq.freeipa.flow.freeipa.binduser.create.CreateBindUserFlowConfig;
import com.sequenceiq.freeipa.flow.freeipa.binduser.create.event.CreateBindUserFlowEvent;
import com.sequenceiq.freeipa.flow.freeipa.cleanup.FreeIpaCleanupEvent;
import com.sequenceiq.freeipa.flow.freeipa.cleanup.FreeIpaCleanupFlowConfig;
import com.sequenceiq.freeipa.flow.stack.termination.StackTerminationEvent;

@Component
public class FreeIpaParallelFlowValidator {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIpaParallelFlowValidator.class);

    private static final Set<Class<?>> IGNORED_ALREADY_RUNNING_PARALLEL_FLOWS = Set.of(
            CreateBindUserFlowConfig.class,
            FreeIpaCleanupFlowConfig.class);

    private static final Set<String> ALLOWED_NEW_PARALLEL_FLOWS = Set.of(
            FreeIpaCleanupEvent.CLEANUP_EVENT.event(),
            StackTerminationEvent.TERMINATION_EVENT.event(),
            CreateBindUserFlowEvent.CREATE_BIND_USER_EVENT.event());

    @Inject
    private FlowLogService flowLogService;

    @Inject
    private FlowNameFormatService flowNameFormatService;

    public void checkFlowAllowedToStart(String selector, Long stackId) {
        Benchmark.measure(() -> checkFlowAllowedToStartForBenchmark(selector, stackId),
                LOGGER, "Checking flow for parallel run took {}ms");
    }

    private void checkFlowAllowedToStartForBenchmark(String selector, Long stackId) {
        if (!isNewFlowAllowedToRunParallelToAnyFlows(selector)) {
            checkRunningFlowsSupportParallelRun(stackId);
        }
    }

    private boolean isNewFlowAllowedToRunParallelToAnyFlows(String selector) {
        if (ALLOWED_NEW_PARALLEL_FLOWS.contains(selector)) {
            LOGGER.info("{} is allowed to run parallel next to any flows", selector);
            return true;
        } else {
            LOGGER.info("{} is not allowed to run parallel next to any flows", selector);
            return false;
        }
    }

    private void checkRunningFlowsSupportParallelRun(Long stackId) {
        Set<FlowLogIdWithTypeAndTimestamp> runningNotAllowedFlows = flowLogService.findAllRunningFlowsByResourceId(stackId).stream()
                .filter(flow -> !IGNORED_ALREADY_RUNNING_PARALLEL_FLOWS.contains(flow.getFlowType().getClassValue()))
                .collect(Collectors.toSet());
        LOGGER.info("Running flows which forbids triggering new flow: {}. Starting flow is {}", flowNameFormatService.formatFlows(runningNotAllowedFlows),
                runningNotAllowedFlows.isEmpty() ? "PERMITTED" : "FORBIDDEN");
        if (!runningNotAllowedFlows.isEmpty()) {
            throw new FlowsAlreadyRunningException(String.format("Request not allowed, freeipa cluster already has a running operation. " +
                            "Running operation(s): [%s]",
                    flowNameFormatService.formatFlows(runningNotAllowedFlows)));
        }
    }
}
