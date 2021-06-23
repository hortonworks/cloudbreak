package com.sequenceiq.environment.environment.service;

import static com.sequenceiq.cloudbreak.common.mappable.CloudPlatform.AWS;
import static com.sequenceiq.environment.environment.service.LoadBalancerPollerService.LOAD_BALANCER_UPDATE_FAILED_STATE;
import static java.util.Objects.requireNonNull;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.google.common.annotations.VisibleForTesting;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.Crn;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.common.api.type.PublicEndpointAccessGateway;
import com.sequenceiq.environment.api.v1.environment.model.base.LoadBalancerUpdateStatus;
import com.sequenceiq.environment.api.v1.environment.model.response.ClusterLbUpdateStatus;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentLbUpdateStatusResponse;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.domain.LbUpdateFlowLog;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.dto.EnvironmentLoadBalancerDto;
import com.sequenceiq.environment.environment.flow.EnvironmentReactorFlowManager;
import com.sequenceiq.environment.environment.flow.loadbalancer.config.LoadBalancerUpdateFlowConfig;
import com.sequenceiq.environment.network.service.LbUpdateFlowLogService;
import com.sequenceiq.environment.network.service.LoadBalancerEntitlementService;
import com.sequenceiq.flow.api.FlowEndpoint;
import com.sequenceiq.flow.api.model.FlowLogResponse;
import com.sequenceiq.flow.service.FlowService;

@Service
public class EnvironmentLoadBalancerService {

    static final String UNKNOWN_STATE = "UNKNOWN";

    private static final Logger LOGGER = LoggerFactory.getLogger(EnvironmentLoadBalancerService.class);

    private final EnvironmentService environmentService;

    private final EnvironmentReactorFlowManager reactorFlowManager;

    private final EntitlementService entitlementService;

    private final LoadBalancerEntitlementService loadBalancerEntitlementService;

    private final FlowService flowService;

    private final FlowEndpoint flowEndpoint;

    private final LbUpdateFlowLogService lbUpdateFlowLogService;

    public EnvironmentLoadBalancerService(
            EnvironmentService environmentService,
            EnvironmentReactorFlowManager reactorFlowManager,
            EntitlementService entitlementService,
            LoadBalancerEntitlementService loadBalancerEntitlementService,
            FlowService flowService,
            FlowEndpoint flowEndpoint,
            LbUpdateFlowLogService lbUpdateFlowLogService) {
        this.environmentService = environmentService;
        this.reactorFlowManager = reactorFlowManager;
        this.entitlementService = entitlementService;
        this.loadBalancerEntitlementService = loadBalancerEntitlementService;
        this.flowService = flowService;
        this.flowEndpoint = flowEndpoint;
        this.lbUpdateFlowLogService = lbUpdateFlowLogService;
    }

    public void updateLoadBalancerInEnvironmentAndStacks(EnvironmentDto environmentDto, EnvironmentLoadBalancerDto environmentLbDto) {
        requireNonNull(environmentDto);
        requireNonNull(environmentLbDto);

        loadBalancerEntitlementService.validateNetworkForEndpointGateway(environmentDto.getCloudPlatform(), environmentDto.getName(),
            environmentLbDto.getEndpointAccessGateway());

        if (!isLoadBalancerEnabledForDatalake(ThreadBasedUserCrnProvider.getAccountId(), environmentDto.getCloudPlatform(),
            environmentLbDto.getEndpointAccessGateway())) {
            throw new BadRequestException("Neither Endpoint Gateway nor Data Lake load balancer is enabled. Nothing to do.");
        }

        LOGGER.debug("Trying to find environment based on name {}, CRN {}", environmentDto.getName(), environmentDto.getResourceCrn());
        String accountId = Crn.safeFromString(environmentDto.getResourceCrn()).getAccountId();
        Environment environment = environmentService
            .findByResourceCrnAndAccountIdAndArchivedIsFalse(environmentDto.getResourceCrn(), accountId).
                orElseThrow(() -> new NotFoundException(
                    String.format("Could not find environment '%s' using crn '%s'", environmentDto.getName(),
                        environmentDto.getResourceCrn())));

        String userCrn = ThreadBasedUserCrnProvider.getUserCrn();
        reactorFlowManager
            .triggerLoadBalancerUpdateFlow(environmentDto, environment.getId(), environment.getName(), environment.getResourceCrn(),
                environmentLbDto.getEndpointAccessGateway(), environmentLbDto.getEndpointGatewaySubnetIds(), userCrn);
    }

