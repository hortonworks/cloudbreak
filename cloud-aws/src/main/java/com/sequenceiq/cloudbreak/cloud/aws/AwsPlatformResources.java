package com.sequenceiq.cloudbreak.cloud.aws;

import static com.amazonaws.util.StringUtils.isNullOrEmpty;
import static com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone.availabilityZone;
import static com.sequenceiq.cloudbreak.cloud.model.Coordinate.coordinate;
import static com.sequenceiq.cloudbreak.cloud.model.DisplayName.displayName;
import static com.sequenceiq.cloudbreak.cloud.model.Region.region;
import static com.sequenceiq.cloudbreak.cloud.model.VmType.vmTypeWithMeta;
import static com.sequenceiq.cloudbreak.cloud.model.VolumeParameterType.EPHEMERAL;
import static com.sequenceiq.cloudbreak.cloud.model.VolumeParameterType.MAGNETIC;
import static com.sequenceiq.cloudbreak.cloud.model.VolumeParameterType.SSD;
import static com.sequenceiq.cloudbreak.cloud.model.VolumeParameterType.ST1;
import static com.sequenceiq.cloudbreak.cloud.model.network.SubnetType.PRIVATE;
import static com.sequenceiq.cloudbreak.cloud.model.network.SubnetType.PUBLIC;
import static com.sequenceiq.cloudbreak.cloud.service.CloudParameterService.ACCESS_CONFIG_TYPE;
import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.SdkClientException;
import com.amazonaws.services.dynamodbv2.model.ListTablesRequest;
import com.amazonaws.services.dynamodbv2.model.ListTablesResult;
import com.amazonaws.services.ec2.model.AmazonEC2Exception;
import com.amazonaws.services.ec2.model.DescribeAvailabilityZonesRequest;
import com.amazonaws.services.ec2.model.DescribeInstanceTypeOfferingsRequest;
import com.amazonaws.services.ec2.model.DescribeInstanceTypesRequest;
import com.amazonaws.services.ec2.model.DescribeInstanceTypesResult;
import com.amazonaws.services.ec2.model.DescribeInternetGatewaysRequest;
import com.amazonaws.services.ec2.model.DescribeInternetGatewaysResult;
import com.amazonaws.services.ec2.model.DescribeKeyPairsRequest;
import com.amazonaws.services.ec2.model.DescribeRegionsRequest;
import com.amazonaws.services.ec2.model.DescribeRegionsResult;
import com.amazonaws.services.ec2.model.DescribeRouteTablesRequest;
import com.amazonaws.services.ec2.model.DescribeSecurityGroupsRequest;
import com.amazonaws.services.ec2.model.DescribeSubnetsRequest;
import com.amazonaws.services.ec2.model.DescribeSubnetsResult;
import com.amazonaws.services.ec2.model.DescribeVpcsRequest;
import com.amazonaws.services.ec2.model.DescribeVpcsResult;
import com.amazonaws.services.ec2.model.DiskInfo;
import com.amazonaws.services.ec2.model.Filter;
import com.amazonaws.services.ec2.model.InstanceStorageInfo;
import com.amazonaws.services.ec2.model.InstanceTypeInfo;
import com.amazonaws.services.ec2.model.InternetGateway;
import com.amazonaws.services.ec2.model.InternetGatewayAttachment;
import com.amazonaws.services.ec2.model.KeyPairInfo;
import com.amazonaws.services.ec2.model.RouteTable;
import com.amazonaws.services.ec2.model.SecurityGroup;
import com.amazonaws.services.ec2.model.Subnet;
import com.amazonaws.services.ec2.model.Tag;
import com.amazonaws.services.ec2.model.Vpc;
import com.amazonaws.services.identitymanagement.model.InstanceProfile;
import com.amazonaws.services.identitymanagement.model.ListInstanceProfilesRequest;
import com.amazonaws.services.identitymanagement.model.ListInstanceProfilesResult;
import com.amazonaws.services.identitymanagement.model.ListRolesRequest;
import com.amazonaws.services.identitymanagement.model.ListRolesResult;
import com.amazonaws.services.identitymanagement.model.Role;
import com.amazonaws.services.kms.model.AliasListEntry;
import com.amazonaws.services.kms.model.DescribeKeyRequest;
import com.amazonaws.services.kms.model.DescribeKeyResult;
import com.amazonaws.services.kms.model.ListAliasesRequest;
import com.amazonaws.services.kms.model.ListAliasesResult;
import com.amazonaws.services.kms.model.ListKeysRequest;
import com.amazonaws.services.kms.model.ListKeysResult;
import com.amazonaws.services.rds.model.Certificate;
import com.amazonaws.services.rds.model.DescribeCertificatesRequest;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.cloud.PlatformResources;
import com.sequenceiq.cloudbreak.cloud.aws.client.AmazonDynamoDBClient;
import com.sequenceiq.cloudbreak.cloud.aws.client.AmazonEc2Client;
import com.sequenceiq.cloudbreak.cloud.aws.client.AmazonIdentityManagementClient;
import com.sequenceiq.cloudbreak.cloud.aws.client.AmazonKmsClient;
import com.sequenceiq.cloudbreak.cloud.aws.client.AmazonRdsClient;
import com.sequenceiq.cloudbreak.cloud.aws.util.AwsPageCollector;
import com.sequenceiq.cloudbreak.cloud.aws.view.AwsCredentialView;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.exception.CloudUnauthorizedException;
import com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone;
import com.sequenceiq.cloudbreak.cloud.model.CloudAccessConfig;
import com.sequenceiq.cloudbreak.cloud.model.CloudAccessConfigs;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudEncryptionKey;
import com.sequenceiq.cloudbreak.cloud.model.CloudEncryptionKeys;
import com.sequenceiq.cloudbreak.cloud.model.CloudGateWay;
import com.sequenceiq.cloudbreak.cloud.model.CloudGateWays;
import com.sequenceiq.cloudbreak.cloud.model.CloudIpPools;
import com.sequenceiq.cloudbreak.cloud.model.CloudNetwork;
import com.sequenceiq.cloudbreak.cloud.model.CloudNetworks;
import com.sequenceiq.cloudbreak.cloud.model.CloudRegions;
import com.sequenceiq.cloudbreak.cloud.model.CloudSecurityGroup;
import com.sequenceiq.cloudbreak.cloud.model.CloudSecurityGroups;
import com.sequenceiq.cloudbreak.cloud.model.CloudSshKey;
import com.sequenceiq.cloudbreak.cloud.model.CloudSshKeys;
import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmTypes;
import com.sequenceiq.cloudbreak.cloud.model.ConfigSpecification;
import com.sequenceiq.cloudbreak.cloud.model.Coordinate;
import com.sequenceiq.cloudbreak.cloud.model.DisplayName;
import com.sequenceiq.cloudbreak.cloud.model.Region;
import com.sequenceiq.cloudbreak.cloud.model.RegionCoordinateSpecification;
import com.sequenceiq.cloudbreak.cloud.model.RegionCoordinateSpecifications;
import com.sequenceiq.cloudbreak.cloud.model.RegionSpecification;
import com.sequenceiq.cloudbreak.cloud.model.RegionsSpecification;
import com.sequenceiq.cloudbreak.cloud.model.VmType;
import com.sequenceiq.cloudbreak.cloud.model.VmTypeMeta.VmTypeMetaBuilder;
import com.sequenceiq.cloudbreak.cloud.model.VolumeParameterConfig;
import com.sequenceiq.cloudbreak.cloud.model.VolumeParameterType;
import com.sequenceiq.cloudbreak.cloud.model.database.CloudDatabaseServerSslCertificate;
import com.sequenceiq.cloudbreak.cloud.model.database.CloudDatabaseServerSslCertificateType;
import com.sequenceiq.cloudbreak.cloud.model.database.CloudDatabaseServerSslCertificates;
import com.sequenceiq.cloudbreak.cloud.model.nosql.CloudNoSqlTable;
import com.sequenceiq.cloudbreak.cloud.model.nosql.CloudNoSqlTables;
import com.sequenceiq.cloudbreak.cloud.model.resourcegroup.CloudResourceGroups;
import com.sequenceiq.cloudbreak.cloud.model.view.PlatformResourceSecurityGroupFilterView;
import com.sequenceiq.cloudbreak.cloud.model.view.PlatformResourceSshKeyFilterView;
import com.sequenceiq.cloudbreak.cloud.model.view.PlatformResourceVpcFilterView;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.common.type.CloudConstants;
import com.sequenceiq.cloudbreak.filter.MinimalHardwareFilter;
import com.sequenceiq.cloudbreak.service.CloudbreakResourceReaderService;
import com.sequenceiq.cloudbreak.util.PermanentlyFailedException;

