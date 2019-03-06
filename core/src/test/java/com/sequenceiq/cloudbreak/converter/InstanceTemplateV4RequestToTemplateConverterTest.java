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
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.ResourceStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.mappable.Mappable;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.mappable.ProviderParameterCalculator;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.EncryptionType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.KeyEncryptionMethod;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.template.AwsEncryptionV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.template.AwsInstanceTemplateV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.template.GcpEncryptionV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.template.GcpInstanceTemplateV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.template.InstanceTemplateV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.template.volume.RootVolumeV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.template.volume.VolumeV4Request;
import com.sequenceiq.cloudbreak.cloud.PlatformParametersConsts;
import com.sequenceiq.cloudbreak.common.type.APIResourceType;
import com.sequenceiq.cloudbreak.converter.v4.stacks.instancegroup.template.InstanceTemplateV4RequestToTemplateConverter;
import com.sequenceiq.cloudbreak.domain.Template;
import com.sequenceiq.cloudbreak.domain.json.Json;
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
    public void convert() {
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

    @Test
    public void convertWithGcpEncryption() {
        InstanceTemplateV4Request source = new InstanceTemplateV4Request();
        source.setCloudPlatform(CloudPlatform.GCP);
        source.setRootVolume(getRootVolume(100));
        source.setInstanceType("n1-standard-4");
        GcpInstanceTemplateV4Parameters parameters = new GcpInstanceTemplateV4Parameters();
        GcpEncryptionV4Parameters encryption = new GcpEncryptionV4Parameters();
        encryption.setType(EncryptionType.CUSTOM);
        encryption.setKeyEncryptionMethod(KeyEncryptionMethod.RAW);
        encryption.setKey("myKey");
        parameters.setEncryption(encryption);
        source.setGcp(parameters);

        ProviderParameterCalculator providerParameterCalculator = new ProviderParameterCalculator();
        ReflectionTestUtils.setField(underTest, "providerParameterCalculator", providerParameterCalculator);

        when(missingResourceNameGenerator.generateName(APIResourceType.TEMPLATE)).thenReturn("name");

        Template result = underTest.convert(source);

        assertEquals(ResourceStatus.USER_MANAGED, result.getStatus());
        assertEquals(source.getCloudPlatform().name(), result.cloudPlatform());
        assertEquals(source.getRootVolume().getSize(), result.getRootVolumeSize());
        assertEquals(source.getInstanceType(), result.getInstanceType());

        assertNotNull(result.getAttributes());
        Map<String, Object> map = result.getAttributes().getMap();
        assertEquals("RAW", map.get("keyEncryptionMethod"));
        assertEquals("CUSTOM", map.get("type"));

        assertNotNull(result.getSecretAttributes());
        assertEquals("myKey", new Json(result.getSecretAttributes()).getMap().get("key"));
    }

    @Test
    public void convertWithAwsEncryption() {
        InstanceTemplateV4Request source = new InstanceTemplateV4Request();
        source.setCloudPlatform(CloudPlatform.AWS);
        source.setRootVolume(getRootVolume(100));
        source.setInstanceType("m5.2xlarge");
        AwsInstanceTemplateV4Parameters parameters = new AwsInstanceTemplateV4Parameters();
        AwsEncryptionV4Parameters encryption = new AwsEncryptionV4Parameters();
        encryption.setType(EncryptionType.CUSTOM);
        encryption.setKey("myKey");
        parameters.setEncryption(encryption);
        source.setAws(parameters);

        ProviderParameterCalculator providerParameterCalculator = new ProviderParameterCalculator();
        ReflectionTestUtils.setField(underTest, "providerParameterCalculator", providerParameterCalculator);

        when(missingResourceNameGenerator.generateName(APIResourceType.TEMPLATE)).thenReturn("name");

        Template result = underTest.convert(source);

        assertEquals(ResourceStatus.USER_MANAGED, result.getStatus());
        assertEquals(source.getCloudPlatform().name(), result.cloudPlatform());
        assertEquals(source.getRootVolume().getSize(), result.getRootVolumeSize());
        assertEquals(source.getInstanceType(), result.getInstanceType());

        assertNotNull(result.getAttributes());
        Map<String, Object> map = result.getAttributes().getMap();
        assertEquals("CUSTOM", map.get("type"));

        assertNotNull(result.getSecretAttributes());
        assertEquals("myKey", new Json(result.getSecretAttributes()).getMap().get("key"));
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