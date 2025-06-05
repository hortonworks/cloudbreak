package com.sequenceiq.freeipa.service.freeipa.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;
import org.springframework.test.util.ReflectionTestUtils;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.common.orchestration.Node;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.service.proxy.ProxyConfigDtoService;
import com.sequenceiq.cloudbreak.telemetry.fluent.cloud.AdlsGen2ConfigGenerator;
import com.sequenceiq.common.api.cloudstorage.old.S3CloudStorageV1Parameters;
import com.sequenceiq.common.api.type.Tunnel;
import com.sequenceiq.common.model.SeLinux;
import com.sequenceiq.freeipa.api.model.Backup;
import com.sequenceiq.freeipa.entity.FreeIpa;
import com.sequenceiq.freeipa.entity.LoadBalancer;
import com.sequenceiq.freeipa.entity.Network;
import com.sequenceiq.freeipa.entity.SecurityConfig;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.EnvironmentService;
import com.sequenceiq.freeipa.service.GatewayConfigService;
import com.sequenceiq.freeipa.service.freeipa.FreeIpaClientFactory;
import com.sequenceiq.freeipa.service.freeipa.FreeIpaService;
import com.sequenceiq.freeipa.service.freeipa.backup.cloud.S3BackupConfigGenerator;
import com.sequenceiq.freeipa.service.freeipa.dns.ReverseDnsZoneCalculator;
import com.sequenceiq.freeipa.service.loadbalancer.FreeIpaLoadBalancerService;
import com.sequenceiq.freeipa.service.stack.NetworkService;

@ExtendWith(MockitoExtension.class)
class FreeIpaConfigServiceTest {

    private static final String DOMAIN = "cloudera.com";

    private static final String HOSTNAME = "foo.cloudera.com";

    private static final String PASSWORD = "foobar";

    private static final String ADMIN = "admin";

    private static final String REVERSE_ZONE = "117.10.in-addr.arpa.";

    private static final String PRIVATE_IP = "10.117.0.69";

    private static final String CIDR = "10.0.0.0/24";

    private static final String ENV_CRN = "envCrn";

    private static final String KERBEROS_SECRET_LOCATION = "kerberosSecretLocation";

    private static final String ACCOUNT = "testAccount";

    private static final String TLS_VERSIONS = "TLSv1.2";

    private static final String TLS_CIPHERSUITES = "CIPHERSUITES";

    private Multimap<String, String> subnetWithCidr;

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

    @Mock
    private Environment environment;

    @Mock
    private ProxyConfigDtoService proxyConfigDtoService;

    @Mock
    private EnvironmentService environmentService;

    @Mock
    private EntitlementService entitlementService;

    @Mock
    private FreeIpaLoadBalancerService loadBalancerService;

    @InjectMocks
    private FreeIpaConfigService underTest;

    @BeforeEach
    public void init() {
        subnetWithCidr = ArrayListMultimap.create();
        subnetWithCidr.put("10.117.0.0", "16");
        ReflectionTestUtils.setField(underTest, "kerberosSecretLocation", KERBEROS_SECRET_LOCATION);
        when(loadBalancerService.findByStackId(any())).thenReturn(Optional.empty());
    }

