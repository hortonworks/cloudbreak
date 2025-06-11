package com.sequenceiq.cloudbreak.cloud.mock;

import static com.sequenceiq.cloudbreak.cloud.model.Coordinate.coordinate;
import static com.sequenceiq.cloudbreak.cloud.model.Platform.platform;
import static com.sequenceiq.cloudbreak.cloud.model.Region.region;
import static com.sequenceiq.cloudbreak.cloud.model.database.CloudDatabaseServerSslCertificateType.ROOT;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import com.google.common.collect.Maps;
import com.sequenceiq.cloudbreak.cloud.PlatformResources;
import com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone;
import com.sequenceiq.cloudbreak.cloud.model.CloudAccessConfigs;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudEncryptionKeys;
import com.sequenceiq.cloudbreak.cloud.model.CloudGateWays;
import com.sequenceiq.cloudbreak.cloud.model.CloudIpPools;
import com.sequenceiq.cloudbreak.cloud.model.CloudNetwork;
import com.sequenceiq.cloudbreak.cloud.model.CloudNetworks;
import com.sequenceiq.cloudbreak.cloud.model.CloudRegions;
import com.sequenceiq.cloudbreak.cloud.model.CloudSecurityGroups;
import com.sequenceiq.cloudbreak.cloud.model.CloudSshKeys;
import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmTypes;
import com.sequenceiq.cloudbreak.cloud.model.Coordinate;
import com.sequenceiq.cloudbreak.cloud.model.ExtendedCloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.PlatformDatabaseCapabilities;
import com.sequenceiq.cloudbreak.cloud.model.Region;
import com.sequenceiq.cloudbreak.cloud.model.RegionCoordinateSpecification;
import com.sequenceiq.cloudbreak.cloud.model.RegionCoordinateSpecifications;
import com.sequenceiq.cloudbreak.cloud.model.VmType;
import com.sequenceiq.cloudbreak.cloud.model.VmTypeMeta;
import com.sequenceiq.cloudbreak.cloud.model.VolumeParameterConfig;
import com.sequenceiq.cloudbreak.cloud.model.VolumeParameterType;
import com.sequenceiq.cloudbreak.cloud.model.database.CloudDatabaseServerSslCertificate;
import com.sequenceiq.cloudbreak.cloud.model.database.CloudDatabaseServerSslCertificates;
import com.sequenceiq.cloudbreak.cloud.model.dns.CloudPrivateDnsZones;
import com.sequenceiq.cloudbreak.cloud.model.nosql.CloudNoSqlTables;
import com.sequenceiq.cloudbreak.cloud.model.resourcegroup.CloudResourceGroups;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.service.CloudbreakResourceReaderService;

@Service
public class MockPlatformResources implements PlatformResources {

    static final String[] LONDON_AVAILABILITY_ZONES = {"london-a", "london-b"};

    static final String[] USA_AVAILABILITY_ZONES = {"usa-a", "usa-b", "usa-c"};

    @Value("${cb.platform.default.regions:}")
    private String defaultRegions;

    @Value("${cb.mock.default.database.vmtype:db.m5.large}")
    private String mockDatabaseVmDefault;

    private String defaultRegion;

    private VmType defaultVmType;

    @Inject
    private MockUrlFactory mockUrlFactory;

    private enum MockedVmTypes {

        SMALL("small", 2, 200.0F),
        MEDIUM("medium", 8, 800.0F),
        LARGE("large", 10, 1000.0F),
        XLARGE("xlarge", 15, 2000.0F);

        private final String value;

        private final Integer cpu;

        private final Float memory;

        MockedVmTypes(String value, Integer cpu, Float memory) {
            this.value = value;
            this.cpu = cpu;
            this.memory = memory;
        }

        public String value() {
            return value;
        }

        public Integer cpu() {
            return cpu;
        }

        public Float memory() {
            return memory;
        }

