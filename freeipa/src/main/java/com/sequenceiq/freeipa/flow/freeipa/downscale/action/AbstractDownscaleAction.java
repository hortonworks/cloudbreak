package com.sequenceiq.freeipa.flow.freeipa.downscale.action;

import static com.sequenceiq.cloudbreak.cloud.model.Location.location;
import static com.sequenceiq.cloudbreak.cloud.model.Region.region;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.springframework.statemachine.StateContext;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Location;
import com.sequenceiq.cloudbreak.common.event.Payload;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.freeipa.converter.cloud.CredentialToCloudCredentialConverter;
import com.sequenceiq.freeipa.converter.cloud.InstanceMetaDataToCloudInstanceConverter;
import com.sequenceiq.freeipa.converter.cloud.ResourceToCloudResourceConverter;
import com.sequenceiq.freeipa.converter.cloud.StackToCloudStackConverter;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.freeipa.downscale.DownscaleFlowEvent;
import com.sequenceiq.freeipa.flow.freeipa.downscale.DownscaleState;
import com.sequenceiq.freeipa.flow.stack.AbstractStackAction;
import com.sequenceiq.freeipa.flow.stack.StackContext;
import com.sequenceiq.freeipa.flow.stack.StackFailureEvent;
import com.sequenceiq.freeipa.service.CredentialService;
import com.sequenceiq.freeipa.service.resource.ResourceService;
import com.sequenceiq.freeipa.service.stack.StackService;

public abstract class AbstractDownscaleAction<P extends Payload> extends AbstractStackAction<DownscaleState, DownscaleFlowEvent, StackContext, P> {

    protected static final String INSTANCE_IDS = "INSTANCE_IDS";

    protected static final String OPERATION_ID = "OPERATION_ID";

    protected static final String HOSTS = "DOWNSCALE_HOSTS";

    @Inject
    private StackService stackService;

    @Inject
    private StackToCloudStackConverter cloudStackConverter;

    @Inject
    private CredentialToCloudCredentialConverter credentialConverter;

    @Inject
    private CredentialService credentialService;

    @Inject
    private ResourceService resourceService;

    @Inject
    private ResourceToCloudResourceConverter resourceConverter;

    @Inject
    private InstanceMetaDataToCloudInstanceConverter instanceConverter;

    protected AbstractDownscaleAction(Class<P> payloadClass) {
        super(payloadClass);
    }

    @Override
    protected StackContext createFlowContext(FlowParameters flowParameters, StateContext<DownscaleState, DownscaleFlowEvent> stateContext,
            P payload) {
        Stack stack = stackService.getByIdWithListsInTransaction(payload.getResourceId());
        MDCBuilder.buildMdcContext(stack);
        Location location = location(region(stack.getRegion()));
        CloudContext cloudContext = new CloudContext(stack.getId(), stack.getName(), stack.getCloudPlatform(), stack.getCloudPlatform(),
                location, stack.getOwner(), stack.getOwner(), stack.getAccountId());
        CloudCredential cloudCredential = credentialConverter.convert(credentialService.getCredentialByEnvCrn(stack.getEnvironmentCrn()));
        CloudStack cloudStack = cloudStackConverter.convert(stack);
        return new StackContext(flowParameters, stack, cloudContext, cloudCredential, cloudStack);
    }

    @Override
    protected Object getFailurePayload(P payload, Optional<StackContext> flowContext, Exception ex) {
        return new StackFailureEvent(payload.getResourceId(), ex);
    }

    protected void setInstanceIds(Map<Object, Object> variables, List<String> instanceIds) {
        variables.put(INSTANCE_IDS, instanceIds);
    }

    protected List<String> getInstanceIds(Map<Object, Object> variables) {
        return (List<String>) variables.get(INSTANCE_IDS);
    }

    protected void setOperationId(Map<Object, Object> variables, String operationId) {
        variables.put(OPERATION_ID, operationId);
    }

    protected String getOperationId(Map<Object, Object> variables) {
        return (String) variables.get(OPERATION_ID);
    }

    protected void setHosts(Map<Object, Object> variables, List<String> hosts) {
        variables.put(HOSTS, hosts);
    }

    protected List<String> getHosts(Map<Object, Object> variables) {
        return (List<String>) variables.get(HOSTS);
    }

    protected List<InstanceMetaData> getInstanceMetadataFromStack(Stack stack, List<String> instanceIds) {
        return stack.getAllInstanceMetaDataList().stream()
                .filter(instanceMetaData -> instanceIds.contains(instanceMetaData.getInstanceId()))
                .collect(Collectors.toList());
    }

    protected List<CloudResource> getCloudResources(Stack stack) {
        return resourceService.findAllByStackId(stack.getId()).stream()
                .map(resource -> resourceConverter.convert(resource))
                .collect(Collectors.toList());
    }

    protected List<CloudInstance> getCloudInstances(Stack stack, List<String> instanceIds) {
        return getInstanceMetadataFromStack(stack, instanceIds).stream()
                .map(instanceMetaData -> instanceConverter.convert(instanceMetaData))
                .collect(Collectors.toList());
    }
}
