package com.sequenceiq.environment.environment.service;

import static com.sequenceiq.environment.environment.service.LoadBalancerPollerService.LOAD_BALANCER_UPDATE_FAILED_STATE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackViewV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackViewV4Responses;
import com.sequenceiq.common.api.type.PublicEndpointAccessGateway;
import com.sequenceiq.environment.environment.domain.LbUpdateFlowLog;
import com.sequenceiq.environment.environment.service.datahub.DatahubService;
import com.sequenceiq.environment.environment.service.sdx.SdxService;
import com.sequenceiq.environment.environment.service.stack.StackService;
import com.sequenceiq.environment.exception.UpdateFailedException;
import com.sequenceiq.environment.network.service.LbUpdateFlowLogService;
import com.sequenceiq.flow.api.FlowEndpoint;
import com.sequenceiq.flow.api.model.FlowCheckResponse;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.api.model.FlowLogResponse;
import com.sequenceiq.flow.api.model.FlowType;
import com.sequenceiq.sdx.api.model.SdxClusterResponse;

@ExtendWith(MockitoExtension.class)
public class LoadBalancerPollerServiceTest {

    private static final String LOAD_BALANCER_UPDATE_FINISHED_STATE = "LOAD_BALANCER_UPDATE_FINISHED_STATE";

    private static final Long ENV_ID = 1L;

    private static final String ENV_CRN = "envCrn";

    private static final String ENV_NAME = "envName";

    private static final String DL_NAME = "datalakeName";

    private static final String DL_CRN = "datalakeCrn";

    private static final String DH_NAME1 = "datahubName1";

    private static final String DH_NAME2 = "datahubName2";

    private static final String DH_CRN1 = "datahubCrn1";

    private static final String DH_CRN2 = "datahubCrn2";

    private static final String FLOW_ID = "flowid-1";

    private static final String PARENT_FLOW_ID = "parentflowid-1";

    private final DatahubService datahubService = Mockito.mock(DatahubService.class);

    private final SdxService sdxService = Mockito.mock(SdxService.class);

    private final StackService stackService = Mockito.mock(StackService.class);

    private final FlowEndpoint flowEndpoint = Mockito.mock(FlowEndpoint.class);

    private final LbUpdateFlowLogService lbUpdateFlowLogService = Mockito.mock(LbUpdateFlowLogService.class);

    private final LoadBalancerPollerService underTest =
        new LoadBalancerPollerService(datahubService, sdxService, stackService, flowEndpoint, lbUpdateFlowLogService);

    @BeforeEach
    public void before() {
        ReflectionTestUtils.setField(underTest, "sleepTime", 1);
        ReflectionTestUtils.setField(underTest, "maxTime", 1);
    }

    @Test
    public void testPollingNoStacks() {
        when(sdxService.list(ENV_NAME)).thenReturn(List.of());
        when(datahubService.list(ENV_CRN)).thenReturn(new StackViewV4Responses());

        underTest.updateStackWithLoadBalancer(ENV_ID, ENV_CRN, ENV_NAME, PublicEndpointAccessGateway.DISABLED, PARENT_FLOW_ID);

        verify(stackService, never()).updateLoadBalancer(anySet());
    }

    @Test
    public void testPollingForSingleDatalake() {
        setupDatalakeResponse();
        when(datahubService.list(ENV_CRN)).thenReturn(new StackViewV4Responses());
        when(stackService.updateLoadBalancer(anySet())).thenReturn(setupFlowIdentifiers(1));
        when(flowEndpoint.hasFlowRunningByFlowId(anyString())).thenReturn(setupFinishedFlowCheckResponse());
        when(flowEndpoint.getFlowLogsByFlowId(anyString())).thenReturn(List.of(setupSuccessFlowLogResponse(FLOW_ID + 0)));
        when(lbUpdateFlowLogService.saveAll(any())).thenReturn(Set.of());

        underTest.updateStackWithLoadBalancer(ENV_ID, ENV_CRN, ENV_NAME, PublicEndpointAccessGateway.ENABLED, PARENT_FLOW_ID);

        verify(stackService, times(1)).updateLoadBalancer(eq(Set.of(DL_NAME)));
        verify(flowEndpoint, times(1)).hasFlowRunningByFlowId(anyString());
        verify(flowEndpoint, times(1)).getFlowLogsByFlowId(anyString());
        verify(lbUpdateFlowLogService, times(1)).saveAll(any());
    }

    @Test
    public void testPollingForDatalakeAndDatahubs() {
        setupDatalakeResponse();
        setupDatahubResponse();
        when(stackService.updateLoadBalancer(eq(Set.of(DL_NAME, DH_NAME1, DH_NAME2)))).thenReturn(setupFlowIdentifiers(3));
        when(flowEndpoint.hasFlowRunningByFlowId(anyString())).thenReturn(setupFinishedFlowCheckResponse());
        when(flowEndpoint.getFlowLogsByFlowId(anyString())).thenReturn(List.of(setupSuccessFlowLogResponse(FLOW_ID)));
        when(lbUpdateFlowLogService.saveAll(any())).thenReturn(Set.of());

        underTest.updateStackWithLoadBalancer(ENV_ID, ENV_CRN, ENV_NAME, PublicEndpointAccessGateway.ENABLED, PARENT_FLOW_ID);

        verify(stackService, times(1)).updateLoadBalancer(eq(Set.of(DL_NAME, DH_NAME1, DH_NAME2)));
        verify(flowEndpoint, times(3)).hasFlowRunningByFlowId(anyString());
        verify(flowEndpoint, times(3)).getFlowLogsByFlowId(anyString());
        verify(lbUpdateFlowLogService, times(1)).saveAll(any());
    }

