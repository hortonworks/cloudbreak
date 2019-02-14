package com.sequenceiq.cloudbreak.clusterdefinition;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMapOf;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.ArrayUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceGroupType;
import com.sequenceiq.cloudbreak.clusterdefinition.utils.ConfigUtils;
import com.sequenceiq.cloudbreak.clusterdefinition.utils.HadoopConfigurationUtils;
import com.sequenceiq.cloudbreak.template.processor.AmbariBlueprintTextProcessor;
import com.sequenceiq.cloudbreak.template.views.HostgroupView;

@RunWith(MockitoJUnitRunner.class)
public class ConfigServiceTest {

    @InjectMocks
    private final ConfigService underTest = new ConfigService();

    @Mock
    private ConfigUtils configUtils;

    @Mock
    private HadoopConfigurationUtils hadoopConfigurationUtils;

    @Test
    public void testCollectBlueprintConfigsFromJson() throws IOException {
        mockBlueprintConfigs();

        Map<String, Map<String, String>> actual = underTest.collectBlueprintConfigsFromJson();

        Map<String, Map<Object, Object>> expected = new HashMap<>();
        expected.put("hive-sites", ArrayUtils.toMap(new String[][]{{"someKey1", "someValue1"}, {"someKey2", "someValue2"}, {"someKey3", "someValue3"}}));
        expected.put("other-sites", emptyMap());

        Assert.assertEquals(actual, expected);
    }

    private void mockBlueprintConfigs() throws IOException {
        ArrayNode rootJsonNode = JsonNodeFactory.instance.arrayNode();
        ObjectNode object1 = rootJsonNode.addObject();
        object1.put("name", "hive-sites");
        ArrayNode configurations = object1.putArray("configurations");
        configurations.addObject()
                .put("key", "someKey1")
                .put("value", "someValue1");

        configurations.addObject()
                .put("key", "someKey2")
                .put("value", "someValue2");

        configurations.addObject()
                .put("key", "someKey3")
                .put("value", "someValue3");

        object1 = rootJsonNode.addObject();
        object1.put("name", "other-sites");
        object1.putArray("configurations");

        when(configUtils.readConfigJson("hdp/bp-config.json", "sites")).thenReturn(rootJsonNode);
    }

    @Test
    public void testCollectBlueprintConfigsFromJsonWhenEmpty() throws IOException {
        ArrayNode rootJsonNode = JsonNodeFactory.instance.arrayNode();
        when(configUtils.readConfigJson("hdp/bp-config.json", "sites")).thenReturn(rootJsonNode);
        Map<String, Map<String, String>> actual = underTest.collectBlueprintConfigsFromJson();

        Assert.assertEquals(actual, emptyMap());
    }

    @Test
    public void testCollectServiceConfigsFromJson() throws IOException {
        mockServiceConfigs();

        Map<String, ServiceConfig> actual = underTest.collectServiceConfigsFromJson();

        Map<String, ServiceConfig> expected = new HashMap<>();
        Map<String, List<ConfigProperty>> globalProperties = singletonMap("type1",
                singletonList(new ConfigProperty("global_name1", "global_dir1", "global_prefix1")));

        Map<String, List<ConfigProperty>> hostProperties = singletonMap("type1",
                singletonList(new ConfigProperty("host_name1", "host_dir1", "host_prefix1")));

        ServiceConfig serviceConfig1 = new ServiceConfig("service1", Arrays.asList("NODE1", "NODE2"), globalProperties, hostProperties);
        ServiceConfig serviceConfig2 = new ServiceConfig("service2", emptyList(), emptyMap(), emptyMap());
        expected.put("service1", serviceConfig1);
        expected.put("service2", serviceConfig2);

        Assert.assertEquals(actual, expected);
    }

