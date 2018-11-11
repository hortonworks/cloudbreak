package com.sequenceiq.cloudbreak.converter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.when;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.sequenceiq.cloudbreak.api.model.CustomInstanceType;
import com.sequenceiq.cloudbreak.api.model.ResourceStatus;
import com.sequenceiq.cloudbreak.api.model.TemplateRequest;
import com.sequenceiq.cloudbreak.cloud.PlatformParametersConsts;
import com.sequenceiq.cloudbreak.domain.Template;
import com.sequenceiq.cloudbreak.domain.Topology;
import com.sequenceiq.cloudbreak.domain.json.Json;
import com.sequenceiq.cloudbreak.service.topology.TopologyService;

@RunWith(MockitoJUnitRunner.class)
public class TemplateRequestToTemplateConverterTest {

    @InjectMocks
    private TemplateRequestToTemplateConverter underTest;

    @Mock
    private TopologyService topologyService;

    @Test
    public void convert() throws Exception {
        TemplateRequest source = new TemplateRequest();
        source.setName("name");
        source.setDescription("description");
        source.setCloudPlatform("gcp");
        source.setRootVolumeSize(100);
        source.setInstanceType("large");
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("someAttr", "value");
        source.setParameters(parameters);
        Map<String, Object> secretParameters = Map.of("secretAttr", "value");
        source.setSecretParameters(secretParameters);
        source.setTopologyId(1L);

        Topology topology = new Topology();
        when(topologyService.get(1L)).thenReturn(topology);

        Template result = underTest.convert(source);

        assertEquals(source.getName(), result.getName());
        assertEquals(source.getDescription(), result.getDescription());
        assertEquals(ResourceStatus.USER_MANAGED, result.getStatus());
        assertEquals(source.getCloudPlatform(), result.cloudPlatform());
        assertEquals(source.getRootVolumeSize(), result.getRootVolumeSize());
        assertEquals(source.getInstanceType(), result.getInstanceType());

        assertNotNull(result.getAttributes());
        assertEquals(new Json(parameters), result.getAttributes());
        assertNotNull(result.getSecretAttributes());
        assertEquals(new Json(secretParameters).getValue(), result.getSecretAttributes());
        assertEquals(topology, result.getTopology());
    }

    @Test
    public void convertWithCustomInstanceType() throws Exception {
        TemplateRequest source = new TemplateRequest();
        source.setName("name");
        source.setDescription("description");
        source.setCloudPlatform("gcp");
        source.setRootVolumeSize(100);
        source.setInstanceType("large");
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("someAttr", "value");
        source.setParameters(parameters);
        Map<String, Object> secretParameters = Map.of("secretAttr", "value");
        source.setSecretParameters(secretParameters);
        source.setTopologyId(1L);

        CustomInstanceType customInstanceType = new CustomInstanceType();
        customInstanceType.setCpus(1);
        customInstanceType.setMemory(1);
        source.setCustomInstanceType(customInstanceType);

        Topology topology = new Topology();
        when(topologyService.get(1L)).thenReturn(topology);

        Template result = underTest.convert(source);

        assertEquals(source.getName(), result.getName());
        assertEquals(source.getDescription(), result.getDescription());
        assertEquals(ResourceStatus.USER_MANAGED, result.getStatus());
        assertEquals(source.getCloudPlatform(), result.cloudPlatform());
        assertEquals(source.getRootVolumeSize(), result.getRootVolumeSize());
        assertEquals(source.getInstanceType(), result.getInstanceType());

        assertNotNull(result.getAttributes());
        Map<String, ? extends Serializable> attributeMap = Map.of(
                PlatformParametersConsts.CUSTOM_INSTANCETYPE_MEMORY, 1,
                PlatformParametersConsts.CUSTOM_INSTANCETYPE_CPUS, 1,
                "someAttr", "value");
        assertEquals(new Json(attributeMap).getMap(), result.getAttributes().getMap());
        assertNotNull(result.getSecretAttributes());
        assertEquals(new Json(secretParameters).getMap(), new Json(result.getSecretAttributes()).getMap());
        assertEquals(topology, result.getTopology());
    }
}