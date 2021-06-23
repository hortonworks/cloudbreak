package com.sequenceiq.environment.environment.service;

import static com.sequenceiq.environment.environment.service.EnvironmentLoadBalancerService.UNKNOWN_STATE;
import static com.sequenceiq.environment.environment.service.LoadBalancerPollerService.LOAD_BALANCER_UPDATE_FAILED_STATE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cloudera.cdp.shaded.org.apache.commons.lang3.StringUtils;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.api.model.FlowType;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
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
import com.sequenceiq.environment.network.service.LbUpdateFlowLogService;
import com.sequenceiq.environment.network.service.LoadBalancerEntitlementService;
import com.sequenceiq.flow.api.FlowEndpoint;
import com.sequenceiq.flow.api.model.FlowCheckResponse;
import com.sequenceiq.flow.api.model.FlowLogResponse;
import com.sequenceiq.flow.service.FlowService;

@ExtendWith(SpringExtension.class)
public class EnvironmentLoadBalancerServiceTest {

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:1234:user:1";

    private static final String ENV_NAME = "environment-name";

    private static final String ENV_CRN = "crn:cdp:environments:us-west-1:accountId:environment:4c5ba74b-c35e-45e9-9f47-123456789876";

    private static final String FLOW_ID = "flowid-1";

    private static final String ENVIRONMENT_UPDATE_STATE = "ENVIRONMENT_UPDATE_STATE";

    private static final String STACK_UPDATE_STATE = "STACK_UPDATE_STATE";

    private static final String CHILD_FLOW_ID1 = "childflowid-1";

    private static final String CHILD_FLOW_ID2 = "childflowid-2";

    @Mock
    private EnvironmentService environmentService;

    @Mock
    private EnvironmentReactorFlowManager reactorFlowManager;

    @Mock
    private EntitlementService entitlementService;

    @Mock
    private LoadBalancerEntitlementService loadBalancerEntitlementService;

    @Mock
    private FlowService flowService;

    @Mock
    private FlowEndpoint flowEndpoint;

    @Mock
    private LbUpdateFlowLogService lbUpdateFlowLogService;

    @InjectMocks
    private EnvironmentLoadBalancerService underTest;

    @Test
    public void testNoEnvironmentFound() {
        EnvironmentLoadBalancerDto environmentLbDto = EnvironmentLoadBalancerDto.builder()
            .withEndpointAccessGateway(PublicEndpointAccessGateway.ENABLED)
            .build();
        EnvironmentDto environmentDto = EnvironmentDto.builder()
            .withName(ENV_NAME)
            .withResourceCrn(ENV_CRN)
            .build();
        String expectedError = String.format("Could not find environment '%s' using crn '%s'", ENV_NAME, ENV_CRN);

        when(environmentService.findByResourceCrnAndAccountIdAndArchivedIsFalse(anyString(), anyString()))
            .thenReturn(Optional.empty());
        doNothing().when(loadBalancerEntitlementService).validateNetworkForEndpointGateway(any(), any(), any());

        final NotFoundException[] exception = new NotFoundException[1];
        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> {
            exception[0] = assertThrows(NotFoundException.class, () ->
                underTest.updateLoadBalancerInEnvironmentAndStacks(environmentDto, environmentLbDto));
        });

