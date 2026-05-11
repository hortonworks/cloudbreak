package com.sequenceiq.redbeams.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.api.model.FlowType;
import com.sequenceiq.redbeams.api.model.common.Status;
import com.sequenceiq.redbeams.domain.stack.DBStack;
import com.sequenceiq.redbeams.domain.stack.DBStackStatus;
import com.sequenceiq.redbeams.flow.RedbeamsFlowManager;
import com.sequenceiq.redbeams.service.stack.DBStackService;

@ExtendWith(MockitoExtension.class)
class RedbeamsTagUpdaterServiceTest {

    private static final String RESOURCE_CRN = "resourceCrn";

    private static final long DB_STACK_ID = 1L;

    private static final Map<String, String> USER_DEFINED_TAGS = Map.of("custom", "value");

    @Mock
    private RedbeamsFlowManager redbeamsFlowManager;

    @Mock
    private DBStackService dbStackService;

    @InjectMocks
    private RedbeamsTagUpdaterService underTest;

    @Test
    void triggerUserDefinedTagsUpdate() {
        DBStackStatus dbStackStatus = new DBStackStatus();
        dbStackStatus.setStatus(Status.AVAILABLE);

        DBStack dbStack = new DBStack();
        dbStack.setId(DB_STACK_ID);
        dbStack.setDBStackStatus(dbStackStatus);

        FlowIdentifier expectedFlow = new FlowIdentifier(FlowType.FLOW, "flow-id-1");

        when(dbStackService.getByCrn(RESOURCE_CRN)).thenReturn(dbStack);
        when(redbeamsFlowManager.triggerUserDefinedTagsUpdate(DB_STACK_ID, USER_DEFINED_TAGS)).thenReturn(expectedFlow);

        FlowIdentifier result = underTest.triggerUserDefinedTagsUpdate(RESOURCE_CRN, USER_DEFINED_TAGS);

        assertEquals(expectedFlow, result);
        verify(dbStackService).getByCrn(RESOURCE_CRN);
        verify(redbeamsFlowManager).triggerUserDefinedTagsUpdate(DB_STACK_ID, USER_DEFINED_TAGS);
    }
}