@Service
public class AwsPlatformResources implements PlatformResources {

    private static final Logger LOGGER = LoggerFactory.getLogger(AwsPlatformResources.class);

    private static final int UNAUTHORIZED = 403;

    private static final String ENABLED_AVAILABILITY_ZONES_FILE = "enabled-availability-zones";

    private static final String SUPPORTED = "supported";

    private static final int SEGMENT = 100;

    private static final int MINIMUM_MAGNETIC_SIZE = 1;

    private static final int ONE = 1;

    private static final int MAXIMUM_MAGNETIC_SIZE = 1024;

    private static final int TWENTY_FOUR = 24;

    private static final int MAXIMUM_ST1_SIZE = 17592;

    private static final int MINIMUM_SSD_SIZE = 1;

    private static final int MAXIMUM_SSD_SIZE = 17592;

    private static final int MINIMUM_ST1_SIZE = 500;

    private static final int MAX_RESULTS = 1000;

    private static final int ONE_THOUSAND = 1000;

    @Inject
    private AwsClient awsClient;

    @Inject
    private CloudbreakResourceReaderService cloudbreakResourceReaderService;

    @Inject
    private AwsDefaultZoneProvider awsDefaultZoneProvider;

    @Inject
    private AwsSubnetIgwExplorer awsSubnetIgwExplorer;

    @Inject
    private AwsAvailabilityZoneProvider awsAvailabilityZoneProvider;

    @Inject
    private MinimalHardwareFilter minimalHardwareFilter;

    @Value("${cb.aws.vm.parameter.definition.path:}")
    private String awsVmParameterDefinitionPath;

    @Value("${cb.aws.disabled.instance.types:}")
    private List<String> disabledInstanceTypes;

    @Value("${cb.aws.distrox.enabled.instance.types:}")
    private List<String> enabledDistroxInstanceTypes;

    @Value("${cb.aws.fetch.max.items:500}")
    private Integer fetchMaxItems;

    @Value("${distrox.restrict.instance.types:true}")
    private boolean restrictInstanceTypes;

    private final Predicate<VmType> enabledInstanceTypeFilter = vmt -> disabledInstanceTypes.stream()
            .filter(it -> !it.isEmpty())
            .noneMatch(di -> vmt.value().startsWith(di));

    private final Predicate<VmType> enabledDistroxInstanceTypeFilter = vmt -> enabledDistroxInstanceTypes.stream()
            .filter(it -> !it.isEmpty())
            .anyMatch(di -> vmt.value().equals(di));

    private Map<Region, Coordinate> regionCoordinates = new HashMap<>();

    private final Map<Region, VmType> defaultVmTypes = new HashMap<>();

    private Map<Region, DisplayName> regionDisplayNames = new HashMap<>();

    private Set<Region> enabledRegions;

    private Set<AvailabilityZone> enabledAvailabilityZones;

    @PostConstruct
    public void init() {
        readEnabledRegionsAndAvailabilityZones();
        regionDisplayNames = readRegionDisplayNames(resourceDefinition("zone-coordinates"));
        regionCoordinates = readRegionCoordinates(resourceDefinition("zone-coordinates"));
    }

    public String resourceDefinition(String resource) {
        return cloudbreakResourceReaderService.resourceDefinition("aws", resource);
    }

    private void addConfig(VmTypeMetaBuilder builder, ConfigSpecification configSpecification) {
        if (configSpecification.getVolumeParameterType().equals(VolumeParameterType.AUTO_ATTACHED.name())) {
            builder.withAutoAttachedConfig(volumeParameterConfig(configSpecification));
        } else if (configSpecification.getVolumeParameterType().equals(VolumeParameterType.EPHEMERAL.name())) {
            builder.withEphemeralConfig(volumeParameterConfig(configSpecification));
        } else if (configSpecification.getVolumeParameterType().equals(MAGNETIC.name())) {
            builder.withMagneticConfig(volumeParameterConfig(configSpecification));
        } else if (configSpecification.getVolumeParameterType().equals(VolumeParameterType.SSD.name())) {
            builder.withSsdConfig(volumeParameterConfig(configSpecification));
        } else if (configSpecification.getVolumeParameterType().equals(ST1.name())) {
            builder.withSt1Config(volumeParameterConfig(configSpecification));
        }
    }

    private VolumeParameterConfig volumeParameterConfig(ConfigSpecification configSpecification) {
        return new VolumeParameterConfig(
                VolumeParameterType.valueOf(configSpecification.getVolumeParameterType()),
                Integer.valueOf(configSpecification.getMinimumSize()),
                Integer.valueOf(configSpecification.getMaximumSize()),
                Integer.valueOf(configSpecification.getMinimumNumber()),
                configSpecification.getMaximumNumberWithLimit());
    }

