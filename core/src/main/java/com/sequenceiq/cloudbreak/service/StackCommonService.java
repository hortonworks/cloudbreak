package com.sequenceiq.cloudbreak.service;

import static com.sequenceiq.cloudbreak.common.anonymizer.AnonymizerUtil.anonymize;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.STACK_RETRY_FLOW_START;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.google.common.annotations.VisibleForTesting;
import com.sequenceiq.cloudbreak.api.endpoint.v4.autoscales.base.ScalingStrategy;
import com.sequenceiq.cloudbreak.api.endpoint.v4.autoscales.request.UpdateStackV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.autoscales.response.CertificateV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.endpoint.v4.dto.NameOrCrn;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.StatusRequest;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.ClusterRepairV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackAddVolumesRequest;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackDeleteVolumesRequest;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackImageChangeV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackScaleV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackVerticalScaleV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.UpdateClusterV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.network.NetworkScaleV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.AutoscaleStackV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.GeneratedBlueprintV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.SaltPasswordStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.api.model.RotateSaltPasswordReason;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.cloudbreak.cloud.service.CloudParameterCache;
import com.sequenceiq.cloudbreak.common.ScalingHardLimitsService;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.controller.StackCreatorService;
import com.sequenceiq.cloudbreak.controller.validation.network.MultiAzValidator;
import com.sequenceiq.cloudbreak.converter.v4.stacks.StackScaleV4RequestToUpdateClusterV4RequestConverter;
import com.sequenceiq.cloudbreak.converter.v4.stacks.StackScaleV4RequestToUpdateStackV4RequestConverter;
import com.sequenceiq.cloudbreak.converter.v4.stacks.StackV4RequestToTemplatePreparationObjectConverter;
import com.sequenceiq.cloudbreak.domain.ImageCatalog;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.dto.SubnetIdWithResourceNameAndCrn;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.service.cluster.ClusterRepairService;
import com.sequenceiq.cloudbreak.service.cluster.flow.ClusterOperationService;
import com.sequenceiq.cloudbreak.service.image.ImageCatalogService;
import com.sequenceiq.cloudbreak.service.image.ImageChangeDto;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.service.stack.flow.StackOperationService;
import com.sequenceiq.cloudbreak.structuredevent.CloudbreakRestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.structuredevent.event.CloudbreakEventService;
import com.sequenceiq.cloudbreak.template.BlueprintUpdaterConnectors;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.util.StackUtil;
import com.sequenceiq.cloudbreak.view.InstanceGroupView;
import com.sequenceiq.cloudbreak.view.StackView;
import com.sequenceiq.cloudbreak.workspace.model.User;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.domain.RetryResponse;
import com.sequenceiq.flow.domain.RetryableFlow;
import com.sequenceiq.flow.service.FlowRetryService;

