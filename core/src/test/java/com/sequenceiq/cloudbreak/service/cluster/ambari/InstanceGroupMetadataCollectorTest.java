package com.sequenceiq.cloudbreak.service.cluster.ambari;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.TestUtil;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.repository.InstanceMetaDataRepository;
import com.sequenceiq.cloudbreak.service.cluster.InstanceGroupMetadataCollector;

@RunWith(MockitoJUnitRunner.class)
public class InstanceGroupMetadataCollectorTest {

    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    @Mock
    private InstanceMetaDataRepository instanceMetadataRepository;

    @InjectMocks
    private final InstanceGroupMetadataCollector underTest = new InstanceGroupMetadataCollector();

    @Test
    public void testCollectFqdnsWhenMetadataAvailable() {
        Stack stack = TestUtil.stack();

        for (InstanceGroup instanceGroup : stack.getInstanceGroups()) {
            when(instanceMetadataRepository.findAliveInstancesInInstanceGroup(instanceGroup.getId()))
                    .thenReturn(Lists.newArrayList(instanceGroup.getAllInstanceMetaData().iterator()));
        }

        Map<String, List<InstanceMetaData>> stringListMap = underTest.collectMetadata(stack);

        Assert.assertEquals(3L, stringListMap.size());
        Assert.assertTrue(stringListMap.keySet().containsAll(Sets.newHashSet("is1", "is2", "is3")));

        verify(instanceMetadataRepository, times(3)).findAliveInstancesInInstanceGroup(anyLong());
    }
}