    @Test
    void testCreateFreeIpaConfigs() {
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
        stack.setRegion("region");
        SecurityConfig securityConfig = new SecurityConfig();
        securityConfig.setSeLinux(SeLinux.ENFORCING);
        stack.setSecurityConfig(securityConfig);
        stack.setEnvironmentCrn(ENV_CRN);
        Network network = new Network();
        network.setNetworkCidrs(List.of(CIDR));
        stack.setNetwork(network);
        stack.setAccountId(ACCOUNT);

        when(freeIpaService.findByStack(any())).thenReturn(freeIpa);
        when(freeIpaClientFactory.getAdminUser()).thenReturn(ADMIN);
        when(networkService.getFilteredSubnetWithCidr(any())).thenReturn(subnetWithCidr);
        when(reverseDnsZoneCalculator.reverseDnsZoneForCidrs(any())).thenReturn(REVERSE_ZONE);
        when(environment.getProperty("freeipa.platform.dnssec.validation.AWS", "true")).thenReturn("true");
        GatewayConfig gatewayConfig = mock(GatewayConfig.class);
        when(gatewayConfig.getHostname()).thenReturn(HOSTNAME);
        when(gatewayConfigService.getPrimaryGatewayConfig(any())).thenReturn(gatewayConfig);
        when(proxyConfigDtoService.getByEnvironmentCrn(anyString())).thenReturn(Optional.empty());
        when(environmentService.isSecretEncryptionEnabled(ENV_CRN)).thenReturn(true);
        when(environmentService.getTlsVersions(ENV_CRN, " ")).thenReturn(TLS_VERSIONS);
        when(environmentService.getTlsVersions(ENV_CRN, ",")).thenReturn(TLS_VERSIONS);

        Node node = new Node(PRIVATE_IP, null, null, null, HOSTNAME, DOMAIN, (String) null);
        Map<String, String> expectedHost = Map.of("ip", PRIVATE_IP, "fqdn", HOSTNAME);
        Set<Object> expectedHosts = ImmutableSet.of(expectedHost);

        FreeIpaConfigView freeIpaConfigView = underTest.createFreeIpaConfigs(
                stack, ImmutableSet.of(node));

        assertEquals(DOMAIN.toUpperCase(Locale.ROOT), freeIpaConfigView.getRealm());
        assertEquals(DOMAIN, freeIpaConfigView.getDomain());
        assertEquals(PASSWORD, freeIpaConfigView.getPassword());
        assertEquals(REVERSE_ZONE, freeIpaConfigView.getReverseZones());
        assertEquals(ADMIN, freeIpaConfigView.getAdminUser());
        assertEquals(HOSTNAME, freeIpaConfigView.getFreeipaToReplicate());
        assertEquals(backupLocation, freeIpaConfigView.getBackup().getLocation());
        assertEquals(CloudPlatform.AWS.name(), freeIpaConfigView.getBackup().getPlatform());
        assertEquals(expectedHosts, freeIpaConfigView.getHosts());
        assertEquals(List.of(CIDR), freeIpaConfigView.getCidrBlocks());
        assertEquals(true, freeIpaConfigView.isSecretEncryptionEnabled());
        assertEquals(KERBEROS_SECRET_LOCATION, freeIpaConfigView.getKerberosSecretLocation());
        assertEquals(SeLinux.ENFORCING.name().toLowerCase(Locale.ROOT), freeIpaConfigView.getSeLinux());
        assertEquals(TLS_VERSIONS, freeIpaConfigView.getTlsVersionsSpaceSeparated());
        assertEquals(TLS_VERSIONS, freeIpaConfigView.getTlsVersionsCommaSeparated());
    }

