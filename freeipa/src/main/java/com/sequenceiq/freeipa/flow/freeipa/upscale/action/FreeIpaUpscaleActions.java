package com.sequenceiq.freeipa.flow.freeipa.upscale.action;


import static com.sequenceiq.cloudbreak.util.NullUtil.getIfNotNullOtherwise;
import static com.sequenceiq.freeipa.flow.freeipa.upscale.UpscaleFlowEvent.FAIL_HANDLED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.upscale.UpscaleFlowEvent.IMAGE_FALLBACK_START_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.upscale.UpscaleFlowEvent.UPSCALE_FINISHED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.upscale.UpscaleFlowEvent.UPSCALE_RECORD_HOSTNAMES_FINISHED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.upscale.UpscaleFlowEvent.UPSCALE_SAVE_METADATA_FINISHED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.upscale.UpscaleFlowEvent.UPSCALE_STARTING_FINISHED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.upscale.UpscaleFlowEvent.UPSCALE_UPDATE_ENVIRONMENT_STACK_CONFIG_FAILED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.upscale.UpscaleFlowEvent.UPSCALE_UPDATE_ENVIRONMENT_STACK_CONFIG_FINISHED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.upscale.UpscaleFlowEvent.UPSCALE_UPDATE_KERBEROS_NAMESERVERS_CONFIG_FAILED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.upscale.UpscaleFlowEvent.UPSCALE_UPDATE_KERBEROS_NAMESERVERS_CONFIG_FINISHED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.upscale.UpscaleFlowEvent.UPSCALE_UPDATE_METADATA_FINISHED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.upscale.UpscaleFlowEvent.UPSCALE_VALIDATE_INSTANCES_FAILED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.upscale.UpscaleFlowEvent.UPSCALE_VALIDATE_INSTANCES_FINISHED_EVENT;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.LongStream;
import java.util.stream.StreamSupport;

