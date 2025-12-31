package com.sequenceiq.cloudbreak.cloud.azure;

import static com.sequenceiq.cloudbreak.cloud.azure.DistroxEnabledInstanceTypes.AZURE_ENABLED_TYPES_LIST;
import static com.sequenceiq.cloudbreak.cloud.model.Coordinate.coordinate;
import static com.sequenceiq.cloudbreak.cloud.model.VolumeParameterType.EPHEMERAL;
import static com.sequenceiq.cloudbreak.cloud.model.VolumeParameterType.MAGNETIC;
import static com.sequenceiq.cloudbreak.cloud.model.VolumeParameterType.SSD;
import static com.sequenceiq.cloudbreak.constant.AzureConstants.NETWORK_ID;
import static com.sequenceiq.cloudbreak.constant.AzureConstants.RESOURCE_GROUP_NAME;
import static org.apache.commons.collections4.CollectionUtils.emptyIfNull;

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import com.azure.resourcemanager.compute.models.VirtualMachineSize;
import com.azure.resourcemanager.msi.models.Identity;
import com.azure.resourcemanager.network.models.Network;
import com.azure.resourcemanager.network.models.NetworkSecurityGroup;
import com.azure.resourcemanager.network.models.Subnet;
import com.azure.resourcemanager.privatedns.models.PrivateDnsZone;
import com.google.common.base.Splitter;
import com.sequenceiq.cloudbreak.cloud.PlatformResources;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClientService;
import com.sequenceiq.cloudbreak.cloud.azure.resource.AzureRegionProvider;
import com.sequenceiq.cloudbreak.cloud.azure.resource.domain.AzureCoordinate;
import com.sequenceiq.cloudbreak.cloud.azure.validator.AzureAcceleratedNetworkValidator;
import com.sequenceiq.cloudbreak.cloud.azure.validator.AzureHostEncryptionValidator;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone;
import com.sequenceiq.cloudbreak.cloud.model.CloudAccessConfig;
import com.sequenceiq.cloudbreak.cloud.model.CloudAccessConfigs;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudDatabaseVmTypes;
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
import com.sequenceiq.cloudbreak.cloud.model.DefaultPlatformDatabaseCapabilities;
import com.sequenceiq.cloudbreak.cloud.model.ExtendedCloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStoreMetadata;
import com.sequenceiq.cloudbreak.cloud.model.PlatformDBStorageCapabilities;
import com.sequenceiq.cloudbreak.cloud.model.PlatformDatabaseCapabilities;
import com.sequenceiq.cloudbreak.cloud.model.Region;
import com.sequenceiq.cloudbreak.cloud.model.VmType;
import com.sequenceiq.cloudbreak.cloud.model.VmTypeMeta.VmTypeMetaBuilder;
import com.sequenceiq.cloudbreak.cloud.model.VolumeParameterConfig;
import com.sequenceiq.cloudbreak.cloud.model.VolumeParameterType;
import com.sequenceiq.cloudbreak.cloud.model.dns.CloudPrivateDnsZone;
import com.sequenceiq.cloudbreak.cloud.model.dns.CloudPrivateDnsZones;
import com.sequenceiq.cloudbreak.cloud.model.nosql.CloudNoSqlTables;
import com.sequenceiq.cloudbreak.cloud.model.resourcegroup.CloudResourceGroup;
import com.sequenceiq.cloudbreak.cloud.model.resourcegroup.CloudResourceGroups;
import com.sequenceiq.cloudbreak.cloud.model.view.PlatformResourceSecurityGroupFilterView;
import com.sequenceiq.cloudbreak.common.domain.CdpSupportedServices;
import com.sequenceiq.cloudbreak.common.network.NetworkConstants;
import com.sequenceiq.cloudbreak.filter.MinimalHardwareFilter;
import com.sequenceiq.cloudbreak.util.PermanentlyFailedException;

@Service
public class AzurePlatformResources implements PlatformResources {

    private static final Logger LOGGER = LoggerFactory.getLogger(AzurePlatformResources.class);

    private static final float NO_MB_PER_GB = 1024.0f;

    private static final int NO_RESOURCE_DISK_ATTACHED_TO_INSTANCE = 0;

