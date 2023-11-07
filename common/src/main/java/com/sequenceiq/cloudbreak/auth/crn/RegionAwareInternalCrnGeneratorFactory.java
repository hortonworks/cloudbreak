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

    public RegionAwareInternalCrnGenerator iam() {
        return init(Crn.Service.IAM);
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

    private RegionAwareInternalCrnGenerator init(Crn.Service serviceType) {
        return regionalAwareInternalCrnGenerator(serviceType, partition, region);
    }

    public String getPartition() {
        return partition;
    }

    public String getRegion() {
        return region;
    }

    public void setPartition(String partition) {
        this.partition = partition;
    }

    public void setRegion(String region) {
        this.region = region;
    }
}
