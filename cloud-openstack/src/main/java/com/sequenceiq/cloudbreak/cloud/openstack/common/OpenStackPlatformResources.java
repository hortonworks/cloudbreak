package com.sequenceiq.cloudbreak.cloud.openstack.common;

import static com.sequenceiq.cloudbreak.cloud.model.Coordinate.coordinate;
import static com.sequenceiq.cloudbreak.cloud.model.Region.region;
import static com.sequenceiq.cloudbreak.cloud.model.VolumeParameterType.MAGNETIC;
import static com.sequenceiq.cloudbreak.cloud.model.VolumeParameterType.values;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.openstack4j.api.OSClient;
import org.openstack4j.model.compute.Flavor;
import org.openstack4j.model.compute.Keypair;
import org.openstack4j.model.compute.SecGroupExtension;
import org.openstack4j.model.network.Network;
import org.openstack4j.model.network.Router;
import org.openstack4j.model.network.Subnet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.PlatformResources;
import com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone;
import com.sequenceiq.cloudbreak.cloud.model.CloudAccessConfigs;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudEncryptionKeys;
import com.sequenceiq.cloudbreak.cloud.model.CloudGateWay;
import com.sequenceiq.cloudbreak.cloud.model.CloudGateWays;
import com.sequenceiq.cloudbreak.cloud.model.CloudIpPool;
import com.sequenceiq.cloudbreak.cloud.model.CloudIpPools;
import com.sequenceiq.cloudbreak.cloud.model.CloudNetwork;
import com.sequenceiq.cloudbreak.cloud.model.CloudNetworks;
import com.sequenceiq.cloudbreak.cloud.model.CloudRegions;
import com.sequenceiq.cloudbreak.cloud.model.CloudSecurityGroup;
import com.sequenceiq.cloudbreak.cloud.model.CloudSecurityGroups;
import com.sequenceiq.cloudbreak.cloud.model.CloudSshKey;
import com.sequenceiq.cloudbreak.cloud.model.CloudSshKeys;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmTypes;
import com.sequenceiq.cloudbreak.cloud.model.Coordinate;
import com.sequenceiq.cloudbreak.cloud.model.Region;
import com.sequenceiq.cloudbreak.cloud.model.RegionCoordinateSpecification;
import com.sequenceiq.cloudbreak.cloud.model.RegionCoordinateSpecifications;
import com.sequenceiq.cloudbreak.cloud.model.VmType;
import com.sequenceiq.cloudbreak.cloud.model.VmTypeMeta;
import com.sequenceiq.cloudbreak.cloud.model.VmTypeMeta.VmTypeMetaBuilder;
import com.sequenceiq.cloudbreak.cloud.model.VolumeParameterConfig;
import com.sequenceiq.cloudbreak.cloud.model.VolumeParameterType;
import com.sequenceiq.cloudbreak.cloud.model.generic.StringType;
import com.sequenceiq.cloudbreak.cloud.openstack.auth.OpenStackClient;
import com.sequenceiq.cloudbreak.cloud.openstack.view.KeystoneCredentialView;
import com.sequenceiq.cloudbreak.service.CloudbreakResourceReaderService;
import com.sequenceiq.cloudbreak.util.JsonUtil;

@Service
public class OpenStackPlatformResources implements PlatformResources {
    private static final Logger LOGGER = LoggerFactory.getLogger(OpenStackPlatformResources.class);

    @Value("${cb.openstack.default.minimum.volume.size:10}")
    private int defaultMinimumVolumeSize;

    @Value("${cb.openstack.default.maximum.volume.size:1023}")
    private int defaultMaximumVolumeSize;

    @Value("${cb.openstack.default.minimum.volume.count:0}")
    private int defaultMinimumVolumeCount;

    @Value("${cb.openstack.default.maximum.volume.count:100}")
    private int defaultMaximumVolumeCount;

    @Inject
    private OpenStackClient openStackClient;

    @Inject
    private CloudbreakResourceReaderService cloudbreakResourceReaderService;

    private Map<Region, Coordinate> regionCoordinates = new HashMap<>();

