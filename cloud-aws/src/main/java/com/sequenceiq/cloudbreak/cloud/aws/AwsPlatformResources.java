package com.sequenceiq.cloudbreak.cloud.aws;

import static com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone.availabilityZone;
import static com.sequenceiq.cloudbreak.cloud.model.DisplayName.displayName;
import static com.sequenceiq.cloudbreak.cloud.model.Region.region;
import static java.util.Collections.singletonList;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import com.amazonaws.regions.RegionUtils;
import com.amazonaws.services.ec2.AmazonEC2Client;
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
import com.amazonaws.services.ec2.model.Vpc;
import com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.cloud.PlatformResources;
import com.sequenceiq.cloudbreak.cloud.aws.view.AwsCredentialView;
import com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
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
import com.sequenceiq.cloudbreak.cloud.model.DisplayName;
import com.sequenceiq.cloudbreak.cloud.model.Region;
import com.sequenceiq.cloudbreak.cloud.model.RegionDisplayNameSpecification;
import com.sequenceiq.cloudbreak.cloud.model.RegionDisplayNameSpecifications;
import com.sequenceiq.cloudbreak.cloud.model.VmSpecification;
import com.sequenceiq.cloudbreak.cloud.model.VmType;
import com.sequenceiq.cloudbreak.cloud.model.VmTypeMeta;
import com.sequenceiq.cloudbreak.cloud.model.VmsSpecification;
import com.sequenceiq.cloudbreak.cloud.model.VolumeParameterConfig;
import com.sequenceiq.cloudbreak.cloud.model.VolumeParameterType;
import com.sequenceiq.cloudbreak.cloud.model.ZoneVmSpecification;
import com.sequenceiq.cloudbreak.cloud.model.ZoneVmSpecifications;
import com.sequenceiq.cloudbreak.cloud.model.view.PlatformResourceSecurityGroupFilterView;
import com.sequenceiq.cloudbreak.cloud.model.view.PlatformResourceSshKeyFilterView;
import com.sequenceiq.cloudbreak.cloud.model.view.PlatformResourceVpcFilterView;
import com.sequenceiq.cloudbreak.cloud.service.CloudbreakResourceReaderService;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;
import com.sequenceiq.cloudbreak.util.JsonUtil;

@Service
public class AwsPlatformResources implements PlatformResources {
    private static final Logger LOGGER = LoggerFactory.getLogger(AwsPlatformResources.class);

    @Inject
    private AwsClient awsClient;

    @Inject
    private CloudbreakResourceReaderService cloudbreakResourceReaderService;

    @Inject
    private AwsPlatformParameters awsPlatformParameters;

    @Value("${cb.aws.zone.parameter.default:eu-west-1}")
    private String awsZoneParameterDefault;

    @Value("${cb.aws.vm.parameter.definition.path:}")
    private String awsVmParameterDefinitionPath;

    private Map<Region, DisplayName> regionDisplayNames = new HashMap<>();

    private Map<Region, Set<VmType>> vmTypes = new HashMap<>();

    private Map<Region, VmType> defaultVmTypes = new HashMap<>();

    @PostConstruct
    public void init() {
        regionDisplayNames = readRegionDisplayNames(resourceDefinition("zone-displaynames"));
        readVmTypes();
    }