    private Map<Region, DisplayName> readRegionDisplayNames(String displayNames) {
        Map<Region, DisplayName> regionDisplayNames = new HashMap<>();
        try {
            RegionCoordinateSpecifications regionCoordinateSpecifications = JsonUtil.readValue(displayNames, RegionCoordinateSpecifications.class);
            for (RegionCoordinateSpecification regionCoordinateSpecification : regionCoordinateSpecifications.getItems()) {
                Region region = region(regionCoordinateSpecification.getName());
                if (!enabledRegions.contains(region)) {
                    continue;
                }
                regionDisplayNames.put(region,
                        displayName(regionCoordinateSpecification.getDisplayName()));
            }
        } catch (IOException ignored) {
            return regionDisplayNames;
        }
        return regionDisplayNames;
    }

    private Map<Region, Coordinate> readRegionCoordinates(String displayNames) {
        Map<Region, Coordinate> regionCoordinates = new HashMap<>();
        try {
            RegionCoordinateSpecifications regionCoordinateSpecifications = JsonUtil.readValue(displayNames, RegionCoordinateSpecifications.class);
            for (RegionCoordinateSpecification regionCoordinateSpecification : regionCoordinateSpecifications.getItems()) {
                Region region = region(regionCoordinateSpecification.getName());
                if (!enabledRegions.contains(region)) {
                    continue;
                }
                Optional<Entry<Region, DisplayName>> regionEntry = regionDisplayNames
                        .entrySet()
                        .stream()
                        .filter(e -> e.getKey().getRegionName().equalsIgnoreCase(regionCoordinateSpecification.getName()))
                        .findFirst();

                regionCoordinates.put(region,
                        coordinate(
                                regionCoordinateSpecification.getLongitude(),
                                regionCoordinateSpecification.getLatitude(),
                                regionCoordinateSpecification.getDisplayName(),
                                regionEntry.isPresent() ? regionEntry.get().getKey().value() : regionCoordinateSpecification.getDisplayName(),
                                regionCoordinateSpecification.isK8sSupported()));
            }
        } catch (IOException ignored) {
            return regionCoordinates;
        }
        return regionCoordinates;
    }

    private void readEnabledRegionsAndAvailabilityZones() {
        try {
            String fileName = resourceDefinition(ENABLED_AVAILABILITY_ZONES_FILE);
            RegionsSpecification regionCoordinateSpecifications = JsonUtil.readValue(fileName, RegionsSpecification.class);
            enabledRegions = regionCoordinateSpecifications.getItems().stream()
                    .map(RegionSpecification::getName)
                    .map(Region::region)
                    .collect(Collectors.toSet());
            enabledAvailabilityZones = regionCoordinateSpecifications.getItems().stream()
                    .flatMap(region -> region.getZones().stream())
                    .map(AvailabilityZone::availabilityZone)
                    .collect(Collectors.toSet());
        } catch (IOException e) {
            LOGGER.error("Failed to read enabled AWS regions and availability zones from file.", e);
            enabledRegions = new HashSet<>();
            enabledAvailabilityZones = new HashSet<>();
        }
    }

    @Override
    public CloudNetworks networks(CloudCredential cloudCredential, Region region, Map<String, String> filters) {
        AmazonEc2Client ec2Client = awsClient.createEc2Client(new AwsCredentialView(cloudCredential), region.value());
        try {
            LOGGER.debug("Describing route tables in region {}", region.getRegionName());
            List<RouteTable> allRouteTables = AwsPageCollector.getAllRouteTables(ec2Client, new DescribeRouteTablesRequest());
            DescribeVpcsRequest describeVpcsRequest = getDescribeVpcsRequestWithFilters(filters);
            Set<CloudNetwork> cloudNetworks = new HashSet<>();

            DescribeVpcsResult describeVpcsResult = null;
            boolean first = true;
            while (first || !isNullOrEmpty(describeVpcsResult.getNextToken())) {
                LOGGER.debug("Getting VPC list in region {}{}", region.getRegionName(), first ? "" : " (continuation)");
                first = false;
                describeVpcsRequest.setNextToken(describeVpcsResult == null ? null : describeVpcsResult.getNextToken());
                describeVpcsResult = ec2Client.describeVpcs(describeVpcsRequest);
                Set<CloudNetwork> partialNetworks = getCloudNetworks(ec2Client, allRouteTables, describeVpcsResult);
                cloudNetworks.addAll(partialNetworks);
            }
            Map<String, Set<CloudNetwork>> result = new HashMap<>();
            result.put(region.value(), cloudNetworks);
            return new CloudNetworks(result);
        } catch (SdkClientException e) {
            LOGGER.error(String.format("Unable to enumerate networks in region '%s'. Check exception for details.", region.getRegionName()), e);
            throw e;
        }
    }

    private DescribeVpcsRequest getDescribeVpcsRequestWithFilters(Map<String, String> filters) {
        //create vpc filter view
        PlatformResourceVpcFilterView filter = new PlatformResourceVpcFilterView(filters);
        DescribeVpcsRequest describeVpcsRequest = new DescribeVpcsRequest();
        // If the filtervalue is provided then we should filter only for those vpc
        if (!Strings.isNullOrEmpty(filter.getVpcId())) {
            describeVpcsRequest.withVpcIds(filter.getVpcId());
        }
        return describeVpcsRequest;
    }

    private Set<CloudNetwork> getCloudNetworks(AmazonEc2Client ec2Client, List<RouteTable> describeRouteTablesResult,
            DescribeVpcsResult describeVpcsResult) {

        Set<CloudNetwork> cloudNetworks = new HashSet<>();
        LOGGER.debug("Processing VPCs");
        for (Vpc vpc : describeVpcsResult.getVpcs()) {
            List<Subnet> awsSubnets = getSubnets(ec2Client, vpc);
            Set<CloudSubnet> subnets = convertAwsSubnetsToCloudSubnets(describeRouteTablesResult, awsSubnets);

            Map<String, Object> properties = prepareNetworkProperties(vpc);
            Optional<String> name = getName(vpc.getTags());
            if (name.isPresent()) {
                cloudNetworks.add(new CloudNetwork(name.get(), vpc.getVpcId(), subnets, properties));
            } else {
                cloudNetworks.add(new CloudNetwork(vpc.getVpcId(), vpc.getVpcId(), subnets, properties));
            }
        }
        return cloudNetworks;
    }

    private List<Subnet> getSubnets(AmazonEc2Client ec2Client, Vpc vpc) {
        List<Subnet> awsSubnets = new ArrayList<>();
        DescribeSubnetsResult describeSubnetsResult = null;
        do {
            LOGGER.debug("Describing subnets for VPC {}{}", vpc.getVpcId(), describeSubnetsResult == null ? "" : " (continuation)");
            DescribeSubnetsRequest describeSubnetsRequest = createSubnetsDescribeRequest(vpc, describeSubnetsResult == null
                    ? null
                    : describeSubnetsResult.getNextToken());
            describeSubnetsResult = ec2Client.describeSubnets(describeSubnetsRequest);
            describeSubnetsResult.getSubnets().stream()
                    .filter(subnet -> enabledAvailabilityZones.contains(availabilityZone(subnet.getAvailabilityZone())))
                    .forEach(awsSubnets::add);
        } while (!isNullOrEmpty(describeSubnetsResult.getNextToken()));
        return awsSubnets;
    }

