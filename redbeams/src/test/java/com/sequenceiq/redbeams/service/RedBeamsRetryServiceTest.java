package com.sequenceiq.redbeams.service;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.api.model.FlowType;
import com.sequenceiq.flow.api.model.RetryableFlowResponse;
import com.sequenceiq.flow.domain.RetryResponse;
import com.sequenceiq.flow.domain.RetryableFlow;
import com.sequenceiq.flow.service.FlowRetryService;
import com.sequenceiq.redbeams.domain.stack.DBStack;
import com.sequenceiq.redbeams.service.stack.DBStackService;

@ExtendWith(MockitoExtension.class)
class RedBeamsRetryServiceTest {
    private static final String DB_CRN = "dbCrn";

    private static final String ACCOUNT_ID = "accId";

    @Mock
    private FlowRetryService flowRetryService;

    @Mock
    private DBStackService dbStackService;

    @InjectMocks
    private RedBeamsRetryService underTest;

    @Test
    public void testRetry() {
        DBStack stack = new DBStack();
        stack.setId(6L);
        when(dbStackService.getByCrn(DB_CRN)).thenReturn(stack);
        FlowIdentifier flowIdentifier = new FlowIdentifier(FlowType.FLOW_CHAIN, "afda");
        when(flowRetryService.retry(stack.getId())).thenReturn(new RetryResponse("asdf", flowIdentifier));

        FlowIdentifier result = underTest.retry(DB_CRN);

        assertEquals(flowIdentifier, result);
    }

    @Test
    public void testRetryableFlows() {
        DBStack stack = new DBStack();
        stack.setId(6L);
        when(dbStackService.getByCrn(DB_CRN)).thenReturn(stack);
        RetryableFlow flow1 = RetryableFlow.RetryableFlowBuilder.builder().setName("fl1").setFailDate(1L).build();
        RetryableFlow flow2 = RetryableFlow.RetryableFlowBuilder.builder().setName("fl2").setFailDate(2L).build();
        when(flowRetryService.getRetryableFlows(stack.getId())).thenReturn(List.of(flow1, flow2));

        List<RetryableFlowResponse> retryableFlows = underTest.getRetryableFlows(DB_CRN);

        assertEquals(2, retryableFlows.size());
        assertThat(retryableFlows,
                hasItem(allOf(
                        hasProperty("name", is(flow1.getName())),
                        hasProperty("failDate", is(flow1.getFailDate()))
                )));
        assertThat(retryableFlows,
                hasItem(allOf(
                        hasProperty("name", is(flow2.getName())),
                        hasProperty("failDate", is(flow2.getFailDate()))
                )));
    }
}
