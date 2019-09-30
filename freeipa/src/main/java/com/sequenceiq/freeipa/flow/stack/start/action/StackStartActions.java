package com.sequenceiq.freeipa.flow.stack.start.action;

import static java.util.Collections.emptyList;

import java.util.List;
import java.util.Map;

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
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.freeipa.converter.cloud.InstanceMetaDataToCloudInstanceConverter;
import com.sequenceiq.freeipa.converter.cloud.StackToCloudStackConverter;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.stack.AbstractStackFailureAction;
import com.sequenceiq.freeipa.flow.stack.StackEvent;
import com.sequenceiq.freeipa.flow.stack.StackFailureContext;
import com.sequenceiq.freeipa.flow.stack.StackFailureEvent;
import com.sequenceiq.freeipa.flow.stack.start.StackStartContext;
import com.sequenceiq.freeipa.flow.stack.start.StackStartEvent;
import com.sequenceiq.freeipa.flow.stack.start.StackStartService;
import com.sequenceiq.freeipa.flow.stack.start.StackStartState;

@Configuration
public class StackStartActions {
    private static final Logger LOGGER = LoggerFactory.getLogger(StackStartActions.class);

    @Inject
    private StackStartService stackStartService;

    @Inject
    private InstanceMetaDataToCloudInstanceConverter metadataConverter;

    @Inject
    private StackToCloudStackConverter cloudStackConverter;

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
                List<CloudInstance> cloudInstances = metadataConverter.convert(stack.getNotDeletedInstanceMetaDataSet());
                return new StartInstancesRequest(context.getCloudContext(), context.getCloudCredential(), emptyList(), cloudInstances);
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
                return new CollectMetadataRequest(context.getCloudContext(), context.getCloudCredential(), emptyList(), cloudInstances, cloudInstances);
            }
        };
    }

    @Bean(name = "START_FINISHED_STATE")
    public Action<?, ?> startFinishedAction() {
        return new AbstractStackStartAction<>(CollectMetadataResult.class) {
            @Override
            protected void doExecute(StackStartContext context, CollectMetadataResult payload, Map<Object, Object> variables) {
                stackStartService.finishStackStart(context, payload.getResults());
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

}
