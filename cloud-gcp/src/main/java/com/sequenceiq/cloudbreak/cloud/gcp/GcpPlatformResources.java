package com.sequenceiq.cloudbreak.cloud.gcp;

import static com.sequenceiq.cloudbreak.cloud.model.Region.region;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

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
import com.sequenceiq.cloudbreak.cloud.model.Region;
import com.sequenceiq.cloudbreak.cloud.model.VmType;
import com.sequenceiq.cloudbreak.cloud.model.VmTypeMeta;
import com.sequenceiq.cloudbreak.cloud.model.VmTypeMeta.VmTypeMetaBuilder;

@Service
public class GcpPlatformResources implements PlatformResources {

    private static final float THOUSAND = 1000.0f;

    private static final int TEN = 10;

    @Value("${cb.gcp.default.vmtype:n1-highcpu-8}")
    private String gcpVmDefault;

    @Value("${cb.gcp.zone.parameter.default:europe-west1}")
    private String gcpZoneParameterDefault;

    @Inject
    private GcpPlatformParameters gcpPlatformParameters;

    @Override
    public CloudNetworks networks(CloudCredential cloudCredential, Region region, Map<String, String> filters) throws Exception {
        Compute compute = GcpStackUtil.buildCompute(cloudCredential);
        String projectId = GcpStackUtil.getProjectId(cloudCredential);
        Map<String, Set<CloudNetwork>> result = new HashMap<>();

        Set<CloudNetwork> cloudNetworks = new HashSet<>();
        if (compute != null) {
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

        }
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
        if (compute != null) {
            FirewallList firewallList = compute.firewalls().list(projectId).execute();
            for (Firewall firewall : firewallList.getItems()) {
                Map<String, Object> properties = new HashMap<>();
                properties.put("network", getNetworkName(firewall));
                CloudSecurityGroup cloudSecurityGroup = new CloudSecurityGroup(firewall.getName(), firewall.getName(), properties);
                result.computeIfAbsent(region.value(), k -> new HashSet<>()).add(cloudSecurityGroup);
            }
        }

        return new CloudSecurityGroups(result);
    }

    private String getNetworkName(Firewall firewall) {
        String [] splittedNetworkName = firewall.getNetwork().split("/");
        return splittedNetworkName[splittedNetworkName.length - 1];
    }

    @Override
    @Cacheable(cacheNames = "cloudResourceRegionCache", key = "#cloudCredential?.id")
    public CloudRegions regions(CloudCredential cloudCredential, Region region, Map<String, String> filters) throws Exception {
        Compute compute = GcpStackUtil.buildCompute(cloudCredential);
        String projectId = GcpStackUtil.getProjectId(cloudCredential);

        Map<Region, List<AvailabilityZone>> regionListMap = new HashMap<>();
        Map<Region, String> displayNames = new HashMap<>();
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
            }
        }
        if (region != null && !Strings.isNullOrEmpty(region.value())) {
            defaultRegion = region.value();
        }
        return new CloudRegions(regionListMap, displayNames, defaultRegion);
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

                            .withMaximumPersistentDisksSizeGb(machineType.getMaximumPersistentDisksSizeGb().toString())
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
        return new CloudEncryptionKeys(new HashSet<>());
    }
}
