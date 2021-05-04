package com.sequenceiq.freeipa.service.stack.instance;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Set;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceStatus;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.Stack;

@RunWith(MockitoJUnitRunner.class)
public class InstanceUpdaterTest {

    @Mock
    private InstanceMetaDataService instanceMetaDataService;

    @Mock
    private Stack stack;

    @InjectMocks
    private InstanceUpdater underTest;

    @Test
    public void testUpdateStatuses() {

        InstanceMetaData instanceMetaData1 = mock(InstanceMetaData.class);
        InstanceMetaData instanceMetaData2 = mock(InstanceMetaData.class);
        when(instanceMetaData1.getInstanceStatus()).thenReturn(InstanceStatus.CREATED);
        when(instanceMetaData2.getInstanceStatus()).thenReturn(InstanceStatus.CREATED);

        Set<InstanceMetaData> metadata = Set.of(instanceMetaData1, instanceMetaData2);
        when(stack.getNotDeletedInstanceMetaDataSet()).thenReturn(metadata);

        underTest.updateStatuses(stack, InstanceStatus.STOPPED);

        verify(instanceMetaData1, times(1)).setInstanceStatus(InstanceStatus.STOPPED);
        verify(instanceMetaData2, times(1)).setInstanceStatus(InstanceStatus.STOPPED);
        verify(instanceMetaDataService, times(2)).save(any());
    }

}