    private final Predicate<VmType> enabledDistroxInstanceTypeFilter = vmt -> AZURE_ENABLED_TYPES_LIST.stream()
            .filter(it -> !it.isEmpty())
            .anyMatch(di -> vmt.value().equals(di));

    @Value("${cb.azure.default.max.disk.size:32767}")
    private int maxDiskSize;

    @Value("${cb.azure.default.vmtype:Standard_D16s_v3}")
    private String armVmDefault;

    @Value("${distrox.restrict.instance.types:true}")
    private boolean restrictInstanceTypes;

    @Inject
    private AzureClientService azureClientService;

    @Inject
    private AzureRegionProvider azureRegionProvider;

    @Inject
    private MinimalHardwareFilter minimalHardwareFilter;

    @Inject
    private AzureCloudSubnetParametersService azureCloudSubnetParametersService;

    @Inject
    private AzureAddressPrefixProvider azureAddressPrefixProvider;

    @Inject
    private AzureDatabaseCapabilityService azureDatabaseCapabilityService;

    @Inject
    private AzureHostEncryptionValidator azureHostEncryptionValidator;

    @Inject
    private AzureAcceleratedNetworkValidator azureAcceleratedNetworkValidator;

    @Override
    public CloudNetworks networks(ExtendedCloudCredential cloudCredential, Region region, Map<String, String> filters) {
        AzureClient client = azureClientService.getClient(cloudCredential);
        Map<String, Set<CloudNetwork>> result = new HashMap<>();
        String networkId = filters.get(NETWORK_ID);
        String resourceGroupName = filters.get(RESOURCE_GROUP_NAME);
        if (!StringUtils.isEmpty(networkId) && !StringUtils.isEmpty(resourceGroupName)) {
            LOGGER.info("Query network with id '{}' and resource group {}", networkId, resourceGroupName);
            Network network = client.getNetworkByResourceGroup(resourceGroupName, networkId);
            if (network != null) {
                LOGGER.info("Network was successfully queried for network with id '{}' and resource group {}: {}",
                        networkId, resourceGroupName, network);
                addToResultIfRegionsAreMatch(region, result, network);
            } else {
                LOGGER.info("Network with id '{}' does not exist in resource group {}", networkId, resourceGroupName);
            }
        } else {
            for (Network network : client.getNetworks().getAll()) {
                addToResultIfRegionsAreMatch(region, result, network);
            }
        }
        if (result.isEmpty() && Objects.nonNull(region)) {
            result.put(region.value(), new HashSet<>());
        }
        return new CloudNetworks(result);
    }

    private void addToResultIfRegionsAreMatch(Region region, Map<String, Set<CloudNetwork>> result, Network network) {
        if (network.region() != null) {
            String actualRegionLabel = network.region().label();
            String actualRegionName = network.region().name();

            LOGGER.info("The region label '{}' and the region name '{}' are for Azure network id: {} name: {}",
                    actualRegionLabel, actualRegionName, network.id(), network.name());
            if (regionMatch(actualRegionLabel, region) || regionMatch(actualRegionName, region)) {
                CloudNetwork cloudNetwork = convertToCloudNetwork(network);
                result.computeIfAbsent(actualRegionLabel, s -> new HashSet<>()).add(cloudNetwork);
                result.computeIfAbsent(actualRegionName, s -> new HashSet<>()).add(cloudNetwork);
            }
        } else {
            LOGGER.info("Network with id {} and name {} has no region which is not supported by CDP: {}",
                    network.id(), network.name(), network);
            throw new BadRequestException(String.format("Network with id %s and name %s has no region which is not supported by CDP.",
                    network.id(), network.name()));
        }
    }

