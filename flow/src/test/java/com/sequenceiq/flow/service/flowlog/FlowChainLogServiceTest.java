package com.sequenceiq.flow.service.flowlog;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;

import java.util.List;

import org.assertj.core.util.Lists;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.sequenceiq.flow.domain.FlowChainLog;
import com.sequenceiq.flow.repository.FlowChainLogRepository;

@RunWith(MockitoJUnitRunner.class)
public class FlowChainLogServiceTest {

    @InjectMocks
    private FlowChainLogService underTest;

    @Mock
    private FlowChainLogRepository flowLogRepository;

    @Test
    public void testGetRelatedFlowChainIds() {
        String flowChainId = "flowChainId";
        String parentFlowChainId = "anotherFlowChainId";
        when(flowLogRepository.findByParentFlowChainIdOrderByCreatedDesc(eq(flowChainId))).thenReturn(Lists.newArrayList(create("anotherFlowChainId")));
        when(flowLogRepository.findByParentFlowChainIdOrderByCreatedDesc(eq(parentFlowChainId))).thenReturn(Lists.newArrayList());

        List<String> flowChainIds = underTest.collectRelatedFlowChainIds(Lists.newArrayList(), flowChainId);

        assertEquals(2, flowChainIds.size());
        assertTrue(flowChainIds.contains(flowChainId));
        assertTrue(flowChainIds.contains(parentFlowChainId));

        verify(flowLogRepository, times(2)).findByParentFlowChainIdOrderByCreatedDesc(any());
    }

    private FlowChainLog create(String flowChanId) {
        FlowChainLog flowChainLog = new FlowChainLog();
        flowChainLog.setFlowChainId(flowChanId);
        flowChainLog.setParentFlowChainId(flowChanId + "parent");
        return flowChainLog;
    }
}