package com.sequenceiq.cloudbreak.auth.altus;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import com.google.common.base.Strings;

/**
 * Class of utility methods for dealing with CRNs.
 */
public class Crn {

    private Crn() {
    }

    /**
     * Parse the account ID out of a CRN.
     *
     * @param crn the CRN
     * @return the account ID
     */
    //CHECKSTYLE:OFF
    public static String getAccountId(String crn) {
        checkNotNull(crn);
        String[] parts = crn.split(":", 6);
        checkState(parts.length > 4);
        checkState(!Strings.isNullOrEmpty(parts[4]));
        return parts[4];
    }
    //CHECKSTYLE:ON
}