@Service
public class StackCommonService {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackCommonService.class);

    @Inject
    private CloudbreakRestRequestThreadLocalService restRequestThreadLocalService;

    @Inject
    private ScalingHardLimitsService scalingHardLimitsService;

    @Inject
    private FlowRetryService flowRetryService;

    @Inject
    private ClusterCommonService clusterCommonService;

    @Inject
    private CloudParameterCache cloudParameterCache;

    @Inject
    private ImageCatalogService imageCatalogService;

    @Inject
    private StackCreatorService stackCreatorService;

    @Inject
    private TlsSecurityService tlsSecurityService;

    @Inject
    private ClusterOperationService clusterOperationService;

    @Inject
    private ClusterRepairService clusterRepairService;

    @Inject
    private StackService stackService;

    @Inject
    private StackDtoService stackDtoService;

    @Inject
    private StackOperationService stackOperationService;

    @Inject
    private BlueprintUpdaterConnectors blueprintUpdaterConnectors;

    @Inject
    private NodeCountLimitValidator nodeCountLimitValidator;

    @Inject
    private StackScaleV4RequestToUpdateStackV4RequestConverter stackScaleV4RequestToUpdateStackV4RequestConverter;

    @Inject
    private StackV4RequestToTemplatePreparationObjectConverter stackV4RequestToTemplatePreparationObjectConverter;

    @Inject
    private StackScaleV4RequestToUpdateClusterV4RequestConverter stackScaleV4RequestToUpdateClusterV4RequestConverter;

    @Inject
    private CloudbreakEventService eventService;

    @Inject
    private MultiAzValidator multiAzValidator;

    @Inject
    private StackUtil stackUtil;

    @Inject
    private RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    @Inject
    private InstanceMetaDataService instanceMetaDataService;

    @Inject
    private VerticalScalingValidatorService verticalScalingValidatorService;

    public StackV4Response createInWorkspace(StackV4Request stackRequest, User user, Workspace workspace, boolean distroxRequest) {
        return stackCreatorService.createStack(user, workspace, stackRequest, distroxRequest);
    }

    public StackV4Response getByCrn(String crn) {
        return stackService.getJsonByCrn(crn);
    }

    public StackV4Response findStackByNameOrCrnAndWorkspaceId(NameOrCrn nameOrCrn, String accountId, Set<String> entries, StackType stackType,
            boolean withResources) {
        return nameOrCrn.hasName()
                ? stackService.getByNameInWorkspaceWithEntries(nameOrCrn.getName(), accountId, entries, stackType, withResources)
                : stackService.getByCrnInWorkspaceWithEntries(nameOrCrn.getCrn(), entries, stackType, withResources);
    }

    public FlowIdentifier putInDefaultWorkspace(String crn, UpdateStackV4Request updateRequest) {
        LOGGER.info("Received putStack on crn: {}, updateRequest: {}", crn, updateRequest);
        StackDto stack = stackDtoService.getByCrn(crn);
        MDCBuilder.buildMdcContext(stack);
        return put(stack, updateRequest);
    }

    public FlowIdentifier putStartInstancesInDefaultWorkspace(NameOrCrn nameOrCrn, String accountId, UpdateStackV4Request updateRequest,
            ScalingStrategy scalingStrategy) {
        LOGGER.info("Received putStack: {}, with scalingStrategy: {}, updateRequest: {}", nameOrCrn, scalingStrategy, updateRequest);
        StackDto stack = stackDtoService.getByNameOrCrn(nameOrCrn, accountId);
        if (!stackUtil.stopStartScalingEntitlementEnabled(stack.getStack())) {
            throw new BadRequestException("The entitlement for scaling via stop/start is not enabled");
        }
        MDCBuilder.buildMdcContext(stack);
        return putStartInstances(stack, updateRequest, scalingStrategy);
    }

    public FlowIdentifier putStopInWorkspace(NameOrCrn nameOrCrn, String accountId) {
        StackDto stack = stackDtoService.getByNameOrCrn(nameOrCrn, accountId);
        MDCBuilder.buildMdcContext(stack);
        if (!cloudParameterCache.isStartStopSupported(stack.getCloudPlatform())) {
            throw new BadRequestException(String.format("Stop is not supported on %s cloudplatform", stack.getCloudPlatform()));
        }
        UpdateStackV4Request updateStackJson = new UpdateStackV4Request();
        updateStackJson.setStatus(StatusRequest.STOPPED);
        updateStackJson.setWithClusterEvent(true);
        return put(stack, updateStackJson);
    }

    public FlowIdentifier syncInWorkspace(NameOrCrn nameOrCrn, String accountId, Set<StackType> permittedStackTypes) {
        StackDto stack = stackDtoService.getByNameOrCrn(nameOrCrn, accountId);
        MDCBuilder.buildMdcContext(stack);
        StackType stackType = stack.getStack().getType();
        if (!permittedStackTypes.contains(stackType)) {
            String resourceType = stackType.getResourceType();
            String permittedResourceTypes = permittedStackTypes.stream()
                    .map(StackType::getResourceType)
                    .collect(Collectors.joining(", "));
            throw new BadRequestException(
                    String.format("Sync is not supported for stack '%s'. Its type is '%s', whereas the operation is permitted only for the following types: %s.",
                            nameOrCrn, resourceType, permittedResourceTypes));
        }
        UpdateStackV4Request updateStackJson = new UpdateStackV4Request();
        updateStackJson.setStatus(StatusRequest.FULL_SYNC);
        updateStackJson.setWithClusterEvent(true);
        return put(stack, updateStackJson);
    }

    public FlowIdentifier syncComponentVersionsFromCmInWorkspace(NameOrCrn nameOrCrn, String accountId, Set<String> candidateImageUuids) {
        StackView stack = stackDtoService.getStackViewByNameOrCrn(nameOrCrn, accountId);
        MDCBuilder.buildMdcContext(stack);
        String operationDescription = "Reading CM and parcel versions from CM";
        ensureStackAvailability(stack, operationDescription);
        ensureInstanceAvailability(stack, operationDescription);

        LOGGER.debug("Triggering sync from CM to db: syncing versions from CM to db, nameOrCrn: {}, accountId: {}, candidateImageUuids: {}",
                nameOrCrn, accountId, candidateImageUuids);
        return syncComponentVersionsFromCm(stack, candidateImageUuids);
    }

    private void ensureInstanceAvailability(StackView stack, String operationDescription) {
        if (instanceMetaDataService.anyInstanceStopped(stack.getId())) {
            String message = String.format("Please start all stopped instances. %s can only be made when all your nodes in running state.",
                    operationDescription);
            LOGGER.debug(message);
            throw new BadRequestException(message);
        }
    }

    private void ensureStackAvailability(StackView stack, String operationDescription) {
        if (stack.getStatus().isStopState()) {
            String message = String.format("%s cannot be initiated as the cluster is in %s state.",
                    operationDescription, stack.getStatus());
            LOGGER.debug(message);
            throw new BadRequestException(message);
        }
    }

    public FlowIdentifier deleteMultipleInstancesInWorkspace(NameOrCrn nameOrCrn, String accountId, Set<String> instanceIds, boolean forced) {
        StackDto stack = stackDtoService.getByNameOrCrn(nameOrCrn, accountId);
        validateStackIsNotDataLake(stack.getStack(), instanceIds);
        return stackOperationService.removeInstances(stack, instanceIds, forced);
    }

    public FlowIdentifier stopMultipleInstancesInWorkspace(NameOrCrn nameOrCrn, String accountId, Set<String> instanceIds, boolean forced) {
        StackDto stack = stackDtoService.getByNameOrCrn(nameOrCrn, accountId);
        if (!stackUtil.stopStartScalingEntitlementEnabled(stack.getStack())) {
            throw new BadRequestException("The entitlement for scaling via stop/start is not enabled");
        }
        validateStackIsNotDataLake(stack.getStack(), instanceIds);
        return stackOperationService.stopInstances(stack, instanceIds, forced);
    }

    public FlowIdentifier putStartInWorkspace(NameOrCrn nameOrCrn, String accountId) {
        StackDto stack = stackDtoService.getByNameOrCrn(nameOrCrn, accountId);
        return putStartInWorkspace(stack);
    }

    public FlowIdentifier rotateSaltPassword(NameOrCrn nameOrCrn, String accountId, RotateSaltPasswordReason reason) {
        return stackOperationService.rotateSaltPassword(nameOrCrn, accountId, reason);
    }

    public SaltPasswordStatus getSaltPasswordStatus(NameOrCrn nameOrCrn, String accountId) {
        return stackOperationService.getSaltPasswordStatus(nameOrCrn, accountId);
    }

    public FlowIdentifier modifyProxyConfig(NameOrCrn nameOrCrn, String accountId, String previousProxyConfigCrn) {
        return stackOperationService.modifyProxyConfig(nameOrCrn, accountId, previousProxyConfigCrn);
    }

    private FlowIdentifier putStartInWorkspace(StackDto stack) {
        MDCBuilder.buildMdcContext(stack);
        if (!cloudParameterCache.isStartStopSupported(stack.getCloudPlatform())) {
            throw new BadRequestException(String.format("Start is not supported on %s cloudplatform", stack.getCloudPlatform()));
        }
        UpdateStackV4Request updateStackJson = new UpdateStackV4Request();
        updateStackJson.setStatus(StatusRequest.STARTED);
        updateStackJson.setWithClusterEvent(true);
        return put(stack, updateStackJson);
    }

    public FlowIdentifier putScalingInWorkspace(NameOrCrn nameOrCrn, String accountId, StackScaleV4Request stackScaleV4Request) {
        StackDto stack = stackDtoService.getByNameOrCrn(nameOrCrn, accountId);
        validateNetworkScaleRequest(stack, stackScaleV4Request.getStackNetworkScaleV4Request());
        MDCBuilder.buildMdcContext(stack);
        stackScaleV4Request.setStackId(stack.getId());
        UpdateStackV4Request updateStackJson = stackScaleV4RequestToUpdateStackV4RequestConverter.convert(stackScaleV4Request);
        Integer scalingAdjustment = updateStackJson.getInstanceGroupAdjustment().getScalingAdjustment();
        validateScalingRequest(stack.getStack(), scalingAdjustment);

        FlowIdentifier flowIdentifier;
        if (scalingAdjustment > 0) {
            flowIdentifier = put(stack, updateStackJson);
        } else {
            UpdateClusterV4Request updateClusterJson = stackScaleV4RequestToUpdateClusterV4RequestConverter.convert(stackScaleV4Request);
            flowIdentifier = clusterCommonService.put(stack, updateClusterJson);
        }
        return flowIdentifier;
    }

    public FlowIdentifier putVerticalScalingInWorkspace(NameOrCrn nameOrCrn, String accountId, StackVerticalScaleV4Request stackVerticalScaleV4Request) {
        StackView stackView = stackDtoService.getStackViewByNameOrCrn(nameOrCrn, accountId);
        Stack stack = stackService.getByIdWithLists(stackView.getId());
        MDCBuilder.buildMdcContext(stackView);
        stackVerticalScaleV4Request.setStackId(stackView.getId());
        validateVerticalScalingRequest(stack, stackVerticalScaleV4Request);
        return clusterCommonService.putVerticalScaling(stackView.getResourceCrn(), stackVerticalScaleV4Request);
    }

    private void validateScalingRequest(StackView stack, Integer scalingAdjustment) {
        if (scalingAdjustment > 0 && !cloudParameterCache.isUpScalingSupported(stack.getCloudPlatform())) {
            throw new BadRequestException(String.format("Upscaling is not supported on %s cloudplatform", stack.getCloudPlatform()));
        }
        if (scalingAdjustment < 0 && !cloudParameterCache.isDownScalingSupported(stack.getCloudPlatform())) {
            throw new BadRequestException(String.format("Downscaling is not supported on %s cloudplatform", stack.getCloudPlatform()));
        }
        nodeCountLimitValidator.validateScale(stack.getId(), scalingAdjustment, Crn.safeFromString(stack.getResourceCrn()).getAccountId());
    }

    private void validateVerticalScalingRequest(Stack stack, StackVerticalScaleV4Request verticalScaleV4Request) {
        verticalScalingValidatorService.validateProvider(stack, "Vertical scaling", verticalScaleV4Request);
        verticalScalingValidatorService.validateRequest(stack, verticalScaleV4Request);
        verticalScalingValidatorService.validateInstanceType(stack, verticalScaleV4Request);
    }

    public void deleteWithKerberosInWorkspace(NameOrCrn nameOrCrn, String accountId, boolean forced) {
        LOGGER.info("Trying to delete stack");
        StackView stack = stackDtoService.getStackViewByNameOrCrn(nameOrCrn, accountId);
        MDCBuilder.buildMdcContext(stack);
        clusterOperationService.delete(stack.getId(), forced);
    }

    public FlowIdentifier repairCluster(Long workspaceId, NameOrCrn nameOrCrn, ClusterRepairV4Request clusterRepairRequest) {
        Long stackId = stackService.getIdByNameOrCrnInWorkspace(nameOrCrn, workspaceId);
        if (clusterRepairRequest.getHostGroups() != null) {
            return clusterRepairService.repairHostGroups(stackId, new HashSet<>(clusterRepairRequest.getHostGroups()),
                    clusterRepairRequest.isRestartServices());
        } else {
            return clusterRepairService.repairNodes(stackId,
                    new HashSet<>(clusterRepairRequest.getNodes().getIds()),
                    clusterRepairRequest.getNodes().isDeleteVolumes(),
                    clusterRepairRequest.isRestartServices());
        }
    }

    public FlowIdentifier retryInWorkspace(NameOrCrn nameOrCrn, Long workspaceId) {
        Long stackId = stackService.getIdByNameOrCrnInWorkspace(nameOrCrn, workspaceId);
        RetryResponse retry = flowRetryService.retry(stackId);
        eventService.fireCloudbreakEvent(stackId, Status.UPDATE_IN_PROGRESS.name(), STACK_RETRY_FLOW_START, List.of(retry.getName()));
        return retry.getFlowIdentifier();
    }

    public List<RetryableFlow> getRetryableFlows(String name, Long workspaceId) {
        Long stackId = stackService.getIdByNameInWorkspace(name, workspaceId);
        return flowRetryService.getRetryableFlows(stackId);
    }

    public GeneratedBlueprintV4Response postStackForBlueprint(StackV4Request stackRequest) {
        TemplatePreparationObject templatePreparationObject = stackV4RequestToTemplatePreparationObjectConverter.convert(stackRequest);

        String blueprintText = blueprintUpdaterConnectors.getBlueprintText(templatePreparationObject);
        GeneratedBlueprintV4Response response = new GeneratedBlueprintV4Response();
        response.setBlueprintText(anonymize(blueprintText));
        return response;
    }

    public CertificateV4Response getCertificate(String crn) {
        Stack stack = stackService.findByCrn(crn);
        return tlsSecurityService.getCertificates(stack.getId());
    }

    public Set<AutoscaleStackV4Response> getAllForAutoscale() {
        LOGGER.debug("Get all stack, autoscale authorized only.");
        return stackService.getAllForAutoscale();
    }

    public FlowIdentifier deleteInstanceInWorkspace(NameOrCrn nameOrCrn, String accountId, String instanceId, boolean forced) {
        StackDto stack = stackDtoService.getByNameOrCrn(nameOrCrn, accountId);
        validateStackIsNotDataLake(stack.getStack(), Set.of(instanceId));
        return stackOperationService.removeInstance(stack, instanceId, forced);
    }

    private void validateStackIsNotDataLake(StackView stack, Set<String> instanceIds) {
        if (StackType.DATALAKE.equals(stack.getType())) {
            if (instanceIds.size() == 1) {
                throw new BadRequestException(String.format("%s is a node of a data lake cluster, therefore it's not allowed to delete/stop it.",
                        List.copyOf(instanceIds).get(0)));
            } else {
                throw new BadRequestException(String.format("%s are nodes of a data lake cluster, therefore it's not allowed to delete/stop them.",
                        String.join(", ", instanceIds)));
            }
        }
    }

    public FlowIdentifier changeImageInWorkspace(NameOrCrn nameOrCrn, Long workspaceId, StackImageChangeV4Request stackImageChangeRequest) {
        ImageChangeDto imageChangeDto = createImageChangeDto(nameOrCrn, workspaceId, stackImageChangeRequest);
        return stackOperationService.updateImage(imageChangeDto);
    }

    public ImageChangeDto createImageChangeDto(NameOrCrn nameOrCrn, Long workspaceId, StackImageChangeV4Request stackImageChangeRequest) {
        Long stackId = stackService.getIdByNameOrCrnInWorkspace(nameOrCrn, workspaceId);
        if (StringUtils.isNotBlank(stackImageChangeRequest.getImageCatalogName())) {
            ImageCatalog imageCatalog = imageCatalogService.getImageCatalogByName(workspaceId, stackImageChangeRequest.getImageCatalogName());
            return new ImageChangeDto(stackId, stackImageChangeRequest.getImageId(), imageCatalog.getName(), imageCatalog.getImageCatalogUrl());
        } else {
            return new ImageChangeDto(stackId, stackImageChangeRequest.getImageId());
        }
    }

    private FlowIdentifier syncComponentVersionsFromCm(StackView stack, Set<String> candidateImageUuids) {
        return stackOperationService.syncComponentVersionsFromCm(stack, candidateImageUuids);
    }

    private FlowIdentifier put(StackDto stack, UpdateStackV4Request updateRequest) {
        MDCBuilder.buildMdcContext(stack);
        if (updateRequest.getStatus() != null) {
            return stackOperationService.updateStatus(stack, updateRequest.getStatus(), updateRequest.getWithClusterEvent());
        } else {
            Integer scalingAdjustment = updateRequest.getInstanceGroupAdjustment().getScalingAdjustment();
            validateHardLimits(scalingAdjustment);
            return stackOperationService.updateNodeCount(stack, updateRequest.getInstanceGroupAdjustment(), updateRequest.getWithClusterEvent());
        }
    }

    private FlowIdentifier putStartInstances(StackDto stack, UpdateStackV4Request updateRequest, ScalingStrategy scalingStrategy) {
        MDCBuilder.buildMdcContext(stack);
        if (updateRequest.getStatus() != null) {
            throw new BadRequestException(String.format("Stack status update is not supported while" +
                            " attempting to scale-up via instance start. Requested status: '%s' (File a bug)",
                    updateRequest.getStatus()));
        }
        if (scalingStrategy == null) {
            scalingStrategy = ScalingStrategy.STOPSTART;
            LOGGER.debug("Scaling strategy is null, and has been set to the default: {}", scalingStrategy);
        }
        Integer scalingAdjustment = updateRequest.getInstanceGroupAdjustment().getScalingAdjustment();
        validateHardLimits(scalingAdjustment);
        return stackOperationService.updateNodeCountStartInstances(stack, updateRequest.getInstanceGroupAdjustment(),
                updateRequest.getWithClusterEvent(), scalingStrategy);
    }

    private void validateHardLimits(Integer scalingAdjustment) {
        boolean forAutoscale = regionAwareInternalCrnGeneratorFactory.autoscale()
                .isInternalCrnForService(restRequestThreadLocalService.getCloudbreakUser().getUserCrn());
        boolean violatingMaxNodeCount = forAutoscale ?
                scalingHardLimitsService.isViolatingAutoscaleMaxStepInNodeCount(scalingAdjustment) :
                scalingHardLimitsService.isViolatingMaxUpscaleStepInNodeCount(scalingAdjustment);
        if (violatingMaxNodeCount) {
            throw new BadRequestException(String.format("Upscaling by more than %d nodes is not supported",
                    forAutoscale ? scalingHardLimitsService.getMaxAutoscaleStepInNodeCount() : scalingHardLimitsService.getMaxUpscaleStepInNodeCount()));
        }
    }

    @VisibleForTesting
    void validateNetworkScaleRequest(StackDto stack, NetworkScaleV4Request stackNetworkScaleV4Request) {
        if (stackNetworkScaleV4Request != null
                && stackNetworkScaleV4Request.getPreferredSubnetIds() != null
                && stackNetworkScaleV4Request.getPreferredSubnetIds().stream().anyMatch(StringUtils::isNotBlank)) {
            String platformVariant = stack.getPlatformVariant();
            boolean supportedVariant = multiAzValidator.supportedVariant(platformVariant);
            if (!supportedVariant) {
                String errorMessage = String.format("Multiple availability zones are not supported on platform variant '%s'", platformVariant);
                LOGGER.info(errorMessage);
                throw new BadRequestException(errorMessage);
            }
            Set<InstanceGroupView> instanceGroupViews = new HashSet<>(stack.getInstanceGroupViews());
            Set<String> subnetIds = multiAzValidator.collectSubnetIds(instanceGroupViews);
            if (subnetIds.size() < 2) {
                String message = "It does not make sense to prefer subnets on a cluster that has been provisioned in a single subnet";
                LOGGER.info(message);
                throw new BadRequestException(message);
            }
        }
    }

    public List<SubnetIdWithResourceNameAndCrn> getAllUsedSubnetsByEnvironmentCrn(String environmentCrn) {
        return instanceMetaDataService.findAllUsedSubnetsByEnvironmentCrn(environmentCrn);
    }

    public FlowIdentifier putDeleteVolumesInWorkspace(NameOrCrn nameOrCrn, String accountId, StackDeleteVolumesRequest deleteRequest) {
        StackView stackView = stackDtoService.getStackViewByNameOrCrn(nameOrCrn, accountId);
        Stack stack = stackService.getByIdWithLists(stackView.getId());
        MDCBuilder.buildMdcContext(stackView);
        LOGGER.debug("Validating Stack Delete Volumes Request for Stack ID {}", stackView.getId());
        validateDeleteVolumesRequest(stack, deleteRequest);
        return clusterCommonService.putDeleteVolumes(stackView.getResourceCrn(), deleteRequest);
    }

    private void validateDeleteVolumesRequest(Stack stack, StackDeleteVolumesRequest deleteRequest) {
        verticalScalingValidatorService.validateProviderForDelete(stack, "Deleting volumes", false);
        verticalScalingValidatorService.validateEntitlementForDelete(stack);
        verticalScalingValidatorService.validateInstanceTypeForDeletingDisks(stack, deleteRequest);
    }

    public FlowIdentifier putAddVolumesInWorkspace(NameOrCrn nameOrCrn, String accountId, StackAddVolumesRequest addVolumesRequest) {
        StackView stackView = stackDtoService.getStackViewByNameOrCrn(nameOrCrn, accountId);
        Stack stack = stackService.getByIdWithLists(stackView.getId());
        MDCBuilder.buildMdcContext(stack);
        LOGGER.debug("Validating Stack Add Volumes Request for Stack ID {}", stack.getId());
        verticalScalingValidatorService.validateProviderForAddVolumes(stack, "Adding volumes");
        return clusterCommonService.putAddVolumes(stack.getResourceCrn(), addVolumesRequest);
    }
}