    protected void mockServiceConfigs() throws IOException {
        ArrayNode rootJsonNode = JsonNodeFactory.instance.arrayNode();
        ObjectNode object1 = rootJsonNode.addObject();
        object1.put("name", "service1");
        ArrayNode relatedServices = object1.putArray("related_services");
        relatedServices.add("NODE1").add("NODE2");
        ArrayNode configurations = object1.putArray("configurations");
        ObjectNode type = configurations.addObject()
                .put("type", "type1");

        type.putArray("global").addObject()
                .put("name", "global_name1")
                .put("prefix", "global_prefix1")
                .put("directory", "global_dir1");

        type.putArray("host")
                .addObject()
                .put("name", "host_name1")
                .put("prefix", "host_prefix1")
                .put("directory", "host_dir1");

        object1 = rootJsonNode.addObject();
        object1.put("name", "service2");
        object1.putArray("configurations");
        object1.putArray("related_services");

        when(configUtils.readConfigJson("hdp/service-config.json", "services")).thenReturn(rootJsonNode);
    }

    @Test
    public void testCollectServiceConfigsFromJsonWhenEmpty() throws IOException {
        ArrayNode rootJsonNode = JsonNodeFactory.instance.arrayNode();
        when(configUtils.readConfigJson("hdp/service-config.json", "services")).thenReturn(rootJsonNode);
        Map<String, ServiceConfig> actual = underTest.collectServiceConfigsFromJson();

        Assert.assertEquals(actual, emptyMap());
    }

    @Test
    public void testCollectBlueprintConfigIfNeed() throws IOException {

        init();

        Map<String, Map<String, String>> config = new HashMap<>();
        Map<String, String> map = new HashMap<>();
        map.put("key", "value");

        config.put("hive-sites", map);

        underTest.collectBlueprintConfigIfNeed(config);

        Map<String, String> hiveSite = config.get("hive-sites");
        Assert.assertEquals(4L, hiveSite.size());
        Assert.assertEquals("value", hiveSite.get("key"));

        Assert.assertNotNull(config.get("other-sites"));

    }

    @Test
    public void testCollectBlueprintConfigIfNeedWhenBpConfigsIsEmpty() throws IOException {
        ArrayNode rootJsonNode = JsonNodeFactory.instance.arrayNode();
        when(configUtils.readConfigJson("hdp/bp-config.json", "sites")).thenReturn(rootJsonNode);
        rootJsonNode = JsonNodeFactory.instance.arrayNode();
        when(configUtils.readConfigJson("hdp/service-config.json", "services")).thenReturn(rootJsonNode);
        underTest.init();

        Map<String, Map<String, String>> config = new HashMap<>();
        Map<String, String> map = new HashMap<>();
        map.put("key", "value");

        config.put("hive-sites", map);

        underTest.collectBlueprintConfigIfNeed(config);

        Map<String, String> hiveSite = config.get("hive-sites");
        Assert.assertEquals(1L, hiveSite.size());
        Assert.assertNull(config.get("other-sites"));
    }

    @Test
    public void testGetHostGroupConfiguration() throws IOException {
        init();

        HostgroupView hostGroup = new HostgroupView("hostGroup", 10, InstanceGroupType.CORE, 1);

        Set<String> components = singleton("component");

        AmbariBlueprintTextProcessor ambariBlueprintTextProcessor = mock(AmbariBlueprintTextProcessor.class);

        Map<String, Map<String, String>> properties = new HashMap<>();
        properties.put("key", singletonMap("propKey", "propValue"));

        when(configUtils.isConfigUpdateNeeded(hostGroup)).thenReturn(true);
        when(ambariBlueprintTextProcessor.getComponentsInHostGroup(hostGroup.getName())).thenReturn(components);
        when(configUtils.getProperties(any(ServiceConfig.class), eq(false), eq(10), eq(components))).thenReturn(properties);

        Map<String, Map<String, Map<String, String>>> actual = underTest.getHostGroupConfiguration(ambariBlueprintTextProcessor, singletonList(hostGroup));

        Map<String, Map<String, Map<String, String>>> expected = new HashMap<>();
        expected.put("hostGroup", properties);

        Assert.assertEquals(actual, expected);

    }

