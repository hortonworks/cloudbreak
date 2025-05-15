package com.sequenceiq.cloudbreak.core.flow2.stack.provision.action;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.UPDATE_IN_PROGRESS;
import static com.sequenceiq.cloudbreak.core.flow2.stack.provision.StackProvisionConstants.START_DATE;

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
import org.springframework.statemachine.action.Action;
import org.springframework.util.CollectionUtils;

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
import com.sequenceiq.cloudbreak.cloud.model.catalog.PrepareImageType;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.converter.spi.InstanceMetaDataToCloudInstanceConverter;
import com.sequenceiq.cloudbreak.converter.spi.ResourceToCloudResourceConverter;
import com.sequenceiq.cloudbreak.converter.spi.StackToCloudStackConverter;
import com.sequenceiq.cloudbreak.core.CloudbreakImageCatalogException;
import com.sequenceiq.cloudbreak.core.CloudbreakImageNotFoundException;
import com.sequenceiq.cloudbreak.core.flow2.stack.AbstractStackFailureAction;
import com.sequenceiq.cloudbreak.core.flow2.stack.StackFailureContext;
import com.sequenceiq.cloudbreak.core.flow2.stack.provision.SetupResultToStackEventConverter;
import com.sequenceiq.cloudbreak.core.flow2.stack.provision.StackCreationEvent;
import com.sequenceiq.cloudbreak.core.flow2.stack.provision.StackCreationState;
import com.sequenceiq.cloudbreak.core.flow2.stack.provision.service.StackCreationService;
import com.sequenceiq.cloudbreak.core.flow2.stack.start.StackCreationContext;
import com.sequenceiq.cloudbreak.domain.FailurePolicy;
import com.sequenceiq.cloudbreak.domain.SecurityConfig;
import com.sequenceiq.cloudbreak.domain.stack.loadbalancer.LoadBalancer;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.ImageFallbackRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.ProvisionEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.StackWithFingerprintsEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.consumption.AttachedVolumeConsumptionCollectionSchedulingRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.consumption.AttachedVolumeConsumptionCollectionSchedulingSuccess;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.encryption.GenerateEncryptionKeysRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.encryption.GenerateEncryptionKeysSuccess;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.userdata.CreateUserDataRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.userdata.CreateUserDataSuccess;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.userdata.UpdateUserdataSecretsRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.userdata.UpdateUserdataSecretsSuccess;
import com.sequenceiq.cloudbreak.reactor.handler.ImageFallbackService;
import com.sequenceiq.cloudbreak.service.image.ImageService;
import com.sequenceiq.cloudbreak.service.metrics.MetricType;
import com.sequenceiq.cloudbreak.service.multiaz.DataLakeAwareInstanceMetadataAvailabilityZoneCalculator;
import com.sequenceiq.cloudbreak.service.resource.ResourceService;
import com.sequenceiq.cloudbreak.service.stack.LoadBalancerPersistenceService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.service.stack.flow.InstanceMetadataInstanceIdUpdater;
import com.sequenceiq.cloudbreak.structuredevent.event.CloudbreakEventService;
import com.sequenceiq.cloudbreak.view.InstanceGroupView;
import com.sequenceiq.cloudbreak.view.InstanceMetadataView;
import com.sequenceiq.cloudbreak.view.StackView;
import com.sequenceiq.common.api.type.LoadBalancerType;
import com.sequenceiq.flow.core.PayloadConverter;

