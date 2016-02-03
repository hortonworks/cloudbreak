package com.sequenceiq.cloudbreak.shell.commands;

import static org.mockito.BDDMockito.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.times;
import static org.mockito.BDDMockito.verify;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.doNothing;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.sequenceiq.cloudbreak.api.endpoint.StackEndpoint;
import com.sequenceiq.cloudbreak.api.model.StackResponse;
import com.sequenceiq.cloudbreak.client.CloudbreakClient;
import com.sequenceiq.cloudbreak.shell.model.CloudbreakContext;
import com.sequenceiq.cloudbreak.shell.model.Hints;

public class StackCommandsTest {
    private static final String STACK_ID = "50";
    private static final String STACK_NAME = "dummyName";

    @InjectMocks
    private StackCommands underTest;

    @Mock
    private StackEndpoint stackEndpoint;

    @Mock
    private CloudbreakClient cloudbreakClient;

    @Mock
    private CloudbreakContext mockContext;

    private StackResponse dummyResult;

    @Before
    public void setUp() throws Exception {
        underTest = new StackCommands();
        MockitoAnnotations.initMocks(this);
        dummyResult = new StackResponse();
        dummyResult.setId(Long.valueOf(STACK_ID));
        dummyResult.setName(STACK_NAME);
        given(mockContext.isCredentialAvailable()).willReturn(true);
        given(cloudbreakClient.stackEndpoint()).willReturn(stackEndpoint);
    }

    @Test
    public void testSelectStackById() throws Exception {
        given(stackEndpoint.get(Long.valueOf(STACK_ID))).willReturn(dummyResult);
        underTest.selectStack(STACK_ID, null);
        verify(stackEndpoint, times(1)).get(anyLong());
        verify(mockContext, times(1)).setHint(Hints.CREATE_CLUSTER);
    }

    @Test
    public void testSelectStackByName() throws Exception {
        given(stackEndpoint.getPublic(STACK_NAME)).willReturn(dummyResult);
        underTest.selectStack(null, STACK_NAME);
        verify(stackEndpoint, times(1)).getPublic(anyString());
        verify(mockContext, times(1)).setHint(Hints.CREATE_CLUSTER);
    }

    @Test
    public void testSelectStackByIdAndName() throws Exception {
        given(stackEndpoint.get(Long.valueOf(STACK_ID))).willReturn(dummyResult);
        underTest.selectStack(STACK_ID, STACK_NAME);
        verify(stackEndpoint, times(1)).get(anyLong());
        verify(stackEndpoint, times(0)).getPublic(anyString());
        verify(mockContext, times(1)).setHint(Hints.CREATE_CLUSTER);
    }

    @Test
    public void testSelectStackNotFoundByName() throws Exception {
        given(stackEndpoint.getPublic(STACK_NAME)).willReturn(null);
        underTest.selectStack(null, STACK_NAME);
        verify(mockContext, times(0)).setHint(Hints.CREATE_CLUSTER);
    }

    @Test
    public void testSelectStackWithoutIdAndName() throws Exception {
        underTest.selectStack(null, null);
        verify(mockContext, times(0)).setHint(Hints.CREATE_CLUSTER);
    }

    @Test
    public void testTerminateStackById() throws Exception {
        doNothing().when(stackEndpoint).delete(Long.valueOf(STACK_ID), false);
        underTest.terminateStack(STACK_ID, null, false);
        verify(mockContext, times(1)).removeStack(anyString());
        verify(stackEndpoint, times(0)).get(anyLong());
    }

    @Test
    public void testTerminateStackByName() throws Exception {
        given(stackEndpoint.getPublic(STACK_NAME)).willReturn(dummyResult);
        doNothing().when(stackEndpoint).delete(Long.valueOf(STACK_ID), false);
        underTest.terminateStack(null, STACK_NAME, false);
        verify(mockContext, times(0)).removeStack(anyString());
        verify(stackEndpoint, times(1)).getPublic(anyString());
        verify(stackEndpoint, times(0)).get(anyLong());
    }

    @Test
    public void testTerminateStackByIdAndName() throws Exception {
        doNothing().when(stackEndpoint).delete(Long.valueOf(STACK_ID), false);
        underTest.terminateStack(STACK_ID, STACK_NAME, false);
        verify(mockContext, times(1)).removeStack(anyString());
        verify(stackEndpoint, times(0)).getPublic(anyString());
    }

    @Test
    public void testTerminateWithoutStackIdAndName() throws Exception {
        underTest.terminateStack(null, null, false);
        verify(mockContext, times(0)).removeStack(anyString());
    }
}
