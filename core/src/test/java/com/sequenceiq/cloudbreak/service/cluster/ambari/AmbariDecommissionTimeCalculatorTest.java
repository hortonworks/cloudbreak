package com.sequenceiq.cloudbreak.service.cluster.ambari;

import static com.sequenceiq.cloudbreak.api.model.Status.AVAILABLE;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.sequenceiq.cloudbreak.TestUtil;
import com.sequenceiq.cloudbreak.core.flow2.stack.FlowMessageService;
import com.sequenceiq.cloudbreak.core.flow2.stack.Msg;
import com.sequenceiq.cloudbreak.domain.Template;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostMetadata;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.service.stack.DefaultRootVolumeSizeProvider;

@RunWith(MockitoJUnitRunner.class)
public class AmbariDecommissionTimeCalculatorTest {

    private static final long BYTE_TO_GB = 1000000000L;

    @Mock
    private FlowMessageService flowMessageService;

    @Mock
    private DefaultRootVolumeSizeProvider defaultRootVolumeSizeProvider;

    @InjectMocks
    private AmbariDecommissionTimeCalculator underTest;

    @Before
    public void setUp() {
    }

    @Test
    public void testCalculateDecommissioningTimeWhenTheCalculatedTimeLessThanOneHour() {
        Stack stack = TestUtil.stack();
        List<HostMetadata> hostMetadata = createHostMetadataList();
        Map<String, Map<Long, Long>> dfsSpace = createDfsSpaceMapsWithLowUsage();
        long usedSpace = 384 * BYTE_TO_GB;

        underTest.calculateDecommissioningTime(stack, hostMetadata, dfsSpace, usedSpace);

        verify(flowMessageService).fireEventAndLog(stack.getId(), Msg.AMBARI_CLUSTER_DECOMMISSIONING_TIME, AVAILABLE.name(), "38 minutes");
    }

    @Test
    public void testCalculateDecommissioningTimeWhenTheCalculatedTimeMoreThanOneHour() {
        Stack stack = TestUtil.stack();
        List<HostMetadata> hostMetadata = createHostMetadataList();
        Map<String, Map<Long, Long>> dfsSpace = createDfsSpaceMapsWithHalfUsage();
        long usedSpace = 5128 * BYTE_TO_GB;

        underTest.calculateDecommissioningTime(stack, hostMetadata, dfsSpace, usedSpace);

        verify(flowMessageService).fireEventAndLog(stack.getId(), Msg.AMBARI_CLUSTER_DECOMMISSIONING_TIME, AVAILABLE.name(), "5 hours");
    }

    @Test
    public void testCalculateWithNullRootVolumeSize() {
        Stack stack = TestUtil.stack();
        List<HostMetadata> hostMetadata = createHostMetadataList();
        hostMetadata.forEach(data -> data.getHostGroup().getInstanceGroup().getTemplate().setRootVolumeSize(null));
        Map<String, Map<Long, Long>> dfsSpace = createDfsSpaceMapsWithHalfUsage();
        long usedSpace = 5128 * BYTE_TO_GB;
        when(defaultRootVolumeSizeProvider.getForPlatform(anyString())).thenReturn(50);

        underTest.calculateDecommissioningTime(stack, hostMetadata, dfsSpace, usedSpace);

        verify(flowMessageService).fireEventAndLog(stack.getId(), Msg.AMBARI_CLUSTER_DECOMMISSIONING_TIME, AVAILABLE.name(), "5 hours");
    }

    private List<HostMetadata> createHostMetadataList() {
        Template template = new Template();
        template.setRootVolumeSize(50);
        template.setVolumeCount(12);
        template.setVolumeSize(1024);
        InstanceGroup instanceGroup = new InstanceGroup();
        instanceGroup.setTemplate(template);
        HostGroup hostGroup = new HostGroup();
        hostGroup.setInstanceGroup(instanceGroup);
        HostMetadata hm = new HostMetadata();
        hm.setHostGroup(hostGroup);
        return Collections.singletonList(hm);
    }

    private Map<String, Map<Long, Long>> createDfsSpaceMapsWithLowUsage() {
        Map<Long, Long> dfsSpaceByUsed = new HashMap<>();
        dfsSpaceByUsed.put(512 * BYTE_TO_GB, 11384 * BYTE_TO_GB);
        Map<String, Map<Long, Long>> dfsMaps = new HashMap<>();
        dfsMaps.put("node1.example.host", Collections.unmodifiableMap(dfsSpaceByUsed));
        dfsMaps.put("node2.example.host", Collections.unmodifiableMap(dfsSpaceByUsed));
        return dfsMaps;
    }

    private Map<String, Map<Long, Long>> createDfsSpaceMapsWithHalfUsage() {
        Map<Long, Long> dfsSpaceByUsed = new HashMap<>();
        dfsSpaceByUsed.put(5128 * BYTE_TO_GB, 6768 * BYTE_TO_GB);
        Map<String, Map<Long, Long>> dfsMaps = new HashMap<>();
        dfsMaps.put("node1.example.host", Collections.unmodifiableMap(dfsSpaceByUsed));
        dfsMaps.put("node2.example.host", Collections.unmodifiableMap(dfsSpaceByUsed));
        return dfsMaps;
    }
}