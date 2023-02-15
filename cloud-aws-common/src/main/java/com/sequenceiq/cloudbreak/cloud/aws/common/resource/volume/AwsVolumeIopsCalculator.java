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
}
