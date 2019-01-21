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

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.ResourceStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.custominstance.CustomInstanceV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.template.InstanceTemplateV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.template.volume.VolumeV4Request;
import com.sequenceiq.cloudbreak.cloud.PlatformParametersConsts;
import com.sequenceiq.cloudbreak.converter.v4.stacks.instancegroup.template.InstanceTemplateV4RequestToTemplateConverter;
import com.sequenceiq.cloudbreak.domain.Template;
import com.sequenceiq.cloudbreak.domain.Topology;
import com.sequenceiq.cloudbreak.domain.json.Json;
import com.sequenceiq.cloudbreak.service.topology.TopologyService;

@RunWith(MockitoJUnitRunner.class)
public class InstanceTemplateV4RequestToTemplateConverterTest {

    @InjectMocks
    private InstanceTemplateV4RequestToTemplateConverter underTest;

    @Mock
    private TopologyService topologyService;

    @Test
    public void convert() throws Exception {
        InstanceTemplateV4Request source = new InstanceTemplateV4Request();
        source.setCloudPlatform(CloudPlatform.GCP);
        source.setRootVolume(getRootVolume(100));
        source.setInstanceType("large");
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("someAttr", "value");

        Topology topology = new Topology();
        when(topologyService.get(1L)).thenReturn(topology);

        Template result = underTest.convert(source);

        assertEquals(ResourceStatus.USER_MANAGED, result.getStatus());
        assertEquals(source.getCloudPlatform(), result.cloudPlatform());
        assertEquals(source.getRootVolume().getSize(), result.getRootVolumeSize());
        assertEquals(source.getInstanceType(), result.getInstanceType());

        assertNotNull(result.getAttributes());
        assertEquals(new Json(parameters), result.getAttributes());
        assertNotNull(result.getSecretAttributes());
        assertEquals(topology, result.getTopology());
    }

    @Test
    public void convertWithCustomInstanceType() throws Exception {
        InstanceTemplateV4Request source = new InstanceTemplateV4Request();
        source.setCloudPlatform(CloudPlatform.GCP);
        source.setRootVolume(getRootVolume(100));
        source.setInstanceType("large");

        CustomInstanceV4Request customInstanceType = new CustomInstanceV4Request();
        customInstanceType.setCpus(1);
        customInstanceType.setMemory(1);
        source.setCustomInstance(customInstanceType);

        Topology topology = new Topology();
        when(topologyService.get(1L)).thenReturn(topology);

        Template result = underTest.convert(source);

        assertEquals(ResourceStatus.USER_MANAGED, result.getStatus());
        assertEquals(source.getCloudPlatform(), result.cloudPlatform());
        assertEquals(source.getInstanceType(), result.getInstanceType());

        assertNotNull(result.getAttributes());
        Map<String, ? extends Serializable> attributeMap = Map.of(
                PlatformParametersConsts.CUSTOM_INSTANCETYPE_MEMORY, 1,
                PlatformParametersConsts.CUSTOM_INSTANCETYPE_CPUS, 1,
                "someAttr", "value");
        assertEquals(new Json(attributeMap).getMap(), result.getAttributes().getMap());
        assertNotNull(result.getSecretAttributes());
        assertEquals(topology, result.getTopology());
    }

    private VolumeV4Request getRootVolume(int size) {
        VolumeV4Request rootVolume = new VolumeV4Request();
        rootVolume.setSize(size);
        return rootVolume;
    }
}