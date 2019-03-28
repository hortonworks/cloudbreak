package com.sequenceiq.cloudbreak.cloud.azure;

import static com.sequenceiq.cloudbreak.cloud.model.Coordinate.coordinate;
import static com.sequenceiq.cloudbreak.cloud.model.Region.region;
import static com.sequenceiq.cloudbreak.cloud.model.VolumeParameterType.MAGNETIC;
import static com.sequenceiq.cloudbreak.cloud.model.VolumeParameterType.SSD;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import com.google.common.base.Strings;
import com.microsoft.azure.management.compute.VirtualMachineSize;
import com.microsoft.azure.management.network.Network;
import com.microsoft.azure.management.network.NetworkSecurityGroup;
import com.microsoft.azure.management.network.Subnet;
import com.sequenceiq.cloudbreak.cloud.PlatformResources;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClientService;
import com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone;
import com.sequenceiq.cloudbreak.cloud.model.CloudAccessConfigs;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
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
import com.sequenceiq.cloudbreak.cloud.model.VmTypeMeta.VmTypeMetaBuilder;
import com.sequenceiq.cloudbreak.cloud.model.VolumeParameterType;
import com.sequenceiq.cloudbreak.service.CloudbreakResourceReaderService;
import com.sequenceiq.cloudbreak.util.JsonUtil;

@Service
public class AzurePlatformResources implements PlatformResources {
    public static final int DEFAULT_MINIMUM_VOLUME_SIZE = 10;

    public static final int DEFAULT_MAXIMUM_VOLUME_SIZE = 1023;

    public static final int DEFAULT_MINIMUM_VOLUME_COUNT = 1;

    private static final Logger LOGGER = LoggerFactory.getLogger(AzurePlatformResources.class);

    private static final float NO_MB_PER_GB = 1024.0f;

    @Value("${cb.azure.default.vmtype:Standard_D12_v2}")
    private String armVmDefault;

    @Value("${cb.arm.zone.parameter.default:North Europe}")
    private String armZoneParameterDefault;

    @Inject
    private AzureClientService azureClientService;

    @Inject
    private CloudbreakResourceReaderService cloudbreakResourceReaderService;

    private Map<Region, Coordinate> regionCoordinates = new HashMap<>();

    @PostConstruct
    public void init() {
        regionCoordinates = readRegionCoordinates(resourceDefinition("zone-coordinates"));
    }

    public String resourceDefinition(String resource) {
        return cloudbreakResourceReaderService.resourceDefinition("azure", resource);
    }

    private Map<Region, Coordinate> readRegionCoordinates(String displayNames) {
        Map<Region, Coordinate> regionCoordinates = new HashMap<>();
        try {
            RegionCoordinateSpecifications regionCoordinateSpecifications = JsonUtil.readValue(displayNames, RegionCoordinateSpecifications.class);
            for (RegionCoordinateSpecification regionCoordinateSpecification : regionCoordinateSpecifications.getItems()) {
                regionCoordinates.put(region(regionCoordinateSpecification.getName()),
                        coordinate(regionCoordinateSpecification.getLongitude(),
                                regionCoordinateSpecification.getLatitude(),
                                regionCoordinateSpecification.getName()));
            }
        } catch (IOException ignored) {
            return regionCoordinates;
        }
        return regionCoordinates;
    }

    @Override
    public CloudNetworks networks(CloudCredential cloudCredential, Region region, Map<String, String> filters) {
        AzureClient client = azureClientService.getClient(cloudCredential);
        Map<String, Set<CloudNetwork>> result = new HashMap<>();

        for (Network network : client.getNetworks()) {
            String actualRegion = network.region().label();
            if (regionMatch(actualRegion, region)) {
                Map<String, String> subnets = new HashMap<>();
                for (Entry<String, Subnet> subnet : network.subnets().entrySet()) {
                    subnets.put(subnet.getKey(), subnet.getKey());
                }
                Map<String, Object> properties = new HashMap<>();
                properties.put("addressSpaces", network.addressSpaces());
                properties.put("dnsServerIPs", network.dnsServerIPs());
                properties.put("resourceGroupName", network.resourceGroupName());

                CloudNetwork cloudNetwork = new CloudNetwork(network.name(), network.id(), subnets, properties);
                result.computeIfAbsent(actualRegion, s -> new HashSet<>()).add(cloudNetwork);
            }
        }
        if (result.isEmpty() && Objects.nonNull(region)) {
            result.put(region.value(), new HashSet<>());
        }
        return new CloudNetworks(result);
    }

