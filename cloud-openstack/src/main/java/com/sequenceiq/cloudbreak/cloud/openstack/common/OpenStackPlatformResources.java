package com.sequenceiq.cloudbreak.cloud.openstack.common;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.openstack4j.api.OSClient;
import org.openstack4j.model.compute.Keypair;
import org.openstack4j.model.compute.SecGroupExtension;
import org.openstack4j.model.network.Network;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.PlatformResources;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudNetwork;
import com.sequenceiq.cloudbreak.cloud.model.CloudNetworks;
import com.sequenceiq.cloudbreak.cloud.model.CloudSecurityGroup;
import com.sequenceiq.cloudbreak.cloud.model.CloudSecurityGroups;
import com.sequenceiq.cloudbreak.cloud.model.CloudSshKey;
import com.sequenceiq.cloudbreak.cloud.model.CloudSshKeys;
import com.sequenceiq.cloudbreak.cloud.model.Region;
import com.sequenceiq.cloudbreak.cloud.openstack.auth.OpenStackClient;
import com.sequenceiq.cloudbreak.cloud.openstack.view.KeystoneCredentialView;

@Service
public class OpenStackPlatformResources implements PlatformResources {

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

            CloudNetwork cloudNetwork = new CloudNetwork(network.getName(), new HashSet<>(network.getSubnets()), properties);
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
}