    public EnvironmentLbUpdateStatusResponse getLoadBalancerUpdateStatus(EnvironmentDto environmentDto) {
        FlowLogResponse flowLogResponse = getLatestLoadBalancerUpdateFlowConfigLogs(environmentDto.getResourceCrn());
        if (flowLogResponse == null) {
            throw new BadRequestException("No LoadBalancerUpdateFlowConfig flows found on " + environmentDto.getResourceCrn());
        }

        String flowId = flowLogResponse.getFlowId();
        EnvironmentLbUpdateStatusResponse envLbUpdateStatusResponse = new EnvironmentLbUpdateStatusResponse();
        envLbUpdateStatusResponse.setEnvironmentFlowId(flowId);
        List<FlowLogResponse> flowLogResponses = flowService.getFlowLogsByFlowId(flowId);

        LOGGER.debug("Checking if status information has already been saved to database.");
        Set<LbUpdateFlowLog> lbUpdateFlowLogs = lbUpdateFlowLogService.findByParentFlowId(flowId);
        if (lbUpdateFlowLogs.isEmpty()) {
            LOGGER.debug("No database entries found. Child processes may not have started. Getting parent status instead.");
            convertParentFlowLogs(envLbUpdateStatusResponse, flowLogResponses, flowId);
        } else {
            LOGGER.debug("Found database entries for child cluster update processes.");
            convertChildFlowLogs(envLbUpdateStatusResponse, lbUpdateFlowLogs, flowId);
        }

        return envLbUpdateStatusResponse;
    }

    @VisibleForTesting
    FlowLogResponse getLatestLoadBalancerUpdateFlowConfigLogs(String environmentCrn) {
        return getLatestFlowLog(flowService.getFlowLogsByCrnAndType(environmentCrn, LoadBalancerUpdateFlowConfig.class));
    }

    private FlowLogResponse getLatestFlowLog(List<FlowLogResponse> flowLogResponses) {
        return flowLogResponses.stream()
            .max(Comparator.comparing(FlowLogResponse::getCreated))
            .orElse(null);
    }

    private void convertParentFlowLogs(EnvironmentLbUpdateStatusResponse envLbUpdateStatusResponse,
            List<FlowLogResponse> flowLogResponses, String flowId) {
        if (hasFlowFailed(flowLogResponses)) {
            LOGGER.debug("Found failed parent flow {}", flowId);
            envLbUpdateStatusResponse.markEnvironmentUpdateFailed();
        } else if (!hasActiveFlow(flowId)) {
            LOGGER.debug("Found finished parent flow [{}] with no child process status", flowId);
            envLbUpdateStatusResponse.markFinishedNoChildren();
        } else {
            LOGGER.debug("Flow {} is still running, but has no child status to report.", flowId);
            envLbUpdateStatusResponse.markNoClusterStatus();
        }
    }

    private void convertChildFlowLogs(EnvironmentLbUpdateStatusResponse envLbUpdateStatusResponse, Set<LbUpdateFlowLog> lbUpdateFlowLogs, String flowId) {
        for (LbUpdateFlowLog lbUpdateFlowLog : lbUpdateFlowLogs) {
            String childName = lbUpdateFlowLog.getChildResourceName();
            String childCrn = lbUpdateFlowLog.getChildResourceCrn();
            String childFlowId = lbUpdateFlowLog.getChildFlowId();
            if (childFlowId == null) {
                LOGGER.debug("Flow for cluster {} failed to start", childName);
                envLbUpdateStatusResponse.addChildStatus(childName, childCrn, null, LoadBalancerUpdateStatus.COULD_NOT_START, null);
            } else {
                LOGGER.debug("Fetching flow logs for child {} flow {}", childName, childFlowId);
                List<FlowLogResponse> childFlowLogResponses = ThreadBasedUserCrnProvider.doAsInternalActor(() ->
                    flowEndpoint.getFlowLogsByFlowId(childFlowId));
                updateChildFlowStatus(childFlowLogResponses, envLbUpdateStatusResponse, lbUpdateFlowLog);
            }
        }
        updateParentFlowStatus(envLbUpdateStatusResponse);
    }

