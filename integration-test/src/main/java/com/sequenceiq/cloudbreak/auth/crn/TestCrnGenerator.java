package com.sequenceiq.cloudbreak.auth.crn;

public class TestCrnGenerator {

    private TestCrnGenerator() {

    }

    public static String getEnvironmentCrn(String resource, String accountId) {
        return Crn.builder().setResourceType(Crn.ResourceType.ENVIRONMENT).setService(Crn.Service.ENVIRONMENTS)
                .setPartition(Crn.Partition.CDP).setRegion(Crn.Region.US_WEST_1)
                .setResource(resource).setAccountId(accountId).build().toString();
    }

    public static String getDatalakeCrn(String resource, String accountId) {
        return Crn.builder().setResourceType(Crn.ResourceType.DATALAKE).setService(Crn.Service.DATALAKE)
                .setPartition(Crn.Partition.CDP).setRegion(Crn.Region.US_WEST_1)
                .setResource(resource).setAccountId(accountId).build().toString();
    }

    public static String getDatahubCrn() {
        return Crn.builder().setResourceType(Crn.ResourceType.CLUSTER).setService(Crn.Service.DATAHUB)
                .setPartition(Crn.Partition.CDP).setRegion(Crn.Region.US_WEST_1).setResource("dh").setAccountId("acc").build().toString();
    }
}
