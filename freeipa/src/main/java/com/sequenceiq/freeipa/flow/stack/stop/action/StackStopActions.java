package com.sequenceiq.freeipa.flow.stack.stop.action;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.action.Action;

import com.sequenceiq.cloudbreak.api.util.ConverterUtil;
import com.sequenceiq.cloudbreak.cloud.event.instance.StopInstancesRequest;
import com.sequenceiq.cloudbreak.cloud.event.instance.StopInstancesResult;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.freeipa.converter.cloud.ResourceToCloudResourceConverter;
import com.sequenceiq.freeipa.entity.Resource;
import com.sequenceiq.freeipa.flow.stack.AbstractStackFailureAction;
import com.sequenceiq.freeipa.flow.stack.StackEvent;
import com.sequenceiq.freeipa.flow.stack.StackFailureContext;
import com.sequenceiq.freeipa.flow.stack.StackFailureEvent;
import com.sequenceiq.freeipa.flow.stack.stop.StackStopContext;
import com.sequenceiq.freeipa.flow.stack.stop.StackStopEvent;
import com.sequenceiq.freeipa.flow.stack.stop.StackStopService;
import com.sequenceiq.freeipa.flow.stack.stop.StackStopState;
import com.sequenceiq.freeipa.service.resource.ResourceService;

@Configuration
public class StackStopActions {
    private static final Logger LOGGER = LoggerFactory.getLogger(StackStopActions.class);

    @Inject
    private ConverterUtil converterUtil;

    @Inject
    private StackStopService stackStopService;

    @Inject
    private ResourceService resourceService;

    @Inject
    private ResourceToCloudResourceConverter resourceToCloudResourceConverter;

    @Bean(name = "STOP_STATE")
    public Action<?, ?> stackStopAction() {
        return new AbstractStackStopAction<>(StackEvent.class) {
            @Override
            protected void doExecute(StackStopContext context, StackEvent payload, Map<Object, Object> variables) {
                stackStopService.startStackStop(context);
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(StackStopContext context) {
                List<CloudInstance> cloudInstances = converterUtil.convertAll(context.getInstanceMetaData(), CloudInstance.class);
                List<CloudResource> cloudResources = getCloudResources(context.getStack().getId());
                return new StopInstancesRequest<StopInstancesResult>(context.getCloudContext(), context.getCloudCredential(), cloudResources, cloudInstances);
            }
        };
    }

    @Bean(name = "STOP_FINISHED_STATE")
    public Action<?, ?> stackStopFinishedAction() {
        return new AbstractStackStopAction<>(StopInstancesResult.class) {
            @Override
            protected void doExecute(StackStopContext context, StopInstancesResult payload, Map<Object, Object> variables) {
                stackStopService.finishStackStop(context, payload);
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(StackStopContext context) {
                return new StackEvent(StackStopEvent.STOP_FINALIZED_EVENT.event(), context.getStack().getId());
            }
        };
    }

    @Bean(name = "STOP_FAILED_STATE")
    public Action<?, ?> stackStopFailedAction() {
        return new AbstractStackFailureAction<StackStopState, StackStopEvent>() {
            @Override
            protected void doExecute(StackFailureContext context, StackFailureEvent payload, Map<Object, Object> variables) {
                stackStopService.handleStackStopError(context.getStack(), payload);
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(StackFailureContext context) {
                return new StackEvent(StackStopEvent.STOP_FAIL_HANDLED_EVENT.event(), context.getStack().getId());
            }
        };
    }

    private List<CloudResource> getCloudResources(Long stackId) {
        List<Resource> resources = resourceService.findAllByStackId(stackId);
        return resourceToCloudResourceConverter.convert(resources);
    }
}
