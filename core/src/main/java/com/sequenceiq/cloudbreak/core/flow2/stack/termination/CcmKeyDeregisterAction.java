package com.sequenceiq.cloudbreak.core.flow2.stack.termination;

import java.util.Map;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.ccm.key.CcmResourceUtil;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.reactor.api.event.recipe.CcmKeyDeregisterRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.recipe.CcmKeyDeregisterSuccess;
import com.sequenceiq.cloudbreak.reactor.api.event.recipe.ClusterProxyDeregisterSuccess;

@Component("CcmKeyDeregisterAction")
public class CcmKeyDeregisterAction extends AbstractStackTerminationAction<ClusterProxyDeregisterSuccess> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CcmKeyDeregisterAction.class);

    public CcmKeyDeregisterAction() {
        super(ClusterProxyDeregisterSuccess.class);
    }

    @Override
    protected void doExecute(StackTerminationContext context, ClusterProxyDeregisterSuccess payload, Map<Object, Object> variables) {
        if(!context.getTerminationType().isRecovery()) {
            CcmKeyDeregisterRequest deregisterRequest = createRequest(context);
            sendEvent(context, deregisterRequest.selector(), deregisterRequest);
        } else {
            LOGGER.debug("Recovery is in progress, skipping CCM de-registration!");
            Stack stack = context.getStack();
            CcmKeyDeregisterSuccess ccmKeyDeregisterSuccess = new CcmKeyDeregisterSuccess(stack.getId());
            sendEvent(context, ccmKeyDeregisterSuccess.selector(), ccmKeyDeregisterSuccess);
        }
    }

    @Override
    protected CcmKeyDeregisterRequest createRequest(StackTerminationContext context) {
        Stack stack = context.getStack();

        String userCrn = ThreadBasedUserCrnProvider.getUserCrn();
        String actorCrn = Objects.requireNonNull(userCrn, "userCrn is null");
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        String keyId = CcmResourceUtil.getKeyId(stack.getResourceCrn());

        return new CcmKeyDeregisterRequest(stack.getId(), actorCrn, accountId, keyId, stack.getTunnel());
    }
}
