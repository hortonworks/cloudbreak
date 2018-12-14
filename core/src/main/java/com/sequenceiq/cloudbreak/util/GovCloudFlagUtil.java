package com.sequenceiq.cloudbreak.util;

public class GovCloudFlagUtil {

    public static final String GOV_CLOUD_KEY = "govCloud";

    private GovCloudFlagUtil() {

    }

    public static Boolean extractGovCloudFlag(Object object) {
        if (object != null) {
            if (object instanceof Boolean) {
                return (Boolean) object;
            } else if (object instanceof String) {
                return Boolean.parseBoolean((String) object);
            }
        }
        return Boolean.FALSE;
    }
}
