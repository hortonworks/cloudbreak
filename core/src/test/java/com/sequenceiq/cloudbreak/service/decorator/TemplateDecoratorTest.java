package com.sequenceiq.cloudbreak.service.decorator;

import static com.google.common.collect.Maps.newHashMap;
import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static java.util.Collections.singletonMap;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmTypes;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.PlatformDisks;
import com.sequenceiq.cloudbreak.cloud.model.VmType;
import com.sequenceiq.cloudbreak.cloud.model.VmTypeMeta;
import com.sequenceiq.cloudbreak.cloud.model.VolumeParameterConfig;
import com.sequenceiq.cloudbreak.cloud.model.VolumeParameterType;
import com.sequenceiq.cloudbreak.controller.validation.LocationService;
import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.domain.Template;
import com.sequenceiq.cloudbreak.domain.VolumeTemplate;
import com.sequenceiq.cloudbreak.service.stack.CloudParameterService;
import com.sequenceiq.cloudbreak.service.stack.DefaultRootVolumeSizeProvider;

@RunWith(MockitoJUnitRunner.class)
public class TemplateDecoratorTest {

    private static final String REGION = "region";

    private static final String AVAILABILITY_ZONE = "availabilityZone";

    private static final String VARIANT = "variant";

    private static final String VM_TYPE = "vmType";

    private static final String PLATFORM_1 = "platform";

    private static final String VOLUME_TYPE = "volumeType";

    @InjectMocks
    private final TemplateDecorator underTest = new TemplateDecorator();

    @Mock
    private CloudParameterService cloudParameterService;

    @Mock
    private LocationService locationService;

    @Mock
    private DefaultRootVolumeSizeProvider defaultRootVolumeSizeProvider;

    private Credential cloudCredential;

    @Test
    public void testDecorator() {
        cloudCredential = mock(Credential.class);

        Template template = new Template();
        template.setInstanceType(VM_TYPE);
        template.setCloudPlatform(PLATFORM_1);
        VolumeTemplate volumeTemplate = new VolumeTemplate();
        volumeTemplate.setVolumeType(VOLUME_TYPE);
        template.setVolumeTemplates(Sets.newHashSet(volumeTemplate));

        Platform platform = Platform.platform(PLATFORM_1);
        int minimumSize = 10;
        int maximumSize = 100;
        int minimumNumber = 1;
        int maximumNumber = 5;
        VolumeParameterConfig config = new VolumeParameterConfig(VolumeParameterType.MAGNETIC, minimumSize, maximumSize, minimumNumber, maximumNumber);

        VmTypeMeta vmTypeMeta = new VmTypeMeta();
        vmTypeMeta.setMagneticConfig(config);

        Map<Platform, Map<String, VolumeParameterType>> diskMappings = newHashMap();
        diskMappings.put(platform, singletonMap(VOLUME_TYPE, VolumeParameterType.MAGNETIC));

        PlatformDisks platformDisk = new PlatformDisks(emptyMap(), emptyMap(), diskMappings, emptyMap());

        CloudVmTypes cloudVmTypes = new CloudVmTypes();
        Map<String, Set<VmType>> region1 = singletonMap(REGION, Collections.singleton(VmType.vmTypeWithMeta(VM_TYPE, vmTypeMeta, true)));
        cloudVmTypes.setCloudVmResponses(region1);

        when(cloudParameterService.getDiskTypes()).thenReturn(platformDisk);
        when(cloudParameterService.getVmTypesV2(eq(cloudCredential), eq(REGION), eq(VARIANT), anyMap())).thenReturn(cloudVmTypes);
        when(locationService.location(REGION, AVAILABILITY_ZONE)).thenReturn(REGION);

        Template actual = underTest.decorate(cloudCredential, template, REGION, AVAILABILITY_ZONE, VARIANT);

        VolumeTemplate next = actual.getVolumeTemplates().iterator().next();
        assertEquals(maximumNumber, next.getVolumeCount().longValue());
        assertEquals(maximumSize, next.getVolumeSize().longValue());
    }

