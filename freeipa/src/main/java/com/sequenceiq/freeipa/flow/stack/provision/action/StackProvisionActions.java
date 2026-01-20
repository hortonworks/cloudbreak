package com.sequenceiq.freeipa.flow.stack.provision.action;

import static com.sequenceiq.cloudbreak.event.ResourceEvent.FREEIPA_CREATION_FAILED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.FREEIPA_CREATION_FINISHED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.FREEIPA_CREATION_STARTED;
import static com.sequenceiq.freeipa.flow.freeipa.common.FailureType.ERROR;
import static com.sequenceiq.freeipa.flow.stack.provision.StackProvisionConstants.START_DATE;
import static com.sequenceiq.freeipa.flow.stack.provision.StackProvisionEvent.IMAGE_FALLBACK_START_EVENT;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;

import com.sequenceiq.cloudbreak.cloud.event.instance.CollectMetadataRequest;
import com.sequenceiq.cloudbreak.cloud.event.instance.CollectMetadataResult;
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
import com.sequenceiq.cloudbreak.cloud.model.TlsInfo;
import com.sequenceiq.cloudbreak.cloud.model.catalog.PrepareImageType;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.common.api.type.AdjustmentType;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.flow.core.PayloadConverter;
import com.sequenceiq.freeipa.converter.cloud.ResourceToCloudResourceConverter;
import com.sequenceiq.freeipa.converter.cloud.StackToCloudStackConverter;
import com.sequenceiq.freeipa.converter.image.ImageConverter;
import com.sequenceiq.freeipa.entity.Resource;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.stack.AbstractStackFailureAction;
import com.sequenceiq.freeipa.flow.stack.StackContext;
import com.sequenceiq.freeipa.flow.stack.StackEvent;
import com.sequenceiq.freeipa.flow.stack.StackFailureContext;
import com.sequenceiq.freeipa.flow.stack.StackFailureEvent;
import com.sequenceiq.freeipa.flow.stack.provision.SetupResultToStackEventConverter;
import com.sequenceiq.freeipa.flow.stack.provision.StackProvisionEvent;
import com.sequenceiq.freeipa.flow.stack.provision.StackProvisionState;
import com.sequenceiq.freeipa.flow.stack.provision.event.clusterproxy.ClusterProxyRegistrationRequest;
import com.sequenceiq.freeipa.flow.stack.provision.event.encryption.GenerateEncryptionKeysRequest;
import com.sequenceiq.freeipa.flow.stack.provision.event.encryption.GenerateEncryptionKeysSuccess;
import com.sequenceiq.freeipa.flow.stack.provision.event.imagefallback.ImageFallbackFailed;
import com.sequenceiq.freeipa.flow.stack.provision.event.imagefallback.ImageFallbackRequest;
import com.sequenceiq.freeipa.flow.stack.provision.event.userdata.CreateUserDataRequest;
import com.sequenceiq.freeipa.flow.stack.provision.event.userdata.CreateUserDataSuccess;
import com.sequenceiq.freeipa.flow.stack.provision.event.userdata.UpdateUserdataSecretsRequest;
import com.sequenceiq.freeipa.flow.stack.provision.event.userdata.UpdateUserdataSecretsSuccess;
import com.sequenceiq.freeipa.service.image.ImageFallbackService;
import com.sequenceiq.freeipa.service.image.ImageService;
import com.sequenceiq.freeipa.service.resource.ResourceService;
import com.sequenceiq.freeipa.service.stack.StackService;