    @Test
    public void testGetHostGroupConfigurationWhenTemplateIsNull() throws IOException {
        init();

        HostgroupView hostGroup = new HostgroupView("hostGroup");

        Set<String> components = singleton("component");

        AmbariBlueprintTextProcessor ambariBlueprintTextProcessor = mock(AmbariBlueprintTextProcessor.class);

        Map<String, Map<String, String>> properties = new HashMap<>();
        properties.put("key", singletonMap("propKey", "propValue"));

        when(configUtils.isConfigUpdateNeeded(hostGroup)).thenReturn(true);
        when(ambariBlueprintTextProcessor.getComponentsInHostGroup(hostGroup.getName())).thenReturn(components);
        when(configUtils.getProperties(any(ServiceConfig.class), eq(false), eq(-1), eq(components))).thenReturn(properties);

        Map<String, Map<String, Map<String, String>>> actual = underTest.getHostGroupConfiguration(ambariBlueprintTextProcessor, singletonList(hostGroup));

        Map<String, Map<String, Map<String, String>>> expected = new HashMap<>();
        expected.put("hostGroup", properties);

        Assert.assertEquals(actual, expected);

    }

    @Test
    public void testGetHostGroupConfigurationWhenConfigServiceIsEmpty() throws IOException {
        ArrayNode rootJsonNode = JsonNodeFactory.instance.arrayNode();
        when(configUtils.readConfigJson("hdp/bp-config.json", "sites")).thenReturn(rootJsonNode);
        rootJsonNode = JsonNodeFactory.instance.arrayNode();
        when(configUtils.readConfigJson("hdp/service-config.json", "services")).thenReturn(rootJsonNode);
        underTest.init();

        HostgroupView hostGroup = new HostgroupView("hostGroup", 1, InstanceGroupType.CORE, 1);

        AmbariBlueprintTextProcessor ambariBlueprintTextProcessor = mock(AmbariBlueprintTextProcessor.class);

        Map<String, Map<String, String>> properties = new HashMap<>();

        when(configUtils.isConfigUpdateNeeded(hostGroup)).thenReturn(true);

        Map<String, Map<String, Map<String, String>>> actual = underTest.getHostGroupConfiguration(ambariBlueprintTextProcessor, singletonList(hostGroup));

        Map<String, Map<String, Map<String, String>>> expected = new HashMap<>();
        expected.put("hostGroup", properties);

        Assert.assertEquals(actual, expected);
    }

    @Test
    public void testGetHostGroupConfigurationWhenConfigUpdateIsNotNeed() {
        HostgroupView hostGroup = new HostgroupView("hostGroupName");
        AmbariBlueprintTextProcessor ambariBlueprintTextProcessor = mock(AmbariBlueprintTextProcessor.class);

        when(configUtils.isConfigUpdateNeeded(hostGroup)).thenReturn(false);

        Map<String, Map<String, Map<String, String>>> actual = underTest.getHostGroupConfiguration(ambariBlueprintTextProcessor, singletonList(hostGroup));

        Assert.assertEquals(actual, emptyMap());
    }

    @Test
    public void testGetHostGroupConfigurationWhenHostGroupsIsEmpty() {
        AmbariBlueprintTextProcessor ambariBlueprintTextProcessor = mock(AmbariBlueprintTextProcessor.class);

        Map<String, Map<String, Map<String, String>>> actual = underTest.getHostGroupConfiguration(ambariBlueprintTextProcessor, emptyList());

        Assert.assertEquals(actual, emptyMap());
    }

