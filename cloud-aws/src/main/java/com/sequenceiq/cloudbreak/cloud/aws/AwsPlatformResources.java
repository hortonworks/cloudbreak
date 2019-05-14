package com.sequenceiq.cloudbreak.cloud.aws;

import static java.util.Collections.singletonList;

import static com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone.availabilityZone;
import static com.sequenceiq.cloudbreak.cloud.model.Coordinate.coordinate;
import static com.sequenceiq.cloudbreak.cloud.model.DisplayName.displayName;
import static com.sequenceiq.cloudbreak.cloud.model.Region.region;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.regions.RegionUtils;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.AmazonEC2Exception;
import com.amazonaws.services.ec2.model.DescribeAvailabilityZonesRequest;
import com.amazonaws.services.ec2.model.DescribeAvailabilityZonesResult;
import com.amazonaws.services.ec2.model.DescribeInternetGatewaysRequest;
import com.amazonaws.services.ec2.model.DescribeInternetGatewaysResult;
import com.amazonaws.services.ec2.model.DescribeKeyPairsRequest;
import com.amazonaws.services.ec2.model.DescribeRegionsRequest;
import com.amazonaws.services.ec2.model.DescribeRegionsResult;
import com.amazonaws.services.ec2.model.DescribeSecurityGroupsRequest;
import com.amazonaws.services.ec2.model.DescribeSubnetsRequest;
import com.amazonaws.services.ec2.model.DescribeVpcsRequest;
import com.amazonaws.services.ec2.model.Filter;
import com.amazonaws.services.ec2.model.InternetGateway;
import com.amazonaws.services.ec2.model.InternetGatewayAttachment;
import com.amazonaws.services.ec2.model.KeyPairInfo;
import com.amazonaws.services.ec2.model.SecurityGroup;
import com.amazonaws.services.ec2.model.Subnet;
import com.amazonaws.services.ec2.model.Tag;
import com.amazonaws.services.ec2.model.Vpc;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagement;
import com.amazonaws.services.identitymanagement.model.InstanceProfile;
import com.amazonaws.services.identitymanagement.model.ListInstanceProfilesResult;
import com.amazonaws.services.kms.AWSKMS;
import com.amazonaws.services.kms.model.AliasListEntry;
import com.amazonaws.services.kms.model.DescribeKeyRequest;
import com.amazonaws.services.kms.model.DescribeKeyResult;
import com.amazonaws.services.kms.model.ListAliasesRequest;
import com.amazonaws.services.kms.model.ListAliasesResult;
import com.amazonaws.services.kms.model.ListKeysRequest;
import com.amazonaws.services.kms.model.ListKeysResult;
import com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.cloud.PlatformResources;
import com.sequenceiq.cloudbreak.cloud.aws.view.AwsCredentialView;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
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
import com.sequenceiq.cloudbreak.cloud.model.CloudVmTypes;
import com.sequenceiq.cloudbreak.cloud.model.ConfigSpecification;
import com.sequenceiq.cloudbreak.cloud.model.Coordinate;
import com.sequenceiq.cloudbreak.cloud.model.DisplayName;
import com.sequenceiq.cloudbreak.cloud.model.PropertySpecification;
import com.sequenceiq.cloudbreak.cloud.model.Region;
import com.sequenceiq.cloudbreak.cloud.model.RegionCoordinateSpecification;
import com.sequenceiq.cloudbreak.cloud.model.RegionCoordinateSpecifications;
import com.sequenceiq.cloudbreak.cloud.model.VmSpecification;
import com.sequenceiq.cloudbreak.cloud.model.VmType;
import com.sequenceiq.cloudbreak.cloud.model.VmTypeMeta;
import com.sequenceiq.cloudbreak.cloud.model.VmTypeMeta.VmTypeMetaBuilder;
import com.sequenceiq.cloudbreak.cloud.model.VmsSpecification;
import com.sequenceiq.cloudbreak.cloud.model.VolumeParameterConfig;
import com.sequenceiq.cloudbreak.cloud.model.VolumeParameterType;
import com.sequenceiq.cloudbreak.cloud.model.ZoneVmSpecification;
import com.sequenceiq.cloudbreak.cloud.model.ZoneVmSpecifications;
import com.sequenceiq.cloudbreak.cloud.model.view.PlatformResourceSecurityGroupFilterView;
import com.sequenceiq.cloudbreak.cloud.model.view.PlatformResourceSshKeyFilterView;
import com.sequenceiq.cloudbreak.cloud.model.view.PlatformResourceVpcFilterView;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.common.type.CloudConstants;
import com.sequenceiq.cloudbreak.service.CloudbreakResourceReaderService;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;

