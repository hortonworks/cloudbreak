package com.sequenceiq.cloudbreak.service.existingstackfix;

import static com.sequenceiq.cloudbreak.domain.stack.StackFix.StackFixType.UNBOUND_RESTART;

import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.dyngr.Polling;
import com.dyngr.core.AttemptResults;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.core.CloudbreakImageNotFoundException;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.StackFix;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.service.cluster.flow.ClusterOperationService;
import com.sequenceiq.cloudbreak.service.stack.StackImageService;
import com.sequenceiq.flow.api.model.FlowCheckResponse;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.service.FlowService;

@Service
public class UnboundRestartFixService extends ExistingStackFixService {

    static final String AFFECTED_STACK_VERSION = "7.2.11";

    static final Set<String> AFFECTED_IMAGE_IDS = Set.of(
            "26cd5a65-cd5c-457d-8d48-9caf1a486516",
            "19cf97b8-56d8-4be9-b317-998eea99d884",
            "c24acec3-9110-4474-9082-3620deac0910"
    );

    private static final Logger LOGGER = LoggerFactory.getLogger(UnboundRestartFixService.class);

    @Inject
    private StackImageService stackImageService;

    @Inject
    private ClusterOperationService clusterOperationService;

    @Inject
    private FlowService flowService;

    @Override
    public StackFix.StackFixType getStackFixType() {
        return UNBOUND_RESTART;
    }

    @Override
    public boolean isAffected(Stack stack) {
        if (!AFFECTED_STACK_VERSION.equals(stack.getStackVersion())) {
            return false;
        }
        try {
            Image image = stackImageService.getCurrentImage(stack);
            return AFFECTED_IMAGE_IDS.contains(image.getImageId());
        } catch (CloudbreakImageNotFoundException e) {
            throw new IllegalStateException("Image not found for stack " + stack.getResourceCrn(), e);
        }
    }

    @Override
    void doApply(Stack stack) {
        if (!isCmServerReachable(stack)) {
            LOGGER.info("UnboundRestartFixService cannot run, because CM server is unreachable of stack: {}", stack.getResourceCrn());
            throw new RuntimeException("CM server is unreachable for stack: " + stack.getResourceCrn());
        }
        FlowIdentifier flowIdentifier = ThreadBasedUserCrnProvider.doAsInternalActor(() -> clusterOperationService.updateSalt(stack));
        Boolean success = Polling.waitPeriodly(1, TimeUnit.MINUTES)
                .run(() -> {
                    FlowCheckResponse flowState = flowService.getFlowState(flowIdentifier.getPollableId());
                    if (flowState.getHasActiveFlow()) {
                        return AttemptResults.justContinue();
                    }
                    return AttemptResults.finishWith(!flowState.getLatestFlowFinalizedAndFailed());
                });
        if (!success) {
            throw new RuntimeException("Failed to update salt for stack " + stack.getResourceCrn());
        }
    }

    private boolean isCmServerReachable(Stack stack) {
        return stack.getInstanceGroups().stream()
                .flatMap(ig -> ig.getInstanceMetaDataSet().stream())
                .filter(InstanceMetaData::getClusterManagerServer)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Could not find CM server for stack: " + stack.getResourceCrn()))
                .isReachable();
    }
}
