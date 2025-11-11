package com.sequenceiq.cloudbreak.cloud.gcp;

import static com.sequenceiq.cloudbreak.cloud.gcp.DistroxEnabledInstanceTypes.GCP_ENABLED_TYPES_LIST;
import static com.sequenceiq.cloudbreak.cloud.gcp.util.GcpStackUtil.SHARED_PROJECT_ID;
import static com.sequenceiq.cloudbreak.cloud.model.Coordinate.coordinate;
import static com.sequenceiq.cloudbreak.cloud.model.Region.region;
import static com.sequenceiq.cloudbreak.cloud.model.network.SubnetType.PRIVATE;
import static com.sequenceiq.cloudbreak.cloud.model.network.SubnetType.PUBLIC;
import static com.sequenceiq.cloudbreak.common.network.NetworkConstants.SUBNET_ID;
import static org.apache.commons.collections4.CollectionUtils.emptyIfNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import com.google.api.client.auth.oauth2.TokenResponseException;
import com.google.api.services.cloudkms.v1.CloudKMS;
import com.google.api.services.cloudkms.v1.model.CryptoKey;
import com.google.api.services.cloudkms.v1.model.KeyRing;
import com.google.api.services.cloudkms.v1.model.ListCryptoKeysResponse;
import com.google.api.services.cloudkms.v1.model.ListKeyRingsResponse;
import com.google.api.services.compute.Compute;
import com.google.api.services.compute.model.Firewall;
import com.google.api.services.compute.model.FirewallList;
import com.google.api.services.compute.model.MachineType;
import com.google.api.services.compute.model.MachineTypeList;
import com.google.api.services.compute.model.Network;
import com.google.api.services.compute.model.NetworkList;
import com.google.api.services.compute.model.RegionList;
import com.google.api.services.compute.model.Subnetwork;
import com.google.api.services.compute.model.SubnetworkList;
import com.google.api.services.iam.v1.Iam;
import com.google.api.services.iam.v1.model.ListServiceAccountsResponse;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.cloud.PlatformResources;
import com.sequenceiq.cloudbreak.cloud.gcp.client.GcpCloudKMSFactory;
import com.sequenceiq.cloudbreak.cloud.gcp.client.GcpComputeFactory;
import com.sequenceiq.cloudbreak.cloud.gcp.client.GcpIamFactory;
import com.sequenceiq.cloudbreak.cloud.gcp.util.GcpStackUtil;
import com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone;
import com.sequenceiq.cloudbreak.cloud.model.CloudAccessConfig;
import com.sequenceiq.cloudbreak.cloud.model.CloudAccessConfigs;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudEncryptionKey;
import com.sequenceiq.cloudbreak.cloud.model.CloudEncryptionKeys;
import com.sequenceiq.cloudbreak.cloud.model.CloudGateWays;
import com.sequenceiq.cloudbreak.cloud.model.CloudIpPools;
import com.sequenceiq.cloudbreak.cloud.model.CloudNetwork;
import com.sequenceiq.cloudbreak.cloud.model.CloudNetworks;
import com.sequenceiq.cloudbreak.cloud.model.CloudRegions;
import com.sequenceiq.cloudbreak.cloud.model.CloudSecurityGroup;
import com.sequenceiq.cloudbreak.cloud.model.CloudSecurityGroups;
import com.sequenceiq.cloudbreak.cloud.model.CloudSshKeys;
import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmTypes;
import com.sequenceiq.cloudbreak.cloud.model.Coordinate;
import com.sequenceiq.cloudbreak.cloud.model.ExtendedCloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.PlatformDatabaseCapabilities;
import com.sequenceiq.cloudbreak.cloud.model.Region;
import com.sequenceiq.cloudbreak.cloud.model.RegionCoordinateSpecification;
import com.sequenceiq.cloudbreak.cloud.model.RegionCoordinateSpecifications;
import com.sequenceiq.cloudbreak.cloud.model.VmType;
import com.sequenceiq.cloudbreak.cloud.model.VmTypeMeta.VmTypeMetaBuilder;
import com.sequenceiq.cloudbreak.cloud.model.dns.CloudPrivateDnsZones;
import com.sequenceiq.cloudbreak.cloud.model.nosql.CloudNoSqlTables;
import com.sequenceiq.cloudbreak.cloud.model.resourcegroup.CloudResourceGroups;
import com.sequenceiq.cloudbreak.common.domain.CdpSupportedServices;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.common.network.NetworkConstants;
import com.sequenceiq.cloudbreak.filter.MinimalHardwareFilter;
import com.sequenceiq.cloudbreak.service.CloudbreakResourceReaderService;

