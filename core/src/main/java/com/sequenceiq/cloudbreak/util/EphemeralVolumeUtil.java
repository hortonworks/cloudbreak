package com.sequenceiq.cloudbreak.util;

import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.CLOUDERA_STACK_VERSION_7_3_1;
import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.isVersionNewerOrEqualThanLimited;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.cloud.gcp.GcpPlatformParameters.GcpDiskType;
import com.sequenceiq.cloudbreak.cloud.model.VolumeSetAttributes;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.VolumeTemplate;
import com.sequenceiq.cloudbreak.view.StackView;
import com.sequenceiq.common.model.AwsDiskType;

public class EphemeralVolumeUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(EphemeralVolumeUtil.class);

    private EphemeralVolumeUtil() {
    }

    public static Set<String> getEphemeralVolumeValues() {
        return Set.of(
                AwsDiskType.Ephemeral.value(),
                GcpDiskType.LOCAL_SSD.value()
        );
    }

    public static Set<String> getEphemeralVolumeWhichMustBeProvisioned() {
        return Set.of(
                GcpDiskType.LOCAL_SSD.value()
        );
    }

    public static boolean volumeIsEphemeral(VolumeTemplate volumeTemplate) {
        return getEphemeralVolumeValues()
                .stream()
                .anyMatch(e -> e.equalsIgnoreCase(volumeTemplate.getVolumeType()));
    }

    public static boolean volumeIsEphemeralWhichMustBeProvisioned(VolumeSetAttributes.Volume volume) {
        return volumeIsEphemeralWhichMustBeProvisioned(volume.getType());
    }

    public static boolean volumeIsEphemeralWhichMustBeProvisioned(VolumeTemplate volume) {
        return volumeIsEphemeralWhichMustBeProvisioned(volume.getVolumeType());
    }

    public static boolean volumeIsEphemeralWhichMustBeProvisioned(String type) {
        return getEphemeralVolumeWhichMustBeProvisioned()
                .stream()
                .anyMatch(e -> e.equalsIgnoreCase(type));
    }

    public static boolean xfsForEphemeralSupported(boolean entitled, StackView stack) {
        return entitled &&
                StackType.WORKLOAD.equals(stack.getType()) &&
                CloudPlatform.AWS.equals(CloudPlatform.fromName(stack.getCloudPlatform())) &&
                CodUtil.isCodCluster(stack) &&
                isVersionNewerOrEqualThanLimited(stack.getStackVersion(), CLOUDERA_STACK_VERSION_7_3_1);
    }
}
