package com.sequenceiq.datalake.service.sdx.flowcheck;

import static com.sequenceiq.datalake.service.sdx.flowcheck.FlowState.FAILED;
import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.repository.SdxClusterRepository;
import com.sequenceiq.flow.api.FlowEndpoint;
import com.sequenceiq.flow.api.model.FlowCheckResponse;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.api.model.FlowLogResponse;
import com.sequenceiq.flow.api.model.FlowType;
import com.sequenceiq.flow.api.model.StateStatus;

@ExtendWith(MockitoExtension.class)
public class CloudbreakFlowServiceTest {

    private static final String FLOW_ID = "flowId";

    private static final String FLOW_CHAIN_ID = "flowChainId";

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:cloudera:user:bob@cloudera.com";

    private static final String CLUSTER_NAME = "dummyCluster";

    private static final String RESOURCE_CRN = "resourceCrn";

    @Mock
    private FlowEndpoint flowEndpoint;

    @Mock
    private SdxClusterRepository sdxClusterRepository;

    @Mock
    private FlowCheckResponseToFlowStateConverter flowCheckResponseToFlowStatusConverter;

    @InjectMocks
    private CloudbreakFlowService underTest;

    @Captor
    private ArgumentCaptor<SdxCluster> clusterCaptor;

    @Test
    public void testActiveFlowCheck() {
        when(flowCheckResponseToFlowStatusConverter.convert(any())).thenCallRealMethod();
        SdxCluster cluster = new SdxCluster();
        cluster.setLastCbFlowChainId(FLOW_CHAIN_ID);
        cluster.setClusterName(CLUSTER_NAME);

        when(flowEndpoint.hasFlowRunningByChainId(eq(FLOW_CHAIN_ID))).thenReturn(createFlowCheckResponse(TRUE));
        assertEquals(FlowState.RUNNING, underTest.getLastKnownFlowState(cluster));

        when(flowEndpoint.hasFlowRunningByChainId(eq(FLOW_CHAIN_ID))).thenReturn(createFlowCheckResponse(TRUE));
        assertEquals(FlowState.RUNNING, underTest.getLastKnownFlowState(cluster));

        verify(flowEndpoint, times(2)).hasFlowRunningByChainId(eq(FLOW_CHAIN_ID));
    }

    @Test
    public void testNoActiveFlowCheck() {
        when(flowCheckResponseToFlowStatusConverter.convert(any())).thenCallRealMethod();
        SdxCluster cluster = new SdxCluster();
        cluster.setLastCbFlowChainId(FLOW_CHAIN_ID);
        cluster.setClusterName(CLUSTER_NAME);

        when(flowEndpoint.hasFlowRunningByChainId(eq(FLOW_CHAIN_ID))).thenReturn(createFlowCheckResponse(FALSE, TRUE));
        assertEquals(FAILED, underTest.getLastKnownFlowState(cluster));

        cluster.setLastCbFlowChainId(null);
        assertEquals(FlowState.UNKNOWN, underTest.getLastKnownFlowState(cluster));

        verify(flowEndpoint, atLeastOnce()).hasFlowRunningByChainId(eq(FLOW_CHAIN_ID));
    }

    @Test
    public void testFlowCheckIfExceptionOccured() {
        SdxCluster cluster = new SdxCluster();
        cluster.setLastCbFlowChainId(FLOW_CHAIN_ID);
        cluster.setClusterName(CLUSTER_NAME);

        when(flowEndpoint.hasFlowRunningByChainId(eq(FLOW_CHAIN_ID))).thenThrow(new RuntimeException("something"));
        assertEquals(FlowState.UNKNOWN, underTest.getLastKnownFlowState(cluster));
    }

    @Test
    public void testFlowCheckIfNotFoundExceptionOccured() {
        SdxCluster cluster = new SdxCluster();
        cluster.setLastCbFlowChainId(FLOW_CHAIN_ID);
        cluster.setClusterName(CLUSTER_NAME);
        when(flowEndpoint.hasFlowRunningByChainId(eq(FLOW_CHAIN_ID))).thenThrow(new NotFoundException("something"));

        assertEquals(FlowState.UNKNOWN, underTest.getLastKnownFlowState(cluster));
    }

