package com.sequenceiq.cloudbreak.auth.crn;

import static java.lang.String.format;
import static org.apache.commons.codec.digest.DigestUtils.md5Hex;

public class CrnEncoder {

    private static final String AUTOSCALE_WORKLOAD_USERNAME_PREFIX = "srv_as%s";

    private static final String AUTOSCALE_MACHINE_USER_PREFIX = "as%s";

    private static final Integer WORKLOAD_USERNAME_CHAR_LIMIT = 30;

    private static final Integer MACHINE_USER_NAME_CHAR_LIMIT = 26;

    private CrnEncoder() {
    }

    public static String generateMd5EncodedAutoscaleMachineUser(String environmentCrn, boolean workloadUser) {
        String encodedResource = md5Hex(Crn.safeFromString(environmentCrn).getResource());
        return workloadUser
                ? format(AUTOSCALE_WORKLOAD_USERNAME_PREFIX, encodedResource).substring(0, WORKLOAD_USERNAME_CHAR_LIMIT)
                : format(AUTOSCALE_MACHINE_USER_PREFIX, encodedResource).substring(0, MACHINE_USER_NAME_CHAR_LIMIT);
    }

}
