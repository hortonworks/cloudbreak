package com.sequenceiq.datalake.service.sdx;

import static com.sequenceiq.cloudbreak.common.exception.NotFoundException.notFound;
import static com.sequenceiq.cloudbreak.common.mappable.CloudPlatform.YARN;
import static com.sequenceiq.sdx.api.model.SdxClusterShape.ENTERPRISE;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.WebApplicationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.StackV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceGroupV4Base;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceMetadataType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackScaleV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.InstanceGroupV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.instancemetadata.InstanceMetaDataV4Response;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.exception.WebApplicationExceptionMessageExtractor;
import com.sequenceiq.datalake.entity.DatalakeInstanceGroupScalingDetails;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.flow.SdxReactorFlowManager;
import com.sequenceiq.datalake.repository.SdxClusterRepository;
import com.sequenceiq.datalake.service.sdx.flowcheck.CloudbreakFlowService;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.sdx.api.model.DatalakeHorizontalScaleRequest;

@Service
public class SdxHorizontalScalingService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SdxHorizontalScalingService.class);

    @Inject
    private SdxClusterRepository sdxClusterRepository;

    @Inject
    private SdxReactorFlowManager sdxReactorFlowManager;

    @Inject
    private StackV4Endpoint stackV4Endpoint;

    @Inject
    private EntitlementService entitlementService;

    @Inject
    private EnvironmentService environmentClientService;

    @Inject
    private WebApplicationExceptionMessageExtractor webApplicationExceptionMessageExtractor;

    @Inject
    private CloudbreakFlowService cloudbreakFlowService;

    @Inject
    private SdxService sdxService;

    public FlowIdentifier horizontalScaleDatalake(String name, DatalakeHorizontalScaleRequest scaleRequest) {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        if (!entitlementService.isDatalakeHorizontalScaleEnabled(accountId)) {
            throw new BadRequestException(String.format("Data lake horizontal scale is not enabled for account %s.", accountId));
        }

        LOGGER.info("Horizontal scaling Data lake with name {}", name);
        SdxCluster sdxCluster = sdxClusterRepository.findByAccountIdAndClusterNameAndDeletedIsNullAndDetachedIsFalse(accountId, name)
                .orElseThrow(() -> notFound("SDX cluster", name).get());
        return sdxReactorFlowManager.triggerHorizontalScaleDataLake(sdxCluster, scaleRequest);
    }

    public String triggerScalingFlow(SdxCluster sdxCluster, StackScaleV4Request scaleRequest) {
        try {
            FlowIdentifier flowIdentifier = ThreadBasedUserCrnProvider.doAsInternalActor(
                    () -> stackV4Endpoint.putScaling(0L, sdxCluster.getClusterName(), scaleRequest, sdxCluster.getAccountId())
            );
            cloudbreakFlowService.saveLastCloudbreakFlowChainId(sdxCluster, flowIdentifier);
            return flowIdentifier.getPollableId();
        } catch (jakarta.ws.rs.NotFoundException e) {
            LOGGER.info("Cannot find stack on Cloudbreak side {}", sdxCluster.getClusterName());
            throw new RuntimeException("Cannot find stack on Cloudbreak side.", e);
        } catch (WebApplicationException e) {
            String errorMessage = webApplicationExceptionMessageExtractor.getErrorMessage(e);
            if (errorMessage.contains("is currently in 'UPDATE_IN_PROGRESS' status")) {
                LOGGER.warn("Flow already running in core, possibly datalake flow was restarted, return with last flow chain id: {}.",
                        sdxCluster.getLastCbFlowChainId());
                return sdxCluster.getLastCbFlowChainId();
            }
            LOGGER.info("Cannot horizontal scale stack {} from cloudbreak: {}", sdxCluster.getStackId(), errorMessage, e);
            throw new RuntimeException(errorMessage);
        } catch (ProcessingException e) {
            throw new RuntimeException(e.getMessage());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void validateHorizontalScaleRequest(SdxCluster sdxCluster, DatalakeHorizontalScaleRequest request) {
        if (ENTERPRISE != sdxCluster.getClusterShape()) {
            throw new BadRequestException(String.format("Horizontal scaling not supported on: %s. Please use ENTERPRISE Data lake shape",
                    sdxCluster.getClusterShape().name()));
        }
        String hostGroup = request.getGroup();
        if (!DatalakeInstanceGroupScalingDetails.valueOf(hostGroup.toUpperCase(Locale.ROOT)).isScalable()) {
            throw new BadRequestException(String.format("%s hostgroup cannot be scaled", hostGroup));
        }
        DetailedEnvironmentResponse environmentResponse = environmentClientService.getByName(sdxCluster.getEnvName());
        if (!EnvironmentStatus.upscalable().contains(environmentResponse.getEnvironmentStatus())) {
            throw new BadRequestException(String.format("The Data lake cannot be scaled in the %s environment status.",
                    environmentResponse.getEnvironmentStatus().name()));
        }
        StackV4Response stackResponse = stackV4Endpoint.getByCrn(1L, sdxCluster.getStackCrn(), Set.of());
        if (YARN == stackResponse.getCloudPlatform()) {
            throw new BadRequestException("The Data lake horizontal scaling is not supported on YCloud");
        }
        Optional<String> nodeRequestValid = validateNodeRequest(stackResponse, request, sdxCluster);
        if (nodeRequestValid.isPresent()) {
            throw new BadRequestException(nodeRequestValid.get());
        }
    }

    public FlowIdentifier rollingRestartServices(Long resourceId, String resourceCrn) {
        SdxCluster sdxCluster = sdxService.getById(resourceId);
        FlowIdentifier flowId = stackV4Endpoint.rollingRestartServices(0L, resourceCrn, true);
        cloudbreakFlowService.saveLastCloudbreakFlowChainId(sdxCluster, flowId);
        return flowId;
    }

    private Optional<String> validateNodeRequest(StackV4Response stack, DatalakeHorizontalScaleRequest request, SdxCluster sdxCluster) {
        if (request.getDesiredCount() < 0) {
            LOGGER.warn("Negative nodeCount was requested.");
            return Optional.of("Negative nodeCount is not accepted. If you want to downscale your Data lake please " +
                    "use lower number than the actual node count");
        }
        String target = request.getGroup();
        String gatewayInstanceName = DatalakeInstanceGroupScalingDetails.GATEWAY.getName().toLowerCase(Locale.ROOT);
        Map<String, Set<InstanceMetaDataV4Response>> instanceMap = getInstanceMap(stack, target, gatewayInstanceName);
        if (!instanceMap.containsKey(target)) {
            LOGGER.warn("The requested hostgroup name not found!");
            return Optional.of(String.format("The requested hostgroup name not found! The requested hostgroup name is %s", target));
        }
        if (!instanceMap.containsKey(gatewayInstanceName)) {
            LOGGER.error("Gateway node is not found in the Stack Instance Map. The horizontal scale cannot be performed while the CM is not " +
                    "available StackId: {}", stack.getId());
            List<String> instanceGroupList = stack.getInstanceGroups().stream().map(InstanceGroupV4Base::getName).toList();
            return Optional.of(String.format("The Gateway instance not found. The horizontal scale cannot be performed while the CM is not reachable" +
                    ", found instances: %s", instanceGroupList));
        }
        List<InstanceMetaDataV4Response> gatewayMetaData = getPrimaryGatewayUnhealthyMetadata(gatewayInstanceName, instanceMap);
        if (!gatewayMetaData.isEmpty()) {
            LOGGER.warn("Gateway(CM node) is not healthy. Can not start horizontal scaling");
            List<InstanceStatus> instanceStatuses = gatewayMetaData.stream().map(InstanceMetaDataV4Response::getInstanceStatus).toList();
            return Optional.of(String.format("Gateway instances are not healthy. Please repair the node and retry horizontal" +
                    " scaling Actual Gateway statuses: %s", instanceStatuses));
        }
        DatalakeInstanceGroupScalingDetails targetInstanceGroupName = DatalakeInstanceGroupScalingDetails.valueOf(request.getGroup().toUpperCase(Locale.ROOT));
        if (targetInstanceGroupName.getMinimumNodeCount() > request.getDesiredCount()) {
            LOGGER.warn("Requested nodeCount is less than the minimum nodeCount.");
            return Optional.of(String.format("Requested node count is less than the minimum node count. The minimum node count for %s is %d",
                    targetInstanceGroupName.name(), targetInstanceGroupName.getMinimumNodeCount()));
        }
        return isScalingAllowedOnHostGroup(stack, request, sdxCluster, targetInstanceGroupName);
    }

    private Optional<String> isScalingAllowedOnHostGroup(StackV4Response stack, DatalakeHorizontalScaleRequest request, SdxCluster sdxCluster,
            DatalakeInstanceGroupScalingDetails targetInstanceGroupName) {
        if (DatalakeInstanceGroupScalingDetails.RAZ_SCALE_OUT == targetInstanceGroupName && !sdxCluster.isRangerRazEnabled()) {
            return Optional.of("The RAZ hostgroup cannot be scaled. RAZ is not enabled for this environment. Please enable RAZ for the environment " +
                    "and try again");
        }
        if (isDownscaleBlocked(targetInstanceGroupName) && isDownscale(stack, request)) {
            return Optional.of(String.format(" %s can not be downscaled as it will cause data loss", targetInstanceGroupName));
        }
        return Optional.empty();
    }

    private boolean isDownscaleBlocked(DatalakeInstanceGroupScalingDetails targetInstanceGroupName) {
        return switch (targetInstanceGroupName) {
            case STORAGE_SCALE_OUT, KAFKA_SCALE_OUT, SOLR_SCALE_OUT -> true;
            default -> false;
        };
    }

    private boolean isInstanceRunning(InstanceStatus status) {
        return switch (status) {
            case CREATED, SERVICES_RUNNING, SERVICES_HEALTHY, DECOMMISSIONED, DECOMMISSION_FAILED -> true;
            default -> false;
        };
    }

    private boolean isDownscale(StackV4Response stack, DatalakeHorizontalScaleRequest request) {
        return stack.getInstanceGroups().stream()
                .filter(instance -> instance.getName().equals(request.getGroup().toLowerCase(Locale.ROOT)))
                .map(InstanceGroupV4Base::getNodeCount)
                .mapToInt(Integer::intValue)
                .sum() > request.getDesiredCount();
    }

    private Map<String, Set<InstanceMetaDataV4Response>> getInstanceMap(StackV4Response stack, String target, String gatewayInstanceName) {
        return stack.getInstanceGroups().stream()
                .filter(instance -> instance.getName().equalsIgnoreCase(target) || instance.getName().equalsIgnoreCase(gatewayInstanceName))
                .collect(Collectors.toMap(instance -> instance.getName().toLowerCase(Locale.ROOT), InstanceGroupV4Response::getMetadata));
    }

    private List<InstanceMetaDataV4Response> getPrimaryGatewayUnhealthyMetadata(String gatewayInstanceName,
            Map<String, Set<InstanceMetaDataV4Response>> instanceMap) {
        return instanceMap.get(gatewayInstanceName)
                .stream()
                .filter(instance -> instance.getInstanceType() == InstanceMetadataType.GATEWAY_PRIMARY && !isInstanceRunning(instance.getInstanceStatus()))
                .toList();
    }
}