    private String getDefinition(String parameter, String type) {
        if (Strings.isNullOrEmpty(parameter)) {
            return resourceDefinition(type);
        } else {
            return FileReaderUtils.readFileFromClasspathQuietly(parameter);
        }
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
                VmTypeMeta.VmTypeMetaBuilder builder = VmTypeMeta.VmTypeMetaBuilder.builder()
                        .withCpuAndMemory(vmSpecification.getMetaSpecification().getProperties().getCpu(),
                                vmSpecification.getMetaSpecification().getProperties().getMemory())
                        .withPrice(vmSpecification.getMetaSpecification().getProperties().getPrice());
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
                VmType vmType = vmTypeMap.get(zvs.getDefaultVmType());
                if (vmType != null) {
                    defaultVmTypes.put(region(zvs.getZone()), vmType);
                }
            }
        } catch (IOException e) {
            LOGGER.error("Cannot initialize platform parameters for aws", e);
        }
    }

    private void addConfig(VmTypeMeta.VmTypeMetaBuilder builder, ConfigSpecification configSpecification) {
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
            RegionDisplayNameSpecifications regionDisplayNameSpecifications = JsonUtil.readValue(displayNames, RegionDisplayNameSpecifications.class);
            for (RegionDisplayNameSpecification regionDisplayNameSpecification : regionDisplayNameSpecifications.getItems()) {
                regionDisplayNames.put(region(regionDisplayNameSpecification.getName()),
                        displayName(regionDisplayNameSpecification.getDisplayName()));
            }
        } catch (IOException ignored) {
            return regionDisplayNames;
        }
        return regionDisplayNames;
    }

    @Override
    public CloudNetworks networks(CloudCredential cloudCredential, Region region, Map<String, String> filters) {
        Map<String, Set<CloudNetwork>> result = new HashMap<>();
        Set<CloudNetwork> cloudNetworks = new HashSet<>();
        AmazonEC2Client ec2Client = awsClient.createAccess(new AwsCredentialView(cloudCredential), region.value());

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
                subnetMap.put(subnet.getSubnetId(), subnet.getSubnetId());
            }
            cloudNetworks.add(new CloudNetwork(vpc.getVpcId(), vpc.getVpcId(), subnetMap, properties));
        }
        result.put(region.value(), cloudNetworks);
        return new CloudNetworks(result);
    }

    private DescribeSubnetsRequest createVpcDescribeRequest(Vpc vpc) {
        return new DescribeSubnetsRequest().withFilters(new Filter("vpc-id", singletonList(vpc.getVpcId())));
    }

    @Override
    public CloudSshKeys sshKeys(CloudCredential cloudCredential, Region region, Map<String, String> filters) {
        Map<String, Set<CloudSshKey>> result = new HashMap<>();
        for (Region actualRegion : awsPlatformParameters.regions().types()) {
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
    public CloudRegions regions(CloudCredential cloudCredential, Region region, Map<String, String> filters) throws Exception {
        AmazonEC2Client ec2Client = awsClient.createAccess(cloudCredential);
        Map<Region, List<AvailabilityZone>> regionListMap = new HashMap<>();
        Map<Region, String> displayNames = new HashMap<>();

        DescribeRegionsRequest describeRegionsRequest = new DescribeRegionsRequest();
        DescribeRegionsResult describeRegionsResult = ec2Client.describeRegions(describeRegionsRequest);
        String defaultRegion = awsZoneParameterDefault;

        for (com.amazonaws.services.ec2.model.Region awsRegion : describeRegionsResult.getRegions()) {
            if (region == null || Strings.isNullOrEmpty(region.value()) || awsRegion.getRegionName().equals(region.value())) {
                DescribeAvailabilityZonesRequest describeAvailabilityZonesRequest = new DescribeAvailabilityZonesRequest();

                ec2Client.setRegion(RegionUtils.getRegion(awsRegion.getRegionName()));
                Filter filter = new Filter();
                filter.setName("region-name");
                List<String> list = new ArrayList<>();
                list.add(awsRegion.getRegionName());
                filter.setValues(list);

                describeAvailabilityZonesRequest.withFilters(filter);

                DescribeAvailabilityZonesResult describeAvailabilityZonesResult = ec2Client.describeAvailabilityZones(describeAvailabilityZonesRequest);

                List<AvailabilityZone> tmpAz = new ArrayList<>();
                for (com.amazonaws.services.ec2.model.AvailabilityZone availabilityZone : describeAvailabilityZonesResult.getAvailabilityZones()) {
                    tmpAz.add(availabilityZone(availabilityZone.getZoneName()));
                }
                regionListMap.put(region(awsRegion.getRegionName()), tmpAz);
                DisplayName displayName = regionDisplayNames.get(region(awsRegion.getRegionName()));
                if (displayName == null || Strings.isNullOrEmpty(displayName.value())) {
                    displayNames.put(region(awsRegion.getRegionName()), awsRegion.getRegionName());
                } else {
                    displayNames.put(region(awsRegion.getRegionName()), displayName.value());
                }
            }
        }
        if (region != null && !Strings.isNullOrEmpty(region.value())) {
            defaultRegion = region.value();
        }
        return new CloudRegions(regionListMap, displayNames, defaultRegion);
    }

    @Override
    @Cacheable(cacheNames = "cloudResourceVmTypeCache", key = "#cloudCredential?.id + #region.getRegionName()")
    public CloudVmTypes virtualMachines(CloudCredential cloudCredential, Region region, Map<String, String> filters) throws Exception {
        CloudRegions regions = regions(cloudCredential, region, filters);

        Map<String, Set<VmType>> cloudVmResponses = new HashMap<>();
        Map<String, VmType> defaultCloudVmResponses = new HashMap<>();

        for (AvailabilityZone availabilityZone : regions.getCloudRegions().get(region)) {
            cloudVmResponses.put(availabilityZone.value(), vmTypes.get(region));
            defaultCloudVmResponses.put(availabilityZone.value(), defaultVmTypes.get(region));
        }

        return new CloudVmTypes(cloudVmResponses, defaultCloudVmResponses);
    }

    @Override
    public CloudGateWays gateways(CloudCredential cloudCredential, Region region, Map<String, String> filters) throws Exception {
        AmazonEC2Client ec2Client = awsClient.createAccess(cloudCredential);

        Map<String, Set<CloudGateWay>> resultCloudGateWayMap = new HashMap<>();
        CloudRegions regions = regions(cloudCredential, region, filters);

        for (Map.Entry<Region, List<AvailabilityZone>> regionListEntry : regions.getCloudRegions().entrySet()) {
            if (region == null || Strings.isNullOrEmpty(region.value()) || regionListEntry.getKey().value().equals(region.value())) {
                ec2Client.setRegion(RegionUtils.getRegion(regionListEntry.getKey().value()));

                DescribeInternetGatewaysRequest describeInternetGatewaysRequest = new DescribeInternetGatewaysRequest();
                DescribeInternetGatewaysResult describeInternetGatewaysResult = ec2Client.describeInternetGateways(describeInternetGatewaysRequest);

                Set<CloudGateWay> gateWays = new HashSet<>();
                for (InternetGateway internetGateway : describeInternetGatewaysResult.getInternetGateways()) {
                    CloudGateWay cloudGateWay = new CloudGateWay();
                    cloudGateWay.setId(internetGateway.getInternetGatewayId());
                    cloudGateWay.setName(internetGateway.getInternetGatewayId());
                    List<String> vpcs = new ArrayList<>();
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
    public CloudIpPools publicIpPool(CloudCredential cloudCredential, Region region, Map<String, String> filters) throws Exception {
        return new CloudIpPools();
    }
}