    @Test
    public void testPollingForDatalakeOnly() {
        setupDatalakeResponse();
        setupDatahubResponse();
        when(stackService.updateLoadBalancer(eq(Set.of(DL_NAME)))).thenReturn(setupFlowIdentifiers(1));
        when(flowEndpoint.hasFlowRunningByFlowId(anyString())).thenReturn(setupFinishedFlowCheckResponse());
        when(flowEndpoint.getFlowLogsByFlowId(anyString())).thenReturn(List.of(setupSuccessFlowLogResponse(FLOW_ID + 0)));
        when(lbUpdateFlowLogService.saveAll(any())).thenReturn(Set.of());

        underTest.updateStackWithLoadBalancer(ENV_ID, ENV_CRN, ENV_NAME, PublicEndpointAccessGateway.DISABLED, PARENT_FLOW_ID);

        verify(stackService, times(1)).updateLoadBalancer(eq(Set.of(DL_NAME)));
        verify(flowEndpoint, times(1)).hasFlowRunningByFlowId(anyString());
        verify(flowEndpoint, times(1)).getFlowLogsByFlowId(anyString());
        verify(lbUpdateFlowLogService, times(1)).saveAll(any());
    }

    @Test
    public void testPollingSingleFailure() {
        Map<String, FlowIdentifier> flowIdentifiers = setupFlowIdentifiers(3);
        Iterator<Map.Entry<String, FlowIdentifier>> iterator = flowIdentifiers.entrySet().iterator();
        Map.Entry<String, FlowIdentifier> failedCluster = iterator.next();
        FlowIdentifier failFlowId = failedCluster.getValue();
        String successFlowId1 = iterator.next().getValue().getPollableId();
        String successFlowId2 = iterator.next().getValue().getPollableId();
        String expectedError = "Data Lake or Data Hub update flows failed: " + Map.of(failedCluster.getKey(), failedCluster.getValue());

        setupDatalakeResponse();
        setupDatahubResponse();
        when(stackService.updateLoadBalancer(eq(Set.of(DL_NAME, DH_NAME1, DH_NAME2)))).thenReturn(flowIdentifiers);
        when(flowEndpoint.hasFlowRunningByFlowId(anyString())).thenReturn(setupFinishedFlowCheckResponse());
        when(flowEndpoint.getFlowLogsByFlowId(failFlowId.getPollableId())).thenReturn(List.of(setupFailFlowLogResponse(failFlowId.getPollableId())));
        when(flowEndpoint.getFlowLogsByFlowId(successFlowId1)).thenReturn(List.of(setupSuccessFlowLogResponse(successFlowId1)));
        when(flowEndpoint.getFlowLogsByFlowId(successFlowId2)).thenReturn(List.of(setupSuccessFlowLogResponse(successFlowId2)));

        UpdateFailedException exception =
            assertThrows(UpdateFailedException.class, () ->
                underTest.updateStackWithLoadBalancer(ENV_ID, ENV_CRN, ENV_NAME, PublicEndpointAccessGateway.ENABLED, PARENT_FLOW_ID));

        verify(flowEndpoint, times(3)).hasFlowRunningByFlowId(anyString());
        verify(flowEndpoint, times(3)).getFlowLogsByFlowId(anyString());
        assertEquals(expectedError, exception.getMessage());
    }

    @Test
    public void testPollingTimeout() {
        ReflectionTestUtils.setField(underTest, "maxTime", 5);
        setupDatalakeResponse();
        setupDatahubResponse();
        when(stackService.updateLoadBalancer(eq(Set.of(DL_NAME, DH_NAME1, DH_NAME2)))).thenReturn(setupFlowIdentifiers(3));
        when(flowEndpoint.hasFlowRunningByFlowId(anyString())).thenReturn(setupActiveFlowCheckResponse());
        String expectedError = "Stack update poller reached timeout.";

        UpdateFailedException exception =
            assertThrows(UpdateFailedException.class, () ->
                underTest.updateStackWithLoadBalancer(ENV_ID, ENV_CRN, ENV_NAME, PublicEndpointAccessGateway.ENABLED, PARENT_FLOW_ID));

        verify(flowEndpoint, times(18)).hasFlowRunningByFlowId(anyString());
        assertEquals(expectedError, exception.getMessage());
    }

