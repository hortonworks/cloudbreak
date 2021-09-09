package com.sequenceiq.cloudbreak.core.flow2.stack.update.loadbalancer;

import static com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone.availabilityZone;
import static com.sequenceiq.cloudbreak.cloud.model.Location.location;
import static com.sequenceiq.cloudbreak.cloud.model.Region.region;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Location;
import com.sequenceiq.cloudbreak.common.event.Payload;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.converter.spi.ResourceToCloudResourceConverter;
import com.sequenceiq.cloudbreak.converter.spi.StackToCloudStackConverter;
import com.sequenceiq.cloudbreak.core.flow2.AbstractStackAction;
import com.sequenceiq.cloudbreak.core.flow2.stack.AbstractStackFailureAction;
import com.sequenceiq.cloudbreak.core.flow2.stack.StackContext;
import com.sequenceiq.cloudbreak.core.flow2.stack.StackFailureContext;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.loadbalancer.LoadBalancer;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.loadbalancer.CreateCloudLoadBalancersSuccess;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.loadbalancer.LoadBalancerMetadataRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.loadbalancer.CreateCloudLoadBalancersRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.loadbalancer.CreateLoadBalancerEntityRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.loadbalancer.CreateLoadBalancerEntitySuccess;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.loadbalancer.LoadBalancerMetadataSuccess;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.loadbalancer.RegisterFreeIpaDnsRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.loadbalancer.RegisterFreeIpaDnsSuccess;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.loadbalancer.RegisterPublicDnsRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.loadbalancer.RegisterPublicDnsSuccess;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.loadbalancer.RestartCmForLbRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.loadbalancer.RestartCmForLbSuccess;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.loadbalancer.UpdateServiceConfigRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.loadbalancer.UpdateServiceConfigSuccess;
import com.sequenceiq.cloudbreak.service.resource.ResourceService;
import com.sequenceiq.cloudbreak.service.stack.LoadBalancerPersistenceService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.util.StackUtil;
import com.sequenceiq.common.api.type.LoadBalancerType;
import com.sequenceiq.flow.core.FlowParameters;

