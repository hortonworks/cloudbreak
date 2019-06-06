package com.sequenceiq.redbeams.flow.stack.provision.action;

import org.springframework.context.annotation.Configuration;

@Configuration
public class DatabaseServerProvisionActions {

    // Blocked out for now
    /*
    @Inject
    private ImageService imageService;

    @Inject
    private StackToCloudStackConverter cloudStackConverter;

    @Inject
    private StackProvisionService stackProvisionService;

    @Inject
    private InstanceMetaDataToCloudInstanceConverter metadataConverter;

    @Inject
    private StackService stackService;

    @Bean(name = "VALIDATION_STATE")
    public Action<?, ?> provisioningValidationAction() {
        return new AbstractStackProvisionAction<>(RedbeamsEvent.class) {
            @Override
            protected void doExecute(RedbeamsContext context, RedbeamsEvent payload, Map<Object, Object> variables) {
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(RedbeamsContext context) {
                return new ValidationRequest(context.getCloudContext(), context.getCloudCredential(), context.getCloudStack());
            }
        };
    }

    @Bean(name = "SETUP_STATE")
    public Action<?, ?> provisioningSetupAction() {
        return new AbstractStackProvisionAction<>(ValidationResult.class) {
            @Override
            protected void doExecute(RedbeamsContext context, ValidationResult payload, Map<Object, Object> variables) {
                stackProvisionService.setupProvision(context.getStack());
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(RedbeamsContext context) {
                return new SetupRequest<SetupResult>(context.getCloudContext(), context.getCloudCredential(), context.getCloudStack());
            }
        };
    }

    @Bean(name = "CREATE_CREDENTIAL_STATE")
    public Action<?, ?> createCredentialAction() {
        return new AbstractStackProvisionAction<>(SetupResult.class) {
            @Override
            protected void doExecute(RedbeamsContext context, SetupResult payload, Map<Object, Object> variables) {
                variables.put(START_DATE, new Date());
                stackProvisionService.startProvisioning(context);
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(RedbeamsContext context) {
                return new CreateCredentialRequest(context.getCloudContext(), context.getCloudCredential(), context.getCloudStack());
            }
        };
    }

    @Bean(name = "START_PROVISIONING_STATE")
    public Action<?, ?> startProvisioningAction() {
        return new AbstractStackProvisionAction<>(CreateCredentialResult.class) {
            @Override
            protected void doExecute(RedbeamsContext context, CreateCredentialResult payload, Map<Object, Object> variables) {
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(RedbeamsContext context) {
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
            protected void doExecute(RedbeamsContext context, LaunchStackResult payload, Map<Object, Object> variables) {
                Stack stack = stackProvisionService.provisioningFinished(context, payload, variables);
                RedbeamsContext newContext = new RedbeamsContext(context.getFlowParameters(), stack, context.getCloudContext(),
                        context.getCloudCredential(), context.getCloudStack());
                sendEvent(newContext);
            }

            @Override
            protected Selectable createRequest(RedbeamsContext context) {
                List<CloudInstance> cloudInstances = cloudStackConverter.buildInstances(context.getStack());
//                List<CloudResource> cloudResources = cloudResourceConverter.convert(context.getStack().getResources());
                return new CollectMetadataRequest(context.getCloudContext(), context.getCloudCredential(), Collections.emptyList(), cloudInstances,
                        cloudInstances);
            }
        };
    }

    @Bean(name = "COLLECTMETADATA_STATE")
    public Action<?, ?> collectMetadataAction() {
        return new AbstractStackProvisionAction<>(CollectMetadataResult.class) {
            @Override
            protected void doExecute(RedbeamsContext context, CollectMetadataResult payload, Map<Object, Object> variables) {
                Stack stack = stackProvisionService.setupMetadata(context, payload);
                RedbeamsContext newContext = new RedbeamsContext(context.getFlowParameters(), stack, context.getCloudContext(),
                        context.getCloudCredential(), context.getCloudStack());
                sendEvent(newContext);
            }

            @Override
            protected Selectable createRequest(RedbeamsContext context) {
                CloudStack cloudStack = cloudStackConverter.convert(context.getStack());
                return new GetTlsInfoRequest<GetTlsInfoResult>(context.getCloudContext(), context.getCloudCredential(), cloudStack);
            }
        };
    }

    @Bean(name = "GET_TLS_INFO_STATE")
    public Action<?, ?> getTlsInfoAction() {
        return new AbstractStackProvisionAction<>(GetTlsInfoResult.class) {
            @Override
            protected void doExecute(RedbeamsContext context, GetTlsInfoResult payload, Map<Object, Object> variables) {
                stackProvisionService.saveTlsInfo(context, payload.getTlsInfo());
                sendEvent(context, new RedbeamsEvent(StackProvisionEvent.SETUP_TLS_EVENT.event(), context.getStack().getId()));
            }
        };
    }

    @Bean(name = "TLS_SETUP_STATE")
    public Action<?, ?> tlsSetupAction() {
        return new AbstractStackProvisionAction<>(RedbeamsEvent.class) {
            @Override
            protected void doExecute(RedbeamsContext context, RedbeamsEvent payload, Map<Object, Object> variables) throws Exception {
                stackProvisionService.setupTls(context);
                sendEvent(context, new RedbeamsEvent(StackProvisionEvent.TLS_SETUP_FINISHED_EVENT.event(), context.getStack().getId()));
            }
        };
    }

    @Bean(name = "STACK_CREATION_FINISHED_STATE")
    public Action<?, ?> stackCreationFinishedAction() {
        return new AbstractStackProvisionAction<>(RedbeamsEvent.class) {
            @Override
            protected void doExecute(RedbeamsContext context, RedbeamsEvent payload, Map<Object, Object> variables) {
                stackProvisionService.stackCreationFinished(context.getStack());
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(RedbeamsContext context) {
                return new RedbeamsEvent(StackProvisionEvent.STACK_CREATION_FINISHED_EVENT.event(), context.getStack().getId());
            }
        };
    }

    @Bean(name = "STACK_CREATION_FAILED_STATE")
    public Action<?, ?> stackCreationFailureAction() {
        return new AbstractStackFailureAction<StackProvisionState, StackProvisionEvent>() {
            @Override
            protected StackFailureContext createFlowContext(
                    FlowParameters flowParameters, StateContext<StackProvisionState, StackProvisionEvent> stateContext, RedbeamsFailureEvent payload) {
                Flow flow = getFlow(flowParameters.getFlowId());
                Stack stack = stackService.getStackById(payload.getResourceId());
                MDCBuilder.buildMdcContext(stack);
                flow.setFlowFailed(payload.getException());
                return new StackFailureContext(flowParameters, stack);
            }

            @Override
            protected void doExecute(StackFailureContext context, RedbeamsFailureEvent payload, Map<Object, Object> variables) {
                stackProvisionService.handleStackCreationFailure(context.getStack(), payload.getException());
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(StackFailureContext context) {
                return new RedbeamsEvent(StackProvisionEvent.STACKCREATION_FAILURE_HANDLED_EVENT.event(), context.getStack().getId());
            }
        };
    }
    */
}
