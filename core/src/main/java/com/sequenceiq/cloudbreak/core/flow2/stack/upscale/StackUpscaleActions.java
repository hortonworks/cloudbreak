package com.sequenceiq.cloudbreak.core.flow2.stack.upscale;

import static com.sequenceiq.cloudbreak.core.flow2.stack.upscale.AbstractStackUpscaleAction.HOST_GROUP_WITH_ADJUSTMENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.upscale.AbstractStackUpscaleAction.HOST_GROUP_WITH_HOSTNAMES;
import static com.sequenceiq.cloudbreak.util.NullUtil.getIfNotNullOtherwise;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.LongStream;
import java.util.stream.StreamSupport;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.action.Action;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.cloud.event.instance.CollectMetadataRequest;
import com.sequenceiq.cloudbreak.cloud.event.instance.CollectMetadataResult;
import com.sequenceiq.cloudbreak.cloud.event.instance.GetSSHFingerprintsRequest;
import com.sequenceiq.cloudbreak.cloud.event.instance.GetSSHFingerprintsResult;
import com.sequenceiq.cloudbreak.cloud.event.resource.UpscaleStackValidationRequest;
import com.sequenceiq.cloudbreak.cloud.event.resource.UpscaleStackValidationResult;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;
import com.sequenceiq.cloudbreak.clusterproxy.ClusterProxyEnablementService;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.common.service.TransactionService.TransactionExecutionException;
import com.sequenceiq.cloudbreak.converter.spi.InstanceMetaDataToCloudInstanceConverter;
import com.sequenceiq.cloudbreak.converter.spi.ResourceToCloudResourceConverter;
import com.sequenceiq.cloudbreak.converter.spi.StackToCloudStackConverter;
import com.sequenceiq.cloudbreak.core.flow2.dto.NetworkScaleDetails;
import com.sequenceiq.cloudbreak.core.flow2.event.StackScaleTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.stack.AbstractStackFailureAction;
import com.sequenceiq.cloudbreak.core.flow2.stack.StackFailureContext;
import com.sequenceiq.cloudbreak.core.flow2.stack.downscale.StackScalingFlowContext;
import com.sequenceiq.cloudbreak.core.flow2.stack.provision.StackCreationEvent;
import com.sequenceiq.cloudbreak.core.flow2.stack.provision.action.AbstractStackCreationAction;
import com.sequenceiq.cloudbreak.core.flow2.stack.start.StackCreationContext;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.dto.StackDtoDelegate;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.ClusterProxyReRegistrationRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.BootstrapNewNodesRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.BootstrapNewNodesResult;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.ExtendHostMetadataRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.ExtendHostMetadataResult;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.CleanupFreeIpaEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.ImageFallbackRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.StackUpscaleFailedConclusionRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.UpdateDomainDnsResolverRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.UpdateDomainDnsResolverResult;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.UpscaleStackImageFallbackResult;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.UpscaleStackRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.UpscaleStackResult;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.UpscaleStackSaltValidationRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.UpscaleStackSaltValidationResult;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.loadbalancer.UpscaleUpdateLoadBalancersRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.userdata.UpscaleCreateUserdataSecretsRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.userdata.UpscaleCreateUserdataSecretsSuccess;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.userdata.UpscaleUpdateUserdataSecretsRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.userdata.UpscaleUpdateUserdataSecretsSuccess;
import com.sequenceiq.cloudbreak.service.StackUpdater;
import com.sequenceiq.cloudbreak.service.encryption.UserdataSecretsService;
import com.sequenceiq.cloudbreak.service.environment.EnvironmentClientService;
import com.sequenceiq.cloudbreak.service.metrics.MetricType;
import com.sequenceiq.cloudbreak.service.multiaz.DataLakeAwareInstanceMetadataAvailabilityZoneCalculator;
import com.sequenceiq.cloudbreak.service.publicendpoint.ClusterPublicEndpointManagementService;
import com.sequenceiq.cloudbreak.service.resource.ResourceService;
import com.sequenceiq.cloudbreak.service.stack.InstanceGroupService;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.service.stack.StackUpgradeService;
import com.sequenceiq.cloudbreak.view.InstanceGroupView;
import com.sequenceiq.cloudbreak.view.InstanceMetadataView;
import com.sequenceiq.cloudbreak.view.StackView;
import com.sequenceiq.common.api.adjustment.AdjustmentTypeWithThreshold;
import com.sequenceiq.common.api.type.AdjustmentType;
import com.sequenceiq.common.api.type.InstanceGroupType;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.flow.core.PayloadConverter;
import com.sequenceiq.flow.event.EventSelectorUtil;

