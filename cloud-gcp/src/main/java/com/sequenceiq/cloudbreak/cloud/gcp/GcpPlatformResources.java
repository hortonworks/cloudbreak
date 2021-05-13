package com.sequenceiq.cloudbreak.cloud.gcp;

import static com.sequenceiq.cloudbreak.cloud.gcp.util.GcpStackUtil.SHARED_PROJECT_ID;
import static com.sequenceiq.cloudbreak.cloud.gcp.util.GcpStackUtil.getMissingServiceAccountKeyError;
import static com.sequenceiq.cloudbreak.cloud.model.Coordinate.coordinate;
import static com.sequenceiq.cloudbreak.cloud.model.Region.region;
import static com.sequenceiq.cloudbreak.cloud.model.network.SubnetType.PRIVATE;
import static com.sequenceiq.cloudbreak.cloud.model.network.SubnetType.PUBLIC;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

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
import com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.cloud.PlatformResources;
import com.sequenceiq.cloudbreak.filter.MinimalHardwareFilter;
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
import com.sequenceiq.cloudbreak.cloud.model.Region;
import com.sequenceiq.cloudbreak.cloud.model.RegionCoordinateSpecification;
import com.sequenceiq.cloudbreak.cloud.model.RegionCoordinateSpecifications;
import com.sequenceiq.cloudbreak.cloud.model.VmType;
import com.sequenceiq.cloudbreak.cloud.model.VmTypeMeta;
import com.sequenceiq.cloudbreak.cloud.model.VmTypeMeta.VmTypeMetaBuilder;
import com.sequenceiq.cloudbreak.cloud.model.nosql.CloudNoSqlTables;
import com.sequenceiq.cloudbreak.cloud.model.resourcegroup.CloudResourceGroups;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.service.CloudbreakResourceReaderService;

@Service
public class GcpPlatformResources implements PlatformResources {

    private static final Logger LOGGER = LoggerFactory.getLogger(GcpPlatformResources.class);

    private static final float THOUSAND = 1000.0f;

    private static final int TEN = 10;

    private static final int DEFAULT_PAGE_SIZE = 50;

    @Value("${cb.gcp.default.vmtype:n2-highcpu-8}")
    private String gcpVmDefault;

    @Value("${cb.gcp.zone.parameter.default:europe-west1}")
    private String gcpZoneParameterDefault;

    @Value("${cb.gcp.distrox.enabled.instance.types:}")
    private List<String> enabledDistroxInstanceTypes;

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

    private Map<Region, Coordinate> regionCoordinates = new HashMap<>();

