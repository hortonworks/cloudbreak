package com.sequenceiq.cloudbreak.service.blueprint;

import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
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
import org.mockito.runners.MockitoJUnitRunner;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.sequenceiq.cloudbreak.api.model.ExposedService;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.Constraint;
import com.sequenceiq.cloudbreak.domain.HostGroup;
import com.sequenceiq.cloudbreak.domain.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.InstanceMetaData;
import com.sequenceiq.cloudbreak.service.cluster.flow.blueprint.BlueprintProcessor;
import com.sequenceiq.cloudbreak.service.hostgroup.HostGroupService;

@RunWith(MockitoJUnitRunner.class)
public class ComponentLocatorServiceTest {

    @Mock
    private BlueprintProcessor blueprintProcessor;

    @Mock
    private HostGroupService hostGroupService;

    @InjectMocks
    private ComponentLocatorService underTest;

    private Cluster cluster;

    @Before
    public void setUp() {
        cluster = new Cluster();
        cluster.setBlueprint(new Blueprint());

        HostGroup hg1 = createHostGroup("hg1", "myhost1");
        HostGroup hg2 = createHostGroup("hg2", "myhost2");

        Set<String> hg1Components = set("RESOURCEMANAGER", "Service1", "HIVE_SERVER");
        Set<String> hg2Components = set("NAMENODE", "Service2", "Service3");

        when(hostGroupService.getByCluster(anyLong())).thenReturn(ImmutableSet.of(hg1, hg2));
        when(blueprintProcessor.getComponentsInHostGroup(anyString(), eq("hg1"))).thenReturn(hg1Components);
        when(blueprintProcessor.getComponentsInHostGroup(anyString(), eq("hg2"))).thenReturn(hg2Components);
    }

    private HostGroup createHostGroup(String name, String hostname) {
        HostGroup hg = new HostGroup();
        hg.setName(name);
        Constraint constraint = new Constraint();
        hg.setConstraint(constraint);

        InstanceGroup ig = new InstanceGroup();
        constraint.setInstanceGroup(ig);

        InstanceMetaData im = new InstanceMetaData();
        im.setDiscoveryFQDN(hostname);
        ig.setInstanceMetaData(ImmutableSet.of(im));

        return hg;
    }

    private <T> Set<T> set(T... array) {
        return new HashSet<>(Arrays.asList(array));
    }

    @Test
    public void getComponentLocation() throws Exception {

        Map<String, List<String>> expected = Maps.newHashMap();
        expected.put("RESOURCEMANAGER", ImmutableList.of("myhost1"));
        expected.put("HIVE_SERVER", ImmutableList.of("myhost1"));
        expected.put("NAMENODE", ImmutableList.of("myhost2"));

        Map<String, List<String>> result = underTest.getComponentLocation(cluster, new HashSet<>(ExposedService.getAllServiceName()));
        Assert.assertEquals(3, result.size());
        Assert.assertEquals(expected, result);
    }

}