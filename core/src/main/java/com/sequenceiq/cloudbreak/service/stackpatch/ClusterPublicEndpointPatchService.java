package com.sequenceiq.cloudbreak.service.stackpatch;

import static com.sequenceiq.cloudbreak.domain.stack.StackPatchType.CLUSTER_PUBLIC_ENDPOINT;

import jakarta.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.auth.security.internal.InternalCrnModifier;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.StackPatchType;
import com.sequenceiq.cloudbreak.service.publicendpoint.ClusterPublicEndpointManagementService;
import com.sequenceiq.cloudbreak.service.stackpatch.config.ClusterPublicEndpointPatchConfig;

@Service
public class ClusterPublicEndpointPatchService extends ExistingStackPatchService {

    @Inject
    private ClusterPublicEndpointManagementService clusterPublicEndpointManagementService;

    @Inject
    private ClusterPublicEndpointPatchConfig clusterPublicEndpointPatchConfig;

    @Inject
    private InternalCrnModifier internalCrnModifier;

    @Override
    public StackPatchType getStackPatchType() {
        return CLUSTER_PUBLIC_ENDPOINT;
    }

    @Override
    public boolean isAffected(Stack stack) {
        return clusterPublicEndpointPatchConfig.getRelatedStacks().contains(stack.getId())
                && !stack.isStackInDeletionPhase()
                && !stack.isStackInStopPhase();
    }

    @Override
    boolean doApply(Stack stack) {
        ThreadBasedUserCrnProvider.doAs(
                internalCrnModifier.getInternalCrnWithAccountId(Crn.safeFromString(stack.getResourceCrn()).getAccountId()),
                () -> clusterPublicEndpointManagementService.start(stack));
        return true;
    }
}