@Service
public class GcpPlatformResources implements PlatformResources {

    private static final Logger LOGGER = LoggerFactory.getLogger(GcpPlatformResources.class);

    private static final float THOUSAND = 1000.0f;

    private static final int THOUSAND_FIVE_HUNDRED = 1500;

    private static final int TEN = 10;

    private static final int DEFAULT_PAGE_SIZE = 50;

    private static final int GCP_LOCAL_SSD_ALLOWED_VALUES = 375;

    private static final Set<Integer> GCP_LOCAL_SSD_POSSIBLE_NUMBER_VALUES = Set.of(1, 2, 3, 4, 5, 6, 7, 8, 16, 24);

    private static final Set<String> MACHINE_TYPES_WITH_LOCAL_SSD = Set.of("n1", "n2", "n2d");

    @Value("${cb.gcp.default.vmtype:n2-highcpu-8}")
    private String gcpVmDefault;

    @Value("${cb.gcp.default.database.vmtype:db-custom-2-13312}")
    private String gcpDatabaseVmDefault;

    @Value("${cb.gcp.zone.parameter.default:europe-west1}")
    private String gcpZoneParameterDefault;

    @Value("${distrox.restrict.instance.types:true}")
    private boolean restrictInstanceTypes;

    @Inject
    private CloudbreakResourceReaderService cloudbreakResourceReaderService;

    @Inject
    private GcpComputeFactory gcpComputeFactory;

    @Inject
    private GcpIamFactory gcpIamFactory;

    @Inject
    private MinimalHardwareFilter minimalHardwareFilter;

    @Inject
    private GcpCloudKMSFactory gcpCloudKMSFactory;

    @Inject
    private GcpStackUtil gcpStackUtil;

    @Inject
    private ExtremeDiskCalculator extremeDiskCalculator;

    private Map<Region, Coordinate> regionCoordinates = new HashMap<>();

    private final Predicate<VmType> enabledDistroxInstanceTypeFilter = vmt -> GCP_ENABLED_TYPES_LIST.stream()
            .filter(it -> !it.isEmpty())
            .anyMatch(di -> vmt.value().equals(di));

    @PostConstruct
    public void init() {
        regionCoordinates = readRegionCoordinates(resourceDefinition("zone-coordinates"));
    }

    public String resourceDefinition(String resource) {
        return cloudbreakResourceReaderService.resourceDefinition("gcp", resource);
    }

    private Map<Region, Coordinate> readRegionCoordinates(String displayNames) {
        Map<Region, Coordinate> regionCoordinates = new HashMap<>();
        try {
            RegionCoordinateSpecifications regionCoordinateSpecifications = JsonUtil.readValue(displayNames, RegionCoordinateSpecifications.class);
            for (RegionCoordinateSpecification regionCoordinateSpecification : regionCoordinateSpecifications.getItems()) {
                regionCoordinates.put(region(regionCoordinateSpecification.getName()),
                        coordinate(
                                regionCoordinateSpecification.getLongitude(),
                                regionCoordinateSpecification.getLatitude(),
                                regionCoordinateSpecification.getDisplayName(),
                                regionCoordinateSpecification.getName(),
                                regionCoordinateSpecification.isK8sSupported(),
                                regionCoordinateSpecification.getEntitlements(),
                                regionCoordinateSpecification.getDefaultDbVmtype(),
                                null,
                                regionCoordinateSpecification.getCdpSupportedServices()));
            }
        } catch (IOException ignored) {
            return regionCoordinates;
        }
        return regionCoordinates;
    }