@Configuration
public class StackLoadBalancerUpdateActions {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackLoadBalancerUpdateActions.class);

    @Inject
    private LoadBalancerPersistenceService loadBalancerPersistenceService;

    @Inject
    private StackLoadBalancerUpdateService stackLoadBalancerUpdateService;

    @Inject
    private ResourceToCloudResourceConverter cloudResourceConverter;

    @Bean(name = "CREATING_LOAD_BALANCER_ENTITY_STATE")
    public Action<?, ?> createLoadBalancerEntityAction() {
        return new AbstractStackLoadBalancerUpdateAction<>(StackEvent.class) {
            @Override
            protected void doExecute(StackContext context, StackEvent payload, Map<Object, Object> variables) {
                stackLoadBalancerUpdateService.creatingLoadBalancerEntity(context.getStack());
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(StackContext context) {
                return new CreateLoadBalancerEntityRequest(context.getStack());
            }
        };
    }

    @Bean(name = "CREATING_CLOUD_LOAD_BALANCERS_STATE")
    public Action<?, ?> createCloudLoadBalancersAction() {
        return new AbstractStackLoadBalancerUpdateAction<>(CreateLoadBalancerEntitySuccess.class) {
            @Override
            protected void doExecute(StackContext context, CreateLoadBalancerEntitySuccess payload, Map<Object, Object> variables) {
                stackLoadBalancerUpdateService.creatingCloudResources(context.getStack());
                StackContext newContext = new StackContext(context.getFlowParameters(), payload.getSavedStack(), context.getCloudContext(),
                    context.getCloudCredential(), context.getCloudStack());
                sendEvent(newContext);
            }

            @Override
            protected Selectable createRequest(StackContext context) {
                return new CreateCloudLoadBalancersRequest(context.getStack(), context.getCloudContext(), context.getCloudCredential(),
                    context.getCloudStack());
            }
        };
    }

    @Bean(name = "COLLECTING_LOAD_BALANCER_METADATA_STATE")
    public Action<?, ?> collectLoadBalancerMetadataAction() {
        return new AbstractStackLoadBalancerUpdateAction<>(CreateCloudLoadBalancersSuccess.class) {
            @Override
            protected void doExecute(StackContext context, CreateCloudLoadBalancersSuccess payload, Map<Object, Object> variables) {
                stackLoadBalancerUpdateService.collectingMetadata(context.getStack());
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(StackContext context) {
                List<LoadBalancerType> loadBalancerTypes = loadBalancerPersistenceService.findByStackId(context.getStack().getId()).stream()
                    .map(LoadBalancer::getType)
                    .collect(Collectors.toList());
                List<CloudResource> cloudResources = context.getStack().getResources().stream()
                        .map(r -> cloudResourceConverter.convert(r))
                        .collect(Collectors.toList());
                return new LoadBalancerMetadataRequest(context.getStack(), context.getCloudContext(), context.getCloudCredential(),
                    context.getCloudStack(), loadBalancerTypes, cloudResources);
            }
        };
    }

    @Bean(name = "REGISTERING_PUBLIC_DNS_STATE")
    public Action<?, ?> registerPublicDnsAction() {
        return new AbstractStackLoadBalancerUpdateAction<>(LoadBalancerMetadataSuccess.class) {
            @Override
            protected void doExecute(StackContext context, LoadBalancerMetadataSuccess payload, Map<Object, Object> variables) {
                stackLoadBalancerUpdateService.registeringPublicDns(context.getStack());
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(StackContext context) {
                return new RegisterPublicDnsRequest(context.getStack());
            }
        };
    }

    @Bean(name = "REGISTERING_FREEIPA_DNS_STATE")
    public Action<?, ?> registerFreeIpaDnsAction() {
        return new AbstractStackLoadBalancerUpdateAction<>(RegisterPublicDnsSuccess.class) {
            @Override
            protected void doExecute(StackContext context, RegisterPublicDnsSuccess payload, Map<Object, Object> variables) {
                stackLoadBalancerUpdateService.registeringFreeIpaDns(context.getStack());
                StackContext newContext = new StackContext(context.getFlowParameters(), payload.getStack(), context.getCloudContext(),
                    context.getCloudCredential(), context.getCloudStack());
                sendEvent(newContext);
            }

            @Override
            protected Selectable createRequest(StackContext context) {
                return new RegisterFreeIpaDnsRequest(context.getStack());
            }
        };
    }

    @Bean(name = "UPDATING_SERVICE_CONFIG_STATE")
    public Action<?, ?> updateServiceConfigAction() {
        return new AbstractStackLoadBalancerUpdateAction<>(RegisterFreeIpaDnsSuccess.class) {
            @Override
            protected void doExecute(StackContext context, RegisterFreeIpaDnsSuccess payload, Map<Object, Object> variables) {
                stackLoadBalancerUpdateService.updatingCmConfig(context.getStack());
                StackContext newContext = new StackContext(context.getFlowParameters(), payload.getStack(), context.getCloudContext(),
                    context.getCloudCredential(), context.getCloudStack());
                sendEvent(newContext);
            }

            @Override
            protected Selectable createRequest(StackContext context) {
                return new UpdateServiceConfigRequest(context.getStack());
            }
        };
    }

    @Bean(name = "RESTARTING_CM_STATE")
    public Action<?, ?> restartCmAction() {
        return new AbstractStackLoadBalancerUpdateAction<>(UpdateServiceConfigSuccess.class) {
            @Override
            protected void doExecute(StackContext context, UpdateServiceConfigSuccess payload, Map<Object, Object> variables) {
                stackLoadBalancerUpdateService.restartingCm(context.getStack());
                StackContext newContext = new StackContext(context.getFlowParameters(), payload.getStack(), context.getCloudContext(),
                    context.getCloudCredential(), context.getCloudStack());
                sendEvent(newContext);
            }

            @Override
            protected Selectable createRequest(StackContext context) {
                return new RestartCmForLbRequest(context.getStack());
            }
        };
    }

    @Bean(name = "LOAD_BALANCER_UPDATE_FINISHED_STATE")
    public Action<?, ?> loadBalancerUpdateFinishedAction() {
        return new AbstractStackLoadBalancerUpdateAction<>(RestartCmForLbSuccess.class) {
            @Override
            protected void doExecute(StackContext context, RestartCmForLbSuccess payload, Map<Object, Object> variables) {
                stackLoadBalancerUpdateService.updateFinished(context.getStack());
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(StackContext context) {
                return new StackEvent(StackLoadBalancerUpdateEvent.LOAD_BALANCER_UPDATE_FINISHED_EVENT.event(), context.getStack().getId());
            }
        };
    }

    @Bean(name = "LOAD_BALANCER_UPDATE_FAILED_STATE")
    public Action<?, ?> loadBalancerUpdateFailedAction() {
        return new AbstractStackFailureAction<StackLoadBalancerUpdateState, StackLoadBalancerUpdateEvent>() {
            @Override
            protected void doExecute(StackFailureContext context, StackFailureEvent payload, Map<Object, Object> variables) {
                stackLoadBalancerUpdateService.updateClusterFailed(context.getStackView().getId(), payload.getException());
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(StackFailureContext context) {
                return new StackEvent(StackLoadBalancerUpdateEvent.LOAD_BALANCER_UPDATE_FAIL_HANDLED_EVENT.event(), context.getStackView().getId());
            }
        };
    }

    private abstract static class AbstractStackLoadBalancerUpdateAction<P extends Payload>
        extends AbstractStackAction<StackLoadBalancerUpdateState, StackLoadBalancerUpdateEvent, StackContext, P> {
        private static final Logger LOGGER = LoggerFactory.getLogger(AbstractStackLoadBalancerUpdateAction.class);

        @Inject
        private StackService stackService;

        @Inject
        private StackUtil stackUtil;

        @Inject
        private ResourceService resourceService;

        @Inject
        private StackToCloudStackConverter cloudStackConverter;

        protected AbstractStackLoadBalancerUpdateAction(Class<P> payloadClass) {
            super(payloadClass);
        }

        @Override
        protected StackContext createFlowContext(FlowParameters flowParameters, StateContext<StackLoadBalancerUpdateState,
            StackLoadBalancerUpdateEvent> stateContext, P payload) {
            Stack stack = stackService.getByIdWithListsInTransaction(payload.getResourceId());
            stack.setResources(new HashSet<>(resourceService.getAllByStackId(payload.getResourceId())));
            MDCBuilder.buildMdcContext(stack);
            Location location = location(region(stack.getRegion()), availabilityZone(stack.getAvailabilityZone()));
            CloudContext cloudContext = CloudContext.Builder.builder()
                .withId(stack.getId())
                .withName(stack.getName())
                .withCrn(stack.getResourceCrn())
                .withPlatform(stack.getCloudPlatform())
                .withVariant(stack.getPlatformVariant())
                .withLocation(location)
                .withUserName(stack.getCreator().getUserName())
                .withAccountId(stack.getWorkspace().getId())
                .build();
            CloudCredential cloudCredential = stackUtil.getCloudCredential(stack);
            CloudStack cloudStack = cloudStackConverter.convert(stack);
            return new StackContext(flowParameters, stack, cloudContext, cloudCredential, cloudStack);
        }

        @Override
        protected Object getFailurePayload(P payload, Optional<StackContext> flowContext, Exception ex) {
            return new StackFailureEvent(payload.getResourceId(), ex);
        }
    }
}
