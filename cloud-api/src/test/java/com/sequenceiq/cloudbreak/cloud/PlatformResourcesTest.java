package com.sequenceiq.cloudbreak.cloud;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
import com.sequenceiq.cloudbreak.cloud.model.ExtendedCloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.Region;
import com.sequenceiq.cloudbreak.cloud.model.dns.CloudPrivateDnsZones;
import com.sequenceiq.cloudbreak.cloud.model.nosql.CloudNoSqlTables;
import com.sequenceiq.cloudbreak.cloud.model.resourcegroup.CloudResourceGroups;

@ExtendWith(MockitoExtension.class)
class PlatformResourcesTest {

    @Mock
    private ExtendedCloudCredential cloudCredential;

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
        public CloudNetworks networks(ExtendedCloudCredential cloudCredential, Region region, Map<String, String> filters) {
            return null;
        }

        @Override
        public CloudSshKeys sshKeys(ExtendedCloudCredential cloudCredential, Region region, Map<String, String> filters) {
            return null;
        }

        @Override
        public CloudSecurityGroups securityGroups(ExtendedCloudCredential cloudCredential, Region region, Map<String, String> filters) {
            return null;
        }

        @Override
        public CloudRegions regions(ExtendedCloudCredential cloudCredential, Region region, Map<String, String> filters,
            boolean availabilityZonesNeeded) {
            return null;
        }

        @Override
        public CloudVmTypes virtualMachines(ExtendedCloudCredential cloudCredential, Region region, Map<String, String> filters) {
            return null;
        }

        @Override
        public CloudDatabaseVmTypes databaseVirtualMachines(ExtendedCloudCredential cloudCredential, Region region, Map<String, String> filters) {
            return null;
        }

        @Override
        public Optional<String> getVirtualMachineUrl(ExtendedCloudCredential cloudCredential, Region region, String instanceId, Map<String, String> filters) {
            return Optional.empty();
        }

        @Override
        public CloudGateWays gateways(ExtendedCloudCredential cloudCredential, Region region, Map<String, String> filters) {
            return null;
        }

        @Override
        public CloudIpPools publicIpPool(ExtendedCloudCredential cloudCredential, Region region, Map<String, String> filters) {
            return null;
        }

        @Override
        public CloudAccessConfigs accessConfigs(ExtendedCloudCredential cloudCredential, Region region, Map<String, String> filters) {
            return null;
        }

        @Override
        public boolean isEncryptionKeyUsable(ExtendedCloudCredential cloudCredential, String region, String key) {
            return true;
        }

        @Override
        public CloudEncryptionKeys encryptionKeys(ExtendedCloudCredential cloudCredential, Region region, Map<String, String> filters) {
            return null;
        }

        @Override
        public CloudNoSqlTables noSqlTables(ExtendedCloudCredential cloudCredential, Region region, Map<String, String> filters) {
            return null;
        }

        @Override
        public CloudResourceGroups resourceGroups(ExtendedCloudCredential cloudCredential, Region region, Map<String, String> filters) {
            return null;
        }

        @Override
        public CloudPrivateDnsZones privateDnsZones(ExtendedCloudCredential cloudCredential, Map<String, String> filters) {
            return null;
        }
    }
}