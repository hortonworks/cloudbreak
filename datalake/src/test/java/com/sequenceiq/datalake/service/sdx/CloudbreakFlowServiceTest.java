package com.sequenceiq.datalake.service.sdx;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import org.assertj.core.util.Lists;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.exception.NotFoundException;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.repository.SdxClusterRepository;
import com.sequenceiq.flow.api.FlowEndpoint;
import com.sequenceiq.flow.api.model.FlowLogResponse;
import com.sequenceiq.flow.api.model.StateStatus;

@ExtendWith(MockitoExtension.class)
public class CloudbreakFlowServiceTest {

    private static final String FLOW_CHAIN_ID = "flowChainId";

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:cloudera:user:bob@cloudera.com";

    private static final String CLUSTER_NAME = "dummyCluster";

    @Mock
    private FlowEndpoint flowEndpoint;

    @Mock
    private SdxClusterRepository sdxClusterRepository;

    @InjectMocks
    private CloudbreakFlowService underTest;

    @Test
    public void testActiveFlowCheck() {
        SdxCluster cluster = new SdxCluster();
        cluster.setLastCbFlowChainId(FLOW_CHAIN_ID);
        cluster.setInitiatorUserCrn(USER_CRN);
        cluster.setClusterName(CLUSTER_NAME);

        when(flowEndpoint.getFlowLogsByResourceNameAndChainId(anyString(), eq(FLOW_CHAIN_ID)))
                .thenReturn(Lists.newArrayList(createFlowLogResponse(StateStatus.PENDING, true, null)));
        assertTrue(underTest.isLastKnownFlowRunning(cluster));

        when(flowEndpoint.getFlowLogsByResourceNameAndChainId(anyString(), eq(FLOW_CHAIN_ID)))
                .thenReturn(Lists.newArrayList(createFlowLogResponse(StateStatus.SUCCESSFUL, false, null)));
        assertTrue(underTest.isLastKnownFlowRunning(cluster));

        verify(flowEndpoint, times(2)).getFlowLogsByResourceNameAndChainId(eq(CLUSTER_NAME), eq(FLOW_CHAIN_ID));
    }

    @Test
    public void testNoActiveFlowCheck() {
        SdxCluster cluster = new SdxCluster();
        cluster.setLastCbFlowChainId(FLOW_CHAIN_ID);
        cluster.setInitiatorUserCrn(USER_CRN);
        cluster.setClusterName(CLUSTER_NAME);

        when(flowEndpoint.getFlowLogsByResourceNameAndChainId(anyString(), eq(FLOW_CHAIN_ID)))
                .thenReturn(Lists.newArrayList(createFlowLogResponse(StateStatus.SUCCESSFUL, true, null)));
        assertFalse(underTest.isLastKnownFlowRunning(cluster));

        cluster.setLastCbFlowChainId(null);
        assertFalse(underTest.isLastKnownFlowRunning(cluster));

        verify(flowEndpoint, atLeastOnce()).getFlowLogsByResourceNameAndChainId(eq(CLUSTER_NAME), eq(FLOW_CHAIN_ID));
    }

    @Test
    public void testFlowCheckFalseResult() {
        SdxCluster cluster = new SdxCluster();
        cluster.setLastCbFlowChainId(FLOW_CHAIN_ID);
        cluster.setInitiatorUserCrn(USER_CRN);
        cluster.setClusterName(CLUSTER_NAME);

        when(flowEndpoint.getFlowLogsByResourceNameAndChainId(anyString(), eq(FLOW_CHAIN_ID)))
                .thenReturn(Lists.newArrayList(createFlowLogResponse(StateStatus.SUCCESSFUL, true, null)))
                .thenReturn(Lists.newArrayList(createFlowLogResponse(StateStatus.PENDING, true, null)));
        assertTrue(underTest.isLastKnownFlowRunning(cluster));

        cluster.setLastCbFlowChainId(null);
        assertFalse(underTest.isLastKnownFlowRunning(cluster));

        verify(flowEndpoint, atLeast(2)).getFlowLogsByResourceNameAndChainId(eq(CLUSTER_NAME), eq(FLOW_CHAIN_ID));
    }

    @Test
    public void testFlowCheckIfExceptionOccured() {
        SdxCluster cluster = new SdxCluster();
        cluster.setLastCbFlowChainId(FLOW_CHAIN_ID);
        cluster.setInitiatorUserCrn(USER_CRN);
        cluster.setClusterName(CLUSTER_NAME);

        when(flowEndpoint.getFlowLogsByResourceNameAndChainId(anyString(), eq(FLOW_CHAIN_ID))).thenThrow(new RuntimeException("something"));

        assertFalse(underTest.isLastKnownFlowRunning(cluster));
    }

    @Test
    public void testSaveFlowChainId() {
        SdxCluster cluster = new SdxCluster();
        cluster.setLastCbFlowChainId(FLOW_CHAIN_ID);
        cluster.setInitiatorUserCrn(USER_CRN);
        cluster.setClusterName(CLUSTER_NAME);

        when(flowEndpoint.getLastFlowByResourceName(anyString()))
                .thenReturn(createFlowLogResponse(StateStatus.SUCCESSFUL, true, null));
        when(sdxClusterRepository.save(any())).thenReturn(cluster);

        underTest.getAndSaveLastCloudbreakFlowChainId(cluster);

        verify(flowEndpoint).getLastFlowByResourceName(anyString());

        ArgumentCaptor<SdxCluster> sdxCaptor = ArgumentCaptor.forClass(SdxCluster.class);
        verify(sdxClusterRepository).save(sdxCaptor.capture());
        assertEquals(FLOW_CHAIN_ID, sdxCaptor.getValue().getLastCbFlowChainId());
    }

    @Test
    public void testSaveFlowChainIdWhenThereIsNoFlow() {
        SdxCluster cluster = new SdxCluster();
        cluster.setLastCbFlowChainId(FLOW_CHAIN_ID);
        cluster.setInitiatorUserCrn(USER_CRN);
        cluster.setClusterName(CLUSTER_NAME);

        when(flowEndpoint.getLastFlowByResourceName(anyString())).thenThrow(new NotFoundException("something"));

        Assertions.assertThrows(NotFoundException.class,
                () -> underTest.getAndSaveLastCloudbreakFlowChainId(cluster));

        verify(flowEndpoint).getLastFlowByResourceName(anyString());
        verifyZeroInteractions(sdxClusterRepository);
    }

    private FlowLogResponse createFlowLogResponse(StateStatus stateStatus, boolean finalized, String nextEvent) {
        FlowLogResponse response = new FlowLogResponse();
        response.setStateStatus(stateStatus);
        response.setFinalized(finalized);
        response.setNextEvent(nextEvent);
        response.setFlowChainId(FLOW_CHAIN_ID);
        return response;
    }
}
