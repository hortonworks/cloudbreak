package com.sequenceiq.cloudbreak.auth.altus;

import static com.google.common.base.Preconditions.checkNotNull;

import com.sequenceiq.cloudbreak.auth.CrnUser;
import com.sequenceiq.cloudbreak.auth.altus.Crn.Service;

/**
 * Builder for constructing the user CRN for an internal actor.
 *
 * <p>
 * <strong>Note:</strong> The current Altus implementation ({@code services/libs/protocols/src/main/java/com/cloudera/thunderhead/service/common/crn/Crns.java},
 * lines 718--724) recognizes the CRN formulated here as an internal actor <em>only if</em> the service in the constructor {@link #InternalCrnBuilder(Service)}
 * is given as {@link Service#IAM}. So be sure to use that service type for all calls targeting Altus / CDP-CP components like IAM or any service depending
 * on this check (e.g. IDBMMS). Calls taking place among CB services may, of course, specify any {@link Service} constant as they see fit.
 * </p>
 */
public class InternalCrnBuilder {

    public static final String INTERNAL_ACCOUNT = "altus";

    private static final String INTERNAL_USER_CRN = "__internal__actor__";

    private final Service serviceType;

    /**
     * Creates a new {@code InternalCrnBuilder} instance using the given {@code serviceType}. Please read the class javadoc for the implications of how this
     * parameter is interpreted.
     *
     * @param serviceType service type to base the CRN on; must not be {@code null}
     * @throws NullPointerException if {@code serviceType == null}
     */
    public InternalCrnBuilder(Service serviceType) {
        checkNotNull(serviceType);
        this.serviceType = serviceType;
    }

    public static boolean isInternalCrn(String crn) {
        Crn c = Crn.fromString(crn);
        return INTERNAL_USER_CRN.equals(c.getResource());
    }

    public static boolean isInternalCrn(Crn crn) {
        return INTERNAL_USER_CRN.equals(crn.getResource());
    }

    public String getInternalCrnForServiceAsString() {
        // Not sure if region and partition do matter or not in case of  internal actor
        return getInternalCrnForService(Crn.Partition.CDP, Crn.Region.US_WEST_1).toString();
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

    private Crn getInternalCrnForService(Crn.Partition partition, Crn.Region region) {
        return Crn.builder()
                .setPartition(partition)
                .setRegion(region)
                .setService(serviceType)
                .setAccountId(INTERNAL_ACCOUNT)
                .setResourceType(Crn.ResourceType.USER)
                .setResource(INTERNAL_USER_CRN)
                .build();
    }

}