    private Set<CloudSubnet> convertAwsSubnetsToCloudSubnets(List<RouteTable> describeRouteTablesResult, List<Subnet> awsSubnets) {
        Set<CloudSubnet> subnets = new HashSet<>();
        for (Subnet subnet : awsSubnets) {
            boolean hasInternetGateway = awsSubnetIgwExplorer.hasInternetGatewayOfSubnet(describeRouteTablesResult, subnet.getSubnetId(), subnet.getVpcId());
            LOGGER.info("The subnet {} has internetGateway value is '{}'", subnet, hasInternetGateway);
            Optional<String> subnetName = getName(subnet.getTags());
            subnets.add(
                    new CloudSubnet(
                            subnet.getSubnetId(),
                            subnetName.orElse(subnet.getSubnetId()),
                            subnet.getAvailabilityZone(),
                            subnet.getCidrBlock(),
                            !hasInternetGateway,
                            subnet.getMapPublicIpOnLaunch(),
                            hasInternetGateway,
                            hasInternetGateway ? PUBLIC : PRIVATE)
            );
        }
        return subnets;
    }

    private Map<String, Object> prepareNetworkProperties(Vpc vpc) {
        Map<String, Object> properties = new HashMap<>();
        properties.put("cidrBlock", vpc.getCidrBlock());
        properties.put("default", vpc.getIsDefault());
        properties.put("dhcpOptionsId", vpc.getDhcpOptionsId());
        properties.put("instanceTenancy", vpc.getInstanceTenancy());
        properties.put("state", vpc.getState());
        return properties;
    }

    private Optional<String> getName(List<Tag> tags) {
        for (Tag tag : tags) {
            if ("Name".equals(tag.getKey())) {
                return Optional.ofNullable(tag.getValue());
            }
        }
        return Optional.empty();
    }

    private DescribeSubnetsRequest createSubnetsDescribeRequest(Vpc vpc, String nextToken) {
        return new DescribeSubnetsRequest()
                .withFilters(new Filter("vpc-id", singletonList(vpc.getVpcId())))
                .withNextToken(nextToken);
    }

    @Override
    public CloudSshKeys sshKeys(CloudCredential cloudCredential, Region region, Map<String, String> filters) {
        Map<String, Set<CloudSshKey>> result = new HashMap<>();
        for (Region actualRegion : regions(cloudCredential, region, new HashMap<>(), true).getCloudRegions().keySet()) {
            // If region is provided then should filter for those region
            if (regionMatch(actualRegion, region)) {
                Set<CloudSshKey> cloudSshKeys = new HashSet<>();
                AmazonEc2Client ec2Client = awsClient.createEc2Client(new AwsCredentialView(cloudCredential), actualRegion.value());

                //create sshkey filter view
                PlatformResourceSshKeyFilterView filter = new PlatformResourceSshKeyFilterView(filters);

                DescribeKeyPairsRequest describeKeyPairsRequest = new DescribeKeyPairsRequest();

                // If the filtervalue is provided then we should filter only for those securitygroups
                if (!Strings.isNullOrEmpty(filter.getKeyName())) {
                    describeKeyPairsRequest.withKeyNames(filter.getKeyName());
                }

                for (KeyPairInfo keyPairInfo : ec2Client.describeKeyPairs(describeKeyPairsRequest).getKeyPairs()) {
                    Map<String, Object> properties = new HashMap<>();
                    properties.put("fingerPrint", keyPairInfo.getKeyFingerprint());
                    cloudSshKeys.add(new CloudSshKey(keyPairInfo.getKeyName(), properties));
                }
                result.put(actualRegion.value(), cloudSshKeys);
            }
        }
        return new CloudSshKeys(result);
    }

    @Override
    public CloudSecurityGroups securityGroups(CloudCredential cloudCredential, Region region, Map<String, String> filters) {
        Map<String, Set<CloudSecurityGroup>> result = new HashMap<>();
        Set<CloudSecurityGroup> cloudSecurityGroups = new HashSet<>();
        AmazonEc2Client ec2Client = awsClient.createEc2Client(new AwsCredentialView(cloudCredential), region.value());

        //create securitygroup filter view
        PlatformResourceSecurityGroupFilterView filter = new PlatformResourceSecurityGroupFilterView(filters);

        DescribeSecurityGroupsRequest describeSecurityGroupsRequest = new DescribeSecurityGroupsRequest();
        // If the filtervalue is provided then we should filter only for those securitygroups
        if (!Strings.isNullOrEmpty(filter.getVpcId())) {
            describeSecurityGroupsRequest.withFilters(new Filter("vpc-id", singletonList(filter.getVpcId())));
        }
        if (!Strings.isNullOrEmpty(filter.getGroupId())) {
            describeSecurityGroupsRequest.withGroupIds(filter.getGroupId());
        }
        if (!Strings.isNullOrEmpty(filter.getGroupName())) {
            describeSecurityGroupsRequest.withGroupNames(filter.getGroupName());
        }

        for (SecurityGroup securityGroup : fetchSecurityGroups(ec2Client, describeSecurityGroupsRequest)) {
            Map<String, Object> properties = new HashMap<>();
            properties.put("vpcId", securityGroup.getVpcId());
            properties.put("description", securityGroup.getDescription());
            properties.put("ipPermissions", securityGroup.getIpPermissions());
            properties.put("ipPermissionsEgress", securityGroup.getIpPermissionsEgress());
            cloudSecurityGroups.add(new CloudSecurityGroup(securityGroup.getGroupName(), securityGroup.getGroupId(), properties));
        }
        result.put(region.value(), cloudSecurityGroups);
        return new CloudSecurityGroups(result);
    }

    private List<SecurityGroup> fetchSecurityGroups(AmazonEc2Client ec2Client, DescribeSecurityGroupsRequest describeSecurityGroupsRequest) {
        try {
            return ec2Client.describeSecurityGroups(describeSecurityGroupsRequest).getSecurityGroups();
        } catch (AmazonEC2Exception e) {
            if (e.getStatusCode() == HttpStatus.BAD_REQUEST.value() || e.getStatusCode() == HttpStatus.NOT_FOUND.value()) {
                throw new PermanentlyFailedException(e.getErrorMessage(), e);
            } else {
                throw e;
            }
        }
    }

