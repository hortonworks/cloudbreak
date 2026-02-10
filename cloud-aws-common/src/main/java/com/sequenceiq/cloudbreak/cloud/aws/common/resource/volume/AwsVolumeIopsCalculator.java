package com.sequenceiq.cloudbreak.cloud.aws.common.resource.volume;

import static com.sequenceiq.common.model.AwsDiskType.Gp3;

import org.springframework.stereotype.Component;

/**
 * This class calculates the IOPS values for GP3 volumes to be equivalent to the IOPS values for GP2 volumes in the same size.
 *
 * <p>
 * Calcualtion is based on the followings: https://aws.amazon.com/blogs/storage/migrate-your-amazon-ebs-volumes-from-gp2-to-gp3-and-save-up-to-20-on-costs/
 */
@Component
public class AwsVolumeIopsCalculator {

    private static final int MIN_GP3_IOPS = 3000;

    private static final int MAX_GP3_IOPS = 16000;

    private static final int GP2_IOPS_PER_GB = 3;

    private static final int GP2_MIN_IOPS = 100;

    private static final int GP2_MAX_IOPS = 16000;

    // It is a magic constant from the AWS documentation
    private static final int IOPS_MULTIPLIER = 3;

    public Integer getIops(String type, Integer size) {
        if (!Gp3.value().equalsIgnoreCase(type)) {
            return null;
        }
        int iops = IOPS_MULTIPLIER * size;
        // IOPS must be between MIN_GP3_IOPS and MAX_GP3_IOPS
        return Math.max(MIN_GP3_IOPS, Math.min(MAX_GP3_IOPS, iops));
    }

    /**
     * Calculates the equivalent IOPS for a GP3 volume based on the GP2 volume size.
     *
     * @param volumeSizeGb the size of the volume in GB
     * @return the calculated IOPS value to use for the GP3 volume
     */
    public int getEquivalentGp3IopsforGp2Volume(int volumeSizeGb) {
        // Calculate GP2 IOPS: 3 IOPS per GB
        int gp2Iops = volumeSizeGb * GP2_IOPS_PER_GB;

        // Apply GP2 constraints (min 100, max 16,000)
        int effectiveGp2Iops = Math.max(GP2_MIN_IOPS, Math.min(gp2Iops, GP2_MAX_IOPS));

        // Use at least GP3 baseline (3000) or the gp2 equivalent, whichever is higher
        return Math.max(MIN_GP3_IOPS, effectiveGp2Iops);
    }

}
