package com.sequenceiq.cloudbreak.blueprint.nifi;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.TestUtil;
import com.sequenceiq.cloudbreak.blueprint.BlueprintProcessorFactory;
import com.sequenceiq.cloudbreak.blueprint.BlueprintTextProcessor;
import com.sequenceiq.cloudbreak.domain.HostGroup;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;

@RunWith(MockitoJUnitRunner.class)
public class HdfConfigProviderTest {

    @Mock
    private BlueprintTextProcessor blueprintProcessor;

    @Mock
    private BlueprintProcessorFactory blueprintProcessorFactory;

    @InjectMocks
    private HdfConfigProvider underTest;

    @Test
    public void testNifiNodeIdentitiesWhenMasterIsAvailable() throws IOException {
        String blueprintText = FileReaderUtils.readFileFromClasspath("blueprints-jackson/bp-kerberized-test.bp");

        HostGroup master = TestUtil.hostGroup("master");
        HostGroup worker = TestUtil.hostGroup("worker");
        HostGroup compute = TestUtil.hostGroup("compute");

        Map<String, List<String>> fqdns = new HashMap<>();
        fqdns.put("master", TestUtil.hostMetadata(master, 1).stream().map(i -> i.getHostName()).collect(Collectors.toList()));
        fqdns.put("worker", TestUtil.hostMetadata(worker, 3).stream().map(i -> i.getHostName()).collect(Collectors.toList()));
        fqdns.put("compute", TestUtil.hostMetadata(compute, 5).stream().map(i -> i.getHostName()).collect(Collectors.toList()));

        when(blueprintProcessor.getHostGroupsWithComponent("NIFI_MASTER")).thenReturn(Sets.newHashSet("master"));
        when(blueprintProcessorFactory.get(anyString())).thenReturn(blueprintProcessor);

        Set<HostGroup> hostGroups = TestUtil.hostGroups(Sets.newHashSet("master", "worker", "compute"));
        hostGroups.forEach(hostGroup -> hostGroup.getConstraint().getInstanceGroup().setGroupName(hostGroup.getName()));

        HdfConfigs hdfConfigs = underTest.nodeIdentities(hostGroups, fqdns, blueprintText);

        Assert.assertEquals("<property name=\"Node Identity 1\">CN=hostname-2, OU=NIFI</property>", hdfConfigs.getNodeEntities());

        verify(blueprintProcessor, times(1)).getHostGroupsWithComponent("NIFI_MASTER");
    }

}