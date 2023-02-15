package com.sequenceiq.cloudbreak.cloud.aws.common.resource.volume;

import static com.sequenceiq.common.model.AwsDiskType.Gp3;

import org.springframework.stereotype.Component;

/**
 * This class calculates the throughput values for GP3 volumes to be equivalent to the throughput values for GP2 volumes in the same size.
 *
 * <p>
 * Calcualtion is based on the followings: https://aws.amazon.com/blogs/storage/migrate-your-amazon-ebs-volumes-from-gp2-to-gp3-and-save-up-to-20-on-costs/
 */
@Component
public class AwsVolumeThroughputCalculator {

    private static final int MIN_GP3_THROUGHPUT = 125;

    private static final int GP3_SIZE_500GB = 500;

    private static final int GP3_THROUGHPUT_OVER_500GB = 250;

    public Integer getThroughput(String type, int size) {
        if (!Gp3.value().equalsIgnoreCase(type)) {
            return null;
        }
        int throughput = MIN_GP3_THROUGHPUT;
        if (size >= GP3_SIZE_500GB) {
            throughput = GP3_THROUGHPUT_OVER_500GB;
        }
        return throughput;
    }
}
