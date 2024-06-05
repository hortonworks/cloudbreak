package com.sequenceiq.freeipa.flow.freeipa.rebuild.action;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.event.instance.RebootInstancesRequest;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.DetailedStackStatus;
import com.sequenceiq.freeipa.converter.cloud.InstanceMetaDataToCloudInstanceConverter;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.flow.freeipa.rebuild.event.restore.FreeIpaRestoreSuccess;
import com.sequenceiq.freeipa.flow.instance.InstanceFailureEvent;
import com.sequenceiq.freeipa.flow.stack.StackContext;

@Component("RebuildRebootAction")
public class RebuildRebootAction extends AbstractRebuildAction<FreeIpaRestoreSuccess> {

    @Inject
    private InstanceMetaDataToCloudInstanceConverter instanceMetaDataToCloudInstanceConverter;

    protected RebuildRebootAction() {
        super(FreeIpaRestoreSuccess.class);
    }

    @Override
    protected void doExecute(StackContext context, FreeIpaRestoreSuccess payload, Map<Object, Object> variables) throws Exception {
        stackUpdater().updateStackStatus(context.getStack(), DetailedStackStatus.REBUILD_IN_PROGRESS, "Rebooting FreeIPA instance after restore");
        sendEvent(context);
    }

    @Override
    protected Selectable createRequest(StackContext context) {
        List<CloudInstance> cloudInstances = context.getStack().getNotDeletedInstanceMetaDataSet().stream().map(instanceMetaData ->
                instanceMetaDataToCloudInstanceConverter.convert(instanceMetaData)).collect(Collectors.toList());
        return new RebootInstancesRequest<>(context.getCloudContext(), context.getCloudCredential(), List.of(), cloudInstances);
    }

    @Override
    protected Object getFailurePayload(FreeIpaRestoreSuccess payload, Optional<StackContext> flowContext, Exception ex) {
        List<String> instanceIds = flowContext.map(stackContext -> stackContext.getStack().getNotDeletedInstanceMetaDataSet().stream()
                .map(InstanceMetaData::getInstanceId).toList())
                .orElseGet(List::of);
        return new InstanceFailureEvent(payload.getResourceId(), ex, instanceIds);
    }
}