        public VmTypeMeta getVmTypeMeta() {
            VmTypeMeta vmTypeMeta = new VmTypeMeta();
            vmTypeMeta.setSsdConfig(getVolumeConfig(VolumeParameterType.SSD));
            vmTypeMeta.setEphemeralConfig(getVolumeConfig(VolumeParameterType.EPHEMERAL));
            vmTypeMeta.setMagneticConfig(getVolumeConfig(VolumeParameterType.MAGNETIC));
            vmTypeMeta.setAutoAttachedConfig(getVolumeConfig(VolumeParameterType.AUTO_ATTACHED));
            vmTypeMeta.getProperties().put(VmTypeMeta.CPU, cpu);
            vmTypeMeta.getProperties().put(VmTypeMeta.MEMORY, memory);
            return vmTypeMeta;
        }

        private VolumeParameterConfig getVolumeConfig(VolumeParameterType ssd) {
            return new VolumeParameterConfig(ssd, 1, Integer.MAX_VALUE, 0, Integer.MAX_VALUE);
        }
    }

    private Map<Region, List<AvailabilityZone>> regions = new HashMap<>();

    private Map<String, Set<VmType>> vmTypes = new HashMap<>();

    private Map<String, VmType> defaultVmTypes = new HashMap<>();

    private Map<Region, Coordinate> regionCoordinates = new HashMap<>();

    private final Map<Region, String> regionDisplayNames = new HashMap<>();

    @Inject
    private CloudbreakResourceReaderService cloudbreakResourceReaderService;

    @PostConstruct
    public void init() {
        regions = readRegionsMock();
        vmTypes = readVmTypes();
        defaultRegion = getDefaultRegion().getRegionName();
        defaultVmType = vmTypes.get(vmTypes.keySet().iterator().next()).iterator().next();
        defaultVmTypes = readDefaultVmTypes();
        regionCoordinates = readRegionCoordinates(resourceDefinition("zone-coordinates"));
    }

