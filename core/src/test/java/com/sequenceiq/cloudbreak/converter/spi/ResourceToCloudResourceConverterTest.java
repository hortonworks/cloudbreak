package com.sequenceiq.cloudbreak.converter.spi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cluster.util.ResourceAttributeUtil;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.common.api.type.CommonStatus;
import com.sequenceiq.common.api.type.LoadBalancerTypeAttribute;
import com.sequenceiq.common.api.type.ResourceType;

@ExtendWith(MockitoExtension.class)
class ResourceToCloudResourceConverterTest {

    @Mock
    private ResourceAttributeUtil resourceAttributeUtil;

    @Mock
    private Resource resource;

    @InjectMocks
    private ResourceToCloudResourceConverter underTest;

    @Test
    void testConvertShouldPreserveExtraAttributeFieldsAsParameters() {
        String jsonContent = "{\"name\":\"PRIVATE\",\"attributeType\":\"com.sequenceiq.common.api.type.LoadBalancerTypeAttribute\","
                + "\"hcport\":{\"path\":\"/lb-health-check\",\"port\":5080,\"protocol\":\"HTTPS\",\"interval\":10,\"probeDownThreshold\":2}}";
        Json attributes = new Json(jsonContent);

        when(resourceAttributeUtil.getTypedAttributes(resource)).thenReturn(Optional.of(LoadBalancerTypeAttribute.PRIVATE));
        when(resource.getResourceType()).thenReturn(ResourceType.GCP_HEALTH_CHECK);
        when(resource.getResourceName()).thenReturn("test-hc");
        when(resource.getResourceStatus()).thenReturn(CommonStatus.CREATED);
        when(resource.getAttributes()).thenReturn(attributes);

        CloudResource cloudResource = underTest.convert(resource);

        assertEquals(LoadBalancerTypeAttribute.PRIVATE, cloudResource.getParameter(CloudResource.ATTRIBUTES, LoadBalancerTypeAttribute.class));
        assertNotNull(cloudResource.getParameter("hcport", Map.class));
        Map<String, Object> hcportMap = cloudResource.getParameter("hcport", Map.class);
        assertEquals("/lb-health-check", hcportMap.get("path"));
        assertEquals(5080, hcportMap.get("port"));
    }

    @Test
    void testConvertShouldPreserveTrafficportsFromForwardingRule() {
        String jsonContent = "{\"name\":\"PRIVATE\",\"attributeType\":\"com.sequenceiq.common.api.type.LoadBalancerTypeAttribute\","
                + "\"hcport\":{\"path\":\"/lb-health-check\",\"port\":5080,\"protocol\":\"HTTPS\",\"interval\":10,\"probeDownThreshold\":2},"
                + "\"trafficports\":{\"trafficPorts\":[88,636,53],\"trafficProtocol\":\"TCP\"}}";
        Json attributes = new Json(jsonContent);

        when(resourceAttributeUtil.getTypedAttributes(resource)).thenReturn(Optional.of(LoadBalancerTypeAttribute.PRIVATE));
        when(resource.getResourceType()).thenReturn(ResourceType.GCP_FORWARDING_RULE);
        when(resource.getResourceName()).thenReturn("test-fr");
        when(resource.getResourceStatus()).thenReturn(CommonStatus.CREATED);
        when(resource.getAttributes()).thenReturn(attributes);

        CloudResource cloudResource = underTest.convert(resource);

        assertNotNull(cloudResource.getParameter("trafficports", Map.class));
        Map<String, Object> trafficMap = cloudResource.getParameter("trafficports", Map.class);
        assertNotNull(trafficMap.get("trafficPorts"));
        assertEquals("TCP", trafficMap.get("trafficProtocol"));
    }

    @Test
    void testConvertShouldExcludeAttributeTypeFromParameters() {
        String jsonContent = "{\"name\":\"PRIVATE\",\"attributeType\":\"com.sequenceiq.common.api.type.LoadBalancerTypeAttribute\","
                + "\"hcport\":5080}";
        Json attributes = new Json(jsonContent);

        when(resourceAttributeUtil.getTypedAttributes(resource)).thenReturn(Optional.of(LoadBalancerTypeAttribute.PRIVATE));
        when(resource.getResourceType()).thenReturn(ResourceType.GCP_RESERVED_IP);
        when(resource.getResourceName()).thenReturn("test-ip");
        when(resource.getResourceStatus()).thenReturn(CommonStatus.CREATED);
        when(resource.getAttributes()).thenReturn(attributes);

        CloudResource cloudResource = underTest.convert(resource);

        assertNull(cloudResource.getParameter("attributeType", String.class));
        assertEquals(5080, cloudResource.getParameter("hcport", Integer.class));
    }

    @Test
    void testConvertShouldHandleNullAttributes() {
        when(resourceAttributeUtil.getTypedAttributes(resource)).thenReturn(Optional.empty());
        when(resource.getResourceType()).thenReturn(ResourceType.GCP_INSTANCE);
        when(resource.getResourceName()).thenReturn("test");
        when(resource.getResourceStatus()).thenReturn(CommonStatus.CREATED);
        when(resource.getAttributes()).thenReturn(null);

        CloudResource cloudResource = underTest.convert(resource);

        assertNotNull(cloudResource);
        assertEquals("test", cloudResource.getName());
    }

    @Test
    void testConvertShouldNotOverwriteTypedAttributeWithRawEntry() {
        String jsonContent = "{\"name\":\"PRIVATE\",\"attributeType\":\"com.sequenceiq.common.api.type.LoadBalancerTypeAttribute\","
                + "\"hcport\":5080}";
        Json attributes = new Json(jsonContent);

        when(resourceAttributeUtil.getTypedAttributes(resource)).thenReturn(Optional.of(LoadBalancerTypeAttribute.PRIVATE));
        when(resource.getResourceType()).thenReturn(ResourceType.GCP_RESERVED_IP);
        when(resource.getResourceName()).thenReturn("test-ip");
        when(resource.getResourceStatus()).thenReturn(CommonStatus.CREATED);
        when(resource.getAttributes()).thenReturn(attributes);

        CloudResource cloudResource = underTest.convert(resource);

        assertEquals(LoadBalancerTypeAttribute.PRIVATE, cloudResource.getParameter(CloudResource.ATTRIBUTES, LoadBalancerTypeAttribute.class));
    }
}
