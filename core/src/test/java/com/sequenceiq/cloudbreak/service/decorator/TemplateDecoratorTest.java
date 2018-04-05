package com.sequenceiq.cloudbreak.service.decorator;

import static com.google.common.collect.Maps.newHashMap;
import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static java.util.Collections.singletonMap;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Matchers.eq;
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
import com.sequenceiq.cloudbreak.service.stack.CloudParameterService;

@RunWith(MockitoJUnitRunner.class)
public class TemplateDecoratorTest {

    @InjectMocks
    private TemplateDecorator underTest = new TemplateDecorator();

    @Mock
    private CloudParameterService cloudParameterService;

    @Mock
    private LocationService locationService;

    @Test
    public void testDecorator() {

        String vmType = "vmType";
        String platform1 = "platform";
        String volumeType = "volumeType";
        String region = "region";
        String availibilityZone = "availabilityZone";
        String variant = "variant";

        Credential cloudCredential = mock(Credential.class);

        Template template = new Template();
        template.setInstanceType(vmType);
        template.setCloudPlatform(platform1);
        template.setVolumeType(volumeType);

        Platform platform = Platform.platform(platform1);
        int minimumSize = 10;
        int maximumSize = 100;
        int minimumNumber = 1;
        int maximumNumber = 5;
        VolumeParameterConfig config = new VolumeParameterConfig(VolumeParameterType.MAGNETIC, minimumSize, maximumSize, minimumNumber, maximumNumber);

        VmTypeMeta vmTypeMeta = new VmTypeMeta();
        vmTypeMeta.setMagneticConfig(config);

        Map<Platform, Map<String, VolumeParameterType>> diskMappings = newHashMap();
        diskMappings.put(platform, singletonMap(volumeType, VolumeParameterType.MAGNETIC));

        PlatformDisks platformDisk = new PlatformDisks(emptyMap(), emptyMap(), diskMappings, emptyMap());

        CloudVmTypes cloudVmTypes = new CloudVmTypes();
        Map<String, Set<VmType>> region1 = singletonMap(region, Collections.singleton(VmType.vmTypeWithMeta(vmType, vmTypeMeta, true)));
        cloudVmTypes.setCloudVmResponses(region1);

        when(cloudParameterService.getDiskTypes()).thenReturn(platformDisk);
        when(cloudParameterService.getVmTypesV2(eq(cloudCredential), eq(region), eq(variant), anyMap())).thenReturn(cloudVmTypes);
        when(locationService.location(region, availibilityZone)).thenReturn(region);

        Template actual = underTest.decorate(cloudCredential, template, region, availibilityZone, variant);

        Assert.assertEquals(maximumNumber, actual.getVolumeCount().longValue());
        Assert.assertEquals(maximumSize, actual.getVolumeSize().longValue());
    }

    @Test
    public void testDecoratorWhenNoLocation() {
        String region = "region";
        String availibilityZone = "availabilityZone";
        String variant = "variant";
        String vmType = "vmType";

        Template template = new Template();
        Credential cloudCredential = mock(Credential.class);

        CloudVmTypes cloudVmTypes = new CloudVmTypes(singletonMap(region, emptySet()), emptyMap());

        when(cloudParameterService.getVmTypesV2(eq(cloudCredential), eq(region), eq(variant), anyMap())).thenReturn(cloudVmTypes);
        when(locationService.location(region, availibilityZone)).thenReturn(null);

        Template actual = underTest.decorate(cloudCredential, template, region, availibilityZone, variant);

        Assert.assertNull(actual.getVolumeCount());
        Assert.assertNull(actual.getVolumeSize());
    }

    @Test
    public void testDecoratorWhenNoInstanceType() {
        String vmType = "vmType";
        String platform1 = "platform";
        String volumeType = "volumeType";
        String region = "region";
        String availibilityZone = "availabilityZone";
        String variant = "variant";

        Credential cloudCredential = mock(Credential.class);

        Template template = new Template();
        template.setInstanceType("missingVmType");
        template.setCloudPlatform(platform1);
        template.setVolumeType(volumeType);

        Platform platform = Platform.platform(platform1);
        int minimumSize = 10;
        int maximumSize = 100;
        int minimumNumber = 1;
        int maximumNumber = 5;
        VolumeParameterConfig config = new VolumeParameterConfig(VolumeParameterType.MAGNETIC, minimumSize, maximumSize, minimumNumber, maximumNumber);

        VmTypeMeta vmTypeMeta = new VmTypeMeta();
        vmTypeMeta.setMagneticConfig(config);

        Map<Platform, Map<String, VolumeParameterType>> diskMappings = newHashMap();
        diskMappings.put(platform, singletonMap(volumeType, VolumeParameterType.MAGNETIC));

        PlatformDisks platformDisk = new PlatformDisks(emptyMap(), emptyMap(), diskMappings, emptyMap());

        CloudVmTypes cloudVmTypes = new CloudVmTypes();
        Map<String, Set<VmType>> region1 = singletonMap(region, Collections.singleton(VmType.vmTypeWithMeta(vmType, vmTypeMeta, true)));
        cloudVmTypes.setCloudVmResponses(region1);

        when(cloudParameterService.getDiskTypes()).thenReturn(platformDisk);
        when(cloudParameterService.getVmTypesV2(eq(cloudCredential), eq(region), eq(variant), anyMap())).thenReturn(cloudVmTypes);
        when(locationService.location(region, availibilityZone)).thenReturn(region);

        Template actual = underTest.decorate(cloudCredential, template, region, availibilityZone, variant);

        Assert.assertNull(actual.getVolumeCount());
        Assert.assertNull(actual.getVolumeSize());
    }
}