    @Test
    public void testFlowCheckBasedOnFlowId() {
        when(flowCheckResponseToFlowStatusConverter.convert(any())).thenCallRealMethod();
        SdxCluster cluster = new SdxCluster();
        cluster.setLastCbFlowId(FLOW_ID);
        cluster.setClusterName(CLUSTER_NAME);

        FlowCheckResponse flowCheckResponse = new FlowCheckResponse();
        flowCheckResponse.setHasActiveFlow(TRUE);

        when(flowEndpoint.hasFlowRunningByFlowId(anyString()))
                .thenReturn(flowCheckResponse);
        assertEquals(FlowState.RUNNING, underTest.getLastKnownFlowState(cluster));
        verify(flowEndpoint).hasFlowRunningByFlowId(FLOW_ID);
    }

    @Test
    public void testSaveFlowIdWhenNoFlowIdentifierIsPresent() {
        SdxCluster cluster = new SdxCluster();
        cluster.setLastCbFlowChainId(FLOW_CHAIN_ID);
        cluster.setClusterName(CLUSTER_NAME);

        FlowLogResponse response = new FlowLogResponse();
        response.setStateStatus(StateStatus.SUCCESSFUL);
        response.setFinalized(true);
        response.setFlowId(FLOW_ID);
        when(flowEndpoint.getLastFlowByResourceName(any(), anyString())).thenReturn(response);
        when(sdxClusterRepository.save(any())).thenReturn(cluster);
        underTest.saveLastCloudbreakFlowChainId(cluster, null);

        verify(flowEndpoint).getLastFlowByResourceName(any(), anyString());

        verify(sdxClusterRepository).save(clusterCaptor.capture());
        assertEquals(FLOW_ID, clusterCaptor.getValue().getLastCbFlowId());
        assertNull(clusterCaptor.getValue().getLastCbFlowChainId());
    }

    @Test
    public void testSaveFlowChainIdWhenNoFlowIdentifierIsPresent() {
        SdxCluster cluster = new SdxCluster();
        cluster.setLastCbFlowChainId(FLOW_CHAIN_ID);
        cluster.setClusterName(CLUSTER_NAME);

        FlowLogResponse response = new FlowLogResponse();
        response.setStateStatus(StateStatus.SUCCESSFUL);
        response.setFinalized(true);
        response.setFlowChainId(FLOW_CHAIN_ID);
        when(flowEndpoint.getLastFlowByResourceName(any(), anyString()))
                .thenReturn(response);
        when(sdxClusterRepository.save(any())).thenReturn(cluster);
        underTest.saveLastCloudbreakFlowChainId(cluster, null);

        verify(flowEndpoint).getLastFlowByResourceName(any(), anyString());

        verify(sdxClusterRepository).save(clusterCaptor.capture());
        assertEquals(FLOW_CHAIN_ID, clusterCaptor.getValue().getLastCbFlowChainId());
        assertNull(clusterCaptor.getValue().getLastCbFlowId());
    }

    @Test
    public void testSaveFlowChainIdWhenNoFlowIdentifierIsPresentAndBothFlowIdAndFlowChainIdIsPresentInFlowLog() {
        SdxCluster cluster = new SdxCluster();
        cluster.setLastCbFlowChainId(FLOW_CHAIN_ID);
        cluster.setClusterName(CLUSTER_NAME);

        FlowLogResponse response = new FlowLogResponse();
        response.setStateStatus(StateStatus.SUCCESSFUL);
        response.setFinalized(true);
        response.setFlowId(FLOW_ID);
        response.setFlowChainId(FLOW_CHAIN_ID);
        when(flowEndpoint.getLastFlowByResourceName(any(), anyString()))
                .thenReturn(response);
        when(sdxClusterRepository.save(any())).thenReturn(cluster);
        underTest.saveLastCloudbreakFlowChainId(cluster, null);

        verify(flowEndpoint).getLastFlowByResourceName(any(), anyString());

        verify(sdxClusterRepository).save(clusterCaptor.capture());
        assertEquals(FLOW_CHAIN_ID, clusterCaptor.getValue().getLastCbFlowChainId());
        assertNull(clusterCaptor.getValue().getLastCbFlowId());
    }

