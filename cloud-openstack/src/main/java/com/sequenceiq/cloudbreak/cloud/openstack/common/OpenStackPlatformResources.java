package com.sequenceiq.cloudbreak.cloud.openstack.common;

import static com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone.availabilityZone;
import static com.sequenceiq.cloudbreak.cloud.model.Region.region;
import static com.sequenceiq.cloudbreak.cloud.model.VolumeParameterType.MAGNETIC;
import static com.sequenceiq.cloudbreak.cloud.model.VolumeParameterType.values;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.openstack4j.api.OSClient;
import org.openstack4j.model.compute.Flavor;
import org.openstack4j.model.compute.Keypair;
import org.openstack4j.model.compute.SecGroupExtension;
import org.openstack4j.model.network.Network;
import org.openstack4j.model.network.Router;
import org.openstack4j.model.network.Subnet;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.PlatformResources;
import com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
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
import com.sequenceiq.cloudbreak.cloud.model.Region;
import com.sequenceiq.cloudbreak.cloud.model.VmType;
import com.sequenceiq.cloudbreak.cloud.model.VmTypeMeta;
import com.sequenceiq.cloudbreak.cloud.model.VolumeParameterConfig;
import com.sequenceiq.cloudbreak.cloud.model.VolumeParameterType;
import com.sequenceiq.cloudbreak.cloud.openstack.auth.OpenStackClient;
import com.sequenceiq.cloudbreak.cloud.openstack.view.KeystoneCredentialView;

@Service
public class OpenStackPlatformResources implements PlatformResources {

    public static final int DEFAULT_MINIMUM_VOLUME_SIZE = 10;

    public static final int DEFAULT_MAXIMUM_VOLUME_SIZE = 1023;

    public static final int DEFAULT_MINIMUM_VOLUME_COUNT = 0;

    @Inject
    private OpenStackClient openStackClient;