    private CloudNetwork convertToCloudNetwork(Network network) {
        Set<CloudSubnet> subnets = new HashSet<>();
        for (Entry<String, Subnet> subnet : network.subnets().entrySet()) {
            String addressPrefix = azureAddressPrefixProvider.getAddressPrefix(subnet.getValue());
            CloudSubnet cloudSubnet = new CloudSubnet.Builder()
                    .id(subnet.getKey())
                    .name(subnet.getKey())
                    .cidr(addressPrefix)
                    .build();
            azureCloudSubnetParametersService.addPrivateEndpointNetworkPolicies(cloudSubnet, subnet.getValue().innerModel().privateEndpointNetworkPolicies());
            azureCloudSubnetParametersService.addFlexibleServerDelegatedSubnet(cloudSubnet, subnet.getValue().innerModel().delegations());
            subnets.add(cloudSubnet);
            LOGGER.debug("Mapping azure subnet '{}' to cloudSubnet: {}", subnet.getKey(), cloudSubnet);
        }
        Map<String, Object> properties = new HashMap<>();
        properties.put("addressSpaces", network.addressSpaces());
        properties.put("dnsServerIPs", network.dnsServerIPs());
        properties.put("resourceGroupName", network.resourceGroupName());

        return new CloudNetwork(network.name(), network.id(), subnets, properties);
    }

    @Override
    public CloudSshKeys sshKeys(ExtendedCloudCredential cloudCredential, Region region, Map<String, String> filters) {
        return new CloudSshKeys();
    }

    @Override
    public CloudSecurityGroups securityGroups(ExtendedCloudCredential cloudCredential, Region region, Map<String, String> filters) {
        AzureClient client = azureClientService.getClient(cloudCredential);
        Map<String, Set<CloudSecurityGroup>> result = new HashMap<>();
        PlatformResourceSecurityGroupFilterView filter = new PlatformResourceSecurityGroupFilterView(filters);
        String groupId = filter.getGroupId();
        if (groupId != null) {
            NetworkSecurityGroup networkSecurityGroup = getNetworkSecurityGroup(client, groupId);
            convertAndAddToResult(region, result, networkSecurityGroup);
        } else {
            for (NetworkSecurityGroup securityGroup : client.getSecurityGroupsList().getAll()) {
                convertAndAddToResult(region, result, securityGroup);
            }
        }
        if (result.isEmpty() && Objects.nonNull(region)) {
            result.put(region.value(), new HashSet<>());
        }
        return new CloudSecurityGroups(result);
    }

    private NetworkSecurityGroup getNetworkSecurityGroup(AzureClient client, String groupId) {
        try {
            NetworkSecurityGroup networkSecurityGroup = client.getSecurityGroups().getById(groupId);
            if (networkSecurityGroup == null) {
                throw new PermanentlyFailedException("Nothing found on Azure with id: " + groupId);
            }
            return networkSecurityGroup;
        } catch (InvalidParameterException e) {
            throw new PermanentlyFailedException(e.getMessage(), e);
        }
    }

    private void convertAndAddToResult(Region region, Map<String, Set<CloudSecurityGroup>> result, NetworkSecurityGroup securityGroup) {
        String actualRegionLabel = securityGroup.region().label();
        String actualRegionName = securityGroup.region().name();
        if (regionMatch(actualRegionLabel, region) || regionMatch(actualRegionName, region)) {
            Map<String, Object> properties = new HashMap<>();
            properties.put("resourceGroupName", securityGroup.resourceGroupName());
            properties.put("networkInterfaceIds", securityGroup.networkInterfaceIds());
            CloudSecurityGroup cloudSecurityGroup = new CloudSecurityGroup(securityGroup.name(), securityGroup.id(), properties);
            result.computeIfAbsent(actualRegionLabel, s -> new HashSet<>()).add(cloudSecurityGroup);
            result.computeIfAbsent(actualRegionName, s -> new HashSet<>()).add(cloudSecurityGroup);
        }
    }

    @Override
    @Cacheable(cacheNames = "cloudResourceRegionCache", key = "#cloudCredential?.id")
    public CloudRegions regions(ExtendedCloudCredential cloudCredential, Region region, Map<String, String> filters,
            boolean availabilityZonesNeeded) {
        AzureClient client = azureClientService.getClient(cloudCredential);
        Collection<com.azure.core.management.Region> azureRegions = client.getRegion(region);
        return azureRegionProvider.regions(region, azureRegions, cloudCredential.getEntitlements());
    }

