package com.sequenceiq.cloudbreak.core.flow2.stack.upscale;

import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.action.Action;

import com.sequenceiq.cloudbreak.cloud.event.Selectable;
import com.sequenceiq.cloudbreak.cloud.event.instance.CollectMetadataRequest;
import com.sequenceiq.cloudbreak.cloud.event.instance.CollectMetadataResult;
import com.sequenceiq.cloudbreak.cloud.event.resource.UpscaleStackRequest;
import com.sequenceiq.cloudbreak.cloud.event.resource.UpscaleStackResult;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.converter.spi.CloudResourceToResourceConverter;
import com.sequenceiq.cloudbreak.converter.spi.ResourceToCloudResourceConverter;
import com.sequenceiq.cloudbreak.converter.spi.StackToCloudStackConverter;
import com.sequenceiq.cloudbreak.core.flow2.event.StackScaleTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.stack.AbstractStackFailureAction;
import com.sequenceiq.cloudbreak.core.flow2.stack.StackFailureContext;
import com.sequenceiq.cloudbreak.core.flow2.stack.downscale.StackScalingFlowContext;
import com.sequenceiq.cloudbreak.domain.InstanceGroup;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.BootstrapNewNodesRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.BootstrapNewNodesResult;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.ExtendHostMetadataRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.ExtendHostMetadataResult;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetadataService;

@Configuration
public class StackUpscaleActions {
    private static final Logger LOGGER = LoggerFactory.getLogger(StackUpscaleActions.class);

    @Inject
    private ResourceToCloudResourceConverter cloudResourceConverter;

    @Inject
    private CloudResourceToResourceConverter resourceConverter;

    @Inject
    private InstanceMetadataService instanceMetadataService;

    @Inject
    private StackUpscaleService stackUpscaleService;

    @Inject
    private StackToCloudStackConverter cloudStackConverter;