    @Override
    public CloudNetworks networks(ExtendedCloudCredential cloudCredential, Region region, Map<String, String> filters) throws Exception {
        Compute compute = gcpComputeFactory.buildCompute(cloudCredential);
        String projectId = gcpStackUtil.getProjectId(cloudCredential);
        Map<String, Set<CloudNetwork>> result = new HashMap<>();

        String networkId = null;
        List<String> subnetIds = new ArrayList<>();
        String sharedProjectId = null;
        String customAvailabilityZone;
        if (filters != null) {
            networkId = filters.getOrDefault("networkId", null);
            subnetIds = getSubnetIds(filters);
            sharedProjectId = filters.getOrDefault("sharedProjectId", null);
            customAvailabilityZone = filters.getOrDefault(GcpStackUtil.CUSTOM_AVAILABILITY_ZONE, "");
        } else {
            customAvailabilityZone = "";
        }

        LOGGER.debug("Get subnets with filter values, networkId : {}, subnetId : {}", networkId, subnetIds);
        Set<CloudNetwork> cloudNetworks = new HashSet<>();
        NetworkList networkList = getNetworkList(compute, projectId, networkId, sharedProjectId);
        SubnetworkList subnetworkList = getSubnetworkList(region, compute, projectId, subnetIds, sharedProjectId);

        // GCP VPCs are global. Subnets have a global scope in region. So picking the first availability zone in the region for subnet.
        String zone = compute.regions().get(projectId, region.value())
                .execute()
                .getZones()
                .stream()
                .filter(zoneName -> zoneName.endsWith(customAvailabilityZone))
                .findFirst()
                .map(tmpZone -> tmpZone.substring(tmpZone.lastIndexOf('/') + 1))
                .orElse(null);
        LOGGER.debug("Zone chosen for the subnets is {}", zone);

        if (isNetworkListNotNull(networkList)) {
            for (Network network : networkList.getItems()) {
                Map<String, Object> properties = new HashMap<>();
                properties.put("gatewayIPv4", Strings.nullToEmpty(network.getGatewayIPv4()));
                properties.put("description", Strings.nullToEmpty(network.getDescription()));
                properties.put("IPv4Range", Strings.nullToEmpty(network.getIPv4Range()));
                properties.put("creationTimestamp", Strings.nullToEmpty(network.getCreationTimestamp()));

                Set<CloudSubnet> subnets = new HashSet<>();
                if (subnetworkList != null && network.getSubnetworks() != null && subnetworkList.getItems() != null) {
                    for (Subnetwork subnetwork : subnetworkList.getItems()) {
                        if (network.getSubnetworks().contains(subnetwork.getSelfLink())) {
                            boolean igwAvailable = !Strings.isNullOrEmpty(subnetwork.getGatewayAddress());
                            subnets.add(
                                    new CloudSubnet.Builder()
                                            .id(subnetwork.getName())
                                            .name(subnetwork.getName())
                                            .availabilityZone(zone)
                                            .cidr(subnetwork.getIpCidrRange())
                                            .secondaryCidrs(getSecondaryRanges(subnetwork))
                                            .secondaryCidrsWithNames(getSecondaryRangesWithNames(subnetwork))
                                            .privateSubnet(subnetwork.getPrivateIpGoogleAccess())
                                            .mapPublicIpOnLaunch(!subnetwork.getPrivateIpGoogleAccess())
                                            .igwAvailable(igwAvailable)
                                            .type(igwAvailable ? PUBLIC : PRIVATE)
                                            .build()
                            );
                        }
                    }
                }

                CloudNetwork cloudNetwork = new CloudNetwork(network.getName(), network.getId().toString(), subnets, properties);
                cloudNetworks.add(cloudNetwork);
            }
        }
        result.put(region.value(), cloudNetworks);

        return new CloudNetworks(result);
    }

    private Map<String, String> getSecondaryRangesWithNames(Subnetwork subnetwork) {
        if (subnetwork.getSecondaryIpRanges() != null) {
            return subnetwork.getSecondaryIpRanges().stream().collect(Collectors.toMap(
                    obj -> obj.getIpCidrRange(),
                    obj -> obj.getRangeName()
            ));
        }
        return Map.of();
    }

    private List<String> getSecondaryRanges(Subnetwork subnetwork) {
        if (subnetwork.getSecondaryIpRanges() != null) {
            return subnetwork.getSecondaryIpRanges().stream().map(s -> s.getIpCidrRange()).collect(Collectors.toList());
        }
        return List.of();
    }

    private static boolean isNetworkListNotNull(NetworkList networkList) {
        return networkList != null && networkList.getItems() != null;
    }

    public List<String> getSubnetIds(Map<String, String> filters) {
        List<String> subnetIds = new ArrayList<>();
        String subnetId = filters.getOrDefault(SUBNET_ID, null);
        if (!Strings.isNullOrEmpty(subnetId)) {
            subnetIds.add(subnetId);
        } else {
            String subnetIdsString = filters.getOrDefault("subnetIds", null);
            if (!Strings.isNullOrEmpty(subnetIdsString)) {
                subnetIds = List.of(subnetIdsString.split(","));
            }
        }
        return subnetIds;
    }

    private SubnetworkList getSubnetworkList(Region region, Compute compute, String projectId, List<String> subnetIds,
            String sharedProjectId) throws IOException {
        SubnetworkList subnetworkList;
        if (subnetIds.isEmpty() && Strings.isNullOrEmpty(sharedProjectId)) {
            subnetworkList = compute.subnetworks().list(projectId, region.value()).execute();
        } else {
            String tmpProjectId = !Strings.isNullOrEmpty(sharedProjectId) ? sharedProjectId : projectId;
            subnetworkList = new SubnetworkList().setItems(new ArrayList<>());
            for (String subnetId : subnetIds) {
                try {
                    Subnetwork subnetwork = compute.subnetworks().get(tmpProjectId, region.value(), subnetId).execute();
                    subnetworkList.getItems().add(subnetwork);
                } catch (Exception e) {
                    LOGGER.info("Could not get Subnet with project: '{}' region: '{}' with subnetId: '{}'", tmpProjectId, region.value(), subnetId, e);
                }
            }
        }
        return subnetworkList;
    }