    @Override
    @Cacheable(cacheNames = "cdpCloudResourceRegionCache", key = "'AZURE'")
    public CloudRegions cdpEnabledRegions() {
        Map<Region, List<AvailabilityZone>> regionListMap = new HashMap<>();
        Map<Region, String> displayNames = new HashMap<>();
        Map<Region, Coordinate> coordinates = new HashMap<>();

        for (Entry<Region, AzureCoordinate> enabledRegion : azureRegionProvider.enabledRegions().entrySet()) {
            regionListMap.put(enabledRegion.getKey(), List.of());
            Coordinate regionCoordinateSpecification = enabledRegion.getValue();
            displayNames.put(enabledRegion.getKey(), enabledRegion.getValue().getDisplayName());

            Set<CdpSupportedServices> cdpServices = regionCoordinateSpecification.getCdpSupportedServices()
                    .stream()
                    .map(e -> e.services())
                    .flatMap(list -> list.stream())
                    .collect(Collectors.toSet());
            Coordinate coordinate =  coordinate(
                    regionCoordinateSpecification.getLongitude().toString(),
                    regionCoordinateSpecification.getLatitude().toString(),
                    regionCoordinateSpecification.getDisplayName(),
                    regionCoordinateSpecification.getKey(),
                    regionCoordinateSpecification.isK8sSupported(),
                    regionCoordinateSpecification.getEntitlements(),
                    regionCoordinateSpecification.getDefaultDbVmType(),
                    regionCoordinateSpecification.getDefaultArmDbVmType(),
                    cdpServices);
            coordinates.put(enabledRegion.getKey(), coordinate);
        }
        return new CloudRegions(
                regionListMap,
                displayNames,
                coordinates,
                azureRegionProvider.getArmZoneParameterDefault(),
                true);
    }

    @Override
    @Cacheable(cacheNames = "cloudResourceVmTypeCache", key = "#cloudCredential?.id + #region.getRegionName() + #filters")
    public CloudVmTypes virtualMachines(ExtendedCloudCredential cloudCredential, Region region, Map<String, String> filters) {
        return virtualMachinesNonExtended(cloudCredential, region, filters);
    }

    @Override
    public CloudDatabaseVmTypes databaseVirtualMachines(ExtendedCloudCredential cloudCredential, Region region, Map<String, String> filters) {
        return azureDatabaseCapabilityService.databaseVmTypes(cloudCredential, region);
    }

    @Cacheable(cacheNames = "cloudResourceVmTypeCache", key = "#cloudCredential?.id + #region.getRegionName() + #filters")
    public CloudVmTypes virtualMachinesNonExtended(CloudCredential cloudCredential, Region region, Map<String, String> filters) {
        AzureClient client = azureClientService.getClient(cloudCredential);
        Set<VirtualMachineSize> vmTypes = client.getVmTypes(region.value());
        vmTypes = vmTypes.stream()
                .filter(filterOutV6Instances())
                .collect(Collectors.toSet());

        Map<String, List<String>> availabilityZones = client.getAvailabilityZones(region.value());
        Map<String, AzureVmCapabilities> azureVmCapabilities = client.getHostCapabilities(region.value());

        Map<String, Set<VmType>> cloudVmResponses = new HashMap<>();
        Map<String, VmType> defaultCloudVmResponses = new HashMap<>();

        Set<VmType> types = new HashSet<>();
        VmType defaultVmType = null;
        Set<String> vmTypesWithoutResourceDisks = new HashSet<>();
        LOGGER.debug("Requested Filters are {}", filters);
        for (VirtualMachineSize virtualMachineSize : vmTypes) {
            List<String> availabilityZonesForVm = availabilityZones.getOrDefault(virtualMachineSize.name(), new ArrayList<>());
            LOGGER.trace("Availability Zones for VM type {} are {}", virtualMachineSize.name(), availabilityZonesForVm);
            if (matchAvailabilityZones(filters, availabilityZonesForVm)) {
                float memoryInGB = virtualMachineSize.memoryInMB() / NO_MB_PER_GB;
                VmTypeMetaBuilder builder = VmTypeMetaBuilder.builder()
                        .withCpuAndMemory(virtualMachineSize.numberOfCores(), memoryInGB);
                for (VolumeParameterType volumeParameterType : VolumeParameterType.values()) {
                    if (volumeParameterType.in(MAGNETIC, SSD)) {
                        volumeParameterType.buildForVmTypeMetaBuilder(builder, virtualMachineSize.maxDataDiskCount(), maxDiskSize);
                    }
                }
                if (virtualMachineSize.resourceDiskSizeInMB() != NO_RESOURCE_DISK_ATTACHED_TO_INSTANCE) {
                    builder.withResourceDiskAttached(true);
                } else {
                    builder.withResourceDiskAttached(false);
                }
                builder.withHostEncryptionSupport(azureHostEncryptionValidator.isVmSupported(virtualMachineSize.name(), azureVmCapabilities));
                builder.withEnhancedNetwork(azureAcceleratedNetworkValidator.isSupportedForVm(virtualMachineSize.name(), azureVmCapabilities));
                builder.withAvailabilityZones(availabilityZonesForVm);
                VmType vmType = VmType.vmTypeWithMeta(virtualMachineSize.name(), builder.create(), true);
                types.add(vmType);
                if (virtualMachineSize.name().equals(armVmDefault)) {
                    defaultVmType = vmType;
                }
            } else {
                LOGGER.debug("{} with Availability Zones {} is not supported in requested Availability Zones. Do not add it to the result",
                        virtualMachineSize.name(), availabilityZonesForVm);
            }
        }
        LOGGER.debug("The following Azure VM types have been filtered out, because there is no resource disk available with them: {}",
                String.join(", ", vmTypesWithoutResourceDisks));
        cloudVmResponses.put(region.value(), types);
        defaultCloudVmResponses.put(region.value(), defaultVmType);
        return new CloudVmTypes(cloudVmResponses, defaultCloudVmResponses);
    }

