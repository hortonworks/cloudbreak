package com.sequenceiq.cloudbreak.auth.crn;

import java.util.UUID;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Service;

@Service
@ConfigurationProperties("crn")
public class RegionAwareCrnGenerator {

    private static final String ACCOUNT_IN_IAM_CRNS = "altus";

    private String partition;

    private String region;

    public Crn generateAltusCrn(CrnResourceDescriptor resourceDescriptor, String resource) {
        return Crn.builder()
                .setAccountId(ACCOUNT_IN_IAM_CRNS)
                .setResource(resource)
                .setPartition(Crn.Partition.ALTUS)
                .setRegion(Crn.Region.safeFromString(getRegion()))
                .setService(resourceDescriptor.getServiceType())
                .setResourceType(resourceDescriptor.getResourceType())
                .build();
    }

    public Crn generateCrn(CrnResourceDescriptor resourceDescriptor, String resource, String accountId) {
        return Crn.builder()
                .setAccountId(accountId)
                .setResource(resource)
                .setPartition(Crn.Partition.safeFromString(getPartition()))
                .setRegion(Crn.Region.safeFromString(getRegion()))
                .setService(resourceDescriptor.getServiceType())
                .setResourceType(resourceDescriptor.getResourceType())
                .build();
    }

    public Crn generateCrnWithUuid(CrnResourceDescriptor resourceDescriptor, String accountId) {
        return generateCrn(resourceDescriptor, UUID.randomUUID().toString(), accountId);
    }

    public String generateCrnString(CrnResourceDescriptor resourceDescriptor, String resource, String accountId) {
        return generateCrn(resourceDescriptor, resource, accountId).toString();
    }

    public String generateCrnStringWithUuid(CrnResourceDescriptor resourceDescriptor, String accountId) {
        return generateCrnWithUuid(resourceDescriptor, accountId).toString();
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
