package com.sequenceiq.cloudbreak.service.cluster.ambari;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.TestUtil;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.service.cluster.InstanceGroupMetadataCollector;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;

@ExtendWith(MockitoExtension.class)
class InstanceGroupMetadataCollectorTest {

    @Mock
    private InstanceMetaDataService instanceMetaDataService;

    @InjectMocks
    private final InstanceGroupMetadataCollector underTest = new InstanceGroupMetadataCollector();

    @Test
    void testCollectFqdnsWhenMetadataAvailable() {
        Stack stack = TestUtil.stack();

        for (InstanceGroup instanceGroup : stack.getInstanceGroups()) {
            when(instanceMetaDataService.findAliveInstancesInInstanceGroup(instanceGroup.getId()))
                    .thenReturn(Lists.newArrayList(instanceGroup.getAllInstanceMetaData().iterator()));
        }

        Map<String, List<InstanceMetaData>> stringListMap = underTest.collectMetadata(stack);

        assertEquals(3L, stringListMap.size());
        assertTrue(stringListMap.keySet().containsAll(Sets.newHashSet("is1", "is2", "is3")));

        verify(instanceMetaDataService, times(3)).findAliveInstancesInInstanceGroup(anyLong());
    }

}
