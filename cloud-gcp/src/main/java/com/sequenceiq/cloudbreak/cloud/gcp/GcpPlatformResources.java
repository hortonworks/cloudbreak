package com.sequenceiq.cloudbreak.cloud.gcp;

import static com.sequenceiq.cloudbreak.cloud.model.Coordinate.coordinate;
import static com.sequenceiq.cloudbreak.cloud.model.Region.region;

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
import com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.cloud.PlatformResources;
import com.sequenceiq.cloudbreak.cloud.gcp.util.GcpStackUtil;
import com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone;
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
import com.sequenceiq.cloudbreak.cloud.model.CloudVmTypes;
import com.sequenceiq.cloudbreak.cloud.model.Coordinate;
import com.sequenceiq.cloudbreak.cloud.model.Region;
import com.sequenceiq.cloudbreak.cloud.model.RegionCoordinateSpecification;
import com.sequenceiq.cloudbreak.cloud.model.RegionCoordinateSpecifications;
import com.sequenceiq.cloudbreak.cloud.model.VmType;
import com.sequenceiq.cloudbreak.cloud.model.VmTypeMeta;
import com.sequenceiq.cloudbreak.cloud.model.VmTypeMeta.VmTypeMetaBuilder;
import com.sequenceiq.cloudbreak.service.CloudbreakResourceReaderService;
import com.sequenceiq.cloudbreak.util.JsonUtil;

@Service
public class GcpPlatformResources implements PlatformResources {

    private static final Logger LOGGER = LoggerFactory.getLogger(GcpPlatformResources.class);

    private static final float THOUSAND = 1000.0f;

    private static final int TEN = 10;

    @Value("${cb.gcp.default.vmtype:n1-highcpu-8}")
    private String gcpVmDefault;

    @Value("${cb.gcp.zone.parameter.default:europe-west1}")
    private String gcpZoneParameterDefault;

    @Inject
    private CloudbreakResourceReaderService cloudbreakResourceReaderService;

    private Map<Region, Coordinate> regionCoordinates = new HashMap<>();

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
    public CloudNetworks networks(CloudCredential cloudCredential, Region region, Map<String, String> filters) throws Exception {
        Compute compute = GcpStackUtil.buildCompute(cloudCredential);
        String projectId = GcpStackUtil.getProjectId(cloudCredential);
        Map<String, Set<CloudNetwork>> result = new HashMap<>();

        Set<CloudNetwork> cloudNetworks = new HashSet<>();
        NetworkList networkList = compute.networks().list(projectId).execute();
        List<Subnetwork> subnetworkList = compute.subnetworks().list(projectId, region.value()).execute().getItems();
        for (Network network : networkList.getItems()) {
            Map<String, Object> properties = new HashMap<>();
            properties.put("gatewayIPv4", Strings.nullToEmpty(network.getGatewayIPv4()));
            properties.put("description", Strings.nullToEmpty(network.getDescription()));
            properties.put("IPv4Range", Strings.nullToEmpty(network.getIPv4Range()));
            properties.put("creationTimestamp", Strings.nullToEmpty(network.getCreationTimestamp()));

            Map<String, String> subnets = new HashMap<>();
            if (subnetworkList != null && network.getSubnetworks() != null) {
                for (Subnetwork subnetwork : subnetworkList) {
                    if (network.getSubnetworks().contains(subnetwork.getSelfLink())) {
                        subnets.put(subnetwork.getName(), subnetwork.getName());
                    }
                }
            }

            CloudNetwork cloudNetwork = new CloudNetwork(network.getName(), network.getId().toString(), subnets, properties);
            cloudNetworks.add(cloudNetwork);
        }
        result.put(region.value(), cloudNetworks);

        return new CloudNetworks(result);
    }

    @Override
    public CloudSshKeys sshKeys(CloudCredential cloudCredential, Region region, Map<String, String> filters) {
        return new CloudSshKeys();
    }

    @Override
    public CloudSecurityGroups securityGroups(CloudCredential cloudCredential, Region region, Map<String, String> filters) throws IOException {
        Compute compute = GcpStackUtil.buildCompute(cloudCredential);
        String projectId = GcpStackUtil.getProjectId(cloudCredential);

        Map<String, Set<CloudSecurityGroup>> result = new HashMap<>();
        FirewallList firewallList = compute.firewalls().list(projectId).execute();
        for (Firewall firewall : firewallList.getItems()) {
            Map<String, Object> properties = new HashMap<>();
            properties.put("network", getNetworkName(firewall));
            CloudSecurityGroup cloudSecurityGroup = new CloudSecurityGroup(firewall.getName(), firewall.getName(), properties);
            result.computeIfAbsent(region.value(), k -> new HashSet<>()).add(cloudSecurityGroup);
        }

        return new CloudSecurityGroups(result);
    }

    private String getNetworkName(Firewall firewall) {
        String[] splittedNetworkName = firewall.getNetwork().split("/");
        return splittedNetworkName[splittedNetworkName.length - 1];
    }

    @Override
    @Cacheable(cacheNames = "cloudResourceRegionCache", key = "#cloudCredential?.id")
    public CloudRegions regions(CloudCredential cloudCredential, Region region, Map<String, String> filters) throws Exception {
        Compute compute = GcpStackUtil.buildCompute(cloudCredential);
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
        Compute compute = GcpStackUtil.buildCompute(cloudCredential);
        String projectId = GcpStackUtil.getProjectId(cloudCredential);

        Map<String, Set<VmType>> cloudVmResponses = new HashMap<>();
        Map<String, VmType> defaultCloudVmResponses = new HashMap<>();

        try {
            Set<VmType> types = new HashSet<>();
            VmType defaultVmType = null;

            CloudRegions regions = regions(cloudCredential, region, filters);

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
        return new CloudAccessConfigs(new HashSet<>());
    }

    @Override
    public CloudEncryptionKeys encryptionKeys(CloudCredential cloudCredential, Region region, Map<String, String> filters) {
        CloudKMS cloudKMS;
        try {
            cloudKMS = GcpStackUtil.buildCloudKMS(cloudCredential);
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

    private List<KeyRing> getKeyRingList(CloudKMS cloudKMS, String projectId, String regionName) {
        String keyRingPath = String.format("projects/%s/locations/%s", projectId, regionName);
        try {
            ListKeyRingsResponse response = cloudKMS.projects().locations()
                    .keyRings()
                    .list(keyRingPath)
                    .execute();
            return Optional.ofNullable(response.getKeyRings()).orElse(List.of());
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
}