@Service
public class AwsPlatformResources implements PlatformResources {
    private static final Logger LOGGER = LoggerFactory.getLogger(AwsPlatformResources.class);

    private static final int UNAUTHORIZED = 403;

    @Inject
    private AwsClient awsClient;

    @Inject
    private CloudbreakResourceReaderService cloudbreakResourceReaderService;

    @Inject
    private AwsDefaultZoneProvider awsDefaultZoneProvider;

    @Value("${cb.aws.vm.parameter.definition.path:}")
    private String awsVmParameterDefinitionPath;

    @Value("#{'${cb.aws.disabled.instance.types:}'.split(',')}")
    private List<String> disabledInstanceTypes;

    private final Predicate<VmType> enabledInstanceTypeFilter = vmt -> disabledInstanceTypes.stream()
            .filter(it -> !it.isEmpty())
            .noneMatch(di -> vmt.value().startsWith(di));

    private Map<Region, Coordinate> regionCoordinates = new HashMap<>();

    private final Map<Region, Set<VmType>> vmTypes = new HashMap<>();

    private final Map<Region, VmType> defaultVmTypes = new HashMap<>();

    private Map<Region, DisplayName> regionDisplayNames = new HashMap<>();

    @PostConstruct
    public void init() {
        regionDisplayNames = readRegionDisplayNames(resourceDefinition("zone-coordinates"));
        regionCoordinates = readRegionCoordinates(resourceDefinition("zone-coordinates"));
        readVmTypes();
    }

    private String getDefinition(String parameter, String type) {
        return Strings.isNullOrEmpty(parameter) ? resourceDefinition(type) : FileReaderUtils.readFileFromClasspathQuietly(parameter);
    }

    public String resourceDefinition(String resource) {
        return cloudbreakResourceReaderService.resourceDefinition("aws", resource);
    }

    private void readVmTypes() {
        Map<String, VmType> vmTypeMap = new TreeMap<>();
        String vm = getDefinition(awsVmParameterDefinitionPath, "vm");
        String zoneVms = getDefinition(awsVmParameterDefinitionPath, "zone-vm");
        try {
            VmsSpecification oVms = JsonUtil.readValue(vm, VmsSpecification.class);
            for (VmSpecification vmSpecification : oVms.getItems()) {
                PropertySpecification properties = vmSpecification.getMetaSpecification().getProperties();
                VmTypeMetaBuilder builder = VmTypeMetaBuilder.builder()
                        .withCpuAndMemory(Integer.valueOf(properties.getCpu()), Float.valueOf(properties.getMemory()))
                        .withPrice(properties.getPrice())
                        .withVolumeEncryptionSupport(properties.getEncryptionSupported());
                for (ConfigSpecification configSpecification : vmSpecification.getMetaSpecification().getConfigSpecification()) {
                    addConfig(builder, configSpecification);
                }
                VmTypeMeta vmTypeMeta = builder.create();
                vmTypeMap.put(vmSpecification.getValue(), VmType.vmTypeWithMeta(vmSpecification.getValue(), vmTypeMeta, vmSpecification.getExtended()));
            }
            ZoneVmSpecifications zoneVmSpecifications = JsonUtil.readValue(zoneVms, ZoneVmSpecifications.class);
            for (ZoneVmSpecification zvs : zoneVmSpecifications.getItems()) {
                Set<VmType> regionVmTypes = new HashSet<>();
                for (String vmTypeString : zvs.getVmTypes()) {
                    VmType vmType = vmTypeMap.get(vmTypeString);
                    if (vmType != null) {
                        regionVmTypes.add(vmType);
                    }
                }
                vmTypes.put(region(zvs.getZone()), regionVmTypes);
                Optional.ofNullable(vmTypeMap.get(zvs.getDefaultVmType()))
                        .filter(enabledInstanceTypeFilter)
                        .ifPresent(vmType -> defaultVmTypes.put(region(zvs.getZone()), vmType));
            }
        } catch (IOException e) {
            LOGGER.error("Cannot initialize platform parameters for aws", e);
        } catch (NumberFormatException e) {
            LOGGER.error("One of CPU or Memory fields has unexpected format", e);
        }
    }