    @PostConstruct
    public void init() {
        regionCoordinates = readRegionCoordinates(resourceDefinition("zone-coordinates"));
    }

    public String resourceDefinition(String resource) {
        return cloudbreakResourceReaderService.resourceDefinition("openstack", resource);
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
        OSClient<?> osClient = openStackClient.createOSClient(cloudCredential);
        KeystoneCredentialView osCredential = openStackClient.createKeystoneCredential(cloudCredential);

        Set<CloudNetwork> cloudNetworks = new HashSet<>();
        List<? extends Network> networks = getNetworks(osClient);
        for (Network network : networks) {
            Map<String, Object> properties = new HashMap<>();
            properties.put("networkType", network.getNetworkType());
            properties.put("providerPhyNet", network.getProviderPhyNet());
            properties.put("providerSegID", network.getProviderSegID());
            properties.put("tenantId", network.getTenantId());

            Map<String, String> subnets = new HashMap<>();

            List<? extends Subnet> neutronSubnets = network.getNeutronSubnets();
            LOGGER.debug("Neutron subnets for {}: {}", network.getName(), neutronSubnets);
            if (neutronSubnets != null) {
                for (Subnet neutronSubnet : neutronSubnets) {
                    if (neutronSubnet != null) {
                        subnets.put(neutronSubnet.getId(), neutronSubnet.getName());
                    }
                }
            }

            CloudNetwork cloudNetwork = new CloudNetwork(network.getName(), network.getId(), subnets, properties);
            cloudNetworks.add(cloudNetwork);
        }

        Map<String, Set<CloudNetwork>> result = new HashMap<>(1);
        result.put(region.value() == null ? osCredential.getTenantName() : region.value(), cloudNetworks);
        LOGGER.debug("Openstack cloud networks result: {}", result);
        return new CloudNetworks(result);
    }

    @Override
    public CloudSshKeys sshKeys(CloudCredential cloudCredential, Region region, Map<String, String> filters) {
        OSClient<?> osClient = openStackClient.createOSClient(cloudCredential);
        KeystoneCredentialView osCredential = openStackClient.createKeystoneCredential(cloudCredential);

        Set<CloudSshKey> cloudSshKeys = new HashSet<>();
        for (Keypair keypair : osClient.compute().keypairs().list()) {
            Map<String, Object> properties = new HashMap<>();
            properties.put("fingerprint", keypair.getFingerprint());
            properties.put("id", keypair.getId());
            properties.put("publicKey", keypair.getPublicKey());
            properties.put("createdAt", keypair.getCreatedAt());

            CloudSshKey cloudSshKey = new CloudSshKey();
            cloudSshKey.setName(keypair.getName());
            cloudSshKey.setProperties(properties);
            cloudSshKeys.add(cloudSshKey);
        }

        Map<String, Set<CloudSshKey>> result = new HashMap<>();
        result.put(region.value() == null ? osCredential.getTenantName() : region.value(), cloudSshKeys);
        LOGGER.debug("Openstack cloud ssh keys result: {}", result);
        return new CloudSshKeys(result);
    }

    @Override
    public CloudSecurityGroups securityGroups(CloudCredential cloudCredential, Region region, Map<String, String> filters) {
        OSClient<?> osClient = openStackClient.createOSClient(cloudCredential);
        KeystoneCredentialView osCredential = openStackClient.createKeystoneCredential(cloudCredential);

        Set<CloudSecurityGroup> cloudSecurityGroups = new HashSet<>();
        List<? extends SecGroupExtension> osSecurityGroups = osClient.compute().securityGroups().list();
        LOGGER.debug("Security groups from openstack: {}", osSecurityGroups);
        for (SecGroupExtension secGroup : osSecurityGroups) {
            Map<String, Object> properties = new HashMap<>();
            properties.put("tenantId", secGroup.getTenantId());
            properties.put("rules", secGroup.getRules());

            CloudSecurityGroup cloudSecurityGroup = new CloudSecurityGroup(secGroup.getName(), secGroup.getId(), properties);
            cloudSecurityGroups.add(cloudSecurityGroup);
        }

        Map<String, Set<CloudSecurityGroup>> result = new HashMap<>();
        result.put(region.value() == null ? osCredential.getTenantName() : region.value(), cloudSecurityGroups);
        LOGGER.debug("Openstack security groups result: {}", result);
        return new CloudSecurityGroups(result);
    }