import jakarta.inject.Inject;
import jakarta.ws.rs.ClientErrorException;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.cloud.event.instance.CollectMetadataRequest;
import com.sequenceiq.cloudbreak.cloud.event.instance.CollectMetadataResult;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmInstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmMetaDataStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.ResourceStatus;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.common.exception.WebApplicationExceptionMessageExtractor;
import com.sequenceiq.common.api.adjustment.AdjustmentTypeWithThreshold;
import com.sequenceiq.common.api.type.AdjustmentType;
import com.sequenceiq.environment.api.v1.environment.endpoint.EnvironmentEndpoint;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.flow.core.Flow;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.flow.core.PayloadConverter;
import com.sequenceiq.flow.reactor.api.event.DelayEvent;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.FailureDetails;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.SuccessDetails;
import com.sequenceiq.freeipa.converter.cloud.ResourceToCloudResourceConverter;
import com.sequenceiq.freeipa.converter.cloud.StackToCloudStackConverter;
import com.sequenceiq.freeipa.entity.InstanceGroup;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.Resource;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.freeipa.loadbalancer.event.update.LoadBalancerUpdateRequest;
import com.sequenceiq.freeipa.flow.freeipa.provision.event.bootstrap.BootstrapMachinesRequest;
import com.sequenceiq.freeipa.flow.freeipa.provision.event.bootstrap.BootstrapMachinesSuccess;
import com.sequenceiq.freeipa.flow.freeipa.provision.event.cloudstorage.ValidateCloudStorageRequest;
import com.sequenceiq.freeipa.flow.freeipa.provision.event.clusterproxy.ClusterProxyUpdateRegistrationRequest;
import com.sequenceiq.freeipa.flow.freeipa.provision.event.clusterproxy.ClusterProxyUpdateRegistrationSuccess;
import com.sequenceiq.freeipa.flow.freeipa.provision.event.hostmetadatasetup.HostMetadataSetupSuccess;
import com.sequenceiq.freeipa.flow.freeipa.provision.event.orchestrator.OrchestratorConfigRequest;
import com.sequenceiq.freeipa.flow.freeipa.provision.event.postinstall.PostInstallFreeIpaRequest;
import com.sequenceiq.freeipa.flow.freeipa.provision.event.postinstall.PostInstallFreeIpaSuccess;
import com.sequenceiq.freeipa.flow.freeipa.provision.event.services.InstallFreeIpaServicesRequest;
import com.sequenceiq.freeipa.flow.freeipa.provision.event.services.InstallFreeIpaServicesSuccess;
import com.sequenceiq.freeipa.flow.freeipa.upscale.ImageFallbackSuccessToUpscaleCreateUserdataSecretsSuccessConverter;
import com.sequenceiq.freeipa.flow.freeipa.upscale.UpscaleFlowEvent;
import com.sequenceiq.freeipa.flow.freeipa.upscale.UpscaleState;
import com.sequenceiq.freeipa.flow.freeipa.upscale.event.UpscaleCreateUserdataSecretsRequest;
import com.sequenceiq.freeipa.flow.freeipa.upscale.event.UpscaleCreateUserdataSecretsSuccess;
import com.sequenceiq.freeipa.flow.freeipa.upscale.event.UpscaleEvent;
import com.sequenceiq.freeipa.flow.freeipa.upscale.event.UpscaleFailureEvent;
import com.sequenceiq.freeipa.flow.freeipa.upscale.event.UpscaleStackImageFallbackResult;
import com.sequenceiq.freeipa.flow.freeipa.upscale.event.UpscaleStackRequest;
import com.sequenceiq.freeipa.flow.freeipa.upscale.event.UpscaleStackResult;
import com.sequenceiq.freeipa.flow.freeipa.upscale.event.UpscaleUpdateUserdataSecretsRequest;
import com.sequenceiq.freeipa.flow.freeipa.upscale.event.UpscaleUpdateUserdataSecretsSuccess;
import com.sequenceiq.freeipa.flow.freeipa.upscale.event.ValidateInstancesHealthEvent;
import com.sequenceiq.freeipa.flow.freeipa.upscale.failure.BootstrapMachinesFailedToUpscaleFailureEventConverter;
import com.sequenceiq.freeipa.flow.freeipa.upscale.failure.ClusterProxyRegistrationFailedToUpscaleFailureEventConverter;
import com.sequenceiq.freeipa.flow.freeipa.upscale.failure.ClusterProxyUpdateRegistrationFailedToUpscaleFailureEventConverter;
import com.sequenceiq.freeipa.flow.freeipa.upscale.failure.CollectMetadataResultToUpscaleFailureEventConverter;
import com.sequenceiq.freeipa.flow.freeipa.upscale.failure.HostMetadataSetupFailedToUpscaleFailureEventConverter;
import com.sequenceiq.freeipa.flow.freeipa.upscale.failure.ImageFallbackFailedToUpscaleFailureEventConverter;
import com.sequenceiq.freeipa.flow.freeipa.upscale.failure.InstallFreeIpaServicesFailedToUpscaleFailureEventConverter;
import com.sequenceiq.freeipa.flow.freeipa.upscale.failure.LoadBalancerUpdateFailureEventToUpscaleFailureEventConverter;
import com.sequenceiq.freeipa.flow.freeipa.upscale.failure.PostInstallFreeIpaFailedToUpscaleFailureEventConverter;
import com.sequenceiq.freeipa.flow.freeipa.upscale.failure.StackFailureEventToUpscaleFailureEventConverter;
import com.sequenceiq.freeipa.flow.freeipa.upscale.failure.UpscaleStackResultToUpscaleFailureEventConverter;
import com.sequenceiq.freeipa.flow.freeipa.upscale.failure.ValidateCloudStorageFailedToUpscaleFailureEventConverter;
import com.sequenceiq.freeipa.flow.stack.StackContext;
import com.sequenceiq.freeipa.flow.stack.StackEvent;
import com.sequenceiq.freeipa.flow.stack.provision.action.AbstractStackProvisionAction;
import com.sequenceiq.freeipa.flow.stack.provision.event.clusterproxy.ClusterProxyRegistrationRequest;
import com.sequenceiq.freeipa.flow.stack.provision.event.clusterproxy.ClusterProxyRegistrationSuccess;
import com.sequenceiq.freeipa.flow.stack.provision.event.imagefallback.ImageFallbackFailed;
import com.sequenceiq.freeipa.flow.stack.provision.event.imagefallback.ImageFallbackRequest;
import com.sequenceiq.freeipa.service.EnvironmentService;
import com.sequenceiq.freeipa.service.TlsSetupService;
import com.sequenceiq.freeipa.service.client.CachedEnvironmentClientService;
import com.sequenceiq.freeipa.service.config.KerberosConfigUpdateService;
import com.sequenceiq.freeipa.service.image.ImageFallbackService;
import com.sequenceiq.freeipa.service.operation.OperationService;
import com.sequenceiq.freeipa.service.resource.ResourceService;
import com.sequenceiq.freeipa.service.secret.UserdataSecretsService;
import com.sequenceiq.freeipa.service.stack.InstanceGroupAttributeAndStackTemplateUpdater;
import com.sequenceiq.freeipa.service.stack.StackUpdater;
import com.sequenceiq.freeipa.service.stack.instance.InstanceGroupService;
import com.sequenceiq.freeipa.service.stack.instance.InstanceMetaDataService;
import com.sequenceiq.freeipa.service.stack.instance.InstanceValidationService;
import com.sequenceiq.freeipa.service.stack.instance.MetadataSetupService;

