package com.sequenceiq.cloudbreak.auth.crn;

import static com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGenerator.regionalAwareInternalCrnGenerator;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Service;

@Service
@ConfigurationProperties("crn")
public class RegionAwareInternalCrnGeneratorFactory {

    private String partition;

    private String region;

    public RegionAwareInternalCrnGenerator datahub() {
        return init(Crn.Service.DATAHUB);
    }

    /**
     * @deprecated use {@link #iam(String)} instead
     */
    @Deprecated
    public RegionAwareInternalCrnGenerator iam() {
        return init(Crn.Service.IAM);
    }

    public RegionAwareInternalCrnGenerator iam(String accountId) {
        return init(Crn.Service.IAM, accountId);
    }

    public RegionAwareInternalCrnGenerator sdxAdmin() {
        return init(Crn.Service.SDXADMIN);
    }

    public RegionAwareInternalCrnGenerator coreAdmin() {
        return init(Crn.Service.COREADMIN);
    }

    public RegionAwareInternalCrnGenerator autoscale() {
        return init(Crn.Service.AUTOSCALE);
    }

    public RegionAwareInternalCrnGenerator remoteCluster() {
        return init(Crn.Service.REMOTECLUSTER);
    }

    public RegionAwareInternalCrnGenerator externalizedCompute() {
        return init(Crn.Service.EXTERNALIZED_COMPUTE);
    }

    private RegionAwareInternalCrnGenerator init(Crn.Service serviceType) {
        return init(serviceType, null);
    }

    private RegionAwareInternalCrnGenerator init(Crn.Service serviceType, String accountId) {
        return regionalAwareInternalCrnGenerator(serviceType, partition, region, accountId);
    }

    public String getPartition() {
        return partition;
    }

    public void setPartition(String partition) {
        this.partition = partition;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }
}