    public NetworkList getNetworkList(Compute compute, String projectId, String networkId, String sharedProjectId) throws IOException {
        NetworkList networkList;
        if (StringUtils.isEmpty(networkId)) {
            networkList = compute.networks()
                    .list(projectId).execute();
        } else {
            if (!Strings.isNullOrEmpty(sharedProjectId)) {
                try {
                    networkList = new NetworkList()
                        .setItems(Collections.singletonList(compute.networks().get(sharedProjectId, networkId).execute()));
                } catch (Exception e) {
                    LOGGER.info("Could not get Subnetworks from {} project with {} networkId: {}", sharedProjectId, networkId, e);
                    networkList = new NetworkList()
                            .setItems(List.of());
                }
            } else {
                networkList = new NetworkList()
                        .setItems(Collections.singletonList(compute.networks().get(projectId, networkId).execute()));
            }
        }
        return networkList;
    }

    @Override
    public CloudSshKeys sshKeys(ExtendedCloudCredential cloudCredential, Region region, Map<String, String> filters) {
        return new CloudSshKeys();
    }

    @Override
    public CloudSecurityGroups securityGroups(ExtendedCloudCredential cloudCredential, Region region, Map<String, String> filters) throws IOException {
        Compute compute = gcpComputeFactory.buildCompute(cloudCredential);
        String projectId = gcpStackUtil.getProjectId(cloudCredential);

        Map<String, Set<CloudSecurityGroup>> result = new HashMap<>();
        FirewallList firewallList = compute.firewalls().list(projectId).execute();
        if (firewallList.getItems() != null) {
            for (Firewall firewall : firewallList.getItems()) {
                Map<String, Object> properties = new HashMap<>();
                properties.put("network", getNetworkName(firewall));
                CloudSecurityGroup cloudSecurityGroup = new CloudSecurityGroup(firewall.getName(), firewall.getName(), properties);
                result.computeIfAbsent(region.value(), k -> new HashSet<>()).add(cloudSecurityGroup);
            }
        }

        if (filters != null) {
            String sharedProjectId = filters.get(SHARED_PROJECT_ID);
            if (!Strings.isNullOrEmpty(sharedProjectId)) {
                try {
                    FirewallList sharedProjectFirewalls = compute.firewalls().list(sharedProjectId).execute();
                    if (sharedProjectFirewalls.getItems() != null) {
                        for (Firewall firewall : sharedProjectFirewalls.getItems()) {
                            Map<String, Object> properties = new HashMap<>();
                            properties.put("network", getNetworkName(firewall));
                            CloudSecurityGroup cloudSecurityGroup = new CloudSecurityGroup(firewall.getName(), firewall.getName(), properties);
                            result.computeIfAbsent(region.value(), k -> new HashSet<>()).add(cloudSecurityGroup);
                        }
                    }
                } catch (Exception ex) {
                    LOGGER.warn(String.format("We can not read the host project with id %s", sharedProjectId));
                }
            }
        }

        return new CloudSecurityGroups(result);
    }

    private String getNetworkName(Firewall firewall) {
        String[] splittedNetworkName = firewall.getNetwork().split("/");
        return splittedNetworkName[splittedNetworkName.length - 1];
    }