    @Override
    public CloudSshKeys sshKeys(CloudCredential cloudCredential, Region region, Map<String, String> filters) {
        return new CloudSshKeys();
    }

    @Override
    public CloudSecurityGroups securityGroups(CloudCredential cloudCredential, Region region, Map<String, String> filters) {
        AzureClient client = azureClientService.getClient(cloudCredential);
        Map<String, Set<CloudSecurityGroup>> result = new HashMap<>();

        for (NetworkSecurityGroup securityGroup : client.getSecurityGroups().list()) {
            String actualRegion = securityGroup.region().label();
            if (regionMatch(actualRegion, region)) {
                Map<String, Object> properties = new HashMap<>();
                properties.put("resourceGroupName", securityGroup.resourceGroupName());
                properties.put("networkInterfaceIds", securityGroup.networkInterfaceIds());
                CloudSecurityGroup cloudSecurityGroup = new CloudSecurityGroup(securityGroup.name(), securityGroup.id(), properties);
                result.computeIfAbsent(actualRegion, s -> new HashSet<>()).add(cloudSecurityGroup);
            }
        }
        if (result.isEmpty() && Objects.nonNull(region)) {
            result.put(region.value(), new HashSet<>());
        }
        return new CloudSecurityGroups(result);
    }

    @Override
    @Cacheable(cacheNames = "cloudResourceRegionCache", key = "#cloudCredential?.id")
    public CloudRegions regions(CloudCredential cloudCredential, Region region, Map<String, String> filters) {
        AzureClient client = azureClientService.getClient(cloudCredential);
        Map<Region, List<AvailabilityZone>> cloudRegions = new HashMap<>();
        Map<Region, String> displayNames = new HashMap<>();
        Map<Region, Coordinate> coordinates = new HashMap<>();

        String defaultRegion = armZoneParameterDefault;
        for (com.microsoft.azure.management.resources.fluentcore.arm.Region azureRegion : client.getRegion(region)) {
            cloudRegions.put(region(azureRegion.label()), new ArrayList<>());
            displayNames.put(region(azureRegion.label()), azureRegion.label());

            Coordinate coordinate = regionCoordinates.get(region(azureRegion.label()));
            if (coordinate == null || coordinate.getLongitude() == null || coordinate.getLatitude() == null) {
                LOGGER.warn("Unregistered region with location coordinates on azure side: {} using default California", azureRegion.label());
                coordinates.put(region(azureRegion.label()), Coordinate.defaultCoordinate());
            } else {
                coordinates.put(region(azureRegion.label()), coordinate);
            }
        }
        if (region != null && !Strings.isNullOrEmpty(region.value())) {
            defaultRegion = region.value();
        }
        return new CloudRegions(cloudRegions, displayNames, coordinates, defaultRegion, true);
    }

    @Override
    @Cacheable(cacheNames = "cloudResourceVmTypeCache", key = "#cloudCredential?.id + #region.getRegionName()")
    public CloudVmTypes virtualMachines(CloudCredential cloudCredential, Region region, Map<String, String> filters) {
        AzureClient client = azureClientService.getClient(cloudCredential);
        Set<VirtualMachineSize> vmTypes = client.getVmTypes(region.value());

        Map<String, Set<VmType>> cloudVmResponses = new HashMap<>();
        Map<String, VmType> defaultCloudVmResponses = new HashMap<>();

        Set<VmType> types = new HashSet<>();
        VmType defaultVmType = null;

        for (VirtualMachineSize virtualMachineSize : vmTypes) {
            float memoryInGB = virtualMachineSize.memoryInMB() / NO_MB_PER_GB;
            VmTypeMetaBuilder builder = VmTypeMetaBuilder.builder().withCpuAndMemory(virtualMachineSize.numberOfCores(), memoryInGB);

            for (VolumeParameterType volumeParameterType : VolumeParameterType.values()) {
                if (volumeParameterType.in(MAGNETIC, SSD)) {
                    volumeParameterType.buildForVmTypeMetaBuilder(builder, virtualMachineSize.maxDataDiskCount());
                }
            }

            VmType vmType = VmType.vmTypeWithMeta(virtualMachineSize.name(), builder.create(), true);
            types.add(vmType);
            if (virtualMachineSize.name().equals(armVmDefault)) {
                defaultVmType = vmType;
            }
        }
        cloudVmResponses.put(region.value(), types);
        defaultCloudVmResponses.put(region.value(), defaultVmType);
        return new CloudVmTypes(cloudVmResponses, defaultCloudVmResponses);
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
        return new CloudEncryptionKeys(new HashSet<>());
    }

}