    @Test
    public void testSaveFlowInfoResetWhenThereIsNoFlow() {
        SdxCluster cluster = new SdxCluster();
        cluster.setLastCbFlowChainId(FLOW_CHAIN_ID);
        cluster.setClusterName(CLUSTER_NAME);

        when(flowEndpoint.getLastFlowByResourceName(any(), anyString())).thenThrow(new NotFoundException("something"));
        underTest.saveLastCloudbreakFlowChainId(cluster, null);

        verify(flowEndpoint).getLastFlowByResourceName(any(), anyString());
        verify(sdxClusterRepository).save(clusterCaptor.capture());
        assertNull(clusterCaptor.getValue().getLastCbFlowId());
        assertNull(clusterCaptor.getValue().getLastCbFlowChainId());
    }

    @Test
    public void testSaveFlowIdWhenFlowTypeIsFlow() {
        SdxCluster cluster = new SdxCluster();
        cluster.setLastCbFlowChainId(FLOW_CHAIN_ID);
        cluster.setClusterName(CLUSTER_NAME);

        FlowIdentifier flowIdentifier = new FlowIdentifier(FlowType.FLOW, FLOW_ID);

        underTest.saveLastCloudbreakFlowChainId(cluster, flowIdentifier);

        verify(sdxClusterRepository).save(clusterCaptor.capture());
        assertEquals(FLOW_ID, clusterCaptor.getValue().getLastCbFlowId());
        assertNull(clusterCaptor.getValue().getLastCbFlowChainId());
    }

    @Test
    public void testSaveFlowChainIdWhenFlowTypeIsFlowChain() {
        SdxCluster cluster = new SdxCluster();
        cluster.setLastCbFlowChainId(FLOW_CHAIN_ID);
        cluster.setClusterName(CLUSTER_NAME);

        FlowIdentifier flowIdentifier = new FlowIdentifier(FlowType.FLOW_CHAIN, FLOW_CHAIN_ID);

        underTest.saveLastCloudbreakFlowChainId(cluster, flowIdentifier);

        verify(sdxClusterRepository).save(clusterCaptor.capture());
        assertEquals(FLOW_CHAIN_ID, clusterCaptor.getValue().getLastCbFlowChainId());
        assertNull(clusterCaptor.getValue().getLastCbFlowId());
    }

    @Test
    public void testResetCbFlowInfoWhenNotTriggeredFlow() {
        SdxCluster cluster = new SdxCluster();
        cluster.setLastCbFlowChainId(FLOW_CHAIN_ID);
        cluster.setClusterName(CLUSTER_NAME);

        FlowIdentifier flowIdentifier = FlowIdentifier.notTriggered();

        underTest.saveLastCloudbreakFlowChainId(cluster, flowIdentifier);

        verify(sdxClusterRepository).save(clusterCaptor.capture());
        assertNull(clusterCaptor.getValue().getLastCbFlowId());
        assertNull(clusterCaptor.getValue().getLastCbFlowChainId());
    }

