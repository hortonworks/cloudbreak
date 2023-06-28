package com.sequenceiq.datalake.service.sdx;

import static com.sequenceiq.cloudbreak.common.exception.NotFoundException.notFound;
import static com.sequenceiq.cloudbreak.common.mappable.CloudPlatform.YARN;
import static com.sequenceiq.sdx.api.model.SdxClusterShape.ENTERPRISE;
import static java.util.function.Predicate.*;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.WebApplicationException;

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
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.exception.WebApplicationExceptionMessageExtractor;
import com.sequenceiq.datalake.entity.DatalakeInstanceGroupScalingDetails;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.flow.SdxReactorFlowManager;
import com.sequenceiq.datalake.repository.SdxClusterRepository;
import com.sequenceiq.datalake.service.EnvironmentClientService;
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
    private EnvironmentClientService environmentClientService;

    @Inject
    private RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    @Inject
    private WebApplicationExceptionMessageExtractor webApplicationExceptionMessageExtractor;

    @Inject
    private CloudbreakFlowService cloudbreakFlowService;

    private static Set<InstanceMetaDataV4Response> getInstances(StackV4Response stack, String target) {
        return stack.getInstanceGroups().stream()
                .filter(instance -> instance.getName().equalsIgnoreCase(target))
                .map(InstanceGroupV4Response::getMetadata)
                .flatMap(Collection::stream)
                .collect(Collectors.toSet());
    }

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
                    regionAwareInternalCrnGeneratorFactory.iam().getInternalCrnForServiceAsString(),
                    () -> stackV4Endpoint.putScaling(0L, sdxCluster.getClusterName(), scaleRequest, sdxCluster.getAccountId())
            );
            cloudbreakFlowService.saveLastCloudbreakFlowChainId(sdxCluster, flowIdentifier);
            return flowIdentifier.getPollableId();
        } catch (javax.ws.rs.NotFoundException e) {
            LOGGER.info("Cannot find stack on Cloudbreak side {}", sdxCluster.getClusterName());
            throw new RuntimeException("Cannot find stack on Cloudbreak side.", e);
        } catch (WebApplicationException e) {
            String errorMessage = webApplicationExceptionMessageExtractor.getErrorMessage(e);
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
            throw new BadRequestException(String.format("%s hostgroup cannot scale", hostGroup));
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
        assertNodeRequestValid(stackResponse, request);
    }

    private void assertNodeRequestValid(StackV4Response stack, DatalakeHorizontalScaleRequest request) {
        if (request.getDesiredCount() < 0) {
            LOGGER.warn("Negative nodeCount was requested.");
            throw new BadRequestException(
                    "Negative nodeCount is not accepted. If you want to downscale your Data lake please use lower number than the actual node count");
        }
        String target = request.getGroup();
        String gatewayInstanceName = DatalakeInstanceGroupScalingDetails.GATEWAY.getName().toLowerCase(Locale.ROOT);
        Set<InstanceMetaDataV4Response> targetGroupInstances = getInstances(stack, target);
        Set<InstanceMetaDataV4Response> gatewayInstances = getInstances(stack, target);
        if (targetGroupInstances.isEmpty()) {
            LOGGER.warn("The requested hostgroup name not found!");
            throw new BadRequestException("The requested hostgroup name not found! The requested hostgroup name is " + target);
        }
        if (gatewayInstances.isEmpty()) {
            LOGGER.error("Gateway node is not found in the Stack Instance Map. The horizontal scale cannot be performed while the CM is not " +
                    "available StackId: {}", stack.getId());
            List<String> instanceGroupList = stack.getInstanceGroups().stream().map(InstanceGroupV4Base::getName).toList();
            throw new BadRequestException(
                    "The Gateway instance not found. The horizontal scale cannot be performed while the CM is not reachable, found instances: " + instanceGroupList);
        }
        List<InstanceStatus> gatewayInstancesStatuses = gatewayInstances.stream()
                .filter(data -> data.getInstanceType() == InstanceMetadataType.GATEWAY_PRIMARY)
                .map(InstanceMetaDataV4Response::getInstanceStatus)
                .toList();
        boolean gatewaysHasFaultyInstances = gatewayInstancesStatuses.stream()
                .anyMatch(not(status -> status == InstanceStatus.SERVICES_HEALTHY || status == InstanceStatus.SERVICES_RUNNING));
        if (gatewaysHasFaultyInstances) {
            LOGGER.warn("Gateway(CM node) is not healthy. Can not start horizontal scaling");
            throw new BadRequestException("Gateway instances are not healthy. Please repair the node and retry horizontal scaling Actual Gateway statuses: " + gatewayInstancesStatuses);
        }
        DatalakeInstanceGroupScalingDetails targetInstanceGroupName = DatalakeInstanceGroupScalingDetails.valueOf(request.getGroup().toUpperCase(Locale.ROOT));
        if (targetInstanceGroupName.getMinimumNodeCount() > request.getDesiredCount()) {
            LOGGER.warn("Requested nodeCount is less than the minimum nodeCount.");
            throw new BadRequestException(String.format("Requested node count is less than the minimum node count. The minimum node cont for %s is %d",
                    targetInstanceGroupName.name(), targetInstanceGroupName.getMinimumNodeCount()));
        }
        if (isDownscaleBlocked(targetInstanceGroupName) && isDownscale(stack, request)) {
            throw new BadRequestException("The storage hostgroup down scale is not supported, because it can cause data loss");
        }
    }

    private boolean isDownscaleBlocked(DatalakeInstanceGroupScalingDetails targetInstanceGroupName) {
        return switch (targetInstanceGroupName) {
            case STORAGEHG, KAFKAHG, SOLRHG -> false;
            default -> true;
        };
    }

    private boolean isInstanceRunning(InstanceStatus status) {
        return switch (status) {
            case CREATED, SERVICES_RUNNING, SERVICES_HEALTHY, DECOMMISSIONED, DECOMMISSION_FAILED -> true;
            default -> false;
        };
    }

    private boolean isDownscale(StackV4Response stack, DatalakeHorizontalScaleRequest request) {
        int currentNodeCount = stack.getInstanceGroups().stream()
                .filter(instance -> instance.getName().equals(request.getGroup().toLowerCase(Locale.ROOT)))
                .map(InstanceGroupV4Base::getNodeCount)
                .mapToInt(Integer::intValue)
                .sum();
        return currentNodeCount > request.getDesiredCount();
    }
}
