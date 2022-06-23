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

import javax.ws.rs.NotFoundException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGenerator;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
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

    @Mock
    private FlowEndpoint flowEndpoint;

    @Mock
    private SdxClusterRepository sdxClusterRepository;

    @Mock
    private FlowCheckResponseToFlowStateConverter flowCheckResponseToFlowStatusConverter;

    @Mock
    private RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    @Mock
    private RegionAwareInternalCrnGenerator regionAwareInternalCrnGenerator;

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
        when(regionAwareInternalCrnGenerator.getInternalCrnForServiceAsString()).thenReturn("crn:cdp:freeipa:us-west-1:altus:user:__internal__actor__");
        when(regionAwareInternalCrnGeneratorFactory.iam()).thenReturn(regionAwareInternalCrnGenerator);
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
        when(regionAwareInternalCrnGenerator.getInternalCrnForServiceAsString()).thenReturn("crn:cdp:freeipa:us-west-1:altus:user:__internal__actor__");
        when(regionAwareInternalCrnGeneratorFactory.iam()).thenReturn(regionAwareInternalCrnGenerator);
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
        when(regionAwareInternalCrnGenerator.getInternalCrnForServiceAsString()).thenReturn("crn:cdp:freeipa:us-west-1:altus:user:__internal__actor__");
        when(regionAwareInternalCrnGeneratorFactory.iam()).thenReturn(regionAwareInternalCrnGenerator);
        assertEquals(FlowState.UNKNOWN, underTest.getLastKnownFlowState(cluster));
    }

    @Test
    public void testFlowCheckIfNotFoundExceptionOccured() {
        SdxCluster cluster = new SdxCluster();
        cluster.setLastCbFlowChainId(FLOW_CHAIN_ID);
        cluster.setClusterName(CLUSTER_NAME);
        when(regionAwareInternalCrnGenerator.getInternalCrnForServiceAsString()).thenReturn("crn:cdp:freeipa:us-west-1:altus:user:__internal__actor__");
        when(regionAwareInternalCrnGeneratorFactory.iam()).thenReturn(regionAwareInternalCrnGenerator);
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
        when(regionAwareInternalCrnGenerator.getInternalCrnForServiceAsString()).thenReturn("crn:cdp:freeipa:us-west-1:altus:user:__internal__actor__");
        when(regionAwareInternalCrnGeneratorFactory.iam()).thenReturn(regionAwareInternalCrnGenerator);
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
        when(regionAwareInternalCrnGenerator.getInternalCrnForServiceAsString()).thenReturn("crn:cdp:freeipa:us-west-1:altus:user:__internal__actor__");
        when(regionAwareInternalCrnGeneratorFactory.iam()).thenReturn(regionAwareInternalCrnGenerator);
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
        when(regionAwareInternalCrnGenerator.getInternalCrnForServiceAsString()).thenReturn("crn:cdp:freeipa:us-west-1:altus:user:__internal__actor__");
        when(regionAwareInternalCrnGeneratorFactory.iam()).thenReturn(regionAwareInternalCrnGenerator);
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
        when(regionAwareInternalCrnGenerator.getInternalCrnForServiceAsString()).thenReturn("crn:cdp:freeipa:us-west-1:altus:user:__internal__actor__");
        when(regionAwareInternalCrnGeneratorFactory.iam()).thenReturn(regionAwareInternalCrnGenerator);
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
        when(regionAwareInternalCrnGenerator.getInternalCrnForServiceAsString()).thenReturn("crn:cdp:freeipa:us-west-1:altus:user:__internal__actor__");
        when(regionAwareInternalCrnGeneratorFactory.iam()).thenReturn(regionAwareInternalCrnGenerator);
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