    @Override
    @Cacheable(cacheNames = "cloudResourceRegionCache", key = "{ #cloudCredential?.id, #availabilityZonesNeeded }")
    public CloudRegions regions(CloudCredential cloudCredential, Region region, Map<String, String> filters, boolean availabilityZonesNeeded) {
        AmazonEc2Client ec2Client = awsClient.createEc2Client(new AwsCredentialView(cloudCredential));
        Map<Region, List<AvailabilityZone>> regionListMap = new HashMap<>();
        Map<Region, String> displayNames = new HashMap<>();
        Map<Region, Coordinate> coordinates = new HashMap<>();

        DescribeRegionsResult describeRegionsResult = describeRegionsResult(ec2Client);
        String defaultRegion = awsDefaultZoneProvider.getDefaultZone(cloudCredential);

        for (com.amazonaws.services.ec2.model.Region awsRegion : describeRegionsResult.getRegions()) {
            if (!enabledRegions.contains(region(awsRegion.getRegionName()))) {
                continue;
            }
            if (region == null || Strings.isNullOrEmpty(region.value()) || awsRegion.getRegionName().equals(region.value())) {
                try {
                    fetchAZsIfNeeded(availabilityZonesNeeded, regionListMap, awsRegion, cloudCredential);
                } catch (AmazonEC2Exception e) {
                    LOGGER.info("Failed to retrieve AZ from Region: {}!", awsRegion.getRegionName(), e);
                }
                addDisplayName(displayNames, awsRegion);
                addCoordinate(coordinates, awsRegion);
            }
        }
        if (region != null && !Strings.isNullOrEmpty(region.value())) {
            defaultRegion = region.value();
        }
        return new CloudRegions(regionListMap, displayNames, coordinates, defaultRegion, true);
    }

    private void fetchAZsIfNeeded(boolean availabilityZonesNeeded, Map<Region, List<AvailabilityZone>> regionListMap,
            com.amazonaws.services.ec2.model.Region awsRegion, CloudCredential cloudCredential) {
        List<AvailabilityZone> collectedAZs = new ArrayList<>();
        if (availabilityZonesNeeded) {
            DescribeAvailabilityZonesRequest describeAvailabilityZonesRequest = getDescribeAvailabilityZonesRequest(awsRegion);
            LOGGER.debug("Describing AZs in region {}", awsRegion.getRegionName());
            List<com.amazonaws.services.ec2.model.AvailabilityZone> availabilityZones
                    = awsAvailabilityZoneProvider.describeAvailabilityZones(cloudCredential, describeAvailabilityZonesRequest, awsRegion);
            availabilityZones.stream()
                    .map(com.amazonaws.services.ec2.model.AvailabilityZone::getZoneName)
                    .map(AvailabilityZone::availabilityZone)
                    .filter(enabledAvailabilityZones::contains)
                    .forEach(collectedAZs::add);
        }
        regionListMap.put(region(awsRegion.getRegionName()), collectedAZs);
    }

    private DescribeAvailabilityZonesRequest getDescribeAvailabilityZonesRequest(com.amazonaws.services.ec2.model.Region awsRegion) {
        DescribeAvailabilityZonesRequest describeAvailabilityZonesRequest = new DescribeAvailabilityZonesRequest();
        Filter filter = new Filter();
        filter.setName("region-name");
        Collection<String> list = new ArrayList<>();
        list.add(awsRegion.getRegionName());
        filter.setValues(list);
        describeAvailabilityZonesRequest.withFilters(filter);
        return describeAvailabilityZonesRequest;
    }

    public void addDisplayName(Map<Region, String> displayNames, com.amazonaws.services.ec2.model.Region awsRegion) {
        DisplayName displayName = regionDisplayNames.get(region(awsRegion.getRegionName()));
        if (displayName == null || Strings.isNullOrEmpty(displayName.value())) {
            displayNames.put(region(awsRegion.getRegionName()), awsRegion.getRegionName());
        } else {
            displayNames.put(region(awsRegion.getRegionName()), displayName.value());
        }
    }

    public void addCoordinate(Map<Region, Coordinate> coordinates, com.amazonaws.services.ec2.model.Region awsRegion) {
        Coordinate coordinate = regionCoordinates.get(region(awsRegion.getRegionName()));
        if (coordinate == null || coordinate.getLongitude() == null || coordinate.getLatitude() == null) {
            LOGGER.warn("Unregistered region with location coordinates on aws side: {} using default California", awsRegion.getRegionName());
            coordinates.put(region(awsRegion.getRegionName()), Coordinate.defaultCoordinate());
        } else {
            coordinates.put(region(awsRegion.getRegionName()), coordinate);
        }
    }

    private DescribeRegionsResult describeRegionsResult(AmazonEc2Client ec2Client) {
        LOGGER.debug("Getting regions");
        try {
            DescribeRegionsRequest describeRegionsRequest = new DescribeRegionsRequest();
            return ec2Client.describeRegions(describeRegionsRequest);
        } catch (AmazonEC2Exception e) {
            LOGGER.info("Failed to retrieve regions!", e);
        }
        return new DescribeRegionsResult();
    }

    @Override
    @Cacheable(cacheNames = "cloudResourceVmTypeCache", key = "#cloudCredential?.id + #region.getRegionName()")
    public CloudVmTypes virtualMachines(CloudCredential cloudCredential, Region region, Map<String, String> filters) {
        return getCloudVmTypes(cloudCredential, region, filters, enabledInstanceTypeFilter, false);
    }

    @Override
    @Cacheable(cacheNames = "cloudResourceVmTypeCache", key = "#cloudCredential?.id + #region.getRegionName() + 'distrox'")
    public CloudVmTypes virtualMachinesForDistroX(CloudCredential cloudCredential, Region region, Map<String, String> filters) {
        if (restrictInstanceTypes) {
            return getCloudVmTypes(cloudCredential, region, filters, enabledDistroxInstanceTypeFilter, true);
        } else {
            return getCloudVmTypes(cloudCredential, region, filters, enabledInstanceTypeFilter, true);
        }
    }

