package com.sequenceiq.environment.environment.poller;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import jakarta.ws.rs.BadRequestException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.dyngr.core.AttemptMaker;
import com.dyngr.core.AttemptResult;
import com.dyngr.core.AttemptResults;
import com.dyngr.core.AttemptState;
import com.sequenceiq.environment.environment.service.stack.StackService;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.core.FlowConstants;
import com.sequenceiq.flow.domain.FlowLogWithoutPayload;
import com.sequenceiq.flow.service.flowlog.FlowLogDBService;

@Component
public class StackPollerProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(StackPollerProvider.class);

    private final StackService stackService;

    private final FlowLogDBService flowLogDBService;

    private final FlowResultPollerEvaluator flowResultPollerEvaluator;

    public StackPollerProvider(
            StackService stackService,
            FlowLogDBService flowLogDBService, FlowResultPollerEvaluator lowResultPollerEvaluator) {
        this.stackService = stackService;
        this.flowLogDBService = flowLogDBService;
        this.flowResultPollerEvaluator = lowResultPollerEvaluator;
    }

    public AttemptMaker<List<FlowIdentifier>> saltUpdateOnStacksPoller(List<String> stackNames, Long envId) {
        List<String> mutableNamesList = new ArrayList<>(stackNames);
        return () -> {
            LOGGER.info("Attempting to update salt on {} clusters for environment id {}",
                    mutableNamesList.size(), envId);
            List<String> remaining = new ArrayList<>();
            List<AttemptResult<FlowIdentifier>> results = collectSaltUpdateResults(mutableNamesList,
                    remaining);
            mutableNamesList.retainAll(remaining);
            return evaluateResultWithFlowIdentifier(results);
        };
    }

    private List<AttemptResult<FlowIdentifier>> collectSaltUpdateResults(List<String> stackNames,
            List<String> remaining) {
        return stackNames.stream()
                .map(stackName -> fetchSaltUpdateResults(remaining, stackName))
                .collect(Collectors.toList());
    }

    private AttemptResult<FlowIdentifier> fetchSaltUpdateResults(List<String> remainingStacks, String stackName) {
        try {
            LOGGER.info("Calling cloudbreak to update salt on cluster {}", stackName);
            FlowIdentifier flowIdentifier = stackService.triggerSaltUpdateForStack(stackName);
            return AttemptResults.finishWith(flowIdentifier);
        } catch (BadRequestException e) {
            LOGGER.info("Unable to start salt update on {}.  Cluster has flow running already. Retrying.",
                    stackName);
            remainingStacks.add(stackName);
            return AttemptResults.justContinue();
        } catch (Exception e) {
            String msg = String.format("Failure asking Cloudbreak for a salt update on stack [%s], error message is: %s",
                    stackName, e.getMessage());
            LOGGER.warn(msg);
            return AttemptResults.breakFor(msg);
        }
    }

    private AttemptResult<List<FlowIdentifier>> evaluateResultWithFlowIdentifier(List<AttemptResult<FlowIdentifier>> results) {
        return results.stream().collect(Collectors.collectingAndThen(Collectors.toList(), flowResultPollerEvaluator::attemptResultFinisher));
    }

    public AttemptMaker<Void> stackUpdateConfigPoller(List<String> stackCrns, Long envId, String flowId) {
        List<String> mutableCrnsList = new ArrayList<>(stackCrns);
        return () -> {
            LOGGER.info("Attempting to update pillar information for {} clusters for environment id {}",
                    mutableCrnsList.size(), envId);
            Optional<FlowLogWithoutPayload> flowLog = flowLogDBService.getLastFlowLog(flowId);
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