    @Override
    @Cacheable(cacheNames = "cloudResourceRegionCache", key = "#cloudCredential?.id")
    public CloudRegions regions(ExtendedCloudCredential cloudCredential, Region region, Map<String, String> filters,
            boolean availabilityZonesNeeded) throws Exception {
        Compute compute = gcpComputeFactory.buildCompute(cloudCredential);
        String projectId = gcpStackUtil.getProjectId(cloudCredential);

        Map<Region, List<AvailabilityZone>> regionListMap = new HashMap<>();
        Map<Region, String> displayNames = new HashMap<>();
        Map<Region, Coordinate> coordinates = new HashMap<>();

        String defaultRegion = gcpZoneParameterDefault;
        RegionList regionList = compute.regions().list(projectId).execute();
        for (com.google.api.services.compute.model.Region gcpRegion : regionList.getItems()) {
            Coordinate coordinate = regionCoordinates.get(region(gcpRegion.getName()));
            if (coordinate != null && (region == null || Strings.isNullOrEmpty(region.value()) || gcpRegion.getName().equals(region.value()))) {
                List<AvailabilityZone> availabilityZones = new ArrayList<>();
                for (String s : gcpRegion.getZones()) {
                    String[] split = s.split("/");
                    if (split.length > 0) {
                        availabilityZones.add(AvailabilityZone.availabilityZone(split[split.length - 1]));
                    }
                }
                regionListMap.put(region(gcpRegion.getName()), availabilityZones);
                displayNames.put(region(gcpRegion.getName()), displayName(gcpRegion.getName()));
                addCoordinate(coordinates, gcpRegion);
            }
        }
        if (region != null && !Strings.isNullOrEmpty(region.value())) {
            defaultRegion = region.value();
        }
        return new CloudRegions(regionListMap, displayNames, coordinates, defaultRegion, true);
    }

    @Override
    @Cacheable(cacheNames = "cdpCloudResourceRegionCache", key = "'GCP'")
    public CloudRegions cdpEnabledRegions() {
        Map<Region, List<AvailabilityZone>> regionListMap = new HashMap<>();
        Map<Region, String> displayNames = new HashMap<>();
        Map<Region, Coordinate> coordinates = new HashMap<>();


        for (Map.Entry<Region, Coordinate> enabledRegion : regionCoordinates.entrySet()) {
            Set<CdpSupportedServices> cdpServices = enabledRegion.getValue().getCdpSupportedServices()
                    .stream()
                    .map(e -> e.services())
                    .flatMap(list -> list.stream())
                    .collect(Collectors.toSet());

            regionListMap.put(enabledRegion.getKey(), List.of());
            Coordinate regionCoordinateSpecification = enabledRegion.getValue();
            displayNames.put(enabledRegion.getKey(), enabledRegion.getValue().getDisplayName());
            Coordinate coordinate =  coordinate(
                    regionCoordinateSpecification.getLongitude().toString(),
                    regionCoordinateSpecification.getLatitude().toString(),
                    regionCoordinateSpecification.getDisplayName(),
                    regionCoordinateSpecification.getKey(),
                    regionCoordinateSpecification.isK8sSupported(),
                    regionCoordinateSpecification.getEntitlements(),
                    regionCoordinateSpecification.getDefaultDbVmType(),
                    null,
                    cdpServices);
            coordinates.put(enabledRegion.getKey(), coordinate);
        }
        return new CloudRegions(
                regionListMap,
                displayNames,
                coordinates,
                gcpZoneParameterDefault,
                true);
    }

    public void addCoordinate(Map<Region, Coordinate> coordinates, com.google.api.services.compute.model.Region gcpRegion) {
        Coordinate coordinate = regionCoordinates.get(region(gcpRegion.getName()));
        if (coordinate == null || coordinate.getLongitude() == null || coordinate.getLatitude() == null) {
            LOGGER.warn("Unregistered region with location coordinates on gcp side: {} using default California", gcpRegion.getName());
            coordinates.put(region(gcpRegion.getName()), Coordinate.defaultCoordinate());
        } else {
            coordinates.put(region(gcpRegion.getName()), coordinate);
        }
    }

    private String displayName(String word) {
        String[] split = word.split("-");
        List<String> list = Arrays.asList(split);
        Collections.reverse(list);
        StringBuilder sb = new StringBuilder();
        for (String s : list) {
            sb.append(StringUtils.capitalize(s.replaceAll("[0-9]", "")));
            sb.append(' ');
        }
        split = word.split("(?<=\\D)(?=\\d)");
        if (split.length == 2) {
            sb.append(split[1]);
        }
        return sb.toString().trim();
    }

    @Override
    @Cacheable(cacheNames = "cloudResourceVmTypeCache", key = "#cloudCredential?.id + #region.getRegionName()")
    public CloudVmTypes virtualMachines(ExtendedCloudCredential cloudCredential, Region region, Map<String, String> filters) {
        CloudVmTypes cloudVmTypes = getCloudVmTypes(cloudCredential, region, filters);
        return new CloudVmTypes(cloudVmTypes.getCloudVmResponses(), cloudVmTypes.getDefaultCloudVmResponses());
    }

