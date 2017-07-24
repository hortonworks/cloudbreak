package com.sequenceiq.cloudbreak.cloud.byos;

import java.util.Map;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.PlatformResources;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudNetworks;
import com.sequenceiq.cloudbreak.cloud.model.CloudSecurityGroups;
import com.sequenceiq.cloudbreak.cloud.model.CloudSshKeys;
import com.sequenceiq.cloudbreak.cloud.model.Region;

@Service
public class BYOSPlatformResources implements PlatformResources {

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

}
