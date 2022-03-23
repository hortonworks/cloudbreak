package com.sequenceiq.cloudbreak.auth.crn;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorUtil.INTERNAL_ACCOUNT;
import static com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorUtil.INTERNAL_USER_CRN;
import static com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorUtil.isInternalCrn;

import com.sequenceiq.cloudbreak.auth.crn.Crn.Service;

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
public class RegionAwareInternalCrnGenerator {

    private final Service serviceType;

    private final String partition;

    private final String region;

    /**
     * Creates a new {@code InternalCrnBuilder} instance using the given {@code serviceType}. Please read the class javadoc for the implications of how this
     * parameter is interpreted.
     *
     * @param serviceType service type to base the CRN on; must not be {@code null}
     * @throws NullPointerException if {@code serviceType == null}
     */
    private RegionAwareInternalCrnGenerator(Service serviceType, String partition, String region) {
        checkNotNull(serviceType, "serviceType should not be null.");
        checkNotNull(partition, "partition should not be null.");
        checkNotNull(region, "region should not be null.");

        this.serviceType = serviceType;
        this.region = region;
        this.partition = partition;
    }

    public boolean isInternalCrnForService(String crn) {
        Crn c = Crn.fromString(crn);
        return isInternalCrn(crn) && serviceType.equals(c.getService());
    }

    public String getInternalCrnForServiceAsString() {
        return getInternalCrnForService(Crn.Partition.safeFromString(getPartition()), Crn.Region.safeFromString(getRegion())).toString();
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

    public String getPartition() {
        return partition;
    }

    public String getRegion() {
        return region;
    }

    public static RegionAwareInternalCrnGenerator regionalAwareInternalCrnGenerator(Service serviceType, String partition, String region) {
        return new RegionAwareInternalCrnGenerator(serviceType, partition, region);
    }
}
