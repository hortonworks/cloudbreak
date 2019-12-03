package com.sequenceiq.freeipa.flow.stack.termination.action;

import java.util.Map;
import java.util.Objects;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.ccm.key.CcmResourceUtil;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.DetailedStackStatus;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.stack.termination.StackTerminationContext;
import com.sequenceiq.freeipa.flow.stack.termination.event.TerminationEvent;
import com.sequenceiq.freeipa.flow.stack.termination.event.ccm.CcmKeyDeregistrationRequest;
import com.sequenceiq.freeipa.service.stack.StackUpdater;

@Component("DeregisterCcmKeyAction")
public class DeregisterCcmKeyAction extends AbstractStackTerminationAction<TerminationEvent> {

    @Inject
    private StackUpdater stackUpdater;

    public DeregisterCcmKeyAction() {
        super(TerminationEvent.class);
    }

    @Override
    protected void doExecute(StackTerminationContext context, TerminationEvent payload, Map<Object, Object> variables) {
        Stack stack = context.getStack();

        stackUpdater.updateStackStatus(stack.getId(), DetailedStackStatus.DEREGISTERING_CCM_KEY,
                "Deregistering CCM key.");

        String userCrn = ThreadBasedUserCrnProvider.getUserCrn();
        String actorCrn = Objects.requireNonNull(userCrn, "userCrn is null");
        String keyId = CcmResourceUtil.getKeyId(stack.getResourceCrn());

        CcmKeyDeregistrationRequest clusterProxyDeregistrationRequest = new CcmKeyDeregistrationRequest(payload.getResourceId(), payload.getForced(), actorCrn,
                stack.getAccountId(), keyId, stack.getUseCcm(), stack.getMinaSshdServiceId());

        sendEvent(context, clusterProxyDeregistrationRequest.selector(), clusterProxyDeregistrationRequest);
    }
}