@Configuration
public class StackUpscaleActions {
    private static final Logger LOGGER = LoggerFactory.getLogger(StackUpscaleActions.class);

    @Inject
    private ResourceToCloudResourceConverter cloudResourceConverter;

    @Inject
    private InstanceMetaDataService instanceMetaDataService;

    @Inject
    private StackUpscaleService stackUpscaleService;

    @Inject
    private StackToCloudStackConverter cloudStackConverter;

    @Inject
    private InstanceGroupService instanceGroupService;

    @Inject
    private InstanceMetaDataToCloudInstanceConverter metadataConverter;

    @Inject
    private StackDtoService stackDtoService;

    @Inject
    private ResourceService resourceService;

    @Inject
    private ClusterPublicEndpointManagementService clusterPublicEndpointManagementService;

    @Inject
    private StackUpgradeService stackUpgradeService;

    @Inject
    private DataLakeAwareInstanceMetadataAvailabilityZoneCalculator availabilityZoneCalculator;

    @Inject
    private EnvironmentClientService environmentClientService;

    @Inject
    private UserdataSecretsService userdataSecretsService;

    @Inject
    private StackUpdater stackUpdater;

    @Bean(name = "UPDATE_DOMAIN_DNS_RESOLVER_STATE")
    public Action<?, ?> updateDomainDnsResolverAction() {
        return new AbstractStackUpscaleAction<>(StackScaleTriggerEvent.class) {

            @Override
            protected void prepareExecution(StackScaleTriggerEvent payload, Map<Object, Object> variables) {
                variables.put(HOST_GROUP_WITH_ADJUSTMENT, payload.getHostGroupsWithAdjustment());
                variables.put(HOST_GROUP_WITH_HOSTNAMES, payload.getHostGroupsWithHostNames());
                variables.put(REPAIR, payload.isRepair());
                if (payload.getTriggeredStackVariant() != null) {
                    variables.put(TRIGGERED_VARIANT, payload.getTriggeredStackVariant());
                }
                variables.put(NETWORK_SCALE_DETAILS, payload.getNetworkScaleDetails());
                variables.put(ADJUSTMENT_WITH_THRESHOLD, payload.getAdjustmentTypeWithThreshold());
            }

            @Override
            protected void doExecute(StackScalingFlowContext context, StackScaleTriggerEvent payload, Map<Object, Object> variables) {
                stackUpdater.updateStackStatus(context.getStackId(), DetailedStackStatus.UPSCALE_IN_PROGRESS);
                if (context.isRepair()) {
                    LOGGER.debug("We do not need to update domainDnsResolver in case of repair activity, since it matters only in case of upscale activity.");
                    sendEvent(context, new UpdateDomainDnsResolverResult(context.getStackId()));
                } else {
                    sendEvent(context, new UpdateDomainDnsResolverRequest(context.getStackId()));
                }
            }

            @Override
            protected Object getFailurePayload(StackScaleTriggerEvent payload, Optional<StackScalingFlowContext> flowContext, Exception ex) {
                return new StackFailureEvent(EventSelectorUtil.failureSelector(UpdateDomainDnsResolverResult.class), payload.getResourceId(), ex);
            }
        };
    }

    @Bean(name = "UPSCALE_SALT_PREVALIDATION_STATE")
    public Action<?, ?> prevalidateSaltAction() {
        return new AbstractStackUpscaleAction<>(UpdateDomainDnsResolverResult.class) {

            @Override
            protected void doExecute(StackScalingFlowContext context, UpdateDomainDnsResolverResult payload, Map<Object, Object> variables) {
                stackUpdater.updateStackStatus(context.getStackId(), DetailedStackStatus.UPSCALE_IN_PROGRESS);
                if (context.isRepair()) {
                    LOGGER.debug("Do not check salt yum lock during repair.");
                    sendEvent(context, new UpscaleStackSaltValidationResult(context.getStackId()));
                } else {
                    sendEvent(context, new UpscaleStackSaltValidationRequest(context.getStackId()));
                }
            }

            @Override
            protected Object getFailurePayload(UpdateDomainDnsResolverResult payload, Optional<StackScalingFlowContext> flowContext, Exception ex) {
                return new StackFailureEvent(EventSelectorUtil.failureSelector(UpscaleStackSaltValidationResult.class), payload.getResourceId(), ex);
            }
        };
    }

