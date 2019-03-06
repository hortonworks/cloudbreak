package com.sequenceiq.cloudbreak.core.flow2.stack.downscale;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.action.Action;

import com.sequenceiq.cloudbreak.cloud.event.Selectable;
import com.sequenceiq.cloudbreak.cloud.event.resource.DownscaleStackCollectResourcesRequest;
import com.sequenceiq.cloudbreak.cloud.event.resource.DownscaleStackCollectResourcesResult;
import com.sequenceiq.cloudbreak.cloud.event.resource.DownscaleStackRequest;
import com.sequenceiq.cloudbreak.cloud.event.resource.DownscaleStackResult;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.converter.spi.InstanceMetaDataToCloudInstanceConverter;
import com.sequenceiq.cloudbreak.converter.spi.ResourceToCloudResourceConverter;
import com.sequenceiq.cloudbreak.core.flow2.event.StackDownscaleTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.stack.AbstractStackFailureAction;
import com.sequenceiq.cloudbreak.core.flow2.stack.StackFailureContext;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;
import com.sequenceiq.cloudbreak.service.TransactionService.TransactionExecutionException;

@Configuration
public class StackDownscaleActions {
    private static final Logger LOGGER = LoggerFactory.getLogger(StackDownscaleActions.class);

    private static final String RESOURCES = "RESOURCES";

    private static final String INSTANCES = "INSTANCES";

    @Inject
    private InstanceMetaDataToCloudInstanceConverter metadataConverter;

    @Inject
    private ResourceToCloudResourceConverter cloudResourceConverter;

    @Inject
    private StackDownscaleService stackDownscaleService;

    @Bean(name = "DOWNSCALE_COLLECT_RESOURCES_STATE")
    public Action<?, ?> stackDownscaleCollectResourcesAction() {
        return new AbstractStackDownscaleAction<>(StackDownscaleTriggerEvent.class) {
            @Override
            protected void doExecute(StackScalingFlowContext context, StackDownscaleTriggerEvent payload, Map<Object, Object> variables) {
                stackDownscaleService.startStackDownscale(context, payload);
                Stack stack = context.getStack();
                LOGGER.debug("Assembling downscale stack event for stack: {}", stack);
                List<CloudResource> resources = cloudResourceConverter.convert(stack.getResources());
                variables.put(RESOURCES, resources);
                List<CloudInstance> instances = new ArrayList<>();
                InstanceGroup group = stack.getInstanceGroupByInstanceGroupName(context.getInstanceGroupName());
                for (InstanceMetaData metaData : group.getAllInstanceMetaData()) {
                    if (context.getInstanceIds().contains(metaData.getInstanceId())) {
                        CloudInstance cloudInstance = metadataConverter.convert(metaData);
                        instances.add(cloudInstance);
                    }
                }
                variables.put(INSTANCES, instances);
                Selectable request = new DownscaleStackCollectResourcesRequest(context.getCloudContext(),
                        context.getCloudCredential(), context.getCloudStack(), resources, instances);
                sendEvent(context.getFlowId(), request);
            }
        };
    }

    @Bean(name = "DOWNSCALE_STATE")
    public Action<?, ?> stackDownscaleAction() {
        return new AbstractStackDownscaleAction<>(DownscaleStackCollectResourcesResult.class) {
            @Override
            protected void doExecute(StackScalingFlowContext context, DownscaleStackCollectResourcesResult payload, Map<Object, Object> variables) {
                Selectable request = new DownscaleStackRequest(context.getCloudContext(), context.getCloudCredential(), context.getCloudStack(),
                        (List<CloudResource>) variables.get(RESOURCES), (List<CloudInstance>) variables.get(INSTANCES), payload.getResourcesToScale());
                sendEvent(context.getFlowId(), request);
            }
        };
    }

    @Bean(name = "DOWNSCALE_FINISHED_STATE")
    public Action<?, ?> stackDownscaleFinishedAction() {
        return new AbstractStackDownscaleAction<>(DownscaleStackResult.class) {
            @Override
            protected void doExecute(StackScalingFlowContext context, DownscaleStackResult payload, Map<Object, Object> variables)
                    throws TransactionExecutionException {
                stackDownscaleService.finishStackDownscale(context, getInstanceGroupName(variables), getInstanceIds(variables));
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(StackScalingFlowContext context) {
                return new StackEvent(StackDownscaleEvent.DOWNSCALE_FINALIZED_EVENT.event(), context.getStack().getId());
            }
        };
    }

    @Bean(name = "DOWNSCALE_FAILED_STATE")
    public Action<?, ?> stackDownscaleFailedAction() {
        return new AbstractStackFailureAction<StackDownscaleState, StackDownscaleEvent>() {
            @Override
            protected void doExecute(StackFailureContext context, StackFailureEvent payload, Map<Object, Object> variables) {
                stackDownscaleService.handleStackDownscaleError(context, payload.getException());
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(StackFailureContext context) {
                return new StackEvent(StackDownscaleEvent.DOWNSCALE_FAIL_HANDLED_EVENT.event(), context.getStackView().getId());
            }
        };
    }
}
