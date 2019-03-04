package com.sequenceiq.cloudbreak.core.flow2.stack.provision.action;

import static com.sequenceiq.cloudbreak.core.flow2.stack.provision.StackProvisionConstants.START_DATE;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;

import com.sequenceiq.cloudbreak.cloud.event.Selectable;
import com.sequenceiq.cloudbreak.cloud.event.instance.CollectMetadataRequest;
import com.sequenceiq.cloudbreak.cloud.event.instance.CollectMetadataResult;
import com.sequenceiq.cloudbreak.cloud.event.instance.GetSSHFingerprintsRequest;
import com.sequenceiq.cloudbreak.cloud.event.instance.GetSSHFingerprintsResult;
import com.sequenceiq.cloudbreak.cloud.event.instance.GetTlsInfoRequest;
import com.sequenceiq.cloudbreak.cloud.event.instance.GetTlsInfoResult;
import com.sequenceiq.cloudbreak.cloud.event.resource.CreateCredentialRequest;
import com.sequenceiq.cloudbreak.cloud.event.resource.CreateCredentialResult;
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
import com.sequenceiq.cloudbreak.converter.spi.InstanceMetaDataToCloudInstanceConverter;
import com.sequenceiq.cloudbreak.converter.spi.ResourceToCloudResourceConverter;
import com.sequenceiq.cloudbreak.converter.spi.StackToCloudStackConverter;
import com.sequenceiq.cloudbreak.core.CloudbreakImageNotFoundException;
import com.sequenceiq.cloudbreak.core.flow2.Flow;
import com.sequenceiq.cloudbreak.core.flow2.stack.AbstractStackFailureAction;
import com.sequenceiq.cloudbreak.core.flow2.stack.StackContext;
import com.sequenceiq.cloudbreak.core.flow2.stack.StackFailureContext;
import com.sequenceiq.cloudbreak.core.flow2.stack.provision.StackCreationEvent;
import com.sequenceiq.cloudbreak.core.flow2.stack.provision.StackCreationState;
import com.sequenceiq.cloudbreak.domain.FailurePolicy;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.view.StackView;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.StackWithFingerprintsEvent;
import com.sequenceiq.cloudbreak.service.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.service.image.ImageService;
import com.sequenceiq.cloudbreak.service.metrics.MetricType;
import com.sequenceiq.cloudbreak.service.stack.StackService;

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
    private StackService stackService;

    @Bean(name = "VALIDATION_STATE")
    public Action<?, ?> provisioningValidationAction() {
        return new AbstractStackCreationAction<>(StackEvent.class) {
            @Override
            protected void doExecute(StackContext context, StackEvent payload, Map<Object, Object> variables) {
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(StackContext context) {
                return new ValidationRequest(context.getCloudContext(), context.getCloudCredential(), context.getCloudStack());
            }
        };
    }

    @Bean(name = "SETUP_STATE")
    public Action<?, ?> provisioningSetupAction() {
        return new AbstractStackCreationAction<>(ValidationResult.class) {
            @Override
            protected void doExecute(StackContext context, ValidationResult payload, Map<Object, Object> variables) {
                stackCreationService.setupProvision(context.getStack());
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(StackContext context) {
                return new SetupRequest<SetupResult>(context.getCloudContext(), context.getCloudCredential(), context.getCloudStack());
            }
        };
    }

    @Bean(name = "IMAGESETUP_STATE")
    public Action<?, ?> prepareImageAction() {
        return new AbstractStackCreationAction<>(SetupResult.class) {
            @Override
            protected void doExecute(StackContext context, SetupResult payload, Map<Object, Object> variables) {
                stackCreationService.prepareImage(context.getStack());
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(StackContext context) {
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
            protected void doExecute(StackContext context, StackEvent payload, Map<Object, Object> variables) {
                variables.put(START_DATE, new Date());
                stackCreationService.startProvisioning(context);
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(StackContext context) {
                return new CreateCredentialRequest(context.getCloudContext(), context.getCloudCredential(), context.getCloudStack());
            }
        };
    }

    @Bean(name = "START_PROVISIONING_STATE")
    public Action<?, ?> startProvisioningAction() {
        return new AbstractStackCreationAction<>(CreateCredentialResult.class) {
            @Override
            protected void doExecute(StackContext context, CreateCredentialResult payload, Map<Object, Object> variables) {
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(StackContext context) {
                FailurePolicy policy = Optional.ofNullable(context.getStack().getFailurePolicy()).orElse(new FailurePolicy());
                return new LaunchStackRequest(context.getCloudContext(), context.getCloudCredential(), context.getCloudStack(),
                        policy.getAdjustmentType(), policy.getThreshold());
            }
        };
    }

    @Bean(name = "PROVISIONING_FINISHED_STATE")
    public Action<?, ?> provisioningFinishedAction() {
        return new AbstractStackCreationAction<>(LaunchStackResult.class) {
            @Override
            protected void doExecute(StackContext context, LaunchStackResult payload, Map<Object, Object> variables) {
                Stack stack = stackCreationService.provisioningFinished(context, payload, variables);
                StackContext newContext = new StackContext(context.getFlowId(), stack, context.getCloudContext(),
                        context.getCloudCredential(), context.getCloudStack());
                sendEvent(newContext);
            }

            @Override
            protected Selectable createRequest(StackContext context) {
                List<CloudInstance> cloudInstances = cloudStackConverter.buildInstances(context.getStack());
                List<CloudResource> cloudResources = cloudResourceConverter.convert(context.getStack().getResources());
                return new CollectMetadataRequest(context.getCloudContext(), context.getCloudCredential(), cloudResources, cloudInstances, cloudInstances);
            }
        };
    }

    @Bean(name = "COLLECTMETADATA_STATE")
    public Action<?, ?> collectMetadataAction() {
        return new AbstractStackCreationAction<>(CollectMetadataResult.class) {
            @Override
            protected void doExecute(StackContext context, CollectMetadataResult payload, Map<Object, Object> variables) {
                Stack stack = stackCreationService.setupMetadata(context, payload);
                StackContext newContext = new StackContext(context.getFlowId(), stack, context.getCloudContext(), context.getCloudCredential(),
                        context.getCloudStack());
                sendEvent(newContext);
            }

            @Override
            protected Selectable createRequest(StackContext context) {
                CloudStack cloudStack = cloudStackConverter.convert(context.getStack());
                return new GetTlsInfoRequest<GetTlsInfoResult>(context.getCloudContext(), context.getCloudCredential(), cloudStack);
            }
        };
    }

    @Bean(name = "GET_TLS_INFO_STATE")
    public Action<?, ?> getTlsInfoAction() {
        return new AbstractStackCreationAction<>(GetTlsInfoResult.class) {
            @Override
            protected void doExecute(StackContext context, GetTlsInfoResult payload, Map<Object, Object> variables) {
                Stack stack = stackCreationService.saveTlsInfo(context, payload.getTlsInfo());
                StackContext newContext = new StackContext(context.getFlowId(), stack, context.getCloudContext(), context.getCloudCredential(),
                        context.getCloudStack());
                sendEvent(newContext);
            }

            @Override
            protected Selectable createRequest(StackContext context) {
                InstanceMetaData gatewayMetaData = context.getStack().getPrimaryGatewayInstance();
                CloudInstance gatewayInstance = metadataConverter.convert(gatewayMetaData);
                return new GetSSHFingerprintsRequest<GetSSHFingerprintsResult>(context.getCloudContext(), context.getCloudCredential(), gatewayInstance);
            }
        };
    }

    @Bean(name = "TLS_SETUP_STATE")
    public Action<?, ?> tlsSetupAction() {
        return new AbstractStackCreationAction<>(GetSSHFingerprintsResult.class) {
            @Override
            protected void doExecute(StackContext context, GetSSHFingerprintsResult payload, Map<Object, Object> variables) throws Exception {
                stackCreationService.setupTls(context);
                StackWithFingerprintsEvent fingerprintsEvent = new StackWithFingerprintsEvent(payload.getStackId(), payload.getSshFingerprints());
                sendEvent(context.getFlowId(), StackCreationEvent.TLS_SETUP_FINISHED_EVENT.event(), fingerprintsEvent);
            }
        };
    }

    @Bean(name = "STACK_CREATION_FINISHED_STATE")
    public Action<?, ?> stackCreationFinishedAction() {
        return new AbstractStackCreationAction<>(StackWithFingerprintsEvent.class) {
            @Override
            protected void doExecute(StackContext context, StackWithFingerprintsEvent payload, Map<Object, Object> variables) {
                stackCreationService.stackCreationFinished(context.getStack());
                getMetricService().incrementMetricCounter(MetricType.STACK_CREATION_SUCCESSFUL, context.getStack());
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(StackContext context) {
                return new StackEvent(StackCreationEvent.STACK_CREATION_FINISHED_EVENT.event(), context.getStack().getId());
            }
        };
    }

    @Bean(name = "STACK_CREATION_FAILED_STATE")
    public Action<?, ?> stackCreationFailureAction() {
        return new AbstractStackFailureAction<StackCreationState, StackCreationEvent>() {
            @Override
            protected StackFailureContext createFlowContext(
                String flowId, StateContext<StackCreationState, StackCreationEvent> stateContext, StackFailureEvent payload) {
                Flow flow = getFlow(flowId);
                StackView stackView = stackService.getViewByIdWithoutAuth(payload.getStackId());
                MDCBuilder.buildMdcContext(stackView);
                flow.setFlowFailed(payload.getException());
                return new StackFailureContext(flowId, stackView);
            }

            @Override
            protected void doExecute(StackFailureContext context, StackFailureEvent payload, Map<Object, Object> variables) {
                stackCreationService.handleStackCreationFailure(context.getStackView(), payload.getException());
                getMetricService().incrementMetricCounter(MetricType.STACK_CREATION_FAILED, context.getStackView());
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(StackFailureContext context) {
                return new StackEvent(StackCreationEvent.STACKCREATION_FAILURE_HANDLED_EVENT.event(), context.getStackView().getId());
            }
        };
    }
}
