package com.sequenceiq.cloudbreak.core.flow2.stack.provision.action;

import static com.sequenceiq.cloudbreak.core.flow2.stack.provision.StackProvisionConstants.START_DATE;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.action.Action;

import com.sequenceiq.cloudbreak.cloud.event.instance.CollectMetadataRequest;
import com.sequenceiq.cloudbreak.cloud.event.instance.CollectMetadataResult;
import com.sequenceiq.cloudbreak.cloud.event.instance.GetSSHFingerprintsRequest;
import com.sequenceiq.cloudbreak.cloud.event.instance.GetSSHFingerprintsResult;
import com.sequenceiq.cloudbreak.cloud.event.instance.GetTlsInfoRequest;
import com.sequenceiq.cloudbreak.cloud.event.instance.GetTlsInfoResult;
import com.sequenceiq.cloudbreak.cloud.event.loadbalancer.CollectLoadBalancerMetadataRequest;
import com.sequenceiq.cloudbreak.cloud.event.loadbalancer.CollectLoadBalancerMetadataResult;
import com.sequenceiq.cloudbreak.cloud.event.resource.CreateCredentialRequest;
import com.sequenceiq.cloudbreak.cloud.event.resource.CreateCredentialResult;
import com.sequenceiq.cloudbreak.cloud.event.resource.LaunchLoadBalancerRequest;
import com.sequenceiq.cloudbreak.cloud.event.resource.LaunchLoadBalancerResult;
import com.sequenceiq.cloudbreak.cloud.event.resource.LaunchStackRequest;
import com.sequenceiq.cloudbreak.cloud.event.resource.LaunchStackResult;
import com.sequenceiq.cloudbreak.cloud.event.setup.PrepareImageRequest;
import com.sequenceiq.cloudbreak.cloud.event.setup.SetupRequest;
import com.sequenceiq.cloudbreak.cloud.event.setup.SetupResult;
import com.sequenceiq.cloudbreak.cloud.event.setup.ValidationRequest;
import com.sequenceiq.cloudbreak.cloud.event.setup.ValidationResult;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.cloud.model.TlsInfo;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.converter.spi.InstanceMetaDataToCloudInstanceConverter;
import com.sequenceiq.cloudbreak.converter.spi.ResourceToCloudResourceConverter;
import com.sequenceiq.cloudbreak.converter.spi.StackToCloudStackConverter;
import com.sequenceiq.cloudbreak.core.CloudbreakImageNotFoundException;
import com.sequenceiq.cloudbreak.core.flow2.stack.AbstractStackFailureAction;
import com.sequenceiq.cloudbreak.core.flow2.stack.StackFailureContext;
import com.sequenceiq.cloudbreak.core.flow2.stack.provision.StackCreationEvent;
import com.sequenceiq.cloudbreak.core.flow2.stack.provision.StackCreationState;
import com.sequenceiq.cloudbreak.core.flow2.stack.provision.service.StackCreationService;
import com.sequenceiq.cloudbreak.core.flow2.stack.start.StackCreationContext;
import com.sequenceiq.cloudbreak.domain.FailurePolicy;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.stack.loadbalancer.LoadBalancer;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.ProvisionEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.StackWithFingerprintsEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.userdata.CreateUserDataRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.userdata.CreateUserDataSuccess;
import com.sequenceiq.cloudbreak.service.environment.EnvironmentClientService;
import com.sequenceiq.cloudbreak.service.image.ImageService;
import com.sequenceiq.cloudbreak.service.metrics.MetricType;
import com.sequenceiq.cloudbreak.service.stack.LoadBalancerPersistenceService;
import com.sequenceiq.common.api.type.LoadBalancerType;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;

@Configuration
public class StackCreationActions {

    @Inject
    private ImageService imageService;

    @Inject
    private StackToCloudStackConverter cloudStackConverter;

    @Inject
    private StackCreationService stackCreationService;

    @Inject
    private ResourceToCloudResourceConverter cloudResourceConverter;

