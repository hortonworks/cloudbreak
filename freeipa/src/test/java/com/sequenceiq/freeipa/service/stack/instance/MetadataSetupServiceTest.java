package com.sequenceiq.freeipa.service.stack.instance;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Set;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.sequenceiq.cloudbreak.common.service.Clock;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceStatus;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.Stack;

@RunWith(MockitoJUnitRunner.class)
public class MetadataSetupServiceTest {

    private static final Long STACK_ID = 1L;

    private static final String GROUP_NAME = "GROUP_NAME";

    private static final Long PRIVATE_ID = 2L;

    private static final String SUBNET_ID = "SUBNET_ID";

    private static final String INSTANCE_NAME = "INSTANCE_NAME";

    private static final String PRIVATE_IP = "PRIVATE_IP";

    private static final String PUBLIC_IP = "PUBLIC_IP";

    private static final Integer SSH_PORT = 22;

    private static final String LOCALITY_INDICATOR = "LOCALITY_INDICATOR";

    private static final Long INSTANCE_GROUP_ID = 3L;

    private static final Long CURRENT_TIME = System.currentTimeMillis();

    @Mock
    private InstanceGroupService instanceGroupService;

    @Mock
    private InstanceMetaDataService instanceMetaDataService;

    @Mock
    private Clock clock;

    @InjectMocks
    private MetadataSetupService underTest;

    @Test
    public void testCleanupRequestedInstances() {
        Stack stack = new Stack();
        stack.setId(STACK_ID);

        InstanceMetaData instanceMetaData1 = mock(InstanceMetaData.class);
        InstanceMetaData instanceMetaData2 = mock(InstanceMetaData.class);
        when(instanceMetaData1.getInstanceStatus()).thenReturn(InstanceStatus.REQUESTED);
        when(instanceMetaData2.getInstanceStatus()).thenReturn(InstanceStatus.CREATED);
        Set<InstanceMetaData> metadata = Set.of(instanceMetaData1, instanceMetaData2);

        when(clock.getCurrentTimeMillis()).thenReturn(CURRENT_TIME);
        when(instanceMetaDataService.findNotTerminatedForStack(any())).thenReturn(metadata);

        underTest.cleanupRequestedInstances(stack);

        verify(instanceMetaData1, times(1)).setTerminationDate(CURRENT_TIME);
        verify(instanceMetaData1, times(1)).setInstanceStatus(InstanceStatus.TERMINATED);
        verify(instanceMetaData2, never()).setTerminationDate(any());
        verify(instanceMetaData2, never()).setInstanceStatus(InstanceStatus.TERMINATED);
        verify(instanceMetaDataService, times(1)).saveAll(any());
    }
}