package com.sequenceiq.environment.environment.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone;
import com.sequenceiq.cloudbreak.cloud.model.CloudRegions;
import com.sequenceiq.cloudbreak.cloud.model.Coordinate;
import com.sequenceiq.cloudbreak.cloud.model.Region;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.environment.api.v1.environment.model.base.Tunnel;
import com.sequenceiq.environment.credential.domain.Credential;
import com.sequenceiq.environment.environment.EnvironmentStatus;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.domain.EnvironmentAuthentication;
import com.sequenceiq.environment.network.dao.domain.AzureNetwork;
import com.sequenceiq.environment.network.dao.domain.BaseNetwork;
import com.sequenceiq.environment.parameters.dao.domain.AwsParameters;
import com.sequenceiq.environment.parameters.dao.domain.S3GuardTableCreation;

public class EnvironmentTestData {
    public static final String ACCOUNT_ID = "accid";

    public static final String USER = "userid";

    public static final String CRN = "crnid";

    public static final String ENVIRONMENT_NAME = "envname";

    public static final String CIDR = "cidr";

    public static final String TEST = "test";

    public static final String DEFAULT_SG = "defaultSG";

    public static final String DESCRIPTION = "description";

    public static final String OK = "OK";

    public static final String KNOX_SG = "knoxSG";

    public static final AwsParameters PARAMETERS = newAwsParameters();

    public static final String S3_GUARD_TABLE_NAME = "dynamotable";

    public static final String CLOUD_PLATFORM = "AZURE";

    public static final Credential CREDENTIAL = newCredential();

    public static final double LATITUDE = 10.0d;

    public static final double LONGITUDE = 20.0d;

    public static final String LOCATION = "location";

    public static final String LOCATION_DISPLAY_NAME = "locationDisplay";

    public static final Json REGIONS = new Json("[\"region1\"]");

    public static final BaseNetwork NETWORK = newNetwork();

    public static final String NETWORK_ID = "networkId";

    public static final String CREDENTIAL_CRN = "credentialCrn";

    public static final EnvironmentAuthentication AUTHENTICATION = newAuthentication();

    private EnvironmentTestData() {
    }

    public static Environment newTestEnvironment() {
        Environment environment = new Environment();
        environment.setName(ENVIRONMENT_NAME);
        environment.setId(1L);
        environment.setAccountId(ACCOUNT_ID);
        environment.setResourceCrn(CRN);
        environment.setArchived(false);
        environment.setAuthentication(AUTHENTICATION);
        environment.setCidr(CIDR);
        environment.setCloudPlatform(CLOUD_PLATFORM);
        environment.setCreated(1L);
        environment.setCreateFreeIpa(true);
        environment.setCreator(TEST);
        environment.setCredential(CREDENTIAL);
        environment.setDefaultSecurityGroupId(DEFAULT_SG);
        environment.setDescription(DESCRIPTION);
        environment.setLatitude(LATITUDE);
        environment.setLongitude(LONGITUDE);
        environment.setLocation(LOCATION);
        environment.setLocationDisplayName(LOCATION_DISPLAY_NAME);
        environment.setStatus(EnvironmentStatus.AVAILABLE);
        environment.setStatusReason(OK);
        environment.getExperimentalFeatures().setTunnel(Tunnel.DIRECT);
        environment.setSecurityGroupIdForKnox(KNOX_SG);
        environment.setParameters(PARAMETERS);
        environment.setRegions(REGIONS);
        environment.setNetwork(NETWORK);
        return environment;
    }

    private static EnvironmentAuthentication newAuthentication() {
        EnvironmentAuthentication environmentAuthentication = new EnvironmentAuthentication();
        environmentAuthentication.setLoginUserName(TEST);
        return environmentAuthentication;
    }

    private static AwsParameters newAwsParameters() {
        AwsParameters awsParameters = new AwsParameters();
        awsParameters.setAccountId(ACCOUNT_ID);
        awsParameters.setId(10L);
        awsParameters.setName(ENVIRONMENT_NAME);
        awsParameters.setS3guardTableName(S3_GUARD_TABLE_NAME);
        awsParameters.setS3guardTableCreation(S3GuardTableCreation.CREATE_NEW);
        return awsParameters;
    }

    private static Credential newCredential() {
        Credential credential = new Credential();
        credential.setResourceCrn(CREDENTIAL_CRN);
        return credential;
    }

    private static BaseNetwork newNetwork() {
        AzureNetwork azureNetwork = new AzureNetwork();
        azureNetwork.setNetworkId(NETWORK_ID);
        return azureNetwork;
    }

    public static CloudRegions getCloudRegions() {
        List<Region> regions = List.of(Region.region("r1"), Region.region("r2"));
        List<String> displayNames = List.of("region 1", "region 2");
        List<Coordinate> coordinates = List.of(Coordinate.coordinate("1", "2", "Here"),
                Coordinate.coordinate("2", "2", "There"));
        List<List<AvailabilityZone>> availabilityZones = List.of(List.of(AvailabilityZone.availabilityZone("r1z1")),
                List.of(AvailabilityZone.availabilityZone("r2z1")));

        Map regionZones = new Zip().intoMap(regions, availabilityZones);
        Map regionDisplayNames = new Zip().intoMap(regions, displayNames);
        Map regionCoordinates = new Zip().intoMap(regions, coordinates);
        return new CloudRegions(regionZones, regionDisplayNames, regionCoordinates, "r1", true);
    }

    static class Zip<T, S> {
        public Map<T, S> intoMap(List<T> t, List<S> s) {
            return IntStream.range(0, Math.min(t.size(), s.size())).boxed()
                    .collect(Collectors.toMap(t::get, s::get));
        }
    }
}