    @Override
    @Cacheable(cacheNames = "cloudResourceVmTypeCache", key = "#cloudCredential?.id + #region.getRegionName() + 'distrox'")
    public CloudVmTypes virtualMachinesForDistroX(ExtendedCloudCredential cloudCredential, Region region, Map<String, String> filters) {
        CloudVmTypes cloudVmTypes = virtualMachines(cloudCredential, region, filters);
        Map<String, Set<VmType>> returnVmResponses = new HashMap<>();
        Map<String, Set<VmType>> cloudVmResponses = cloudVmTypes.getCloudVmResponses();
        if (restrictInstanceTypes) {
            for (Map.Entry<String, Set<VmType>> stringSetEntry : cloudVmResponses.entrySet()) {
                returnVmResponses.put(stringSetEntry.getKey(), stringSetEntry.getValue().stream()
                        .filter(enabledDistroxInstanceTypeFilter)
                        .filter(e -> minimalHardwareFilter
                                .suitableAsMinimumHardware(e.getMetaData().getCPU(), e.getMetaData().getMemoryInGb()))
                        .collect(Collectors.toSet()));
            }
        } else {
            for (Map.Entry<String, Set<VmType>> stringSetEntry : cloudVmResponses.entrySet()) {
                returnVmResponses.put(stringSetEntry.getKey(), stringSetEntry.getValue().stream()
                        .filter(e -> minimalHardwareFilter
                                .suitableAsMinimumHardware(e.getMetaData().getCPU(), e.getMetaData().getMemoryInGb()))
                        .collect(Collectors.toSet()));
            }
        }
        return new CloudVmTypes(returnVmResponses, cloudVmTypes.getDefaultCloudVmResponses());
    }

    private CloudVmTypes getCloudVmTypes(ExtendedCloudCredential cloudCredential, Region region, Map<String, String> filters) {
        Compute compute = gcpComputeFactory.buildCompute(cloudCredential);
        String projectId = gcpStackUtil.getProjectId(cloudCredential);
        Map<String, Set<String>> availabilityZones = getAvailabilityZonesForVmTypes(cloudCredential, region);

        Map<String, Set<VmType>> cloudVmResponses = new HashMap<>();
        Map<String, VmType> defaultCloudVmResponses = new HashMap<>();

        try {
            Set<VmType> types = new HashSet<>();
            VmType defaultVmType = null;

            CloudRegions regions = regions(cloudCredential, region, filters, true);

            for (AvailabilityZone availabilityZone : regions.getCloudRegions().get(region)) {
                MachineTypeList machineTypeList = compute.machineTypes().list(projectId, availabilityZone.value()).execute();
                for (MachineType machineType : machineTypeList.getItems()) {
                    Set<String> availabilityZonesForVm = availabilityZones.getOrDefault(machineType.getName(), new HashSet<>());
                    LOGGER.trace("Availability Zones for VM type {} are {}", machineType.getName(), availabilityZonesForVm);
                    if (matchAvailabilityZones(filters, availabilityZonesForVm)) {
                        VmTypeMetaBuilder vmTypeMetaBuilder = VmTypeMetaBuilder.builder()
                                .withCpuAndMemory(machineType.getGuestCpus(),
                                        machineType.getMemoryMb().floatValue() / THOUSAND)

                                .withMagneticConfig(TEN, machineType.getMaximumPersistentDisksSizeGb().intValue(),
                                        1, machineType.getMaximumPersistentDisks())

                                .withSsdConfig(TEN, machineType.getMaximumPersistentDisksSizeGb().intValue(),
                                        1, machineType.getMaximumPersistentDisks())

                                .withBalancedHddConfig(TEN, machineType.getMaximumPersistentDisksSizeGb().intValue(),
                                        1, machineType.getMaximumPersistentDisks())

                                .withMaximumPersistentDisksSizeGb(machineType.getMaximumPersistentDisksSizeGb())
                                .withVolumeEncryptionSupport(true);
                        if (isLocalSsdSupportedForInstanceType(machineType)) {
                            LOGGER.trace("Adding the local disk configurations to the instance {}.", machineType);
                            vmTypeMetaBuilder.withLocalSsdConfig(
                                    Set.of(GCP_LOCAL_SSD_ALLOWED_VALUES),
                                    GCP_LOCAL_SSD_POSSIBLE_NUMBER_VALUES
                            );
                        }
                        if (isExtremeSsdSupportedForInstanceType(machineType)) {
                            LOGGER.trace("Adding the extreme disk configurations to the instance {}.", machineType);
                            vmTypeMetaBuilder.withExtremeSsdConfig(
                                    TEN,
                                    THOUSAND_FIVE_HUNDRED,
                                    1,
                                    machineType.getMaximumPersistentDisks());
                        }
                        vmTypeMetaBuilder.withAvailabilityZones(new ArrayList<>(availabilityZonesForVm));
                        VmType vmType = VmType.vmTypeWithMeta(machineType.getName(), vmTypeMetaBuilder.create(), true);
                        types.add(vmType);
                        if (machineType.getName().equals(gcpVmDefault)) {
                            defaultVmType = vmType;
                        }
                    } else {
                        LOGGER.debug("{} with Availability Zones {} is not supported in requested Availability Zones. Do not add it to the result",
                                machineType.getName(), availabilityZonesForVm);
                    }
                }
                cloudVmResponses.put(availabilityZone.value(), types);
                defaultCloudVmResponses.put(availabilityZone.value(), defaultVmType);
            }
            return new CloudVmTypes(cloudVmResponses, defaultCloudVmResponses);
        } catch (Exception e) {
            return new CloudVmTypes(new HashMap<>(), new HashMap<>());
        }
    }