    @Bean(name = "UPSCALE_PREVALIDATION_STATE")
    public Action<?, ?> prevalidate() {
        return new AbstractStackUpscaleAction<>(UpscaleStackSaltValidationResult.class) {

            @Override
            protected void doExecute(StackScalingFlowContext context, UpscaleStackSaltValidationResult payload, Map<Object, Object> variables) {
                StackDto stack = stackDtoService.getById(context.getStackId());
                Map<String, Integer> hostGroupsWithAdjustment = (Map<String, Integer>) variables.get(HOST_GROUP_WITH_ADJUSTMENT);
                int instanceCountToCreate = 0;
                for (Map.Entry<String, Integer> hostGroupWithAdjustment : hostGroupsWithAdjustment.entrySet()) {
                    instanceCountToCreate += stackUpscaleService.getInstanceCountToCreate(stack, hostGroupWithAdjustment.getKey(),
                            hostGroupWithAdjustment.getValue(), context.isRepair());
                }

                stackUpscaleService.addInstanceFireEventAndLog(context.getStack(), hostGroupsWithAdjustment,
                        (AdjustmentTypeWithThreshold) variables.get(ADJUSTMENT_WITH_THRESHOLD));
                if (instanceCountToCreate > 0) {
                    stackUpscaleService.startAddInstances(context.getStack(), hostGroupsWithAdjustment);
                    sendEvent(context);
                } else {
                    StackEvent event = new StackEvent(StackUpscaleEvent.EXTEND_METADATA_EVENT.event(), payload.getResourceId());
                    sendEvent(context, event.selector(), event);
                }
            }

            @Override
            protected Selectable createRequest(StackScalingFlowContext context) {
                StackDto stack = stackDtoService.getById(context.getStackId());
                Map<String, Integer> hostGroupWithInstanceCountToCreate = getHostGroupsWithInstanceCountToCreate(context, stack);
                StackDtoDelegate updatedStack = instanceMetaDataService.saveInstanceAndGetUpdatedStack(stack, hostGroupWithInstanceCountToCreate,
                        context.getHostgroupWithHostnames(), false, context.isRepair(), context.getStackNetworkScaleDetails());
                CloudStack cloudStack = cloudStackConverter.convert(updatedStack);
                return new UpscaleStackValidationRequest<UpscaleStackValidationResult>(context.getCloudContext(), context.getCloudCredential(), cloudStack);
            }
        };
    }

    @Bean(name = "UPSCALE_CREATE_USERDATA_SECRETS_STATE")
    public Action<?, ?> createUserdataSecretsAction() {
        return new AbstractStackUpscaleAction<>(UpscaleStackValidationResult.class) {

            @Override
            protected void doExecute(StackScalingFlowContext context, UpscaleStackValidationResult payload, Map<Object, Object> variables) {
                StackView stackView = context.getStack();
                Long stackId = stackView.getId();
                DetailedEnvironmentResponse environment = environmentClientService.getByCrn(stackView.getEnvironmentCrn());
                if (environment.isEnableSecretEncryption()) {
                    variables.put(SECRET_ENCRYPTION_ENABLED, Boolean.TRUE);
                    StackDto stackDto = stackDtoService.getById(stackId);
                    Integer numberOfSecretsToCreate = getHostGroupsWithInstanceCountToCreate(context, stackDto).values().stream()
                            .reduce(0, Integer::sum);
                    long firstValidPrivateId = instanceMetaDataService.getFirstValidPrivateId(stackId);
                    List<Long> newPrivateIds = LongStream.range(firstValidPrivateId, firstValidPrivateId + numberOfSecretsToCreate).boxed().toList();
                    UpscaleCreateUserdataSecretsRequest request = new UpscaleCreateUserdataSecretsRequest(stackId, context.getCloudContext(),
                            context.getCloudCredential(), newPrivateIds);
                    sendEvent(context, request.selector(), request);
                } else {
                    variables.put(SECRET_ENCRYPTION_ENABLED, Boolean.FALSE);
                    LOGGER.info("Skipping userdata secret creation, since secret encryption is not enabled.");
                    sendEvent(context, new UpscaleCreateUserdataSecretsSuccess(stackId, List.of()));
                }
            }
        };
    }