    @Override
    @Cacheable(cacheNames = "cloudResourceRegionCache", key = "#cloudCredential?.id")
    public CloudRegions regions(CloudCredential cloudCredential, Region region, Map<String, String> filters) {
        Set<String> regionsFromOpenStack = openStackClient.getRegion(cloudCredential);
        OSClient<?> osClient = openStackClient.createOSClient(cloudCredential);

        Map<Region, List<AvailabilityZone>> cloudRegions = new HashMap<>();
        Map<Region, String> displayNames = new HashMap<>();
        for (String regionFromOpenStack : regionsFromOpenStack) {
            List<AvailabilityZone> availabilityZones = openStackClient.getZones(osClient, regionFromOpenStack);
            cloudRegions.put(region(regionFromOpenStack), availabilityZones);
            displayNames.put(region(regionFromOpenStack), regionFromOpenStack);
        }
        String defaultRegion = null;
        if (!cloudRegions.keySet().isEmpty()) {
            defaultRegion = ((StringType) cloudRegions.keySet().toArray()[0]).value();
        }
        CloudRegions regions = new CloudRegions(cloudRegions, displayNames, regionCoordinates, defaultRegion, true);
        LOGGER.debug("Openstack regions result: {}", regions);
        return regions;
    }

    @Override
    @Cacheable(cacheNames = "cloudResourceVmTypeCache", key = "#cloudCredential?.id + #region.getRegionName()")
    public CloudVmTypes virtualMachines(CloudCredential cloudCredential, Region region, Map<String, String> filters) {
        OSClient<?> osClient = openStackClient.createOSClient(cloudCredential);
        Map<String, Set<VmType>> cloudVmResponses = new HashMap<>();
        Map<String, VmType> defaultCloudVmResponses = new HashMap<>();
        CloudRegions regions = regions(cloudCredential, region, filters);

        regions.getCloudRegions().forEach((cloudRegion, availabilityZones) -> {
            Set<VmType> types = collectVmTypes(osClient);
            convertVmSizeToGB(types);
            availabilityZones.forEach(availabilityZone -> cloudVmResponses.put(availabilityZone.value(), types));
            defaultCloudVmResponses.put(cloudRegion.value(), types.isEmpty() ? null : (VmType) types.toArray()[0]);
        });

        CloudVmTypes cloudVmTypes = new CloudVmTypes(cloudVmResponses, defaultCloudVmResponses);
        LOGGER.debug("Openstack virtual machine types: {}", cloudVmTypes);
        return cloudVmTypes;
    }

    private void convertVmSizeToGB(Collection<VmType> types) {
        types.stream()
                .filter(t -> t.getMetaData().getProperties().containsKey(VmTypeMeta.MEMORY))
                .forEach(t -> {
                    String memory = t.getMetaData().getProperties().get(VmTypeMeta.MEMORY).toString();
                    String formattedSize = ConversionUtil.convertToGB(memory);
                    t.getMetaData().getProperties().put(VmTypeMeta.MEMORY, formattedSize);
                });
    }

    private Set<VmType> collectVmTypes(OSClient<?> osClient) {
        Set<VmType> types = new HashSet<>();
        for (Flavor flavor : openStackClient.getFlavors(osClient)) {
            VmTypeMetaBuilder builder = VmTypeMetaBuilder.builder()
                    .withCpuAndMemory(flavor.getVcpus(), flavor.getRam());
            for (VolumeParameterType volumeParameterType : values()) {
                switch (volumeParameterType) {
                    case MAGNETIC:
                        builder.withMagneticConfig(volumeParameterConfig(MAGNETIC));
                        break;
                    case SSD:
                        builder.withSsdConfig(null);
                        break;
                    case EPHEMERAL:
                        builder.withEphemeralConfig(null);
                        break;
                    case ST1:
                        builder.withSt1Config(null);
                        break;
                    case AUTO_ATTACHED:
                        builder.withAutoAttachedConfig(null);
                        break;
                    default:
                        break;
                }
            }
            VmType vmType = VmType.vmTypeWithMeta(flavor.getName(), builder.create(), true);
            types.add(vmType);
        }
        LOGGER.debug("Openstack collect vm types result: {}", types);
        return types;
    }

