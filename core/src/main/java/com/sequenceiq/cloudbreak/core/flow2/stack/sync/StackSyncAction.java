package com.sequenceiq.cloudbreak.core.flow2.stack.sync;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.event.resource.GetInstancesStateRequest;
import com.sequenceiq.cloudbreak.cloud.event.resource.GetInstancesStateResult;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.converter.spi.InstanceMetaDataToCloudInstanceConverter;
import com.sequenceiq.cloudbreak.core.flow.context.StackStatusUpdateContext;

@Component("StackSyncAction")
public class StackSyncAction extends AbstractStackSyncAction<StackStatusUpdateContext> {

    @Inject
    private InstanceMetaDataToCloudInstanceConverter cloudInstanceConverter;

    public StackSyncAction() {
        super(StackStatusUpdateContext.class);
    }

    @Override
    protected void doExecute(StackSyncContext context, StackStatusUpdateContext payload, Map<Object, Object> variables) {
        List<CloudInstance> cloudInstances = cloudInstanceConverter.convert(context.getInstanceMetaData());
        GetInstancesStateRequest<GetInstancesStateResult> stateRequest = new GetInstancesStateRequest<>(context.getCloudContext(), context.getCloudCredential(),
                cloudInstances);
        sendEvent(context.getFlowId(), stateRequest.selector(), stateRequest);
    }

    @Override
    protected Long getStackId(StackStatusUpdateContext payload) {
        return payload.getStackId();
    }
}