    @Bean(name = "ADD_INSTANCES_STATE")
    public Action addInstances() {
        return new AbstractStackUpscaleAction<StackScaleTriggerEvent>(StackScaleTriggerEvent.class) {
            @Override
            protected void prepareExecution(StackScaleTriggerEvent payload, Map<Object, Object> variables) {
                variables.put(INSTANCEGROUPNAME, payload.getInstanceGroup());
                variables.put(ADJUSTMENT, payload.getAdjustment());
            }

            @Override
            protected void doExecute(StackScalingFlowContext context, StackScaleTriggerEvent payload, Map<Object, Object> variables) throws Exception {
                stackUpscaleService.startAddInstances(context.getStack(), payload.getAdjustment());
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(StackScalingFlowContext context) {
                LOGGER.debug("Assembling upscale stack event for stack: {}", context.getStack());
                InstanceGroup group = context.getStack().getInstanceGroupByInstanceGroupName(context.getInstanceGroupName());
                group.setNodeCount(group.getNodeCount() + context.getAdjustment());
                CloudStack cloudStack = cloudStackConverter.convert(context.getStack());
                instanceMetadataService.saveInstanceRequests(context.getStack(), cloudStack.getGroups());
                List<CloudResource> resources = cloudResourceConverter.convert(context.getStack().getResources());
                return new UpscaleStackRequest<UpscaleStackResult>(context.getCloudContext(), context.getCloudCredential(), cloudStack, resources);
            }
        };
    }

    @Bean(name = "ADD_INSTANCES_FINISHED_STATE")
    public Action finishAddInstances() {
        return new AbstractStackUpscaleAction<UpscaleStackResult>(UpscaleStackResult.class) {
            @Override
            protected void doExecute(StackScalingFlowContext context, UpscaleStackResult payload, Map<Object, Object> variables) throws Exception {
                stackUpscaleService.finishAddInstances(context, payload);
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(StackScalingFlowContext context) {
                return new StackEvent(StackUpscaleEvent.EXTEND_METADATA_EVENT.event(), context.getStack().getId());
            }
        };
    }

    @Bean(name = "EXTEND_METADATA_STATE")
    public Action extendMetadata() {
        return new AbstractStackUpscaleAction<StackEvent>(StackEvent.class) {
            @Override
            protected void doExecute(StackScalingFlowContext context, StackEvent payload, Map<Object, Object> variables) throws Exception {
                stackUpscaleService.extendingMetadata(context.getStack());
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(StackScalingFlowContext context) {
                List<CloudResource> cloudResources = cloudResourceConverter.convert(context.getStack().getResources());
                List<CloudInstance> cloudInstances = stackUpscaleService.getNewInstances(context.getStack());
                return new CollectMetadataRequest(context.getCloudContext(), context.getCloudCredential(), cloudResources, cloudInstances);
            }
        };
    }

    @Bean(name = "EXTEND_METADATA_FINISHED_STATE")
    public Action finishExtendMetadata() {
        return new AbstractStackUpscaleAction<CollectMetadataResult>(CollectMetadataResult.class) {
            @Override
            protected void doExecute(StackScalingFlowContext context, CollectMetadataResult payload, Map<Object, Object> variables) throws Exception {
                Set<String> upscaleCandidateAddresses = stackUpscaleService.finishExtendMetadata(context.getStack(), context.getInstanceGroupName(), payload);
                BootstrapNewNodesEvent bootstrapPayload = new BootstrapNewNodesEvent(context.getStack().getId(), upscaleCandidateAddresses);
                sendEvent(context.getFlowId(), StackUpscaleEvent.BOOTSTRAP_NEW_NODES_EVENT.event(), bootstrapPayload);
            }
        };
    }

    @Bean(name = "BOOTSTRAP_NEW_NODES_STATE")
    public Action bootstrapNewNodes() {
        return new AbstractStackUpscaleAction<BootstrapNewNodesEvent>(BootstrapNewNodesEvent.class) {
            @Override
            protected void doExecute(StackScalingFlowContext context, BootstrapNewNodesEvent payload, Map<Object, Object> variables) throws Exception {
                stackUpscaleService.bootstrappingNewNodes(context.getStack());
                BootstrapNewNodesRequest request = new BootstrapNewNodesRequest(context.getStack().getId(), payload.getUpscaleCandidateAddresses());
                sendEvent(context.getFlowId(), request);
            }
        };
    }

    @Bean(name = "EXTEND_HOST_METADATA_STATE")
    public Action extendHostMetadata() {
        return new AbstractStackUpscaleAction<BootstrapNewNodesResult>(BootstrapNewNodesResult.class) {
            @Override
            protected void doExecute(StackScalingFlowContext context, BootstrapNewNodesResult payload, Map<Object, Object> variables) throws Exception {
                stackUpscaleService.extendingHostMetadata(context.getStack());
                ExtendHostMetadataRequest request = new ExtendHostMetadataRequest(context.getStack().getId(),
                        payload.getRequest().getUpscaleCandidateAddresses());
                sendEvent(context.getFlowId(), request);
            }
        };
    }

    @Bean(name = "EXTEND_HOST_METADATA_FINISHED_STATE")
    public Action finishExtendHostMetadata() {
        return new AbstractStackUpscaleAction<ExtendHostMetadataResult>(ExtendHostMetadataResult.class) {
            @Override
            protected void doExecute(StackScalingFlowContext context, ExtendHostMetadataResult payload, Map<Object, Object> variables) throws Exception {
                stackUpscaleService.finishExtendHostMetadata(context.getStack());
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(StackScalingFlowContext context) {
                return new StackEvent(StackUpscaleEvent.UPSCALE_FINALIZED_EVENT.event(), context.getStack().getId());
            }
        };
    }

    @Bean(name = "UPSCALE_FAILED_STATE")
    public Action stackStartFailedAction() {
        return new AbstractStackFailureAction<StackUpscaleState, StackUpscaleEvent>() {
            @Override
            protected void doExecute(StackFailureContext context, StackFailureEvent payload, Map<Object, Object> variables) throws Exception {
                stackUpscaleService.handleStackUpscaleFailure(context.getStack(), payload);
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(StackFailureContext context) {
                return new StackEvent(StackUpscaleEvent.UPSCALE_FAIL_HANDLED_EVENT.event(), context.getStack().getId());
            }
        };
    }
}