    public String resourceDefinition(String resource) {
        return cloudbreakResourceReaderService.resourceDefinition("mock", resource);
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

    private Map<String, VmType> readDefaultVmTypes() {
        Map<String, VmType> result = new HashMap<>();
        for (Region region : regions.keySet()) {
            result.put(region.getRegionName(), defaultVmType);
        }
        return result;
    }

    private Map<Region, List<AvailabilityZone>> readRegionsMock() {
        Map<Region, List<AvailabilityZone>> regions = new HashMap<>();
        regions.put(region("USA"), getAvailabilityZones(USA_AVAILABILITY_ZONES));
        regions.put(region("Europe"), getAvailabilityZones(LONDON_AVAILABILITY_ZONES));
        return regions;
    }

    private Map<String, Set<VmType>> readVmTypes() {
        Map<String, Set<VmType>> availabilityZoneHashMap = new HashMap<>();
        Set<VmType> vmTypeList = new HashSet<>();
        for (MockedVmTypes vmType : MockedVmTypes.values()) {
            vmTypeList.add(VmType.vmTypeWithMeta(vmType.value, vmType.getVmTypeMeta(), true));
        }

        for (Entry<Region, List<AvailabilityZone>> regionListEntry : regions.entrySet()) {
            for (AvailabilityZone availabilityZone : regionListEntry.getValue()) {
                availabilityZoneHashMap.put(availabilityZone.value(), vmTypeList);
            }
        }
        return availabilityZoneHashMap;
    }

    private List<AvailabilityZone> getAvailabilityZones(String[] availabilityZones) {
        List<AvailabilityZone> availabilityZoneList = new ArrayList<>();
        for (String availabilityZone : availabilityZones) {
            availabilityZoneList.add(new AvailabilityZone(availabilityZone));
        }
        return availabilityZoneList;
    }

    @Override
    public CloudNetworks networks(ExtendedCloudCredential cloudCredential, Region region, Map<String, String> filters) {
        Map<String, Set<CloudNetwork>> result = new HashMap<>();
        String network1Id = "vpc1";
        result.put(network1Id, getCloudNetworks(network1Id, LONDON_AVAILABILITY_ZONES));
        return new CloudNetworks(result);
    }

    private Set<CloudNetwork> getCloudNetworks(String name, String[] azs) {
        Set<CloudSubnet> subnets = new HashSet<>();

        for (int i = 0; i < azs.length; i++) {
            String londonAvailabilityZone = azs[i];
            String subnetId = name + "-subnet" + i;
            subnets.add(
                new CloudSubnet.Builder()
                    .id(subnetId)
                    .name(subnetId)
                    .availabilityZone(londonAvailabilityZone)
                    .cidr("192.168.0.0/16")
                    .build()
            );
        }
        return Set.of(new CloudNetwork(name, name, subnets, new HashMap<>()));
    }

    @Override
    public CloudSshKeys sshKeys(ExtendedCloudCredential cloudCredential, Region region, Map<String, String> filters) {
        return new CloudSshKeys();
    }

    @Override
    public CloudSecurityGroups securityGroups(ExtendedCloudCredential cloudCredential, Region region, Map<String, String> filters) {
        return new CloudSecurityGroups();
    }

    @Override
    @Cacheable(cacheNames = "cloudResourceRegionCache", key = "#cloudCredential?.id")
    public CloudRegions regions(ExtendedCloudCredential cloudCredential, Region region, Map<String, String> filters,
            boolean availabilityZonesNeeded) {
        return new CloudRegions(regions, regionDisplayNames, regionCoordinates, defaultRegion, true);
    }

    @Override
    @Cacheable(cacheNames = "cloudResourceVmTypeCache", key = "#cloudCredential?.id + #region.getRegionName()")
    public CloudVmTypes virtualMachines(ExtendedCloudCredential cloudCredential, Region region, Map<String, String> filters) {
        return new CloudVmTypes(vmTypes, defaultVmTypes);
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
        return new CloudAccessConfigs(new HashSet<>());
    }

    @Override
    public boolean isEncryptionKeyUsable(ExtendedCloudCredential cloudCredential, String region, String keyArn) {
        throw new UnsupportedOperationException("Method not implemented");
    }

    @Override
    public CloudEncryptionKeys encryptionKeys(ExtendedCloudCredential cloudCredential, Region region, Map<String, String> filters) {
        return new CloudEncryptionKeys(new HashSet<>());
    }

    @Override
    public CloudNoSqlTables noSqlTables(ExtendedCloudCredential cloudCredential, Region region, Map<String, String> filters) {
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

    private Region getRegionByName(String name) {
        for (Region region : regions.keySet()) {
            if (name.equals(region.value())) {
                return region;
            }
        }
        return null;
    }

    private Region getDefaultRegion() {
        Map<Platform, Region> defaultRegionsMap = Maps.newHashMap();
        if (isNotEmpty(defaultRegions)) {
            for (String entry : defaultRegions.split(",")) {
                String[] keyValue = entry.split(":");
                defaultRegionsMap.put(platform(keyValue[0]), region(keyValue[1]));
            }
            Region platformRegion = defaultRegionsMap.get(platform(MockConstants.MOCK));
            if (platformRegion != null && !isEmpty(platformRegion.value())) {
                return getRegionByName(platformRegion.value());
            }
        }
        return regions.keySet().iterator().next();
    }

    @Override
    public CloudDatabaseServerSslCertificates databaseServerGeneralSslRootCertificates(CloudCredential cloudCredential, Region region) {
        String[] certificates = mockUrlFactory.get("/db/certificates").get(String[].class);
        Set<CloudDatabaseServerSslCertificate> setOfCertificates = Stream.of(certificates).map(
                certificate -> new CloudDatabaseServerSslCertificate(ROOT, certificate))
                    .collect(Collectors.toSet());

        return new CloudDatabaseServerSslCertificates(setOfCertificates);
    }

    @Override
    public PlatformDatabaseCapabilities databaseCapabilities(CloudCredential cloudCredential, Region region, Map<String, String> filters) {
        try {
            CloudRegions regions = regions((ExtendedCloudCredential) cloudCredential, region, filters, false);
            Map<Region, String> regionDefaultInstanceTypeMap = new HashMap<>();
            for (Region actualRegion : regions.getCloudRegions().keySet()) {
                String defaultDbVmType = regionCoordinates.get(actualRegion).getDefaultDbVmType();
                regionDefaultInstanceTypeMap.put(actualRegion, defaultDbVmType == null ? mockDatabaseVmDefault : defaultDbVmType);
            }
            return new PlatformDatabaseCapabilities(new HashMap<>(), regionDefaultInstanceTypeMap, new HashMap<>());
        } catch (Exception e) {
            return new PlatformDatabaseCapabilities(new HashMap<>(), new HashMap<>(), new HashMap<>());
        }
    }
}
