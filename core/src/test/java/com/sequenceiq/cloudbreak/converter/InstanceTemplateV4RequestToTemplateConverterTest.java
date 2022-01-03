package com.sequenceiq.cloudbreak.converter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.ResourceStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.KeyEncryptionMethod;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.template.AwsEncryptionV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.template.AwsInstanceTemplateV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.template.GcpEncryptionV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.template.GcpInstanceTemplateV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.template.InstanceTemplateV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.template.volume.RootVolumeV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.template.volume.VolumeV4Request;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.cloud.PlatformParametersConsts;
import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate;
import com.sequenceiq.cloudbreak.common.converter.MissingResourceNameGenerator;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.common.mappable.Mappable;
import com.sequenceiq.cloudbreak.common.mappable.ProviderParameterCalculator;
import com.sequenceiq.cloudbreak.common.type.APIResourceType;
import com.sequenceiq.cloudbreak.common.type.TemporaryStorage;
import com.sequenceiq.cloudbreak.converter.v4.stacks.instancegroup.template.InstanceTemplateV4RequestToTemplateConverter;
import com.sequenceiq.cloudbreak.domain.Template;
import com.sequenceiq.cloudbreak.service.stack.DefaultRootVolumeSizeProvider;
import com.sequenceiq.common.api.type.EncryptionType;

@ExtendWith(MockitoExtension.class)
public class InstanceTemplateV4RequestToTemplateConverterTest {

    private static final String ACCOUNT_ID = UUID.randomUUID().toString();

    private static final String USER_CRN = "crn:altus:iam:us-west-1:" + ACCOUNT_ID + ":user:" + UUID.randomUUID().toString();

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

    @BeforeEach
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

        assertThat(result.getStatus()).isEqualTo(ResourceStatus.USER_MANAGED);
        assertThat(result.cloudPlatform()).isEqualTo(source.getCloudPlatform().name());
        assertThat(result.getRootVolumeSize()).isEqualTo(source.getRootVolume().getSize());
        assertThat(result.getInstanceType()).isEqualTo(source.getInstanceType());

        assertThat(result.getAttributes()).isNotNull();
        assertThat(result.getAttributes().getMap().get("cpus")).isEqualTo(1);
        assertThat(result.getSecretAttributes()).isNotNull();
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

        assertThat(result.getStatus()).isEqualTo(ResourceStatus.USER_MANAGED);
        assertThat(result.cloudPlatform()).isEqualTo(source.getCloudPlatform().name());
        assertThat(result.getInstanceType()).isEqualTo(source.getInstanceType());

        assertThat(result.getAttributes()).isNotNull();
        assertThat(result.getAttributes().getMap().get(PlatformParametersConsts.CUSTOM_INSTANCETYPE_MEMORY)).isEqualTo(1);
        assertThat(result.getAttributes().getMap().get(PlatformParametersConsts.CUSTOM_INSTANCETYPE_CPUS)).isEqualTo(1);
        assertThat(result.getSecretAttributes()).isNotNull();
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

        assertThat(result.getStatus()).isEqualTo(ResourceStatus.USER_MANAGED);
        assertThat(result.cloudPlatform()).isEqualTo(source.getCloudPlatform().name());
        assertThat(result.getInstanceType()).isEqualTo(source.getInstanceType());

