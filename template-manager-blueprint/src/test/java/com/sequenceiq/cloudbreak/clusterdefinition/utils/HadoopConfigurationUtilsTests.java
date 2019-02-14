package com.sequenceiq.cloudbreak.clusterdefinition.utils;

import static java.util.Collections.singletonList;

import java.util.Collection;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceGroupType;
import com.sequenceiq.cloudbreak.clusterdefinition.ConfigProperty;
import com.sequenceiq.cloudbreak.template.ClusterDefinitionProcessingException;
import com.sequenceiq.cloudbreak.template.views.HostgroupView;

@RunWith(MockitoJUnitRunner.class)
public class HadoopConfigurationUtilsTests {

    @Rule
    public final ExpectedException expectedException = ExpectedException.none();

    private final HadoopConfigurationUtils underTest = new HadoopConfigurationUtils();

    @Test
    public void testFindHostGroupForNode() {
        HostgroupView hostGroup = new HostgroupView("hostGroupName", 1, InstanceGroupType.CORE, 1);
        Collection<HostgroupView> hostGroups = singletonList(hostGroup);
        HostgroupView actual = underTest.findHostGroupForNode(hostGroups, "hostGroupName");

        Assert.assertEquals(actual, hostGroup);
    }

    @Test
    public void testFindHostGroupForNodeWhenNotFound() {
        expectedException.expect(ClusterDefinitionProcessingException.class);
        expectedException.expectMessage("Couldn't find a saved hostgroup for [hostGroupName] hostgroup name in the validation.");

        HostgroupView hostGroup = new HostgroupView("hostGroupName1", 1, InstanceGroupType.CORE, 1);
        Collection<HostgroupView> hostGroups = singletonList(hostGroup);
        underTest.findHostGroupForNode(hostGroups, "hostGroupName");
    }

    @Test
    public void testGetValueWhenVolumeMoreThan0AndGlobal() {
        ConfigProperty configProperty = new ConfigProperty("global-name", "global-dir", "global-prefix");

        String actual = underTest.getValue(configProperty, "serviceName", true, 1);

        String expected = "global-prefix/hadoopfs/fs1/servicename/global-dir";
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testGetValueWhenVolumeIsNullAndGlobal() {
        ConfigProperty configProperty = new ConfigProperty("global-name", "global-dir", "global-prefix");

        String actual = underTest.getValue(configProperty, "serviceName", true, 0);

        String expected = "global-prefix/hadoopfs/fs1/servicename/global-dir";
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testGetValueWhenVolumeIsNullAndNotGlobal() {
        ConfigProperty configProperty = new ConfigProperty("global-name", "global-dir", "global-prefix");

        String actual = underTest.getValue(configProperty, "serviceName", false, 0);

        Assert.assertNull(actual);
    }

    @Test
    public void testGetValueWhenVolumeIsMoreThan0AndNotGlobal() {
        ConfigProperty configProperty = new ConfigProperty("global-name", "global-dir", "global-prefix");

        String actual = underTest.getValue(configProperty, "serviceName", false, 1);

        String expected = "/hadoopfs/fs1/servicename/global-dir";
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testGetValueWhenVolumeIsMoreLess0AndNotGlobal() {
        ConfigProperty configProperty = new ConfigProperty("global-name", "global-dir", "global-prefix");

        String actual = underTest.getValue(configProperty, "serviceName", false, -1);

        Assert.assertNull(actual);
    }

    @Test
    public void testGetValueWhenVolumeIsMoreLess0AndGlobal() {
        ConfigProperty configProperty = new ConfigProperty("global-name", "global-dir", "global-prefix");

        String actual = underTest.getValue(configProperty, "serviceName", false, -1);

        Assert.assertNull(actual);
    }
}
