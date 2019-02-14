package com.sequenceiq.cloudbreak.clusterdefinition.nifi;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.TestUtil;
import com.sequenceiq.cloudbreak.clusterdefinition.AmbariBlueprintProcessorFactory;
import com.sequenceiq.cloudbreak.template.processor.AmbariBlueprintTextProcessor;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.template.model.HdfConfigs;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;

@RunWith(MockitoJUnitRunner.class)
public class HdfConfigProviderTest {

    @Mock
    private AmbariBlueprintTextProcessor blueprintProcessor;

    @Mock
    private AmbariBlueprintProcessorFactory ambariBlueprintProcessorFactory;

    @InjectMocks
    private HdfConfigProvider underTest;

    @Test
    public void testNifiNodeIdentitiesWhenMasterIsAvailable() throws IOException {
        //GIVEN
        String blueprintText = FileReaderUtils.readFileFromClasspath("blueprints-jackson/bp-kerberized-test.bp");
        Set<HostGroup> hostGroups = new HashSet<>();
        hostGroups.add(TestUtil.hostGroup("master", 2));
        hostGroups.add(TestUtil.hostGroup("worker", 2));
        hostGroups.add(TestUtil.hostGroup("compute", 2));
        Map<String, List<InstanceMetaData>> groupInstanes = new HashMap<>();
        for (HostGroup hg: hostGroups) {
            groupInstanes.put(hg.getName(), new ArrayList<>(hg.getConstraint().getInstanceGroup().getAllInstanceMetaData()));
        }
        when(blueprintProcessor.getHostGroupsWithComponent("NIFI_MASTER")).thenReturn(Sets.newHashSet("master"));
        when(blueprintProcessor.pathValue("configurations", "nifi-ambari-config", "nifi.node.ssl.port")).thenReturn(Optional.of("9091"));
        when(ambariBlueprintProcessorFactory.get(anyString())).thenReturn(blueprintProcessor);

        // WHEN
        HdfConfigs hdfConfigs = underTest.createHdfConfig(hostGroups, groupInstanes, blueprintText);

        // THEN
        StringBuilder expectedNodeEntities = new StringBuilder();
        int i = 0;
        for (InstanceMetaData instance : groupInstanes.get("master")) {
            expectedNodeEntities.append("<property name=\"Node Identity ").append(++i).append("\">CN=").append(instance.getDiscoveryFQDN())
                    .append(", OU=NIFI</property>");
        }
        Assert.assertEquals(expectedNodeEntities.toString(), hdfConfigs.getNodeEntities());
        Assert.assertEquals(Optional.of(groupInstanes.get("master").stream().map(im -> im.getPublicIp() + ":9091").collect(Collectors.joining(","))),
                hdfConfigs.getProxyHosts());
        verify(blueprintProcessor, times(1)).getHostGroupsWithComponent("NIFI_MASTER");
    }
}
