package com.sequenceiq.cloudbreak.domain;

import static com.sequenceiq.cloudbreak.domain.CloudPlatform.AWS;
import static com.sequenceiq.cloudbreak.domain.CloudPlatform.AZURE;
import static com.sequenceiq.cloudbreak.domain.CloudPlatform.GCP;
import static com.sequenceiq.cloudbreak.domain.CloudPlatform.OPENSTACK;

import java.util.ArrayList;
import java.util.List;

public enum CloudRegion {

    EAST_ASIA("East Asia", "East Asia", AZURE),
    NORTH_EUROPE("North Europe", "North Europe", AZURE),
    WEST_EUROPE("West Europe", "West Europe", AZURE),
    EAST_US("East US", "East US", AZURE),
    CENTRAL_US("Central US", "Central US", AZURE),
    SOUTH_CENTRAL_US("South Central US", "South Central US", AZURE),
    NORTH_CENTRAL_US("North Central US", "North Central US", AZURE),
    EAST_US_2("East US 2", "East US 2", AZURE),
    WEST_US("West US", "West US", AZURE),
    JAPAN_EAST("Japan East", "Japan East", AZURE),
    JAPAN_WEST("Japan West", "Japan West", AZURE),
    SOUTHEAST_ASIA("Southeast Asia", "Southeast Asia", AZURE),
    BRAZIL_SOUTH("Brazil South", "Brazil South", AZURE),

    US_CENTRAL1_A("us-central1-a", "us-central1", GCP),
    US_CENTRAL1_B("us-central1-b", "us-central1", GCP),
    US_CENTRAL1_F("us-central1-f", "us-central1", GCP),
    US_CENTRAL1_C("us-central1-c", "us-central1", GCP),
    EUROPE_WEST1_B("europe-west1-b", "europe-west1", GCP),
    EUROPE_WEST1_C("europe-west1-c", "europe-west1", GCP),
    EUROPE_WEST1_D("europe-west1-d", "europe-west1", GCP),
    ASIA_EAST1_A("asia-east1-a", "asia-east1", GCP),
    ASIA_EAST1_B("asia-east1-b", "asia-east1", GCP),
    ASIA_EAST1_C("asia-east1-c", "asia-east1", GCP),

    GovCloud("us-gov-west-1", "us-gov-west-1", AWS),
    US_EAST_1("us-east-1", "us-east-1", AWS),
    US_WEST_1("us-west-1", "us-west-1", AWS),
    US_WEST_2("us-west-2", "us-west-2", AWS),
    EU_WEST_1("eu-west-1", "eu-west-1", AWS),
    EU_CENTRAL_1("eu-central-1", "eu-central-1", AWS),
    AP_SOUTHEAST_1("ap-southeast-1", "ap-southeast-1", AWS),
    AP_SOUTHEAST_2("ap-southeast-2", "ap-southeast-2", AWS),
    AP_NORTHEAST_1("ap-northeast-1", "ap-northeast-1", AWS),
    SA_EAST_1("sa-east-1", "sa-east-1", AWS),
    CN_NORTH_1("cn-north-1", "cn-north-1", AWS),

    LOCAL("local", "local", OPENSTACK);

    private final String value;
    private final String region;
    private final CloudPlatform cloudPlatform;

    private CloudRegion(String value, String region, CloudPlatform cloudPlatform) {
        this.region = region;
        this.value = value;
        this.cloudPlatform = cloudPlatform;
    }

    public String region() {
        return this.region;
    }

    public String value() {
        return this.value;
    }

    public CloudPlatform cloudPlatform() {
        return this.cloudPlatform;
    }

    public static CloudRegion fromName(String regionName) {
        for (CloudRegion region : CloudRegion.values()) {
            if (regionName.equals(region.region())) {
                return region;
            }
        }
        throw new IllegalArgumentException("Cannot get location enum from " + regionName + " value!");
    }

    public static List<CloudRegion> gcpRegions() {
        List<CloudRegion> cloudRegions = new ArrayList<>();
        for (CloudRegion region : CloudRegion.values()) {
            if (GCP.equals(region.cloudPlatform())) {
                cloudRegions.add(region);
            }
        }
        return cloudRegions;
    }

    public static List<CloudRegion> azureRegions() {
        List<CloudRegion> cloudRegions = new ArrayList<>();
        for (CloudRegion region : CloudRegion.values()) {
            if (AZURE.equals(region.cloudPlatform())) {
                cloudRegions.add(region);
            }
        }
        return cloudRegions;
    }

    public static List<CloudRegion> awsRegions() {
        List<CloudRegion> cloudRegions = new ArrayList<>();
        for (CloudRegion region : CloudRegion.values()) {
            if (AWS.equals(region.cloudPlatform())) {
                cloudRegions.add(region);
            }
        }
        return cloudRegions;
    }

    public static List<CloudRegion> openstackRegions() {
        List<CloudRegion> cloudRegions = new ArrayList<>();
        for (CloudRegion region : CloudRegion.values()) {
            if (OPENSTACK.equals(region.cloudPlatform())) {
                cloudRegions.add(region);
            }
        }
        return cloudRegions;
    }
}
