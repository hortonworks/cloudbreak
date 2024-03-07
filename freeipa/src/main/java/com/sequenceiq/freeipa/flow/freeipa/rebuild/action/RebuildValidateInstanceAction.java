package com.sequenceiq.freeipa.flow.freeipa.rebuild.action;

import static com.sequenceiq.freeipa.flow.freeipa.rebuild.FreeIpaRebuildFlowEvent.VALIDATE_INSTANCE_FINISHED_EVENT;

import java.util.Map;

import jakarta.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.DetailedStackStatus;
import com.sequenceiq.freeipa.flow.freeipa.upscale.action.FreeIpaUpscaleActions;
import com.sequenceiq.freeipa.flow.freeipa.upscale.event.UpscaleStackResult;
import com.sequenceiq.freeipa.flow.stack.StackContext;
import com.sequenceiq.freeipa.flow.stack.StackEvent;
import com.sequenceiq.freeipa.service.stack.instance.InstanceValidationService;

/**
 * TODO
 * Validate newly created instance on cloud is OK
 *
 * @see FreeIpaUpscaleActions#validateInstancesAction()
 */
@Component("RebuildValidateInstanceAction")
public class RebuildValidateInstanceAction extends AbstractRebuildAction<UpscaleStackResult> {

    @Inject
    private InstanceValidationService instanceValidationService;

    protected RebuildValidateInstanceAction() {
        super(UpscaleStackResult.class);
    }

    @Override
    protected void doExecute(StackContext context, UpscaleStackResult payload, Map<Object, Object> variables) throws Exception {
        stackUpdater().updateStackStatus(context.getStack(), DetailedStackStatus.REBUILD_IN_PROGRESS, "Validating new instances");
        instanceValidationService.finishAddInstances(context, payload);
        sendEvent(context, new StackEvent(VALIDATE_INSTANCE_FINISHED_EVENT.event(), payload.getResourceId()));
    }
}
