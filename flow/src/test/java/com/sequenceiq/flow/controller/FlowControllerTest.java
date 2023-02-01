package com.sequenceiq.flow.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.sequenceiq.flow.api.model.FlowCheckResponse;
import com.sequenceiq.flow.api.model.FlowLogResponse;
import com.sequenceiq.flow.service.FlowService;

@ExtendWith(SpringExtension.class)
public class FlowControllerTest {

    private static final String FLOW_CHAIN_ID = "FLOW_CHAIN_ID";

    private static final String FLOW_ID = "FLOW_ID";

    private static final Pageable PAGEABLE = PageRequest.of(0, 50);

    @Mock
    private FlowService flowService;

    @InjectMocks
    private FlowController underTest;

    @Test
    void testGetFlowLogStatusesByFlowIdsNullInput() {
        List<FlowLogResponse> response = underTest.getFlowLogsByFlowIds(null, 50, 0);
        assertThat(response).isEmpty();
    }

    @Test
    void testGetFlowLogStatusesByFlowIds() {
        doReturn(new PageImpl<>(List.of())).when(flowService).getFlowLogsByIds(List.of(FLOW_ID), PAGEABLE);
        List<FlowLogResponse> response = underTest.getFlowLogsByFlowIds(List.of(FLOW_ID), 50, 0);
        assertThat(response).isEmpty();
    }

    @Test
    void testGetFlowLogStatusesByFlowChainIdsNullInput() {
        List<FlowCheckResponse> response = underTest.getFlowChainsStatusesByChainIds(null, 50, 0);
        assertThat(response).isEmpty();
    }

    @Test
    void testGetFlowLogStatusesByFlowChainIds() {
        doReturn(new PageImpl<>(List.of())).when(flowService).getFlowChainsByChainIds(List.of(FLOW_CHAIN_ID), PAGEABLE);
        List<FlowCheckResponse> response = underTest.getFlowChainsStatusesByChainIds(List.of(FLOW_CHAIN_ID), 50, 0);
        assertThat(response).isEmpty();
    }
}
