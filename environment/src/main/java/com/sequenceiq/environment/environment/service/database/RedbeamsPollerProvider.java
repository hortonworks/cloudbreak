package com.sequenceiq.environment.environment.service.database;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.ws.rs.BadRequestException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.dyngr.core.AttemptMaker;
import com.dyngr.core.AttemptResult;
import com.dyngr.core.AttemptResults;
import com.sequenceiq.environment.environment.poller.FlowResultPollerEvaluator;
import com.sequenceiq.flow.api.model.FlowIdentifier;

@Component
public class RedbeamsPollerProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(RedbeamsPollerProvider.class);

    private final RedBeamsService redbeamsService;

    private final FlowResultPollerEvaluator flowResultPollerEvaluator;

    public RedbeamsPollerProvider(
            RedBeamsService redbeamsService,
            FlowResultPollerEvaluator flowResultPollerEvaluator) {
        this.redbeamsService = redbeamsService;
        this.flowResultPollerEvaluator = flowResultPollerEvaluator;
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

    private AttemptResult<List<FlowIdentifier>> evaluateResultWithFlowIdentifier(List<AttemptResult<FlowIdentifier>> results) {
        return results.stream().collect(Collectors.collectingAndThen(Collectors.toList(), flowResultPollerEvaluator::attemptResultFinisher));
    }
}
