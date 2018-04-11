package com.sequenceiq.cloudbreak.service.blueprint;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.sequenceiq.cloudbreak.api.model.ExposedService;
import com.sequenceiq.cloudbreak.domain.*;
import com.sequenceiq.cloudbreak.service.hostgroup.HostGroupService;
import com.sequenceiq.cloudbreak.templateprocessor.processor.TemplateProcessorFactory;
import com.sequenceiq.cloudbreak.templateprocessor.processor.TemplateTextProcessor;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.*;

import static org.mockito.Matchers.*;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ComponentLocatorServiceTest {

    @Mock
    private TemplateTextProcessor blueprintProcessor;

    @Mock
    private HostGroupService hostGroupService;

    @Mock
    private TemplateProcessorFactory blueprintProcessorFactory;

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
        when(blueprintProcessor.getComponentsInHostGroup(eq("hg1"))).thenReturn(hg1Components);
        when(blueprintProcessor.getComponentsInHostGroup(eq("hg2"))).thenReturn(hg2Components);
        when(blueprintProcessorFactory.get(anyString())).thenReturn(blueprintProcessor);
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