    @Override
    @Cacheable(cacheNames = "cloudResourceVmTypeCache", key = "#cloudCredential?.id + #region.getRegionName() + 'distrox'")
    public CloudVmTypes virtualMachinesForDistroX(ExtendedCloudCredential cloudCredential, Region region, Map<String, String> filters) {
        CloudVmTypes cloudVmTypes = virtualMachines(cloudCredential, region, filters);
        Map<String, Set<VmType>> returnVmResponses = new HashMap<>();
        Map<String, Set<VmType>> cloudVmResponses = cloudVmTypes.getCloudVmResponses();
        if (restrictInstanceTypes) {
            for (Entry<String, Set<VmType>> stringSetEntry : cloudVmResponses.entrySet()) {
                returnVmResponses.put(stringSetEntry.getKey(), stringSetEntry.getValue().stream()
                        .filter(e -> minimalHardwareFilter
                                .suitableAsMinimumHardware(e.getMetaData().getCPU(), e.getMetaData().getMemoryInGb()))
                        .filter(enabledDistroxInstanceTypeFilter)
                        .collect(Collectors.toSet()));
            }
        } else {
            for (Entry<String, Set<VmType>> stringSetEntry : cloudVmResponses.entrySet()) {
                returnVmResponses.put(stringSetEntry.getKey(), stringSetEntry.getValue().stream()
                        .filter(e -> minimalHardwareFilter
                                .suitableAsMinimumHardware(e.getMetaData().getCPU(), e.getMetaData().getMemoryInGb()))
                        .collect(Collectors.toSet()));
            }
        }
        return new CloudVmTypes(returnVmResponses, cloudVmTypes.getDefaultCloudVmResponses());
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
    @Cacheable(cacheNames = "databaseCapabilities", key =
            "#cloudCredential?.id + #region.getRegionName() + #filters[T(com.sequenceiq.cloudbreak.cloud.CloudParameterConst).DATABASE_TYPE] + 'databases'")
    public PlatformDatabaseCapabilities databaseCapabilities(CloudCredential cloudCredential, Region region, Map<String, String> filters) {
        return azureDatabaseCapabilityService.databaseCapabilities(cloudCredential, region, filters);
    }

    @Override
    public DefaultPlatformDatabaseCapabilities defaultDatabaseCapabilities() {
        return azureDatabaseCapabilityService.defaultPlatformDatabaseCapabilities();
    }

    @Override
    @Cacheable(cacheNames = "dbStorageCapabilities", key = "#cloudCredential?.id + #region.getRegionName() + 'databases'")
    public Optional<PlatformDBStorageCapabilities> databaseStorageCapabilities(CloudCredential cloudCredential, Region region) {
        return azureDatabaseCapabilityService.databaseStorageCapabilities(cloudCredential, region);
    }

    @Override
    public CloudAccessConfigs accessConfigs(ExtendedCloudCredential cloudCredential, Region region, Map<String, String> filters) {
        CloudAccessConfigs cloudAccessConfigs = new CloudAccessConfigs(new HashSet<>());
        AzureClient client = azureClientService.getClient(cloudCredential);
        List<Identity> identities;
        if (!"null".equals(region.getRegionName())) {
            identities = client.listIdentitiesByRegion(region.getRegionName());
        } else {
            identities = client.listIdentities().getAll();
        }
        Set<CloudAccessConfig> configs = identities.stream().map(identity -> {
            Map<String, Object> properties = new HashMap<>();
            properties.put("resourceId", identity.id());
            properties.put("resourceGroupName", identity.resourceGroupName());
            return new CloudAccessConfig(
                    identity.name(),
                    identity.principalId(),
                    properties);
        }).collect(Collectors.toSet());

        cloudAccessConfigs.getCloudAccessConfigs().addAll(configs);
        return cloudAccessConfigs;
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
        LOGGER.warn("NoSQL table list is not supported on 'AZURE'");
        return new CloudNoSqlTables(new ArrayList<>());
    }

    @Override
    public CloudResourceGroups resourceGroups(ExtendedCloudCredential cloudCredential, Region region, Map<String, String> filters) {
        List<CloudResourceGroup> resourceGroups = azureClientService.getClient(cloudCredential).getResourceGroups()
                .getStream()
                .map(rg -> new CloudResourceGroup(rg.name()))
                .collect(Collectors.toList());
        return new CloudResourceGroups(resourceGroups);
    }

    @Override
    public CloudPrivateDnsZones privateDnsZones(ExtendedCloudCredential cloudCredential, Map<String, String> filters) {
        List<PrivateDnsZone> azurePrivateDnsZones = azureClientService.getClient(cloudCredential).getPrivateDnsZoneListFromAllSubscriptions();
        List<CloudPrivateDnsZone> cloudPrivateDnsZoneList = azurePrivateDnsZones.stream()
                .map(pdz -> new CloudPrivateDnsZone(pdz.id())).collect(Collectors.toList());
        LOGGER.debug("Found private DNS zones are: {}", cloudPrivateDnsZoneList);
        return new CloudPrivateDnsZones(cloudPrivateDnsZoneList);
    }

    public InstanceStoreMetadata collectInstanceStorageCount(AuthenticatedContext ac, List<String> instanceTypes) {
        try {
            AzureClient client = azureClientService.getClient(ac.getCloudCredential());
            Set<VirtualMachineSize> vmTypes = client.getVmTypes(ac.getCloudContext().getLocation().getRegion().value());

            Map<String, VolumeParameterConfig> instanceTypeToInstanceStorageMap = vmTypes.stream()
                    .filter(vmType -> instanceTypes.contains(vmType.name()))
                    .filter(vmType -> vmType.resourceDiskSizeInMB() != NO_RESOURCE_DISK_ATTACHED_TO_INSTANCE)
                    .collect(Collectors.toMap(VirtualMachineSize::name, vmType -> new VolumeParameterConfig(EPHEMERAL,
                            (int) (vmType.resourceDiskSizeInMB() / NO_MB_PER_GB),
                            (int) (vmType.resourceDiskSizeInMB() / NO_MB_PER_GB),
                            1,
                            1)));
            return new InstanceStoreMetadata(instanceTypeToInstanceStorageMap);
        } catch (Exception e) {
            LOGGER.warn("Failed to get vm type data: {}", instanceTypes, e);
            throw new CloudConnectorException(e.getMessage(), e);
        }
    }

    private boolean matchAvailabilityZones(Map<String, String> filters, List<String> availabilityZones) {
        return filters == null || StringUtils.isEmpty(filters.get(NetworkConstants.AVAILABILITY_ZONES)) ||
                CollectionUtils.containsAll(emptyIfNull(availabilityZones),
                        Splitter.on(",").splitToList(filters.get(NetworkConstants.AVAILABILITY_ZONES))
                );
    }

    private Predicate<VirtualMachineSize> filterOutV6Instances() {
        return e -> !e.name().toLowerCase(Locale.ROOT).endsWith("_v6");
    }
}
