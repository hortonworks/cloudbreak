package com.sequenceiq.cloudbreak.service;

import static com.sequenceiq.cloudbreak.common.anonymizer.AnonymizerUtil.anonymize;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.STACK_RETRY_FLOW_START;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.autoscales.request.UpdateStackV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.autoscales.response.CertificateV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.endpoint.v4.dto.NameOrCrn;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.StatusRequest;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.ClusterRepairV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackImageChangeV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackScaleV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.UpdateClusterV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.network.NetworkScaleV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.AutoscaleStackV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.GeneratedBlueprintV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.auth.crn.InternalCrnBuilder;
import com.sequenceiq.cloudbreak.common.ScalingHardLimitsService;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.controller.StackCreatorService;
import com.sequenceiq.cloudbreak.controller.validation.network.MultiAzValidator;
import com.sequenceiq.cloudbreak.converter.v4.stacks.StackScaleV4RequestToUpdateClusterV4RequestConverter;
import com.sequenceiq.cloudbreak.converter.v4.stacks.StackScaleV4RequestToUpdateStackV4RequestConverter;
import com.sequenceiq.cloudbreak.converter.v4.stacks.StackV4RequestToTemplatePreparationObjectConverter;
import com.sequenceiq.cloudbreak.domain.ImageCatalog;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.service.cluster.ClusterRepairService;
import com.sequenceiq.cloudbreak.service.cluster.flow.ClusterOperationService;
import com.sequenceiq.cloudbreak.service.image.ImageCatalogService;
import com.sequenceiq.cloudbreak.service.image.ImageChangeDto;
import com.sequenceiq.cloudbreak.service.stack.CloudParameterCache;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.service.stack.flow.StackOperationService;
import com.sequenceiq.cloudbreak.service.stack.flow.UpdateNodeCountValidator;
import com.sequenceiq.cloudbreak.service.user.UserService;
import com.sequenceiq.cloudbreak.service.workspace.WorkspaceService;
import com.sequenceiq.cloudbreak.structuredevent.CloudbreakRestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.structuredevent.event.CloudbreakEventService;
import com.sequenceiq.cloudbreak.template.BlueprintUpdaterConnectors;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
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
    private WorkspaceService workspaceService;

    @Inject
    private ClusterOperationService clusterOperationService;

    @Inject
    private ClusterRepairService clusterRepairService;

    @Inject
    private StackService stackService;

    @Inject
    private StackOperationService stackOperationService;

    @Inject
    private UserService userService;

    @Inject
    private BlueprintUpdaterConnectors blueprintUpdaterConnectors;

    @Inject
    private NodeCountLimitValidator nodeCountLimitValidator;

    @Inject
    private UpdateNodeCountValidator updateNodeCountLimitValidator;

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

    public StackV4Response createInWorkspace(StackV4Request stackRequest, User user, Workspace workspace, boolean distroxRequest) {
        return stackCreatorService.createStack(user, workspace, stackRequest, distroxRequest);
    }

    public StackV4Response get(Long id, Set<String> entries) {
        return stackService.getJsonById(id, entries);
    }

    public StackV4Response getByCrn(String crn, Set<String> entries) {
        return stackService.getJsonByCrn(crn, entries);
    }

    public StackV4Response findStackByNameOrCrnAndWorkspaceId(NameOrCrn nameOrCrn, Long workspaceId, Set<String> entries, StackType stackType) {
        return nameOrCrn.hasName()
                ? findStackByNameAndWorkspaceId(nameOrCrn.getName(), workspaceId, entries, stackType)
                : findStackByCrnAndWorkspaceId(nameOrCrn.getCrn(), workspaceId, entries, stackType);
    }

    private StackV4Response findStackByNameAndWorkspaceId(String name, Long workspaceId, Set<String> entries, StackType stackType) {
        User user = userService.getOrCreate(restRequestThreadLocalService.getCloudbreakUser());
        return stackService.getByNameInWorkspaceWithEntries(name, workspaceId, entries, user, stackType);
    }

    private StackV4Response findStackByCrnAndWorkspaceId(String crn, Long workspaceId, Set<String> entries, StackType stackType) {
        User user = userService.getOrCreate(restRequestThreadLocalService.getCloudbreakUser());
        return stackService.getByCrnInWorkspaceWithEntries(crn, workspaceId, entries, user, stackType);
    }

    public void putInDefaultWorkspace(String crn, UpdateStackV4Request updateRequest) {
        Stack stack = stackService.getByCrn(crn);
        MDCBuilder.buildMdcContext(stack);
        put(stack, updateRequest);
    }

    public FlowIdentifier putStopInWorkspace(NameOrCrn nameOrCrn, Long workspaceId) {
        Stack stack = stackService.getByNameOrCrnInWorkspace(nameOrCrn, workspaceId);
        MDCBuilder.buildMdcContext(stack);
        if (!cloudParameterCache.isStartStopSupported(stack.cloudPlatform())) {
            throw new BadRequestException(String.format("Stop is not supported on %s cloudplatform", stack.cloudPlatform()));
        }
        UpdateStackV4Request updateStackJson = new UpdateStackV4Request();
        updateStackJson.setStatus(StatusRequest.STOPPED);
        updateStackJson.setWithClusterEvent(true);
        return put(stack, updateStackJson);
    }

    public FlowIdentifier syncInWorkspace(NameOrCrn nameOrCrn, Long workspaceId) {
        Stack stack = stackService.getByNameOrCrnInWorkspace(nameOrCrn, workspaceId);
        MDCBuilder.buildMdcContext(stack);
        UpdateStackV4Request updateStackJson = new UpdateStackV4Request();
        updateStackJson.setStatus(StatusRequest.FULL_SYNC);
        updateStackJson.setWithClusterEvent(true);
        return put(stack, updateStackJson);
    }

    public FlowIdentifier syncComponentVersionsFromCmInWorkspace(NameOrCrn nameOrCrn, Long workspaceId, Set<String> candidateImageUuids) {
        Stack stack = stackService.getByNameOrCrnInWorkspace(nameOrCrn, workspaceId);
        MDCBuilder.buildMdcContext(stack);
        if (stack.getStackStatus().getStatus().isStopState()) {
            String message = String.format("Reading CM and parcel versions from CM cannot be initiated as the cluster is in %s state",
                    stack.getStackStatus().getStatus());
            LOGGER.debug(message);
            throw new BadRequestException(message);
        }

        LOGGER.debug("Triggering sync from CM to db: syncing versions from CM to db, nameOrCrn: {}, workspaceId: {}, candidateImageUuids: {}",
                nameOrCrn, workspaceId, candidateImageUuids);
        return syncComponentVersionsFromCm(stack, candidateImageUuids);
    }

    public FlowIdentifier deleteMultipleInstancesInWorkspace(NameOrCrn nameOrCrn, Long workspaceId, Set<String> instanceIds, boolean forced) {
        Optional<Stack> stack = stackService.findStackByNameOrCrnAndWorkspaceId(nameOrCrn, workspaceId);
        if (stack.isEmpty()) {
            throw new BadRequestException("The requested Data Hub does not exist.");
        }
        validateStackIsNotDataLake(stack.orElse(null), instanceIds);
        return stackOperationService.removeInstances(stack.orElse(null), instanceIds, forced);
    }

    public FlowIdentifier putStartInWorkspace(NameOrCrn nameOrCrn, Long workspaceId) {
        Stack stack = stackService.getByNameOrCrnInWorkspace(nameOrCrn, workspaceId);
        return putStartInWorkspace(stack);
    }

    private FlowIdentifier putStartInWorkspace(Stack stack) {
        MDCBuilder.buildMdcContext(stack);
        if (!cloudParameterCache.isStartStopSupported(stack.cloudPlatform())) {
            throw new BadRequestException(String.format("Start is not supported on %s cloudplatform", stack.cloudPlatform()));
        }
        UpdateStackV4Request updateStackJson = new UpdateStackV4Request();
        updateStackJson.setStatus(StatusRequest.STARTED);
        updateStackJson.setWithClusterEvent(true);
        return put(stack, updateStackJson);
    }

    public FlowIdentifier putScalingInWorkspace(NameOrCrn nameOrCrn, Long workspaceId, StackScaleV4Request updateRequest) {
        User user = userService.getOrCreate(restRequestThreadLocalService.getCloudbreakUser());
        Stack stack = stackService.getByNameOrCrnInWorkspace(nameOrCrn, workspaceId);


        MDCBuilder.buildMdcContext(stack);
        updateRequest.setStackId(stack.getId());
        validateNetworkScaleRequest(stack, updateRequest.getStackNetworkScaleV4Request());
        UpdateStackV4Request updateStackJson = stackScaleV4RequestToUpdateStackV4RequestConverter.convert(updateRequest);
        Integer scalingAdjustment = updateStackJson.getInstanceGroupAdjustment().getScalingAdjustment();
        validateScalingRequest(stack, scalingAdjustment);

        FlowIdentifier flowIdentifier;
        if (scalingAdjustment > 0) {
            flowIdentifier = put(stack, updateStackJson);
        } else {
            updateNodeCountLimitValidator.validateClouderaManagerLimitationForDownscale(Map.of(updateRequest.getGroup(), scalingAdjustment));
            UpdateClusterV4Request updateClusterJson = stackScaleV4RequestToUpdateClusterV4RequestConverter.convert(updateRequest);
            workspaceService.get(workspaceId, user);
            flowIdentifier = clusterCommonService.put(stack.getResourceCrn(), updateClusterJson);
        }
        return flowIdentifier;
    }

    private void validateScalingRequest(Stack stack, Integer scalingAdjustment) {
        if (scalingAdjustment > 0 && !cloudParameterCache.isUpScalingSupported(stack.cloudPlatform())) {
            throw new BadRequestException(String.format("Upscaling is not supported on %s cloudplatform", stack.cloudPlatform()));
        }
        if (scalingAdjustment < 0 && !cloudParameterCache.isDownScalingSupported(stack.cloudPlatform())) {
            throw new BadRequestException(String.format("Downscaling is not supported on %s cloudplatform", stack.cloudPlatform()));
        }
        nodeCountLimitValidator.validateScale(stack.getId(), scalingAdjustment);
    }

    public void deleteWithKerberosInWorkspace(NameOrCrn nameOrCrn, Long workspaceId, boolean forced) {
        LOGGER.info("Trying to delete stack");
        Stack stack = stackService.getByNameOrCrnInWorkspace(nameOrCrn, workspaceId);
        MDCBuilder.buildMdcContext(stack);
        clusterOperationService.delete(stack.getId(), forced);
    }

    public FlowIdentifier repairCluster(Long workspaceId, NameOrCrn nameOrCrn, ClusterRepairV4Request clusterRepairRequest) {
        Stack stack = stackService.getByNameOrCrnInWorkspace(nameOrCrn, workspaceId);
        if (clusterRepairRequest.getHostGroups() != null) {
            return clusterRepairService.repairHostGroups(stack.getId(), new HashSet<>(clusterRepairRequest.getHostGroups()),
                    clusterRepairRequest.isRemoveOnly(), clusterRepairRequest.isRestartServices());
        } else {
            return clusterRepairService.repairNodes(stack.getId(),
                    new HashSet<>(clusterRepairRequest.getNodes().getIds()),
                    clusterRepairRequest.getNodes().isDeleteVolumes(),
                    clusterRepairRequest.isRemoveOnly(), clusterRepairRequest.isRestartServices());
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

    public FlowIdentifier deleteInstanceInWorkspace(NameOrCrn nameOrCrn, Long workspaceId, String instanceId, boolean forced) {
        Optional<Stack> stack = stackService.findStackByNameOrCrnAndWorkspaceId(nameOrCrn, workspaceId);
        if (stack.isEmpty()) {
            throw new BadRequestException("The requested Data Hub does not exist.");
        }
        validateStackIsNotDataLake(stack.orElse(null), Set.of(instanceId));
        return stackOperationService.removeInstance(stack.orElse(null), instanceId, forced);
    }

    private void validateStackIsNotDataLake(Stack stack, Set<String> instanceIds) {
        if (StackType.DATALAKE.equals(stack.getType())) {
            if (instanceIds.size() == 1) {
                throw new BadRequestException(String.format("%s is a node of a data lake cluster, therefore it's not allowed to delete it.",
                        List.copyOf(instanceIds).get(0)));
            } else {
                throw new BadRequestException(String.format("%s are nodes of a data lake cluster, therefore it's not allowed to delete them.",
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

    private FlowIdentifier syncComponentVersionsFromCm(Stack stack, Set<String> candidateImageUuids) {
        return stackOperationService.syncComponentVersionsFromCm(stack, candidateImageUuids);
    }

    private FlowIdentifier put(Stack stack, UpdateStackV4Request updateRequest) {
        MDCBuilder.buildMdcContext(stack);
        User user = userService.getOrCreate(restRequestThreadLocalService.getCloudbreakUser());
        if (updateRequest.getStatus() != null) {
            return stackOperationService.updateStatus(stack.getId(), updateRequest.getStatus(), updateRequest.getWithClusterEvent(), user);
        } else {
            Integer scalingAdjustment = updateRequest.getInstanceGroupAdjustment().getScalingAdjustment();
            validateHardLimits(scalingAdjustment);
            return stackOperationService.updateNodeCount(stack, updateRequest.getInstanceGroupAdjustment(), updateRequest.getWithClusterEvent());
        }
    }

    private void validateHardLimits(Integer scalingAdjustment) {
        boolean forAutoscale = InternalCrnBuilder.isInternalCrnForService(restRequestThreadLocalService.getCloudbreakUser().getUserCrn(),
                Crn.Service.AUTOSCALE);
        boolean violatingMaxNodeCount = forAutoscale ?
                scalingHardLimitsService.isViolatingAutoscaleMaxStepInNodeCount(scalingAdjustment) :
                scalingHardLimitsService.isViolatingMaxUpscaleStepInNodeCount(scalingAdjustment);
        if (violatingMaxNodeCount) {
            throw new BadRequestException(String.format("Upscaling by more than %d nodes is not supported",
                    forAutoscale ? scalingHardLimitsService.getMaxAutoscaleStepInNodeCount() : scalingHardLimitsService.getMaxUpscaleStepInNodeCount()));
        }
    }

    private void validateNetworkScaleRequest(Stack stack, NetworkScaleV4Request stackNetworkScaleV4Request) {
        if (stackNetworkScaleV4Request != null && CollectionUtils.isNotEmpty(stackNetworkScaleV4Request.getPreferredSubnetIds())) {
            String platformVariant = stack.getPlatformVariant();
            boolean supportedVariant = multiAzValidator.supportedVariant(platformVariant);
            if (!supportedVariant) {
                String errorMessage = String.format("Multiple availability zones are not supported on platform variant '%s'", platformVariant);
                LOGGER.info(errorMessage);
                throw new BadRequestException(errorMessage);
            }
            Set<String> subnetIds = multiAzValidator.collectSubnetIds(stack.getInstanceGroups());
            if (subnetIds.size() < 2) {
                String message = "It does not make sense to prefer subnets on a cluster that has been provisioned in a single subnet";
                LOGGER.info(message);
                throw new BadRequestException(message);
            }
        }
    }
}