@Configuration
public class FreeIpaUpscaleActions {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIpaUpscaleActions.class);

    private static final String IMAGE_FALLBACK_STARTED = "IMAGE_FALLBACK_STARTED";

    private static final String NEW_INSTANCE_ENTITY_IDS = "NEW_INSTANCE_ENTITY_IDS";

    private static final String SECRET_ENCRYPTION_ENABLED = "SECRET_ENCRYPTION_ENABLED";

    @Inject
    private ResourceService resourceService;

    @Inject
    private InstanceMetaDataService instanceMetaDataService;

    @Inject
    private StackToCloudStackConverter cloudStackConverter;

    @Inject
    private ResourceToCloudResourceConverter resourceConverter;

    @Inject
    private MetadataSetupService metadataSetupService;

    @Inject
    private TlsSetupService tlsSetupService;

    @Inject
    private StackUpdater stackUpdater;

    @Inject
    private InstanceGroupService instanceGroupService;

    @Inject
    private PrivateIdProvider privateIdProvider;

    @Inject
    private ImageFallbackService imageFallbackService;

    @Inject
    private CachedEnvironmentClientService cachedEnvironmentClientService;

    @Inject
    private UserdataSecretsService userdataSecretsService;

    @Bean(name = "UPSCALE_STARTING_STATE")
    public Action<?, ?> startingAction() {
        return new AbstractUpscaleAction<>(UpscaleEvent.class) {

            @Inject
            private InstanceGroupAttributeAndStackTemplateUpdater instanceGroupAttributeAndStackTemplateUpdater;

            @Override
            protected void prepareExecution(UpscaleEvent payload, Map<Object, Object> variables) {
                if (payload.getTriggeredVariant() != null) {
                    variables.put(TRIGGERED_VARIANT, payload.getTriggeredVariant());
                }
            }

            @Override
            protected void doExecute(StackContext context, UpscaleEvent payload, Map<Object, Object> variables) {
                Stack stack = context.getStack();
                LOGGER.info("Starting upscale {}", payload);
                stackUpdater.updateStackStatus(stack, getInProgressStatus(variables), "Starting upscale");
                String operationId = payload.getOperationId();
                setOperationId(variables, operationId);
                setInstanceCountByGroup(variables, payload.getInstanceCountByGroup());
                setRepair(variables, payload.getRepair());
                setChainedAction(variables, payload.isChained());
                setFinalChain(variables, payload.isFinalChain());
                setInstanceIds(variables, payload.getInstanceIds());
                instanceGroupAttributeAndStackTemplateUpdater.updateInstanceGroupAttributesAndTemplateIfDefaultDifferent(context, stack);
                sendEvent(context, UPSCALE_STARTING_FINISHED_EVENT.selector(), new StackEvent(stack.getId()));
            }

        };
    }

    @Bean(name = "UPSCALE_CREATE_USERDATA_SECRETS_STATE")
    public Action<?, ?> createUserdataSecretsAction() {
        return new AbstractUpscaleAction<>(StackEvent.class) {

            @Override
            protected void doExecute(StackContext context, StackEvent payload, Map<Object, Object> variables) {
                Stack stack = context.getStack();
                DetailedEnvironmentResponse environment = cachedEnvironmentClientService.getByCrn(stack.getEnvironmentCrn());
                if (environment.isEnableSecretEncryption()) {
                    variables.put(SECRET_ENCRYPTION_ENABLED, Boolean.TRUE);
                    stackUpdater.updateStackStatus(stack, getInProgressStatus(variables), "Creating userdata secrets");
                    int instanceCountByGroup = getInstanceCountByGroup(variables);
                    int numberOfSecretsToCreate = stack.getInstanceGroups().stream()
                            .map(ig -> instanceCountByGroup - ig.getNotDeletedInstanceMetaDataSet().size())
                            .reduce(0, Integer::sum);
                    LOGGER.info("Going to create [{}] new userdata secrets for stack [{}].", numberOfSecretsToCreate, stack.getName());
                    long firstValidPrivateId = privateIdProvider.getFirstValidPrivateId(stack.getInstanceGroups());
                    List<Long> newPrivateIds = LongStream.range(firstValidPrivateId, firstValidPrivateId + numberOfSecretsToCreate).boxed().toList();
                    UpscaleCreateUserdataSecretsRequest request = new UpscaleCreateUserdataSecretsRequest(stack.getId(), context.getCloudContext(),
                            context.getCloudCredential(), newPrivateIds);
                    sendEvent(context, request.selector(), request);
                } else {
                    variables.put(SECRET_ENCRYPTION_ENABLED, Boolean.FALSE);
                    LOGGER.info("Skipping userdata secret creation, since secret encryption is not enabled.");
                    sendEvent(context, new UpscaleCreateUserdataSecretsSuccess(stack.getId(), List.of()));
                }
            }
        };
    }

    @Bean(name = "UPSCALE_ADD_INSTANCES_STATE")
    public Action<?, ?> addInstancesAction() {
        return new AbstractUpscaleAction<>(UpscaleCreateUserdataSecretsSuccess.class) {

            @Override
            protected void doExecute(StackContext context, UpscaleCreateUserdataSecretsSuccess payload, Map<Object, Object> variables) {
                Stack stack = context.getStack();
                stackUpdater.updateStackStatus(stack, getInProgressStatus(variables), "Adding instances");

                List<CloudInstance> newInstances = buildNewInstances(context.getStack(), getInstanceCountByGroup(variables));
                LOGGER.debug("Freeipa upscale new instances: {}", newInstances);
                if (newInstances.isEmpty()) {
                    skipAddingNewInstances(context, stack.getId());
                } else {
                    List<Long> createdSecretResourceIds = payload.getCreatedSecretResourceIds();
                    addNewInstances(context, stack, newInstances, createdSecretResourceIds, variables);
                }
            }

            private void skipAddingNewInstances(StackContext context, Long stackId) {
                List<CloudResourceStatus> list = resourceService.getAllAsCloudResourceStatus(stackId);
                UpscaleStackResult result = new UpscaleStackResult(stackId, ResourceStatus.CREATED, list);
                sendEvent(context, result.selector(), result);
            }

            private void addNewInstances(StackContext context, Stack stack, List<CloudInstance> newInstances, List<Long> createdSecretResourceIds,
                    Map<Object, Object> variables) {
                List<String> instanceIdsToRemove = Optional.ofNullable(getInstanceIds(variables)).orElseGet(ArrayList::new);
                List<InstanceMetaData> instancesToRemove = stack.getInstanceGroups().stream().flatMap(ig -> ig.getInstanceMetaData().stream())
                        .filter(im -> instanceIdsToRemove.contains(im.getInstanceId()) && Objects.nonNull(im.getSubnetId()))
                        .collect(Collectors.toList());
                LOGGER.debug("Instances to replace and keep the AZ: {}", instancesToRemove);
                Stack updatedStack = instanceMetaDataService.saveInstanceAndGetUpdatedStack(stack, newInstances, instancesToRemove);
                assignNewSecretsToNewInstances(updatedStack, createdSecretResourceIds, variables);
                List<CloudResource> cloudResources = resourceService.findAllByStackId(stack.getId()).stream()
                        .map(resource -> resourceConverter.convert(resource))
                        .collect(Collectors.toList());
                CloudStack updatedCloudStack = cloudStackConverter.convert(updatedStack);
                Optional<String> fallbackImage = imageFallbackService.determineFallbackImageIfPermitted(context);
                UpscaleStackRequest<UpscaleStackResult> request = new UpscaleStackRequest<>(context.getCloudContext(), context.getCloudCredential(),
                        updatedCloudStack, cloudResources, new AdjustmentTypeWithThreshold(AdjustmentType.EXACT, (long) newInstances.size()), fallbackImage);
                sendEvent(context, request.selector(), request);
            }

            private List<CloudInstance> buildNewInstances(Stack stack, int instanceCountByGroup) {
                long privateId = privateIdProvider.getFirstValidPrivateId(stack.getInstanceGroups());
                List<CloudInstance> newInstances = new ArrayList<>();
                for (InstanceGroup instanceGroup : stack.getInstanceGroups()) {
                    int remainingInstances = instanceCountByGroup - instanceGroup.getNotDeletedInstanceMetaDataSet().size();
                    for (long i = 0; i < remainingInstances; ++i) {
                        newInstances.add(cloudStackConverter.buildInstance(stack, null, instanceGroup,
                                stack.getStackAuthentication(), privateId++, InstanceStatus.CREATE_REQUESTED));
                    }
                }
                return newInstances;
            }

            private void assignNewSecretsToNewInstances(Stack stack, List<Long> createdSecretResourceIds, Map<Object, Object> variables) {
                if (!createdSecretResourceIds.isEmpty()) {
                    List<Resource> secretResources = StreamSupport.stream(
                            resourceService.findAllByResourceId(createdSecretResourceIds).spliterator(), false).toList();
                    List<InstanceMetaData> newInstanceMetadas = stack.getInstanceGroups().stream()
                            .flatMap(ig -> ig.getNotDeletedInstanceMetaDataSet().stream())
                            .filter(imd -> imd.getInstanceId() == null && imd.getUserdataSecretResourceId() == null)
                            .toList();
                    variables.put(NEW_INSTANCE_ENTITY_IDS, newInstanceMetadas.stream().map(InstanceMetaData::getId).toList());
                    userdataSecretsService.assignSecretsToInstances(stack, secretResources, newInstanceMetadas);
                }
            }

            @Override
            protected void initPayloadConverterMap(List<PayloadConverter<UpscaleCreateUserdataSecretsSuccess>> payloadConverters) {
                payloadConverters.add(new ImageFallbackSuccessToUpscaleCreateUserdataSecretsSuccessConverter());
            }
        };
    }

    @Bean(name = "FREEIPA_UPSCALE_IMAGE_FALLBACK_STATE")
    public Action<?, ?> imageFallbackAction() {
        return new AbstractUpscaleAction<>(UpscaleStackImageFallbackResult.class) {
            @Override
            protected void doExecute(StackContext context, UpscaleStackImageFallbackResult payload, Map<Object, Object> variables) {
                if ((Boolean) variables.getOrDefault(IMAGE_FALLBACK_STARTED, Boolean.FALSE)) {
                    LOGGER.warn("Image fallback already happened at least once! Failing flow to avoid infinite loop!");
                    sendEvent(context, new ImageFallbackFailed(payload.getResourceId(), new Exception("Image fallback started second time!")));
                } else {
                    Stack stack = context.getStack();
                    String notificationMessage = payload.getNotificationMessage();
                    stackUpdater.updateStackStatus(stack, getInProgressStatus(variables),
                            StringUtils.isEmpty(notificationMessage) ? "Image fallback initiated" : notificationMessage);
                    instanceMetaDataService.updateInstanceStatusOnUpscaleFailure(stack.getNotDeletedInstanceMetaDataSet());

                    sendEvent(context, new StackEvent(IMAGE_FALLBACK_START_EVENT.event(), payload.getResourceId()));
                }
            }
        };
    }

    @Bean(name = "FREEIPA_UPSCALE_IMAGE_FALLBACK_START_STATE")
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

    @Bean(name = "UPSCALE_VALIDATE_INSTANCES_STATE")
    public Action<?, ?> validateInstancesAction() {
        return new AbstractUpscaleAction<>(UpscaleStackResult.class) {

            @Inject
            private InstanceValidationService instanceValidationService;

            @Override
            protected void doExecute(StackContext context, UpscaleStackResult payload, Map<Object, Object> variables) {
                Stack stack = context.getStack();
                stackUpdater.updateStackStatus(stack, getInProgressStatus(variables), "Validating new instances");
                try {
                    instanceValidationService.finishAddInstances(context, payload);
                    sendEvent(context, UPSCALE_VALIDATE_INSTANCES_FINISHED_EVENT.selector(), new StackEvent(stack.getId()));
                } catch (Exception e) {
                    LOGGER.error("Failed to validate the instances", e);
                    sendEvent(context, UPSCALE_VALIDATE_INSTANCES_FAILED_EVENT.selector(),
                            new UpscaleFailureEvent(stack.getId(), "Validating new instances", Set.of(), Map.of(), e));
                }
            }

        };
    }

    @Bean(name = "UPSCALE_EXTEND_METADATA_STATE")
    public Action<?, ?> extendMetadataAction() {
        return new AbstractUpscaleAction<>(StackEvent.class) {

            private final Set<InstanceStatus> unusedInstanceStatuses = Set.of(InstanceStatus.CREATE_REQUESTED, InstanceStatus.CREATED);

            @Override
            protected void doExecute(StackContext context, StackEvent payload, Map<Object, Object> variables) {
                Stack stack = context.getStack();
                stackUpdater.updateStackStatus(stack, getInProgressStatus(variables), "Extending metadata");

                List<CloudInstance> allKnownInstances = cloudStackConverter.buildInstances(stack);
                List<Resource> resources = resourceService.findAllByStackId(stack.getId());
                List<CloudResource> cloudResources = resources.stream()
                        .map(r -> resourceConverter.convert(r))
                        .collect(Collectors.toList());
                List<CloudInstance> newCloudInstances = allKnownInstances.stream()
                        .filter(this::isNewInstances)
                        .collect(Collectors.toList());
                CollectMetadataRequest request = new CollectMetadataRequest(context.getCloudContext(), context.getCloudCredential(), cloudResources,
                        newCloudInstances, allKnownInstances);

                sendEvent(context, request.selector(), request);
            }

            private boolean isNewInstances(CloudInstance cloudInstance) {
                return unusedInstanceStatuses.contains(cloudInstance.getTemplate().getStatus());
            }
        };
    }

    @Bean(name = "UPSCALE_SAVE_METADATA_STATE")
    public Action<?, ?> saveMetadataAction() {
        return new AbstractUpscaleAction<>(CollectMetadataResult.class) {

            @Override
            protected void doExecute(StackContext context, CollectMetadataResult payload, Map<Object, Object> variables) {
                Stack stack = context.getStack();
                stackUpdater.updateStackStatus(stack, getInProgressStatus(variables), "Saving metadata");
                List<String> instanceIds = payload.getResults().stream()
                        .map(CloudVmMetaDataStatus::getCloudVmInstanceStatus)
                        .map(CloudVmInstanceStatus::getCloudInstance)
                        .map(CloudInstance::getInstanceId)
                        .collect(Collectors.toList());
                setInstanceIds(variables, instanceIds);
                metadataSetupService.saveInstanceMetaData(stack, payload.getResults(),
                        com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceStatus.CREATED);
                sendEvent(context, UPSCALE_SAVE_METADATA_FINISHED_EVENT.selector(), new StackEvent(stack.getId()));
            }
        };
    }

    @Bean(name = "UPSCALE_UPDATE_USERDATA_SECRETS_STATE")
    public Action<?, ?> updateUserdataSecretsAction() {
        return new AbstractUpscaleAction<>(StackEvent.class) {

            @Override
            protected void doExecute(StackContext context, StackEvent payload, Map<Object, Object> variables) {
                Stack stack = context.getStack();
                if ((Boolean) variables.getOrDefault(SECRET_ENCRYPTION_ENABLED, Boolean.FALSE)) {
                    stackUpdater.updateStackStatus(stack, getInProgressStatus(variables), "Updating userdata secrets");
                    List<Long> newInstanceIds = getIfNotNullOtherwise(variables.remove(NEW_INSTANCE_ENTITY_IDS), x -> (List<Long>) x, List.of());
                    UpscaleUpdateUserdataSecretsRequest request = new UpscaleUpdateUserdataSecretsRequest(stack.getId(), context.getCloudContext(),
                            context.getCloudCredential(), newInstanceIds);
                    sendEvent(context, request.selector(), request);
                } else {
                    sendEvent(context, new UpscaleUpdateUserdataSecretsSuccess(stack.getId()));
                }
            }
        };
    }

    @Bean(name = "UPSCALE_TLS_SETUP_STATE")
    public Action<?, ?> tlsSetupAction() {
        return new AbstractUpscaleAction<>(StackEvent.class) {

            @Override
            protected void doExecute(StackContext context, StackEvent payload, Map<Object, Object> variables) {
                Stack stack = context.getStack();
                stackUpdater.updateStackStatus(stack, getInProgressStatus(variables), "Setting up TLS");

                StackEvent event;
                try {
                    if (!stack.getTunnel().useCcm()) {
                        Set<InstanceMetaData> newInstancesMetaData = stack.getInstanceGroups().stream()
                                .flatMap(instanceGroup -> instanceGroup.getAllInstanceMetaData().stream())
                                .filter(this::isNewInstancesWithoutTlsCert)
                                .collect(Collectors.toSet());
                        for (InstanceMetaData gwInstance : newInstancesMetaData) {
                            tlsSetupService.setupTls(stack.getId(), gwInstance);
                        }
                    }
                    event = new StackEvent(UpscaleFlowEvent.UPSCALE_TLS_SETUP_FINISHED_EVENT.event(), stack.getId());
                } catch (Exception e) {
                    LOGGER.error("Failed to setup TLS", e);
                    event = new UpscaleFailureEvent(stack.getId(), "Setting ups TLS", Set.of(), Map.of(), e);
                }
                sendEvent(context, event.selector(), event);
            }

            private boolean isNewInstancesWithoutTlsCert(InstanceMetaData instanceMetaData) {
                return instanceMetaData.getInstanceStatus().equals(com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceStatus.CREATED) &&
                        Objects.isNull(instanceMetaData.getServerCert());
            }

        };
    }

    @Bean(name = "UPSCALE_UPDATE_CLUSTERPROXY_REGISTRATION_PRE_BOOTSTRAP_STATE")
    public Action<?, ?> updateClusterProxyRegistrationPreBootstrapAction() {
        return new AbstractUpscaleAction<>(StackEvent.class) {
            @Override
            protected void doExecute(StackContext context, StackEvent payload, Map<Object, Object> variables) {
                Stack stack = context.getStack();
                stackUpdater.updateStackStatus(stack, getInProgressStatus(variables), "Update cluster proxy registration before bootstrap");
                List<String> instanceIds = getInstanceIds(variables);
                Set<InstanceMetaData> newInstances = instanceMetaDataService.getByInstanceIds(stack.getId(), instanceIds);
                boolean allNewInstanceHasFqdn = newInstances.stream().allMatch(im -> StringUtils.isNotBlank(im.getDiscoveryFQDN()));
                Selectable event = allNewInstanceHasFqdn ? new ClusterProxyRegistrationRequest(stack.getId())
                        : new ClusterProxyRegistrationSuccess(stack.getId());
                sendEvent(context, event.selector(), event);
            }
        };
    }

    @Bean(name = "UPSCALE_BOOTSTRAPPING_MACHINES_STATE")
    public Action<?, ?> bootstrappingMachinesAction() {
        return new AbstractUpscaleAction<>(ClusterProxyRegistrationSuccess.class) {
            @Override
            protected void doExecute(StackContext context, ClusterProxyRegistrationSuccess payload, Map<Object, Object> variables) {
                Stack stack = context.getStack();
                stackUpdater.updateStackStatus(stack, getInProgressStatus(variables), "Bootstrapping machines");
                BootstrapMachinesRequest request = new BootstrapMachinesRequest(stack.getId());
                sendEvent(context, request.selector(), request);
            }
        };
    }

    @Bean(name = "UPSCALE_COLLECTING_HOST_METADATA_STATE")
    public Action<?, ?> collectingHostMetadataAction() {
        return new AbstractUpscaleAction<>(BootstrapMachinesSuccess.class) {
            @Override
            protected void doExecute(StackContext context, BootstrapMachinesSuccess payload, Map<Object, Object> variables) {
                Stack stack = context.getStack();
                // UNUSED STEP
                HostMetadataSetupSuccess request = new HostMetadataSetupSuccess(stack.getId());
                sendEvent(context, request.selector(), request);
            }
        };
    }

    @Bean(name = "UPSCALE_RECORD_HOSTNAMES_STATE")
    public Action<?, ?> recordHostnamesAction() {
        return new AbstractUpscaleAction<>(HostMetadataSetupSuccess.class) {
            @Override
            protected void doExecute(StackContext context, HostMetadataSetupSuccess payload, Map<Object, Object> variables) {
                Stack stack = context.getStack();
                stackUpdater.updateStackStatus(stack, getInProgressStatus(variables), "Recording hostnames");
                List<String> instanceIds = getInstanceIds(variables);
                List<String> hosts = stack.getNotDeletedInstanceMetaDataList().stream()
                        .filter(instanceMetaData -> instanceIds.contains(instanceMetaData.getInstanceId()))
                        .map(InstanceMetaData::getDiscoveryFQDN)
                        .collect(Collectors.toList());
                setUpscaleHosts(variables, hosts);
                sendEvent(context, UPSCALE_RECORD_HOSTNAMES_FINISHED_EVENT.selector(), new StackEvent(stack.getId()));
            }
        };
    }

    @Bean(name = "UPSCALE_ORCHESTRATOR_CONFIG_STATE")
    public Action<?, ?> orchestratorConfig() {
        return new AbstractUpscaleAction<>(StackEvent.class) {
            @Override
            protected void doExecute(StackContext context, StackEvent payload, Map<Object, Object> variables) {
                Stack stack = context.getStack();
                stackUpdater.updateStackStatus(stack, getInProgressStatus(variables), "Configuring the orchestrator");
                OrchestratorConfigRequest request = new OrchestratorConfigRequest(stack.getId());
                sendEvent(context, request.selector(), request);
            }
        };
    }

    @Bean(name = "UPSCALE_VALIDATING_CLOUD_STORAGE_STATE")
    public Action<?, ?> validateFreeIpaCloudStorage() {
        return new AbstractUpscaleAction<>(StackEvent.class) {
            @Override
            protected void doExecute(StackContext context, StackEvent payload, Map<Object, Object> variables) {
                Stack stack = context.getStack();
                stackUpdater.updateStackStatus(stack, getInProgressStatus(variables), "Validating cloud storage");
                ValidateCloudStorageRequest request = new ValidateCloudStorageRequest(stack.getId());
                sendEvent(context, request.selector(), request);
            }
        };
    }

    @Bean(name = "UPSCALE_FREEIPA_INSTALL_STATE")
    public Action<?, ?> installFreeIpaAction() {
        return new AbstractUpscaleAction<>(StackEvent.class) {
            @Override
            protected void doExecute(StackContext context, StackEvent payload, Map<Object, Object> variables) {
                Stack stack = context.getStack();
                stackUpdater.updateStackStatus(stack, getInProgressStatus(variables), "Installing FreeIPA");
                InstallFreeIpaServicesRequest request = new InstallFreeIpaServicesRequest(stack.getId());
                sendEvent(context, request.selector(), request);
            }
        };
    }

    @Bean(name = "UPSCALE_UPDATE_CLUSTERPROXY_REGISTRATION_STATE")
    public Action<?, ?> updateClusterProxyRegistrationAction() {
        return new AbstractUpscaleAction<>(InstallFreeIpaServicesSuccess.class) {
            @Override
            protected void doExecute(StackContext context, InstallFreeIpaServicesSuccess payload, Map<Object, Object> variables) {
                Stack stack = context.getStack();
                stackUpdater.updateStackStatus(stack, getInProgressStatus(variables), "Update cluster proxy registration after bootstrap");
                ClusterProxyUpdateRegistrationRequest request = new ClusterProxyUpdateRegistrationRequest(stack.getId());
                sendEvent(context, request.selector(), request);
            }
        };
    }

    @Bean(name = "UPSCALE_FREEIPA_POST_INSTALL_STATE")
    public Action<?, ?> freeIpaPostInstallAction() {
        return new AbstractUpscaleAction<>(ClusterProxyUpdateRegistrationSuccess.class) {
            @Override
            protected void doExecute(StackContext context, ClusterProxyUpdateRegistrationSuccess payload, Map<Object, Object> variables) {
                Stack stack = context.getStack();
                stackUpdater.updateStackStatus(stack, getInProgressStatus(variables), "FreeIPA Post Installation");
                PostInstallFreeIpaRequest request = new PostInstallFreeIpaRequest(stack.getId(), false);
                sendEvent(context, request.selector(), request);
            }
        };
    }

    @Bean(name = "UPSCALE_UPDATE_METADATA_STATE")
    public Action<?, ?> updateMetadataAction() {
        return new AbstractUpscaleAction<>(PostInstallFreeIpaSuccess.class) {
            @Override
            protected void doExecute(StackContext context, PostInstallFreeIpaSuccess payload, Map<Object, Object> variables) {
                Stack stack = context.getStack();
                stackUpdater.updateStackStatus(stack, getInProgressStatus(variables), "Upscale update metadata");
                if (!isRepair(variables)) {
                    int nodeCount = getInstanceCountByGroup(variables);
                    for (InstanceGroup instanceGroup : stack.getInstanceGroups()) {
                        instanceGroup.setNodeCount(nodeCount);
                        instanceGroupService.save(instanceGroup);
                    }
                }
                sendEvent(context, UPSCALE_UPDATE_METADATA_FINISHED_EVENT.selector(), new StackEvent(stack.getId()));
            }
        };
    }

    @Bean(name = "UPSCALE_VALIDATE_NEW_INSTANCES_HEALTH_STATE")
    public Action<?, ?> validateNewInstanceAction() {
        return new AbstractUpscaleAction<>(StackEvent.class) {
            @Override
            protected void doExecute(StackContext context, StackEvent payload, Map<Object, Object> variables) {
                sendEvent(context, new ValidateInstancesHealthEvent(payload.getResourceId(), getInstanceIds(variables)));
            }
        };
    }

    @Bean(name = "UPSCALE_UPDATE_KERBEROS_NAMESERVERS_CONFIG_STATE")
    public Action<?, ?> updateKerberosNameserversConfigAction() {
        return new AbstractUpscaleAction<>(StackEvent.class) {
            @Inject
            private KerberosConfigUpdateService kerberosConfigUpdateService;

            @Override
            protected void doExecute(StackContext context, StackEvent payload, Map<Object, Object> variables) {
                Stack stack = context.getStack();
                stackUpdater.updateStackStatus(stack, getInProgressStatus(variables), "Updating kerberos nameserver config");
                try {
                    kerberosConfigUpdateService.updateNameservers(stack.getId());
                    sendEvent(context, UPSCALE_UPDATE_KERBEROS_NAMESERVERS_CONFIG_FINISHED_EVENT.selector(), new StackEvent(stack.getId()));
                } catch (Exception e) {
                    LOGGER.error("Failed to update the kerberos nameserver config", e);
                    sendEvent(context, UPSCALE_UPDATE_KERBEROS_NAMESERVERS_CONFIG_FAILED_EVENT.selector(),
                            new UpscaleFailureEvent(stack.getId(), "Updating kerberos nameserver config", Set.of(), Map.of(), e));
                }
            }
        };
    }

    @Bean(name = "UPSCALE_UPDATE_LOAD_BALANCER_STATE")
    public Action<?, ?> updateLoadBalancerAction() {
        return new AbstractUpscaleAction<>(StackEvent.class) {
            @Override
            protected void doExecute(StackContext context, StackEvent payload, Map<Object, Object> variables) {
                Stack stack = context.getStack();
                stackUpdater.updateStackStatus(stack, getInProgressStatus(variables), "Updating FreeIPA load balancer");
                sendEvent(context,
                        new LoadBalancerUpdateRequest(stack.getId(), context.getCloudContext(), context.getCloudCredential(), context.getCloudStack()));
            }
        };
    }

    @Bean(name = "UPSCALE_UPDATE_ENVIRONMENT_STACK_CONFIG_STATE")
    public Action<?, ?> updateEnvironmentStackConfigAction() {
        return new AbstractUpscaleAction<>(StackEvent.class) {
            @Inject
            private EnvironmentEndpoint environmentEndpoint;

            @Inject
            private WebApplicationExceptionMessageExtractor webApplicationExceptionMessageExtractor;

            @Value("${freeipa.delayed.scale-sec}")
            private long delayInSec;

            @Override
            protected void doExecute(StackContext context, StackEvent payload, Map<Object, Object> variables) {
                Stack stack = context.getStack();
                stackUpdater.updateStackStatus(stack, getInProgressStatus(variables), "Updating environment stack config");
                try {
                    ThreadBasedUserCrnProvider.doAsInternalActor(
                            () -> environmentEndpoint.updateConfigsInEnvironmentByCrn(stack.getEnvironmentCrn()));
                    if (isChainedAction(variables) && !isFinalChain(variables)) {
                        sendEvent(context, new DelayEvent(stack.getId(),
                                new StackEvent(UPSCALE_UPDATE_ENVIRONMENT_STACK_CONFIG_FINISHED_EVENT.selector(), stack.getId()), delayInSec, true));
                    } else {
                        sendEvent(context, UPSCALE_UPDATE_ENVIRONMENT_STACK_CONFIG_FINISHED_EVENT.selector(), new StackEvent(stack.getId()));
                    }
                } catch (ClientErrorException e) {
                    String errorMessage = webApplicationExceptionMessageExtractor.getErrorMessage(e);
                    LOGGER.error("Failed to update the stack config due to {}", errorMessage, e);
                    sendEvent(context, UPSCALE_UPDATE_ENVIRONMENT_STACK_CONFIG_FAILED_EVENT.selector(),
                            new UpscaleFailureEvent(stack.getId(), "Updating environment stack config", Set.of(), Map.of(), e));
                } catch (Exception e) {
                    LOGGER.error("Failed to update the stack config", e);
                    sendEvent(context, UPSCALE_UPDATE_ENVIRONMENT_STACK_CONFIG_FAILED_EVENT.selector(),
                            new UpscaleFailureEvent(stack.getId(), "Updating environment stack config", Set.of(), Map.of(), e));
                }
            }
        };
    }

    @Bean(name = "UPSCALE_FINISHED_STATE")
    public Action<?, ?> upscaleFinsihedAction() {
        return new AbstractUpscaleAction<>(StackEvent.class) {
            @Inject
            private OperationService operationService;

            @Inject
            private EnvironmentService environmentService;

            @Override
            protected void doExecute(StackContext context, StackEvent payload, Map<Object, Object> variables) {
                Stack stack = context.getStack();
                stackUpdater.updateStackStatus(stack, getUpscaleCompleteStatus(variables), "Upscale complete");
                if (!isChainedAction(variables)) {
                    environmentService.setFreeIpaNodeCount(stack.getEnvironmentCrn(), stack.getNotDeletedInstanceMetaDataList().size());
                }
                if (shouldCompleteOperation(variables)) {
                    SuccessDetails successDetails = new SuccessDetails(stack.getEnvironmentCrn());
                    successDetails.getAdditionalDetails().put("Hosts", getUpscaleHosts(variables));
                    operationService.completeOperation(stack.getAccountId(), getOperationId(variables), List.of(successDetails), Collections.emptyList());
                }
                sendEvent(context, UPSCALE_FINISHED_EVENT.selector(), new StackEvent(stack.getId()));
            }
        };
    }

    @Bean(name = "UPSCALE_FAIL_STATE")
    public Action<?, ?> upscaleFailureAction() {
        return new AbstractUpscaleAction<>(UpscaleFailureEvent.class) {

            @Inject
            private OperationService operationService;

            @Inject
            private InstanceMetaDataService instanceMetaDataService;

            @Override
            protected StackContext createFlowContext(FlowParameters flowParameters, StateContext<UpscaleState, UpscaleFlowEvent> stateContext,
                    UpscaleFailureEvent payload) {
                Flow flow = getFlow(flowParameters.getFlowId());
                flow.setFlowFailed(payload.getException());
                return super.createFlowContext(flowParameters, stateContext, payload);
            }

            @Override
            protected void doExecute(StackContext context, UpscaleFailureEvent payload, Map<Object, Object> variables) {
                LOGGER.error("Upscale failed with payload: {}", payload);
                Stack stack = context.getStack();
                String errorReason = getErrorReason(payload.getException());
                stackUpdater.updateStackStatus(context.getStack(), getFailedStatus(variables), errorReason);
                String environmentCrn = stack.getEnvironmentCrn();
                SuccessDetails successDetails = new SuccessDetails(environmentCrn);
                successDetails.getAdditionalDetails()
                        .put(payload.getFailedPhase(), payload.getSuccess() == null ? List.of() : new ArrayList<>(payload.getSuccess()));
                String message = "Upscale failed during [" + payload.getFailedPhase() + "]. Reason: " + errorReason;
                FailureDetails failureDetails = new FailureDetails(environmentCrn, message);
                if (payload.getFailureDetails() != null) {
                    failureDetails.getAdditionalDetails().putAll(payload.getFailureDetails());
                }
                operationService.failOperation(stack.getAccountId(), getOperationId(variables), message, List.of(successDetails), List.of(failureDetails));
                instanceMetaDataService.updateInstanceStatusOnUpscaleFailure(stack.getNotDeletedInstanceMetaDataSet());
                enableStatusChecker(stack, "Failed upscaling FreeIPA");
                sendEvent(context, FAIL_HANDLED_EVENT.event(), payload);
            }

            @Override
            protected void initPayloadConverterMap(List<PayloadConverter<UpscaleFailureEvent>> payloadConverters) {
                payloadConverters.add(new UpscaleStackResultToUpscaleFailureEventConverter());
                payloadConverters.add(new CollectMetadataResultToUpscaleFailureEventConverter());
                payloadConverters.add(new BootstrapMachinesFailedToUpscaleFailureEventConverter());
                payloadConverters.add(new HostMetadataSetupFailedToUpscaleFailureEventConverter());
                payloadConverters.add(new InstallFreeIpaServicesFailedToUpscaleFailureEventConverter());
                payloadConverters.add(new ClusterProxyUpdateRegistrationFailedToUpscaleFailureEventConverter());
                payloadConverters.add(new PostInstallFreeIpaFailedToUpscaleFailureEventConverter());
                payloadConverters.add(new ClusterProxyRegistrationFailedToUpscaleFailureEventConverter());
                payloadConverters.add(new ImageFallbackFailedToUpscaleFailureEventConverter());
                payloadConverters.add(new ValidateCloudStorageFailedToUpscaleFailureEventConverter());
                payloadConverters.add(new LoadBalancerUpdateFailureEventToUpscaleFailureEventConverter());
                // this should be the last one
                payloadConverters.add(new StackFailureEventToUpscaleFailureEventConverter());

            }
        };
    }
}