    private void updateChildFlowStatus(List<FlowLogResponse> childFlowLogResponses, EnvironmentLbUpdateStatusResponse envLbUpdateStatusResponse,
            LbUpdateFlowLog lbUpdateFlowLog) {
        String childName = lbUpdateFlowLog.getChildResourceName();
        String childCrn = lbUpdateFlowLog.getChildResourceCrn();
        String childFlowId = lbUpdateFlowLog.getChildFlowId();
        FlowLogResponse flowLogResponse = getLatestFlowLog(childFlowLogResponses);
        String currentState = UNKNOWN_STATE;
        if (flowLogResponse != null) {
            currentState = flowLogResponse.getCurrentState();
        }

        if (childFlowLogResponses.isEmpty()) {
            LOGGER.debug("Did not find matching flow logs for flow id {}", childFlowId);
            envLbUpdateStatusResponse.addChildStatus(childName, childCrn, childFlowId, LoadBalancerUpdateStatus.AMBIGUOUS, currentState);
        } else {
            if (hasFlowFailed(childFlowLogResponses)) {
                LOGGER.debug("Found failed flow [{}] for resource {}, crn {}", childFlowId, childName, childCrn);
                envLbUpdateStatusResponse.addChildStatus(childName, childCrn, childFlowId, LoadBalancerUpdateStatus.FAILED, currentState);
            } else if (!hasActiveFlow(childFlowId)) {
                LOGGER.debug("Found finished flow [{}] for resource {}, crn {}", childFlowId, childName, childCrn);
                envLbUpdateStatusResponse.addChildStatus(childName, childCrn, childFlowId, LoadBalancerUpdateStatus.FINISHED, currentState);
            } else {
                LOGGER.debug("Flow {} still in progress for resource {}, crn {}", childFlowId, childName, childCrn);
                envLbUpdateStatusResponse.addChildStatus(childName, childCrn, childFlowId, LoadBalancerUpdateStatus.IN_PROGRESS, currentState);
            }
        }
    }

    private void updateParentFlowStatus(EnvironmentLbUpdateStatusResponse envLbUpdateStatusResponse) {
        List<LoadBalancerUpdateStatus> childStatuses = envLbUpdateStatusResponse.getClusterStatus().stream()
            .map(ClusterLbUpdateStatus::getStatus)
            .collect(Collectors.toList());
        if (childStatuses.stream().anyMatch(LoadBalancerUpdateStatus::isErrorCase)) {
            envLbUpdateStatusResponse.markClusterUpdateFailed();
        } else if (childStatuses.stream().anyMatch(LoadBalancerUpdateStatus.AMBIGUOUS::equals)) {
            envLbUpdateStatusResponse.markAmbiguous();
        } else if (childStatuses.stream().allMatch(LoadBalancerUpdateStatus.FINISHED::equals)) {
            envLbUpdateStatusResponse.markFinished();
        } else {
            envLbUpdateStatusResponse.markInProgress();
        }
    }

    private boolean isLoadBalancerEnabledForDatalake(String accountId, String cloudPlatform, PublicEndpointAccessGateway endpointEnum) {
        return !isLoadBalancerEntitlementRequiredForCloudProvider(cloudPlatform) ||
                entitlementService.datalakeLoadBalancerEnabled(accountId) ||
                PublicEndpointAccessGateway.ENABLED.equals(endpointEnum);
    }

    private boolean isLoadBalancerEntitlementRequiredForCloudProvider(String cloudPlatform) {
        return !(AWS.equalsIgnoreCase(cloudPlatform));
    }

    private boolean hasFlowFailed(List<FlowLogResponse> flowLogs) {
        return flowLogs.stream().map(FlowLogResponse::getCurrentState).anyMatch(LOAD_BALANCER_UPDATE_FAILED_STATE::equals);
    }

    private Boolean hasActiveFlow(String flowId) {
        return ThreadBasedUserCrnProvider.doAsInternalActor(() ->
            flowEndpoint.hasFlowRunningByFlowId(flowId).getHasActiveFlow());
    }
}
