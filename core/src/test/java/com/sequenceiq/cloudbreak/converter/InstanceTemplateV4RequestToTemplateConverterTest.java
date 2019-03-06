package com.sequenceiq.cloudbreak.converter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.ResourceStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.mappable.Mappable;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.mappable.ProviderParameterCalculator;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.template.InstanceTemplateV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.template.volume.RootVolumeV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.template.volume.VolumeV4Request;
import com.sequenceiq.cloudbreak.cloud.PlatformParametersConsts;
import com.sequenceiq.cloudbreak.common.type.APIResourceType;
import com.sequenceiq.cloudbreak.converter.v4.stacks.instancegroup.template.InstanceTemplateV4RequestToTemplateConverter;
import com.sequenceiq.cloudbreak.domain.Template;
import com.sequenceiq.cloudbreak.service.MissingResourceNameGenerator;
import com.sequenceiq.cloudbreak.service.stack.DefaultRootVolumeSizeProvider;

@RunWith(MockitoJUnitRunner.class)
public class InstanceTemplateV4RequestToTemplateConverterTest {

    @Mock
    private MissingResourceNameGenerator missingResourceNameGenerator;

    @Mock
    private ProviderParameterCalculator providerParameterCalculator;

    @Mock
    private DefaultRootVolumeSizeProvider defaultRootVolumeSizeProvider;

    @Mock
    private Mappable mappable;

    @InjectMocks
    private InstanceTemplateV4RequestToTemplateConverter underTest;

    @Before
    public void setup() {
        when(missingResourceNameGenerator.generateName(APIResourceType.TEMPLATE)).thenReturn("name");
    }

    @Test
    public void convert() throws Exception {
        InstanceTemplateV4Request source = new InstanceTemplateV4Request();
        source.setCloudPlatform(CloudPlatform.GCP);
        source.setRootVolume(getRootVolume(100));
        source.setInstanceType("large");
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("cpus", 1);

        when(missingResourceNameGenerator.generateName(APIResourceType.TEMPLATE)).thenReturn("name");
        when(providerParameterCalculator.get(source)).thenReturn(mappable);
        when(mappable.asMap()).thenReturn(parameters);

        Template result = underTest.convert(source);

        assertEquals(ResourceStatus.USER_MANAGED, result.getStatus());
        assertEquals(source.getCloudPlatform().name(), result.cloudPlatform());
        assertEquals(source.getRootVolume().getSize(), result.getRootVolumeSize());
        assertEquals(source.getInstanceType(), result.getInstanceType());

        assertNotNull(result.getAttributes());
        assertEquals(1, result.getAttributes().getMap().get("cpus"));
        assertNotNull(result.getSecretAttributes());
    }

    @Test
    public void convertWithCustomInstanceType() {
        InstanceTemplateV4Request source = new InstanceTemplateV4Request();
        source.setCloudPlatform(CloudPlatform.GCP);
        source.setRootVolume(getRootVolume(100));
        source.setInstanceType("large");
        source.setAttachedVolumes(getAttachedVolumes(50));

        Map<String, Object> attributeMap = Map.of(
                PlatformParametersConsts.CUSTOM_INSTANCETYPE_MEMORY, 1,
                PlatformParametersConsts.CUSTOM_INSTANCETYPE_CPUS, 1);

        when(missingResourceNameGenerator.generateName(APIResourceType.TEMPLATE)).thenReturn("name");
        when(providerParameterCalculator.get(source)).thenReturn(mappable);
        when(mappable.asMap()).thenReturn(attributeMap);

        Template result = underTest.convert(source);

        assertEquals(ResourceStatus.USER_MANAGED, result.getStatus());
        assertEquals(source.getCloudPlatform().name(), result.cloudPlatform());
        assertEquals(source.getInstanceType(), result.getInstanceType());

        assertNotNull(result.getAttributes());
        assertEquals(result.getAttributes().getMap().get(PlatformParametersConsts.CUSTOM_INSTANCETYPE_MEMORY), 1);
        assertEquals(result.getAttributes().getMap().get(PlatformParametersConsts.CUSTOM_INSTANCETYPE_CPUS), 1);
        assertNotNull(result.getSecretAttributes());
    }

    @Test
    public void convertWithNullRootVolumeSize() {
        InstanceTemplateV4Request source = new InstanceTemplateV4Request();
        source.setCloudPlatform(CloudPlatform.GCP);
        source.setRootVolume(getRootVolume(null));
        source.setInstanceType("large");
        source.setAttachedVolumes(getAttachedVolumes(50));

        Map<String, Object> attributeMap = Map.of(
                PlatformParametersConsts.CUSTOM_INSTANCETYPE_MEMORY, 1,
                PlatformParametersConsts.CUSTOM_INSTANCETYPE_CPUS, 1);

        int rootVolumeSize = 60;
        when(defaultRootVolumeSizeProvider.getForPlatform(CloudPlatform.GCP.name())).thenReturn(rootVolumeSize);
        when(missingResourceNameGenerator.generateName(APIResourceType.TEMPLATE)).thenReturn("name");
        when(providerParameterCalculator.get(source)).thenReturn(mappable);
        when(mappable.asMap()).thenReturn(attributeMap);

        Template result = underTest.convert(source);

        assertEquals(ResourceStatus.USER_MANAGED, result.getStatus());
        assertEquals(source.getCloudPlatform().name(), result.cloudPlatform());
        assertEquals(source.getInstanceType(), result.getInstanceType());

        assertNotNull(result.getAttributes());
        assertEquals(1, result.getAttributes().getMap().get(PlatformParametersConsts.CUSTOM_INSTANCETYPE_MEMORY));
        assertEquals(1, result.getAttributes().getMap().get(PlatformParametersConsts.CUSTOM_INSTANCETYPE_CPUS));
        assertNotNull(result.getSecretAttributes());
        assertEquals(rootVolumeSize, result.getRootVolumeSize().intValue());
    }

    private RootVolumeV4Request getRootVolume(Integer size) {
        RootVolumeV4Request rootVolume = new RootVolumeV4Request();
        rootVolume.setSize(size);
        return rootVolume;
    }

    private Set<VolumeV4Request> getAttachedVolumes(int size) {
        VolumeV4Request volume = new VolumeV4Request();
        volume.setSize(size);
        volume.setCount(1);
        volume.setType("type");
        return Collections.singleton(volume);
    }
}