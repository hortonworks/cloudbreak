package com.sequenceiq.cloudbreak.auth.crn;

import com.sequenceiq.cloudbreak.auth.CrnUser;

public class RegionAwareInternalCrnGeneratorUtil {

    public static final String INTERNAL_ACCOUNT = "altus";

    public static final String INTERNAL_USER_CRN = "__internal__actor__";

    private RegionAwareInternalCrnGeneratorUtil() {

    }

    public static boolean isInternalCrn(String crn) {
        Crn c = Crn.fromString(crn);
        return INTERNAL_USER_CRN.equals(c.getResource());
    }

    public static boolean isInternalCrn(Crn crn) {
        return INTERNAL_USER_CRN.equals(crn.getResource());
    }

    public static CrnUser createInternalCrnUser(Crn crn)  {
        String service = crn.getService().toString().toUpperCase();
        String role = "AUTOSCALE".equals(service) ? "ROLE_AUTOSCALE" : "ROLE_INTERNAL";
        return new CrnUser(crn.getResource(),
                crn.toString(),
                crn.getResourceType().toString(),
                crn.getResourceType().toString(),
                crn.getAccountId(),
                role);
    }
}