    private CloudVmTypes getCloudVmTypes(CloudCredential cloudCredential, Region region, Map<String, String> filters,
            Predicate<VmType> enabledInstanceTypeFilter, boolean enableMinimalHardwareFilter) {
        CloudRegions regions = regions(cloudCredential, region, filters, true);

        Map<String, Set<VmType>> cloudVmResponses = new HashMap<>();
        Map<String, VmType> defaultCloudVmResponses = new HashMap<>();

        AwsCredentialView awsCredentialView = new AwsCredentialView(cloudCredential);
        AmazonEc2Client ec2Client = awsClient.createEc2Client(awsCredentialView, region.getRegionName());

        List<String> instanceTypes = ec2Client
                .describeInstanceTypeOfferings(getOfferingsRequest(region))
                .getInstanceTypeOfferings()
                .stream()
                .map(e -> e.getInstanceType())
                .collect(Collectors.toList());

        Set<VmType> awsInstances = new HashSet<>();
        for (int actualSegment = 0; actualSegment < instanceTypes.size(); actualSegment += SEGMENT) {
            DescribeInstanceTypesRequest request = new DescribeInstanceTypesRequest();
            request.setInstanceTypes(getInstanceTypes(instanceTypes, actualSegment));
            getVmTypesWithAwsCall(awsInstances, ec2Client.describeInstanceTypes(request));
        }
        if (enableMinimalHardwareFilter) {
            awsInstances = awsInstances.stream()
                    .filter(e -> minimalHardwareFilter
                            .suitableAsMinimumHardware(e.getMetaData().getCPU(), e.getMetaData().getMemoryInGb()))
                    .collect(Collectors.toSet());
        }
        fillUpAvailabilityZones(region, enabledInstanceTypeFilter, regions, cloudVmResponses, defaultCloudVmResponses, awsInstances);
        filterInstancesByFilters(enabledInstanceTypeFilter, cloudVmResponses);

        return new CloudVmTypes(cloudVmResponses, defaultCloudVmResponses);
    }

    private void fillUpAvailabilityZones(Region region,
        Predicate<VmType> enabledInstanceTypeFilter,
        CloudRegions regions,
        Map<String, Set<VmType>> cloudVmResponses,
        Map<String, VmType> defaultCloudVmResponses,
        Set<VmType> awsInstances) {
        List<AvailabilityZone> availabilityZones = regions.getCloudRegions().get(region);
        if (availabilityZones != null && !availabilityZones.isEmpty()) {
            for (AvailabilityZone availabilityZone : availabilityZones) {
                Set<VmType> types = awsInstances.stream()
                        .filter(enabledInstanceTypeFilter)
                        .collect(Collectors.toSet());
                cloudVmResponses.put(availabilityZone.value(), types);
                defaultCloudVmResponses.put(availabilityZone.value(), defaultVmTypes.get(region));
            }
        } else {
            LOGGER.info("Availability zones is null or empty in {}", region.getRegionName());
        }
    }

    private DescribeInstanceTypeOfferingsRequest getOfferingsRequest(Region region) {
        return new DescribeInstanceTypeOfferingsRequest()
                .withLocationType("region")
                .withFilters(new Filter().withName("location").withValues(region.getRegionName()))
                .withMaxResults(MAX_RESULTS);
    }

    private void filterInstancesByFilters(Predicate<VmType> enabledInstanceTypeFilter, Map<String, Set<VmType>> cloudVmResponses) {
        cloudVmResponses.entrySet().forEach(az -> {
            Set<VmType> vmTypes = cloudVmResponses.get(az.getKey())
                    .stream()
                    .filter(enabledInstanceTypeFilter)
                    .collect(Collectors.toSet());
            cloudVmResponses.put(az.getKey(), vmTypes);
        });
    }

    private void getVmTypesWithAwsCall(Set<VmType> awsInstances, DescribeInstanceTypesResult describeInstanceTypesResult) {
        for (InstanceTypeInfo instanceType : describeInstanceTypesResult.getInstanceTypes()) {
            if (!instanceType.isBareMetal()) {
                VmTypeMetaBuilder vmTypeMetaBuilder = VmTypeMetaBuilder.builder()
                        .withCpuAndMemory(instanceType.getVCpuInfo().getDefaultVCpus(), getMemory(instanceType))
                        .withMagneticConfig(new VolumeParameterConfig(
                                MAGNETIC,
                                MINIMUM_MAGNETIC_SIZE,
                                MAXIMUM_MAGNETIC_SIZE,
                                ONE,
                                TWENTY_FOUR))
                        .withSsdConfig(new VolumeParameterConfig(
                                SSD,
                                MINIMUM_SSD_SIZE,
                                MAXIMUM_SSD_SIZE,
                                ONE,
                                TWENTY_FOUR))
                        .withSt1Config(new VolumeParameterConfig(
                                ST1,
                                MINIMUM_ST1_SIZE,
                                MAXIMUM_ST1_SIZE,
                                ONE,
                                TWENTY_FOUR));
                if (instanceType.getInstanceStorageSupported()) {
                    InstanceStorageInfo instanceStorageInfo = instanceType.getInstanceStorageInfo();
                    DiskInfo diskInfo = instanceStorageInfo.getDisks().get(0);
                    vmTypeMetaBuilder.withEphemeralConfig(new VolumeParameterConfig(
                            EPHEMERAL,
                            diskInfo.getSizeInGB().intValue(),
                            diskInfo.getSizeInGB().intValue(),
                            diskInfo.getCount(),
                            diskInfo.getCount()));
                }
                if (getEncryptionSupported(instanceType)) {
                    vmTypeMetaBuilder.withVolumeEncryptionSupport(true);
                }
                VmType vmType = vmTypeWithMeta(instanceType.getInstanceType(), vmTypeMetaBuilder.create(), true);
                awsInstances.add(vmType);
            }
        }
    }

    @VisibleForTesting
    boolean getEncryptionSupported(InstanceTypeInfo instanceTypeInfo) {
        boolean supported = false;
        if (instanceTypeInfo.getEbsInfo() != null) {
            if (instanceTypeInfo.getEbsInfo().getEncryptionSupport() != null) {
                supported = instanceTypeInfo.getEbsInfo().getEncryptionSupport().toLowerCase().equals(SUPPORTED);
            }
        }
        return supported;
    }

    private float getMemory(InstanceTypeInfo instanceType) {
        return (float) instanceType.getMemoryInfo().getSizeInMiB() / ONE_THOUSAND;
    }

    private List<String> getInstanceTypes(List<String> instanceTypes, int i) {
        return instanceTypes.subList(i, (i + SEGMENT) < instanceTypes.size() ? (i + SEGMENT) : instanceTypes.size());
    }