    @Test
    void testCreateFreeIpaConfigsWithLb() {
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
        stack.setRegion("region");
        SecurityConfig securityConfig = new SecurityConfig();
        securityConfig.setSeLinux(SeLinux.ENFORCING);
        stack.setSecurityConfig(securityConfig);
        stack.setEnvironmentCrn(ENV_CRN);
        Network network = new Network();
        network.setNetworkCidrs(List.of(CIDR));
        stack.setNetwork(network);
        stack.setAccountId(ACCOUNT);
        LoadBalancer loadBalancer = new LoadBalancer();
        loadBalancer.setFqdn("lb.fqdn");
        loadBalancer.setEndpoint("lb");

        when(freeIpaService.findByStack(any())).thenReturn(freeIpa);
        when(freeIpaClientFactory.getAdminUser()).thenReturn(ADMIN);
        when(networkService.getFilteredSubnetWithCidr(any())).thenReturn(subnetWithCidr);
        when(reverseDnsZoneCalculator.reverseDnsZoneForCidrs(any())).thenReturn(REVERSE_ZONE);
        when(environment.getProperty("freeipa.platform.dnssec.validation.AWS", "true")).thenReturn("true");
        GatewayConfig gatewayConfig = mock(GatewayConfig.class);
        when(gatewayConfig.getHostname()).thenReturn(HOSTNAME);
        when(gatewayConfigService.getPrimaryGatewayConfig(any())).thenReturn(gatewayConfig);
        when(proxyConfigDtoService.getByEnvironmentCrn(anyString())).thenReturn(Optional.empty());
        when(environmentService.isSecretEncryptionEnabled(ENV_CRN)).thenReturn(true);
        when(environmentService.getTlsVersions(ENV_CRN, " ")).thenReturn(TLS_VERSIONS);
        when(environmentService.getTlsVersions(ENV_CRN, ",")).thenReturn(TLS_VERSIONS);
        when(environmentService.getTlsCipherSuites(ENV_CRN)).thenReturn(TLS_CIPHERSUITES);

        when(loadBalancerService.findByStackId(any())).thenReturn(Optional.of(loadBalancer));

        Node node = new Node(PRIVATE_IP, null, null, null, HOSTNAME, DOMAIN, (String) null);
        Map<String, String> expectedHost = Map.of("ip", PRIVATE_IP, "fqdn", HOSTNAME);
        Set<Object> expectedHosts = ImmutableSet.of(expectedHost);

        FreeIpaConfigView freeIpaConfigView = underTest.createFreeIpaConfigs(
                stack, ImmutableSet.of(node));

        assertEquals(DOMAIN.toUpperCase(Locale.ROOT), freeIpaConfigView.getRealm());
        assertEquals(DOMAIN, freeIpaConfigView.getDomain());
        assertEquals(PASSWORD, freeIpaConfigView.getPassword());
        assertEquals(REVERSE_ZONE, freeIpaConfigView.getReverseZones());
        assertEquals(ADMIN, freeIpaConfigView.getAdminUser());
        assertEquals(HOSTNAME, freeIpaConfigView.getFreeipaToReplicate());
        assertEquals(backupLocation, freeIpaConfigView.getBackup().getLocation());
        assertEquals(CloudPlatform.AWS.name(), freeIpaConfigView.getBackup().getPlatform());
        assertEquals(expectedHosts, freeIpaConfigView.getHosts());
        assertEquals(List.of(CIDR), freeIpaConfigView.getCidrBlocks());
        assertEquals(true, freeIpaConfigView.isSecretEncryptionEnabled());
        assertEquals(KERBEROS_SECRET_LOCATION, freeIpaConfigView.getKerberosSecretLocation());
        assertEquals(SeLinux.ENFORCING.name().toLowerCase(Locale.ROOT), freeIpaConfigView.getSeLinux());
        assertEquals(TLS_VERSIONS, freeIpaConfigView.getTlsVersionsSpaceSeparated());
        assertEquals(TLS_VERSIONS, freeIpaConfigView.getTlsVersionsCommaSeparated());
        Map<String, Object> lbConf = (Map<String, Object>) freeIpaConfigView.toMap().get("loadBalancer");
        assertEquals(loadBalancer.getEndpoint(), lbConf.get("endpoint"));
        assertEquals(loadBalancer.getFqdn(), lbConf.get("fqdn"));
        assertTrue((Boolean) lbConf.get("enabled"));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("ccmv2Scenarios")
    void testCcmV2Tunnel(Tunnel tunnel, boolean expectedCcmv2Enabled, boolean expectedCcmV2JumpgateEnabled) {
        Stack stack = new Stack();
        stack.setTunnel(tunnel);
        Network network = new Network();
        network.setNetworkCidrs(List.of(CIDR));
        stack.setNetwork(network);
        stack.setEnvironmentCrn(ENV_CRN);
        stack.setAccountId(ACCOUNT);

        FreeIpa freeIpa = new FreeIpa();
        freeIpa.setDomain(DOMAIN);
        when(freeIpaService.findByStack(any())).thenReturn(freeIpa);
        when(reverseDnsZoneCalculator.reverseDnsZoneForCidrs(any())).thenReturn(REVERSE_ZONE);
        when(networkService.getFilteredSubnetWithCidr(any())).thenReturn(subnetWithCidr);
        GatewayConfig gatewayConfig = mock(GatewayConfig.class);
        when(gatewayConfig.getHostname()).thenReturn(HOSTNAME);
        when(gatewayConfigService.getPrimaryGatewayConfig(any())).thenReturn(gatewayConfig);
        when(networkService.getFilteredSubnetWithCidr(any())).thenReturn(subnetWithCidr);
        when(environmentService.isSecretEncryptionEnabled(ENV_CRN)).thenReturn(false);
        when(environmentService.getTlsVersions(ENV_CRN, " ")).thenReturn(TLS_VERSIONS);
        when(environmentService.getTlsVersions(ENV_CRN, ",")).thenReturn(TLS_VERSIONS);
        when(environmentService.getTlsCipherSuites(ENV_CRN)).thenReturn(TLS_CIPHERSUITES);


        FreeIpaConfigView freeIpaConfigView = underTest.createFreeIpaConfigs(
                stack, Set.of());

        assertEquals(expectedCcmv2Enabled, freeIpaConfigView.isCcmv2Enabled());
        assertEquals(expectedCcmV2JumpgateEnabled, freeIpaConfigView.isCcmv2JumpgateEnabled());
        assertEquals(false, freeIpaConfigView.isSecretEncryptionEnabled());
        assertEquals(KERBEROS_SECRET_LOCATION, freeIpaConfigView.getKerberosSecretLocation());
        assertEquals(SeLinux.PERMISSIVE.name().toLowerCase(Locale.ROOT), freeIpaConfigView.getSeLinux());
        assertEquals(TLS_VERSIONS, freeIpaConfigView.getTlsVersionsSpaceSeparated());
        assertEquals(TLS_VERSIONS, freeIpaConfigView.getTlsVersionsCommaSeparated());
    }

    // @formatter:off
    // CHECKSTYLE:OFF
    static Object[][] ccmv2Scenarios() {
        return new Object[][] {
                // testName              expectedCcmv2Enabled    expectedCcmV2JumpgateEnabled
                { Tunnel.DIRECT,         false,                  false },
                { Tunnel.CLUSTER_PROXY,  false,                  false },
                { Tunnel.CCM,            false,                  false },
                { Tunnel.CCMV2,          true,                   false },
                { Tunnel.CCMV2_JUMPGATE, true,                   true  },
        };
    }
    // CHECKSTYLE:ON
    // @formatter:on

}
