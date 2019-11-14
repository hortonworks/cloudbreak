package com.sequenceiq.cloudbreak.core.flow2.stack.termination;

import java.util.Map;
import java.util.Objects;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.ccm.key.CcmResourceUtil;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.reactor.api.event.recipe.CcmKeyDeregisterRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.recipe.ClusterProxyDeregisterSuccess;
import com.sequenceiq.cloudbreak.service.StackUpdater;

@Component("CcmKeyDeregisterAction")
public class CcmKeyDeregisterAction extends AbstractStackTerminationAction<ClusterProxyDeregisterSuccess> {

    @Inject
    private StackUpdater stackUpdater;

    @Inject
    private ThreadBasedUserCrnProvider threadBasedUserCrnProvider;

    public CcmKeyDeregisterAction() {
        super(ClusterProxyDeregisterSuccess.class);
    }

    @Override
    protected void doExecute(StackTerminationContext context, ClusterProxyDeregisterSuccess payload, Map<Object, Object> variables) {
        CcmKeyDeregisterRequest deregisterRequest = createRequest(context);
        sendEvent(context, deregisterRequest.selector(), deregisterRequest);
    }

    @Override
    protected CcmKeyDeregisterRequest createRequest(StackTerminationContext context) {
        Stack stack = context.getStack();

        String userCrn = threadBasedUserCrnProvider.getUserCrn();
        String actorCrn = Objects.requireNonNull(userCrn, "userCrn is null");
        String accountId = threadBasedUserCrnProvider.getAccountId();
        String keyId = CcmResourceUtil.getKeyId(stack.getResourceCrn());

        return new CcmKeyDeregisterRequest(stack.getId(), actorCrn, accountId, keyId, stack.getUseCcm());
    }
}
