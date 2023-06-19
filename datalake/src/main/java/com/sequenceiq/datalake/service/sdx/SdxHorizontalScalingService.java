package com.sequenceiq.datalake.service.sdx;

import static com.sequenceiq.cloudbreak.common.exception.NotFoundException.notFound;
import static com.sequenceiq.sdx.api.model.SdxClusterShape.ENTERPRISE;

import java.util.Locale;
import java.util.Set;

import javax.inject.Inject;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.WebApplicationException;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.StackV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackScaleV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.InstanceGroupV4Response;
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

    public FlowIdentifier horizontalScaleDatalake(String name, DatalakeHorizontalScaleRequest scaleRequest) {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
//        if (!entitlementService.isDatalakeHorizontalScaleEnabled(accountId)) {
//            throw new BadRequestException(String.format("Datalake horizontal scale is not enabled for account %s.", accountId));
//        }

        LOGGER.info("Horizontal scaling Datalake with name {}", name);
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
        if (!ENTERPRISE.equals(sdxCluster.getClusterShape())) {
            throw new BadRequestException(String.format("scaling not supported on: %s. " +
                    "Only the ENTERPRISE Datalake shape support the horizontal scaling functionality.", sdxCluster.getClusterShape().name()));
        }
        String hostGroup = request.getGroup();
        if (!DatalakeInstanceGroupScalingDetails.valueOf(hostGroup.toUpperCase(Locale.ROOT)).isScalable()) {
            throw new BadRequestException(String.format("%s host group cannot scale", hostGroup));
        }
        DetailedEnvironmentResponse environmentResponse = environmentClientService.getByName(sdxCluster.getEnvName());
        if (!EnvironmentStatus.upscalable().contains(environmentResponse.getEnvironmentStatus())) {
            throw new BadRequestException(String.format("The DAtalake cannot be scaled because the environment %s is in the %s status.",
                    sdxCluster.getEnvName(), environmentResponse.getEnvironmentStatus().name()));
        }
        StackV4Response stackResponse = stackV4Endpoint.getByCrn(1L, sdxCluster.getStackCrn(), Set.of());
        String nodeRequestValidity = isNodeRequestValid(stackResponse, request);
        if (StringUtils.isNotEmpty(nodeRequestValidity)) {
            throw new BadRequestException(String.format("The requested nodeCount does not reach the minimum condition of horizontal scaling. Issues: %s",
                    nodeRequestValidity));
        }
    }

    private String isNodeRequestValid(StackV4Response stack, DatalakeHorizontalScaleRequest request) {
        if (request.getDesiredCount() < 0) {
            LOGGER.warn("Negative nodeCount was requested.");
            return "Negative nodeCount is not accepted. If you want to downsacle your cluster please use lower number than the acutal node count";
        }
        String target = request.getGroup();
        boolean hostGroupNotExists = stack.getInstanceGroups().stream()
                .map(InstanceGroupV4Response::getName)
                .noneMatch(target::equalsIgnoreCase);
        if (hostGroupNotExists) {
            LOGGER.warn("The requested hostgroup name not found!");
            return String.format("The requested hostgroup name not found! The requested host group name is %s", target);
        }
        DatalakeInstanceGroupScalingDetails instanceGroupName = DatalakeInstanceGroupScalingDetails.valueOf(request.getGroup().toUpperCase(Locale.ROOT));
        if (instanceGroupName.getMinimumNodeCount() > request.getDesiredCount()) {
            LOGGER.warn("Requested nodeCount is less than the minimum nodeCount.");
            return String.format("Requested nodeCount is less than the minimum nodeCount. The minimum node cont for %s is %d",
                    instanceGroupName.name(), instanceGroupName.getMinimumNodeCount());
        }
        return "";
    }
}
