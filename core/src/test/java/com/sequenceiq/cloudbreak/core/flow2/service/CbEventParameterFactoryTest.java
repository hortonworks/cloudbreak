package com.sequenceiq.cloudbreak.core.flow2.service;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Optional;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.sequenceiq.cloudbreak.TestUtil;
import com.sequenceiq.cloudbreak.auth.security.authentication.AuthenticatedUserService;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.workspace.model.User;
import com.sequenceiq.flow.core.FlowConstants;

@RunWith(MockitoJUnitRunner.class)
public class CbEventParameterFactoryTest {

    private static final String CRN = "crn";

    @Mock
    private StackService stackService;

    @Mock
    private AuthenticatedUserService authenticatedUserService;

    @InjectMocks
    private CbEventParameterFactory underTest;

    @Test
    public void testUserServiceReturnCrn() {
        when(authenticatedUserService.getUserCrn()).thenReturn(CRN);

        Map<String, Object> eventParameters = underTest.createEventParameters(1L);

        assertEquals(1, eventParameters.size());
        assertEquals(CRN, eventParameters.get(FlowConstants.FLOW_TRIGGER_USERCRN));
    }

    @Test
    public void testStackServiceReturnCrn() {
        when(authenticatedUserService.getUserCrn()).thenThrow(new RuntimeException());
        Stack stack = TestUtil.stack();
        User creator = new User();
        creator.setUserCrn(CRN);
        stack.setCreator(creator);
        when(stackService.findById(1L)).thenReturn(Optional.of(stack));

        Map<String, Object> eventParameters = underTest.createEventParameters(1L);

        assertEquals(1, eventParameters.size());
        assertEquals(CRN, eventParameters.get(FlowConstants.FLOW_TRIGGER_USERCRN));
    }

    @Test(expected = IllegalStateException.class)
    public void testNoCrn() {
        when(authenticatedUserService.getUserCrn()).thenThrow(new RuntimeException());
        when(stackService.findById(1L)).thenReturn(Optional.empty());

        Map<String, Object> eventParameters = underTest.createEventParameters(1L);

        assertEquals(0, eventParameters.size());
    }
}