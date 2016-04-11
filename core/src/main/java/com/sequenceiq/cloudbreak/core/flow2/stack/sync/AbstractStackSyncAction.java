package com.sequenceiq.cloudbreak.core.flow2.stack.sync;

import static com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone.availabilityZone;
import static com.sequenceiq.cloudbreak.cloud.model.Location.location;
import static com.sequenceiq.cloudbreak.cloud.model.Region.region;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.springframework.statemachine.StateContext;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.event.Payload;
import com.sequenceiq.cloudbreak.cloud.event.resource.GetInstancesStateRequest;
import com.sequenceiq.cloudbreak.cloud.event.resource.GetInstancesStateResult;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.Location;
import com.sequenceiq.cloudbreak.converter.spi.CredentialToCloudCredentialConverter;
import com.sequenceiq.cloudbreak.converter.spi.InstanceMetaDataToCloudInstanceConverter;
import com.sequenceiq.cloudbreak.core.flow2.AbstractAction;
import com.sequenceiq.cloudbreak.core.flow2.MessageFactory;
import com.sequenceiq.cloudbreak.domain.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.repository.InstanceMetaDataRepository;
import com.sequenceiq.cloudbreak.service.stack.StackService;

abstract class AbstractStackSyncAction<P extends Payload> extends AbstractAction<StackSyncState, StackSyncEvent, StackSyncContext, P> {

    @Inject
    private StackService stackService;
    @Inject
    private InstanceMetaDataRepository instanceMetaDataRepository;
    @Inject
    private CredentialToCloudCredentialConverter credentialConverter;
    @Inject
    private InstanceMetaDataToCloudInstanceConverter cloudInstanceConverter;

    protected AbstractStackSyncAction(Class<P> payloadClass) {
        super(payloadClass);
    }

    @Override
    protected StackSyncContext createFlowContext(StateContext<StackSyncState, StackSyncEvent> stateContext, P payload) {
        String flowId = (String) stateContext.getMessageHeader(MessageFactory.HEADERS.FLOW_ID.name());
        Long stackId = payload.getStackId();
        Stack stack = stackService.getById(stackId);
        MDCBuilder.buildMdcContext(stack);
        List<InstanceMetaData> instances = new ArrayList<>(instanceMetaDataRepository.findNotTerminatedForStack(stackId));
        Location location = location(region(stack.getRegion()), availabilityZone(stack.getAvailabilityZone()));
        CloudContext cloudContext = new CloudContext(stack.getId(), stack.getName(), stack.cloudPlatform(), stack.getOwner(), stack.getPlatformVariant(),
                location);
        CloudCredential cloudCredential = credentialConverter.convert(stack.getCredential());
        return new StackSyncContext(flowId, stack, instances, cloudContext, cloudCredential);
    }

    @Override
    protected Object getFailurePayload(StackSyncContext flowContext, Exception ex) {
        List<CloudInstance> cloudInstances = cloudInstanceConverter.convert(flowContext.getInstanceMetaData());
        GetInstancesStateRequest<GetInstancesStateResult> stateRequest =
                new GetInstancesStateRequest<>(flowContext.getCloudContext(), flowContext.getCloudCredential(), cloudInstances);
        return new GetInstancesStateResult(ex.getMessage(), ex, stateRequest);
    }

}
