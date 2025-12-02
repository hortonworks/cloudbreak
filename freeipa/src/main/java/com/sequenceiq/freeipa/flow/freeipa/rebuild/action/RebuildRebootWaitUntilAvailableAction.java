package com.sequenceiq.freeipa.flow.freeipa.rebuild.action;

import static com.sequenceiq.freeipa.flow.freeipa.common.FailureType.ERROR;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.event.instance.RebootInstancesResult;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.DetailedStackStatus;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.flow.stack.HealthCheckFailed;
import com.sequenceiq.freeipa.flow.stack.HealthCheckRequest;
import com.sequenceiq.freeipa.flow.stack.StackContext;

@Component("RebuildRebootWaitUntilAvailableAction")
public class RebuildRebootWaitUntilAvailableAction extends AbstractRebuildAction<RebootInstancesResult> {

    protected RebuildRebootWaitUntilAvailableAction() {
        super(RebootInstancesResult.class);
    }

    @Override
    protected void doExecute(StackContext context, RebootInstancesResult payload, Map<Object, Object> variables) throws Exception {
        stackUpdater().updateStackStatus(context.getStack(), DetailedStackStatus.REBUILD_IN_PROGRESS, "Waiting for FreeIPA to be available");
        sendEvent(context);
    }

    @Override
    protected Selectable createRequest(StackContext context) {
        List<String> instanceIds = context.getStack().getNotDeletedInstanceMetaDataSet().stream()
                .map(InstanceMetaData::getInstanceId).toList();
        return new HealthCheckRequest(context.getStack().getId(), true, instanceIds);
    }

    @Override
    protected Object getFailurePayload(RebootInstancesResult payload, Optional<StackContext> flowContext, Exception ex) {
        return new HealthCheckFailed(payload.getResourceId(), payload.getInstanceIds(), ex, ERROR);
    }
}
