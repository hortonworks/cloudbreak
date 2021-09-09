package com.sequenceiq.freeipa.flow.stack.start.action;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.action.Action;

import com.sequenceiq.cloudbreak.cloud.event.instance.CollectMetadataRequest;
import com.sequenceiq.cloudbreak.cloud.event.instance.CollectMetadataResult;
import com.sequenceiq.cloudbreak.cloud.event.instance.StartInstancesRequest;
import com.sequenceiq.cloudbreak.cloud.event.instance.StartInstancesResult;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.freeipa.converter.cloud.InstanceMetaDataToCloudInstanceConverter;
import com.sequenceiq.freeipa.converter.cloud.ResourceToCloudResourceConverter;
import com.sequenceiq.freeipa.converter.cloud.StackToCloudStackConverter;
import com.sequenceiq.freeipa.entity.Resource;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.stack.AbstractStackFailureAction;
import com.sequenceiq.freeipa.flow.stack.StackEvent;
import com.sequenceiq.freeipa.flow.stack.StackFailureContext;
import com.sequenceiq.freeipa.flow.stack.StackFailureEvent;
import com.sequenceiq.freeipa.flow.stack.start.StackStartContext;
import com.sequenceiq.freeipa.flow.stack.start.StackStartEvent;
import com.sequenceiq.freeipa.flow.stack.start.StackStartService;
import com.sequenceiq.freeipa.flow.stack.start.StackStartState;
import com.sequenceiq.freeipa.flow.stack.HealthCheckRequest;
import com.sequenceiq.freeipa.flow.stack.HealthCheckSuccess;
import com.sequenceiq.freeipa.service.resource.ResourceService;

@Configuration
public class StackStartActions {
    private static final Logger LOGGER = LoggerFactory.getLogger(StackStartActions.class);

    @Inject
    private StackStartService stackStartService;

    @Inject
    private InstanceMetaDataToCloudInstanceConverter metadataConverter;

    @Inject
    private StackToCloudStackConverter cloudStackConverter;

    @Inject
    private ResourceService resourceService;

    @Inject
    private ResourceToCloudResourceConverter resourceToCloudResourceConverter;

    @Bean(name = "START_STATE")
    public Action<?, ?> stackStartAction() {
        return new AbstractStackStartAction<>(StackEvent.class) {
            @Override
            protected void doExecute(StackStartContext context, StackEvent payload, Map<Object, Object> variables) {
                stackStartService.startStack(context.getStack());
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(StackStartContext context) {
                Stack stack = context.getStack();
                LOGGER.debug("Assembling start request for stack: {}", stack);
                List<CloudInstance> cloudInstances = stack.getNotDeletedInstanceMetaDataSet().stream()
                        .map(i -> metadataConverter.convert(i))
                        .collect(Collectors.toList());
                List<CloudResource> cloudResources = getCloudResources(stack.getId());
                return new StartInstancesRequest(context.getCloudContext(), context.getCloudCredential(), cloudResources, cloudInstances);
            }
        };
    }

    @Bean(name = "COLLECTING_METADATA")
    public Action<?, ?> collectingMetadataAction() {
        return new AbstractStackStartAction<>(StartInstancesResult.class) {
            @Override
            protected void doExecute(StackStartContext context, StartInstancesResult payload, Map<Object, Object> variables) {
                stackStartService.validateStackStartResult(context, payload);
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(StackStartContext context) {
                List<CloudInstance> cloudInstances = cloudStackConverter.buildInstances(context.getStack());
                List<CloudResource> cloudResources = getCloudResources(context.getStack().getId());
                return new CollectMetadataRequest(context.getCloudContext(), context.getCloudCredential(), cloudResources, cloudInstances, cloudInstances);
            }
        };
    }

    @Bean(name = "START_SAVE_METADATA_STATE")
    public Action<?, ?> startSaveMetadataAction() {
        return new AbstractStackStartAction<>(CollectMetadataResult.class) {
            @Override
            protected void doExecute(StackStartContext context, CollectMetadataResult payload, Map<Object, Object> variables) {
                stackStartService.saveMetadata(context, payload.getResults());
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(StackStartContext context) {
                return new StackEvent(StackStartEvent.START_SAVE_METADATA_FINISHED_EVENT.event(), context.getStack().getId());
            }
        };
    }

    @Bean(name = "START_WAIT_UNTIL_AVAILABLE_STATE")
    public Action<?, ?> startWaitUntilAvailableAction() {
        return new AbstractStackStartAction<>(StackEvent.class) {
            @Override
            protected void doExecute(StackStartContext context, StackEvent payload, Map<Object, Object> variables) {
                stackStartService.waitForAvailableStatus(context);
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(StackStartContext context) {
                return new HealthCheckRequest(context.getStack().getId(), true);
            }
        };
    }

    @Bean(name = "START_FINISHED_STATE")
    public Action<?, ?> startFinishedAction() {
        return new AbstractStackStartAction<>(HealthCheckSuccess.class) {
            @Override
            protected void doExecute(StackStartContext context, HealthCheckSuccess payload, Map<Object, Object> variables) {
                stackStartService.finishStackStart(context);
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(StackStartContext context) {
                return new StackEvent(StackStartEvent.START_FINALIZED_EVENT.event(), context.getStack().getId());
            }
        };
    }

    @Bean(name = "START_FAILED_STATE")
    public Action<?, ?> stackStartFailedAction() {
        return new AbstractStackFailureAction<StackStartState, StackStartEvent>() {
            @Override
            protected void doExecute(StackFailureContext context, StackFailureEvent payload, Map<Object, Object> variables) {
                stackStartService.handleStackStartError(context.getStack(), payload);
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(StackFailureContext context) {
                return new StackEvent(StackStartEvent.START_FAIL_HANDLED_EVENT.event(), context.getStack().getId());
            }
        };
    }

    private List<CloudResource> getCloudResources(Long stackId) {
        List<Resource> resources = resourceService.findAllByStackId(stackId);
        return resources.stream()
                .map(r -> resourceToCloudResourceConverter.convert(r))
                .collect(Collectors.toList());
    }
}
