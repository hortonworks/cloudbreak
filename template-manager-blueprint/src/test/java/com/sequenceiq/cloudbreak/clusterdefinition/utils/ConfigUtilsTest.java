package com.sequenceiq.cloudbreak.clusterdefinition.utils;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
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

import com.fasterxml.jackson.databind.JsonNode;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceGroupType;
import com.sequenceiq.cloudbreak.clusterdefinition.ConfigProperty;
import com.sequenceiq.cloudbreak.clusterdefinition.ServiceConfig;
import com.sequenceiq.cloudbreak.template.views.HostgroupView;

@RunWith(MockitoJUnitRunner.class)
public class ConfigUtilsTest {

    @Rule
    public final ExpectedException expectedException = ExpectedException.none();

    @InjectMocks
    private final ConfigUtils underTest = new ConfigUtils();

    @Mock
    private HadoopConfigurationUtils hadoopConfigurationUtils;

    @Test
    public void testIsConfigNeedTrue() {
        HostgroupView hostGroup = new HostgroupView("hostGroupName1", 1, InstanceGroupType.CORE, 1);

        boolean actual = underTest.isConfigUpdateNeeded(hostGroup);

        Assert.assertTrue(actual);
    }

    @Test
    public void testIsConfigNeedFalse() {
        HostgroupView hostGroup = new HostgroupView("hostGroupName1");

        boolean actual = underTest.isConfigUpdateNeeded(hostGroup);

        Assert.assertFalse(actual);
    }

    @Test
    public void testGetPropertiesWhenGlobal() {
        Map<String, List<ConfigProperty>> globalConfig = new HashMap<>();
        ConfigProperty configProperty = new ConfigProperty("global-name", "global-dir", "global-prefix");
        globalConfig.put("global-key", Collections.singletonList(configProperty));

        ServiceConfig serviceConfig = new ServiceConfig("service", Collections.singletonList("NODE1"), globalConfig, emptyMap());
        Collection<String> hostComponents = Arrays.asList("NODE1", "NODE2");

        when(hadoopConfigurationUtils.getValue(configProperty, serviceConfig.getServiceName(), true, 1)).thenReturn("value");

        Map<String, Map<String, String>> actual = underTest.getProperties(serviceConfig, true, 1, hostComponents);

        Map<String, Map<String, String>> expected = new HashMap<>();
        expected.put("global-key", Collections.singletonMap("global-name", "value"));
        Assert.assertEquals(expected, actual);

    }

    @Test
    public void testGetPropertiesWhenNotGlobal() {
        Map<String, List<ConfigProperty>> hostConfig = new HashMap<>();
        ConfigProperty configProperty = new ConfigProperty("host-name", "host-dir", "host-prefix");
        hostConfig.put("host-key", Collections.singletonList(configProperty));

        ServiceConfig serviceConfig = new ServiceConfig("service", Collections.singletonList("NODE1"), emptyMap(), hostConfig);
        Collection<String> hostComponents = Arrays.asList("NODE1", "NODE2");

        when(hadoopConfigurationUtils.getValue(configProperty, serviceConfig.getServiceName(), false, 1)).thenReturn("value");

        Map<String, Map<String, String>> actual = underTest.getProperties(serviceConfig, false, 1, hostComponents);

        Map<String, Map<String, String>> expected = new HashMap<>();
        expected.put("host-key", Collections.singletonMap("host-name", "value"));
        Assert.assertEquals(expected, actual);

    }

    @Test
    public void testGetPropertiesWhenValueIsNull() {
        Map<String, List<ConfigProperty>> hostConfig = new HashMap<>();
        ConfigProperty configProperty = new ConfigProperty("host-name", "host-dir", "host-prefix");
        hostConfig.put("host-key", Collections.singletonList(configProperty));

        ServiceConfig serviceConfig = new ServiceConfig("service", Collections.singletonList("NODE1"), emptyMap(), hostConfig);
        Collection<String> hostComponents = Arrays.asList("NODE1", "NODE2");

        when(hadoopConfigurationUtils.getValue(configProperty, serviceConfig.getServiceName(), false, 1)).thenReturn(null);

        Map<String, Map<String, String>> actual = underTest.getProperties(serviceConfig, false, 1, hostComponents);

        Assert.assertEquals(emptyMap(), actual);

    }

