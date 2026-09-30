package com.sequenceiq.environment.environment.service.database;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.dyngr.core.AttemptMaker;
import com.dyngr.core.AttemptResult;
import com.dyngr.core.AttemptResults;
import com.dyngr.core.AttemptState;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.cloud.scheduler.PollGroup;
import com.sequenceiq.environment.environment.poller.UserDefinedTagsPollerSupport;
import com.sequenceiq.environment.store.EnvironmentInMemoryStateStore;
import com.sequenceiq.flow.api.model.FlowCheckResponse;
import com.sequenceiq.flow.api.model.FlowIdentifier;

@Component
public class RedbeamsPollerProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(RedbeamsPollerProvider.class);

    private final RedBeamsService redbeamsService;

    private final UserDefinedTagsPollerSupport userDefinedTagsPollerSupport;

    public RedbeamsPollerProvider(
            RedBeamsService redbeamsService,
            UserDefinedTagsPollerSupport userDefinedTagsPollerSupport) {
        this.redbeamsService = redbeamsService;
        this.userDefinedTagsPollerSupport = userDefinedTagsPollerSupport;
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
        return userDefinedTagsPollerSupport.updatePoller(
                stackCrns,
                envId,
                "redbeams clusters",
                "Redbeams",
                "redbeams cluster",
                stackCrn -> redbeamsService.triggerUserDefinedTagsUpdate(stackCrn, tags));
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

    public AttemptMaker<List<FlowIdentifier>> userDefinedTagsDeletePoller(List<String> stackCrns, Long envId, Set<String> tagKeys) {
        return userDefinedTagsPollerSupport.deletePoller(
                stackCrns,
                envId,
                "redbeams clusters",
                "Redbeams",
                "redbeams cluster",
                stackCrn -> redbeamsService.triggerUserDefinedTagsDelete(stackCrn, tagKeys));
    }
}
