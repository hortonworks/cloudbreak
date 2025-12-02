package com.sequenceiq.freeipa.flow.freeipa.rebuild.action;

import static com.sequenceiq.freeipa.flow.freeipa.common.FailureType.VALIDATION;

import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.DetailedStackStatus;
import com.sequenceiq.freeipa.flow.freeipa.provision.event.postinstall.PostInstallFreeIpaSuccess;
import com.sequenceiq.freeipa.flow.freeipa.rebuild.event.health.RebuildValidateHealthFailed;
import com.sequenceiq.freeipa.flow.freeipa.rebuild.event.health.RebuildValidateHealthRequest;
import com.sequenceiq.freeipa.flow.stack.StackContext;

@Component("RebuildValidateHealthAction")
public class RebuildValidateHealthAction extends AbstractRebuildAction<PostInstallFreeIpaSuccess> {
    protected RebuildValidateHealthAction() {
        super(PostInstallFreeIpaSuccess.class);
    }

    @Override
    protected void doExecute(StackContext context, PostInstallFreeIpaSuccess payload, Map<Object, Object> variables) throws Exception {
        stackUpdater().updateStackStatus(context.getStack(), DetailedStackStatus.REBUILD_IN_PROGRESS, "Validate FreeIPA health");
        sendEvent(context, new RebuildValidateHealthRequest(payload.getResourceId()));
    }

    @Override
    protected Object getFailurePayload(PostInstallFreeIpaSuccess payload, Optional<StackContext> flowContext, Exception ex) {
        return new RebuildValidateHealthFailed(payload.getResourceId(), ex, VALIDATION);
    }
}
