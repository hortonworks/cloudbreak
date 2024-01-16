package com.sequenceiq.freeipa.service.freeipa.flow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.flow.core.FlowConstants;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.stack.StackService;

@ExtendWith(MockitoExtension.class)
class FreeIpaEventParameterFactoryTest {

    private static final String USER_CRN = "crn:user";

    private static final Map<String, Object> EVENT_PARAMETERS = Map.of(FlowConstants.FLOW_TRIGGER_USERCRN, USER_CRN);

    private static final long STACK_ID = 1L;

    @InjectMocks
    private FreeIpaEventParameterFactory underTest;

    @Mock
    private StackService stackService;

    @Mock
    private Stack stack;

    @Test
    void getUserCrnByResourceIdEmpty() {
        Optional<String> result = underTest.getUserCrnByResourceId(STACK_ID);

        assertTrue(result.isEmpty());
    }

    @Test
    void getUserCrnByResourceIdValue() {
        when(stackService.getStackById(STACK_ID)).thenReturn(stack);
        when(stack.getOwner()).thenReturn(USER_CRN);

        Optional<String> result = underTest.getUserCrnByResourceId(STACK_ID);

        assertEquals(USER_CRN, result.get());
    }

}