    private void addConfig(VmTypeMetaBuilder builder, ConfigSpecification configSpecification) {
        if (configSpecification.getVolumeParameterType().equals(VolumeParameterType.AUTO_ATTACHED.name())) {
            builder.withAutoAttachedConfig(volumeParameterConfig(configSpecification));
        } else if (configSpecification.getVolumeParameterType().equals(VolumeParameterType.EPHEMERAL.name())) {
            builder.withEphemeralConfig(volumeParameterConfig(configSpecification));
        } else if (configSpecification.getVolumeParameterType().equals(VolumeParameterType.MAGNETIC.name())) {
            builder.withMagneticConfig(volumeParameterConfig(configSpecification));
        } else if (configSpecification.getVolumeParameterType().equals(VolumeParameterType.SSD.name())) {
            builder.withSsdConfig(volumeParameterConfig(configSpecification));
        } else if (configSpecification.getVolumeParameterType().equals(VolumeParameterType.ST1.name())) {
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
                regionDisplayNames.put(region(regionCoordinateSpecification.getName()),
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
                regionCoordinates.put(region(regionCoordinateSpecification.getName()),
                        coordinate(regionCoordinateSpecification.getLongitude(),
                                regionCoordinateSpecification.getLatitude(),
                                regionCoordinateSpecification.getDisplayName()));
            }
        } catch (IOException ignored) {
            return regionCoordinates;
        }
        return regionCoordinates;
    }

    @Override
    public CloudNetworks networks(CloudCredential cloudCredential, Region region, Map<String, String> filters) {
        Map<String, Set<CloudNetwork>> result = new HashMap<>();
        Set<CloudNetwork> cloudNetworks = new HashSet<>();
        AmazonEC2Client ec2Client = awsClient.createAccess(new AwsCredentialView(cloudCredential), getRegion(region));

        //create vpc filter view
        PlatformResourceVpcFilterView filter = new PlatformResourceVpcFilterView(filters);

        DescribeVpcsRequest describeVpcsRequest = new DescribeVpcsRequest();
        // If the filtervalue is provided then we should filter only for those vpc
        if (!Strings.isNullOrEmpty(filter.getVpcId())) {
            describeVpcsRequest.withVpcIds(filter.getVpcId());
        }
        for (Vpc vpc : ec2Client.describeVpcs(describeVpcsRequest).getVpcs()) {
            Map<String, String> subnetMap = new HashMap<>();
            List<Subnet> subnets = ec2Client.describeSubnets(createVpcDescribeRequest(vpc)).getSubnets();
            Map<String, Object> properties = new HashMap<>();
            properties.put("cidrBlock", vpc.getCidrBlock());
            properties.put("default", vpc.getIsDefault());
            properties.put("dhcpOptionsId", vpc.getDhcpOptionsId());
            properties.put("instanceTenancy", vpc.getInstanceTenancy());
            properties.put("state", vpc.getState());

            for (Subnet subnet : subnets) {
                Optional<String> subnetName = getName(subnet.getTags());
                subnetMap.put(subnet.getSubnetId(), subnetName.isPresent() ? subnetName.get() : subnet.getSubnetId());
            }

            Optional<String> name = getName(vpc.getTags());
            if (name.isPresent()) {
                cloudNetworks.add(new CloudNetwork(name.get(), vpc.getVpcId(), subnetMap, properties));
            } else {
                cloudNetworks.add(new CloudNetwork(vpc.getVpcId(), vpc.getVpcId(), subnetMap, properties));
            }
        }
        result.put(region.value(), cloudNetworks);
        return new CloudNetworks(result);
    }

    private String getRegion(Region region) {
        return region.value().split(",")[0];
    }