    @Override
    public CloudGateWays gateways(CloudCredential cloudCredential, Region region, Map<String, String> filters) {
        Map<String, Set<CloudGateWay>> resultCloudGateWayMap = new HashMap<>();
        CloudRegions regions = regions(cloudCredential, region, filters, true);

        for (Entry<Region, List<AvailabilityZone>> regionListEntry : regions.getCloudRegions().entrySet()) {
            if (region == null || Strings.isNullOrEmpty(region.value()) || regionListEntry.getKey().value().equals(region.value())) {
                AmazonEc2Client ec2Client = awsClient.createEc2Client(new AwsCredentialView(cloudCredential), regionListEntry.getKey().value());

                DescribeInternetGatewaysRequest describeInternetGatewaysRequest = new DescribeInternetGatewaysRequest();
                DescribeInternetGatewaysResult describeInternetGatewaysResult = ec2Client.describeInternetGateways(describeInternetGatewaysRequest);

                Set<CloudGateWay> gateWays = new HashSet<>();
                for (InternetGateway internetGateway : describeInternetGatewaysResult.getInternetGateways()) {
                    CloudGateWay cloudGateWay = new CloudGateWay();
                    cloudGateWay.setId(internetGateway.getInternetGatewayId());
                    cloudGateWay.setName(internetGateway.getInternetGatewayId());
                    Collection<String> vpcs = new ArrayList<>();
                    for (InternetGatewayAttachment internetGatewayAttachment : internetGateway.getAttachments()) {
                        vpcs.add(internetGatewayAttachment.getVpcId());
                    }
                    Map<String, Object> properties = new HashMap<>();
                    properties.put("attachment", vpcs);
                    cloudGateWay.setProperties(properties);
                    gateWays.add(cloudGateWay);
                }
                for (AvailabilityZone availabilityZone : regionListEntry.getValue()) {
                    resultCloudGateWayMap.put(availabilityZone.value(), gateWays);
                }
            }
        }
        return new CloudGateWays(resultCloudGateWayMap);
    }

    @Override
    public CloudIpPools publicIpPool(CloudCredential cloudCredential, Region region, Map<String, String> filters) {
        return new CloudIpPools();
    }

    @Override
    public CloudAccessConfigs accessConfigs(CloudCredential cloudCredential, Region region, Map<String, String> filters) {
        CloudAccessConfigs cloudAccessConfigs = new CloudAccessConfigs(new HashSet<>());
        AwsCredentialView awsCredentialView = new AwsCredentialView(cloudCredential);
        AmazonIdentityManagementClient client = awsClient.createAmazonIdentityManagement(awsCredentialView);
        String accessConfigType = filters.get(ACCESS_CONFIG_TYPE);
        Set<CloudAccessConfig> cloudAccessConfigSet;
        if (AwsAccessConfigType.ROLE.name().equals(accessConfigType)) {
            cloudAccessConfigSet = getAccessConfigByRole(client);
        } else {
            cloudAccessConfigSet = getAccessConfigByInstanceProfile(client);
        }
        cloudAccessConfigs.getCloudAccessConfigs().addAll(cloudAccessConfigSet);
        return cloudAccessConfigs;
    }

    @Override
    public CloudEncryptionKeys encryptionKeys(CloudCredential cloudCredential, Region region, Map<String, String> filters) {
        String queryFailedMessage = "Could not get encryption keys from Amazon: ";

        CloudEncryptionKeys cloudEncryptionKeys = new CloudEncryptionKeys(new HashSet<>());
        AwsCredentialView awsCredentialView = new AwsCredentialView(cloudCredential);
        AmazonKmsClient client = awsClient.createAWSKMS(awsCredentialView, region.value());
        try {
            ListKeysRequest listKeysRequest = new ListKeysRequest();
            ListKeysResult listKeysResult = client.listKeys(listKeysRequest);
            ListAliasesResult listAliasesResult = client.listAliases(new ListAliasesRequest());

            for (AliasListEntry keyListEntry : listAliasesResult.getAliases()) {
                try {
                    listKeysResult.getKeys().stream()
                            .filter(item -> item.getKeyId().equals(keyListEntry.getTargetKeyId())).findFirst()
                            .ifPresent(item -> {
                                DescribeKeyRequest describeKeyRequest = new DescribeKeyRequest().withKeyId(item.getKeyId());
                                DescribeKeyResult describeKeyResult = client.describeKey(describeKeyRequest);
                                Map<String, Object> meta = new HashMap<>();
                                meta.put("aWSAccountId", describeKeyResult.getKeyMetadata().getAWSAccountId());
                                meta.put("creationDate", describeKeyResult.getKeyMetadata().getCreationDate());
                                meta.put("enabled", describeKeyResult.getKeyMetadata().getEnabled());
                                meta.put("expirationModel", describeKeyResult.getKeyMetadata().getExpirationModel());
                                meta.put("keyManager", describeKeyResult.getKeyMetadata().getKeyManager());
                                meta.put("keyState", describeKeyResult.getKeyMetadata().getKeyState());
                                meta.put("keyUsage", describeKeyResult.getKeyMetadata().getKeyUsage());
                                meta.put("origin", describeKeyResult.getKeyMetadata().getOrigin());
                                meta.put("validTo", describeKeyResult.getKeyMetadata().getValidTo());

                                if (!CloudConstants.AWS.equalsIgnoreCase(describeKeyResult.getKeyMetadata().getKeyManager())) {
                                    CloudEncryptionKey key = new CloudEncryptionKey(
                                            item.getKeyArn(),
                                            describeKeyResult.getKeyMetadata().getKeyId(),
                                            describeKeyResult.getKeyMetadata().getDescription(),
                                            keyListEntry.getAliasName().replace("alias/", ""),
                                            meta);
                                    cloudEncryptionKeys.getCloudEncryptionKeys().add(key);
                                }
                            });
                } catch (AmazonServiceException e) {
                    if (e.getStatusCode() == UNAUTHORIZED) {
                        String policyMessage = "Could not get encryption keys because the user does not have enough permission.";
                        LOGGER.error(policyMessage, e);
                    } else {
                        LOGGER.info(queryFailedMessage, e);
                    }
                } catch (Exception e) {
                    LOGGER.warn(queryFailedMessage, e);
                }
            }
        } catch (AmazonServiceException ase) {
            if (ase.getStatusCode() == UNAUTHORIZED) {
                String policyMessage = "Could not get encryption keys because the user does not have enough permission.";
                LOGGER.error(policyMessage, ase);
                throw new CloudUnauthorizedException(policyMessage, ase);
            } else {
                LOGGER.info(queryFailedMessage, ase);
                throw new CloudConnectorException(queryFailedMessage + ase.getMessage(), ase);
            }
        } catch (Exception e) {
            LOGGER.warn(queryFailedMessage, e);
            throw new CloudConnectorException(queryFailedMessage + e.getMessage(), e);
        }
        return cloudEncryptionKeys;
    }

    @Override
    public CloudNoSqlTables noSqlTables(CloudCredential cloudCredential, Region region, Map<String, String> filters) {
        List<CloudNoSqlTable> noSqlTables = new ArrayList<>();
        AmazonDynamoDBClient dynamoDbClient = getAmazonDynamoDBClient(cloudCredential, region);
        ListTablesRequest listTablesRequest = new ListTablesRequest();
        ListTablesResult listTablesResult = null;
        boolean first = true;
        while (first || !isNullOrEmpty(listTablesResult.getLastEvaluatedTableName())) {
            first = false;
            listTablesRequest.setExclusiveStartTableName(listTablesResult == null ? null : listTablesResult.getLastEvaluatedTableName());
            listTablesResult = dynamoDbClient.listTables(listTablesRequest);
            List<String> partialTableNames = listTablesResult.getTableNames();
            List<CloudNoSqlTable> partialResult = partialTableNames.stream().map(CloudNoSqlTable::new).collect(Collectors.toList());
            noSqlTables.addAll(partialResult);
        }
        return new CloudNoSqlTables(noSqlTables);
    }

