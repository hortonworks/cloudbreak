package com.sequenceiq.cloudbreak.auth.crn;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;

import java.util.UUID;

public class CrnTestUtil {

    private CrnTestUtil() {

    }

    public static void mockCrnGenerator(RegionAwareCrnGenerator regionAwareCrnGenerator) {
        lenient().when(regionAwareCrnGenerator.getPartition()).thenReturn("cdp");
        lenient().when(regionAwareCrnGenerator.getRegion()).thenReturn("us-west-1");
        lenient().when(regionAwareCrnGenerator.generateCrnStringWithUuid(any(), anyString())).thenCallRealMethod();
        lenient().when(regionAwareCrnGenerator.generateCrnWithUuid(any(), anyString())).thenCallRealMethod();
        lenient().when(regionAwareCrnGenerator.generateCrnString(any(), anyString(), anyString())).thenCallRealMethod();
        lenient().when(regionAwareCrnGenerator.generateCrn(any(), anyString(), anyString())).thenCallRealMethod();
        lenient().when(regionAwareCrnGenerator.generateAltusCrn(any(), anyString())).thenCallRealMethod();
    }

    public static Crn.Builder getCustomCrnBuilder(CrnResourceDescriptor resourceDescriptor) {
        return Crn.builder()
                .setPartition(Crn.Partition.CDP)
                .setRegion(Crn.Region.US_WEST_1)
                .setService(resourceDescriptor.getServiceType())
                .setResourceType(resourceDescriptor.getResourceType())
                .setResource(UUID.randomUUID().toString());
    }

    public static Crn.Builder getEnvironmentCrnBuilder() {
        return Crn.builder()
                .setPartition(Crn.Partition.CDP)
                .setRegion(Crn.Region.US_WEST_1)
                .setService(Crn.Service.ENVIRONMENTS)
                .setResourceType(Crn.ResourceType.ENVIRONMENT);
    }

    public static Crn.Builder getUserCrnBuilder() {
        return Crn.builder()
                .setPartition(Crn.Partition.CDP)
                .setRegion(Crn.Region.US_WEST_1)
                .setService(Crn.Service.IAM)
                .setResourceType(Crn.ResourceType.USER);
    }

    public static Crn.Builder getMachineUserCrnBuilder() {
        return Crn.builder()
                .setPartition(Crn.Partition.CDP)
                .setRegion(Crn.Region.US_WEST_1)
                .setService(Crn.Service.IAM)
                .setResourceType(Crn.ResourceType.MACHINE_USER);
    }

    public static Crn.Builder getGroupCrnBuilder() {
        return Crn.builder()
                .setPartition(Crn.Partition.CDP)
                .setRegion(Crn.Region.US_WEST_1)
                .setService(Crn.Service.IAM)
                .setResourceType(Crn.ResourceType.GROUP);
    }

    public static Crn.Builder getFreeipaCrnBuilder() {
        return Crn.builder()
                .setPartition(Crn.Partition.CDP)
                .setRegion(Crn.Region.US_WEST_1)
                .setService(Crn.Service.FREEIPA)
                .setResourceType(Crn.ResourceType.FREEIPA);
    }

    public static Crn.Builder getDatalakeCrnBuilder() {
        return Crn.builder()
                .setPartition(Crn.Partition.CDP)
                .setRegion(Crn.Region.US_WEST_1)
                .setService(Crn.Service.DATALAKE)
                .setResourceType(Crn.ResourceType.DATALAKE);
    }

    public static Crn.Builder getDatahubCrnBuilder() {
        return Crn.builder()
                .setPartition(Crn.Partition.CDP)
                .setRegion(Crn.Region.US_WEST_1)
                .setService(Crn.Service.DATAHUB)
                .setResourceType(Crn.ResourceType.CLUSTER);
    }

    public static Crn.Builder getDatabaseServerCrnBuilder() {
        return Crn.builder()
                .setPartition(Crn.Partition.CDP)
                .setRegion(Crn.Region.US_WEST_1)
                .setService(Crn.Service.REDBEAMS)
                .setResourceType(Crn.ResourceType.DATABASE_SERVER);
    }

    public static Crn.Builder getDatabaseCrnBuilder() {
        return Crn.builder()
                .setPartition(Crn.Partition.CDP)
                .setRegion(Crn.Region.US_WEST_1)
                .setService(Crn.Service.REDBEAMS)
                .setResourceType(Crn.ResourceType.DATABASE);
    }

    public static Crn.Builder getRecipeCrnBuilder() {
        return Crn.builder()
                .setPartition(Crn.Partition.CDP)
                .setRegion(Crn.Region.US_WEST_1)
                .setService(Crn.Service.DATAHUB)
                .setResourceType(Crn.ResourceType.RECIPE);
    }

    public static Crn.Builder getNetworkCrnBuilder() {
        return Crn.builder()
                .setPartition(Crn.Partition.CDP)
                .setRegion(Crn.Region.US_WEST_1)
                .setService(Crn.Service.ENVIRONMENTS)
                .setResourceType(Crn.ResourceType.NETWORK);
    }

    public static Crn.Builder getCredentialCrnBuilder() {
        return Crn.builder()
                .setPartition(Crn.Partition.CDP)
                .setRegion(Crn.Region.US_WEST_1)
                .setService(Crn.Service.ENVIRONMENTS)
                .setResourceType(Crn.ResourceType.CREDENTIAL);
    }
}