    private boolean isLocalSsdSupportedForInstanceType(MachineType machineType) {
        return MACHINE_TYPES_WITH_LOCAL_SSD.contains(getMachineTypeFamily(machineType));
    }

    private boolean isExtremeSsdSupportedForInstanceType(MachineType machineType) {
        return extremeDiskCalculator.extremeDiskSupported(machineType);
    }

    private String getMachineTypeFamily(MachineType machineType) {
        return machineType.getName().toLowerCase(Locale.ROOT).split("-")[0];
    }

    @Override
    public CloudGateWays gateways(ExtendedCloudCredential cloudCredential, Region region, Map<String, String> filters) {
        return new CloudGateWays();
    }

    @Override
    public CloudIpPools publicIpPool(ExtendedCloudCredential cloudCredential, Region region, Map<String, String> filters) {
        return new CloudIpPools();
    }

    @Override
    public CloudAccessConfigs accessConfigs(ExtendedCloudCredential cloudCredential, Region region, Map<String, String> filters) {
        Iam iam = gcpIamFactory.buildIam(cloudCredential);
        String projectId = gcpStackUtil.getProjectId(cloudCredential);
        Set<CloudAccessConfig> collect = new HashSet<>();
        try {
            Iam.Projects.ServiceAccounts.List listServiceAccountEmailsRequest = iam
                    .projects()
                    .serviceAccounts()
                    .list("projects/" + projectId)
                    .setPageSize(DEFAULT_PAGE_SIZE);
            ListServiceAccountsResponse response;
            do {
                response = listServiceAccountEmailsRequest.execute();
                Set<CloudAccessConfig> accessConfigs = response
                        .getAccounts()
                        .stream()
                        .map(e -> new CloudAccessConfig(e.getName(), e.getEmail(), new HashMap<>()))
                        .collect(Collectors.toSet());
                collect.addAll(accessConfigs);
                listServiceAccountEmailsRequest.setPageToken(response.getNextPageToken());
            } while (response.getNextPageToken() != null);
            return new CloudAccessConfigs(collect);
        } catch (Exception ex) {
            return new CloudAccessConfigs(collect);
        }
    }

    @Override
    public boolean isEncryptionKeyUsable(ExtendedCloudCredential cloudCredential, String region, String keyArn) {
        throw new UnsupportedOperationException("Method not implemented");
    }

    @Override
    public CloudEncryptionKeys encryptionKeys(ExtendedCloudCredential cloudCredential, Region region, Map<String, String> filters) {
        CloudKMS cloudKMS;
        try {
            cloudKMS = gcpCloudKMSFactory.buildCloudKMS(cloudCredential);
        } catch (Exception e) {
            LOGGER.warn("Failed to build CloudKMS client.", e);
            return new CloudEncryptionKeys(new HashSet<>());
        }

        String projectId = gcpStackUtil.getProjectId(cloudCredential);
        Set<CloudEncryptionKey> cloudEncryptionKeys = getKeyRingList(cloudKMS, projectId, region.getRegionName()).stream().parallel()
                .map(KeyRing::getName)
                .map(toCryptoKeyPathList(cloudKMS, projectId, region.getRegionName()))
                .flatMap(Set::stream)
                .collect(Collectors.toSet());

        return new CloudEncryptionKeys(cloudEncryptionKeys);
    }

