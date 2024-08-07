package com.sequenceiq.cloudbreak.core.flow2.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.workspace.model.User;

@ExtendWith(MockitoExtension.class)
class CbEventParameterFactoryTest {

    private static final String CRN = "crn";

    private static final long RESOURCE_ID = 1L;

    @InjectMocks
    private CbEventParameterFactory underTest;

    @Mock
    private StackService stackService;

    @Mock
    private Stack stack;

    @Mock
    private User user;

    @Test
    void getUserCrnByResourceIdEmpty() {
        Optional<String> result = underTest.getUserCrnByResourceId(RESOURCE_ID);

        assertTrue(result.isEmpty());
    }

    @Test
    void getUserCrnByResourceIdValue() {
        when(stackService.get(RESOURCE_ID)).thenReturn(stack);
        when(stack.getCreator()).thenReturn(user);
        when(user.getUserCrn()).thenReturn(CRN);

        Optional<String> result = underTest.getUserCrnByResourceId(RESOURCE_ID);

        assertEquals(CRN, result.get());
    }
}