    private final Predicate<VmType> enabledDistroxInstanceTypeFilter = vmt -> enabledDistroxInstanceTypes.stream()
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
                                regionCoordinateSpecification.isK8sSupported()));
            }
        } catch (IOException ignored) {
            return regionCoordinates;
        }
        return regionCoordinates;
    }

    @Override
    public CloudNetworks networks(CloudCredential cloudCredential, Region region, Map<String, String> filters) throws Exception {
        Compute compute = gcpComputeFactory.buildCompute(cloudCredential);
        String projectId = GcpStackUtil.getProjectId(cloudCredential);
        Map<String, Set<CloudNetwork>> result = new HashMap<>();

        String networkId = null;
        List<String> subnetIds = new ArrayList<>();
        String sharedProjectId = null;
        if (filters != null) {
            networkId = filters.getOrDefault("networkId", null);
            subnetIds = getSubnetIds(filters);
            sharedProjectId = filters.getOrDefault("sharedProjectId", null);
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
                .findFirst()
                .map(tmpZone -> tmpZone.substring(tmpZone.lastIndexOf('/') + 1))
                .orElse(null);
        LOGGER.debug("Zone chosen for the subnets is {}", zone);
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
                                new CloudSubnet(
                                        subnetwork.getId().toString(),
                                        subnetwork.getName(),
                                        zone,
                                        subnetwork.getIpCidrRange(),
                                        subnetwork.getPrivateIpGoogleAccess(),
                                        !subnetwork.getPrivateIpGoogleAccess(),
                                        igwAvailable,
                                        igwAvailable ? PUBLIC : PRIVATE));
                    }
                }
            }

            CloudNetwork cloudNetwork = new CloudNetwork(network.getName(), network.getId().toString(), subnets, properties);
            cloudNetworks.add(cloudNetwork);
        }
        result.put(region.value(), cloudNetworks);

        return new CloudNetworks(result);
    }

    public List<String> getSubnetIds(Map<String, String> filters) {
        List<String> subnetIds = new ArrayList<>();
        String subnetId = filters.getOrDefault("subnetId", null);
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
                    LOGGER.info("Could not get Subnetworks from {} project with {} subnetId: {}", tmpProjectId, subnetId, e);
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
    public CloudSshKeys sshKeys(CloudCredential cloudCredential, Region region, Map<String, String> filters) {
        return new CloudSshKeys();
    }

    @Override
    public CloudSecurityGroups securityGroups(CloudCredential cloudCredential, Region region, Map<String, String> filters) throws IOException {
        Compute compute = gcpComputeFactory.buildCompute(cloudCredential);
        String projectId = GcpStackUtil.getProjectId(cloudCredential);

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
    public CloudRegions regions(CloudCredential cloudCredential, Region region, Map<String, String> filters, boolean availabilityZonesNeeded) throws Exception {
        Compute compute = gcpComputeFactory.buildCompute(cloudCredential);
        String projectId = GcpStackUtil.getProjectId(cloudCredential);

        Map<Region, List<AvailabilityZone>> regionListMap = new HashMap<>();
        Map<Region, String> displayNames = new HashMap<>();
        Map<Region, Coordinate> coordinates = new HashMap<>();

        String defaultRegion = gcpZoneParameterDefault;
        RegionList regionList = compute.regions().list(projectId).execute();
        for (com.google.api.services.compute.model.Region gcpRegion : regionList.getItems()) {
            if (region == null || Strings.isNullOrEmpty(region.value()) || gcpRegion.getName().equals(region.value())) {
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
    public CloudVmTypes virtualMachines(CloudCredential cloudCredential, Region region, Map<String, String> filters) {
        CloudVmTypes cloudVmTypes = getCloudVmTypes(cloudCredential, region, filters);
        return new CloudVmTypes(cloudVmTypes.getCloudVmResponses(), cloudVmTypes.getDefaultCloudVmResponses());
    }

    @Override
    @Cacheable(cacheNames = "cloudResourceVmTypeCache", key = "#cloudCredential?.id + #region.getRegionName() + 'distrox'")
    public CloudVmTypes virtualMachinesForDistroX(CloudCredential cloudCredential, Region region, Map<String, String> filters) {
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

    private CloudVmTypes getCloudVmTypes(CloudCredential cloudCredential, Region region, Map<String, String> filters) {
        Compute compute = gcpComputeFactory.buildCompute(cloudCredential);
        String projectId = GcpStackUtil.getProjectId(cloudCredential);

        Map<String, Set<VmType>> cloudVmResponses = new HashMap<>();
        Map<String, VmType> defaultCloudVmResponses = new HashMap<>();

        try {
            Set<VmType> types = new HashSet<>();
            VmType defaultVmType = null;

            CloudRegions regions = regions(cloudCredential, region, filters, true);

            for (AvailabilityZone availabilityZone : regions.getCloudRegions().get(region)) {
                MachineTypeList machineTypeList = compute.machineTypes().list(projectId, availabilityZone.value()).execute();
                for (MachineType machineType : machineTypeList.getItems()) {
                    VmTypeMeta vmTypeMeta = VmTypeMetaBuilder.builder()
                            .withCpuAndMemory(machineType.getGuestCpus(),
                                    machineType.getMemoryMb().floatValue() / THOUSAND)

                            .withMagneticConfig(TEN, machineType.getMaximumPersistentDisksSizeGb().intValue(),
                                    1, machineType.getMaximumPersistentDisksSizeGb().intValue())

                            .withSsdConfig(TEN, machineType.getMaximumPersistentDisksSizeGb().intValue(),
                                    1, machineType.getMaximumPersistentDisks())

                            .withMaximumPersistentDisksSizeGb(machineType.getMaximumPersistentDisksSizeGb())
                            .withVolumeEncryptionSupport(true)
                            .create();
                    VmType vmType = VmType.vmTypeWithMeta(machineType.getName(), vmTypeMeta, true);
                    types.add(vmType);
                    if (machineType.getName().equals(gcpVmDefault)) {
                        defaultVmType = vmType;
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

    @Override
    public CloudGateWays gateways(CloudCredential cloudCredential, Region region, Map<String, String> filters) {
        return new CloudGateWays();
    }

    @Override
    public CloudIpPools publicIpPool(CloudCredential cloudCredential, Region region, Map<String, String> filters) {
        return new CloudIpPools();
    }

    @Override
    public CloudAccessConfigs accessConfigs(CloudCredential cloudCredential, Region region, Map<String, String> filters) {
        Iam iam = gcpIamFactory.buildIam(cloudCredential);
        String projectId = GcpStackUtil.getProjectId(cloudCredential);
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
    public CloudEncryptionKeys encryptionKeys(CloudCredential cloudCredential, Region region, Map<String, String> filters) {
        CloudKMS cloudKMS;
        try {
            cloudKMS = gcpCloudKMSFactory.buildCloudKMS(cloudCredential);
        } catch (Exception e) {
            LOGGER.warn("Failed to build CloudKMS client.", e);
            return new CloudEncryptionKeys(new HashSet<>());
        }

        String projectId = GcpStackUtil.getProjectId(cloudCredential);
        Set<CloudEncryptionKey> cloudEncryptionKeys = getKeyRingList(cloudKMS, projectId, region.getRegionName()).stream().parallel()
                .map(KeyRing::getName)
                .map(toCryptoKeyPathList(cloudKMS, projectId, region.getRegionName()))
                .flatMap(Set::stream)
                .collect(Collectors.toSet());

        return new CloudEncryptionKeys(cloudEncryptionKeys);
    }

    @Override
    public CloudNoSqlTables noSqlTables(CloudCredential cloudCredential, Region region, Map<String, String> filters) {
        LOGGER.warn("NoSQL table list is not supported on 'GCP'");
        return new CloudNoSqlTables(new ArrayList<>());
    }

    @Override
    public CloudResourceGroups resourceGroups(CloudCredential cloudCredential, Region region, Map<String, String> filters) {
        return new CloudResourceGroups();
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
            throw getMissingServiceAccountKeyError(e, projectId);
        }  catch (IOException e) {
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
}
