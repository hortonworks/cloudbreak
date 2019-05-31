package com.sequenceiq.cloudbreak.auth.altus;

import com.sequenceiq.cloudbreak.auth.altus.Crn.Service;

public class InternalCrnBuilder {

    private static final String INTERNAL_USER_CRN = "__internal__actor__";

    private static final String INTERNAL_ACCOUNT = "altus";

    private Service serviceType;

    public InternalCrnBuilder(Service serviceType) {
        this.serviceType = serviceType;
    }

    public Crn getInternalCrnForService() {
        return Crn.builder()
                .setService(serviceType)
                .setAccountId(INTERNAL_ACCOUNT)
                .setResourceType(Crn.ResourceType.USER)
                .setResource(INTERNAL_USER_CRN)
                .build();
    }

    public static boolean isInternalCrn(String crn) {
        Crn c = Crn.fromString(crn);
        return INTERNAL_ACCOUNT.equals(c.getAccountId()) && INTERNAL_USER_CRN.equals(c.getResource());

    }

    public String getInternalCrnForServiceAsString() {
        return getInternalCrnForService().toString();
    }
}