        assertEquals(expectedError, exception[0].getMessage());
    }

    @Test
    public void testEndpointGatewayEnabled() {
        EnvironmentLoadBalancerDto environmentLbDto = EnvironmentLoadBalancerDto.builder()
            .withEndpointAccessGateway(PublicEndpointAccessGateway.ENABLED)
            .build();
        EnvironmentDto environmentDto = EnvironmentDto.builder()
            .withResourceCrn(ENV_CRN)
            .build();

        when(environmentService.findByResourceCrnAndAccountIdAndArchivedIsFalse(anyString(), anyString()))
            .thenReturn(Optional.of(new Environment()));
        doNothing().when(reactorFlowManager).triggerLoadBalancerUpdateFlow(any(), any(), any(), any(), any(), any(), anyString());
        doNothing().when(loadBalancerEntitlementService).validateNetworkForEndpointGateway(any(), any(), any());

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> {
            underTest.updateLoadBalancerInEnvironmentAndStacks(environmentDto, environmentLbDto);
        });

        verify(reactorFlowManager, times(1))
            .triggerLoadBalancerUpdateFlow(any(), any(), any(), any(), any(), any(), anyString());
    }

    @Test
    public void testDataLakeLoadBalancerEnabled() {
        EnvironmentLoadBalancerDto environmentLbDto = EnvironmentLoadBalancerDto.builder()
            .withEndpointAccessGateway(PublicEndpointAccessGateway.DISABLED)
            .build();
        EnvironmentDto environmentDto = EnvironmentDto.builder()
            .withResourceCrn(ENV_CRN)
            .build();

        when(entitlementService.datalakeLoadBalancerEnabled(anyString())).thenReturn(true);
        when(environmentService.findByResourceCrnAndAccountIdAndArchivedIsFalse(anyString(), anyString()))
            .thenReturn(Optional.of(new Environment()));
        doNothing().when(reactorFlowManager).triggerLoadBalancerUpdateFlow(any(), any(), any(), any(), any(), any(), anyString());
        doNothing().when(loadBalancerEntitlementService).validateNetworkForEndpointGateway(any(), any(), any());

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> {
            underTest.updateLoadBalancerInEnvironmentAndStacks(environmentDto, environmentLbDto);
        });

        verify(reactorFlowManager, times(1))
            .triggerLoadBalancerUpdateFlow(any(), any(), any(), any(), any(), any(), anyString());
    }

    @Test
    public void testNoEntitlements() {
        EnvironmentLoadBalancerDto environmentLbDto = EnvironmentLoadBalancerDto.builder()
            .withEndpointAccessGateway(PublicEndpointAccessGateway.DISABLED)
            .build();
        EnvironmentDto environmentDto = EnvironmentDto.builder()
            .withResourceCrn(ENV_CRN)
            .build();
        String expectedError = "Neither Endpoint Gateway nor Data Lake load balancer is enabled. Nothing to do.";

        when(entitlementService.datalakeLoadBalancerEnabled(anyString())).thenReturn(false);
        doNothing().when(loadBalancerEntitlementService).validateNetworkForEndpointGateway(any(), any(), any());

        final BadRequestException[] exception = new BadRequestException[1];
        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> {
            exception[0] = assertThrows(BadRequestException.class, () ->
                underTest.updateLoadBalancerInEnvironmentAndStacks(environmentDto, environmentLbDto));
        });

        assertEquals(expectedError, exception[0].getMessage());
    }

    @Test
    public void testFlowStartedNoChildren() {
        EnvironmentDto environmentDto = EnvironmentDto.builder()
            .withName(ENV_NAME)
            .withResourceCrn(ENV_CRN)
            .build();
        FlowLogResponse flowLogResponse = new FlowLogResponse();
        flowLogResponse.setCreated(1L);
        flowLogResponse.setFlowId(FLOW_ID);
        flowLogResponse.setCurrentState(ENVIRONMENT_UPDATE_STATE);
        List<FlowLogResponse> flowLogResponses = List.of(flowLogResponse);
        FlowCheckResponse flowCheckResponse = new FlowCheckResponse();
        flowCheckResponse.setHasActiveFlow(true);
        FlowIdentifier flowIdentifier = new FlowIdentifier(FlowType.FLOW, FLOW_ID);

        when(lbUpdateFlowLogService.findByParentFlowId(any())).thenReturn(Set.of());
        when(flowService.getFlowLogsByCrnAndType(any(), any())).thenReturn(flowLogResponses);
        when(flowService.getFlowLogsByFlowId(eq(FLOW_ID))).thenReturn(flowLogResponses);
        when(flowEndpoint.hasFlowRunningByFlowId(eq(FLOW_ID))).thenReturn(flowCheckResponse);

        EnvironmentLbUpdateStatusResponse response = underTest.getLoadBalancerUpdateStatus(environmentDto);

        assertEquals(LoadBalancerUpdateStatus.IN_PROGRESS, response.getOverallStatus());
        assertEquals(EnvironmentLbUpdateStatusResponse.NO_CLUSTER_STATUS, response.getStatusReason());
        assertEquals(flowIdentifier, response.getEnvironmentFlowId());
        assertTrue(response.getClusterStatus().isEmpty());
    }

    @Test
    public void testFlowInFailedState() {
        EnvironmentDto environmentDto = EnvironmentDto.builder()
            .withName(ENV_NAME)
            .withResourceCrn(ENV_CRN)
            .build();
        FlowLogResponse flowLogResponse = new FlowLogResponse();
        flowLogResponse.setCreated(1L);
        flowLogResponse.setFlowId(FLOW_ID);
        flowLogResponse.setCurrentState(LOAD_BALANCER_UPDATE_FAILED_STATE);
        List<FlowLogResponse> flowLogResponses = List.of(flowLogResponse);
        FlowCheckResponse flowCheckResponse = new FlowCheckResponse();
        flowCheckResponse.setHasActiveFlow(true);
        FlowIdentifier flowIdentifier = new FlowIdentifier(FlowType.FLOW, FLOW_ID);

        when(lbUpdateFlowLogService.findByParentFlowId(any())).thenReturn(Set.of());
        when(flowService.getFlowLogsByCrnAndType(any(), any())).thenReturn(flowLogResponses);
        when(flowService.getFlowLogsByFlowId(eq(FLOW_ID))).thenReturn(flowLogResponses);
        when(flowEndpoint.hasFlowRunningByFlowId(eq(FLOW_ID))).thenReturn(flowCheckResponse);

        EnvironmentLbUpdateStatusResponse response = underTest.getLoadBalancerUpdateStatus(environmentDto);

        assertEquals(LoadBalancerUpdateStatus.FAILED, response.getOverallStatus());
        assertEquals(EnvironmentLbUpdateStatusResponse.ENV_ERROR, response.getStatusReason());
        assertEquals(flowIdentifier, response.getEnvironmentFlowId());
        assertTrue(response.getClusterStatus().isEmpty());
    }

    @Test
    public void testFlowFinishedNoChildren() {
        EnvironmentDto environmentDto = EnvironmentDto.builder()
            .withName(ENV_NAME)
            .withResourceCrn(ENV_CRN)
            .build();
        FlowLogResponse flowLogResponse = new FlowLogResponse();
        flowLogResponse.setCreated(1L);
        flowLogResponse.setFlowId(FLOW_ID);
        flowLogResponse.setCurrentState(ENVIRONMENT_UPDATE_STATE);
        List<FlowLogResponse> flowLogResponses = List.of(flowLogResponse);
        FlowCheckResponse flowCheckResponse = new FlowCheckResponse();
        flowCheckResponse.setHasActiveFlow(false);
        FlowIdentifier flowIdentifier = new FlowIdentifier(FlowType.FLOW, FLOW_ID);

        when(lbUpdateFlowLogService.findByParentFlowId(any())).thenReturn(Set.of());
        when(flowService.getFlowLogsByCrnAndType(any(), any())).thenReturn(flowLogResponses);
        when(flowService.getFlowLogsByFlowId(eq(FLOW_ID))).thenReturn(flowLogResponses);
        when(flowEndpoint.hasFlowRunningByFlowId(eq(FLOW_ID))).thenReturn(flowCheckResponse);

        EnvironmentLbUpdateStatusResponse response = underTest.getLoadBalancerUpdateStatus(environmentDto);

        assertEquals(LoadBalancerUpdateStatus.FINISHED, response.getOverallStatus());
        assertEquals(EnvironmentLbUpdateStatusResponse.FINISHED_NO_CHILDREN, response.getStatusReason());
        assertEquals(flowIdentifier, response.getEnvironmentFlowId());
        assertTrue(response.getClusterStatus().isEmpty());
    }

    @Test
    public void testNoEnvFlow() {
        EnvironmentDto environmentDto = EnvironmentDto.builder()
            .withName(ENV_NAME)
            .withResourceCrn(ENV_CRN)
            .build();
        String expectedError = "No LoadBalancerUpdateFlowConfig flows found on " + ENV_CRN;

        when(lbUpdateFlowLogService.findByParentFlowId(any())).thenReturn(Set.of());
        when(flowService.getFlowLogsByCrnAndType(any(), any())).thenReturn(List.of());

        BadRequestException exception = assertThrows(BadRequestException.class, () ->
            underTest.getLoadBalancerUpdateStatus(environmentDto));

        assertEquals(expectedError, exception.getMessage());
    }

    @Test
    public void testAllChildrenInProgress() {
        EnvironmentDto environmentDto = EnvironmentDto.builder()
            .withName(ENV_NAME)
            .withResourceCrn(ENV_CRN)
            .build();
        FlowLogResponse parentFlowLogResponse = createFlowLogResponse(FLOW_ID, ENVIRONMENT_UPDATE_STATE);
        FlowLogResponse child1FlowLogResponse = createFlowLogResponse(CHILD_FLOW_ID1, STACK_UPDATE_STATE);
        FlowCheckResponse child1FlowCheckResponse = createFlowCheckResponse(true);
        FlowLogResponse child2FlowLogResponse = createFlowLogResponse(CHILD_FLOW_ID2, STACK_UPDATE_STATE);
        FlowCheckResponse child2FlowCheckResponse = createFlowCheckResponse(true);
        LbUpdateFlowLog lbUpdateFlowLog1 = createLbUpdateFlowLog(CHILD_FLOW_ID1);
        LbUpdateFlowLog lbUpdateFlowLog2 = createLbUpdateFlowLog(CHILD_FLOW_ID2);
        FlowIdentifier flowIdentifier = new FlowIdentifier(FlowType.FLOW, FLOW_ID);

        ClusterLbUpdateStatus expectedChild1Status = createClusterLbUpdateStatus(STACK_UPDATE_STATE,
            LoadBalancerUpdateStatus.IN_PROGRESS, CHILD_FLOW_ID1);
        ClusterLbUpdateStatus expectedChild2Status = createClusterLbUpdateStatus(STACK_UPDATE_STATE,
            LoadBalancerUpdateStatus.IN_PROGRESS, CHILD_FLOW_ID2);

        when(lbUpdateFlowLogService.findByParentFlowId(any())).thenReturn(Set.of(lbUpdateFlowLog1, lbUpdateFlowLog2));
        when(flowService.getFlowLogsByCrnAndType(any(), any())).thenReturn(List.of(parentFlowLogResponse));
        when(flowService.getFlowLogsByFlowId(eq(FLOW_ID))).thenReturn(List.of(parentFlowLogResponse));
        when(flowEndpoint.getFlowLogsByFlowId(eq(CHILD_FLOW_ID1))).thenReturn(List.of(child1FlowLogResponse));
        when(flowEndpoint.getFlowLogsByFlowId(eq(CHILD_FLOW_ID2))).thenReturn(List.of(child2FlowLogResponse));
        when(flowEndpoint.hasFlowRunningByFlowId(eq(CHILD_FLOW_ID1))).thenReturn(child1FlowCheckResponse);
        when(flowEndpoint.hasFlowRunningByFlowId(eq(CHILD_FLOW_ID2))).thenReturn(child2FlowCheckResponse);

        EnvironmentLbUpdateStatusResponse response = underTest.getLoadBalancerUpdateStatus(environmentDto);

        assertEquals(LoadBalancerUpdateStatus.IN_PROGRESS, response.getOverallStatus());
        assertEquals(EnvironmentLbUpdateStatusResponse.IN_PROGRESS, response.getStatusReason());
        assertEquals(flowIdentifier, response.getEnvironmentFlowId());
        assertEquals(2, response.getClusterStatus().size());
        assertTrue(response.getClusterStatus().containsAll(Set.of(expectedChild1Status, expectedChild2Status)));
    }

    @Test
    public void testOneChildInProgress() {
        EnvironmentDto environmentDto = EnvironmentDto.builder()
            .withName(ENV_NAME)
            .withResourceCrn(ENV_CRN)
            .build();
        FlowLogResponse parentFlowLogResponse = createFlowLogResponse(FLOW_ID, ENVIRONMENT_UPDATE_STATE);
        FlowLogResponse child1FlowLogResponse = createFlowLogResponse(CHILD_FLOW_ID1, STACK_UPDATE_STATE);
        FlowCheckResponse child1FlowCheckResponse = createFlowCheckResponse(true);
        FlowLogResponse child2FlowLogResponse = createFlowLogResponse(CHILD_FLOW_ID2, STACK_UPDATE_STATE);
        FlowCheckResponse child2FlowCheckResponse = createFlowCheckResponse(false);
        LbUpdateFlowLog lbUpdateFlowLog1 = createLbUpdateFlowLog(CHILD_FLOW_ID1);
        LbUpdateFlowLog lbUpdateFlowLog2 = createLbUpdateFlowLog(CHILD_FLOW_ID2);
        FlowIdentifier flowIdentifier = new FlowIdentifier(FlowType.FLOW, FLOW_ID);

        ClusterLbUpdateStatus expectedChild1Status = createClusterLbUpdateStatus(STACK_UPDATE_STATE,
            LoadBalancerUpdateStatus.IN_PROGRESS, CHILD_FLOW_ID1);
        ClusterLbUpdateStatus expectedChild2Status = createClusterLbUpdateStatus(STACK_UPDATE_STATE,
            LoadBalancerUpdateStatus.FINISHED, CHILD_FLOW_ID2);

        when(lbUpdateFlowLogService.findByParentFlowId(any())).thenReturn(Set.of(lbUpdateFlowLog1, lbUpdateFlowLog2));
        when(flowService.getFlowLogsByCrnAndType(any(), any())).thenReturn(List.of(parentFlowLogResponse));
        when(flowService.getFlowLogsByFlowId(eq(FLOW_ID))).thenReturn(List.of(parentFlowLogResponse));
        when(flowEndpoint.getFlowLogsByFlowId(eq(CHILD_FLOW_ID1))).thenReturn(List.of(child1FlowLogResponse));
        when(flowEndpoint.getFlowLogsByFlowId(eq(CHILD_FLOW_ID2))).thenReturn(List.of(child2FlowLogResponse));
        when(flowEndpoint.hasFlowRunningByFlowId(eq(CHILD_FLOW_ID1))).thenReturn(child1FlowCheckResponse);
        when(flowEndpoint.hasFlowRunningByFlowId(eq(CHILD_FLOW_ID2))).thenReturn(child2FlowCheckResponse);

        EnvironmentLbUpdateStatusResponse response = underTest.getLoadBalancerUpdateStatus(environmentDto);

        assertEquals(LoadBalancerUpdateStatus.IN_PROGRESS, response.getOverallStatus());
        assertEquals(EnvironmentLbUpdateStatusResponse.IN_PROGRESS, response.getStatusReason());
        assertEquals(flowIdentifier, response.getEnvironmentFlowId());
        assertEquals(2, response.getClusterStatus().size());
        assertTrue(response.getClusterStatus().containsAll(Set.of(expectedChild1Status, expectedChild2Status)));
    }

    @Test
    public void testOneChildFailedOthersInProgress() {
        EnvironmentDto environmentDto = EnvironmentDto.builder()
            .withName(ENV_NAME)
            .withResourceCrn(ENV_CRN)
            .build();
        FlowLogResponse parentFlowLogResponse = createFlowLogResponse(FLOW_ID, ENVIRONMENT_UPDATE_STATE);
        FlowLogResponse child1FlowLogResponse = createFlowLogResponse(CHILD_FLOW_ID1, STACK_UPDATE_STATE);
        FlowCheckResponse child1FlowCheckResponse = createFlowCheckResponse(true);
        FlowLogResponse child2FlowLogResponse = createFlowLogResponse(CHILD_FLOW_ID2, LOAD_BALANCER_UPDATE_FAILED_STATE);
        FlowCheckResponse child2FlowCheckResponse = createFlowCheckResponse(false);
        LbUpdateFlowLog lbUpdateFlowLog1 = createLbUpdateFlowLog(CHILD_FLOW_ID1);
        LbUpdateFlowLog lbUpdateFlowLog2 = createLbUpdateFlowLog(CHILD_FLOW_ID2);
        FlowIdentifier flowIdentifier = new FlowIdentifier(FlowType.FLOW, FLOW_ID);

        ClusterLbUpdateStatus expectedChild1Status = createClusterLbUpdateStatus(STACK_UPDATE_STATE,
            LoadBalancerUpdateStatus.IN_PROGRESS, CHILD_FLOW_ID1);
        ClusterLbUpdateStatus expectedChild2Status = createClusterLbUpdateStatus(LOAD_BALANCER_UPDATE_FAILED_STATE,
            LoadBalancerUpdateStatus.FAILED, CHILD_FLOW_ID2);

        when(lbUpdateFlowLogService.findByParentFlowId(any())).thenReturn(Set.of(lbUpdateFlowLog1, lbUpdateFlowLog2));
        when(flowService.getFlowLogsByCrnAndType(any(), any())).thenReturn(List.of(parentFlowLogResponse));
        when(flowService.getFlowLogsByFlowId(eq(FLOW_ID))).thenReturn(List.of(parentFlowLogResponse));
        when(flowEndpoint.getFlowLogsByFlowId(eq(CHILD_FLOW_ID1))).thenReturn(List.of(child1FlowLogResponse));
        when(flowEndpoint.getFlowLogsByFlowId(eq(CHILD_FLOW_ID2))).thenReturn(List.of(child2FlowLogResponse));
        when(flowEndpoint.hasFlowRunningByFlowId(eq(CHILD_FLOW_ID1))).thenReturn(child1FlowCheckResponse);
        when(flowEndpoint.hasFlowRunningByFlowId(eq(CHILD_FLOW_ID2))).thenReturn(child2FlowCheckResponse);

        EnvironmentLbUpdateStatusResponse response = underTest.getLoadBalancerUpdateStatus(environmentDto);

        assertEquals(LoadBalancerUpdateStatus.FAILED, response.getOverallStatus());
        assertEquals(EnvironmentLbUpdateStatusResponse.CLUSTER_ERROR, response.getStatusReason());
        assertEquals(flowIdentifier, response.getEnvironmentFlowId());
        assertEquals(2, response.getClusterStatus().size());
        assertTrue(response.getClusterStatus().containsAll(Set.of(expectedChild1Status, expectedChild2Status)));
    }

    @Test
    public void testOneChildFailedOthersFinished() {
        EnvironmentDto environmentDto = EnvironmentDto.builder()
            .withName(ENV_NAME)
            .withResourceCrn(ENV_CRN)
            .build();
        FlowLogResponse parentFlowLogResponse = createFlowLogResponse(FLOW_ID, ENVIRONMENT_UPDATE_STATE);
        FlowLogResponse child1FlowLogResponse = createFlowLogResponse(CHILD_FLOW_ID1, STACK_UPDATE_STATE);
        FlowCheckResponse child1FlowCheckResponse = createFlowCheckResponse(false);
        FlowLogResponse child2FlowLogResponse = createFlowLogResponse(CHILD_FLOW_ID2, LOAD_BALANCER_UPDATE_FAILED_STATE);
        FlowCheckResponse child2FlowCheckResponse = createFlowCheckResponse(false);
        LbUpdateFlowLog lbUpdateFlowLog1 = createLbUpdateFlowLog(CHILD_FLOW_ID1);
        LbUpdateFlowLog lbUpdateFlowLog2 = createLbUpdateFlowLog(CHILD_FLOW_ID2);
        FlowIdentifier flowIdentifier = new FlowIdentifier(FlowType.FLOW, FLOW_ID);

        ClusterLbUpdateStatus expectedChild1Status = createClusterLbUpdateStatus(STACK_UPDATE_STATE,
            LoadBalancerUpdateStatus.FINISHED, CHILD_FLOW_ID1);
        ClusterLbUpdateStatus expectedChild2Status = createClusterLbUpdateStatus(LOAD_BALANCER_UPDATE_FAILED_STATE,
            LoadBalancerUpdateStatus.FAILED, CHILD_FLOW_ID2);

        when(lbUpdateFlowLogService.findByParentFlowId(any())).thenReturn(Set.of(lbUpdateFlowLog1, lbUpdateFlowLog2));
        when(flowService.getFlowLogsByCrnAndType(any(), any())).thenReturn(List.of(parentFlowLogResponse));
        when(flowService.getFlowLogsByFlowId(eq(FLOW_ID))).thenReturn(List.of(parentFlowLogResponse));
        when(flowEndpoint.getFlowLogsByFlowId(eq(CHILD_FLOW_ID1))).thenReturn(List.of(child1FlowLogResponse));
        when(flowEndpoint.getFlowLogsByFlowId(eq(CHILD_FLOW_ID2))).thenReturn(List.of(child2FlowLogResponse));
        when(flowEndpoint.hasFlowRunningByFlowId(eq(CHILD_FLOW_ID1))).thenReturn(child1FlowCheckResponse);
        when(flowEndpoint.hasFlowRunningByFlowId(eq(CHILD_FLOW_ID2))).thenReturn(child2FlowCheckResponse);

        EnvironmentLbUpdateStatusResponse response = underTest.getLoadBalancerUpdateStatus(environmentDto);

        assertEquals(LoadBalancerUpdateStatus.FAILED, response.getOverallStatus());
        assertEquals(EnvironmentLbUpdateStatusResponse.CLUSTER_ERROR, response.getStatusReason());
        assertEquals(flowIdentifier, response.getEnvironmentFlowId());
        assertEquals(2, response.getClusterStatus().size());
        assertTrue(response.getClusterStatus().containsAll(Set.of(expectedChild1Status, expectedChild2Status)));
    }

    @Test
    public void testAllChildrenFinished() {
        EnvironmentDto environmentDto = EnvironmentDto.builder()
            .withName(ENV_NAME)
            .withResourceCrn(ENV_CRN)
            .build();
        FlowLogResponse parentFlowLogResponse = createFlowLogResponse(FLOW_ID, ENVIRONMENT_UPDATE_STATE);
        FlowLogResponse child1FlowLogResponse = createFlowLogResponse(CHILD_FLOW_ID1, STACK_UPDATE_STATE);
        FlowCheckResponse child1FlowCheckResponse = createFlowCheckResponse(false);
        FlowLogResponse child2FlowLogResponse = createFlowLogResponse(CHILD_FLOW_ID2, STACK_UPDATE_STATE);
        FlowCheckResponse child2FlowCheckResponse = createFlowCheckResponse(false);
        LbUpdateFlowLog lbUpdateFlowLog1 = createLbUpdateFlowLog(CHILD_FLOW_ID1);
        LbUpdateFlowLog lbUpdateFlowLog2 = createLbUpdateFlowLog(CHILD_FLOW_ID2);
        FlowIdentifier flowIdentifier = new FlowIdentifier(FlowType.FLOW, FLOW_ID);

        ClusterLbUpdateStatus expectedChild1Status = createClusterLbUpdateStatus(STACK_UPDATE_STATE,
            LoadBalancerUpdateStatus.FINISHED, CHILD_FLOW_ID1);
        ClusterLbUpdateStatus expectedChild2Status = createClusterLbUpdateStatus(STACK_UPDATE_STATE,
            LoadBalancerUpdateStatus.FINISHED, CHILD_FLOW_ID2);

        when(lbUpdateFlowLogService.findByParentFlowId(any())).thenReturn(Set.of(lbUpdateFlowLog1, lbUpdateFlowLog2));
        when(flowService.getFlowLogsByCrnAndType(any(), any())).thenReturn(List.of(parentFlowLogResponse));
        when(flowService.getFlowLogsByFlowId(eq(FLOW_ID))).thenReturn(List.of(parentFlowLogResponse));
        when(flowEndpoint.getFlowLogsByFlowId(eq(CHILD_FLOW_ID1))).thenReturn(List.of(child1FlowLogResponse));
        when(flowEndpoint.getFlowLogsByFlowId(eq(CHILD_FLOW_ID2))).thenReturn(List.of(child2FlowLogResponse));
        when(flowEndpoint.hasFlowRunningByFlowId(eq(CHILD_FLOW_ID1))).thenReturn(child1FlowCheckResponse);
        when(flowEndpoint.hasFlowRunningByFlowId(eq(CHILD_FLOW_ID2))).thenReturn(child2FlowCheckResponse);

        EnvironmentLbUpdateStatusResponse response = underTest.getLoadBalancerUpdateStatus(environmentDto);

        assertEquals(LoadBalancerUpdateStatus.FINISHED, response.getOverallStatus());
        assertEquals(EnvironmentLbUpdateStatusResponse.FINISHED, response.getStatusReason());
        assertEquals(flowIdentifier, response.getEnvironmentFlowId());
        assertEquals(2, response.getClusterStatus().size());
        assertTrue(response.getClusterStatus().containsAll(Set.of(expectedChild1Status, expectedChild2Status)));
    }

    @Test
    public void testChildFlowIdIsNull() {
        EnvironmentDto environmentDto = EnvironmentDto.builder()
            .withName(ENV_NAME)
            .withResourceCrn(ENV_CRN)
            .build();
        FlowLogResponse parentFlowLogResponse = createFlowLogResponse(FLOW_ID, ENVIRONMENT_UPDATE_STATE);
        LbUpdateFlowLog lbUpdateFlowLog1 = createLbUpdateFlowLog(null);
        FlowIdentifier flowIdentifier = new FlowIdentifier(FlowType.FLOW, FLOW_ID);

        ClusterLbUpdateStatus expectedChild1Status = createClusterLbUpdateStatus(null,
            LoadBalancerUpdateStatus.COULD_NOT_START, null);

        when(lbUpdateFlowLogService.findByParentFlowId(any())).thenReturn(Set.of(lbUpdateFlowLog1));
        when(flowService.getFlowLogsByCrnAndType(any(), any())).thenReturn(List.of(parentFlowLogResponse));
        when(flowService.getFlowLogsByFlowId(eq(FLOW_ID))).thenReturn(List.of(parentFlowLogResponse));

        EnvironmentLbUpdateStatusResponse response = underTest.getLoadBalancerUpdateStatus(environmentDto);

        assertEquals(LoadBalancerUpdateStatus.FAILED, response.getOverallStatus());
        assertEquals(EnvironmentLbUpdateStatusResponse.CLUSTER_ERROR, response.getStatusReason());
        assertEquals(flowIdentifier, response.getEnvironmentFlowId());
        assertEquals(1, response.getClusterStatus().size());
        assertTrue(response.getClusterStatus().contains(expectedChild1Status));
    }

    @Test
    public void testChildFlowLogsAreEmpty() {
        EnvironmentDto environmentDto = EnvironmentDto.builder()
            .withName(ENV_NAME)
            .withResourceCrn(ENV_CRN)
            .build();
        FlowLogResponse parentFlowLogResponse = createFlowLogResponse(FLOW_ID, ENVIRONMENT_UPDATE_STATE);
        LbUpdateFlowLog lbUpdateFlowLog1 = createLbUpdateFlowLog(CHILD_FLOW_ID1);
        FlowIdentifier flowIdentifier = new FlowIdentifier(FlowType.FLOW, FLOW_ID);

        ClusterLbUpdateStatus expectedChild1Status = createClusterLbUpdateStatus(UNKNOWN_STATE,
            LoadBalancerUpdateStatus.AMBIGUOUS, CHILD_FLOW_ID1);

        when(lbUpdateFlowLogService.findByParentFlowId(any())).thenReturn(Set.of(lbUpdateFlowLog1));
        when(flowService.getFlowLogsByCrnAndType(any(), any())).thenReturn(List.of(parentFlowLogResponse));
        when(flowService.getFlowLogsByFlowId(eq(FLOW_ID))).thenReturn(List.of(parentFlowLogResponse));
        when(flowEndpoint.getFlowLogsByFlowId(eq(CHILD_FLOW_ID1))).thenReturn(List.of());

        EnvironmentLbUpdateStatusResponse response = underTest.getLoadBalancerUpdateStatus(environmentDto);

        assertEquals(LoadBalancerUpdateStatus.AMBIGUOUS, response.getOverallStatus());
        assertEquals(EnvironmentLbUpdateStatusResponse.MISSING_CHILD_FLOWS, response.getStatusReason());
        assertEquals(flowIdentifier, response.getEnvironmentFlowId());
        assertEquals(1, response.getClusterStatus().size());
        assertTrue(response.getClusterStatus().contains(expectedChild1Status));
    }

    private FlowLogResponse createFlowLogResponse(String flowId, String state) {
        FlowLogResponse flowLogResponse = new FlowLogResponse();
        flowLogResponse.setCreated(1L);
        flowLogResponse.setFlowId(flowId);
        flowLogResponse.setCurrentState(state);
        return flowLogResponse;
    }

    private FlowCheckResponse createFlowCheckResponse(boolean active) {
        FlowCheckResponse flowCheckResponse = new FlowCheckResponse();
        flowCheckResponse.setHasActiveFlow(active);
        return flowCheckResponse;
    }

    private LbUpdateFlowLog createLbUpdateFlowLog(String childFlow) {
        LbUpdateFlowLog lbUpdateFlowLog = new LbUpdateFlowLog();
        lbUpdateFlowLog.setParentFlowId(FLOW_ID);
        lbUpdateFlowLog.setChildFlowId(childFlow);
        lbUpdateFlowLog.setEnvironmentCrn(ENV_CRN);
        return lbUpdateFlowLog;
    }

    private ClusterLbUpdateStatus createClusterLbUpdateStatus(String state, LoadBalancerUpdateStatus status, String flow) {
        ClusterLbUpdateStatus clusterLbUpdateStatus = new ClusterLbUpdateStatus();
        clusterLbUpdateStatus.setCurrentState(state);
        clusterLbUpdateStatus.setStatus(status);
        if (StringUtils.isNotEmpty(flow)) {
            clusterLbUpdateStatus.setFlowId(new FlowIdentifier(FlowType.FLOW, flow));
        }
        return clusterLbUpdateStatus;
    }
}