    @Test
    public void testGetPropertiesWhenHostComponentsDoesNotContainsRelatedService() {
        ServiceConfig serviceConfig = new ServiceConfig("service", Collections.singletonList("NODE1"), emptyMap(), emptyMap());
        Collection<String> hostComponents = Arrays.asList("OTHER_NODE1", "OTHER_NODE2");

        Map<String, Map<String, String>> actual = underTest.getProperties(serviceConfig, false, 1, hostComponents);

        Assert.assertEquals(emptyMap(), actual);

        verify(hadoopConfigurationUtils, times(0)).getValue(any(ConfigProperty.class), anyString(), anyBoolean(), anyInt());

    }

    @Test
    public void testGetServiceConfigWhenEqualsServiceName() {
        Map<String, ServiceConfig> serviceConfigs = new HashMap<>();
        serviceConfigs.put("serviceName", new ServiceConfig("serviceName", emptyList(), emptyMap(), emptyMap()));

        ServiceConfig actual = underTest.getServiceConfig("serviceName", serviceConfigs);

        Assert.assertNotNull(actual);
        Assert.assertEquals("serviceName", actual.getServiceName());

    }

    @Test
    public void testGetServiceConfigWhenStartWithServiceName() {
        Map<String, ServiceConfig> serviceConfigs = new HashMap<>();
        serviceConfigs.put("serviceName", new ServiceConfig("serviceName", emptyList(), emptyMap(), emptyMap()));

        ServiceConfig actual = underTest.getServiceConfig("serviceNameStartWith", serviceConfigs);

        Assert.assertNotNull(actual);
        Assert.assertEquals("serviceName", actual.getServiceName());
    }

    @Test
    public void testGetServiceConfigWhenNotStartWithServiceName() {
        Map<String, ServiceConfig> serviceConfigs = new HashMap<>();
        serviceConfigs.put("serviceNameStartWith", new ServiceConfig("serviceName", emptyList(), emptyMap(), emptyMap()));

        ServiceConfig actual = underTest.getServiceConfig("serviceName", serviceConfigs);

        Assert.assertNull(actual);
    }

    @Test
    public void testGetServiceConfigWhenSelectFirstServiceName() {
        Map<String, ServiceConfig> serviceConfigs = new LinkedHashMap<>();
        serviceConfigs.put("serviceName1", new ServiceConfig("serviceName1", emptyList(), emptyMap(), emptyMap()));
        serviceConfigs.put("serviceName", new ServiceConfig("serviceName", emptyList(), emptyMap(), emptyMap()));

        ServiceConfig actual = underTest.getServiceConfig("serviceName1StartWith", serviceConfigs);

        Assert.assertNotNull(actual);
        Assert.assertEquals("serviceName1", actual.getServiceName());
    }

    @Test
    public void testGetServiceConfigWhenSelectSecondServiceName() {
        Map<String, ServiceConfig> serviceConfigs = new LinkedHashMap<>();
        serviceConfigs.put("serviceNam1", new ServiceConfig("serviceNam1", emptyList(), emptyMap(), emptyMap()));
        serviceConfigs.put("serviceName", new ServiceConfig("serviceName", emptyList(), emptyMap(), emptyMap()));

        ServiceConfig actual = underTest.getServiceConfig("serviceNameStartWith", serviceConfigs);

        Assert.assertNotNull(actual);
        Assert.assertEquals("serviceName", actual.getServiceName());
    }

    @Test
    public void readConfigJson() throws IOException {
        JsonNode jsonNode = underTest.readConfigJson("blueprints-jackson/test-read-blueprint.bp", "components");

        Assert.assertNotNull(jsonNode);
        Assert.assertEquals("comp1", jsonNode.get("names").get(0).asText());
        Assert.assertEquals("service-name2", jsonNode.get("services").get("name2").asText());
    }

    @Test
    public void readConfigJsonWhenIOException() throws IOException {
        expectedException.expect(IOException.class);
        expectedException.expectMessage("class path resource [not-exists.bp] cannot be opened because it does not exist");

        underTest.readConfigJson("not-exists.bp", "components");
    }
}