    private Optional<String> getName(List<Tag> tags) {
        for (Tag tag : tags) {
            if (tag.getKey().equals("Name")) {
                return Optional.ofNullable(tag.getValue());
            }
        }
        return Optional.empty();
    }

    private DescribeSubnetsRequest createVpcDescribeRequest(Vpc vpc) {
        return new DescribeSubnetsRequest().withFilters(new Filter("vpc-id", singletonList(vpc.getVpcId())));
    }

    @Override
    public CloudSshKeys sshKeys(CloudCredential cloudCredential, Region region, Map<String, String> filters) {
        Map<String, Set<CloudSshKey>> result = new HashMap<>();
        for (Region actualRegion : regions(cloudCredential, region, new HashMap<>()).getCloudRegions().keySet()) {
            // If region is provided then should filter for those region
            if (regionMatch(actualRegion, region)) {
                Set<CloudSshKey> cloudSshKeys = new HashSet<>();
                AmazonEC2Client ec2Client = awsClient.createAccess(new AwsCredentialView(cloudCredential), actualRegion.value());

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
        AmazonEC2Client ec2Client = awsClient.createAccess(new AwsCredentialView(cloudCredential), region.value());

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

        for (SecurityGroup securityGroup : ec2Client.describeSecurityGroups(describeSecurityGroupsRequest).getSecurityGroups()) {
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

    @Override
    @Cacheable(cacheNames = "cloudResourceRegionCache", key = "#cloudCredential?.id")
    public CloudRegions regions(CloudCredential cloudCredential, Region region, Map<String, String> filters) {
        AmazonEC2Client ec2Client = awsClient.createAccess(cloudCredential);
        Map<Region, List<AvailabilityZone>> regionListMap = new HashMap<>();
        Map<Region, String> displayNames = new HashMap<>();
        Map<Region, Coordinate> coordinates = new HashMap<>();

        DescribeRegionsResult describeRegionsResult = describeRegionsResult(ec2Client);
        String defaultRegion = awsDefaultZoneProvider.getDefaultZone(cloudCredential);

        for (com.amazonaws.services.ec2.model.Region awsRegion : describeRegionsResult.getRegions()) {
            if (region == null || Strings.isNullOrEmpty(region.value()) || awsRegion.getRegionName().equals(region.value())) {
                DescribeAvailabilityZonesRequest describeAvailabilityZonesRequest = new DescribeAvailabilityZonesRequest();

                ec2Client.setRegion(RegionUtils.getRegion(awsRegion.getRegionName()));
                Filter filter = new Filter();
                filter.setName("region-name");
                Collection<String> list = new ArrayList<>();
                list.add(awsRegion.getRegionName());
                filter.setValues(list);

                describeAvailabilityZonesRequest.withFilters(filter);

                try {
                    DescribeAvailabilityZonesResult describeAvailabilityZonesResult = ec2Client.describeAvailabilityZones(describeAvailabilityZonesRequest);

                    List<AvailabilityZone> tmpAz = new ArrayList<>();
                    for (com.amazonaws.services.ec2.model.AvailabilityZone availabilityZone : describeAvailabilityZonesResult.getAvailabilityZones()) {
                        tmpAz.add(availabilityZone(availabilityZone.getZoneName()));
                    }
                    regionListMap.put(region(awsRegion.getRegionName()), tmpAz);

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

    private DescribeAvailabilityZonesResult describeAvailabilityZonesResult(AmazonEC2Client ec2Client, com.amazonaws.services.ec2.model.Region awsRegion) {
        try {
            DescribeAvailabilityZonesRequest describeAvailabilityZonesRequest = new DescribeAvailabilityZonesRequest();

            ec2Client.setRegion(RegionUtils.getRegion(awsRegion.getRegionName()));
            Filter filter = new Filter();
            filter.setName("region-name");
            Collection<String> list = new ArrayList<>();
            list.add(awsRegion.getRegionName());
            filter.setValues(list);

            describeAvailabilityZonesRequest.withFilters(filter);

            return ec2Client.describeAvailabilityZones(describeAvailabilityZonesRequest);
        } catch (AmazonEC2Exception e) {
            LOGGER.info("Failed to retrieve AZ from Region: {}!", awsRegion.getRegionName(), e);
        }
        return new DescribeAvailabilityZonesResult();
    }

    private DescribeRegionsResult describeRegionsResult(AmazonEC2Client ec2Client) {
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
        CloudRegions regions = regions(cloudCredential, region, filters);

        Map<String, Set<VmType>> cloudVmResponses = new HashMap<>();
        Map<String, VmType> defaultCloudVmResponses = new HashMap<>();

        for (AvailabilityZone availabilityZone : regions.getCloudRegions().get(region)) {
            Set<VmType> types = vmTypes.get(region).stream()
                    .filter(enabledInstanceTypeFilter)
                    .collect(Collectors.toSet());
            cloudVmResponses.put(availabilityZone.value(), types);
            defaultCloudVmResponses.put(availabilityZone.value(), defaultVmTypes.get(region));
        }

        return new CloudVmTypes(cloudVmResponses, defaultCloudVmResponses);
    }

    @Override
    public CloudGateWays gateways(CloudCredential cloudCredential, Region region, Map<String, String> filters) {
        AmazonEC2Client ec2Client = awsClient.createAccess(cloudCredential);

        Map<String, Set<CloudGateWay>> resultCloudGateWayMap = new HashMap<>();
        CloudRegions regions = regions(cloudCredential, region, filters);

        for (Entry<Region, List<AvailabilityZone>> regionListEntry : regions.getCloudRegions().entrySet()) {
            if (region == null || Strings.isNullOrEmpty(region.value()) || regionListEntry.getKey().value().equals(region.value())) {
                ec2Client.setRegion(RegionUtils.getRegion(regionListEntry.getKey().value()));

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
        String queryFailedMessage = "Could not get instance profile roles from Amazon: ";

        CloudAccessConfigs cloudAccessConfigs = new CloudAccessConfigs(new HashSet<>());
        AwsCredentialView awsCredentialView = new AwsCredentialView(cloudCredential);
        AmazonIdentityManagement client = awsClient.createAmazonIdentityManagement(awsCredentialView);
        try {
            ListInstanceProfilesResult listRolesResult = client.listInstanceProfiles();
            for (InstanceProfile instanceProfile : listRolesResult.getInstanceProfiles()) {
                Map<String, Object> properties = new HashMap<>();
                properties.put("arn", instanceProfile.getArn());
                properties.put("creationDate", instanceProfile.getCreateDate().toString());
                if (!instanceProfile.getRoles().isEmpty()) {
                    String roleName = instanceProfile.getRoles().get(0).getArn();
                    properties.put("roleArn", Strings.isNullOrEmpty(roleName) ? instanceProfile.getArn() : roleName);
                }
                cloudAccessConfigs.getCloudAccessConfigs().add(
                        new CloudAccessConfig(
                                instanceProfile.getInstanceProfileName(),
                                instanceProfile.getInstanceProfileId(),
                                properties));
            }
        } catch (AmazonServiceException ase) {
            if (ase.getStatusCode() == UNAUTHORIZED) {
                String policyMessage = "Could not get instance profile roles because the user does not have enough permission.";
                LOGGER.error(policyMessage + ase);
                throw new CloudConnectorException(policyMessage, ase);
            } else {
                LOGGER.info(queryFailedMessage, ase);
                throw new CloudConnectorException(queryFailedMessage + ase.getMessage(), ase);
            }
        } catch (Exception e) {
            LOGGER.warn(queryFailedMessage, e);
            throw new CloudConnectorException(queryFailedMessage + e.getMessage(), e);
        }
        return cloudAccessConfigs;
    }

    @Override
    public CloudEncryptionKeys encryptionKeys(CloudCredential cloudCredential, Region region, Map<String, String> filters) {
        String queryFailedMessage = "Could not get encryption keys from Amazon: ";

        CloudEncryptionKeys cloudEncryptionKeys = new CloudEncryptionKeys(new HashSet<>());
        AwsCredentialView awsCredentialView = new AwsCredentialView(cloudCredential);
        AWSKMS client = awsClient.createAWSKMS(awsCredentialView, region.value());
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
                        LOGGER.error(policyMessage + e);
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
                LOGGER.error(policyMessage + ase);
                throw new CloudConnectorException(policyMessage, ase);
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
}
