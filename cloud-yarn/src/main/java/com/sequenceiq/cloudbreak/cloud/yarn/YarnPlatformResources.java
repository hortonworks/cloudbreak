package com.sequenceiq.cloudbreak.cloud.yarn;

import static com.sequenceiq.cloudbreak.cloud.model.Coordinate.coordinate;
import static com.sequenceiq.cloudbreak.cloud.model.Region.region;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.PlatformResources;
import com.sequenceiq.cloudbreak.cloud.model.CloudAccessConfigs;
import com.sequenceiq.cloudbreak.cloud.model.CloudDatabaseVmTypes;
import com.sequenceiq.cloudbreak.cloud.model.CloudEncryptionKeys;
import com.sequenceiq.cloudbreak.cloud.model.CloudGateWays;
import com.sequenceiq.cloudbreak.cloud.model.CloudIpPools;
import com.sequenceiq.cloudbreak.cloud.model.CloudNetworks;
import com.sequenceiq.cloudbreak.cloud.model.CloudRegions;
import com.sequenceiq.cloudbreak.cloud.model.CloudSecurityGroups;
import com.sequenceiq.cloudbreak.cloud.model.CloudSshKeys;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmTypes;
import com.sequenceiq.cloudbreak.cloud.model.Coordinate;
import com.sequenceiq.cloudbreak.cloud.model.ExtendedCloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.Region;
import com.sequenceiq.cloudbreak.cloud.model.RegionCoordinateSpecification;
import com.sequenceiq.cloudbreak.cloud.model.RegionCoordinateSpecifications;
import com.sequenceiq.cloudbreak.cloud.model.dns.CloudPrivateDnsZones;
import com.sequenceiq.cloudbreak.cloud.model.nosql.CloudNoSqlTables;
import com.sequenceiq.cloudbreak.cloud.model.resourcegroup.CloudResourceGroups;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.service.CloudbreakResourceReaderService;

@Service
public class YarnPlatformResources implements PlatformResources {
    private static final Logger LOGGER = LoggerFactory.getLogger(YarnPlatformResources.class);

    @Inject
    private CloudbreakResourceReaderService cloudbreakResourceReaderService;

    private Map<Region, Coordinate> regionCoordinates = new HashMap<>();

    @PostConstruct
    public void init() {
        regionCoordinates = readRegionCoordinates(resourceDefinition("zone-coordinates"));
    }

    public String resourceDefinition(String resource) {
        return cloudbreakResourceReaderService.resourceDefinition("yarn", resource);
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
    public CloudNetworks networks(ExtendedCloudCredential cloudCredential, Region region, Map<String, String> filters) {
        return new CloudNetworks();
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
    public CloudRegions regions(ExtendedCloudCredential cloudCredential, Region region, Map<String, String> filters,
        boolean availabilityZonesNeeded) {
        return new CloudRegions(Collections.emptyMap(), Collections.emptyMap(), regionCoordinates, "", Boolean.FALSE);
    }

    @Override
    public CloudVmTypes virtualMachines(ExtendedCloudCredential cloudCredential, Region region, Map<String, String> filters) {
        return new CloudVmTypes();
    }

    @Override
    public CloudDatabaseVmTypes databaseVirtualMachines(ExtendedCloudCredential cloudCredential, Region region, Map<String, String> filters) {
        return new CloudDatabaseVmTypes();
    }

    @Override
    public Optional<String> getVirtualMachineUrl(ExtendedCloudCredential cloudCredential, Region region, String instanceId, Map<String, String> filters) {
        return Optional.empty();
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
        LOGGER.warn("NoSQL table list is not supported on 'YARN'");
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

}
