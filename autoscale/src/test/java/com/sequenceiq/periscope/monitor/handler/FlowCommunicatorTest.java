package com.sequenceiq.periscope.monitor.handler;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newHashMap;
import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.Map;
import java.util.stream.LongStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.client.CloudbreakInternalCrnClient;
import com.sequenceiq.cloudbreak.client.CloudbreakServiceCrnEndpoints;
import com.sequenceiq.flow.api.FlowEndpoint;

@ExtendWith(MockitoExtension.class)
class FlowCommunicatorTest {

    @Mock
    private CloudbreakInternalCrnClient internalCrnClient;

    @Mock
    private CloudbreakServiceCrnEndpoints userCrnEndpoints;

    @Mock
    private FlowEndpoint flowEndpoint;

    @Captor
    private ArgumentCaptor<List<String>> flowIdsCaptor;

    @InjectMocks
    private FlowCommunicator underTest;

    @Test
    void testGetFlowStatusFromFlowIdsWithFlowIdsLessThanPageSize() {
        Map<String, Long> activityIdByFlowId = getFlowIdToActivityIdMap(40);

        doReturn(userCrnEndpoints).when(internalCrnClient).withInternalCrn();
        doReturn(flowEndpoint).when(userCrnEndpoints).flowEndpoint();

        underTest.getFlowStatusFromFlowIds(activityIdByFlowId);

        verify(flowEndpoint, times(1)).getFlowChainsStatusesByChainIds(eq(newArrayList(activityIdByFlowId.keySet())), anyInt(), anyInt());
    }

    @Test
    void testGetFlowStatusFromFlowIdsWithFlowIdsMoreThanPageSize1() {
        Map<String, Long> activityIdByFlowId = getFlowIdToActivityIdMap(75);

        doReturn(userCrnEndpoints).when(internalCrnClient).withInternalCrn();
        doReturn(flowEndpoint).when(userCrnEndpoints).flowEndpoint();

        underTest.getFlowStatusFromFlowIds(activityIdByFlowId);

        verify(flowEndpoint, times(2)).getFlowChainsStatusesByChainIds(flowIdsCaptor.capture(), anyInt(), anyInt());

        assertThat(flowIdsCaptor.getAllValues()).hasSize(2);
    }

    @Test
    void testGetFlowStatusFromFlowIdsWhenFlowIdsIsMoreThanPageSize2() {
        Map<String, Long> activityIdByFlowId = getFlowIdToActivityIdMap(101);

        doReturn(userCrnEndpoints).when(internalCrnClient).withInternalCrn();
        doReturn(flowEndpoint).when(userCrnEndpoints).flowEndpoint();

        underTest.getFlowStatusFromFlowIds(activityIdByFlowId);

        verify(flowEndpoint, times(3)).getFlowChainsStatusesByChainIds(flowIdsCaptor.capture(), anyInt(), anyInt());

        assertThat(flowIdsCaptor.getAllValues()).hasSize(3);
    }

    private Map<String, Long> getFlowIdToActivityIdMap(int count) {
        Map<String, Long> flowIdToActivityId = newHashMap();
        LongStream.range(0, count).forEach(i -> {
            String flowId = randomUUID().toString();
            flowIdToActivityId.put(flowId, i);
        });
        return flowIdToActivityId;
    }

}