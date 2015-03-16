package com.sequenceiq.cloudbreak.service.stack.flow;

import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anySet;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.HashSet;
import java.util.Set;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.sequenceiq.cloudbreak.conf.ReactorConfig;
import com.sequenceiq.cloudbreak.domain.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.repository.RetryingStackUpdater;
import com.sequenceiq.cloudbreak.repository.StackRepository;

import reactor.core.Reactor;
import reactor.event.Event;

@Ignore("Rewrite this test!!!")
public class AmbariRoleAllocatorTest {
    @InjectMocks
    private AmbariRoleAllocator underTest;

    @Mock
    private StackRepository stackRepository;

    @Mock
    private RetryingStackUpdater stackUpdater;

    @Mock
    private Reactor reactor;

    private Set<CoreInstanceMetaData> coreInstanceMetaData;

    private Stack stack;

    @Before
    public void setUp() {
        underTest = new AmbariRoleAllocator();
        MockitoAnnotations.initMocks(this);
        coreInstanceMetaData = createCoreInstanceMetaData();
        stack = createStack();
    }

    @Test
    public void testAllocateRoles() {
        // GIVEN
        given(stackRepository.findById(1L)).willReturn(stack);
        given(stackUpdater.updateStackMetaData(anyLong(), anySet(), anyString())).willReturn(stack);
        given(stackUpdater.updateMetadataReady(1L, true)).willReturn(updatedStack());
        // WHEN
        underTest.allocateRoles(1L, coreInstanceMetaData);
        // THEN
        verify(reactor, times(1)).notify(any(ReactorConfig.class), any(Event.class));
    }

    @Test
    public void testAllocateRolesWhenStackMetaDataIsReady() {
        // GIVEN
        stack.setMetadataReady(true);
        given(stackRepository.findById(1L)).willReturn(stack);
        // WHEN
        underTest.allocateRoles(1L, coreInstanceMetaData);
        // THEN
        verify(reactor, times(0)).notify(any(ReactorConfig.class), any(Event.class));
        verify(stackUpdater, times(0)).updateMetadataReady(anyLong(), anyBoolean());
    }

    @Test
    public void testAllocateRolesWhenStackNodeCountAndMetaDataSizeIsNotEqual() {
        // GIVEN
        // stack.setNodeCount(3);
        given(stackRepository.findById(1L)).willReturn(stack);
        given(stackRepository.findOneWithLists(1L)).willReturn(stack);
        // WHEN
        underTest.allocateRoles(1L, coreInstanceMetaData);
        // THEN
        verify(reactor, times(1)).notify(any(ReactorConfig.class), any(Event.class));
        verify(stackUpdater, times(0)).updateMetadataReady(anyLong(), anyBoolean());
    }

    @Test
    public void testAllocateRolesWhenExceptionOccurs() {
        // GIVEN
        given(stackRepository.findById(1L)).willThrow(new IllegalStateException());
        given(stackRepository.findOneWithLists(1L)).willReturn(stack);
        // WHEN
        underTest.allocateRoles(1L, coreInstanceMetaData);
        // THEN
        verify(reactor, times(1)).notify(any(ReactorConfig.class), any(Event.class));
        verify(stackUpdater, times(0)).updateMetadataReady(anyLong(), anyBoolean());
    }

    private Set<CoreInstanceMetaData> createCoreInstanceMetaData() {
        Set<CoreInstanceMetaData> metaData = new HashSet<>();
        CoreInstanceMetaData data1 =
                new CoreInstanceMetaData("instanceId1", "123.123.123.123", "dummyPublicIp1", 3, "john1.john.j5.internal.cloudapp.net",
                        new InstanceGroup());
        CoreInstanceMetaData data2 =
                new CoreInstanceMetaData("instanceId2", "123.123.123.124", "dummyPublicIp2", 3, "john2.john.j5.internal.cloudapp.net",
                        new InstanceGroup());
        metaData.add(data1);
        metaData.add(data2);
        return metaData;
    }

    private Stack createStack() {
        Stack stack = new Stack();
        // stack.setNodeCount(2);
        stack.setId(1L);
        return stack;
    }

    private Stack updatedStack() {
        Stack stack = new Stack();
        //stack.setNodeCount(2);
        stack.setId(1L);
        stack.setMetadataReady(true);
        return stack;
    }
}
