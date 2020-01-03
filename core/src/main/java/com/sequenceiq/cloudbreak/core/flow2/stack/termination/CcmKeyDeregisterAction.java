package com.sequenceiq.cloudbreak.core.flow2.stack.termination;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.ccm.key.CcmResourceUtil;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.reactor.api.event.recipe.CcmKeyDeregisterRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.recipe.ClusterProxyDeregisterSuccess;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Objects;

@Component("CcmKeyDeregisterAction")
public class CcmKeyDeregisterAction extends AbstractStackTerminationAction<ClusterProxyDeregisterSuccess> {

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

        String userCrn = ThreadBasedUserCrnProvider.getUserCrn();
        String actorCrn = Objects.requireNonNull(userCrn, "userCrn is null");
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        String keyId = CcmResourceUtil.getKeyId(stack.getResourceCrn());

        return new CcmKeyDeregisterRequest(stack.getId(), actorCrn, accountId, keyId, stack.getUseCcm());
    }
}
