package com.sequenceiq.freeipa.service.freeipa.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Set;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import com.google.common.collect.ImmutableSet;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.orchestrator.model.Node;
import com.sequenceiq.cloudbreak.telemetry.fluent.cloud.AdlsGen2ConfigGenerator;
import com.sequenceiq.common.api.cloudstorage.old.S3CloudStorageV1Parameters;
import com.sequenceiq.freeipa.api.model.Backup;
import com.sequenceiq.freeipa.entity.FreeIpa;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.GatewayConfigService;
import com.sequenceiq.freeipa.service.freeipa.FreeIpaClientFactory;
import com.sequenceiq.freeipa.service.freeipa.FreeIpaService;
import com.sequenceiq.freeipa.service.freeipa.backup.cloud.S3BackupConfigGenerator;
import com.sequenceiq.freeipa.service.freeipa.dns.ReverseDnsZoneCalculator;
import com.sequenceiq.freeipa.service.stack.NetworkService;

@RunWith(MockitoJUnitRunner.class)
public class FreeIpaConfigServiceTest {

    private static final String DOMAIN = "cloudera.com";

    private static final String HOSTNAME = "foo.cloudera.com";

    private static final String PASSWORD = "foobar";

    private static final String ADMIN = "admin";

    private static final Map<String, String> SUBNET_WITH_CIDR = Map.of("10.117.0.0", "16");

    private static final String REVERSE_ZONE = "117.10.in-addr.arpa.";

    private static final String PRIVATE_IP = "10.117.0.69";

    @Spy
    private S3BackupConfigGenerator s3ConfigGenerator;

    @Spy
    private AdlsGen2ConfigGenerator adlsGen2ConfigGenerator;

    @Mock
    private NetworkService networkService;

    @Mock
    private ReverseDnsZoneCalculator reverseDnsZoneCalculator;

    @Mock
    private FreeIpaService freeIpaService;

    @Mock
    private GatewayConfigService gatewayConfigService;

    @Mock
    private FreeIpaClientFactory freeIpaClientFactory;

    @InjectMocks
    private FreeIpaConfigService freeIpaConfigService;

    @Test
    public void testCreateFreeIpaConfigs() {
        String backupLocation = "s3://mybucket/test";
        Backup backup = new Backup();
        backup.setStorageLocation(backupLocation);
        backup.setS3(new S3CloudStorageV1Parameters());
        FreeIpa freeIpa = new FreeIpa();
        freeIpa.setDomain(DOMAIN);
        freeIpa.setAdminPassword(PASSWORD);
        Stack stack = new Stack();
        stack.setCloudPlatform(CloudPlatform.AWS.name());
        stack.setBackup(backup);

        when(freeIpaService.findByStack(any())).thenReturn(freeIpa);
        when(freeIpaClientFactory.getAdminUser()).thenReturn(ADMIN);
        when(networkService.getFilteredSubnetWithCidr(any())).thenReturn(SUBNET_WITH_CIDR);
        when(reverseDnsZoneCalculator.reverseDnsZoneForCidrs(any())).thenReturn(REVERSE_ZONE);
        GatewayConfig gatewayConfig = mock(GatewayConfig.class);
        when(gatewayConfig.getHostname()).thenReturn(HOSTNAME);
        when(gatewayConfigService.getPrimaryGatewayConfig(any())).thenReturn(gatewayConfig);

        Node node = new Node(PRIVATE_IP, null, null, null, HOSTNAME, DOMAIN, null);
        Map<String, String> expectedHost = Map.of("ip", PRIVATE_IP, "fqdn", HOSTNAME);
        Set<Object> expectedHosts = ImmutableSet.of(expectedHost);

        FreeIpaConfigView freeIpaConfigView = freeIpaConfigService.createFreeIpaConfigs(
                stack, ImmutableSet.of(node), null);

        assertEquals(DOMAIN.toUpperCase(), freeIpaConfigView.getRealm());
        assertEquals(DOMAIN, freeIpaConfigView.getDomain());
        assertEquals(PASSWORD, freeIpaConfigView.getPassword());
        assertEquals(REVERSE_ZONE, freeIpaConfigView.getReverseZones());
        assertEquals(ADMIN, freeIpaConfigView.getAdminUser());
        assertEquals(HOSTNAME, freeIpaConfigView.getFreeipaToReplicate());
        assertEquals(backupLocation, freeIpaConfigView.getBackup().getLocation());
        assertEquals(CloudPlatform.AWS.name(), freeIpaConfigView.getBackup().getPlatform());
        assertEquals(expectedHosts, freeIpaConfigView.getHosts());
    }
}