    @Inject
    private InstanceMetaDataToCloudInstanceConverter metadataConverter;

    @Inject
    private LoadBalancerPersistenceService loadBalancerPersistenceService;

    @Inject
    private EnvironmentClientService environmentClientService;

    @Bean(name = "VALIDATION_STATE")
    public Action<?, ?> provisioningValidationAction() {
        return new AbstractStackCreationAction<>(ProvisionEvent.class) {

            @Override
            protected void prepareExecution(ProvisionEvent payload, Map<Object, Object> variables) {
                super.prepareExecution(payload, variables);
                variables.put(PROVISION_TYPE, payload.getProvisionType());
            }

            @Override
            protected void doExecute(StackCreationContext context, ProvisionEvent payload, Map<Object, Object> variables) {
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(StackCreationContext context) {
                return new ValidationRequest(context.getCloudContext(), context.getCloudCredential(), context.getCloudStack());
            }
        };
    }

    @Bean(name = "CREATE_USER_DATA_STATE")
    public Action<?, ?> createUserDataAction() {
        return new AbstractStackCreationAction<>(ValidationResult.class) {
            @Override
            protected void doExecute(StackCreationContext context, ValidationResult payload, Map<Object, Object> variables) {
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(StackCreationContext context) {
                return new CreateUserDataRequest(context.getStack().getId());
            }
        };
    }

    @Bean(name = "SETUP_STATE")
    public Action<?, ?> provisioningSetupAction() {
        return new AbstractStackCreationAction<>(CreateUserDataSuccess.class) {
            @Override
            protected void doExecute(StackCreationContext context, CreateUserDataSuccess payload, Map<Object, Object> variables) {
                stackCreationService.setupProvision(context.getStack());
                stackCreationService.setInstanceStoreCount(context);
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(StackCreationContext context) {
                return new SetupRequest<SetupResult>(context.getCloudContext(), context.getCloudCredential(), context.getCloudStack());
            }
        };
    }

    @Bean(name = "IMAGESETUP_STATE")
    public Action<?, ?> prepareImageAction() {
        return new AbstractStackCreationAction<>(SetupResult.class) {
            @Override
            protected void doExecute(StackCreationContext context, SetupResult payload, Map<Object, Object> variables) {
                stackCreationService.prepareImage(context.getStack(), variables);
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(StackCreationContext context) {
                try {
                    CloudStack cloudStack = cloudStackConverter.convert(context.getStack());
                    Image image = imageService.getImage(context.getCloudContext().getId());
                    return new PrepareImageRequest<>(context.getCloudContext(), context.getCloudCredential(), cloudStack, image);
                } catch (CloudbreakImageNotFoundException e) {
                    throw new CloudbreakServiceException(e);
                }
            }
        };
    }

    @Bean(name = "CREATE_CREDENTIAL_STATE")
    public Action<?, ?> createCredentialAction() {
        return new AbstractStackCreationAction<>(StackEvent.class) {
            @Override
            protected void doExecute(StackCreationContext context, StackEvent payload, Map<Object, Object> variables) {
                variables.put(START_DATE, new Date());
                stackCreationService.startProvisioning(context, variables);
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(StackCreationContext context) {
                return new CreateCredentialRequest(context.getCloudContext(), context.getCloudCredential(), context.getCloudStack());
            }
        };
    }

    @Bean(name = "START_PROVISIONING_STATE")
    public Action<?, ?> startProvisioningAction() {
        return new AbstractStackCreationAction<>(CreateCredentialResult.class) {
            @Override
            protected void doExecute(StackCreationContext context, CreateCredentialResult payload, Map<Object, Object> variables) {
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(StackCreationContext context) {
                FailurePolicy policy = Optional.ofNullable(context.getStack().getFailurePolicy()).orElse(new FailurePolicy());
                return new LaunchStackRequest(context.getCloudContext(), context.getCloudCredential(), context.getCloudStack(),
                        policy.getAdjustmentType(), policy.getThreshold());
            }
        };
    }

    @Bean(name = "PROVISION_LOAD_BALANCER_STATE")
    public Action<?, ?> startProvisioningLoadBalancerAction() {
        return new AbstractStackCreationAction<>(LaunchStackResult.class) {
            @Override
            protected void doExecute(StackCreationContext context, LaunchStackResult payload, Map<Object, Object> variables) {
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(StackCreationContext context) {
                return new LaunchLoadBalancerRequest(context.getCloudContext(), context.getCloudCredential(), context.getCloudStack());
            }
        };
    }

    @Bean(name = "PROVISIONING_FINISHED_STATE")
    public Action<?, ?> provisioningFinishedAction() {
        return new AbstractStackCreationAction<>(LaunchLoadBalancerResult.class) {
            @Override
            protected void doExecute(StackCreationContext context, LaunchLoadBalancerResult payload, Map<Object, Object> variables) {
                Stack stack = stackCreationService.loadBalancerProvisioningFinished(context, payload, variables);
                StackCreationContext newContext = new StackCreationContext(context.getFlowParameters(), stack, context.getCloudContext(),
                        context.getCloudCredential(), context.getCloudStack());
                sendEvent(newContext);
            }

            @Override
            protected Selectable createRequest(StackCreationContext context) {
                List<CloudInstance> cloudInstances = cloudStackConverter.buildInstances(context.getStack());
                List<CloudResource> cloudResources = context.getStack().getResources().stream()
                        .map(r -> cloudResourceConverter.convert(r))
                        .collect(Collectors.toList());
                return new CollectMetadataRequest(context.getCloudContext(), context.getCloudCredential(), cloudResources, cloudInstances, cloudInstances);
            }
        };
    }

    @Bean(name = "COLLECTMETADATA_STATE")
    public Action<?, ?> collectMetadataAction() {
        return new AbstractStackCreationAction<>(CollectMetadataResult.class) {
            @Override
            protected void doExecute(StackCreationContext context, CollectMetadataResult payload, Map<Object, Object> variables) {
                Stack stack = stackCreationService.setupMetadata(context, payload);
                StackCreationContext newContext = new StackCreationContext(context.getFlowParameters(), stack, context.getCloudContext(),
                        context.getCloudCredential(),  context.getCloudStack());
                sendEvent(newContext);
            }

            @Override
            protected Selectable createRequest(StackCreationContext context) {
                List<LoadBalancerType> loadBalancerTypes = loadBalancerPersistenceService.findByStackId(context.getStack().getId()).stream()
                    .map(LoadBalancer::getType)
                    .collect(Collectors.toList());
                List<CloudResource> cloudResources = context.getStack().getResources().stream()
                        .map(r -> cloudResourceConverter.convert(r))
                        .collect(Collectors.toList());
                return new CollectLoadBalancerMetadataRequest(context.getCloudContext(), context.getCloudCredential(),
                    loadBalancerTypes, cloudResources);
            }
        };
    }

    @Bean(name = "COLLECTMETADATA_LOADBALANCER_STATE")
    public Action<?, ?> collectLoadBalancerMetadataAction() {
        return new AbstractStackCreationAction<>(CollectLoadBalancerMetadataResult.class) {
            @Override
            protected void doExecute(StackCreationContext context, CollectLoadBalancerMetadataResult payload, Map<Object, Object> variables) {
                Stack stack = stackCreationService.setupLoadBalancerMetadata(context, payload);
                StackCreationContext newContext = new StackCreationContext(
                        context.getFlowParameters(),
                        stack,
                        context.getCloudContext(),
                        context.getCloudCredential(),
                        context.getCloudStack());
                if (newContext.getStack().getTunnel().useCcm()) {
                    GetTlsInfoResult getTlsInfoResult = new GetTlsInfoResult(context.getCloudContext().getId(), new TlsInfo(true));
                    sendEvent(newContext, getTlsInfoResult.selector(), getTlsInfoResult);
                } else {
                    sendEvent(newContext);
                }
            }

            @Override
            protected Selectable createRequest(StackCreationContext context) {
                CloudStack cloudStack = cloudStackConverter.convert(context.getStack());
                return new GetTlsInfoRequest<GetTlsInfoResult>(context.getCloudContext(), context.getCloudCredential(), cloudStack);
            }
        };
    }

    @Bean(name = "GET_TLS_INFO_STATE")
    public Action<?, ?> getTlsInfoAction() {
        return new AbstractStackCreationAction<>(GetTlsInfoResult.class) {
            @Override
            protected void doExecute(StackCreationContext context, GetTlsInfoResult payload, Map<Object, Object> variables) {
                Stack stack = stackCreationService.saveTlsInfo(context, payload.getTlsInfo());
                StackCreationContext newContext = new StackCreationContext(
                        context.getFlowParameters(),
                        stack,
                        context.getCloudContext(),
                        context.getCloudCredential(),
                        context.getCloudStack());
                sendEvent(newContext);
            }

            @Override
            protected Selectable createRequest(StackCreationContext context) {
                Stack stack = context.getStack();
                InstanceMetaData gatewayMetaData = stack.getPrimaryGatewayInstance();
                DetailedEnvironmentResponse environment = environmentClientService.getByCrnAsInternal(stack.getEnvironmentCrn());
                CloudInstance gatewayInstance = metadataConverter.convert(gatewayMetaData, environment, stack.getStackAuthentication());
                return new GetSSHFingerprintsRequest<GetSSHFingerprintsResult>(context.getCloudContext(), context.getCloudCredential(), gatewayInstance);
            }
        };
    }

    @Bean(name = "TLS_SETUP_STATE")
    public Action<?, ?> tlsSetupAction() {
        return new AbstractStackCreationAction<>(GetSSHFingerprintsResult.class) {
            @Override
            protected void doExecute(StackCreationContext context, GetSSHFingerprintsResult payload, Map<Object, Object> variables) throws Exception {
                if (!context.getStack().getTunnel().useCcm()) {
                    stackCreationService.setupTls(context);
                }
                StackWithFingerprintsEvent fingerprintsEvent = new StackWithFingerprintsEvent(payload.getResourceId(), payload.getSshFingerprints());
                sendEvent(context, StackCreationEvent.TLS_SETUP_FINISHED_EVENT.event(), fingerprintsEvent);
            }
        };
    }

    @Bean(name = "STACK_CREATION_FINISHED_STATE")
    public Action<?, ?> stackCreationFinishedAction() {
        return new AbstractStackCreationAction<>(StackWithFingerprintsEvent.class) {

            @Override
            protected void doExecute(StackCreationContext context, StackWithFingerprintsEvent payload, Map<Object, Object> variables) {
                stackCreationService.stackCreationFinished(context.getStack());
                getMetricService().incrementMetricCounter(MetricType.STACK_CREATION_SUCCESSFUL, context.getStack());
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(StackCreationContext context) {
                return new StackEvent(StackCreationEvent.STACK_CREATION_FINISHED_EVENT.event(), context.getStack().getId());
            }
        };
    }

    @Bean(name = "STACK_CREATION_FAILED_STATE")
    public Action<?, ?> stackCreationFailureAction() {
        return new AbstractStackFailureAction<StackCreationState, StackCreationEvent>() {

            @Override
            protected void doExecute(StackFailureContext context, StackFailureEvent payload, Map<Object, Object> variables) {
                stackCreationService.handleStackCreationFailure(context.getStackView(), payload.getException(), context.getProvisionType());
                getMetricService().incrementMetricCounter(MetricType.STACK_CREATION_FAILED, context.getStackView(), payload.getException());
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(StackFailureContext context) {
                return new StackEvent(StackCreationEvent.STACKCREATION_FAILURE_HANDLED_EVENT.event(), context.getStackView().getId());
            }
        };
    }
}