    @Override
    public CloudNoSqlTables noSqlTables(ExtendedCloudCredential cloudCredential, Region region, Map<String, String> filters) {
        LOGGER.warn("NoSQL table list is not supported on 'GCP'");
        return new CloudNoSqlTables(new ArrayList<>());
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
    public PlatformDatabaseCapabilities databaseCapabilities(CloudCredential cloudCredential, Region region, Map<String, String> filters) {
        try {
            CloudRegions regions = regions((ExtendedCloudCredential) cloudCredential, region, filters, false);
            Map<Region, String> regionDefaultInstanceTypeMap = new HashMap<>();
            for (Region actualRegion : regions.getCloudRegions().keySet()) {
                String defaultDbVmType = regionCoordinates.get(actualRegion).getDefaultDbVmType();
                regionDefaultInstanceTypeMap.put(actualRegion, defaultDbVmType == null ? gcpDatabaseVmDefault : defaultDbVmType);
            }
            return new PlatformDatabaseCapabilities(new HashMap<>(), regionDefaultInstanceTypeMap, new HashMap<>());
        } catch (Exception e) {
            return new PlatformDatabaseCapabilities(new HashMap<>(), new HashMap<>(), new HashMap<>());
        }
    }

    public Map<String, Set<String>> getAvailabilityZonesForVmTypes(ExtendedCloudCredential cloudCredential, Region region) {
        Compute compute = gcpComputeFactory.buildCompute(cloudCredential);
        String projectId = gcpStackUtil.getProjectId(cloudCredential);

        Map<String, Set<String>> availabilityZonesForVmTypes = new HashMap<>();

        try {
            CloudRegions regions = regions(cloudCredential, region, null, true);
            for (AvailabilityZone availabilityZone : regions.getCloudRegions().get(region)) {
                MachineTypeList machineTypeList = compute.machineTypes().list(projectId, availabilityZone.value()).execute();
                for (MachineType machineType : machineTypeList.getItems()) {
                    availabilityZonesForVmTypes.putIfAbsent(machineType.getName(), new HashSet<>());
                    availabilityZonesForVmTypes.get(machineType.getName()).add(availabilityZone.value());
                }
            }
        } catch (Exception e) {
            LOGGER.error("Failed to get availability zones: {}", e);
        }

        return availabilityZonesForVmTypes;
    }

    private List<KeyRing> getKeyRingList(CloudKMS cloudKMS, String projectId, String regionName) {
        String keyRingPath = String.format("projects/%s/locations/%s", projectId, regionName);
        try {
            ListKeyRingsResponse response = cloudKMS.projects().locations()
                    .keyRings()
                    .list(keyRingPath)
                    .execute();
            return Optional.ofNullable(response.getKeyRings()).orElse(List.of());
        } catch (TokenResponseException e) {
            throw gcpStackUtil.getMissingServiceAccountKeyError(e, projectId);
        } catch (IOException e) {
            LOGGER.info("Failed to get list of keyrings on keyring path: [{}].", keyRingPath, e);
            return List.of();
        }
    }

    private Function<String, Set<CloudEncryptionKey>> toCryptoKeyPathList(CloudKMS cloudKMS, String projectId, String location) {
        return keyRing -> getCryptoKeysList(cloudKMS, keyRing).stream()
                .map(toCloudEncryptionKey(projectId, keyRing, location))
                .collect(Collectors.toSet());
    }

    private Function<CryptoKey, CloudEncryptionKey> toCloudEncryptionKey(String projectId, String keyRing, String location) {
        return cryptoKey -> {
            Map<String, Object> metadata = new HashMap<>(Optional.ofNullable(cryptoKey.getLabels()).orElse(Map.of()));
            Pattern pattern = Pattern.compile(".*\\/keyRings\\/(.+)\\/cryptoKeys\\/([a-z0-9\\-]+)");
            Matcher matcher = pattern.matcher(cryptoKey.getName());

            String displayName = matcher.matches() ? String.format("%s/%s", matcher.group(1), matcher.group(2)) : cryptoKey.getName();
            String name = cryptoKey.getPrimary().getName();
            return new CloudEncryptionKey(name, name, cryptoKey.getPurpose(), displayName, metadata);
        };
    }

    private List<CryptoKey> getCryptoKeysList(CloudKMS cloudKMS, String cryptoKeysPath) {
        try {
            ListCryptoKeysResponse listCryptoKeysResponse = cloudKMS.projects().locations().keyRings()
                    .cryptoKeys()
                    .list(cryptoKeysPath)
                    .execute();
            return Optional.ofNullable(listCryptoKeysResponse.getCryptoKeys()).orElse(List.of());
        } catch (IOException e) {
            LOGGER.info("Failed to get list of crypto keys on keyring path: [{}].", cryptoKeysPath, e);
            return List.of();
        }
    }

    private boolean matchAvailabilityZones(Map<String, String> filters, Set<String> availabilityZones) {
        return filters == null || StringUtils.isEmpty(filters.get(NetworkConstants.AVAILABILITY_ZONES)) ||
                CollectionUtils.containsAll(emptyIfNull(availabilityZones),
                        Splitter.on(",").splitToList(filters.get(NetworkConstants.AVAILABILITY_ZONES))
                );
    }
}
