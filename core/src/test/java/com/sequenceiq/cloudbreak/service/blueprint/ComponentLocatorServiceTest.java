package com.sequenceiq.cloudbreak.service.blueprint;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.when;

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
import com.sequenceiq.cloudbreak.blueprint.AmbariBlueprintProcessorFactory;
import com.sequenceiq.cloudbreak.blueprint.AmbariBlueprintTextProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessorFactory;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.Constraint;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.service.hostgroup.HostGroupService;

@RunWith(MockitoJUnitRunner.class)
public class ComponentLocatorServiceTest {

    @Mock
    private AmbariBlueprintTextProcessor ambariBlueprintTextProcessor;

    @Mock
    private CmTemplateProcessor cmTemplateProcessor;

    @Mock
    private HostGroupService hostGroupService;

    @Mock
    private AmbariBlueprintProcessorFactory ambariBlueprintProcessorFactory;

    @Mock
    private CmTemplateProcessorFactory cmTemplateProcessorFactory;

    @Mock
    private BlueprintService blueprintService;

    @InjectMocks
    private ComponentLocatorService underTest;

    private Cluster cluster;

    @Before
    public void setUp() {
        cluster = new Cluster();
        cluster.setBlueprint(new Blueprint());

        HostGroup hg1 = createHostGroup("hg1", 0L, "myhost1", "10.0.140.69");
        HostGroup hg2 = createHostGroup("hg2", 1L, "myhost2", "10.0.140.70");

        Set<String> hg1Components = Set.of("RESOURCEMANAGER", "Service1", "HIVE_SERVER");
        Set<String> hg2Components = Set.of("NAMENODE", "Service2", "Service3");

        when(hostGroupService.getByCluster(nullable(Long.class))).thenReturn(ImmutableSet.of(hg1, hg2));
        when(ambariBlueprintTextProcessor.getComponentsInHostGroup(eq("hg1"))).thenReturn(hg1Components);
        when(ambariBlueprintTextProcessor.getComponentsInHostGroup(eq("hg2"))).thenReturn(hg2Components);
        when(ambariBlueprintProcessorFactory.get(nullable(String.class))).thenReturn(ambariBlueprintTextProcessor);
    }

    private HostGroup createHostGroup(String name, Long id, String hostname, String privateIp) {
        HostGroup hg = new HostGroup();
        hg.setName(name);
        Constraint constraint = new Constraint();
        hg.setConstraint(constraint);

        InstanceGroup ig = new InstanceGroup();
        ig.setId(id);
        constraint.setInstanceGroup(ig);

        InstanceMetaData im = new InstanceMetaData();
        im.setDiscoveryFQDN(hostname);
        im.setPrivateIp(privateIp);
        ig.setInstanceMetaData(ImmutableSet.of(im));

        return hg;
    }

    @Test
    public void getComponentLocation() {
        when(blueprintService.isAmbariBlueprint(any())).thenReturn(true);

        Map<String, List<String>> expected = Maps.newHashMap();
        expected.put("RESOURCEMANAGER", ImmutableList.of("myhost1"));
        expected.put("HIVE_SERVER", ImmutableList.of("myhost1"));
        expected.put("NAMENODE", ImmutableList.of("myhost2"));

        Map<String, List<String>> result = underTest.getComponentLocation(cluster, new HashSet<>(ExposedService.getAllServiceNameForAmbari()));
        Assert.assertEquals(3L, result.size());
        Assert.assertEquals(expected, result);
    }

    @Test
    public void getComponentLocationCM() {
        when(blueprintService.isAmbariBlueprint(any())).thenReturn(false);
        when(cmTemplateProcessorFactory.get(nullable(String.class))).thenReturn(cmTemplateProcessor);
        when(cmTemplateProcessor.getComponentsInHostGroup(eq("hg1"))).thenReturn(Set.of("RESOURCEMANAGER", "Service1", "HIVESERVER2"));
        when(cmTemplateProcessor.getComponentsInHostGroup(eq("hg2"))).thenReturn(Set.of("NAMENODE", "Service2", "Service3"));

        Map<String, List<String>> expected = Map.of(
                "RESOURCEMANAGER", ImmutableList.of("myhost1"),
                "HIVESERVER2", ImmutableList.of("myhost1"),
                "NAMENODE", ImmutableList.of("myhost2")
        );

        Map<String, List<String>> result = underTest.getComponentLocation(cluster, new HashSet<>(ExposedService.getAllServiceNameForCM()));

        Assert.assertEquals(expected, result);
    }

    @Test
    public void getComponentPrivateIp() {

        Map<String, List<String>> expected = Maps.newHashMap();
        expected.put("RESOURCEMANAGER", ImmutableList.of("10.0.140.69"));
        expected.put("HIVE_SERVER", ImmutableList.of("10.0.140.69"));
        expected.put("NAMENODE", ImmutableList.of("10.0.140.70"));

        Map<String, List<String>> result = underTest.getComponentPrivateIp(cluster.getId(), ambariBlueprintTextProcessor,
                new HashSet<>(ExposedService.getAllServiceNameForAmbari()));
        Assert.assertEquals(3L, result.size());
        Assert.assertEquals(expected, result);
    }

}