    @Bean(name = "ADD_INSTANCES_STATE")
    public Action<?, ?> addInstances() {
        return new AbstractStackUpscaleAction<>(UpscaleCreateUserdataSecretsSuccess.class) {
            @Override
            protected void doExecute(StackScalingFlowContext context, UpscaleCreateUserdataSecretsSuccess payload, Map<Object, Object> variables) {
                boolean repair = context.isRepair();
                List<Long> createdSecretResourceIds = payload.getCreatedSecretResourceIds();
                NetworkScaleDetails stackNetworkScaleDetails = context.getStackNetworkScaleDetails();
                StackDto stack = stackDtoService.getById(context.getStackId());
                Map<String, Integer> hostGroupWithInstanceCountToCreate = getHostGroupsWithInstanceCountToCreate(context, stack);
                StackDtoDelegate updatedStack = instanceMetaDataService.saveInstanceAndGetUpdatedStack(stack, hostGroupWithInstanceCountToCreate,
                        context.getHostgroupWithHostnames(), true, repair, stackNetworkScaleDetails);
                assignNewSecretsToNewInstances(updatedStack, createdSecretResourceIds, variables);
                if (availabilityZoneCalculator.populateForScaling(updatedStack, hostGroupWithInstanceCountToCreate.keySet(),
                        repair, stackNetworkScaleDetails)) {
                    updatedStack = stackDtoService.getById(context.getStackId());
                }
                List<CloudResource> resources = resourceService.getAllByStackId(updatedStack.getId()).stream()
                        .map(r -> cloudResourceConverter.convert(r))
                        .collect(Collectors.toList());
                CloudStack updatedCloudStack = cloudStackConverter.convert(updatedStack);
                AdjustmentTypeWithThreshold adjustmentTypeWithThreshold = context.getAdjustmentTypeWithThreshold();
                if (adjustmentTypeWithThreshold == null) {
                    Integer exactNumber = hostGroupWithInstanceCountToCreate.values().stream().reduce(0, Integer::sum);
                    adjustmentTypeWithThreshold = new AdjustmentTypeWithThreshold(AdjustmentType.EXACT, exactNumber.longValue());
                }
                LOGGER.info("Adjustment type with threshold for upscale request: {}", adjustmentTypeWithThreshold);
                String triggeredVariant = (String) variables.get(TRIGGERED_VARIANT);
                boolean migrationNeed = stackUpgradeService.awsVariantMigrationIsFeasible(stack.getStack(), triggeredVariant);
                UpscaleStackRequest<UpscaleStackResult> request = new UpscaleStackRequest<>(context.getCloudContext(), context.getCloudCredential(),
                        updatedCloudStack, resources, adjustmentTypeWithThreshold, migrationNeed);
                sendEvent(context, request);
            }

            private void assignNewSecretsToNewInstances(StackDtoDelegate stack, List<Long> createdSecretResourceIds, Map<Object, Object> variables) {
                if (!createdSecretResourceIds.isEmpty()) {
                    List<Resource> secretResources = StreamSupport.stream(
                            resourceService.findAllByResourceId(createdSecretResourceIds).spliterator(), false).toList();
                    List<InstanceMetaData> newInstanceMetadas = stack.getAllAvailableInstances()
                            .stream()
                            .filter(imd -> imd instanceof InstanceMetaData && imd.getInstanceId() == null && imd.getUserdataSecretResourceId() == null)
                            .map(imd -> (InstanceMetaData) imd)
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

    @Bean(name = "UPSCALE_IMAGE_FALLBACK_STATE")
    public Action<?, ?> imageFallbackAction() {
        return new AbstractStackCreationAction<>(UpscaleStackImageFallbackResult.class) {

            @Override
            protected void doExecute(StackCreationContext context, UpscaleStackImageFallbackResult payload, Map<Object, Object> variables) {
                stackUpscaleService.fireImageFallbackFlowMessage(context.getStackId(), payload.getNotificationMessage());
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(StackCreationContext context) {
                return new ImageFallbackRequest(context.getStackId(), context.getCloudContext());
            }

        };
    }

    @Bean(name = "ADD_INSTANCES_FINISHED_STATE")
    public Action<?, ?> finishAddInstances() {
        return new AbstractStackUpscaleAction<>(UpscaleStackResult.class) {
            @Override
            protected void doExecute(StackScalingFlowContext context, UpscaleStackResult payload, Map<Object, Object> variables) {
                stackUpscaleService.finishAddInstances(context, payload);
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(StackScalingFlowContext context) {
                return new StackEvent(StackUpscaleEvent.EXTEND_METADATA_EVENT.event(), context.getStack().getId());
            }
        };
    }

    @Bean(name = "EXTEND_METADATA_STATE")
    public Action<?, ?> extendMetadata() {
        return new AbstractStackUpscaleAction<>(StackEvent.class) {
            @Override
            protected void doExecute(StackScalingFlowContext context, StackEvent payload, Map<Object, Object> variables) {
                stackUpscaleService.extendingMetadata(context.getStackId());
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(StackScalingFlowContext context) {
                StackDto stack = stackDtoService.getById(context.getStackId());
                List<CloudResource> cloudResources = resourceService.getAllByStackId(stack.getId()).stream()
                        .map(r -> cloudResourceConverter.convert(r))
                        .collect(Collectors.toList());
                List<CloudInstance> allKnownInstances = cloudStackConverter.buildInstances(stack);
                LOGGER.info("All known instances: {}", allKnownInstances);
                Set<String> unusedInstancesForGroup = new HashSet<>();
                for (String hostGroup : context.getHostGroupWithAdjustment().keySet()) {
                    unusedInstancesForGroup.addAll(instanceMetaDataService.unusedInstancesInInstanceGroupByName(context.getStackId(), hostGroup).stream()
                            .map(InstanceMetaData::getInstanceId)
                            .collect(Collectors.toSet()));
                }

                LOGGER.info("Unused instances for group: {}", unusedInstancesForGroup);
                List<CloudInstance> newCloudInstances = allKnownInstances.stream()
                        .filter(cloudInstance -> InstanceStatus.CREATE_REQUESTED.equals(cloudInstance.getTemplate().getStatus())
                                || unusedInstancesForGroup.contains(cloudInstance.getInstanceId()))
                        .collect(Collectors.toList());
                return new CollectMetadataRequest(context.getCloudContext(), context.getCloudCredential(), cloudResources, newCloudInstances,
                        allKnownInstances);
            }
        };
    }

    @Bean(name = "EXTEND_METADATA_FINISHED_STATE")
    public Action<?, ?> finishExtendMetadata() {
        return new AbstractStackUpscaleAction<>(CollectMetadataResult.class) {
            @Override
            protected void doExecute(StackScalingFlowContext context, CollectMetadataResult payload, Map<Object, Object> variables)
                    throws TransactionExecutionException {
                Integer adjustment = context.getHostGroupWithAdjustment().values().stream().reduce(0, Integer::sum);
                Set<String> upscaleCandidateAddresses = stackUpscaleService.finishExtendMetadata(context.getStack(), adjustment, payload);
                variables.put(UPSCALE_CANDIDATE_ADDRESSES, upscaleCandidateAddresses);
                sendEvent(context, StackUpscaleEvent.UPSCALE_UPDATE_LOAD_BALANCERS_EVENT.event(), new StackEvent(context.getStackId()));
            }
        };
    }

    @Bean("UPSCALE_UPDATE_LOAD_BALANCERS_STATE")
    public Action<?, ?> upscaleUpdateLoadBalancersAction() {
        return new AbstractStackCreationAction<>(StackEvent.class) {
            @Override
            protected void doExecute(StackCreationContext context, StackEvent payload, Map<Object, Object> variables) {
                StackDto stack = stackDtoService.getById(context.getStackId());
                CloudStack cloudStack = cloudStackConverter.convert(stack);
                UpscaleUpdateLoadBalancersRequest request =
                        new UpscaleUpdateLoadBalancersRequest(context.getStackId(), cloudStack, context.getCloudContext(), context.getCloudCredential());
                sendEvent(context, request.selector(), request);
            }
        };
    }

    @Bean("UPSCALE_UPDATE_USERDATA_SECRETS_STATE")
    public Action<?, ?> updateUserdataSecretsAction() {
        return new AbstractStackUpscaleAction<>(StackEvent.class) {

            @Override
            protected void doExecute(StackScalingFlowContext context, StackEvent payload, Map<Object, Object> variables) {
                StackView stack = context.getStack();
                if ((Boolean) variables.getOrDefault(SECRET_ENCRYPTION_ENABLED, Boolean.FALSE)) {
                    List<Long> newInstanceIds = getIfNotNullOtherwise(variables.remove(NEW_INSTANCE_ENTITY_IDS), x -> (List<Long>) x, List.of());
                    UpscaleUpdateUserdataSecretsRequest request = new UpscaleUpdateUserdataSecretsRequest(stack.getId(), context.getCloudContext(),
                            context.getCloudCredential(), newInstanceIds);
                    sendEvent(context, request.selector(), request);
                } else {
                    LOGGER.info("Skipping updating userdata secrets, since secret encryption is not enabled.");
                    sendEvent(context, new UpscaleUpdateUserdataSecretsSuccess(context.getStackId()));
                }
            }
        };
    }

    @Bean("UPSCALE_UPDATE_USERDATA_SECRETS_FINISHED_STATE")
    public Action<?, ?> updateUserdataSecretsFinishedAction() {
        return new AbstractStackUpscaleAction<>(StackEvent.class) {

            @Override
            protected void doExecute(StackScalingFlowContext context, StackEvent payload, Map<Object, Object> variables) {
                Set<String> hostGroups = context.getHostGroupWithAdjustment().keySet();
                List<InstanceGroupView> scaledInstanceGroups = instanceGroupService.findAllInstanceGroupViewByStackIdAndGroupName(payload.getResourceId(),
                        hostGroups);
                boolean gatewayWasUpscaled = scaledInstanceGroups.stream()
                        .anyMatch(instanceGroup -> InstanceGroupType.GATEWAY.equals(instanceGroup.getInstanceGroupType()));
                if (gatewayWasUpscaled) {
                    LOGGER.info("Gateway type instance group");
                    StackView stack = context.getStack();
                    InstanceMetadataView gatewayMetaData = instanceMetaDataService.getPrimaryGatewayInstanceMetadata(stack.getId()).orElse(null);
                    if (null == gatewayMetaData) {
                        throw new CloudbreakServiceException("Could not get gateway instance metadata from the cloud provider.");
                    }
                    InstanceGroupView instanceGroup = scaledInstanceGroups.stream()
                            .filter(ig -> ig.getGroupName().equals(gatewayMetaData.getInstanceGroupName()))
                            .findFirst()
                            .orElseThrow(NotFoundException.notFound("Cannot found InstanceGroup for Gateway instance metadata"));
                    CloudInstance gatewayInstance = metadataConverter.convert(gatewayMetaData, instanceGroup, stack);
                    LOGGER.info("Send GetSSHFingerprintsRequest because we need to collect SSH fingerprints");
                    Selectable sshFingerPrintReq = new GetSSHFingerprintsRequest<GetSSHFingerprintsResult>(context.getCloudContext(),
                            context.getCloudCredential(), gatewayInstance);
                    sendEvent(context, sshFingerPrintReq);
                } else {
                    sendEvent(context, StackUpscaleEvent.BOOTSTRAP_NEW_NODES_EVENT.event(), new StackEvent(context.getStackId()));
                }
            }
        };
    }

    @Bean(name = "GATEWAY_TLS_SETUP_STATE")
    public Action<?, ?> tlsSetupAction() {
        return new AbstractStackUpscaleAction<>(GetSSHFingerprintsResult.class) {
            @Override
            protected void doExecute(StackScalingFlowContext context, GetSSHFingerprintsResult payload, Map<Object, Object> variables) throws Exception {
                StackDto stack = stackDtoService.getById(context.getStackId());
                stackUpscaleService.setupTls(stack);
                StackEvent event = new StackEvent(payload.getResourceId());
                sendEvent(context, StackCreationEvent.TLS_SETUP_FINISHED_EVENT.event(), event);
            }
        };
    }

    @Bean(name = "RE_REGISTER_WITH_CLUSTER_PROXY_STATE")
    public Action<?, ?> reRegisterWithClusterProxy() {
        return new AbstractStackUpscaleAction<>(StackEvent.class) {
            @Inject
            private ClusterProxyEnablementService clusterProxyEnablementService;

            @Override
            protected void doExecute(StackScalingFlowContext context, StackEvent payload, Map<Object, Object> variables) {
                if (clusterProxyEnablementService.isClusterProxyApplicable(context.getStack().getCloudPlatform())) {
                    stackUpscaleService.reRegisterWithClusterProxy(context.getStack().getId());
                    sendEvent(context);
                } else {
                    LOGGER.info("Cluster Proxy integration is DISABLED, skipping re-registering with Cluster Proxy service");
                    StackEvent bootstrapNewNodesEvent =
                            new StackEvent(StackUpscaleEvent.CLUSTER_PROXY_RE_REGISTRATION_FINISHED_EVENT.event(), payload.getResourceId());
                    sendEvent(context, bootstrapNewNodesEvent);
                }
            }

            @Override
            protected Selectable createRequest(StackScalingFlowContext context) {
                return new ClusterProxyReRegistrationRequest(context.getStack().getId(),
                        context.getStack().getCloudPlatform(),
                        StackUpscaleEvent.CLUSTER_PROXY_RE_REGISTRATION_FINISHED_EVENT.event());
            }
        };
    }

    @Bean(name = "BOOTSTRAP_NEW_NODES_STATE")
    public Action<?, ?> bootstrapNewNodes() {
        return new AbstractStackUpscaleAction<>(StackEvent.class) {
            @Override
            protected void doExecute(StackScalingFlowContext context, StackEvent payload, Map<Object, Object> variables) {
                stackUpscaleService.bootstrappingNewNodes(context.getStack());
                Map<String, Set<String>> hostgroupWithHostnames = context.getHostgroupWithHostnames();
                Selectable request = new BootstrapNewNodesRequest(context.getStack().getId(),
                        (Set<String>) variables.get(UPSCALE_CANDIDATE_ADDRESSES),
                        hostgroupWithHostnames.values().stream().flatMap(Collection::stream).collect(Collectors.toSet()));
                sendEvent(context, request);
            }
        };
    }

    @Bean(name = "EXTEND_HOST_METADATA_STATE")
    public Action<?, ?> extendHostMetadata() {
        return new AbstractStackUpscaleAction<>(BootstrapNewNodesResult.class) {
            @Override
            protected void doExecute(StackScalingFlowContext context, BootstrapNewNodesResult payload, Map<Object, Object> variables) {
                // UNUSED STEP
                ExtendHostMetadataRequest request = new ExtendHostMetadataRequest(context.getStack().getId(),
                        payload.getRequest().getUpscaleCandidateAddresses());
                sendEvent(context, new ExtendHostMetadataResult(request));
            }
        };
    }

    @Bean(name = "CLEANUP_FREEIPA_UPSCALE_STATE")
    public Action<?, ?> cleanupFreeIpaAction() {
        return new AbstractStackUpscaleAction<>(ExtendHostMetadataResult.class) {

            @Inject
            private InstanceMetaDataService instanceMetaDataService;

            @Override
            protected void doExecute(StackScalingFlowContext context, ExtendHostMetadataResult payload, Map<Object, Object> variables) {
                List<InstanceMetadataView> instanceMetaData = instanceMetaDataService.getAllAvailableInstanceMetadataViewsByStackId(context.getStack().getId());
                Set<String> ips = payload.getRequest().getUpscaleCandidateAddresses();
                Set<String> hostNames = instanceMetaData.stream()
                        .filter(im -> ips.contains(im.getPrivateIp()))
                        .filter(im -> im.getDiscoveryFQDN() != null)
                        .map(InstanceMetadataView::getDiscoveryFQDN)
                        .collect(Collectors.toSet());
                    CleanupFreeIpaEvent cleanupFreeIpaEvent = new CleanupFreeIpaEvent(context.getStackId(), hostNames, ips, context.isRepair());
                    sendEvent(context, cleanupFreeIpaEvent);
            }
        };
    }

    @Bean(name = "EXTEND_HOST_METADATA_FINISHED_STATE")
    public Action<?, ?> finishExtendHostMetadata() {
        return new AbstractStackUpscaleAction<>(CleanupFreeIpaEvent.class) {
            @Override
            protected void doExecute(StackScalingFlowContext context, CleanupFreeIpaEvent payload, Map<Object, Object> variables) {
                final StackDtoDelegate stack = stackDtoService.getById(context.getStackId());
                stackUpscaleService.finishExtendHostMetadata(context.getStack());
                final Set<String> newAddresses = payload.getIps();
                final Map<String, String> newAddressesByHostname = stack.getAllAvailableInstances().stream()
                        .filter(instanceMetaData -> newAddresses.contains(instanceMetaData.getPrivateIp()))
                        .filter(instanceMetaData -> instanceMetaData.getShortHostname() != null)
                        .collect(Collectors.toMap(InstanceMetadataView::getShortHostname, InstanceMetadataView::getPublicIpWrapper));
                clusterPublicEndpointManagementService.upscale(stack, newAddressesByHostname);
                getMetricService().incrementMetricCounter(MetricType.STACK_UPSCALE_SUCCESSFUL, stack.getStack());
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(StackScalingFlowContext context) {
                return new StackEvent(StackUpscaleEvent.UPSCALE_FINALIZED_EVENT.event(), context.getStack().getId());
            }
        };
    }

    @Bean(name = "UPSCALE_FAILED_STATE")
    public Action<?, ?> stackUpscaleFailedAction() {
        return new AbstractStackFailureAction<StackUpscaleState, StackUpscaleEvent>() {
            @Override
            protected void doExecute(StackFailureContext context, StackFailureEvent payload, Map<Object, Object> variables) {
                Map<String, Set<String>> hostgroupWithHostnames = (Map<String, Set<String>>) variables.getOrDefault(HOST_GROUP_WITH_HOSTNAMES, new HashMap<>());
                Map<String, Integer> hostGroupWithAdjustment = (Map<String, Integer>) variables.getOrDefault(HOST_GROUP_WITH_ADJUSTMENT, new HashMap<>());
                getMetricService().incrementMetricCounter(MetricType.STACK_UPSCALE_FAILED, context.getStack(), payload.getException());
                StackUpscaleFailedConclusionRequest stackUpscaleFailedConclusionRequest = new StackUpscaleFailedConclusionRequest(context.getStackId(),
                        hostgroupWithHostnames, hostGroupWithAdjustment, isRepair(variables), payload.getException());
                sendEvent(context, stackUpscaleFailedConclusionRequest);
            }
        };
    }

    private Map<String, Integer> getHostGroupsWithInstanceCountToCreate(StackScalingFlowContext context, StackDto stack) {
        LOGGER.debug("Assembling upscale stack event for stack: {}", context.getStack().getName());
        Map<String, Integer> hostGroupsWithAdjustment = context.getHostGroupWithAdjustment();
        Map<String, Integer> hostGroupWithInstanceCountToCreate = new HashMap<>();
        for (Map.Entry<String, Integer> hostGroupWithAdjustment : hostGroupsWithAdjustment.entrySet()) {
            String hostGroup = hostGroupWithAdjustment.getKey();
            int instanceCountToCreate = stackUpscaleService.getInstanceCountToCreate(stack, hostGroup,
                    hostGroupWithAdjustment.getValue(), context.isRepair());
            hostGroupWithInstanceCountToCreate.put(hostGroup, instanceCountToCreate);
        }
        return hostGroupWithInstanceCountToCreate;
    }

    private boolean isRepair(Map<Object, Object> variables) {
        return (boolean) variables.getOrDefault(AbstractStackUpscaleAction.REPAIR, Boolean.FALSE);
    }
}