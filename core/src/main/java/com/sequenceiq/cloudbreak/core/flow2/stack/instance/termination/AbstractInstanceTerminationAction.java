package com.sequenceiq.cloudbreak.core.flow2.stack.instance.termination;

import static com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone.availabilityZone;
import static com.sequenceiq.cloudbreak.cloud.model.Location.location;
import static com.sequenceiq.cloudbreak.cloud.model.Region.region;

import java.util.List;

import javax.inject.Inject;

import org.springframework.statemachine.StateContext;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.event.InstancePayload;
import com.sequenceiq.cloudbreak.cloud.event.resource.RemoveInstanceRequest;
import com.sequenceiq.cloudbreak.cloud.event.resource.RemoveInstanceResult;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Location;
import com.sequenceiq.cloudbreak.converter.spi.CredentialToCloudCredentialConverter;
import com.sequenceiq.cloudbreak.converter.spi.InstanceMetaDataToCloudInstanceConverter;
import com.sequenceiq.cloudbreak.converter.spi.ResourceToCloudResourceConverter;
import com.sequenceiq.cloudbreak.converter.spi.StackToCloudStackConverter;
import com.sequenceiq.cloudbreak.core.flow2.AbstractAction;
import com.sequenceiq.cloudbreak.core.flow2.MessageFactory;
import com.sequenceiq.cloudbreak.domain.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.repository.InstanceMetaDataRepository;
import com.sequenceiq.cloudbreak.service.stack.StackService;

abstract class AbstractInstanceTerminationAction<P extends InstancePayload>
        extends AbstractAction<InstanceTerminationState, InstanceTerminationEvent, InstanceTerminationContext, P> {

    @Inject
    private StackService stackService;

    @Inject
    private InstanceMetaDataRepository instanceMetaDataRepository;

    @Inject
    private CredentialToCloudCredentialConverter credentialConverter;

    @Inject
    private StackToCloudStackConverter cloudStackConverter;

    @Inject
    private ResourceToCloudResourceConverter cloudResourceConverter;

    @Inject
    private InstanceMetaDataToCloudInstanceConverter metadataConverter;

    protected AbstractInstanceTerminationAction(Class<P> payloadClass) {
        super(payloadClass);
    }

    @Override
    protected InstanceTerminationContext createFlowContext(StateContext<InstanceTerminationState, InstanceTerminationEvent> stateContext, P payload) {
        String flowId = (String) stateContext.getMessageHeader(MessageFactory.HEADERS.FLOW_ID.name());
        Stack stack = stackService.getById(payload.getStackId());
        MDCBuilder.buildMdcContext(stack);
        Location location = location(region(stack.getRegion()), availabilityZone(stack.getAvailabilityZone()));
        CloudContext cloudContext = new CloudContext(stack.getId(), stack.getName(), stack.cloudPlatform(), stack.getOwner(), stack.getPlatformVariant(),
                location);
        CloudCredential cloudCredential = credentialConverter.convert(stack.getCredential());
        String instanceId = payload.getInstanceId();
        CloudStack cloudStack = cloudStackConverter.convertForTermination(stack, instanceId);
        List<CloudResource> cloudResources = cloudResourceConverter.convert(stack.getResources());
        InstanceMetaData instanceMetaData = instanceMetaDataRepository.findByInstanceId(stack.getId(), instanceId);
        CloudInstance cloudInstance = metadataConverter.convert(instanceMetaData);
        return new InstanceTerminationContext(flowId, stack, cloudContext, cloudCredential, cloudStack, cloudResources, cloudInstance, instanceMetaData);
    }

    @Override
    protected Object getFailurePayload(InstanceTerminationContext flowContext, Exception ex) {
        RemoveInstanceRequest<RemoveInstanceResult> downscaleStackRequest = new RemoveInstanceRequest<>(flowContext.getCloudContext(),
                flowContext.getCloudCredential(), flowContext.getCloudStack(), flowContext.getCloudResources(), flowContext.getCloudInstance());
        return new RemoveInstanceResult(ex.getMessage(), ex, downscaleStackRequest);
    }

}