    @Override
    public CloudGateWays gateways(CloudCredential cloudCredential, Region region, Map<String, String> filters) {
        OSClient<?> osClient = openStackClient.createOSClient(cloudCredential);
        Map<String, Set<CloudGateWay>> resultCloudGateWayMap = new HashMap<>();
        CloudRegions regions = regions(cloudCredential, region, filters);
        for (Entry<Region, List<AvailabilityZone>> regionListEntry : regions.getCloudRegions().entrySet()) {
            Set<CloudGateWay> cloudGateWays = new HashSet<>();
            List<? extends Router> routerList = osClient.networking().router().list();
            LOGGER.debug("Routers from openstack: {}", routerList);
            for (Router router : routerList) {
                CloudGateWay cloudGateWay = new CloudGateWay();
                cloudGateWay.setId(router.getId());
                cloudGateWay.setName(router.getName());
                Map<String, Object> properties = new HashMap<>();
                properties.put("tenantId", router.getTenantId());
                cloudGateWay.setProperties(properties);
                cloudGateWays.add(cloudGateWay);
            }
            for (AvailabilityZone availabilityZone : regionListEntry.getValue()) {
                resultCloudGateWayMap.put(availabilityZone.value(), cloudGateWays);
            }
        }
        CloudGateWays cloudGateWays = new CloudGateWays(resultCloudGateWayMap);
        LOGGER.debug("Openstack cloudgateway result: {}", cloudGateWays);
        return cloudGateWays;
    }

    @Override
    public CloudIpPools publicIpPool(CloudCredential cloudCredential, Region region, Map<String, String> filters) {
        OSClient<?> osClient = openStackClient.createOSClient(cloudCredential);
        Map<String, Set<CloudIpPool>> cloudIpPools = new HashMap<>();
        CloudRegions regions = regions(cloudCredential, region, filters);
        for (Entry<Region, List<AvailabilityZone>> regionListEntry : regions.getCloudRegions().entrySet()) {
            Set<CloudIpPool> cloudGateWays = new HashSet<>();
            List<? extends Network> networks = getNetworks(osClient);
            List<? extends Network> networksWithExternalRouter = networks.stream().filter(Network::isRouterExternal).collect(Collectors.toList());
            for (Network network : networksWithExternalRouter) {
                CloudIpPool cloudIpPool = new CloudIpPool();
                cloudIpPool.setId(network.getId());
                cloudIpPool.setName(network.getName());
                cloudGateWays.add(cloudIpPool);
            }
            for (AvailabilityZone availabilityZone : regionListEntry.getValue()) {
                cloudIpPools.put(availabilityZone.value(), cloudGateWays);
            }
        }
        LOGGER.debug("Openstack public ip pool result: {}", cloudIpPools);
        return new CloudIpPools(cloudIpPools);
    }

    @Override
    public CloudAccessConfigs accessConfigs(CloudCredential cloudCredential, Region region, Map<String, String> filters) {
        return new CloudAccessConfigs(new HashSet<>());
    }

    @Override
    public CloudEncryptionKeys encryptionKeys(CloudCredential cloudCredential, Region region, Map<String, String> filters) {
        return new CloudEncryptionKeys(new HashSet<>());
    }

    private VolumeParameterConfig volumeParameterConfig(VolumeParameterType volumeParameterType) {
        return new VolumeParameterConfig(
                volumeParameterType,
                defaultMinimumVolumeSize,
                defaultMaximumVolumeSize,
                defaultMinimumVolumeCount,
                defaultMaximumVolumeCount);
    }

    private List<? extends Network> getNetworks(OSClient<?> osClient) {
        List<? extends Network> networks = osClient.networking().network().list();
        LOGGER.debug("Networks from openstack: {}", networks);
        return networks;
    }

}
