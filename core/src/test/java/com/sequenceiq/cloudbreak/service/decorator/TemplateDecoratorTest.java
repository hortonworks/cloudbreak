package com.sequenceiq.cloudbreak.service.decorator;

import static com.google.common.collect.Maps.newHashMap;
import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static java.util.Collections.singletonMap;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmTypes;
import com.sequenceiq.cloudbreak.cloud.model.ExtendedCloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.PlatformDisks;
import com.sequenceiq.cloudbreak.cloud.model.VmType;
import com.sequenceiq.cloudbreak.cloud.model.VmTypeMeta;
import com.sequenceiq.cloudbreak.cloud.model.VolumeParameterConfig;
import com.sequenceiq.cloudbreak.cloud.model.VolumeParameterType;
import com.sequenceiq.cloudbreak.cloud.service.CloudParameterService;
import com.sequenceiq.cloudbreak.controller.validation.LocationService;
import com.sequenceiq.cloudbreak.converter.spi.CredentialToExtendedCloudCredentialConverter;
import com.sequenceiq.cloudbreak.domain.Template;
import com.sequenceiq.cloudbreak.domain.VolumeTemplate;
import com.sequenceiq.cloudbreak.dto.credential.Credential;
import com.sequenceiq.cloudbreak.service.stack.DefaultRootVolumeSizeProvider;
import com.sequenceiq.common.api.type.CdpResourceType;

@ExtendWith(MockitoExtension.class)
public class TemplateDecoratorTest {

    private static final String REGION = "region";

    private static final String AVAILABILITY_ZONE = "availabilityZone";

    private static final String VARIANT = "variant";

    private static final String VM_TYPE = "vmType";

    private static final String PLATFORM_1 = "platform";

    private static final String VOLUME_TYPE = "volumeType";

    @InjectMocks
    private TemplateDecorator underTest;

    @Mock
    private CloudParameterService cloudParameterService;

    @Mock
    private LocationService locationService;

    @Mock
    private DefaultRootVolumeSizeProvider defaultRootVolumeSizeProvider;

    @Mock
    private CredentialToExtendedCloudCredentialConverter extendedCloudCredentialConverter;

    @Mock
    private Credential cloudCredential;

    @Mock
    private ExtendedCloudCredential extendedCloudCredential;

    @Test
    public void testDecorator() {
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

        when(extendedCloudCredentialConverter.convert(cloudCredential)).thenReturn(extendedCloudCredential);
        when(cloudParameterService.getDiskTypes()).thenReturn(platformDisk);
        when(cloudParameterService.getVmTypesV2(eq(extendedCloudCredential), eq(REGION), eq(VARIANT),
                eq(CdpResourceType.DATAHUB), anyMap())).thenReturn(cloudVmTypes);
        when(locationService.location(REGION, AVAILABILITY_ZONE)).thenReturn(REGION);

        Template actual = underTest.decorate(cloudCredential, template, REGION, AVAILABILITY_ZONE, VARIANT, CdpResourceType.DATAHUB);

        VolumeTemplate next = actual.getVolumeTemplates().iterator().next();
        assertEquals(maximumNumber, next.getVolumeCount().longValue());
        assertEquals(maximumSize, next.getVolumeSize().longValue());
        verify(cloudParameterService, times(1)).getVmTypesV2(any(), any(), any(), any(), any());
    }