@Configuration
public class StackCreationActions {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackCreationActions.class);

    @Inject
    private ImageService imageService;

    @Inject
    private ImageFallbackService imageFallbackService;

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
    private ResourceService resourceService;

    @Inject
    private StackDtoService stackDtoService;

    @Inject
    private CloudbreakEventService eventService;

    @Inject
    private DataLakeAwareInstanceMetadataAvailabilityZoneCalculator instanceMetadataAvailabilityZoneCalculator;

    @Inject
    private InstanceMetadataInstanceIdUpdater instanceMetadataInstanceIdUpdater;

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
                StackDto stack = stackDtoService.getById(context.getStackId());
                CloudStack cloudStack = cloudStackConverter.convert(stack);
                return new ValidationRequest(context.getCloudContext(), context.getCloudCredential(), cloudStack);
            }
        };
    }

    @Bean(name = "GENERATE_ENCRYPTION_KEYS_STATE")
    public Action<?, ?> generateEncryptionKeysAction() {
        return new AbstractStackCreationAction<>(ValidationResult.class) {
            @Override
            protected void doExecute(StackCreationContext context, ValidationResult payload, Map<Object, Object> variables) {
                handleValidationWarnings(context, payload);
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(StackCreationContext context) {
                return new GenerateEncryptionKeysRequest(context.getStack().getId());
            }
        };
    }

    @Bean(name = "CREATE_USER_DATA_STATE")
    public Action<?, ?> createUserDataAction() {
        return new AbstractStackCreationAction<>(GenerateEncryptionKeysSuccess.class) {
            @Override
            protected void doExecute(StackCreationContext context, GenerateEncryptionKeysSuccess payload, Map<Object, Object> variables) {
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(StackCreationContext context) {
                return new CreateUserDataRequest(context.getStackId(), context.getCloudContext(), context.getCloudCredential());
            }
        };
    }

    @Bean(name = "SETUP_STATE")
    public Action<?, ?> provisioningSetupAction() {
        return new AbstractStackCreationAction<>(CreateUserDataSuccess.class) {
            @Override
            protected void doExecute(StackCreationContext context, CreateUserDataSuccess payload, Map<Object, Object> variables) {
                stackCreationService.setupProvision(context.getStackId());
                stackCreationService.setInstanceStoreCount(context);
                instanceMetadataAvailabilityZoneCalculator.populate(context.getStackId());
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(StackCreationContext context) {
                StackDto stack = stackDtoService.getById(context.getStackId());
                CloudStack cloudStack = cloudStackConverter.convert(stack);
                return new SetupRequest<SetupResult>(context.getCloudContext(), context.getCloudCredential(), cloudStack);
            }
        };
    }

    @Bean(name = "IMAGESETUP_STATE")
    public Action<?, ?> prepareImageAction() {
        return new AbstractStackCreationAction<>(StackEvent.class) {
            @Override
            protected void doExecute(StackCreationContext context, StackEvent payload, Map<Object, Object> variables) {
                stackCreationService.prepareImage(context.getStackId(), variables);
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(StackCreationContext context) {
                try {
                    StackDto stack = stackDtoService.getById(context.getStackId());
                    CloudStack cloudStack = cloudStackConverter.convert(stack);
                    Image image = imageService.getImage(context.getCloudContext().getId());
                    return new PrepareImageRequest<>(context.getCloudContext(), context.getCloudCredential(), cloudStack, image,
                            PrepareImageType.EXECUTED_DURING_PROVISIONING);
                } catch (CloudbreakImageNotFoundException e) {
                    throw new CloudbreakServiceException(e);
                }
            }

            @Override
            protected void initPayloadConverterMap(List<PayloadConverter<StackEvent>> payloadConverters) {
                payloadConverters.add(new SetupResultToStackEventConverter());
            }
        };
    }

    @Bean(name = "CREATE_CREDENTIAL_STATE")
    public Action<?, ?> createCredentialAction() {
        return new AbstractStackCreationAction<>(StackEvent.class) {
            @Override
            protected void doExecute(StackCreationContext context, StackEvent payload, Map<Object, Object> variables) {
                variables.put(START_DATE, new Date());
                StackDto stack = stackDtoService.getById(context.getStackId());
                CloudStack cloudStack = cloudStackConverter.convert(stack);
                stackCreationService.startProvisioning(stack, cloudStack, variables);
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(StackCreationContext context) {
                return new CreateCredentialRequest(context.getCloudContext(), context.getCloudCredential());
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
                StackDto stack = stackDtoService.getById(context.getStackId());
                CloudStack cloudStack = cloudStackConverter.convert(stack);
                String fallbackImageName = getFallbackImageName(cloudStack, stack);

                FailurePolicy policy = Optional.ofNullable(stack.getFailurePolicy()).orElse(new FailurePolicy());
                return new LaunchStackRequest(context.getCloudContext(), context.getCloudCredential(), cloudStack,
                        policy.getAdjustmentType(), policy.getThreshold(), Optional.ofNullable(fallbackImageName));
            }

            private String getFallbackImageName(CloudStack cloudStack, StackDto stack) {
                try {
                    Image image = cloudStack.getImage();
                    return imageFallbackService.getFallbackImageName(stack.getStack(), image);
                } catch (CloudbreakImageNotFoundException e) {
                    LOGGER.info("Fallback image could not be determined due to exception {}," +
                            " we should continue execution", e.getMessage());
                    return null;
                } catch (CloudbreakImageCatalogException e) {
                    throw new CloudbreakServiceException(e);
                }
            }
        };
    }

    @Bean(name = "IMAGE_FALLBACK_STATE")
    public Action<?, ?> imageFallbackAction() {
        return new AbstractStackCreationAction<>(LaunchStackResult.class) {
            @Override
            protected void doExecute(StackCreationContext context, LaunchStackResult payload, Map<Object, Object> variables) {
                stackCreationService.fireImageFallbackFlowMessage(context.getStackId(), payload.getNotificationMessage());
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(StackCreationContext context) {
                return new ImageFallbackRequest(context.getStackId(), context.getCloudContext());
            }
        };
    }

    @Bean(name = "PROVISION_LOAD_BALANCER_STATE")
    public Action<?, ?> startProvisioningLoadBalancerAction() {
        return new AbstractStackCreationAction<>(LaunchStackResult.class) {
            @Override
            protected void doExecute(StackCreationContext context, LaunchStackResult payload, Map<Object, Object> variables) {
                instanceMetadataInstanceIdUpdater.updateWithInstanceIdAndStatus(context, payload.getResults());
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(StackCreationContext context) {
                StackDto stack = stackDtoService.getById(context.getStackId());
                CloudStack cloudStack = cloudStackConverter.convert(stack);
                return new LaunchLoadBalancerRequest(context.getCloudContext(), context.getCloudCredential(), cloudStack);
            }
        };
    }

    @Bean(name = "PROVISIONING_FINISHED_STATE")
    public Action<?, ?> provisioningFinishedAction() {
        return new AbstractStackCreationAction<>(LaunchLoadBalancerResult.class) {
            @Override
            protected void doExecute(StackCreationContext context, LaunchLoadBalancerResult payload, Map<Object, Object> variables) {
                stackCreationService.loadBalancerProvisioningFinished(context, payload, variables);
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(StackCreationContext context) {
                StackDto stack = stackDtoService.getById(context.getStackId());
                List<CloudInstance> cloudInstances = cloudStackConverter.buildInstances(stack);
                List<CloudResource> cloudResources = resourceService.getAllByStackId(stack.getId()).stream()
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
                stackCreationService.setupMetadata(context.getStack(), payload);
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(StackCreationContext context) {
                Long stackId = context.getStackId();
                List<LoadBalancerType> loadBalancerTypes = loadBalancerPersistenceService.findByStackId(stackId).stream()
                        .map(LoadBalancer::getType)
                        .collect(Collectors.toList());
                List<CloudResource> cloudResources = resourceService.getAllByStackId(stackId).stream()
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
                StackView stack = context.getStack();
                stackCreationService.setupLoadBalancerMetadata(stack, payload);
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(StackCreationContext context) {
                return new UpdateUserdataSecretsRequest(context.getStackId(), context.getCloudContext(), context.getCloudCredential());
            }
        };
    }

    @Bean("UPDATE_USERDATA_SECRETS_STATE")
    public Action<?, ?> updateUserDataSecretsAction() {
        return new AbstractStackCreationAction<>(UpdateUserdataSecretsSuccess.class) {
            @Override
            protected void doExecute(StackCreationContext context, UpdateUserdataSecretsSuccess payload, Map<Object, Object> variables) throws Exception {
                if (context.getStack().getTunnel().useCcm()) {
                    GetTlsInfoResult getTlsInfoResult = new GetTlsInfoResult(context.getCloudContext().getId(), new TlsInfo(true));
                    sendEvent(context, getTlsInfoResult.selector(), getTlsInfoResult);
                } else {
                    sendEvent(context);
                }
            }

            @Override
            protected Selectable createRequest(StackCreationContext context) {
                StackDto stack = stackDtoService.getById(context.getStackId());
                CloudStack cloudStack = cloudStackConverter.convert(stack);
                return new GetTlsInfoRequest<GetTlsInfoResult>(context.getCloudContext(), context.getCloudCredential(), cloudStack);
            }
        };
    }

    @Bean(name = "GET_TLS_INFO_STATE")
    public Action<?, ?> getTlsInfoAction() {
        return new AbstractStackCreationAction<>(GetTlsInfoResult.class) {
            @Override
            protected void doExecute(StackCreationContext context, GetTlsInfoResult payload, Map<Object, Object> variables) {
                SecurityConfig securityConfig = stackDtoService.getSecurityConfig(context.getStackId());
                stackCreationService.saveTlsInfo(securityConfig, payload.getTlsInfo());
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(StackCreationContext context) {
                StackDto stack = stackDtoService.getById(context.getStackId());
                InstanceMetadataView gatewayMetaData = stack.getPrimaryGatewayInstance();
                InstanceGroupView instanceGroup = stack.getInstanceGroupByInstanceGroupName(gatewayMetaData.getInstanceGroupName()).getInstanceGroup();
                CloudInstance gatewayInstance = metadataConverter.convert(gatewayMetaData, instanceGroup, stack.getStack());
                return new GetSSHFingerprintsRequest<GetSSHFingerprintsResult>(context.getCloudContext(), context.getCloudCredential(), gatewayInstance);
            }
        };
    }

    @Bean(name = "TLS_SETUP_STATE")
    public Action<?, ?> tlsSetupAction() {
        return new AbstractStackCreationAction<>(GetSSHFingerprintsResult.class) {
            @Override
            protected void doExecute(StackCreationContext context, GetSSHFingerprintsResult payload, Map<Object, Object> variables) throws Exception {
                StackDto stackDto = stackDtoService.getById(context.getStackId());
                if (!stackDto.getTunnel().useCcm()) {
                    stackCreationService.setupTls(stackDto);
                }
                StackWithFingerprintsEvent fingerprintsEvent = new StackWithFingerprintsEvent(payload.getResourceId(), payload.getSshFingerprints());
                sendEvent(context, StackCreationEvent.TLS_SETUP_FINISHED_EVENT.event(), fingerprintsEvent);
            }
        };
    }

    @Bean(name = "ATTACHED_VOLUME_CONSUMPTION_COLLECTION_SCHEDULING_STATE")
    public Action<?, ?> attachedVolumeConsumptionCollectionSchedulingAction() {
        return new AbstractStackCreationAction<>(StackWithFingerprintsEvent.class) {
            @Override
            protected void doExecute(StackCreationContext context, StackWithFingerprintsEvent payload, Map<Object, Object> variables) {
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(StackCreationContext context) {
                return new AttachedVolumeConsumptionCollectionSchedulingRequest(context.getStackId());
            }
        };
    }

    @Bean(name = "STACK_CREATION_FINISHED_STATE")
    public Action<?, ?> stackCreationFinishedAction() {
        return new AbstractStackCreationAction<>(AttachedVolumeConsumptionCollectionSchedulingSuccess.class) {

            @Override
            protected void doExecute(StackCreationContext context, AttachedVolumeConsumptionCollectionSchedulingSuccess payload, Map<Object, Object> variables) {
                stackCreationService.stackCreationFinished(context.getStackId());
                StackView stackView = context.getStack();
                getMetricService().incrementMetricCounter(MetricType.STACK_CREATION_SUCCESSFUL, stackView);
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(StackCreationContext context) {
                return new StackEvent(StackCreationEvent.STACK_CREATION_FINISHED_EVENT.event(), context.getStackId());
            }
        };
    }

    @Bean(name = "STACK_CREATION_FAILED_STATE")
    public Action<?, ?> stackCreationFailureAction() {
        return new AbstractStackFailureAction<StackCreationState, StackCreationEvent>() {

            @Override
            protected void doExecute(StackFailureContext context, StackFailureEvent payload, Map<Object, Object> variables) {
                StackDto stackDto = stackDtoService.getById(context.getStackId());
                stackCreationService.handleStackCreationFailure(stackDto, payload.getException(), context.getProvisionType());
                getMetricService().incrementMetricCounter(MetricType.STACK_CREATION_FAILED, context.getStack(), payload.getException());
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(StackFailureContext context) {
                return new StackEvent(StackCreationEvent.STACKCREATION_FAILURE_HANDLED_EVENT.event(), context.getStackId());
            }
        };
    }

    private void handleValidationWarnings(StackCreationContext context, ValidationResult payload) {
        if (!CollectionUtils.isEmpty(payload.getWarningMessages())) {
            eventService.fireCloudbreakEvent(context.getStackId(), UPDATE_IN_PROGRESS.name(), ResourceEvent.CLOUD_PROVIDER_VALIDATION_WARNING,
                    payload.getWarningMessages());
        }
    }
}