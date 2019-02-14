package com.sequenceiq.cloudbreak.service.clusterdefinition;

import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.sequenceiq.cloudbreak.api.endpoint.v4.ExposedService;
import com.sequenceiq.cloudbreak.clusterdefinition.AmbariBlueprintProcessorFactory;
import com.sequenceiq.cloudbreak.domain.ClusterDefinition;
import com.sequenceiq.cloudbreak.domain.Constraint;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.repository.InstanceMetaDataRepository;
import com.sequenceiq.cloudbreak.service.hostgroup.HostGroupService;
import com.sequenceiq.cloudbreak.template.processor.AmbariBlueprintTextProcessor;

@RunWith(MockitoJUnitRunner.class)
public class ComponentLocatorServiceTest {

    @Mock
    private AmbariBlueprintTextProcessor blueprintProcessor;

    @Mock
    private HostGroupService hostGroupService;

    @Mock
    private AmbariBlueprintProcessorFactory ambariBlueprintProcessorFactory;

    @Mock
    private InstanceMetaDataRepository instanceMetaDataRepository;

    @InjectMocks
    private ComponentLocatorService underTest;

    private Cluster cluster;

    @Before
    public void setUp() {
        cluster = new Cluster();
        cluster.setClusterDefinition(new ClusterDefinition());

        HostGroup hg1 = createHostGroup("hg1", 0L, "myhost1");
        HostGroup hg2 = createHostGroup("hg2", 1L, "myhost2");

        Set<String> hg1Components = set("RESOURCEMANAGER", "Service1", "HIVE_SERVER");
        Set<String> hg2Components = set("NAMENODE", "Service2", "Service3");

        when(hostGroupService.getByCluster(nullable(Long.class))).thenReturn(ImmutableSet.of(hg1, hg2));
        when(blueprintProcessor.getComponentsInHostGroup(eq("hg1"))).thenReturn(hg1Components);
        when(blueprintProcessor.getComponentsInHostGroup(eq("hg2"))).thenReturn(hg2Components);
        when(ambariBlueprintProcessorFactory.get(nullable(String.class))).thenReturn(blueprintProcessor);
    }

    private HostGroup createHostGroup(String name, Long id, String hostname) {
        HostGroup hg = new HostGroup();
        hg.setName(name);
        Constraint constraint = new Constraint();
        hg.setConstraint(constraint);

        InstanceGroup ig = new InstanceGroup();
        ig.setId(id);
        constraint.setInstanceGroup(ig);

        InstanceMetaData im = new InstanceMetaData();
        im.setDiscoveryFQDN(hostname);
        ig.setInstanceMetaData(ImmutableSet.of(im));

        return hg;
    }

    @SafeVarargs
    private final <T> Set<T> set(T... array) {
        return new HashSet<>(Arrays.asList(array));
    }

    @Test
    public void getComponentLocation() {

        Map<String, List<String>> expected = Maps.newHashMap();
        expected.put("RESOURCEMANAGER", ImmutableList.of("myhost1"));
        expected.put("HIVE_SERVER", ImmutableList.of("myhost1"));
        expected.put("NAMENODE", ImmutableList.of("myhost2"));

        Map<String, List<String>> result = underTest.getComponentLocation(cluster, new HashSet<>(ExposedService.getAllServiceName()));
        Assert.assertEquals(3L, result.size());
        Assert.assertEquals(expected, result);
    }

}