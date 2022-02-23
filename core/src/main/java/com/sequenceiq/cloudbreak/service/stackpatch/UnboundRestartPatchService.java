package com.sequenceiq.cloudbreak.service.stackpatch;

import static com.sequenceiq.cloudbreak.domain.stack.StackPatchType.UNBOUND_RESTART;

import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.dyngr.Polling;
import com.dyngr.core.AttemptResult;
import com.dyngr.core.AttemptResults;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.auth.security.internal.InternalCrnModifier;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.core.CloudbreakImageNotFoundException;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.StackPatchType;
import com.sequenceiq.cloudbreak.service.CloudbreakRuntimeException;
import com.sequenceiq.cloudbreak.service.cluster.flow.ClusterOperationService;
import com.sequenceiq.cloudbreak.service.stack.StackImageService;
import com.sequenceiq.flow.api.model.FlowCheckResponse;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.service.FlowService;

@Service
public class UnboundRestartPatchService extends ExistingStackPatchService {

    private static final String AFFECTED_STACK_VERSION = "7.2.11";

    private static final Set<String> AFFECTED_IMAGE_IDS = Set.of(
            "26cd5a65-cd5c-457d-8d48-9caf1a486516",
            "19cf97b8-56d8-4be9-b317-998eea99d884",
            "c24acec3-9110-4474-9082-3620deac0910"
    );

    private static final Logger LOGGER = LoggerFactory.getLogger(UnboundRestartPatchService.class);

    @Inject
    private StackImageService stackImageService;

    @Inject
    private ClusterOperationService clusterOperationService;

    @Inject
    private FlowService flowService;

    @Inject
    private InternalCrnModifier internalCrnModifier;

    @Override
    public StackPatchType getStackPatchType() {
        return UNBOUND_RESTART;
    }

    @Override
    public boolean isAffected(Stack stack) {
        try {
            boolean affected = false;
            if (AFFECTED_STACK_VERSION.equals(stack.getStackVersion())) {
                Image image = stackImageService.getCurrentImage(stack);
                affected = AFFECTED_IMAGE_IDS.contains(image.getImageId());
                LOGGER.debug("Stack {} with version {} and image {} is {} by unbound service restart bug",
                        stack.getResourceCrn(), stack.getStackVersion(), image.getImageId(), affected ? "affected" : "not affected");
            } else {
                LOGGER.debug("Stack {} with version {} is not affected by unbound service restart bug", stack.getResourceCrn(), stack.getStackVersion());
            }
            return affected;
        } catch (CloudbreakImageNotFoundException e) {
            LOGGER.warn("Image not found for stack " + stack.getResourceCrn(), e);
            throw new CloudbreakRuntimeException("Image not found for stack " + stack.getResourceCrn(), e);
        }
    }

    @Override
    boolean doApply(Stack stack) throws ExistingStackPatchApplyException {
        if (isCmServerReachable(stack)) {
            FlowIdentifier flowIdentifier = ThreadBasedUserCrnProvider.doAs(
                    internalCrnModifier.getInternalCrnWithAccountId(Crn.safeFromString(stack.getResourceCrn()).getAccountId()),
                    () -> clusterOperationService.updateSalt(stack));
            LOGGER.debug("Starting update salt for stack {} with flow {}", stack.getResourceCrn(), flowIdentifier.getPollableId());
            Boolean success = Polling.waitPeriodly(1, TimeUnit.MINUTES)
                    .run(() -> pollFlowState(flowIdentifier));
            if (!success) {
                String message = String.format("Failed to update salt for stack %s with flow %s", stack.getResourceCrn(), flowIdentifier.getPollableId());
                LOGGER.warn(message);
                throw new ExistingStackPatchApplyException(message);
            }
            return true;
        } else {
            LOGGER.info("Salt update cannot run, because CM server is unreachable of stack: " + stack.getResourceCrn());
            return false;
        }
    }

    private AttemptResult<Boolean> pollFlowState(FlowIdentifier flowIdentifier) {
        FlowCheckResponse flowState = flowService.getFlowState(flowIdentifier.getPollableId());
        LOGGER.debug("Salt update polling has active flow: {}, with latest fail: {}",
                flowState.getHasActiveFlow(), flowState.getLatestFlowFinalizedAndFailed());
        return flowState.getHasActiveFlow()
                ? AttemptResults.justContinue()
                : AttemptResults.finishWith(!flowState.getLatestFlowFinalizedAndFailed());
    }
}
