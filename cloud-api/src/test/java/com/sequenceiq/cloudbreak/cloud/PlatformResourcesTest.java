package com.sequenceiq.cloudbreak.cloud;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.model.CloudAccessConfigs;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudEncryptionKeys;
import com.sequenceiq.cloudbreak.cloud.model.CloudGateWays;
import com.sequenceiq.cloudbreak.cloud.model.CloudIpPools;
import com.sequenceiq.cloudbreak.cloud.model.CloudNetworks;
import com.sequenceiq.cloudbreak.cloud.model.CloudRegions;
import com.sequenceiq.cloudbreak.cloud.model.CloudSecurityGroups;
import com.sequenceiq.cloudbreak.cloud.model.CloudSshKeys;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmTypes;
import com.sequenceiq.cloudbreak.cloud.model.Region;
import com.sequenceiq.cloudbreak.cloud.model.nosql.CloudNoSqlTables;
import com.sequenceiq.cloudbreak.cloud.model.resourcegroup.CloudResourceGroups;

@ExtendWith(MockitoExtension.class)
class PlatformResourcesTest {

    @Mock
    private CloudCredential cloudCredential;

    @Mock
    private Region region;

    @InjectMocks
    private PlatformResourcesImpl underTest;

    @Test
    void databaseServerGeneralSslRootCertificatesTest() {
        assertThrows(UnsupportedOperationException.class, () -> underTest.databaseServerGeneralSslRootCertificates(cloudCredential, region));
    }

    private static class PlatformResourcesImpl implements PlatformResources {

        @Override
        public CloudNetworks networks(CloudCredential cloudCredential, Region region, Map<String, String> filters) {
            return null;
        }

        @Override
        public CloudSshKeys sshKeys(CloudCredential cloudCredential, Region region, Map<String, String> filters) {
            return null;
        }

        @Override
        public CloudSecurityGroups securityGroups(CloudCredential cloudCredential, Region region, Map<String, String> filters) {
            return null;
        }

        @Override
        public CloudRegions regions(CloudCredential cloudCredential, Region region, Map<String, String> filters,
            boolean availabilityZonesNeeded, List<String> entitlements) {
            return null;
        }

        @Override
        public CloudVmTypes virtualMachines(CloudCredential cloudCredential, Region region, Map<String, String> filters, List<String> entitlements) {
            return null;
        }

        @Override
        public CloudGateWays gateways(CloudCredential cloudCredential, Region region, Map<String, String> filters) {
            return null;
        }

        @Override
        public CloudIpPools publicIpPool(CloudCredential cloudCredential, Region region, Map<String, String> filters) {
            return null;
        }

        @Override
        public CloudAccessConfigs accessConfigs(CloudCredential cloudCredential, Region region, Map<String, String> filters) {
            return null;
        }

        @Override
        public CloudEncryptionKeys encryptionKeys(CloudCredential cloudCredential, Region region, Map<String, String> filters) {
            return null;
        }

        @Override
        public CloudNoSqlTables noSqlTables(CloudCredential cloudCredential, Region region, Map<String, String> filters) {
            return null;
        }

        @Override
        public CloudResourceGroups resourceGroups(CloudCredential cloudCredential, Region region, Map<String, String> filters) {
            return null;
        }

    }

}