    @Test
    public void testDecoratorWhenNoLocation() {
        Template template = initTemplate();

        Template actual = underTest.decorate(cloudCredential, template, REGION, AVAILABILITY_ZONE, VARIANT);

        Assert.assertTrue(actual.getVolumeTemplates().isEmpty());
    }

    @Test
    public void testDecoratorWhenNoInstanceType() {

        cloudCredential = mock(Credential.class);

        Template template = new Template();
        template.setInstanceType("missingVmType");
        template.setCloudPlatform(PLATFORM_1);
        VolumeTemplate volumeTemplate = new VolumeTemplate();
        volumeTemplate.setVolumeType(VOLUME_TYPE);
        template.setVolumeTemplates(Sets.newHashSet(volumeTemplate));

        Platform platform = Platform.platform(PLATFORM_1);
        int minimumSize = 10;
        int maximumSize = 100;
        int minimumNumber = 1;
        int maximumNumber = 5;
        VolumeParameterConfig config = new VolumeParameterConfig(VolumeParameterType.MAGNETIC, minimumSize, maximumSize, minimumNumber, maximumNumber);

        VmTypeMeta vmTypeMeta = new VmTypeMeta();
        vmTypeMeta.setMagneticConfig(config);

        Map<Platform, Map<String, VolumeParameterType>> diskMappings = newHashMap();
        diskMappings.put(platform, singletonMap(VOLUME_TYPE, VolumeParameterType.MAGNETIC));

        PlatformDisks platformDisk = new PlatformDisks(emptyMap(), emptyMap(), diskMappings, emptyMap());

        CloudVmTypes cloudVmTypes = new CloudVmTypes();
        Map<String, Set<VmType>> region1 = singletonMap(REGION, Collections.singleton(VmType.vmTypeWithMeta(VM_TYPE, vmTypeMeta, true)));
        cloudVmTypes.setCloudVmResponses(region1);

        when(cloudParameterService.getDiskTypes()).thenReturn(platformDisk);
        when(cloudParameterService.getVmTypesV2(eq(cloudCredential), eq(REGION), eq(VARIANT), anyMap())).thenReturn(cloudVmTypes);
        when(locationService.location(REGION, AVAILABILITY_ZONE)).thenReturn(REGION);

        Template actual = underTest.decorate(cloudCredential, template, REGION, AVAILABILITY_ZONE, VARIANT);

        VolumeTemplate next = actual.getVolumeTemplates().iterator().next();
        Assert.assertNull(next.getVolumeCount());
        Assert.assertNull(next.getVolumeSize());
    }

    @Test
    public void testConvertWithRootVolumeSizeInRequestAndDefaultIsSet() {
        Template template = initTemplate();

        template.setRootVolumeSize(70);

        Template actual = underTest.decorate(cloudCredential, template, REGION, AVAILABILITY_ZONE, VARIANT);
        assertEquals(70L, (long) actual.getRootVolumeSize());
    }

    @Test
    public void testConvertWithoutRootVolumeSizeInRequestAndDefaultIsSet() {
        Template template = initTemplate();

        when(defaultRootVolumeSizeProvider.getForPlatform(any())).thenReturn(100);

        Template actual = underTest.decorate(cloudCredential, template, REGION, AVAILABILITY_ZONE, VARIANT);
        assertEquals(100L, (long) actual.getRootVolumeSize());
    }

    private Template initTemplate() {
        Template template = new Template();
        template.setVolumeTemplates(Sets.newHashSet());
        cloudCredential = mock(Credential.class);

        CloudVmTypes cloudVmTypes = new CloudVmTypes(singletonMap(REGION, emptySet()), emptyMap());
        when(cloudParameterService.getVmTypesV2(eq(cloudCredential), eq(REGION), eq(VARIANT), anyMap())).thenReturn(cloudVmTypes);
        when(locationService.location(REGION, AVAILABILITY_ZONE)).thenReturn(null);
        return template;
    }
}
