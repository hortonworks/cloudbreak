package com.sequenceiq.cloudbreak.cloud.mock;

import static com.sequenceiq.cloudbreak.cloud.model.Platform.platform;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.isNoneEmpty;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import com.google.common.collect.Maps;
import com.sequenceiq.cloudbreak.cloud.PlatformResources;
import com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone;
import com.sequenceiq.cloudbreak.cloud.model.CloudAccessConfigs;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudGateWays;
import com.sequenceiq.cloudbreak.cloud.model.CloudIpPools;
import com.sequenceiq.cloudbreak.cloud.model.CloudNetworks;
import com.sequenceiq.cloudbreak.cloud.model.CloudRegions;
import com.sequenceiq.cloudbreak.cloud.model.CloudSecurityGroups;
import com.sequenceiq.cloudbreak.cloud.model.CloudSshKeys;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmTypes;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.Region;
import com.sequenceiq.cloudbreak.cloud.model.VmType;
import com.sequenceiq.cloudbreak.cloud.model.VmTypeMeta;
import com.sequenceiq.cloudbreak.cloud.model.VolumeParameterConfig;
import com.sequenceiq.cloudbreak.cloud.model.VolumeParameterType;

@Service
public class MockPlatformResources implements PlatformResources {

    private static final String MOCK_RESOURCE_DEFINITION = "{}";

    private static final String[] EUROPE_AVAILABILITY_ZONES = {"europe-a", "europe-b"};

    private static final String[] USA_AVAILABILITY_ZONES = {"usa-a", "usa-b", "usa-c"};

    @Value("${cb.platform.default.regions:}")
    private String defaultRegions;

    private String defaultRegion;

    private VmType defaultVmType;

    private enum MockedVmTypes {

        SMALL("small"),
        MEDIUM("medium"),
        LARGE("large");

        private final String value;

        MockedVmTypes(String value) {
            this.value = value;
        }

        public String value() {
            return value;
        }

        public VmTypeMeta getVmTypeMeta() {
            VmTypeMeta vmTypeMeta = new VmTypeMeta();
            vmTypeMeta.setSsdConfig(getVolumeConfig(VolumeParameterType.SSD));
            vmTypeMeta.setEphemeralConfig(getVolumeConfig(VolumeParameterType.EPHEMERAL));
            vmTypeMeta.setMagneticConfig(getVolumeConfig(VolumeParameterType.MAGNETIC));
            vmTypeMeta.setAutoAttachedConfig(getVolumeConfig(VolumeParameterType.AUTO_ATTACHED));
            return vmTypeMeta;
        }

        private VolumeParameterConfig getVolumeConfig(VolumeParameterType ssd) {
            return new VolumeParameterConfig(ssd, 1, Integer.MAX_VALUE, 0, Integer.MAX_VALUE);
        }
    }

    private Map<Region, List<AvailabilityZone>> regions = new HashMap<>();

    private Map<String, Set<VmType>> vmTypes = new HashMap<>();

    private Map<String, VmType> defaultVmTypes = new HashMap<>();

    private final Map<Region, String> regionDisplayNames = new HashMap<>();

    @PostConstruct
    public void init() {
        regions = readRegionsMock();
        vmTypes = readVmTypes();
        defaultRegion = getDefaultRegion().getRegionName();
        defaultVmType = vmTypes.get(vmTypes.keySet().iterator().next()).iterator().next();
        defaultVmTypes = readDefaultVmTypes();
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
        regions.put(Region.region("USA"), getAvailabilityZones(USA_AVAILABILITY_ZONES));
        regions.put(Region.region("Europe"), getAvailabilityZones(EUROPE_AVAILABILITY_ZONES));
        return regions;
    }

    private Map<String, Set<VmType>> readVmTypes() {
        Map<String, Set<VmType>> availabilityZoneHashMap = new HashMap<>();
        Set<VmType> vmTypeList = new HashSet<>();
        for (MockPlatformResources.MockedVmTypes vmType : MockPlatformResources.MockedVmTypes.values()) {
            vmTypeList.add(VmType.vmTypeWithMeta(vmType.value, vmType.getVmTypeMeta(), true));
        }

        for (Map.Entry<Region, List<AvailabilityZone>> regionListEntry : regions.entrySet()) {
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
    public CloudNetworks networks(CloudCredential cloudCredential, Region region, Map<String, String> filters) {
        return new CloudNetworks();
    }

    @Override
    public CloudSshKeys sshKeys(CloudCredential cloudCredential, Region region, Map<String, String> filters) {
        return new CloudSshKeys();
    }

    @Override
    public CloudSecurityGroups securityGroups(CloudCredential cloudCredential, Region region, Map<String, String> filters) {
        return new CloudSecurityGroups();
    }

    @Override
    @Cacheable(cacheNames = "cloudResourceRegionCache", key = "#cloudCredential?.id")
    public CloudRegions regions(CloudCredential cloudCredential, Region region, Map<String, String> filters) throws Exception {
        return new CloudRegions(regions, regionDisplayNames, defaultRegion);
    }

    @Override
    @Cacheable(cacheNames = "cloudResourceVmTypeCache", key = "#cloudCredential?.id + #region.getRegionName()")
    public CloudVmTypes virtualMachines(CloudCredential cloudCredential, Region region, Map<String, String> filters) {
        return new CloudVmTypes(vmTypes, defaultVmTypes);
    }

    @Override
    public CloudGateWays gateways(CloudCredential cloudCredential, Region region, Map<String, String> filters) throws Exception {
        return new CloudGateWays();
    }

    @Override
    public CloudIpPools publicIpPool(CloudCredential cloudCredential, Region region, Map<String, String> filters) throws Exception {
        return new CloudIpPools();
    }

    @Override
    public CloudAccessConfigs accessConfigs(CloudCredential cloudCredential, Region region, Map<String, String> filters) throws Exception {
        return new CloudAccessConfigs(new HashSet<>());
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
        Map<Platform, Region> regions = Maps.newHashMap();
        if (isNoneEmpty(defaultRegions)) {
            for (String entry : defaultRegions.split(",")) {
                String[] keyValue = entry.split(":");
                regions.put(platform(keyValue[0]), Region.region(keyValue[1]));
            }
            Region platformRegion = regions.get(platform(MockConstants.MOCK));
            if (platformRegion != null && !isEmpty(platformRegion.value())) {
                return getRegionByName(platformRegion.value());
            }
        }
        return getRegionByName(defaultRegion);
    }
}
