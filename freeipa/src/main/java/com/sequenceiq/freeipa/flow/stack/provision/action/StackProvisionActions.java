package com.sequenceiq.freeipa.flow.stack.provision.action;

import static com.sequenceiq.freeipa.flow.stack.provision.StackProvisionConstants.START_DATE;

import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

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
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.common.api.type.AdjustmentType;
import com.sequenceiq.flow.core.Flow;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.freeipa.converter.cloud.InstanceMetaDataToCloudInstanceConverter;
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
import com.sequenceiq.freeipa.flow.stack.provision.StackProvisionEvent;
import com.sequenceiq.freeipa.flow.stack.provision.StackProvisionState;
import com.sequenceiq.freeipa.flow.stack.provision.event.clusterproxy.ClusterProxyRegistrationRequest;
import com.sequenceiq.freeipa.service.image.ImageService;
import com.sequenceiq.freeipa.service.resource.ResourceService;
import com.sequenceiq.freeipa.service.stack.StackService;

@Configuration
public class StackProvisionActions {

    @Inject
    private ImageService imageService;

    @Inject
    private ImageConverter imageConverter;

    @Inject
    private StackToCloudStackConverter cloudStackConverter;

    @Inject
    private StackProvisionService stackProvisionService;

    @Inject
    private InstanceMetaDataToCloudInstanceConverter metadataConverter;

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
                return new ValidationRequest(context.getCloudContext(), context.getCloudCredential(), context.getCloudStack());
            }
        };
    }

    @Bean(name = "SETUP_STATE")
    public Action<?, ?> provisioningSetupAction() {
        return new AbstractStackProvisionAction<>(ValidationResult.class) {
            @Override
            protected void doExecute(StackContext context, ValidationResult payload, Map<Object, Object> variables) {
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
        return new AbstractStackProvisionAction<>(SetupResult.class) {
            @Override
            protected void doExecute(StackContext context, SetupResult payload, Map<Object, Object> variables) {
                stackProvisionService.prepareImage(context.getStack());
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(StackContext context) {
                CloudStack cloudStack = cloudStackConverter.convert(context.getStack());
                Image image = imageConverter.convert(imageService.getByStack(context.getStack()));
                return new PrepareImageRequest<>(context.getCloudContext(), context.getCloudCredential(), cloudStack, image);
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
                return new CreateCredentialRequest(context.getCloudContext(), context.getCloudCredential(), context.getCloudStack());
            }
        };
    }

    @Bean(name = "START_PROVISIONING_STATE")
    public Action<?, ?> startProvisioningAction() {
        return new AbstractStackProvisionAction<>(CreateCredentialResult.class) {
            @Override
            protected void doExecute(StackContext context, CreateCredentialResult payload, Map<Object, Object> variables) {
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(StackContext context) {
                //FIXME AdjustmentType and treshold
                return new LaunchStackRequest(context.getCloudContext(), context.getCloudCredential(), context.getCloudStack(),
                        AdjustmentType.EXACT, 1L);
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
                List<CloudResource> cloudResources = resourceConverter.convert(resources);
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
                if (newContext.getStack().getUseCcm()) {
                    GetTlsInfoResult getTlsInfoResult = new GetTlsInfoResult(context.getCloudContext().getId(), new TlsInfo(true));
                    sendEvent(newContext, getTlsInfoResult.selector(), getTlsInfoResult);
                } else {
                    sendEvent(newContext);
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
                if (!context.getStack().getUseCcm()) {
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
                Flow flow = getFlow(flowParameters.getFlowId());
                Stack stack = stackService.getStackById(payload.getResourceId());
                MDCBuilder.buildMdcContext(stack);
                flow.setFlowFailed(payload.getException());
                return new StackFailureContext(flowParameters, stack);
            }

            @Override
            protected void doExecute(StackFailureContext context, StackFailureEvent payload, Map<Object, Object> variables) {
                stackProvisionService.handleStackCreationFailure(context.getStack(), payload.getException());
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(StackFailureContext context) {
                return new StackEvent(StackProvisionEvent.STACKCREATION_FAILURE_HANDLED_EVENT.event(), context.getStack().getId());
            }
        };
    }
}