        assertThat(result.getAttributes()).isNotNull();
        assertThat(result.getAttributes().getMap().get(PlatformParametersConsts.CUSTOM_INSTANCETYPE_MEMORY)).isEqualTo(1);
        assertThat(result.getAttributes().getMap().get(PlatformParametersConsts.CUSTOM_INSTANCETYPE_CPUS)).isEqualTo(1);
        assertThat(result.getSecretAttributes()).isNotNull();
        assertThat(result.getRootVolumeSize().intValue()).isEqualTo(rootVolumeSize);
    }

    @Test
    public void convertWithGcpEncryption() {
        InstanceTemplateV4Request source = getSampleGcpRequest();

        Template result = underTest.convert(source);

        assertGcpEncryptionConvertResult(source, result);
        assertThat(new Json(result.getSecretAttributes()).getMap().get(InstanceTemplate.VOLUME_ENCRYPTION_KEY_ID)).isEqualTo("myKey");
    }

    @Test
    public void convertWithNullTemporaryStorage() {
        InstanceTemplateV4Request source = getSampleGcpRequest();
        source.setTemporaryStorage(null);

        Template result = underTest.convert(source);
        assertThat(result.getTemporaryStorage()).isEqualTo(TemporaryStorage.ATTACHED_VOLUMES);
    }

    @Test
    public void convertWithNonNullTemporaryStorage() {
        InstanceTemplateV4Request source = getSampleGcpRequest();
        source.setTemporaryStorage(TemporaryStorage.EPHEMERAL_VOLUMES);

        Template result = underTest.convert(source);
        assertThat(result.getTemporaryStorage()).isEqualTo(TemporaryStorage.EPHEMERAL_VOLUMES);
    }

    @Test
    public void convertWithGcpEncryptionWithNullKey() {
        InstanceTemplateV4Request source = getSampleGcpRequest();
        source.getGcp().getEncryption().setKey(null);

        Template result = underTest.convert(source);

        assertGcpEncryptionConvertResult(source, result);
        assertThat(new Json(result.getSecretAttributes()).getMap().get(InstanceTemplate.VOLUME_ENCRYPTION_KEY_ID)).isNull();
    }

    private void assertGcpEncryptionConvertResult(InstanceTemplateV4Request source, Template result) {
        assertThat(result.getStatus()).isEqualTo(ResourceStatus.USER_MANAGED);
        assertThat(result.cloudPlatform()).isEqualTo(source.getCloudPlatform().name());
        assertThat(result.getRootVolumeSize()).isEqualTo(source.getRootVolume().getSize());
        assertThat(result.getInstanceType()).isEqualTo(source.getInstanceType());

        assertThat(result.getAttributes()).isNotNull();
        Map<String, Object> map = result.getAttributes().getMap();
        assertThat(map.get("keyEncryptionMethod")).isEqualTo("RAW");
        assertThat(map.get(InstanceTemplate.VOLUME_ENCRYPTION_KEY_TYPE)).isEqualTo(EncryptionType.CUSTOM.name());

        assertThat(result.getSecretAttributes()).isNotNull();
    }

    private InstanceTemplateV4Request getSampleGcpRequest() {
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
        return source;
    }

    @Test
    public void convertWithAwsEncryption() {
        InstanceTemplateV4Request source = getSampleAwsRequest();

        Template result = ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.convert(source));

        assertAwsEncryptionConvertResult(source, result);
        assertThat(new Json(result.getSecretAttributes()).getMap().get(InstanceTemplate.VOLUME_ENCRYPTION_KEY_ID)).isEqualTo("myKey");
    }

    @Test
    public void convertWithAwsEncryptionWithNullKey() {
        InstanceTemplateV4Request source = getSampleAwsRequest();
        source.getAws().getEncryption().setKey(null);

        Template result = ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.convert(source));

        assertAwsEncryptionConvertResult(source, result);
        assertThat(new Json(result.getSecretAttributes()).getMap().get(InstanceTemplate.VOLUME_ENCRYPTION_KEY_ID)).isNull();
    }

    private void assertAwsEncryptionConvertResult(InstanceTemplateV4Request source, Template result) {
        assertThat(result.getStatus()).isEqualTo(ResourceStatus.USER_MANAGED);
        assertThat(result.cloudPlatform()).isEqualTo(source.getCloudPlatform().name());
        assertThat(result.getRootVolumeSize()).isEqualTo(source.getRootVolume().getSize());
        assertThat(result.getInstanceType()).isEqualTo(source.getInstanceType());

        assertThat(result.getAttributes()).isNotNull();
        Map<String, Object> map = result.getAttributes().getMap();
        assertThat(map.get(InstanceTemplate.VOLUME_ENCRYPTION_KEY_TYPE)).isEqualTo(EncryptionType.CUSTOM.name());

        assertThat(result.getSecretAttributes()).isNotNull();
    }

    private InstanceTemplateV4Request getSampleAwsRequest() {
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
        return source;
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