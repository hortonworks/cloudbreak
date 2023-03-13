package com.sequenceiq.cloudbreak.cloud.aws.common;

import static com.sequenceiq.cloudbreak.cloud.aws.common.DistroxEnabledInstanceTypes.ENABLED_TYPES;
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
import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang3.StringUtils.isEmpty;
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
import java.util.Objects;
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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.cloud.CloudParameterConst;
import com.sequenceiq.cloudbreak.cloud.PlatformResources;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonDynamoDBClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonEc2Client;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonIdentityManagementClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonKmsClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonRdsClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.util.AwsPageCollector;
import com.sequenceiq.cloudbreak.cloud.aws.common.view.AwsCredentialView;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
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
import com.sequenceiq.cloudbreak.cloud.model.ExtendedCloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStoreMetadata;
import com.sequenceiq.cloudbreak.cloud.model.Location;
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
import com.sequenceiq.cloudbreak.cloud.model.dns.CloudPrivateDnsZones;
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

import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.dynamodb.model.ListTablesRequest;
import software.amazon.awssdk.services.dynamodb.model.ListTablesResponse;
import software.amazon.awssdk.services.ec2.model.DescribeAvailabilityZonesRequest;
import software.amazon.awssdk.services.ec2.model.DescribeInstanceTypeOfferingsRequest;
import software.amazon.awssdk.services.ec2.model.DescribeInstanceTypesRequest;
import software.amazon.awssdk.services.ec2.model.DescribeInstanceTypesResponse;
import software.amazon.awssdk.services.ec2.model.DescribeInternetGatewaysRequest;
import software.amazon.awssdk.services.ec2.model.DescribeInternetGatewaysResponse;
import software.amazon.awssdk.services.ec2.model.DescribeKeyPairsRequest;
import software.amazon.awssdk.services.ec2.model.DescribeRegionsRequest;
import software.amazon.awssdk.services.ec2.model.DescribeRegionsResponse;
import software.amazon.awssdk.services.ec2.model.DescribeRouteTablesRequest;
import software.amazon.awssdk.services.ec2.model.DescribeSecurityGroupsRequest;
import software.amazon.awssdk.services.ec2.model.DescribeSubnetsRequest;
import software.amazon.awssdk.services.ec2.model.DescribeSubnetsResponse;
import software.amazon.awssdk.services.ec2.model.DescribeVpcsRequest;
import software.amazon.awssdk.services.ec2.model.DescribeVpcsResponse;
import software.amazon.awssdk.services.ec2.model.DiskInfo;
import software.amazon.awssdk.services.ec2.model.Ec2Exception;
import software.amazon.awssdk.services.ec2.model.Filter;
import software.amazon.awssdk.services.ec2.model.InstanceStorageInfo;
import software.amazon.awssdk.services.ec2.model.InstanceType;
import software.amazon.awssdk.services.ec2.model.InstanceTypeInfo;
import software.amazon.awssdk.services.ec2.model.InstanceTypeOffering;
import software.amazon.awssdk.services.ec2.model.InternetGateway;
import software.amazon.awssdk.services.ec2.model.InternetGatewayAttachment;
import software.amazon.awssdk.services.ec2.model.KeyPairInfo;
import software.amazon.awssdk.services.ec2.model.LocationType;
import software.amazon.awssdk.services.ec2.model.RouteTable;
import software.amazon.awssdk.services.ec2.model.SecurityGroup;
import software.amazon.awssdk.services.ec2.model.Subnet;
import software.amazon.awssdk.services.ec2.model.Tag;
import software.amazon.awssdk.services.ec2.model.Vpc;
import software.amazon.awssdk.services.iam.model.InstanceProfile;
import software.amazon.awssdk.services.iam.model.ListInstanceProfilesRequest;
import software.amazon.awssdk.services.iam.model.ListInstanceProfilesResponse;
import software.amazon.awssdk.services.iam.model.ListRolesRequest;
import software.amazon.awssdk.services.iam.model.ListRolesResponse;
import software.amazon.awssdk.services.iam.model.Role;
import software.amazon.awssdk.services.kms.model.AliasListEntry;
import software.amazon.awssdk.services.kms.model.DescribeKeyRequest;
import software.amazon.awssdk.services.kms.model.DescribeKeyResponse;
import software.amazon.awssdk.services.kms.model.KeyListEntry;
import software.amazon.awssdk.services.kms.model.KeyMetadata;
import software.amazon.awssdk.services.kms.model.ListAliasesRequest;
import software.amazon.awssdk.services.kms.model.ListAliasesResponse;
import software.amazon.awssdk.services.kms.model.ListKeysRequest;
import software.amazon.awssdk.services.kms.model.ListKeysResponse;
import software.amazon.awssdk.services.rds.model.Certificate;
import software.amazon.awssdk.services.rds.model.DescribeCertificatesRequest;

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

    private static final int ONE_THOUSAND_TWENTY_FOUR = 1024;

    @Inject
    private CommonAwsClient awsClient;

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

    @Inject
    private AwsPageCollector awsPageCollector;

    @Value("${cb.aws.vm.parameter.definition.path:}")
    private String awsVmParameterDefinitionPath;

    @Value("${cb.aws.disabled.instance.types:}")
    private List<String> disabledInstanceTypes;

    @Value("${cb.aws.distrox.enabled.instance.types:" + ENABLED_TYPES + "}")
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
                                regionCoordinateSpecification.isK8sSupported(),
                                regionCoordinateSpecification.getEntitlements()));
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
    public CloudNetworks networks(ExtendedCloudCredential cloudCredential, Region region, Map<String, String> filters) {
        AmazonEc2Client ec2Client = awsClient.createEc2Client(new AwsCredentialView(cloudCredential), region.value());
        try {
            LOGGER.debug("Describing route tables in region {}", region.getRegionName());
            List<RouteTable> allRouteTables = awsPageCollector.getAllRouteTables(ec2Client, DescribeRouteTablesRequest.builder().build());
            DescribeVpcsRequest describeVpcsRequest = getDescribeVpcsRequestWithFilters(filters);
            Set<CloudNetwork> cloudNetworks = new HashSet<>();

            DescribeVpcsResponse describeVpcsResponse = null;
            boolean first = true;
            while (first || !isEmpty(describeVpcsResponse.nextToken())) {
                LOGGER.debug("Getting VPC list in region {}{}", region.getRegionName(), first ? "" : " (continuation)");
                first = false;
                describeVpcsRequest = describeVpcsRequest.toBuilder().nextToken(describeVpcsResponse == null ? null : describeVpcsResponse.nextToken()).build();
                describeVpcsResponse = ec2Client.describeVpcs(describeVpcsRequest);
                Set<CloudNetwork> partialNetworks = getCloudNetworks(ec2Client, allRouteTables, describeVpcsResponse);
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
        DescribeVpcsRequest.Builder describeVpcsRequestBuilder = DescribeVpcsRequest.builder();
        // If the filtervalue is provided then we should filter only for those vpc
        if (!Strings.isNullOrEmpty(filter.getVpcId())) {
            describeVpcsRequestBuilder.vpcIds(filter.getVpcId());
        }
        return describeVpcsRequestBuilder.build();
    }

    private Set<CloudNetwork> getCloudNetworks(AmazonEc2Client ec2Client, List<RouteTable> describeRouteTablesResponse,
            DescribeVpcsResponse describeVpcsResponse) {

        Set<CloudNetwork> cloudNetworks = new HashSet<>();
        LOGGER.debug("Processing VPCs");
        for (Vpc vpc : describeVpcsResponse.vpcs()) {
            List<Subnet> awsSubnets = getSubnets(ec2Client, vpc);
            Set<CloudSubnet> subnets = convertAwsSubnetsToCloudSubnets(describeRouteTablesResponse, awsSubnets);

            Map<String, Object> properties = prepareNetworkProperties(vpc);
            Optional<String> name = getName(vpc.tags());
            if (name.isPresent()) {
                cloudNetworks.add(new CloudNetwork(name.get(), vpc.vpcId(), subnets, properties));
            } else {
                cloudNetworks.add(new CloudNetwork(vpc.vpcId(), vpc.vpcId(), subnets, properties));
            }
        }
        return cloudNetworks;
    }

    private List<Subnet> getSubnets(AmazonEc2Client ec2Client, Vpc vpc) {
        List<Subnet> awsSubnets = new ArrayList<>();
        DescribeSubnetsResponse describeSubnetsResponse = null;
        do {
            LOGGER.debug("Describing subnets for VPC {}{}", vpc.vpcId(), describeSubnetsResponse == null ? "" : " (continuation)");
            DescribeSubnetsRequest describeSubnetsRequest = createSubnetsDescribeRequest(vpc, describeSubnetsResponse == null
                    ? null
                    : describeSubnetsResponse.nextToken());
            describeSubnetsResponse = ec2Client.describeSubnets(describeSubnetsRequest);
            describeSubnetsResponse.subnets().stream()
                    .filter(subnet -> enabledAvailabilityZones.contains(availabilityZone(subnet.availabilityZone())))
                    .forEach(awsSubnets::add);
        } while (!isEmpty(describeSubnetsResponse.nextToken()));
        return awsSubnets;
    }

    private Set<CloudSubnet> convertAwsSubnetsToCloudSubnets(List<RouteTable> describeRouteTablesResponse, List<Subnet> awsSubnets) {
        Set<CloudSubnet> subnets = new HashSet<>();
        for (Subnet subnet : awsSubnets) {
            boolean hasInternetGateway = awsSubnetIgwExplorer.hasInternetGatewayOfSubnet(describeRouteTablesResponse, subnet.subnetId(), subnet.vpcId());
            LOGGER.info("The subnet {} has internetGateway value is '{}'", subnet, hasInternetGateway);
            Optional<String> subnetName = getName(subnet.tags());
            subnets.add(
                    new CloudSubnet(
                            subnet.subnetId(),
                            subnetName.orElse(subnet.subnetId()),
                            subnet.availabilityZone(),
                            subnet.cidrBlock(),
                            !hasInternetGateway,
                            subnet.mapPublicIpOnLaunch(),
                            hasInternetGateway,
                            hasInternetGateway ? PUBLIC : PRIVATE)
            );
        }
        return subnets;
    }

    private Map<String, Object> prepareNetworkProperties(Vpc vpc) {
        Map<String, Object> properties = new HashMap<>();
        properties.put("cidrBlock", vpc.cidrBlock());
        properties.put("default", vpc.isDefault());
        properties.put("dhcpOptionsId", vpc.dhcpOptionsId());
        properties.put("instanceTenancy", vpc.instanceTenancy());
        properties.put("state", vpc.state());
        return properties;
    }

    private Optional<String> getName(List<Tag> tags) {
        for (Tag tag : tags) {
            if ("Name".equals(tag.key())) {
                return Optional.ofNullable(tag.value());
            }
        }
        return Optional.empty();
    }

    private DescribeSubnetsRequest createSubnetsDescribeRequest(Vpc vpc, String nextToken) {
        return DescribeSubnetsRequest.builder()
                .filters(Filter.builder().name("vpc-id").values(singletonList(vpc.vpcId())).build())
                .nextToken(nextToken)
                .build();
    }

    @Override
    public CloudSshKeys sshKeys(ExtendedCloudCredential cloudCredential, Region region, Map<String, String> filters) {
        Map<String, Set<CloudSshKey>> result = new HashMap<>();
        if (region != null && !Strings.isNullOrEmpty(region.value())) {
            CloudRegions regions = regions(cloudCredential, region, new HashMap<>(), true);
            for (Region actualRegion : regions.getCloudRegions().keySet()) {
                // If region is provided then should filter for those region
                if (regionMatch(actualRegion, region)) {
                    Set<CloudSshKey> cloudSshKeys = new HashSet<>();
                    AmazonEc2Client ec2Client = awsClient.createEc2Client(new AwsCredentialView(cloudCredential), actualRegion.value());

                    //create sshkey filter view
                    PlatformResourceSshKeyFilterView filter = new PlatformResourceSshKeyFilterView(filters);

                    DescribeKeyPairsRequest.Builder describeKeyPairsRequestBuilder = DescribeKeyPairsRequest.builder();

                    // If the filtervalue is provided then we should filter only for those securitygroups
                    if (!Strings.isNullOrEmpty(filter.getKeyName())) {
                        describeKeyPairsRequestBuilder.keyNames(filter.getKeyName());
                    }

                    for (KeyPairInfo keyPairInfo : ec2Client.describeKeyPairs(describeKeyPairsRequestBuilder.build()).keyPairs()) {
                        Map<String, Object> properties = new HashMap<>();
                        properties.put("fingerPrint", keyPairInfo.keyFingerprint());
                        cloudSshKeys.add(new CloudSshKey(keyPairInfo.keyName(), properties));
                    }
                    result.put(actualRegion.value(), cloudSshKeys);
                }
            }
        }
        return new CloudSshKeys(result);
    }

    @Override
    public CloudSecurityGroups securityGroups(ExtendedCloudCredential cloudCredential, Region region, Map<String, String> filters) {
        Map<String, Set<CloudSecurityGroup>> result = new HashMap<>();
        Set<CloudSecurityGroup> cloudSecurityGroups = new HashSet<>();
        AmazonEc2Client ec2Client = awsClient.createEc2Client(new AwsCredentialView(cloudCredential), region.value());

        //create securitygroup filter view
        PlatformResourceSecurityGroupFilterView filter = new PlatformResourceSecurityGroupFilterView(filters);

        DescribeSecurityGroupsRequest.Builder describeSecurityGroupsRequestBuilder = DescribeSecurityGroupsRequest.builder();
        // If the filtervalue is provided then we should filter only for those securitygroups
        if (!Strings.isNullOrEmpty(filter.getVpcId())) {
            describeSecurityGroupsRequestBuilder.filters(Filter.builder().name("vpc-id").values(singletonList(filter.getVpcId())).build());
        }
        if (!Strings.isNullOrEmpty(filter.getGroupId())) {
            describeSecurityGroupsRequestBuilder.groupIds(filter.getGroupId());
        }
        if (!Strings.isNullOrEmpty(filter.getGroupName())) {
            describeSecurityGroupsRequestBuilder.groupNames(filter.getGroupName());
        }

        for (SecurityGroup securityGroup : fetchSecurityGroups(ec2Client, describeSecurityGroupsRequestBuilder.build())) {
            Map<String, Object> properties = new HashMap<>();
            properties.put("vpcId", securityGroup.vpcId());
            properties.put("description", securityGroup.description());
            properties.put("ipPermissions", securityGroup.ipPermissions());
            properties.put("ipPermissionsEgress", securityGroup.ipPermissionsEgress());
            cloudSecurityGroups.add(new CloudSecurityGroup(securityGroup.groupName(), securityGroup.groupId(), properties));
        }
        result.put(region.value(), cloudSecurityGroups);
        return new CloudSecurityGroups(result);
    }

    private List<SecurityGroup> fetchSecurityGroups(AmazonEc2Client ec2Client, DescribeSecurityGroupsRequest describeSecurityGroupsRequest) {
        try {
            return ec2Client.describeSecurityGroups(describeSecurityGroupsRequest).securityGroups();
        } catch (Ec2Exception e) {
            if (e.statusCode() == HttpStatus.BAD_REQUEST.value() || e.statusCode() == HttpStatus.NOT_FOUND.value()) {
                throw new PermanentlyFailedException(e.awsErrorDetails().errorMessage(), e);
            } else {
                throw e;
            }
        }
    }

    @Override
    @Cacheable(cacheNames = "cloudResourceRegionCache", key = "{ #cloudCredential?.id, #availabilityZonesNeeded }")
    public CloudRegions regions(ExtendedCloudCredential cloudCredential, Region region, Map<String, String> filters,
            boolean availabilityZonesNeeded) {
        AmazonEc2Client ec2Client = awsClient.createEc2Client(new AwsCredentialView(cloudCredential));
        Map<Region, List<AvailabilityZone>> regionListMap = new HashMap<>();
        Map<Region, String> displayNames = new HashMap<>();
        Map<Region, Coordinate> coordinates = new HashMap<>();

        DescribeRegionsResponse describeRegionsResponse = describeRegionsResponse(ec2Client);
        String defaultRegion = awsDefaultZoneProvider.getDefaultZone(cloudCredential);

        for (software.amazon.awssdk.services.ec2.model.Region awsRegion : describeRegionsResponse.regions()) {
            if (!enabledRegions.contains(region(awsRegion.regionName()))) {
                continue;
            }
            if (region == null || Strings.isNullOrEmpty(region.value()) || awsRegion.regionName().equals(region.value())) {
                try {
                    fetchAZsIfNeeded(availabilityZonesNeeded, regionListMap, awsRegion, cloudCredential);
                } catch (Ec2Exception e) {
                    LOGGER.info("Failed to retrieve AZ from Region: {}!", awsRegion.regionName(), e);
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
            software.amazon.awssdk.services.ec2.model.Region awsRegion, CloudCredential cloudCredential) {
        List<AvailabilityZone> collectedAZs = new ArrayList<>();
        if (availabilityZonesNeeded) {
            DescribeAvailabilityZonesRequest describeAvailabilityZonesRequest = getDescribeAvailabilityZonesRequest(awsRegion);
            LOGGER.debug("Describing AZs in region {}", awsRegion.regionName());
            List<software.amazon.awssdk.services.ec2.model.AvailabilityZone> availabilityZones
                    = awsAvailabilityZoneProvider.describeAvailabilityZones(cloudCredential, describeAvailabilityZonesRequest, awsRegion);
            availabilityZones.stream()
                    .map(software.amazon.awssdk.services.ec2.model.AvailabilityZone::zoneName)
                    .map(AvailabilityZone::availabilityZone)
                    .filter(enabledAvailabilityZones::contains)
                    .forEach(collectedAZs::add);
        }
        regionListMap.put(region(awsRegion.regionName()), collectedAZs);
    }

    private DescribeAvailabilityZonesRequest getDescribeAvailabilityZonesRequest(software.amazon.awssdk.services.ec2.model.Region awsRegion) {
        return DescribeAvailabilityZonesRequest.builder()
                .filters(Filter.builder()
                        .name("region-name")
                        .values(awsRegion.regionName())
                        .build())
                .build();
    }

    public void addDisplayName(Map<Region, String> displayNames, software.amazon.awssdk.services.ec2.model.Region awsRegion) {
        DisplayName displayName = regionDisplayNames.get(region(awsRegion.regionName()));
        if (displayName == null || Strings.isNullOrEmpty(displayName.value())) {
            displayNames.put(region(awsRegion.regionName()), awsRegion.regionName());
        } else {
            displayNames.put(region(awsRegion.regionName()), displayName.value());
        }
    }

    public void addCoordinate(Map<Region, Coordinate> coordinates, software.amazon.awssdk.services.ec2.model.Region awsRegion) {
        Coordinate coordinate = regionCoordinates.get(region(awsRegion.regionName()));
        if (coordinate == null || coordinate.getLongitude() == null || coordinate.getLatitude() == null) {
            LOGGER.warn("Unregistered region with location coordinates on aws side: {} using default California", awsRegion.regionName());
            coordinates.put(region(awsRegion.regionName()), Coordinate.defaultCoordinate());
        } else {
            coordinates.put(region(awsRegion.regionName()), coordinate);
        }
    }

    private DescribeRegionsResponse describeRegionsResponse(AmazonEc2Client ec2Client) {
        LOGGER.debug("Getting regions");
        try {
            DescribeRegionsRequest describeRegionsRequest = DescribeRegionsRequest.builder().build();
            return ec2Client.describeRegions(describeRegionsRequest);
        } catch (Ec2Exception e) {
            LOGGER.info("Failed to retrieve regions!", e);
        }
        return DescribeRegionsResponse.builder().build();
    }

    @Override
    @Cacheable(cacheNames = "cloudResourceVmTypeCache", key = "#cloudCredential?.id + #region.getRegionName()")
    public CloudVmTypes virtualMachines(ExtendedCloudCredential cloudCredential, Region region, Map<String, String> filters) {
        return getCloudVmTypes(cloudCredential, region, filters, enabledInstanceTypeFilter, false);
    }

    @Override
    @Cacheable(cacheNames = "cloudResourceVmTypeCache", key = "#cloudCredential?.id + #region.getRegionName() + 'distrox'")
    public CloudVmTypes virtualMachinesForDistroX(ExtendedCloudCredential cloudCredential, Region region, Map<String, String> filters) {
        if (restrictInstanceTypes) {
            return getCloudVmTypes(cloudCredential, region, filters, enabledDistroxInstanceTypeFilter, true);
        } else {
            return getCloudVmTypes(cloudCredential, region, filters, enabledInstanceTypeFilter, true);
        }
    }

    private CloudVmTypes getCloudVmTypes(ExtendedCloudCredential cloudCredential, Region region, Map<String, String> filters,
            Predicate<VmType> enabledInstanceTypeFilter, boolean enableMinimalHardwareFilter) {
        Map<String, Set<VmType>> cloudVmResponses = new HashMap<>();
        Map<String, VmType> defaultCloudVmResponses = new HashMap<>();
        if (region != null && !Strings.isNullOrEmpty(region.value())) {
            CloudRegions regions = regions(cloudCredential, region, filters, true);
            AwsCredentialView awsCredentialView = new AwsCredentialView(cloudCredential);
            AmazonEc2Client ec2Client = awsClient.createEc2Client(awsCredentialView, region.getRegionName());
            List<InstanceType> instanceTypes = ec2Client
                    .describeInstanceTypeOfferings(getOfferingsRequest(region))
                    .instanceTypeOfferings()
                    .stream()
                    .map(InstanceTypeOffering::instanceType)
                    .filter(instanceType -> InstanceType.UNKNOWN_TO_SDK_VERSION != instanceType)
                    .collect(Collectors.toList());

            Set<VmType> awsInstances = new HashSet<>();
            for (int actualSegment = 0; actualSegment < instanceTypes.size(); actualSegment += SEGMENT) {
                DescribeInstanceTypesRequest request = DescribeInstanceTypesRequest.builder()
                        .filters(Filter.builder()
                                .name("processor-info.supported-architecture")
                                .values("x86_64")
                                .build())
                        .instanceTypes(getInstanceTypes(instanceTypes, actualSegment))
                        .build();
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
        }
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
        return DescribeInstanceTypeOfferingsRequest.builder()
                .locationType(LocationType.REGION)
                .filters(Filter.builder().name("location").values(region.getRegionName()).build())
                .maxResults(MAX_RESULTS)
                .build();
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

    private void getVmTypesWithAwsCall(Set<VmType> awsInstances, DescribeInstanceTypesResponse describeInstanceTypesResponse) {
        for (InstanceTypeInfo instanceType : describeInstanceTypesResponse.instanceTypes()) {
            if (!instanceType.bareMetal()) {
                VmTypeMetaBuilder vmTypeMetaBuilder = VmTypeMetaBuilder.builder()
                        .withCpuAndMemory(instanceType.vCpuInfo().defaultVCpus(), getMemory(instanceType))
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
                if (instanceType.instanceStorageSupported()) {
                    InstanceStorageInfo instanceStorageInfo = instanceType.instanceStorageInfo();
                    DiskInfo diskInfo = instanceStorageInfo.disks().get(0);
                    vmTypeMetaBuilder.withEphemeralConfig(new VolumeParameterConfig(
                            EPHEMERAL,
                            diskInfo.sizeInGB().intValue(),
                            diskInfo.sizeInGB().intValue(),
                            diskInfo.count(),
                            diskInfo.count()));
                }
                if (getEncryptionSupported(instanceType)) {
                    vmTypeMetaBuilder.withVolumeEncryptionSupport(true);
                }
                VmType vmType = vmTypeWithMeta(instanceType.instanceTypeAsString(), vmTypeMetaBuilder.create(), true);
                awsInstances.add(vmType);
            }
        }
    }

    @VisibleForTesting
    boolean getEncryptionSupported(InstanceTypeInfo instanceTypeInfo) {
        boolean supported = false;
        if (instanceTypeInfo.ebsInfo() != null) {
            if (instanceTypeInfo.ebsInfo().encryptionSupport() != null) {
                supported = instanceTypeInfo.ebsInfo().encryptionSupportAsString().equalsIgnoreCase(SUPPORTED);
            }
        }
        return supported;
    }

    private float getMemory(InstanceTypeInfo instanceType) {
        return (float) instanceType.memoryInfo().sizeInMiB() / ONE_THOUSAND_TWENTY_FOUR;
    }

    private List<InstanceType> getInstanceTypes(List<InstanceType> instanceTypes, int i) {
        return instanceTypes.subList(i, (i + SEGMENT) < instanceTypes.size() ? (i + SEGMENT) : instanceTypes.size());
    }

    @Override
    public CloudGateWays gateways(ExtendedCloudCredential cloudCredential, Region region, Map<String, String> filters) {
        Map<String, Set<CloudGateWay>> resultCloudGateWayMap = new HashMap<>();
        if (region != null && !Strings.isNullOrEmpty(region.value())) {
            CloudRegions regions = regions(cloudCredential, region, filters, true);
            for (Entry<Region, List<AvailabilityZone>> regionListEntry : regions.getCloudRegions().entrySet()) {
                if (regionListEntry.getKey().value().equals(region.value())) {
                    AmazonEc2Client ec2Client = awsClient.createEc2Client(new AwsCredentialView(cloudCredential), regionListEntry.getKey().value());

                    DescribeInternetGatewaysRequest describeInternetGatewaysRequest = DescribeInternetGatewaysRequest.builder().build();
                    DescribeInternetGatewaysResponse describeInternetGatewaysResponse = ec2Client.describeInternetGateways(describeInternetGatewaysRequest);

                    Set<CloudGateWay> gateWays = new HashSet<>();
                    for (InternetGateway internetGateway : describeInternetGatewaysResponse.internetGateways()) {
                        CloudGateWay cloudGateWay = new CloudGateWay();
                        cloudGateWay.setId(internetGateway.internetGatewayId());
                        cloudGateWay.setName(internetGateway.internetGatewayId());
                        Collection<String> vpcs = new ArrayList<>();
                        for (InternetGatewayAttachment internetGatewayAttachment : internetGateway.attachments()) {
                            vpcs.add(internetGatewayAttachment.vpcId());
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
        }
        return new CloudGateWays(resultCloudGateWayMap);
    }

    @Override
    public CloudIpPools publicIpPool(ExtendedCloudCredential cloudCredential, Region region, Map<String, String> filters) {
        return new CloudIpPools();
    }

    @Override
    public CloudAccessConfigs accessConfigs(ExtendedCloudCredential cloudCredential, Region region, Map<String, String> filters) {
        CloudAccessConfigs cloudAccessConfigs = new CloudAccessConfigs(new HashSet<>());
        AwsCredentialView awsCredentialView = new AwsCredentialView(cloudCredential);
        AmazonIdentityManagementClient client = awsClient.createAmazonIdentityManagement(awsCredentialView);
        String accessConfigType = filters.get(CloudParameterConst.ACCESS_CONFIG_TYPE);
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
    public CloudEncryptionKeys encryptionKeys(ExtendedCloudCredential cloudCredential, Region region, Map<String, String> filters) {
        String queryFailedMessage = "Could not get encryption keys from Amazon: ";

        CloudEncryptionKeys cloudEncryptionKeys = new CloudEncryptionKeys(new HashSet<>());
        AwsCredentialView awsCredentialView = new AwsCredentialView(cloudCredential);
        AmazonKmsClient client = awsClient.createAWSKMS(awsCredentialView, region.value());

        try {
            List<KeyListEntry> listKeyResult = getListKeyResult(client);
            List<AliasListEntry> listAliasesResult = getListAliasResult(client);
            for (AliasListEntry aliasListEntry : listAliasesResult) {
                String targetKeyId = aliasListEntry.targetKeyId();
                try {
                    listKeyResult.stream()
                            .filter(item -> item.keyId().equals(targetKeyId)).findFirst()
                            .ifPresent(item -> {
                                DescribeKeyRequest describeKeyRequest = DescribeKeyRequest.builder().keyId(item.keyId()).build();
                                DescribeKeyResponse describeKeyResponse = client.describeKey(describeKeyRequest);
                                KeyMetadata keyMetaData = describeKeyResponse.keyMetadata();
                                Map<String, Object> meta = new HashMap<>();
                                meta.put("awsAccountId", keyMetaData.awsAccountId());
                                meta.put("creationDate", keyMetaData.creationDate());
                                meta.put("enabled", keyMetaData.enabled());
                                meta.put("expirationModel", keyMetaData.expirationModel());
                                meta.put("keyManager", keyMetaData.keyManager());
                                meta.put("keyState", keyMetaData.keyState());
                                meta.put("keyUsage", keyMetaData.keyUsage());
                                meta.put("origin", keyMetaData.origin());
                                meta.put("validTo", keyMetaData.validTo());

                                if (keyMetaData.keyManager() == null || !CloudConstants.AWS.equalsIgnoreCase(keyMetaData.keyManager().name())) {
                                    CloudEncryptionKey key = new CloudEncryptionKey(
                                            item.keyArn(),
                                            keyMetaData.keyId(),
                                            keyMetaData.description(),
                                            aliasListEntry.aliasName().replace("alias/", ""),
                                            meta);
                                    cloudEncryptionKeys.getCloudEncryptionKeys().add(key);
                                }
                            });
                } catch (AwsServiceException e) {
                    if (e.awsErrorDetails().sdkHttpResponse().statusCode() == UNAUTHORIZED) {
                        String policyMessage = "Could not fetch the encryption keys since the user does not have enough " +
                                "permission to perform the DescribeKey operation.";
                        LOGGER.error(policyMessage, e);
                    } else {
                        LOGGER.info(queryFailedMessage, e);
                    }
                } catch (Exception e) {
                    LOGGER.warn(queryFailedMessage, e);
                }
            }
        } catch (AwsServiceException ase) {
            if (ase.awsErrorDetails().sdkHttpResponse().statusCode() == UNAUTHORIZED) {
                String policyMessage = "Could not fetch the encryption keys since the user does not have enough permission to perform the ListKeys operation.";
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

    private List<KeyListEntry> getListKeyResult(AmazonKmsClient client) {
        String nextMarker = null;
        List<KeyListEntry> kmsKeys = new ArrayList<>();
        do {
            ListKeysRequest listKeysRequest = ListKeysRequest.builder().marker(nextMarker).build();
            ListKeysResponse listKeysResponse = client.listKeys(listKeysRequest);
            kmsKeys.addAll(listKeysResponse.keys());
            nextMarker = listKeysResponse.nextMarker();
        } while (nextMarker != null);
        return kmsKeys;
    }

    private List<AliasListEntry> getListAliasResult(AmazonKmsClient client) {
        String aliasNextMarker = null;
        List<AliasListEntry> aliasListEntries = new ArrayList<>();
        do {
            ListAliasesRequest listAliasesRequest = ListAliasesRequest.builder().marker(aliasNextMarker).build();
            ListAliasesResponse listAliasesResponse = client.listAliases(listAliasesRequest);
            aliasListEntries.addAll(listAliasesResponse.aliases());
            aliasNextMarker = listAliasesResponse.nextMarker();
        } while (aliasNextMarker != null);
        return aliasListEntries;
    }

    @Override
    public CloudNoSqlTables noSqlTables(ExtendedCloudCredential cloudCredential, Region region, Map<String, String> filters) {
        List<CloudNoSqlTable> noSqlTables = new ArrayList<>();
        AmazonDynamoDBClient dynamoDbClient = getAmazonDynamoDBClient(cloudCredential, region);
        ListTablesRequest listTablesRequest = ListTablesRequest.builder().build();
        ListTablesResponse listTablesResponse = null;
        boolean first = true;
        while (first || isNotEmpty(listTablesResponse.lastEvaluatedTableName())) {
            first = false;

            listTablesRequest = listTablesRequest.toBuilder()
                    .exclusiveStartTableName(listTablesResponse == null ? null : listTablesResponse.lastEvaluatedTableName())
                    .build();
            listTablesResponse = dynamoDbClient.listTables(listTablesRequest);
            List<String> partialTableNames = listTablesResponse.tableNames();
            List<CloudNoSqlTable> partialResponse = partialTableNames.stream().map(CloudNoSqlTable::new).collect(Collectors.toList());
            noSqlTables.addAll(partialResponse);
        }
        return new CloudNoSqlTables(noSqlTables);
    }

    @Override
    public CloudResourceGroups resourceGroups(ExtendedCloudCredential cloudCredential, Region region, Map<String, String> filters) {
        return new CloudResourceGroups();
    }

    @Override
    public CloudPrivateDnsZones privateDnsZones(ExtendedCloudCredential cloudCredential, Map<String, String> filters) {
        return new CloudPrivateDnsZones();
    }

    @Override
    public CloudDatabaseServerSslCertificates databaseServerGeneralSslRootCertificates(CloudCredential cloudCredential, Region region) {
        requireNonNull(cloudCredential);
        requireNonNull(region);
        AmazonRdsClient rdsClient = getAmazonRdsClient(cloudCredential, region);
        List<Certificate> certificates = rdsClient.describeCertificates(DescribeCertificatesRequest.builder().build());
        Set<CloudDatabaseServerSslCertificate> sslCertificates = certificates.stream()
                .map(Certificate::certificateIdentifier)
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
                ListInstanceProfilesRequest.Builder listInstanceProfilesRequestBuilder = ListInstanceProfilesRequest.builder().maxItems(fetchMaxItems);
                if (isNotEmpty(marker)) {
                    listInstanceProfilesRequestBuilder.marker(marker);
                }
                LOGGER.debug("About to fetch instance profiles...");
                ListInstanceProfilesResponse listInstanceProfilesResponse = client.listInstanceProfiles(listInstanceProfilesRequestBuilder.build());
                List<InstanceProfile> fetchedInstanceProfiles = listInstanceProfilesResponse.instanceProfiles();
                instanceProfiles.addAll(fetchedInstanceProfiles);
                if (listInstanceProfilesResponse.isTruncated()) {
                    marker = listInstanceProfilesResponse.marker();
                } else {
                    finished = true;
                }
            }
            LOGGER.debug("The total of {} instance profile(s) has fetched.", instanceProfiles.size());
            return instanceProfiles.stream().map(this::instanceProfileToCloudAccessConfig).collect(Collectors.toSet());
        } catch (AwsServiceException ase) {
            if (ase.statusCode() == UNAUTHORIZED) {
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
                ListRolesRequest.Builder listRolesRequestBuilder = ListRolesRequest.builder().maxItems(fetchMaxItems);
                if (isNotEmpty(marker)) {
                    listRolesRequestBuilder.marker(marker);
                }
                LOGGER.debug("About to fetch roles...");
                ListRolesResponse listRolesResponse = client.listRoles(listRolesRequestBuilder.build());
                roles.addAll(listRolesResponse.roles());
                if (listRolesResponse.isTruncated()) {
                    marker = listRolesResponse.marker();
                } else {
                    finished = true;
                }
            }
            return roles.stream().map(this::roleToCloudAccessConfig).collect(Collectors.toSet());
        } catch (AwsServiceException ase) {
            if (ase.statusCode() == UNAUTHORIZED) {
                String policyMessage = "Could not get roles because the user does not have enough permission. ";
                LOGGER.error(policyMessage + ase.getMessage(), ase);
                throw new CloudUnauthorizedException(ase.awsErrorDetails().errorMessage(), ase);
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
        Map<String, Object> properties = getCloudAccessConfigProperties(role.arn(), role.createDate().toString(), role.arn());
        return new CloudAccessConfig(role.roleName(), role.roleId(), properties);
    }

    private CloudAccessConfig instanceProfileToCloudAccessConfig(InstanceProfile instanceProfile) {
        String roleName = instanceProfile.arn();
        if (!instanceProfile.roles().isEmpty()) {
            roleName = instanceProfile.roles().get(0).arn();
        }
        Map<String, Object> properties = getCloudAccessConfigProperties(instanceProfile.arn(), instanceProfile.createDate().toString(), roleName);
        return new CloudAccessConfig(instanceProfile.instanceProfileName(), instanceProfile.instanceProfileId(), properties);
    }

    private Map<String, Object> getCloudAccessConfigProperties(String arn, String creationDate, String roleArn) {
        Map<String, Object> properties = new HashMap<>();
        properties.put("arn", arn);
        properties.put("creationDate", creationDate);
        properties.put("roleArn", roleArn);
        return properties;
    }

    public InstanceStoreMetadata collectInstanceStorageCount(AuthenticatedContext ac, List<String> instanceTypes, List<String> entitlements) {
        Location location = ac.getCloudContext().getLocation();
        try {
            String accountId = ac.getCloudContext().getAccountId();
            ExtendedCloudCredential extendedCloudCredential = new ExtendedCloudCredential(
                    ac.getCloudCredential(),
                    ac.getCloudContext().getPlatform().value(),
                    "",
                    ac.getCloudContext().getCrn(),
                    accountId,
                    entitlements
            );
            CloudVmTypes cloudVmTypes = virtualMachines(extendedCloudCredential, location.getRegion(), Map.of());
            Map<String, Set<VmType>> cloudVmResponses = cloudVmTypes.getCloudVmResponses();
            Map<String, VolumeParameterConfig> instanceTypeToInstanceStorageMap = cloudVmResponses.getOrDefault(location.getAvailabilityZone().value(), Set.of())
                    .stream()
                    .filter(vmType -> instanceTypes.contains(vmType.value()))
                    .filter(vmType -> Objects.nonNull(vmType.getMetaData().getEphemeralConfig()))
                    .collect(Collectors.toMap(VmType::value, vmType -> vmType.getMetaData().getEphemeralConfig()));
            return new InstanceStoreMetadata(instanceTypeToInstanceStorageMap);
        } catch (Exception e) {
            LOGGER.warn("Failed to get vm type data: {}", instanceTypes, e);
            throw new CloudConnectorException(e.getMessage(), e);
        }
    }

}