    @Override
    public CloudNetworks networks(CloudCredential cloudCredential, Region region, Map<String, String> filters) {
        OSClient osClient = openStackClient.createOSClient(cloudCredential);
        KeystoneCredentialView osCredential = openStackClient.createKeystoneCredential(cloudCredential);

        Set<CloudNetwork> cloudNetworks = new HashSet<>();
        for (Network network : osClient.networking().network().list()) {
            Map<String, Object> properties = new HashMap<>();
            properties.put("networkType", network.getNetworkType());
            properties.put("providerPhyNet", network.getProviderPhyNet());
            properties.put("providerSegID", network.getProviderSegID());
            properties.put("tenantId", network.getTenantId());

            Map<String, String> subnets = new HashMap<>();

            List<? extends Subnet> neutronSubnets = network.getNeutronSubnets();
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
        return new CloudNetworks(result);
    }

    @Override
    public CloudSshKeys sshKeys(CloudCredential cloudCredential, Region region, Map<String, String> filters) {
        OSClient osClient = openStackClient.createOSClient(cloudCredential);
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
        return new CloudSshKeys(result);
    }

    @Override
    public CloudSecurityGroups securityGroups(CloudCredential cloudCredential, Region region, Map<String, String> filters) {
        OSClient osClient = openStackClient.createOSClient(cloudCredential);
        KeystoneCredentialView osCredential = openStackClient.createKeystoneCredential(cloudCredential);

        Set<CloudSecurityGroup> cloudSecurityGroups = new HashSet<>();
        for (SecGroupExtension secGroup : osClient.compute().securityGroups().list()) {
            Map<String, Object> properties = new HashMap<>();
            properties.put("tenantId", secGroup.getTenantId());
            properties.put("rules", secGroup.getRules());

            CloudSecurityGroup cloudSecurityGroup = new CloudSecurityGroup(secGroup.getName(), secGroup.getId(), properties);
            cloudSecurityGroups.add(cloudSecurityGroup);
        }

        Map<String, Set<CloudSecurityGroup>> result = new HashMap<>();
        result.put(region.value() == null ? osCredential.getTenantName() : region.value(), cloudSecurityGroups);
        return new CloudSecurityGroups(result);
    }

    @Override
    @Cacheable(cacheNames = "cloudResourceRegionCache", key = "#cloudCredential?.id")
    public CloudRegions regions(CloudCredential cloudCredential, Region region, Map<String, String> filters) {
        Set<String> regionFromOpenStack = openStackClient.getRegion(cloudCredential);
        OSClient osClient = openStackClient.createOSClient(cloudCredential);

        Map<Region, List<com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone>> cloudRegions = new HashMap<>();
        Map<Region, String> displayNames = new HashMap<>();
        for (String regionOne : regionFromOpenStack) {
            List<AvailabilityZone> availabilityZones = new ArrayList<>();
            for (org.openstack4j.model.compute.ext.AvailabilityZone availabilityZone : osClient.compute().zones().list()) {
                availabilityZones.add(availabilityZone(availabilityZone.getZoneName()));
            }
            cloudRegions.put(region(regionOne), availabilityZones);
            displayNames.put(region(regionOne), regionOne);
        }
        String defaultRegion = null;
        if (cloudRegions.keySet().size() > 0) {
            defaultRegion = ((Region) cloudRegions.keySet().toArray()[0]).value();
        }
        return new CloudRegions(cloudRegions, displayNames, defaultRegion);
    }

    @Override
    @Cacheable(cacheNames = "cloudResourceVmTypeCache", key = "#cloudCredential?.id + #region.getRegionName()")
    public CloudVmTypes virtualMachines(CloudCredential cloudCredential, Region region, Map<String, String> filters) {
        OSClient osClient = openStackClient.createOSClient(cloudCredential);
        Map<String, Set<VmType>> cloudVmResponses = new HashMap<>();
        Map<String, VmType> defaultCloudVmResponses = new HashMap<>();
        CloudRegions regions = regions(cloudCredential, region, filters);
        for (Region cloudRegion : regions.getCloudRegions().keySet()) {
            Set<VmType> types = collectVmTypes(osClient);
            for (AvailabilityZone availabilityZone : regions.getCloudRegions().get(cloudRegion)) {
                cloudVmResponses.put(availabilityZone.value(), types);
            }
            defaultCloudVmResponses.put(cloudRegion.value(), types.size() > 0 ? (VmType) types.toArray()[0] : null);
        }
        return new CloudVmTypes(cloudVmResponses, defaultCloudVmResponses);
    }

    private Set<VmType> collectVmTypes(OSClient osClient) {
        Set<VmType> types = new HashSet<>();
        for (Flavor flavor : osClient.compute().flavors().list()) {
            VmTypeMeta.VmTypeMetaBuilder builder = VmTypeMeta.VmTypeMetaBuilder.builder()
                    .withCpuAndMemory(flavor.getVcpus(), flavor.getRam());
            for (VolumeParameterType volumeParameterType : values()) {
                switch (volumeParameterType) {
                    case MAGNETIC:
                        builder.withMagneticConfig(volumeParameterConfig(MAGNETIC, flavor));
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
        return types;
    }

    @Override
    public CloudGateWays gateways(CloudCredential cloudCredential, Region region, Map<String, String> filters) throws Exception {
        OSClient osClient = openStackClient.createOSClient(cloudCredential);
        Map<String, Set<CloudGateWay>> resultCloudGateWayMap = new HashMap<>();
        CloudRegions regions = regions(cloudCredential, region, filters);
        for (Map.Entry<Region, List<AvailabilityZone>> regionListEntry : regions.getCloudRegions().entrySet()) {
            Set<CloudGateWay> cloudGateWays = new HashSet<>();
            for (Router router : osClient.networking().router().list()) {
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
        return new CloudGateWays(resultCloudGateWayMap);
    }

    @Override
    public CloudIpPools publicIpPool(CloudCredential cloudCredential, Region region, Map<String, String> filters) throws Exception {
        OSClient osClient = openStackClient.createOSClient(cloudCredential);
        Map<String, Set<CloudIpPool>> cloudIpPools = new HashMap<>();
        CloudRegions regions = regions(cloudCredential, region, filters);
        for (Map.Entry<Region, List<AvailabilityZone>> regionListEntry : regions.getCloudRegions().entrySet()) {
            Set<CloudIpPool> cloudGateWays = new HashSet<>();
            List<? extends Network> networks = osClient.networking().network().list();
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
        return new CloudIpPools(cloudIpPools);
    }

    private VolumeParameterConfig volumeParameterConfig(VolumeParameterType volumeParameterType, Flavor flavor) {
        return new VolumeParameterConfig(
                volumeParameterType,
                Integer.valueOf(DEFAULT_MINIMUM_VOLUME_SIZE),
                Integer.valueOf(DEFAULT_MAXIMUM_VOLUME_SIZE),
                Integer.valueOf(DEFAULT_MINIMUM_VOLUME_COUNT),
                flavor.getDisk());
    }

}