    @Test
    public void testSaveFlowLogs() {
        setupDatalakeResponse();
        ArgumentCaptor<List<LbUpdateFlowLog>> flowLogArgumentCaptor = ArgumentCaptor.forClass(List.class);
        Map<String, FlowIdentifier> flowIdentifiers = Map.of(
            DL_NAME, new FlowIdentifier(FlowType.FLOW, FLOW_ID)
        );

        when(datahubService.list(ENV_CRN)).thenReturn(new StackViewV4Responses());
        when(stackService.updateLoadBalancer(anySet())).thenReturn(flowIdentifiers);
        when(flowEndpoint.hasFlowRunningByFlowId(anyString())).thenReturn(setupFinishedFlowCheckResponse());
        when(flowEndpoint.getFlowLogsByFlowId(anyString())).thenReturn(List.of(setupSuccessFlowLogResponse(FLOW_ID)));
        when(lbUpdateFlowLogService.saveAll(flowLogArgumentCaptor.capture())).thenReturn(Set.of());

        underTest.updateStackWithLoadBalancer(ENV_ID, ENV_CRN, ENV_NAME, PublicEndpointAccessGateway.ENABLED, PARENT_FLOW_ID);

        verify(lbUpdateFlowLogService, times(1)).saveAll(any());
        List<LbUpdateFlowLog> flowLogs = flowLogArgumentCaptor.getValue();
        assertEquals(1, flowLogs.size());
        assertEquals(FLOW_ID, flowLogs.get(0).getChildFlowId());
        assertEquals(DL_CRN, flowLogs.get(0).getChildResourceCrn());
        assertEquals(DL_NAME, flowLogs.get(0).getChildResourceName());
        assertEquals(ENV_CRN, flowLogs.get(0).getEnvironmentCrn());
        assertEquals(PARENT_FLOW_ID, flowLogs.get(0).getParentFlowId());
    }

    @Test
    public void testFlowFailedToStart() {
        setupDatalakeResponse();
        Map<String, FlowIdentifier> flowIdentifiers = new HashMap<>();
        flowIdentifiers.put(DL_NAME, null);

        when(datahubService.list(ENV_CRN)).thenReturn(new StackViewV4Responses());
        when(stackService.updateLoadBalancer(anySet())).thenReturn(flowIdentifiers);
        when(flowEndpoint.hasFlowRunningByFlowId(anyString())).thenReturn(setupFinishedFlowCheckResponse());
        when(flowEndpoint.getFlowLogsByFlowId(anyString())).thenReturn(List.of(setupSuccessFlowLogResponse(FLOW_ID + 0)));
        when(lbUpdateFlowLogService.saveAll(any())).thenReturn(Set.of());

        assertThrows(UpdateFailedException.class, () ->
            underTest.updateStackWithLoadBalancer(ENV_ID, ENV_CRN, ENV_NAME, PublicEndpointAccessGateway.ENABLED, PARENT_FLOW_ID));
    }

    private void setupDatalakeResponse() {
        SdxClusterResponse sdxClusterResponse = new SdxClusterResponse();
        sdxClusterResponse.setName(DL_NAME);
        sdxClusterResponse.setCrn(DL_CRN);
        when(sdxService.list(ENV_NAME)).thenReturn(List.of(sdxClusterResponse));
    }

    private void setupDatahubResponse() {
        StackViewV4Response response1 = new StackViewV4Response();
        response1.setName(DH_NAME1);
        response1.setCrn(DH_CRN1);
        StackViewV4Response response2 = new StackViewV4Response();
        response2.setName(DH_NAME2);
        response2.setCrn(DH_CRN2);
        when(datahubService.list(ENV_CRN)).thenReturn(new StackViewV4Responses(Set.of(response1, response2)));
    }

    private Map<String, FlowIdentifier> setupFlowIdentifiers(int count) {
        Map<String, FlowIdentifier> flowIdentifiers = new HashMap<>();
        for (int i = 0; i < count; i++) {
            flowIdentifiers.put("resourceName" + i, new FlowIdentifier(FlowType.FLOW, FLOW_ID + i));
        }
        return flowIdentifiers;
    }

    private FlowLogResponse setupSuccessFlowLogResponse(String flowId) {
        FlowLogResponse flowLogResponse = new FlowLogResponse();
        flowLogResponse.setCurrentState(LOAD_BALANCER_UPDATE_FINISHED_STATE);
        flowLogResponse.setFlowId(flowId);
        return flowLogResponse;
    }

    private FlowLogResponse setupFailFlowLogResponse(String flowId) {
        FlowLogResponse flowLogResponse = new FlowLogResponse();
        flowLogResponse.setCurrentState(LOAD_BALANCER_UPDATE_FAILED_STATE);
        flowLogResponse.setFlowId(flowId);
        return flowLogResponse;
    }

    private FlowCheckResponse setupFinishedFlowCheckResponse() {
        FlowCheckResponse flowCheckResponse = new FlowCheckResponse();
        flowCheckResponse.setHasActiveFlow(false);
        return flowCheckResponse;
    }

    private FlowCheckResponse setupActiveFlowCheckResponse() {
        FlowCheckResponse flowCheckResponse = new FlowCheckResponse();
        flowCheckResponse.setHasActiveFlow(true);
        return flowCheckResponse;
    }
}
