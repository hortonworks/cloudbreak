package com.sequenceiq.cloudbreak.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.cloud.gcp.GcpPlatformParameters;
import com.sequenceiq.cloudbreak.cloud.model.CloudVolumeUsageType;
import com.sequenceiq.cloudbreak.cloud.model.VolumeSetAttributes;
import com.sequenceiq.cloudbreak.domain.VolumeTemplate;
import com.sequenceiq.common.model.AwsDiskType;

public class EphemeralVolumeUtilTest {

    @Test
    public void testGetEphemeralVolumeValuesMustReturnTwoLengthSet() {
        assertEquals(2, EphemeralVolumeUtil.getEphemeralVolumeValues().size());
    }

    @Test
    public void testGetEphemeralVolumeWhichMustBeProvisionedMustReturnOneLengthSet() {
        assertEquals(1, EphemeralVolumeUtil.getEphemeralVolumeWhichMustBeProvisioned().size());
    }

    @Test
    public void testVolumeIsEphemeralWhenGp2MustFalse() {
        VolumeTemplate volumeTemplate = new VolumeTemplate();
        volumeTemplate.setVolumeType(AwsDiskType.Gp2.value());

        assertFalse(EphemeralVolumeUtil.volumeIsEphemeral(volumeTemplate));
    }

    @Test
    public void testVolumeIsEphemeralWhenLocalSsdMustTrue() {
        VolumeTemplate volumeTemplate = new VolumeTemplate();
        volumeTemplate.setVolumeType(GcpPlatformParameters.GcpDiskType.LOCAL_SSD.value());

        assertTrue(EphemeralVolumeUtil.volumeIsEphemeral(volumeTemplate));
    }

    @Test
    public void testVolumeIsEphemeralWhichMustBeProvisionedWithVolumeTemplateWhenGp2MustFalse() {
        VolumeTemplate volumeTemplate = new VolumeTemplate();
        volumeTemplate.setVolumeType(AwsDiskType.Gp2.value());

        assertFalse(EphemeralVolumeUtil.volumeIsEphemeralWhichMustBeProvisioned(volumeTemplate));
    }

    @Test
    public void testVolumeIsEphemeralWhichMustBeProvisionedWithVolumeTemplateWhenLocalSsdMustTrue() {
        VolumeTemplate volumeTemplate = new VolumeTemplate();
        volumeTemplate.setVolumeType(GcpPlatformParameters.GcpDiskType.LOCAL_SSD.value());

        assertTrue(EphemeralVolumeUtil.volumeIsEphemeralWhichMustBeProvisioned(volumeTemplate));
    }

    @Test
    public void testVolumeIsEphemeralWhichMustBeProvisionedWithVolumeWhenGp2MustFalse() {
        VolumeSetAttributes.Volume volumeTemplate = new VolumeSetAttributes.Volume(
                "1.0",
                "1.0",
                1,
                AwsDiskType.Gp2.value(),
                CloudVolumeUsageType.GENERAL
        );

        assertFalse(EphemeralVolumeUtil.volumeIsEphemeralWhichMustBeProvisioned(volumeTemplate));
    }

    @Test
    public void testVolumeIsEphemeralWhichMustBeProvisionedWithVolumeWhenLocalSsdMustTrue() {
        VolumeSetAttributes.Volume volumeTemplate = new VolumeSetAttributes.Volume(
                "1.0",
                "1.0",
                1,
                GcpPlatformParameters.GcpDiskType.LOCAL_SSD.value(),
                CloudVolumeUsageType.GENERAL
        );

        assertTrue(EphemeralVolumeUtil.volumeIsEphemeralWhichMustBeProvisioned(volumeTemplate));
    }

    @Test
    public void testVolumeIsEphemeralWhichMustBeProvisionedWhenGp2MustFalse() {
        assertFalse(EphemeralVolumeUtil.volumeIsEphemeralWhichMustBeProvisioned(AwsDiskType.Gp2.value()));
    }

    @Test
    public void testVolumeIsEphemeralWhichMustBeProvisionedWhenLocalSsdMustTrue() {
        assertTrue(EphemeralVolumeUtil.volumeIsEphemeralWhichMustBeProvisioned(GcpPlatformParameters.GcpDiskType.LOCAL_SSD.value()));
    }

}