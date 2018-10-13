package com.sequenceiq.cloudbreak.core.flow2.stack.instance.termination;

import static com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone.availabilityZone;
import static com.sequenceiq.cloudbreak.cloud.model.Location.location;
import static com.sequenceiq.cloudbreak.cloud.model.Region.region;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;

import org.springframework.statemachine.StateContext;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.event.InstancePayload;
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
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;
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
    protected InstanceTerminationContext createFlowContext(String flowId, StateContext<InstanceTerminationState, InstanceTerminationEvent> stateContext,
            P payload) {
        Stack stack = stackService.getByIdWithListsInTransaction(payload.getStackId());
        MDCBuilder.buildMdcContext(stack);
        Location location = location(region(stack.getRegion()), availabilityZone(stack.getAvailabilityZone()));
        CloudContext cloudContext = new CloudContext(stack.getId(), stack.getName(), stack.cloudPlatform(), stack.getPlatformVariant(),
                location, stack.getCreator().getUserId(), stack.getWorkspace().getId());
        CloudCredential cloudCredential = credentialConverter.convert(stack.getCredential());
        Set<String> instanceIds = payload.getInstanceIds();
        CloudStack cloudStack = cloudStackConverter.convert(stack, instanceIds);
        List<CloudResource> cloudResources = cloudResourceConverter.convert(stack.getResources());
        List<InstanceMetaData> instanceMetaDataList = new ArrayList<>();
        List<CloudInstance> cloudInstances = new ArrayList<>();
        for (String instanceId : instanceIds) {
            InstanceMetaData instanceMetaData = instanceMetaDataRepository.findByInstanceId(stack.getId(), instanceId);
            CloudInstance cloudInstance = metadataConverter.convert(instanceMetaData);
            instanceMetaDataList.add(instanceMetaData);
            cloudInstances.add(cloudInstance);
        }
        return new InstanceTerminationContext(flowId, stack, cloudContext, cloudCredential, cloudStack, cloudResources, cloudInstances, instanceMetaDataList);
    }

    @Override
    protected Object getFailurePayload(P payload, Optional<InstanceTerminationContext> flowContext, Exception ex) {
        return new StackFailureEvent(payload.getStackId(), ex);
    }
}
