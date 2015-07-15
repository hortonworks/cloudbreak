package com.sequenceiq.cloudbreak.repository;

import static org.junit.Assert.assertEquals;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.sequenceiq.cloudbreak.TestUtil;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.domain.Status;
import com.sequenceiq.cloudbreak.service.events.CloudbreakEventService;

@RunWith(MockitoJUnitRunner.class)
public class StackUpdaterTest {

    @Mock
    private StackRepository stackRepository;

    @Mock
    private CloudbreakEventService cloudbreakEventService;

    @Mock
    private ResourceRepository resourceRepository;

    @InjectMocks
    private StackUpdater underTest;

    @Test
    public void updateStackStatusWithoutStatusReasonThenNoNotificationSentOnWebsocket() {
        Stack stack = TestUtil.stack();

        when(stackRepository.findById(anyLong())).thenReturn(stack);
        when(stackRepository.save(any(Stack.class))).thenReturn(stack);
        doNothing().when(cloudbreakEventService).fireCloudbreakEvent(anyLong(), anyString(), anyString());

        Stack newStack = underTest.updateStackStatus(1L, Status.DELETE_COMPLETED);
        assertEquals(Status.DELETE_COMPLETED, newStack.getStatus());
        assertEquals("", newStack.getStatusReason());
        verify(cloudbreakEventService, times(0)).fireCloudbreakEvent(anyLong(), anyString(), anyString());
    }

    @Test
    public void updateStackStatusAndReasonThenNotificationSentOnWebsocket() {
        Stack stack = TestUtil.stack();

        when(stackRepository.findById(anyLong())).thenReturn(stack);
        when(stackRepository.save(any(Stack.class))).thenReturn(stack);
        doNothing().when(cloudbreakEventService).fireCloudbreakEvent(anyLong(), anyString(), anyString());

        Stack newStack = underTest.updateStackStatus(1L, Status.DELETE_COMPLETED, "test");
        assertEquals(Status.DELETE_COMPLETED, newStack.getStatus());
        assertEquals("test", newStack.getStatusReason());
        verify(cloudbreakEventService, times(1)).fireCloudbreakEvent(anyLong(), anyString(), anyString());
    }

    @Test
    public void addStackResourcesWithThreeNewResource() {
        Stack stack = TestUtil.stack();
        List<Resource> resources = TestUtil.generateAzureResources(5);

        when(stackRepository.findOneWithLists(anyLong())).thenReturn(stack);
        when(stackRepository.save(any(Stack.class))).then(returnsFirstArg());
        when(resourceRepository.save(anyList())).then(returnsFirstArg());

        Stack newStack = underTest.addStackResources(1L, resources);
        assertEquals(5, newStack.getResources().size());
        verify(resourceRepository, times(1)).save(anyList());
        verify(stackRepository, times(1)).save(any(Stack.class));
    }

}