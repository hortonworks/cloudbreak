package com.sequenceiq.environment.environment.poller;

import com.dyngr.core.AttemptMaker;
import com.dyngr.core.AttemptResult;
import com.dyngr.core.AttemptResults;
import com.dyngr.core.AttemptState;
import com.sequenceiq.environment.environment.service.stack.StackService;
import com.sequenceiq.flow.core.FlowConstants;
import com.sequenceiq.flow.domain.FlowLog;
import com.sequenceiq.flow.service.flowlog.FlowLogDBService;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.ws.rs.BadRequestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class StackPollerProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(StackPollerProvider.class);

    private final StackService stackService;

    private final FlowLogDBService flowLogDBService;

    public StackPollerProvider(
        StackService stackService,
        FlowLogDBService flowLogDBService) {
        this.stackService = stackService;
        this.flowLogDBService = flowLogDBService;
    }

    public AttemptMaker<Void> stackUpdateConfigPoller(List<String> stackCrns, Long envId, String flowId) {
        List<String> mutableCrnsList = new ArrayList<>(stackCrns);
        return () -> {
            LOGGER.info("Attempting to update pillar information for {} clusters for environment id {}",
                mutableCrnsList.size(), envId);
            Optional<FlowLog> flowLog = flowLogDBService.getLastFlowLog(flowId);
            if (flowLog.isPresent() && flowLog.get().getCurrentState().equals(FlowConstants.CANCELLED_STATE)) {
                return AttemptResults.finishWith(null);
            }
            List<String> remaining = new ArrayList<>();
            List<AttemptResult<Void>> results = collectStackUpdateConfigResults(mutableCrnsList,
                remaining, envId);
            mutableCrnsList.retainAll(remaining);
            return evaluateResult(results);
        };
    }

    private List<AttemptResult<Void>> collectStackUpdateConfigResults(List<String> stackCrns,
        List<String> remaining, Long envId) {
        return stackCrns.stream()
            .map(stackCrn -> fetchStackUpdateConfigResults(remaining, stackCrn))
            .collect(Collectors.toList());
    }

    private AttemptResult<Void> fetchStackUpdateConfigResults(List<String> remainingStacks, String stackCrn) {
        try {
            LOGGER.info("Calling cloudbreak to to update pillar config for cluster {}", stackCrn);
            stackService.triggerConfigUpdateForStack(stackCrn);
            return AttemptResults.finishWith(null);
        } catch (BadRequestException e) {
            LOGGER.info("Unable to start pillar config update for {}.  Cluster has flow running already. Retrying.",
                stackCrn);
            remainingStacks.add(stackCrn);
            return AttemptResults.justContinue();
        } catch (Exception e) {
            LOGGER.warn("Failure asking Cloudbreak for a pillar config update, error message is: {}",
                e.getMessage());
            return AttemptResults.breakFor(e);
        }
    }

    AttemptResult<Void> evaluateResult(List<AttemptResult<Void>> results) {
        Optional<AttemptResult<Void>> error = results.stream()
            .filter(it -> it.getState() == AttemptState.BREAK).findFirst();
        if (error.isPresent()) {
            return error.get();
        }

        if (shouldContinue(results)) {
            return AttemptResults.justContinue();
        }
        return AttemptResults.finishWith(null);
    }

    private boolean shouldContinue(List<AttemptResult<Void>> results) {
        return results.stream().anyMatch(result -> result.getState() == AttemptState.CONTINUE);
    }
}