    @Test
    public void testGetLastKnownFlowCheckResponseWithoutException() {
        // Test using a flow chain ID
        SdxCluster cluster = new SdxCluster();
        cluster.setLastCbFlowChainId(FLOW_CHAIN_ID);
        cluster.setClusterName(CLUSTER_NAME);

        FlowCheckResponse flowCheckResponse = new FlowCheckResponse();

        when(flowEndpoint.hasFlowRunningByChainId(eq(FLOW_CHAIN_ID))).thenReturn(flowCheckResponse);

        // All field values are null if there's no value set.
        FlowCheckResponse lastKnownFlowCheckResponse = underTest.getLastKnownFlowCheckResponse(cluster);
        assertEquals(null, lastKnownFlowCheckResponse.getCurrentState());
        assertEquals(null, lastKnownFlowCheckResponse.getNextEvent());
        assertEquals(null, lastKnownFlowCheckResponse.getFlowType());

        // Set up values and rerun.
        flowCheckResponse.setCurrentState("TEST_STATE");
        flowCheckResponse.setNextEvent("TEST_EVENT");
        flowCheckResponse.setFlowType("TEST_TYPE");
        when(flowEndpoint.hasFlowRunningByChainId(eq(FLOW_CHAIN_ID))).thenReturn(flowCheckResponse);
        lastKnownFlowCheckResponse = underTest.getLastKnownFlowCheckResponse(cluster);
        assertEquals("TEST_STATE", lastKnownFlowCheckResponse.getCurrentState());
        assertEquals("TEST_EVENT", lastKnownFlowCheckResponse.getNextEvent());
        assertEquals("TEST_TYPE", lastKnownFlowCheckResponse.getFlowType());

        // Values can be updated.
        flowCheckResponse.setCurrentState("TEST_STATE1");
        flowCheckResponse.setNextEvent("TEST_EVENT1");
        flowCheckResponse.setFlowType("TEST_TYPE1");
        when(flowEndpoint.hasFlowRunningByChainId(eq(FLOW_CHAIN_ID))).thenReturn(flowCheckResponse);
        lastKnownFlowCheckResponse = underTest.getLastKnownFlowCheckResponse(cluster);
        assertEquals("TEST_STATE1", lastKnownFlowCheckResponse.getCurrentState());
        assertEquals("TEST_EVENT1", lastKnownFlowCheckResponse.getNextEvent());
        assertEquals("TEST_TYPE1", lastKnownFlowCheckResponse.getFlowType());

        verify(flowEndpoint, times(3)).hasFlowRunningByChainId(eq(FLOW_CHAIN_ID));

        // Test using a flow ID
        SdxCluster clusterWithFlowID = new SdxCluster();
        clusterWithFlowID.setLastCbFlowChainId(FLOW_ID);
        clusterWithFlowID.setClusterName(CLUSTER_NAME);

        FlowCheckResponse flowCheckResponseWithFlowID = new FlowCheckResponse();

        when(flowEndpoint.hasFlowRunningByChainId(eq(FLOW_ID))).thenReturn(flowCheckResponseWithFlowID);

        flowCheckResponseWithFlowID.setCurrentState("TEST_WITH_FLOW_ID");
        flowCheckResponseWithFlowID.setNextEvent("TEST_WITH_FLOW_ID");
        flowCheckResponseWithFlowID.setFlowType("TEST_WITH_FLOW_ID");
        FlowCheckResponse lastKnownFlowCheckResponseWithFlowID = underTest.getLastKnownFlowCheckResponse(clusterWithFlowID);
        assertEquals("TEST_WITH_FLOW_ID", lastKnownFlowCheckResponseWithFlowID.getCurrentState());
        assertEquals("TEST_WITH_FLOW_ID", lastKnownFlowCheckResponseWithFlowID.getNextEvent());
        assertEquals("TEST_WITH_FLOW_ID", lastKnownFlowCheckResponseWithFlowID.getFlowType());

        // Test without any flow ID or flow chain ID
        SdxCluster clusterWithoutID = new SdxCluster();
        clusterWithoutID.setClusterName(CLUSTER_NAME);

        FlowCheckResponse lastKnownFlowCheckResponseWithoutID = underTest.getLastKnownFlowCheckResponse(clusterWithoutID);
        assertEquals(null, lastKnownFlowCheckResponseWithoutID);
    }

    @Test
    void testGetFlowIdShouldReturnNullIfLastFlowNotFound() {
        when(flowEndpoint.getLastFlowByResourceCrn(eq(RESOURCE_CRN))).thenThrow(new WebApplicationException("error", Response.Status.NOT_FOUND));
        FlowLogResponse lastFlow = underTest.getLastFlowId(RESOURCE_CRN);
        assertNull(lastFlow);
    }

    private FlowCheckResponse createFlowCheckResponse(Boolean hasActiveFlow) {
        return createFlowCheckResponse(hasActiveFlow, null);
    }

    private FlowCheckResponse createFlowCheckResponse(Boolean hasActiveFlow, Boolean failed) {
        FlowCheckResponse flowCheckResponse = new FlowCheckResponse();
        flowCheckResponse.setFlowChainId(FLOW_CHAIN_ID);
        flowCheckResponse.setHasActiveFlow(hasActiveFlow);
        flowCheckResponse.setLatestFlowFinalizedAndFailed(failed);
        return flowCheckResponse;
    }

}