    @Override
    public CloudResourceGroups resourceGroups(CloudCredential cloudCredential, Region region, Map<String, String> filters) {
        return new CloudResourceGroups();
    }

    @Override
    public CloudDatabaseServerSslCertificates databaseServerGeneralSslRootCertificates(CloudCredential cloudCredential, Region region) {
        requireNonNull(cloudCredential);
        requireNonNull(region);
        AmazonRdsClient rdsClient = getAmazonRdsClient(cloudCredential, region);
        List<Certificate> certificates = rdsClient.describeCertificates(new DescribeCertificatesRequest());
        Set<CloudDatabaseServerSslCertificate> sslCertificates = certificates.stream()
                .map(Certificate::getCertificateIdentifier)
                .map(id -> new CloudDatabaseServerSslCertificate(CloudDatabaseServerSslCertificateType.ROOT, id))
                .collect(Collectors.toSet());
        return new CloudDatabaseServerSslCertificates(sslCertificates);
    }

    Set<Region> getEnabledRegions() {
        return enabledRegions;
    }

    private AmazonDynamoDBClient getAmazonDynamoDBClient(CloudCredential cloudCredential, Region region) {
        AwsCredentialView awsCredentialView = new AwsCredentialView(cloudCredential);
        return awsClient.createDynamoDbClient(awsCredentialView, region.value());
    }

    private AmazonRdsClient getAmazonRdsClient(CloudCredential cloudCredential, Region region) {
        AwsCredentialView awsCredentialView = new AwsCredentialView(cloudCredential);
        return awsClient.createRdsClient(awsCredentialView, region.value());
    }

    private Set<CloudAccessConfig> getAccessConfigByInstanceProfile(AmazonIdentityManagementClient client) {
        LOGGER.info("Get all Instance profiles from Amazon");
        String queryFailedMessage = "Could not get instance profiles from Amazon: ";
        try {
            boolean finished = false;
            String marker = null;
            Set<InstanceProfile> instanceProfiles = new LinkedHashSet<>();
            while (!finished) {
                ListInstanceProfilesRequest listInstanceProfilesRequest = new ListInstanceProfilesRequest();
                listInstanceProfilesRequest.setMaxItems(fetchMaxItems);
                if (isNotEmpty(marker)) {
                    listInstanceProfilesRequest.setMarker(marker);
                }
                LOGGER.debug("About to fetch instance profiles...");
                ListInstanceProfilesResult listInstanceProfilesResult = client.listInstanceProfiles(listInstanceProfilesRequest);
                List<InstanceProfile> fetchedInstanceProfiles = listInstanceProfilesResult.getInstanceProfiles();
                instanceProfiles.addAll(fetchedInstanceProfiles);
                if (listInstanceProfilesResult.isTruncated()) {
                    marker = listInstanceProfilesResult.getMarker();
                } else {
                    finished = true;
                }
            }
            LOGGER.debug("The total of {} instance profile(s) has fetched.", instanceProfiles.size());
            return instanceProfiles.stream().map(this::instanceProfileToCloudAccessConfig).collect(Collectors.toSet());
        } catch (AmazonServiceException ase) {
            if (ase.getStatusCode() == UNAUTHORIZED) {
                LOGGER.error("Could not get instance profiles because the user does not have enough permission.", ase);
                throw new CloudUnauthorizedException(ase.getMessage(), ase);
            } else {
                LOGGER.info(queryFailedMessage, ase);
                throw new CloudConnectorException(ase.getMessage(), ase);
            }
        } catch (Exception e) {
            LOGGER.warn(queryFailedMessage, e);
            throw new CloudConnectorException(queryFailedMessage + e.getMessage(), e);
        }
    }

    private Set<CloudAccessConfig> getAccessConfigByRole(AmazonIdentityManagementClient client) {
        LOGGER.info("Get all Roles from Amazon");
        String queryFailedMessage = "Could not get roles from Amazon: ";
        try {
            boolean finished = false;
            String marker = null;
            List<Role> roles = new LinkedList<>();
            while (!finished) {
                ListRolesRequest listRolesRequest = new ListRolesRequest();
                listRolesRequest.setMaxItems(fetchMaxItems);
                if (isNotEmpty(marker)) {
                    listRolesRequest.setMarker(marker);
                }
                LOGGER.debug("About to fetch roles...");
                ListRolesResult listRolesResult = client.listRoles(listRolesRequest);
                roles.addAll(listRolesResult.getRoles());
                if (listRolesResult.isTruncated()) {
                    marker = listRolesResult.getMarker();
                } else {
                    finished = true;
                }
            }
            return roles.stream().map(this::roleToCloudAccessConfig).collect(Collectors.toSet());
        } catch (AmazonServiceException ase) {
            if (ase.getStatusCode() == UNAUTHORIZED) {
                String policyMessage = "Could not get roles because the user does not have enough permission. ";
                LOGGER.error(policyMessage + ase.getMessage(), ase);
                throw new CloudUnauthorizedException(ase.getErrorMessage(), ase);
            } else {
                LOGGER.info(queryFailedMessage + ase.getMessage(), ase);
                throw new CloudConnectorException(ase.getMessage(), ase);
            }
        } catch (Exception e) {
            LOGGER.warn(queryFailedMessage + e.getMessage(), e);
            throw new CloudConnectorException(e.getMessage(), e);
        }
    }

    private CloudAccessConfig roleToCloudAccessConfig(Role role) {
        Map<String, Object> properties = getCloudAccessConfigProperties(role.getArn(), role.getCreateDate().toString(), role.getArn());
        return new CloudAccessConfig(role.getRoleName(), role.getRoleId(), properties);
    }

    private CloudAccessConfig instanceProfileToCloudAccessConfig(InstanceProfile instanceProfile) {
        String roleName = instanceProfile.getArn();
        if (!instanceProfile.getRoles().isEmpty()) {
            roleName = instanceProfile.getRoles().get(0).getArn();
        }
        Map<String, Object> properties = getCloudAccessConfigProperties(instanceProfile.getArn(), instanceProfile.getCreateDate().toString(), roleName);
        return new CloudAccessConfig(instanceProfile.getInstanceProfileName(), instanceProfile.getInstanceProfileId(), properties);
    }

    private Map<String, Object> getCloudAccessConfigProperties(String arn, String creationDate, String roleArn) {
        Map<String, Object> properties = new HashMap<>();
        properties.put("arn", arn);
        properties.put("creationDate", creationDate);
        properties.put("roleArn", roleArn);
        return properties;
    }

}
