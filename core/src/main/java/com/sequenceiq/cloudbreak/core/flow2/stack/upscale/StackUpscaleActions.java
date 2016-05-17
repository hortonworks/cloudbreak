package com.sequenceiq.cloudbreak.core.flow2.stack.upscale;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.action.Action;

import com.sequenceiq.cloudbreak.api.model.HostGroupAdjustmentJson;
import com.sequenceiq.cloudbreak.api.model.Status;
import com.sequenceiq.cloudbreak.cloud.event.Selectable;
import com.sequenceiq.cloudbreak.cloud.event.instance.CollectMetadataRequest;
import com.sequenceiq.cloudbreak.cloud.event.instance.CollectMetadataResult;
import com.sequenceiq.cloudbreak.cloud.event.resource.UpscaleStackRequest;
import com.sequenceiq.cloudbreak.cloud.event.resource.UpscaleStackResult;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.common.type.ScalingType;
import com.sequenceiq.cloudbreak.converter.spi.CloudResourceToResourceConverter;
import com.sequenceiq.cloudbreak.converter.spi.ResourceToCloudResourceConverter;
import com.sequenceiq.cloudbreak.converter.spi.StackToCloudStackConverter;
import com.sequenceiq.cloudbreak.core.flow.FlowPhases;
import com.sequenceiq.cloudbreak.core.flow.context.ClusterScalingContext;
import com.sequenceiq.cloudbreak.core.flow.context.StackScalingContext;
import com.sequenceiq.cloudbreak.core.flow2.stack.AbstractStackFailureAction;
import com.sequenceiq.cloudbreak.core.flow2.stack.FlowFailureEvent;
import com.sequenceiq.cloudbreak.core.flow2.stack.FlowMessageService;
import com.sequenceiq.cloudbreak.core.flow2.stack.Msg;
import com.sequenceiq.cloudbreak.core.flow2.stack.SelectableFlowStackEvent;
import com.sequenceiq.cloudbreak.core.flow2.stack.StackFailureContext;
import com.sequenceiq.cloudbreak.core.flow2.stack.downscale.StackScalingFlowContext;
import com.sequenceiq.cloudbreak.domain.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.BootstrapNewNodesRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.BootstrapNewNodesResult;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.ExtendConsulMetadataRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.ExtendConsulMetadataResult;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
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
    private ClusterService clusterService;
    @Inject
    private StackToCloudStackConverter cloudStackConverter;
    @Inject
    private FlowMessageService flowMessageService;

    @Bean(name = "ADD_INSTANCES_STATE")
    public Action addInstances() {
        return new AbstractStackUpscaleAction<StackScalingContext>(StackScalingContext.class) {
            @Override
            protected void doExecute(StackScalingFlowContext context, StackScalingContext payload, Map<Object, Object> variables) throws Exception {
                stackUpscaleService.startAddInstances(context, payload);
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
                StackScalingContext nextPayload = stackUpscaleService.finishAddInstances(context, payload);
                StackScalingContext next = new StackScalingContext(nextPayload.getStackId(), nextPayload.getCloudPlatform(),
                        nextPayload.getScalingAdjustment(), nextPayload.getInstanceGroup(), null, nextPayload.getScalingType(),
                        nextPayload.getUpscaleCandidateAddresses());
                sendEvent(context.getFlowId(), FlowPhases.EXTEND_METADATA.name(), next);
            }

            @Override
            protected Selectable createRequest(StackScalingFlowContext context) {
                return null;
            }
        };
    }

    @Bean(name = "EXTEND_METADATA_STATE")
    public Action extendMetadata() {
        return new AbstractStackUpscaleAction<StackScalingContext>(StackScalingContext.class) {
            @Override
            protected void doExecute(StackScalingFlowContext context, StackScalingContext payload, Map<Object, Object> variables) throws Exception {
                clusterService.updateClusterStatusByStackId(context.getStack().getId(), Status.UPDATE_IN_PROGRESS);
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
                Set<String> upscaleCandidateAddresses = stackUpscaleService.finishExtendMetadata(context, payload);
                Set<Resource> resources = new HashSet<>(resourceConverter.convert(payload.getRequest().getCloudResource()));
                StackScalingContext nextPayload = new StackScalingContext(context.getStack().getId(), Platform.platform(context.getStack().cloudPlatform()),
                        context.getAdjustment(), context.getInstanceGroupName(),
                        resources, context.getScalingType(), upscaleCandidateAddresses);
                sendEvent(context.getFlowId(), FlowPhases.BOOTSTRAP_NEW_NODES.name(), nextPayload);
            }

            @Override
            protected Selectable createRequest(StackScalingFlowContext context) {
                return null;
            }
        };
    }

    @Bean(name = "BOOTSTRAP_NEW_NODES_STATE")
    public Action bootstrapNewNodes() {
        return new AbstractStackUpscaleAction<StackScalingContext>(StackScalingContext.class) {
            @Override
            protected void doExecute(StackScalingFlowContext context, StackScalingContext payload, Map<Object, Object> variables) throws Exception {
                flowMessageService.fireEventAndLog(context.getStack().getId(), Msg.STACK_BOOTSTRAP_NEW_NODES, Status.UPDATE_IN_PROGRESS.name());
                MDCBuilder.buildMdcContext(context.getStack());
                BootstrapNewNodesRequest request = new BootstrapNewNodesRequest(context.getStack().getId(), payload.getUpscaleCandidateAddresses());
                sendEvent(context.getFlowId(), request);
            }

            @Override
            protected Selectable createRequest(StackScalingFlowContext context) {
                return null;
            }
        };
    }

    @Bean(name = "EXTEND_CONSUL_METADATA_STATE")
    public Action extendConsulMetadata() {
        return new AbstractStackUpscaleAction<BootstrapNewNodesResult>(BootstrapNewNodesResult.class) {
            @Override
            protected void doExecute(StackScalingFlowContext context, BootstrapNewNodesResult payload, Map<Object, Object> variables) throws Exception {
                MDCBuilder.buildMdcContext(context.getStack());
                ExtendConsulMetadataRequest request = new ExtendConsulMetadataRequest(context.getStack().getId(),
                        payload.getRequest().getUpscaleCandidateAddresses());
                sendEvent(context.getFlowId(), request);
            }

            @Override
            protected Selectable createRequest(StackScalingFlowContext context) {
                return null;
            }
        };
    }

    @Bean(name = "EXTEND_CONSUL_METADATA_FINISHED_STATE")
    public Action finishExtendConsulMetadata() {
        return new AbstractStackUpscaleAction<ExtendConsulMetadataResult>(ExtendConsulMetadataResult.class) {
            @Override
            protected void doExecute(StackScalingFlowContext context, ExtendConsulMetadataResult payload, Map<Object, Object> variables) throws Exception {
                HostGroupAdjustmentJson hostGroupAdjustmentJson = stackUpscaleService.finishExtendConsulMetadata(context);
                ClusterScalingContext request = new ClusterScalingContext(context.getStack().getId(), Platform.platform(context.getStack().cloudPlatform()),
                        hostGroupAdjustmentJson, context.getScalingType());
                sendEvent(context);
                if (ScalingType.isClusterUpScale(context.getScalingType())) {
                    sendEvent(null, FlowPhases.ADD_CLUSTER_CONTAINERS.name(), request);
                }
            }

            @Override
            protected Selectable createRequest(StackScalingFlowContext context) {
                return new SelectableFlowStackEvent(context.getStack().getId(), StackUpscaleEvent.UPSCALE_FINALIZED_EVENT.stringRepresentation());
            }
        };
    }

    @Bean(name = "UPSCALE_FAILED_STATE")
    public Action stackStartFailedAction() {
        return new AbstractStackFailureAction<StackUpscaleState, StackUpscaleEvent>() {
            @Override
            protected void doExecute(StackFailureContext context, FlowFailureEvent payload, Map<Object, Object> variables) throws Exception {
                stackUpscaleService.handleStackUpscaleFailure(context, payload);
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(StackFailureContext context) {
                return new SelectableFlowStackEvent(context.getStack().getId(), StackUpscaleEvent.UPSCALE_FAIL_HANDLED_EVENT.stringRepresentation());
            }
        };
    }

}
