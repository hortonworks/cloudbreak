package com.sequenceiq.cloudbreak.ccm.key;

import com.sequenceiq.cloudbreak.auth.altus.Crn;

/**
 * Provides utilities for dealing with resources.
 */
public class CcmResourceUtil {

    /**
     * Private constructor to prevent instantiation.
     */
    private CcmResourceUtil() {
    }

    /**
     * Returns the CCM SSH tunneling key ID for the specified resource CRN.
     *
     * @param resourceCrn the resource CRN
     * @return the CCM SSH tunneling key ID for the specified resource CRN
     */
    public static String getKeyId(String resourceCrn) {
        return Crn.safeFromString(resourceCrn).getResource();
    }
}