    @Test
    public void testDecoratorWhenNoLocation() {
        Template template = initTemplate();
        VolumeTemplate volumeTemplate = new VolumeTemplate();
        volumeTemplate.setVolumeType(VOLUME_TYPE);
        template.setVolumeTemplates(Sets.newHashSet(volumeTemplate));
        template.setCloudPlatform(PLATFORM_1);

        CloudVmTypes cloudVmTypes = new CloudVmTypes();
        Map<String, Set<VmType>> region1 = singletonMap(REGION, emptySet());
        cloudVmTypes.setCloudVmResponses(region1);

        when(locationService.location(REGION, AVAILABILITY_ZONE)).thenReturn("anylocation");
        when(extendedCloudCredentialConverter.convert(cloudCredential)).thenReturn(extendedCloudCredential);
        when(cloudParameterService.getVmTypesV2(eq(extendedCloudCredential), eq(REGION), eq(VARIANT),
                eq(CdpResourceType.DATAHUB), anyMap())).thenReturn(cloudVmTypes);
        Map<String, VolumeParameterType> map = Map.of(VOLUME_TYPE, VolumeParameterType.MAGNETIC);
        Platform platform = Platform.platform(PLATFORM_1);
        when(cloudParameterService.getDiskTypes()).thenReturn(new PlatformDisks(emptyMap(), emptyMap(), Map.of(platform, map), emptyMap()));

        underTest.decorate(cloudCredential, template, REGION, AVAILABILITY_ZONE, VARIANT, CdpResourceType.DATAHUB);

        assertNull(volumeTemplate.getVolumeSize());
        assertNull(volumeTemplate.getVolumeCount());
        verify(cloudParameterService, times(1)).getVmTypesV2(any(), any(), any(), any(), any());
    }

    @Test
    public void testDecoratorWhenNoInstanceType() {
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

        when(extendedCloudCredentialConverter.convert(cloudCredential)).thenReturn(extendedCloudCredential);
        when(cloudParameterService.getDiskTypes()).thenReturn(platformDisk);
        when(cloudParameterService.getVmTypesV2(eq(extendedCloudCredential), eq(REGION), eq(VARIANT),
                eq(CdpResourceType.DATAHUB), anyMap())).thenReturn(cloudVmTypes);
        when(locationService.location(REGION, AVAILABILITY_ZONE)).thenReturn(REGION);

        Template actual = underTest.decorate(cloudCredential, template, REGION, AVAILABILITY_ZONE, VARIANT, CdpResourceType.DATAHUB);

        VolumeTemplate next = actual.getVolumeTemplates().iterator().next();
        assertNull(next.getVolumeCount());
        assertNull(next.getVolumeSize());
        verify(cloudParameterService, times(1)).getVmTypesV2(any(), any(), any(), any(), any());
    }

    @Test
    public void testDecorateWithRootVolumeSizeInRequestAndDefaultIsSet() {
        Template template = initTemplate();

        template.setRootVolumeSize(70);

        Template actual = underTest.decorate(cloudCredential, template, REGION, AVAILABILITY_ZONE, VARIANT, CdpResourceType.DATAHUB);
        assertEquals(70L, (long) actual.getRootVolumeSize());
        verify(cloudParameterService, never()).getVmTypesV2(any(), any(), any(), any(), any());
    }

    @Test
    public void testDecorateWithoutRootVolumeSizeInRequestAndDefaultIsSet() {
        Template template = initTemplate();

        when(defaultRootVolumeSizeProvider.getForPlatform(any())).thenReturn(100);

        Template actual = underTest.decorate(cloudCredential, template, REGION, AVAILABILITY_ZONE, VARIANT, CdpResourceType.DATAHUB);
        assertEquals(100L, (long) actual.getRootVolumeSize());
        verify(cloudParameterService, never()).getVmTypesV2(any(), any(), any(), any(), any());
    }

    @Test
    public void testDecorateWhenVolumeCountAndSizeSet() {
        Template template = initTemplate();
        VolumeTemplate volumeTemplate = new VolumeTemplate();
        volumeTemplate.setVolumeType(VOLUME_TYPE);
        volumeTemplate.setVolumeSize(1);
        volumeTemplate.setVolumeCount(1);
        template.setVolumeTemplates(Sets.newHashSet(volumeTemplate));
        when(defaultRootVolumeSizeProvider.getForPlatform(any())).thenReturn(100);

        Template actual = underTest.decorate(cloudCredential, template, REGION, AVAILABILITY_ZONE, VARIANT, CdpResourceType.DATAHUB);
        assertEquals(1, actual.getVolumeTemplates().iterator().next().getVolumeSize());
        assertEquals(1, actual.getVolumeTemplates().iterator().next().getVolumeCount());
        verify(cloudParameterService, never()).getVmTypesV2(any(), any(), any(), any(), any());
    }

    private Template initTemplate() {
        Template template = new Template();
        template.setVolumeTemplates(Sets.newHashSet());
        return template;
    }
}