@Configuration
public class StackProvisionActions {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackProvisionActions.class);

    private static final String IMAGE_FALLBACK_STARTED = "IMAGE_FALLBACK_STARTED";

    @Inject
    private ImageService imageService;

    @Inject
    private ImageConverter imageConverter;

    @Inject
    private StackToCloudStackConverter cloudStackConverter;

    @Inject
    private StackProvisionService stackProvisionService;

    @Inject
    private StackService stackService;

    @Inject
    private ResourceToCloudResourceConverter resourceConverter;

    @Inject
    private ResourceService resourceService;

    @Bean(name = "VALIDATION_STATE")
    public Action<?, ?> provisioningValidationAction() {
        return new AbstractStackProvisionAction<>(StackEvent.class) {
            @Override
            protected void doExecute(StackContext context, StackEvent payload, Map<Object, Object> variables) {
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(StackContext context) {
                getEventService().sendEventAndNotification(context.getStack(), context.getFlowTriggerUserCrn(), FREEIPA_CREATION_STARTED);
                return new ValidationRequest(context.getCloudContext(), context.getCloudCredential(), context.getCloudStack());
            }
        };
    }

    @Bean(name = "GENERATE_ENCRYPTION_KEYS_STATE")
    public Action<?, ?> generateEncryptionKeysAction() {
        return new AbstractStackProvisionAction<>(ValidationResult.class) {
            @Override
            protected void doExecute(StackContext context, ValidationResult payload, Map<Object, Object> variables) {
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(StackContext context) {
                LOGGER.info("Generate Encryption keys with Stack {}", context.getStack().getId());
                return new GenerateEncryptionKeysRequest(context.getStack().getId());
            }
        };
    }

    @Bean(name = "CREATE_USER_DATA_STATE")
    public Action<?, ?> createUserDataAction() {
        return new AbstractStackProvisionAction<>(GenerateEncryptionKeysSuccess.class) {
            @Override
            protected void doExecute(StackContext context, GenerateEncryptionKeysSuccess payload, Map<Object, Object> variables) {
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(StackContext context) {
                return new CreateUserDataRequest(context.getStack().getId(), context.getCloudContext(), context.getCloudCredential());
            }
        };
    }

    @Bean(name = "SETUP_STATE")
    public Action<?, ?> provisioningSetupAction() {
        return new AbstractStackProvisionAction<>(CreateUserDataSuccess.class) {
            @Override
            protected void doExecute(StackContext context, CreateUserDataSuccess payload, Map<Object, Object> variables) {
                stackProvisionService.setupProvision(context.getStack());
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
        return new AbstractStackProvisionAction<>(StackEvent.class) {

            @Override
            protected void doExecute(StackContext context, StackEvent payload, Map<Object, Object> variables) {
                stackProvisionService.prepareImage(context.getStack());
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(StackContext context) {
                CloudStack cloudStack = cloudStackConverter.convert(context.getStack());
                Image image = imageConverter.convert(imageService.getByStack(context.getStack()));
                return new PrepareImageRequest<>(context.getCloudContext(), context.getCloudCredential(), cloudStack, image,
                        PrepareImageType.EXECUTED_DURING_PROVISIONING);
            }

            @Override
            protected void initPayloadConverterMap(List<PayloadConverter<StackEvent>> payloadConverters) {
                payloadConverters.add(new SetupResultToStackEventConverter());
            }
        };
    }

    @Bean(name = "CREATE_CREDENTIAL_STATE")
    public Action<?, ?> createCredentialAction() {
        return new AbstractStackProvisionAction<>(StackEvent.class) {
            @Override
            protected void doExecute(StackContext context, StackEvent payload, Map<Object, Object> variables) {
                variables.put(START_DATE, new Date());
                stackProvisionService.startProvisioning(context);
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(StackContext context) {
                return new CreateCredentialRequest(context.getCloudContext(), context.getCloudCredential());
            }
        };
    }

    @Bean(name = "START_PROVISIONING_STATE")
    public Action<?, ?> startProvisioningAction() {
        return new AbstractStackProvisionAction<>(CreateCredentialResult.class) {

            @Inject
            private ImageFallbackService imageFallbackService;

            @Override
            protected void doExecute(StackContext context, CreateCredentialResult payload, Map<Object, Object> variables) {
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(StackContext context) {

                Optional<String> fallbackImage = imageFallbackService.determineFallbackImageIfPermitted(context);

                //FIXME AdjustmentType and threshold
                return new LaunchStackRequest(context.getCloudContext(), context.getCloudCredential(), context.getCloudStack(),
                        AdjustmentType.EXACT, 1L, fallbackImage);
            }
        };
    }

    @Bean(name = "IMAGE_FALLBACK_STATE")
    public Action<?, ?> imageFallbackAction() {
        return new AbstractStackProvisionAction<>(LaunchStackResult.class) {

            @Override
            protected void doExecute(StackContext context, LaunchStackResult payload, Map<Object, Object> variables) {
                if ((Boolean) variables.getOrDefault(IMAGE_FALLBACK_STARTED, Boolean.FALSE)) {
                    LOGGER.warn("Image fallback already happened at least once! Failing flow to avoid infinite loop!");
                    sendEvent(context, new ImageFallbackFailed(
                            payload.getResourceId(),
                            new Exception("Image fallback started second time!"),
                            ERROR
                    ));
                } else {
                    stackProvisionService.stackCreationImageFallbackRequired(context.getStack(), payload.getNotificationMessage());
                    sendEvent(context, new StackEvent(IMAGE_FALLBACK_START_EVENT.event(), payload.getResourceId()));
                }
            }
        };
    }

    @Bean(name = "IMAGE_FALLBACK_START_STATE")
    public Action<?, ?> imageFallbackStartAction() {
        return new AbstractStackProvisionAction<>(StackEvent.class) {
            @Override
            protected void doExecute(StackContext context, StackEvent payload, Map<Object, Object> variables) {
                variables.put(IMAGE_FALLBACK_STARTED, Boolean.TRUE);
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(StackContext context) {
                return new ImageFallbackRequest(context.getStack().getId());
            }
        };
    }

    @Bean(name = "PROVISIONING_FINISHED_STATE")
    public Action<?, ?> provisioningFinishedAction() {
        return new AbstractStackProvisionAction<>(LaunchStackResult.class) {
            @Override
            protected void doExecute(StackContext context, LaunchStackResult payload, Map<Object, Object> variables) {
                Stack stack = stackProvisionService.provisioningFinished(context, payload, variables);
                StackContext newContext = new StackContext(context.getFlowParameters(), stack, context.getCloudContext(),
                        context.getCloudCredential(), context.getCloudStack());
                sendEvent(newContext);
            }

            @Override
            protected Selectable createRequest(StackContext context) {
                List<CloudInstance> cloudInstances = cloudStackConverter.buildInstances(context.getStack());
                List<Resource> resources = resourceService.findAllByStackId(context.getStack().getId());
                List<CloudResource> cloudResources = resources.stream()
                        .map(r -> resourceConverter.convert(r))
                        .collect(Collectors.toList());
                return new CollectMetadataRequest(context.getCloudContext(), context.getCloudCredential(), cloudResources, cloudInstances,
                        cloudInstances);
            }
        };
    }

    @Bean(name = "COLLECTMETADATA_STATE")
    public Action<?, ?> collectMetadataAction() {
        return new AbstractStackProvisionAction<>(CollectMetadataResult.class) {
            @Override
            protected void doExecute(StackContext context, CollectMetadataResult payload, Map<Object, Object> variables) {
                Stack stack = stackProvisionService.setupMetadata(context, payload);
                StackContext newContext = new StackContext(context.getFlowParameters(), stack, context.getCloudContext(),
                        context.getCloudCredential(), context.getCloudStack());
                sendEvent(newContext);
            }

            @Override
            protected Selectable createRequest(StackContext context) {
                return new UpdateUserdataSecretsRequest(context.getStack().getId(), context.getCloudContext(), context.getCloudCredential());
            }
        };
    }

    @Bean(name = "UPDATE_USERDATA_SECRETS_STATE")
    public Action<?, ?> updateUserDataSecretsAction() {
        return new AbstractStackProvisionAction<>(UpdateUserdataSecretsSuccess.class) {
            @Override
            protected void doExecute(StackContext context, UpdateUserdataSecretsSuccess payload, Map<Object, Object> variables) {
                if (context.getStack().getTunnel().useCcm()) {
                    GetTlsInfoResult getTlsInfoResult = new GetTlsInfoResult(context.getCloudContext().getId(), new TlsInfo(true));
                    sendEvent(context, getTlsInfoResult.selector(), getTlsInfoResult);
                } else {
                    sendEvent(context);
                }
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
        return new AbstractStackProvisionAction<>(GetTlsInfoResult.class) {
            @Override
            protected void doExecute(StackContext context, GetTlsInfoResult payload, Map<Object, Object> variables) {
                stackProvisionService.saveTlsInfo(context, payload.getTlsInfo());
                sendEvent(context, new StackEvent(StackProvisionEvent.SETUP_TLS_EVENT.event(), context.getStack().getId()));
            }
        };
    }

    @Bean(name = "TLS_SETUP_STATE")
    public Action<?, ?> tlsSetupAction() {
        return new AbstractStackProvisionAction<>(StackEvent.class) {
            @Override
            protected void doExecute(StackContext context, StackEvent payload, Map<Object, Object> variables) throws Exception {
                if (!context.getStack().getTunnel().useCcm()) {
                    stackProvisionService.setupTls(context);
                }
                sendEvent(context, new StackEvent(StackProvisionEvent.TLS_SETUP_FINISHED_EVENT.event(), context.getStack().getId()));
            }
        };
    }

    @Bean(name = "CLUSTERPROXY_REGISTRATION_STATE")
    public Action<?, ?> registerClusterProxyAction() {
        return new AbstractStackProvisionAction<>(StackEvent.class) {
            @Override
            protected void doExecute(StackContext context, StackEvent payload, Map<Object, Object> variables) throws Exception {
                stackProvisionService.registerClusterProxy(context);
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(StackContext context) {
                return new ClusterProxyRegistrationRequest(context.getStack().getId());
            }
        };
    }

    @Bean(name = "STACK_CREATION_FINISHED_STATE")
    public Action<?, ?> stackCreationFinishedAction() {
        return new AbstractStackProvisionAction<>(StackEvent.class) {
            @Override
            protected void doExecute(StackContext context, StackEvent payload, Map<Object, Object> variables) {
                stackProvisionService.stackCreationFinished(context.getStack());
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(StackContext context) {
                getEventService().sendEventAndNotification(context.getStack(), context.getFlowTriggerUserCrn(), FREEIPA_CREATION_FINISHED);
                return new StackEvent(StackProvisionEvent.STACK_CREATION_FINISHED_EVENT.event(), context.getStack().getId());
            }
        };
    }

    @Bean(name = "STACK_CREATION_FAILED_STATE")
    public Action<?, ?> stackCreationFailureAction() {
        return new AbstractStackFailureAction<StackProvisionState, StackProvisionEvent>() {
            @Override
            protected StackFailureContext createFlowContext(
                    FlowParameters flowParameters, StateContext<StackProvisionState, StackProvisionEvent> stateContext, StackFailureEvent payload) {
                Stack stack = stackService.getStackById(payload.getResourceId());
                MDCBuilder.buildMdcContext(stack);
                return new StackFailureContext(flowParameters, stack);
            }

            @Override
            protected void doExecute(StackFailureContext context, StackFailureEvent payload, Map<Object, Object> variables) {
                stackProvisionService.handleStackCreationFailure(context.getStack(), payload.getException());
                getEventService().sendEventAndNotification(context.getStack(), context.getFlowTriggerUserCrn(), FREEIPA_CREATION_FAILED,
                        List.of(payload.getException()));
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(StackFailureContext context) {
                return new StackEvent(StackProvisionEvent.STACKCREATION_FAILURE_HANDLED_EVENT.event(), context.getStack().getId());
            }
        };
    }
}