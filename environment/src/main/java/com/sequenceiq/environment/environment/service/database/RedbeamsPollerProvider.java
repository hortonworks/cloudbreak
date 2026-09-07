package com.sequenceiq.environment.environment.service.database;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.ws.rs.BadRequestException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.dyngr.core.AttemptMaker;
import com.dyngr.core.AttemptResult;
import com.dyngr.core.AttemptResults;
import com.dyngr.core.AttemptState;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.cloud.scheduler.PollGroup;
import com.sequenceiq.environment.environment.poller.FlowResultPollerEvaluator;
import com.sequenceiq.environment.environment.poller.FlowRunningConflictDetector;
import com.sequenceiq.environment.environment.service.stack.StackService;
import com.sequenceiq.environment.store.EnvironmentInMemoryStateStore;
import com.sequenceiq.flow.api.model.FlowCheckResponse;
import com.sequenceiq.flow.api.model.FlowIdentifier;

@Component
public class RedbeamsPollerProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(RedbeamsPollerProvider.class);

    private final RedBeamsService redbeamsService;

    private final FlowResultPollerEvaluator flowResultPollerEvaluator;

    private final StackService stackService;

    public RedbeamsPollerProvider(
            RedBeamsService redbeamsService,
            FlowResultPollerEvaluator flowResultPollerEvaluator,
            StackService stackService) {
        this.redbeamsService = redbeamsService;
        this.flowResultPollerEvaluator = flowResultPollerEvaluator;
        this.stackService = stackService;
    }

    public AttemptMaker<Void> userDefinedTagsFlowsCompletionPoller(List<FlowIdentifier> flowIdentifiers, Long envId) {
        List<FlowIdentifier> remaining = new ArrayList<>(flowIdentifiers);
        return () -> {
            LOGGER.info("Checking completion of user defined tags update on {} redbeams flows for environment with ID {}",
                    remaining.size(), envId);
            List<FlowIdentifier> stillRunning = new ArrayList<>();
            for (FlowIdentifier flowIdentifier : remaining) {
                AttemptResult<Void> result = updateUserDefinedTags(envId, flowIdentifier);
                if (result.getState() == AttemptState.BREAK) {
                    return result;
                }
                if (result.getState() == AttemptState.CONTINUE) {
                    stillRunning.add(flowIdentifier);
                }
            }
            remaining.retainAll(stillRunning);
            return remaining.isEmpty() ? AttemptResults.finishWith(null) : AttemptResults.justContinue();
        };
    }

    public AttemptMaker<List<FlowIdentifier>> userDefinedTagsUpdatePoller(List<String> stackCrns, Long envId, Map<String, String> tags) {
        List<String> mutableCrnsList = new ArrayList<>(stackCrns);
        return () -> {
            LOGGER.info("Attempting to update user defined tags on {} redbeams clusters for environment with ID {}",
                    mutableCrnsList.size(), envId);
            List<String> remaining = new ArrayList<>();
            List<AttemptResult<FlowIdentifier>> results = collectUserDefinedTagsUpdateResults(mutableCrnsList,
                    remaining, tags);
            mutableCrnsList.retainAll(remaining);
            return evaluateResultWithFlowIdentifier(results);
        };
    }

    private List<AttemptResult<FlowIdentifier>> collectUserDefinedTagsUpdateResults(List<String> stackCrns,
            List<String> remaining, Map<String, String> tags) {
        return stackCrns.stream()
                .map(stackCrn -> fetchUserDefinedTagsUpdateResults(remaining, stackCrn, tags))
                .collect(Collectors.toList());
    }

    private AttemptResult<FlowIdentifier> fetchUserDefinedTagsUpdateResults(List<String> remainingStacks, String stackCrn, Map<String, String> tags) {
        try {
            LOGGER.info("Calling Redbeams to update user defined tags for redbeams cluster {}", stackCrn);
            FlowIdentifier flowIdentifier = redbeamsService.triggerUserDefinedTagsUpdate(stackCrn, tags);
            return AttemptResults.finishWith(flowIdentifier);
        } catch (BadRequestException e) {
            LOGGER.info("Unable to start user defined tags update for {}. Redbeams cluster has flow running already. Retrying.",
                    stackCrn);
            remainingStacks.add(stackCrn);
            return AttemptResults.justContinue();
        } catch (Exception e) {
            LOGGER.warn("Failure asking Redbeams for user defined tags update, error message is: {}",
                    e.getMessage());
            return AttemptResults.breakFor(e);
        }
    }

    public AttemptResult<Void> updateUserDefinedTags(Long envId, FlowIdentifier flowIdentifier) {
        return flowPoller(envId, flowIdentifier, "Update user defined tags on DB stack");
    }

    private AttemptResult<Void> flowPoller(Long envId, FlowIdentifier flowIdentifier, String flowName) {
        if (PollGroup.CANCELLED.equals(EnvironmentInMemoryStateStore.get(envId))) {
            LOGGER.info("Stack polling cancelled in in-memory store, id: {}", envId);
            return AttemptResults.breakFor("Stack polling cancelled in in-memory store, id: " + envId);
        }
        FlowCheckResponse flowCheckResponse = ThreadBasedUserCrnProvider.doAsInternalActor(() -> redbeamsService.checkFlow(flowIdentifier));
        LOGGER.debug("Flow status: {}", flowCheckResponse);
        if (flowCheckResponse.getHasActiveFlow()) {
            return AttemptResults.justContinue();
        } else if (flowCheckResponse.getLatestFlowFinalizedAndFailed()) {
            return AttemptResults.breakFor(flowName + " failed.");
        } else {
            return AttemptResults.justFinish();
        }
    }

    private AttemptResult<List<FlowIdentifier>> evaluateResultWithFlowIdentifier(List<AttemptResult<FlowIdentifier>> results) {
        return results.stream().collect(Collectors.collectingAndThen(Collectors.toList(), flowResultPollerEvaluator::attemptResultFinisher));
    }

    public AttemptMaker<List<FlowIdentifier>> userDefinedTagsDeletePoller(List<String> stackCrns, Long envId, Set<String> tagKeys) {
        List<String> mutableCrnsList = new ArrayList<>(stackCrns);
        List<FlowIdentifier> flowIdentifiers = new ArrayList<>();
        return () -> {
            LOGGER.info("Attempting to delete user defined tag keys on {} redbeams clusters for environment with ID {}",
                    mutableCrnsList.size(), envId);
            List<String> remaining = new ArrayList<>();
            List<AttemptResult<FlowIdentifier>> results = mutableCrnsList.stream()
                    .map(stackCrn -> fetchUserDefinedTagsDeleteResults(remaining, stackCrn, tagKeys))
                    .collect(Collectors.toList());
            AttemptResult<List<FlowIdentifier>> result = evaluateResultWithFlowIdentifier(results);
            if (result.getState() == AttemptState.BREAK) {
                return result;
            }
            flowIdentifiers.addAll(results.stream()
                    .filter(attemptResult -> attemptResult.getState() == AttemptState.FINISH)
                    .map(AttemptResult::getResult)
                    .toList());
            mutableCrnsList.retainAll(remaining);
            return mutableCrnsList.isEmpty() ? AttemptResults.finishWith(flowIdentifiers) : AttemptResults.justContinue();
        };
    }

    private AttemptResult<FlowIdentifier> fetchUserDefinedTagsDeleteResults(List<String> remainingStacks, String stackCrn, Set<String> tagKeys) {
        try {
            LOGGER.info("Calling Redbeams to delete user defined tag keys for redbeams cluster {}", stackCrn);
            FlowIdentifier flowIdentifier = redbeamsService.triggerUserDefinedTagsDelete(stackCrn, tagKeys);
            return AttemptResults.finishWith(flowIdentifier);
        } catch (Exception e) {
            if (FlowRunningConflictDetector.isFlowRunningConflict(e)) {
                LOGGER.info("Unable to start user defined tag deletion for {}. Redbeams cluster has flow running already. Retrying.",
                        stackCrn);
                remainingStacks.add(stackCrn);
                return AttemptResults.justContinue();
            }
            LOGGER.warn("Failure asking Redbeams for user defined tag deletion, error message is: {}",
                    e.getMessage());
            return AttemptResults.breakFor(e);
        }
    }
}