    @Test
    public void testGetComponentsByHostGroup() throws IOException {
        init();

        String hostGroupName = "hostGroup";
        HostgroupView hostGroup = new HostgroupView(hostGroupName, 1, InstanceGroupType.CORE, 1);
        String blueprintText = "blueprintText";

        AmbariBlueprintTextProcessor ambariBlueprintTextProcessor = mock(AmbariBlueprintTextProcessor.class);

        Map<String, Set<String>> componentsByHostgroup = new HashMap<>();
        componentsByHostgroup.put(hostGroupName, Sets.newHashSet("serviceName1", "serviceName2"));

        ServiceConfig serviceConfig1 = new ServiceConfig("serviceName1", emptyList(), emptyMap(), emptyMap());
        ServiceConfig serviceConfig2 = new ServiceConfig("serviceName2", emptyList(), emptyMap(), emptyMap());

        List<HostgroupView> hostGroups = singletonList(hostGroup);

        Set<String> components = singleton("component");

        Map<String, Map<String, String>> properties1 = new HashMap<>();
        properties1.put("key1", singletonMap("propKey1", "propValue1"));
        Map<String, Map<String, String>> properties2 = new HashMap<>();
        properties2.put("key2", singletonMap("propKey2", "propValue2"));

        when(ambariBlueprintTextProcessor.getComponentsByHostGroup()).thenReturn(componentsByHostgroup);
        when(hadoopConfigurationUtils.findHostGroupForNode(hostGroups, hostGroupName)).thenReturn(hostGroup);
        when(configUtils.isConfigUpdateNeeded(hostGroup)).thenReturn(true);
        when(configUtils.getServiceConfig(eq("serviceName1"), anyMapOf(String.class, ServiceConfig.class))).thenReturn(serviceConfig1);
        when(configUtils.getServiceConfig(eq("serviceName2"), anyMapOf(String.class, ServiceConfig.class))).thenReturn(serviceConfig2);
        when(ambariBlueprintTextProcessor.getComponentsInHostGroup(hostGroup.getName())).thenReturn(components);
        when(configUtils.getProperties(eq(serviceConfig1), eq(true), eq(0), eq(components))).thenReturn(properties1);
        when(configUtils.getProperties(eq(serviceConfig2), eq(true), eq(0), eq(components))).thenReturn(properties2);

        Map<String, Map<String, String>> actual = underTest.getComponentsByHostGroup(ambariBlueprintTextProcessor, hostGroups);

        Map<String, Map<String, String>> expected = properties1;
        expected.putAll(properties2);

        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testGetComponentsByHostGroupWhenConfigUpdateIsNotNeed() throws IOException {
        init();

        String hostGroupName = "hostGroup";
        HostgroupView hostGroup = new HostgroupView(hostGroupName, 1, InstanceGroupType.CORE, 1);

        AmbariBlueprintTextProcessor ambariBlueprintTextProcessor = mock(AmbariBlueprintTextProcessor.class);

        Map<String, Set<String>> componentsByHostgroup = new HashMap<>();
        componentsByHostgroup.put(hostGroupName, Sets.newHashSet("serviceName"));

        ServiceConfig serviceConfig = new ServiceConfig("serviceName", emptyList(), emptyMap(), emptyMap());

        List<HostgroupView> hostGroups = singletonList(hostGroup);

        Set<String> components = singleton("component");

        Map<String, Map<String, String>> properties = new HashMap<>();
        properties.put("key", singletonMap("propKey", "propValue"));

        when(ambariBlueprintTextProcessor.getComponentsByHostGroup()).thenReturn(componentsByHostgroup);
        when(hadoopConfigurationUtils.findHostGroupForNode(hostGroups, hostGroupName)).thenReturn(hostGroup);
        when(configUtils.isConfigUpdateNeeded(hostGroup)).thenReturn(false);
        when(configUtils.getServiceConfig(eq("serviceName"), anyMapOf(String.class, ServiceConfig.class))).thenReturn(serviceConfig);
        when(ambariBlueprintTextProcessor.getComponentsInHostGroup(hostGroup.getName())).thenReturn(components);
        when(configUtils.getProperties(eq(serviceConfig), eq(true), eq(-1), eq(components))).thenReturn(properties);

        Map<String, Map<String, String>> actual = underTest.getComponentsByHostGroup(ambariBlueprintTextProcessor, hostGroups);

        Map<String, Map<String, String>> expected = properties;

        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testGetComponentsByHostGroupWhenServiceConfigIsNull() throws IOException {
        init();

        String hostGroupName = "hostGroup";
        HostgroupView hostGroup = new HostgroupView(hostGroupName, 1, InstanceGroupType.CORE, 1);
        String blueprintText = "blueprintText";

        AmbariBlueprintTextProcessor ambariBlueprintTextProcessor = mock(AmbariBlueprintTextProcessor.class);

        Map<String, Set<String>> componentsByHostgroup = new HashMap<>();
        componentsByHostgroup.put(hostGroupName, Sets.newHashSet("serviceName"));

        List<HostgroupView> hostGroups = singletonList(hostGroup);

        when(ambariBlueprintTextProcessor.getComponentsByHostGroup()).thenReturn(componentsByHostgroup);
        when(hadoopConfigurationUtils.findHostGroupForNode(hostGroups, hostGroupName)).thenReturn(hostGroup);
        when(configUtils.isConfigUpdateNeeded(hostGroup)).thenReturn(true);
        when(configUtils.getServiceConfig(eq("serviceName"), anyMapOf(String.class, ServiceConfig.class))).thenReturn(null);

        Map<String, Map<String, String>> actual = underTest.getComponentsByHostGroup(ambariBlueprintTextProcessor, hostGroups);

        Assert.assertEquals(emptyMap(), actual);

        verify(ambariBlueprintTextProcessor, times(0)).getComponentsInHostGroup(hostGroupName);
    }

    @Test
    public void testGetComponentsByHostGroupWhenServicesIsEmpty() throws IOException {
        init();

        String hostGroupName = "hostGroup";
        HostgroupView hostGroup = new HostgroupView(hostGroupName, 1, InstanceGroupType.CORE, 1);

        AmbariBlueprintTextProcessor ambariBlueprintTextProcessor = mock(AmbariBlueprintTextProcessor.class);

        Map<String, Set<String>> componentsByHostgroup = new HashMap<>();
        componentsByHostgroup.put(hostGroupName, emptySet());

        List<HostgroupView> hostGroups = singletonList(hostGroup);

        when(ambariBlueprintTextProcessor.getComponentsByHostGroup()).thenReturn(componentsByHostgroup);
        when(hadoopConfigurationUtils.findHostGroupForNode(hostGroups, hostGroupName)).thenReturn(hostGroup);
        when(configUtils.isConfigUpdateNeeded(hostGroup)).thenReturn(true);

        Map<String, Map<String, String>> actual = underTest.getComponentsByHostGroup(ambariBlueprintTextProcessor, hostGroups);

        Assert.assertEquals(emptyMap(), actual);

        verify(configUtils, times(1)).isConfigUpdateNeeded(hostGroup);
        verify(ambariBlueprintTextProcessor, times(0)).getComponentsInHostGroup(hostGroupName);
    }

    @Test
    public void testGetComponentsByHostGroupWhenComponentsIsEmpty() throws IOException {
        init();

        String hostGroupName = "hostGroup";
        HostgroupView hostGroup = new HostgroupView(hostGroupName, 1, InstanceGroupType.CORE, 1);

        AmbariBlueprintTextProcessor ambariBlueprintTextProcessor = mock(AmbariBlueprintTextProcessor.class);

        List<HostgroupView> hostGroups = singletonList(hostGroup);

        when(ambariBlueprintTextProcessor.getComponentsByHostGroup()).thenReturn(emptyMap());

        Map<String, Map<String, String>> actual = underTest.getComponentsByHostGroup(ambariBlueprintTextProcessor, hostGroups);

        Assert.assertEquals(emptyMap(), actual);

        verify(configUtils, times(0)).isConfigUpdateNeeded(hostGroup);
    }

    public void init() throws IOException {
        mockBlueprintConfigs();
        mockServiceConfigs();
        underTest.init();
    }
}
