package com.sequenceiq.cloudbreak.service.publicendpoint;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.cloudera.thunderhead.service.usermanagement.UserManagementProto;
import com.sequenceiq.cloudbreak.TestUtil;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.GrpcUmsClient;
import com.sequenceiq.cloudbreak.certificate.service.CertificateCreationService;
import com.sequenceiq.cloudbreak.certificate.service.DnsManagementService;
import com.sequenceiq.cloudbreak.dns.EnvironmentBasedDomainNameProvider;
import com.sequenceiq.cloudbreak.domain.SecurityConfig;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.stack.loadbalancer.LoadBalancer;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.environment.EnvironmentClientService;
import com.sequenceiq.cloudbreak.service.securityconfig.SecurityConfigService;
import com.sequenceiq.cloudbreak.service.stack.LoadBalancerService;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;

@ExtendWith(MockitoExtension.class)
class GatewayPublicEndpointManagementServiceTest {
    private static final String USER_CRN = "crn:cdp:iam:us-west-1:123:user:123";

    private static final String USER_FACING_PRIVATE_KEY = "-----BEGIN RSA PRIVATE KEY-----\n" +
            "MIIEpAIBAAKCAQEAoFtGc23VFKkiy5O6I5xiMK2Unx74Sdrk+kmqHMMxyICFUaEP\n" +
            "oC5yskgGAPEDDBwtMO0vwgW3Rthgl0ECU7Fd0q9HoNJE89Or7pnKdP4zbRKVPxqI\n" +
            "xl+pene59UEmwf3DLELmuziq42ZWlgTH7RePfGfGYuCo56CGZDjfOws7pGNuQr3x\n" +
            "+0xZoFW4e10Mle5+pg45WgG/SWWEc8ZLBQ/4xX6CBFyh5doQQp2w0TJrLtx/CKrr\n" +
            "xf4wKqOX6BfFXrkpJbQdbMMNe71XvT3AmNUKyTJiYNlqCuSz4yA4l72Wwoi/K63p\n" +
            "sMY5ZbRc4nMG8JuRvehS7TTAULWx+zTjWrhAPwIDAQABAoIBACRs93DrBxcdYIkL\n" +
            "8qF6OZfDJlqK522napIsP5cvA9T+1Mn5IxqI0ocK80otdTq//8f4aPvS3pIaPr69\n" +
            "BrFKPfzI0iWG/iDA+XJGvwWaZMYnDX37Igyl2FK4daZveUVhxn78Z3sp4S+spIiJ\n" +
            "Z3zE+FQTIq59SADtpvmHLbY01ASAMZKXEc33e5C92G2fCMlkGra5bLLl9CzxwtYQ\n" +
            "24PI/3qJ/nbcURT2jsE1IwBDeTb2BpYwu35FP/aKe/8aHmgEif7qvP4wQO4i8RIX\n" +
            "Hqef1FJXM5haEyRj8rnWZrsSDvbQZKwxAVWubyNVv56FWK5CJRO4tceHkSysbA/n\n" +
            "vvWIwtkCgYEAzOrYQefAa9zmT0A3PE11f/6y0m4Dsfvb1y3Q79DVJsUIBMiK5oZQ\n" +
            "9WyZw83gRyUt67mEwN+6nKMXGgGZb5hEtMUnH+Wo/ps+rhih8Uvi+P4pIfNsdLkV\n" +
            "E2DQP2qQ/8oI80r00VAi7LHd66z04XiJTo0tCcW2IhoSRYeHC4MkK/UCgYEAyFS0\n" +
            "csEIw5yU9vRGL+L/6hcLRxGkEBuMrDovLVkOI9i22sQwYYA/u8MeWlKJIEOuPUFw\n" +
            "L32ligGiWyvPN6C+mrvrNb5QiJ6RE4T/rCxQ6FSCx0VHND/s6P6n8Hv4iUR7rZo5\n" +
            "XrK8kF+hOIIncImLskNTCKFFr7tzn2pysMR6buMCgYBy7gCOrkGw/Xs61cRlEPim\n" +
            "6h43gbaW27CIdkzqRFFYZkfCDwxAkPLVQ6zWMiDpJkQkIq//UTwj4CDz2BPLkDZw\n" +
            "wObZsABhlIbKNEyXvj018670OMgKi0fzz2fdOZLLs+/jLJY002JTKMtUBHRwBR/V\n" +
            "q76n7XjilYbL8mBep5XhBQKBgQCp6C2jplTok0VErqlYtA6ZXDIUdMHRZ4xIBpE8\n" +
            "xOtZO9TadssR5tQnS7XSpW3oD38YNQgRP1/HTNuGuAFoDM2cLwWu71sehF5HT+YJ\n" +
            "AQ0d/49rszZQ+mbUtid5r6t7wLmk48kEqFOFn5X9d2Y77GyvJKqoByAzi6jk7EOZ\n" +
            "6QpuQQKBgQDIo/ob4N1PpIk68aiq5sxFbx7pZB+ACfH+shRAR9faYJ7nLVW+kKaG\n" +
            "BZJjfgbDroNYDOAd9ygNcswRHKqt2A4qELoZHNsJj1mCfDHbN2h66/kdX7S+bMUF\n" +
            "D6msf4xWBnyPOtWobjCRTiFFZAB3ME/1ZTj0/W1itArWVNjOXrZyHw==\n" +
            "-----END RSA PRIVATE KEY-----";

    @Mock
    private EnvironmentClientService environmentClientService;

    @Mock
    private SecurityConfigService securityConfigService;

    @Mock
    private ClusterService clusterService;

    @Mock
    private DnsManagementService dnsManagementService;

    @Mock
    private EnvironmentBasedDomainNameProvider domainNameProvider;

    @Mock
    private GrpcUmsClient grpcUmsClient;

    @Mock
    private CertificateCreationService certificateCreationService;

    @Mock
    private LoadBalancerService loadBalancerService;

    @InjectMocks
    private GatewayPublicEndpointManagementService underTest;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(underTest, "certGenerationEnabled", Boolean.TRUE);
    }

    @Test
    void testRenewCertificateWhenCertGenerationFeatureIsDisabled() {
        ReflectionTestUtils.setField(underTest, "certGenerationEnabled", Boolean.FALSE);

        boolean result = underTest.renewCertificate(null);

        verify(environmentClientService, Mockito.times(0)).getByCrn(Mockito.anyString());
        Assertions.assertEquals(Boolean.TRUE, result);
    }

    @Test
    void testRenewCertificateWhenCertGenerationFeatureIsEnabledButClusterDoesNotHaveUserFacingCert() {
        Stack stack = TestUtil.stack();

        boolean result = underTest.renewCertificate(stack);

        verify(environmentClientService, Mockito.times(0)).getByCrn(Mockito.anyString());
        Assertions.assertEquals(Boolean.TRUE, result);
    }

    @Test
    void testRenewCertificateWhenCertGenerationIsTriggerableButGenerationFailsAndDnsEntryShouldBeUpdated() {
        SecurityConfig securityConfig = new SecurityConfig();
        securityConfig.setUserFacingCert("CERT");
        securityConfig.setUserFacingKey(USER_FACING_PRIVATE_KEY);
        Cluster cluster = TestUtil.cluster();
        Stack stack = cluster.getStack();
        stack.setSecurityConfig(securityConfig);
        stack.setCluster(cluster);
        String envName = "anEnvName";
        DetailedEnvironmentResponse environment = DetailedEnvironmentResponse.Builder
                .builder()
                .withName(envName)
                .build();
        when(environmentClientService.getByCrn(Mockito.anyString()))
                .thenReturn(null)
                .thenReturn(environment);

        String accountWorkloadSubdomain = "aWorkloadSubdomain";
        UserManagementProto.Account umsAccount = UserManagementProto.Account.newBuilder()
                .setWorkloadSubdomain(accountWorkloadSubdomain)
                .build();
        when(grpcUmsClient.getAccountDetails(USER_CRN, "123", Optional.empty())).thenReturn(umsAccount);

        String endpointName = stack.getPrimaryGatewayInstance().getShortHostname();
        String fqdn = endpointName + ".anenvname.xcu2-8y8x.dev.cldr.work";
        when(domainNameProvider.getFullyQualifiedEndpointName(endpointName, envName, accountWorkloadSubdomain)).thenReturn(fqdn);
        when(dnsManagementService.createOrUpdateDnsEntryWithIp(anyString(), anyString(), anyString(), anyString(), anyBoolean(), any())).thenReturn(true);

        boolean result = ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.renewCertificate(stack));

        verify(environmentClientService, times(2)).getByCrn(anyString());
        verify(dnsManagementService, times(1)).createOrUpdateDnsEntryWithIp(anyString(), anyString(), anyString(), anyString(),
                anyBoolean(), any());
        verify(clusterService, times(1)).save(cluster);
        Mockito.verifyNoMoreInteractions(environmentClientService);
        Mockito.verifyNoMoreInteractions(domainNameProvider);
        Mockito.verifyNoMoreInteractions(dnsManagementService);
        Mockito.verifyNoMoreInteractions(securityConfigService);
        Assertions.assertEquals(Boolean.FALSE, result);
    }

    @Test
    void testRenewCertificateWhenCertGenerationIsTriggerableButGenerationFailsAndDnsEntryShouldNotBeUpdated() {
        SecurityConfig securityConfig = new SecurityConfig();
        securityConfig.setUserFacingCert("CERT");
        securityConfig.setUserFacingKey("KEY");
        Cluster cluster = TestUtil.cluster();
        cluster.setFqdn("aNiceShortHostname.testdomain");
        Stack stack = cluster.getStack();
        stack.setSecurityConfig(securityConfig);
        stack.setCluster(cluster);

        boolean result = ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.renewCertificate(stack));

        verify(environmentClientService, times(1)).getByCrn(anyString());
        Mockito.verifyNoMoreInteractions(environmentClientService);
        Mockito.verifyNoMoreInteractions(domainNameProvider);
        Mockito.verifyNoMoreInteractions(dnsManagementService);
        Mockito.verifyNoMoreInteractions(securityConfigService);
        Assertions.assertEquals(Boolean.FALSE, result);
    }

    @Test
    void testRenewCertificateWhenCertGenerationIsTriggerableAndDnsEntryShouldBeCreated() throws IOException {
        SecurityConfig securityConfig = new SecurityConfig();
        securityConfig.setUserFacingCert("CERT");
        securityConfig.setUserFacingKey(USER_FACING_PRIVATE_KEY);
        Cluster cluster = TestUtil.cluster();
        Stack stack = cluster.getStack();
        stack.setSecurityConfig(securityConfig);
        stack.setCluster(cluster);

        String envName = "anEnvName";
        DetailedEnvironmentResponse environment = DetailedEnvironmentResponse.Builder
                .builder()
                .withName(envName)
                .build();
        when(environmentClientService.getByCrn(Mockito.anyString())).thenReturn(environment);
        String accountWorkloadSubdomain = "aWorkloadSubdomain";
        UserManagementProto.Account umsAccount = UserManagementProto.Account.newBuilder()
                .setWorkloadSubdomain(accountWorkloadSubdomain)
                .build();
        when(grpcUmsClient.getAccountDetails(USER_CRN, "123", Optional.empty())).thenReturn(umsAccount);
        String endpointName = stack.getPrimaryGatewayInstance().getShortHostname();
        String commonName = "hashofshorthostname.anenvname.xcu2-8y8x.dev.cldr.work";
        when(domainNameProvider.getCommonName(endpointName, envName, accountWorkloadSubdomain)).thenReturn(commonName);
        String fqdn = endpointName + ".anenvname.xcu2-8y8x.dev.cldr.work";
        when(domainNameProvider.getFullyQualifiedEndpointName(endpointName, envName, accountWorkloadSubdomain)).thenReturn(fqdn);
        when(certificateCreationService.create(eq(USER_CRN), eq("123"), eq(endpointName), eq(envName), any(PKCS10CertificationRequest.class)))
                .thenReturn(List.of());
        when(dnsManagementService.createOrUpdateDnsEntryWithIp(anyString(), anyString(), anyString(), anyString(), anyBoolean(), any())).thenReturn(true);

        boolean result = ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.renewCertificate(stack));

        verify(environmentClientService, times(2)).getByCrn(anyString());
        verify(grpcUmsClient, times(2)).getAccountDetails(USER_CRN, "123", Optional.empty());
        verify(domainNameProvider, times(1)).getCommonName(endpointName, envName, accountWorkloadSubdomain);
        verify(domainNameProvider, times(2)).getFullyQualifiedEndpointName(endpointName, envName, accountWorkloadSubdomain);
        verify(certificateCreationService, times(1))
                .create(eq(USER_CRN), eq("123"), eq(endpointName), eq(envName), any(PKCS10CertificationRequest.class));
        verify(securityConfigService, times(1)).save(any(SecurityConfig.class));
        verify(dnsManagementService, times(1)).createOrUpdateDnsEntryWithIp(anyString(), anyString(), anyString(), anyString(),
                anyBoolean(), any());
        verify(clusterService, times(1)).save(cluster);
        Assertions.assertEquals(Boolean.TRUE, result);
    }

    @Test
    void testRenewCertificateWhenCertGenerationIsTriggerableAndDnsEntryShouldNotBeCreated() throws IOException {
        SecurityConfig securityConfig = new SecurityConfig();
        securityConfig.setUserFacingCert("CERT");
        securityConfig.setUserFacingKey(USER_FACING_PRIVATE_KEY);
        Cluster cluster = TestUtil.cluster();
        Stack stack = cluster.getStack();
        stack.setSecurityConfig(securityConfig);
        stack.setCluster(cluster);

        String envName = "anEnvName";
        DetailedEnvironmentResponse environment = DetailedEnvironmentResponse.Builder
                .builder()
                .withName(envName)
                .build();
        when(environmentClientService.getByCrn(Mockito.anyString())).thenReturn(environment);

        String accountWorkloadSubdomain = "aWorkloadSubdomain";
        UserManagementProto.Account umsAccount = UserManagementProto.Account.newBuilder()
                .setWorkloadSubdomain(accountWorkloadSubdomain)
                .build();
        when(grpcUmsClient.getAccountDetails(USER_CRN, "123", Optional.empty())).thenReturn(umsAccount);

        String endpointName = stack.getPrimaryGatewayInstance().getShortHostname();
        String commonName = "hashofshorthostname.anenvname.xcu2-8y8x.dev.cldr.work";
        when(domainNameProvider.getCommonName(endpointName, envName, accountWorkloadSubdomain)).thenReturn(commonName);

        String fqdn = endpointName + ".anenvname.xcu2-8y8x.dev.cldr.work";
        when(domainNameProvider.getFullyQualifiedEndpointName(endpointName, envName, accountWorkloadSubdomain)).thenReturn(fqdn);

        when(certificateCreationService.create(eq(USER_CRN), eq("123"), eq(endpointName), eq(envName), any(PKCS10CertificationRequest.class)))
                .thenReturn(List.of());
        cluster.setFqdn(fqdn);

        boolean result = ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.renewCertificate(stack));

        verify(environmentClientService, times(1)).getByCrn(anyString());
        verify(grpcUmsClient, times(1)).getAccountDetails(USER_CRN, "123", Optional.empty());
        verify(domainNameProvider, times(1)).getCommonName(endpointName, envName, accountWorkloadSubdomain);
        verify(domainNameProvider, times(1)).getFullyQualifiedEndpointName(endpointName, envName, accountWorkloadSubdomain);
        verify(certificateCreationService, times(1))
                .create(eq(USER_CRN), eq("123"), eq(endpointName), eq(envName), any(PKCS10CertificationRequest.class));
        verify(securityConfigService, times(1)).save(any(SecurityConfig.class));
        Assertions.assertEquals(Boolean.TRUE, result);
    }

    @Test
    void testGenerateCertAndSaveForStackAndUpdateDnsEntryWhenStackIsNull() throws IOException {
        Stack stack = null;

        boolean result = ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.generateCertAndSaveForStackAndUpdateDnsEntry(stack));

        Assertions.assertEquals(Boolean.FALSE, result);
    }

    @Test
    void testGenerateCertAndSaveForStackAndUpdateDnsEntryShouldUpdateDnsEntryWhenUserFacingCertHasAlreadyBeenGenerated() {
        SecurityConfig securityConfig = new SecurityConfig();
        securityConfig.setUserFacingKey(USER_FACING_PRIVATE_KEY);
        securityConfig.setUserFacingCert("CERT");
        Cluster cluster = TestUtil.cluster();
        Stack stack = cluster.getStack();
        stack.setSecurityConfig(securityConfig);
        stack.setCluster(cluster);
        InstanceMetaData primaryGatewayInstance = stack.getPrimaryGatewayInstance();
        String endpointName = primaryGatewayInstance.getShortHostname();
        String envName = "anEnvName";
        String accountWorkloadSubdomain = "aWorkloadSubdomain";
        String fqdn = endpointName + ".anenvname.xcu2-8y8x.dev.cldr.work";

        DetailedEnvironmentResponse environment = DetailedEnvironmentResponse.Builder
                .builder()
                .withName(envName)
                .build();
        when(environmentClientService.getByCrn(Mockito.anyString())).thenReturn(environment);
        UserManagementProto.Account umsAccount = UserManagementProto.Account.newBuilder()
                .setWorkloadSubdomain(accountWorkloadSubdomain)
                .build();
        when(grpcUmsClient.getAccountDetails(USER_CRN, "123", Optional.empty())).thenReturn(umsAccount);
        when(domainNameProvider.getFullyQualifiedEndpointName(endpointName, envName, accountWorkloadSubdomain)).thenReturn(fqdn);
        when(dnsManagementService.createOrUpdateDnsEntryWithIp(eq(USER_CRN), eq("123"), eq(endpointName), eq(envName), eq(Boolean.FALSE),
                eq(List.of(primaryGatewayInstance.getPublicIpWrapper())))).thenReturn(Boolean.TRUE);

        boolean result = ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.generateCertAndSaveForStackAndUpdateDnsEntry(stack));

        verify(environmentClientService, times(1)).getByCrn(anyString());
        verify(grpcUmsClient, times(1)).getAccountDetails(USER_CRN, "123", Optional.empty());
        verify(domainNameProvider, times(1)).getFullyQualifiedEndpointName(endpointName, envName, accountWorkloadSubdomain);
        verify(dnsManagementService, times(1))
                .createOrUpdateDnsEntryWithIp(eq(USER_CRN), eq("123"), eq(endpointName), eq(envName), eq(Boolean.FALSE),
                        eq(List.of(primaryGatewayInstance.getPublicIpWrapper())));
        verify(clusterService, times(1)).save(any(Cluster.class));
        Assertions.assertEquals(Boolean.FALSE, result);
    }

    @Test
    void testGenerateCertAndSaveForStackAndUpdateDnsEntryWhenUserFacingKeyNeedsToBeGenerated() throws IOException {
        SecurityConfig securityConfig = new SecurityConfig();
        Cluster cluster = TestUtil.cluster();
        Stack stack = cluster.getStack();
        stack.setSecurityConfig(securityConfig);
        stack.setCluster(cluster);

        InstanceMetaData primaryGatewayInstance = stack.getPrimaryGatewayInstance();
        String endpointName = primaryGatewayInstance.getShortHostname();
        String commonName = "hashofshorthostname.anenvname.xcu2-8y8x.dev.cldr.work";
        String fqdn = endpointName + ".anenvname.xcu2-8y8x.dev.cldr.work";
        String envName = "anEnvName";
        String accountWorkloadSubdomain = "aWorkloadSubdomain";

        DetailedEnvironmentResponse environment = DetailedEnvironmentResponse.Builder
                .builder()
                .withName(envName)
                .build();
        when(environmentClientService.getByCrn(Mockito.anyString())).thenReturn(environment);
        UserManagementProto.Account umsAccount = UserManagementProto.Account.newBuilder()
                .setWorkloadSubdomain(accountWorkloadSubdomain)
                .build();
        when(grpcUmsClient.getAccountDetails(USER_CRN, "123", Optional.empty())).thenReturn(umsAccount);
        when(domainNameProvider.getCommonName(endpointName, envName, accountWorkloadSubdomain)).thenReturn(commonName);
        when(domainNameProvider.getFullyQualifiedEndpointName(endpointName, envName, accountWorkloadSubdomain)).thenReturn(fqdn);
        when(certificateCreationService.create(eq(USER_CRN), eq("123"), eq(endpointName), eq(envName), any(PKCS10CertificationRequest.class)))
                .thenReturn(List.of());
        when(dnsManagementService.createOrUpdateDnsEntryWithIp(eq(USER_CRN), eq("123"), eq(endpointName), eq(envName), eq(Boolean.FALSE),
                eq(List.of(primaryGatewayInstance.getPublicIpWrapper())))).thenReturn(Boolean.TRUE);

        boolean result = ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.generateCertAndSaveForStackAndUpdateDnsEntry(stack));

        verify(environmentClientService, times(2)).getByCrn(anyString());
        verify(grpcUmsClient, times(2)).getAccountDetails(USER_CRN, "123", Optional.empty());
        verify(domainNameProvider, times(1)).getCommonName(endpointName, envName, accountWorkloadSubdomain);
        verify(domainNameProvider, times(2)).getFullyQualifiedEndpointName(endpointName, envName, accountWorkloadSubdomain);
        verify(certificateCreationService, times(1))
                .create(eq(USER_CRN), eq("123"), eq(endpointName), eq(envName), any(PKCS10CertificationRequest.class));
        verify(dnsManagementService, times(1))
                .createOrUpdateDnsEntryWithIp(eq(USER_CRN), eq("123"), eq(endpointName), eq(envName), eq(Boolean.FALSE),
                        eq(List.of(primaryGatewayInstance.getPublicIpWrapper())));
        verify(securityConfigService, times(2)).save(any(SecurityConfig.class));
        Assertions.assertEquals(Boolean.TRUE, result);
    }

    @Test
    void testGenerateCertAndSaveForStackAndUpdateDnsEntryWhenUserFacingKeyHasAlreadyBeenGenerated() throws IOException {
        SecurityConfig securityConfig = new SecurityConfig();
        securityConfig.setUserFacingKey(USER_FACING_PRIVATE_KEY);
        Cluster cluster = TestUtil.cluster();
        Stack stack = cluster.getStack();
        stack.setSecurityConfig(securityConfig);
        stack.setCluster(cluster);

        String envName = "anEnvName";
        DetailedEnvironmentResponse environment = DetailedEnvironmentResponse.Builder
                .builder()
                .withName(envName)
                .build();
        when(environmentClientService.getByCrn(Mockito.anyString())).thenReturn(environment);

        String accountWorkloadSubdomain = "aWorkloadSubdomain";
        UserManagementProto.Account umsAccount = UserManagementProto.Account.newBuilder()
                .setWorkloadSubdomain(accountWorkloadSubdomain)
                .build();
        when(grpcUmsClient.getAccountDetails(USER_CRN, "123", Optional.empty())).thenReturn(umsAccount);

        InstanceMetaData primaryGatewayInstance = stack.getPrimaryGatewayInstance();
        String endpointName = primaryGatewayInstance.getShortHostname();
        String commonName = "hashofshorthostname.anenvname.xcu2-8y8x.dev.cldr.work";
        String fqdn = endpointName + ".anenvname.xcu2-8y8x.dev.cldr.work";
        when(domainNameProvider.getCommonName(endpointName, envName, accountWorkloadSubdomain)).thenReturn(commonName);
        when(domainNameProvider.getFullyQualifiedEndpointName(endpointName, envName, accountWorkloadSubdomain)).thenReturn(fqdn);
        when(certificateCreationService.create(eq(USER_CRN), eq("123"), eq(endpointName), eq(envName), any(PKCS10CertificationRequest.class)))
                .thenReturn(List.of());
        when(dnsManagementService.createOrUpdateDnsEntryWithIp(eq(USER_CRN), eq("123"), eq(endpointName), eq(envName), eq(Boolean.FALSE),
                eq(List.of(primaryGatewayInstance.getPublicIpWrapper())))).thenReturn(Boolean.TRUE);

        boolean result = ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.generateCertAndSaveForStackAndUpdateDnsEntry(stack));

        verify(environmentClientService, times(2)).getByCrn(anyString());
        verify(grpcUmsClient, times(2)).getAccountDetails(USER_CRN, "123", Optional.empty());
        verify(domainNameProvider, times(1)).getCommonName(endpointName, envName, accountWorkloadSubdomain);
        verify(domainNameProvider, times(2)).getFullyQualifiedEndpointName(endpointName, envName, accountWorkloadSubdomain);
        verify(certificateCreationService, times(1))
                .create(eq(USER_CRN), eq("123"), eq(endpointName), eq(envName), any(PKCS10CertificationRequest.class));
        verify(dnsManagementService, times(1))
                .createOrUpdateDnsEntryWithIp(eq(USER_CRN), eq("123"), eq(endpointName), eq(envName), eq(Boolean.FALSE),
                        eq(List.of(primaryGatewayInstance.getPublicIpWrapper())));
        verify(securityConfigService, times(1)).save(any(SecurityConfig.class));
        Assertions.assertEquals(Boolean.TRUE, result);
    }

    @Test
    void testUpdateDnsEntryWhenEnvironmentServiceClientFails() {
        Cluster cluster = TestUtil.cluster();
        Stack stack = cluster.getStack();
        stack.setCluster(cluster);
        InstanceMetaData primaryGatewayInstance = stack.getPrimaryGatewayInstance();
        String endpointName = primaryGatewayInstance.getShortHostname();
        String envName = "anEnvName";
        DetailedEnvironmentResponse environment = DetailedEnvironmentResponse.Builder
                .builder()
                .withName(envName)
                .build();
        when(environmentClientService.getByCrn(Mockito.anyString())).thenReturn(environment);
        when(dnsManagementService.createOrUpdateDnsEntryWithIp(eq(USER_CRN), eq("123"), eq(endpointName), eq(envName), eq(Boolean.FALSE),
                eq(List.of(primaryGatewayInstance.getPublicIpWrapper())))).thenReturn(Boolean.FALSE);

        String result = ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.updateDnsEntry(stack, null));

        Assertions.assertNull(result);
    }

    @Test
    void testUpdateDnsEntryWhenUmsServiceCallFails() {
        Cluster cluster = TestUtil.cluster();
        Stack stack = cluster.getStack();
        stack.setCluster(cluster);
        InstanceMetaData primaryGatewayInstance = stack.getPrimaryGatewayInstance();
        String endpointName = primaryGatewayInstance.getShortHostname();
        String envName = "anEnvName";
        DetailedEnvironmentResponse environment = DetailedEnvironmentResponse.Builder
                .builder()
                .withName(envName)
                .build();
        when(environmentClientService.getByCrn(Mockito.anyString())).thenReturn(environment);
        when(dnsManagementService.createOrUpdateDnsEntryWithIp(eq(USER_CRN), eq("123"), eq(endpointName), eq(envName), eq(Boolean.FALSE),
                eq(List.of(primaryGatewayInstance.getPublicIpWrapper())))).thenReturn(Boolean.TRUE);
        when(grpcUmsClient.getAccountDetails(USER_CRN, "123", Optional.empty())).thenThrow(new RuntimeException());

        String result = ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.updateDnsEntry(stack, null));

        Assertions.assertNull(result);
    }

    @Test
    void testUpdateDnsEntryWhenPublicEndpointManagementServiceCallFails() {
        Cluster cluster = TestUtil.cluster();
        Stack stack = cluster.getStack();
        stack.setCluster(cluster);
        InstanceMetaData primaryGatewayInstance = stack.getPrimaryGatewayInstance();
        String endpointName = primaryGatewayInstance.getShortHostname();
        String envName = "anEnvName";
        DetailedEnvironmentResponse environment = DetailedEnvironmentResponse.Builder
                .builder()
                .withName(envName)
                .build();
        when(environmentClientService.getByCrn(Mockito.anyString())).thenReturn(environment);
        when(dnsManagementService.createOrUpdateDnsEntryWithIp(eq(USER_CRN), eq("123"), eq(endpointName), eq(envName), eq(Boolean.FALSE),
                eq(List.of(primaryGatewayInstance.getPublicIpWrapper())))).thenReturn(Boolean.TRUE);
        String accountWorkloadSubdomain = "aWorkloadSubdomain";
        UserManagementProto.Account umsAccount = UserManagementProto.Account.newBuilder()
                .setWorkloadSubdomain(accountWorkloadSubdomain)
                .build();
        when(grpcUmsClient.getAccountDetails(USER_CRN, "123", Optional.empty())).thenReturn(umsAccount);
        when(domainNameProvider.getFullyQualifiedEndpointName(endpointName, envName, accountWorkloadSubdomain)).thenThrow(new IllegalStateException());

        String result = ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.updateDnsEntry(stack, null));

        Assertions.assertNull(result);
    }

    @Test
    void testUpdateDnsEntryShouldReturnWithFqdnInCaseOfSuccess() {
        Cluster cluster = TestUtil.cluster();
        Stack stack = cluster.getStack();
        stack.setCluster(cluster);
        InstanceMetaData primaryGatewayInstance = stack.getPrimaryGatewayInstance();
        String endpointName = primaryGatewayInstance.getShortHostname();
        String envName = "anEnvName";
        DetailedEnvironmentResponse environment = DetailedEnvironmentResponse.Builder
                .builder()
                .withName(envName)
                .build();
        when(environmentClientService.getByCrn(Mockito.anyString())).thenReturn(environment);
        when(dnsManagementService.createOrUpdateDnsEntryWithIp(eq(USER_CRN), eq("123"), eq(endpointName), eq(envName), eq(Boolean.FALSE),
                eq(List.of(primaryGatewayInstance.getPublicIpWrapper())))).thenReturn(Boolean.TRUE);
        String accountWorkloadSubdomain = "aWorkloadSubdomain";
        UserManagementProto.Account umsAccount = UserManagementProto.Account.newBuilder()
                .setWorkloadSubdomain(accountWorkloadSubdomain)
                .build();
        when(grpcUmsClient.getAccountDetails(USER_CRN, "123", Optional.empty())).thenReturn(umsAccount);
        String fqdn = endpointName + ".anenvname.xcu2-8y8x.dev.cldr.work";
        when(domainNameProvider.getFullyQualifiedEndpointName(endpointName, envName, accountWorkloadSubdomain)).thenReturn(fqdn);

        String result = ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.updateDnsEntry(stack, null));

        Assertions.assertNotNull(result);
        Assertions.assertEquals(fqdn, result);
        verify(dnsManagementService, times(1))
                .createOrUpdateDnsEntryWithIp(eq(USER_CRN), eq("123"), eq(endpointName), eq(envName), eq(Boolean.FALSE),
                        eq(List.of(primaryGatewayInstance.getPublicIpWrapper())));
    }

    @Test
    void testUpdateDnsEntryShouldReturnWithFqdnInCaseOfSuccessAndSetEntryToGatewayIpWhenGatewayIpIsSpecified() {
        String gatewayIp = "10.191.192.193";
        Cluster cluster = TestUtil.cluster();
        Stack stack = cluster.getStack();
        stack.setCluster(cluster);
        InstanceMetaData primaryGatewayInstance = stack.getPrimaryGatewayInstance();
        String endpointName = primaryGatewayInstance.getShortHostname();
        String envName = "anEnvName";
        DetailedEnvironmentResponse environment = DetailedEnvironmentResponse.Builder
                .builder()
                .withName(envName)
                .build();
        when(environmentClientService.getByCrn(Mockito.anyString())).thenReturn(environment);
        when(dnsManagementService.createOrUpdateDnsEntryWithIp(eq(USER_CRN), eq("123"), eq(endpointName), eq(envName), eq(Boolean.FALSE),
                eq(List.of(gatewayIp)))).thenReturn(Boolean.TRUE);
        String accountWorkloadSubdomain = "aWorkloadSubdomain";
        UserManagementProto.Account umsAccount = UserManagementProto.Account.newBuilder()
                .setWorkloadSubdomain(accountWorkloadSubdomain)
                .build();
        when(grpcUmsClient.getAccountDetails(USER_CRN, "123", Optional.empty())).thenReturn(umsAccount);
        String fqdn = endpointName + ".anenvname.xcu2-8y8x.dev.cldr.work";
        when(domainNameProvider.getFullyQualifiedEndpointName(endpointName, envName, accountWorkloadSubdomain)).thenReturn(fqdn);

        String result = ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.updateDnsEntry(stack, gatewayIp));

        Assertions.assertNotNull(result);
        Assertions.assertEquals(fqdn, result);
        verify(dnsManagementService, times(1))
                .createOrUpdateDnsEntryWithIp(eq(USER_CRN), eq("123"), eq(endpointName), eq(envName), eq(Boolean.FALSE),
                        eq(List.of(gatewayIp)));
    }

    @Test
    void testUpdateDnsEntryShouldCallDnsManagementServiceWithGatewayInstanceShortHostnameAsEndpointName() {
        String gatewayIp = "10.191.192.193";
        Cluster cluster = TestUtil.cluster();
        Stack stack = cluster.getStack();
        stack.setCluster(cluster);
        InstanceMetaData primaryGatewayInstance = stack.getPrimaryGatewayInstance();
        String endpointName = primaryGatewayInstance.getShortHostname();
        String envName = "anEnvName";
        DetailedEnvironmentResponse environment = DetailedEnvironmentResponse.Builder
                .builder()
                .withName(envName)
                .build();
        when(environmentClientService.getByCrn(Mockito.anyString())).thenReturn(environment);
        when(dnsManagementService.createOrUpdateDnsEntryWithIp(eq(USER_CRN), eq("123"), eq(endpointName), eq(envName), eq(Boolean.FALSE),
                eq(List.of(gatewayIp)))).thenReturn(Boolean.TRUE);
        String accountWorkloadSubdomain = "aWorkloadSubdomain";
        UserManagementProto.Account umsAccount = UserManagementProto.Account.newBuilder()
                .setWorkloadSubdomain(accountWorkloadSubdomain)
                .build();
        when(grpcUmsClient.getAccountDetails(USER_CRN, "123", Optional.empty())).thenReturn(umsAccount);
        String fqdn = endpointName + ".anenvname.xcu2-8y8x.dev.cldr.work";
        when(domainNameProvider.getFullyQualifiedEndpointName(endpointName, envName, accountWorkloadSubdomain)).thenReturn(fqdn);

        String result = ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.updateDnsEntry(stack, gatewayIp));

        Assertions.assertNotNull(result);
        Assertions.assertEquals(fqdn, result);
        verify(dnsManagementService, times(1))
                .createOrUpdateDnsEntryWithIp(eq(USER_CRN), eq("123"), eq(endpointName), eq(envName), eq(Boolean.FALSE),
                        eq(List.of(gatewayIp)));
    }

    @Test
    void testDeleteDnsEntryShouldCallDnsManagementServiceWithGatewayInstanceShortHostnameAsEndpointNameWhenEnvNameIsSpecified() {
        Cluster cluster = TestUtil.cluster();
        Stack stack = cluster.getStack();
        stack.setCluster(cluster);
        InstanceMetaData primaryGatewayInstance = stack.getPrimaryGatewayInstance();
        String endpointName = primaryGatewayInstance.getShortHostname();
        String gatewayIp = primaryGatewayInstance.getPublicIpWrapper();
        String envName = "anEnvName";
        when(dnsManagementService.deleteDnsEntryWithIp(eq(USER_CRN), eq("123"), eq(endpointName), eq(envName), eq(Boolean.FALSE),
                eq(List.of(gatewayIp)))).thenReturn(Boolean.TRUE);

        String result = ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.deleteDnsEntry(stack, envName));

        Assertions.assertNotNull(result);
        Assertions.assertEquals(gatewayIp, result);
        verify(dnsManagementService, times(1))
                .deleteDnsEntryWithIp(eq(USER_CRN), eq("123"), eq(endpointName), eq(envName), eq(Boolean.FALSE),
                        eq(List.of(gatewayIp)));
    }

    @Test
    void testDeleteDnsEntryShouldCallDnsManagementServiceWithGatewayInstanceShortHostnameAsEndpointNameWhenEnvNameIsNotSpecified() {
        Cluster cluster = TestUtil.cluster();
        Stack stack = cluster.getStack();
        stack.setCluster(cluster);
        InstanceMetaData primaryGatewayInstance = stack.getPrimaryGatewayInstance();
        String endpointName = primaryGatewayInstance.getShortHostname();
        String gatewayIp = primaryGatewayInstance.getPublicIpWrapper();
        String envName = "anEnvName";
        DetailedEnvironmentResponse environment = DetailedEnvironmentResponse.Builder
                .builder()
                .withName(envName)
                .build();
        when(environmentClientService.getByCrn(Mockito.anyString())).thenReturn(environment);
        when(dnsManagementService.deleteDnsEntryWithIp(eq(USER_CRN), eq("123"), eq(endpointName), eq(envName), eq(Boolean.FALSE),
                eq(List.of(gatewayIp)))).thenReturn(Boolean.TRUE);

        String result = ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.deleteDnsEntry(stack, null));

        Assertions.assertNotNull(result);
        Assertions.assertEquals(gatewayIp, result);
        verify(dnsManagementService, times(1))
                .deleteDnsEntryWithIp(eq(USER_CRN), eq("123"), eq(endpointName), eq(envName), eq(Boolean.FALSE),
                        eq(List.of(gatewayIp)));
    }

    @Test
    void testGenerateCertAndSaveForStackWithLoadBalancerWithCloudDns() throws IOException {
        SecurityConfig securityConfig = new SecurityConfig();
        Cluster cluster = TestUtil.cluster();
        LoadBalancer loadBalancer = new LoadBalancer();
        loadBalancer.setEndpoint("gateway");
        loadBalancer.setDns("http://cloud.dns");
        loadBalancer.setHostedZoneId("1");
        Stack stack = cluster.getStack();
        stack.setSecurityConfig(securityConfig);
        stack.setCluster(cluster);
        stack.setLoadBalancers(Set.of(loadBalancer));

        InstanceMetaData primaryGatewayInstance = stack.getPrimaryGatewayInstance();
        String endpointName = primaryGatewayInstance.getShortHostname();
        String lbEndpointName = loadBalancer.getEndpoint();
        String commonName = "hashofshorthostname.anenvname.xcu2-8y8x.dev.cldr.work";
        String fqdn = endpointName + ".anenvname.xcu2-8y8x.dev.cldr.work";
        String lbfqdn = lbEndpointName + ".anenvname.xcu2-8y8x.dev.cldr.work";
        String envName = "anEnvName";
        String accountWorkloadSubdomain = "aWorkloadSubdomain";
        String cloudDns = loadBalancer.getDns();
        String hostedZoneId = loadBalancer.getHostedZoneId();

        DetailedEnvironmentResponse environment = DetailedEnvironmentResponse.Builder
            .builder()
            .withName(envName)
            .build();
        when(environmentClientService.getByCrn(Mockito.anyString())).thenReturn(environment);
        UserManagementProto.Account umsAccount = UserManagementProto.Account.newBuilder()
            .setWorkloadSubdomain(accountWorkloadSubdomain)
            .build();
        when(grpcUmsClient.getAccountDetails(USER_CRN, "123", Optional.empty())).thenReturn(umsAccount);
        when(domainNameProvider.getCommonName(endpointName, envName, accountWorkloadSubdomain)).thenReturn(commonName);
        when(domainNameProvider.getFullyQualifiedEndpointName(endpointName, envName, accountWorkloadSubdomain)).thenReturn(fqdn);
        when(domainNameProvider.getFullyQualifiedEndpointName(lbEndpointName, envName, accountWorkloadSubdomain)).thenReturn(lbfqdn);
        when(certificateCreationService.create(eq(USER_CRN), eq("123"), eq(endpointName), eq(envName), any(PKCS10CertificationRequest.class)))
            .thenReturn(List.of());
        when(dnsManagementService.createOrUpdateDnsEntryWithIp(eq(USER_CRN), eq("123"), eq(endpointName), eq(envName), eq(Boolean.FALSE),
            eq(List.of(primaryGatewayInstance.getPublicIpWrapper())))).thenReturn(Boolean.TRUE);
        when(dnsManagementService.createOrUpdateDnsEntryWithCloudDns(eq(USER_CRN), eq("123"), eq(lbEndpointName), eq(envName), eq(cloudDns),
            eq(hostedZoneId))).thenReturn(Boolean.TRUE);
        when(loadBalancerService.findByStackId(anyLong())).thenReturn(Set.of(loadBalancer));

        boolean result = ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.generateCertAndSaveForStackAndUpdateDnsEntry(stack));

        verify(environmentClientService, times(3)).getByCrn(anyString());
        verify(grpcUmsClient, times(2)).getAccountDetails(USER_CRN, "123", Optional.empty());
        verify(domainNameProvider, times(1)).getCommonName(endpointName, envName, accountWorkloadSubdomain);
        verify(domainNameProvider, times(2)).getFullyQualifiedEndpointName(endpointName, envName, accountWorkloadSubdomain);
        verify(certificateCreationService, times(1))
            .create(eq(USER_CRN), eq("123"), eq(endpointName), eq(envName), any(PKCS10CertificationRequest.class));
        verify(dnsManagementService, times(1))
            .createOrUpdateDnsEntryWithIp(eq(USER_CRN), eq("123"), eq(endpointName), eq(envName), eq(Boolean.FALSE),
                eq(List.of(primaryGatewayInstance.getPublicIpWrapper())));
        verify(dnsManagementService, times(1))
            .createOrUpdateDnsEntryWithCloudDns(eq(USER_CRN), eq("123"), eq(lbEndpointName), eq(envName), eq(cloudDns),
                eq(hostedZoneId));
        verify(securityConfigService, times(2)).save(any(SecurityConfig.class));
        Assertions.assertEquals(Boolean.TRUE, result);
    }

    @Test
    void testGenerateCertAndSaveForStackWithLoadBalancerWithIpAddress() throws IOException {
        SecurityConfig securityConfig = new SecurityConfig();
        Cluster cluster = TestUtil.cluster();
        LoadBalancer loadBalancer = new LoadBalancer();
        loadBalancer.setEndpoint("gateway");
        loadBalancer.setIp("1.2.3.4");
        Stack stack = cluster.getStack();
        stack.setSecurityConfig(securityConfig);
        stack.setCluster(cluster);
        stack.setLoadBalancers(Set.of(loadBalancer));

        InstanceMetaData primaryGatewayInstance = stack.getPrimaryGatewayInstance();
        String endpointName = primaryGatewayInstance.getShortHostname();
        String lbEndpointName = loadBalancer.getEndpoint();
        String commonName = "hashofshorthostname.anenvname.xcu2-8y8x.dev.cldr.work";
        String fqdn = endpointName + ".anenvname.xcu2-8y8x.dev.cldr.work";
        String lbfqdn = lbEndpointName + ".anenvname.xcu2-8y8x.dev.cldr.work";
        String envName = "anEnvName";
        String accountWorkloadSubdomain = "aWorkloadSubdomain";
        String lbIp = loadBalancer.getIp();

        DetailedEnvironmentResponse environment = DetailedEnvironmentResponse.Builder
            .builder()
            .withName(envName)
            .build();
        when(environmentClientService.getByCrn(Mockito.anyString())).thenReturn(environment);
        UserManagementProto.Account umsAccount = UserManagementProto.Account.newBuilder()
            .setWorkloadSubdomain(accountWorkloadSubdomain)
            .build();
        when(grpcUmsClient.getAccountDetails(USER_CRN, "123", Optional.empty())).thenReturn(umsAccount);
        when(domainNameProvider.getCommonName(endpointName, envName, accountWorkloadSubdomain)).thenReturn(commonName);
        when(domainNameProvider.getFullyQualifiedEndpointName(endpointName, envName, accountWorkloadSubdomain)).thenReturn(fqdn);
        when(domainNameProvider.getFullyQualifiedEndpointName(lbEndpointName, envName, accountWorkloadSubdomain)).thenReturn(lbfqdn);
        when(certificateCreationService.create(eq(USER_CRN), eq("123"), eq(endpointName), eq(envName), any(PKCS10CertificationRequest.class)))
            .thenReturn(List.of());
        when(dnsManagementService.createOrUpdateDnsEntryWithIp(eq(USER_CRN), eq("123"), eq(endpointName), eq(envName), eq(Boolean.FALSE),
            eq(List.of(primaryGatewayInstance.getPublicIpWrapper())))).thenReturn(Boolean.TRUE);
        when(dnsManagementService.createOrUpdateDnsEntryWithIp(eq(USER_CRN), eq("123"), eq(lbEndpointName), eq(envName), eq(Boolean.FALSE),
            eq(List.of(lbIp)))).thenReturn(Boolean.TRUE);
        when(loadBalancerService.findByStackId(anyLong())).thenReturn(Set.of(loadBalancer));

        boolean result = ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.generateCertAndSaveForStackAndUpdateDnsEntry(stack));

        verify(environmentClientService, times(3)).getByCrn(anyString());
        verify(grpcUmsClient, times(2)).getAccountDetails(USER_CRN, "123", Optional.empty());
        verify(domainNameProvider, times(1)).getCommonName(endpointName, envName, accountWorkloadSubdomain);
        verify(domainNameProvider, times(2)).getFullyQualifiedEndpointName(endpointName, envName, accountWorkloadSubdomain);
        verify(certificateCreationService, times(1))
            .create(eq(USER_CRN), eq("123"), eq(endpointName), eq(envName), any(PKCS10CertificationRequest.class));
        verify(dnsManagementService, times(1))
            .createOrUpdateDnsEntryWithIp(eq(USER_CRN), eq("123"), eq(endpointName), eq(envName), eq(Boolean.FALSE),
                eq(List.of(primaryGatewayInstance.getPublicIpWrapper())));
        verify(dnsManagementService, times(1))
            .createOrUpdateDnsEntryWithIp(eq(USER_CRN), eq("123"), eq(lbEndpointName), eq(envName), eq(Boolean.FALSE),
                eq(List.of(lbIp)));
        verify(securityConfigService, times(2)).save(any(SecurityConfig.class));
        Assertions.assertEquals(Boolean.TRUE, result);
    }

    @Test
    void testDeleteDnsEntryForLoadBalancerWithCloudDns() {
        Cluster cluster = TestUtil.cluster();
        LoadBalancer loadBalancer = new LoadBalancer();
        loadBalancer.setEndpoint("gateway");
        loadBalancer.setDns("http://cloud.dns");
        loadBalancer.setHostedZoneId("1");
        Stack stack = cluster.getStack();
        stack.setCluster(cluster);
        stack.setLoadBalancers(Set.of(loadBalancer));

        String lbEndpointName = loadBalancer.getEndpoint();
        String envName = "anEnvName";
        String cloudDns = loadBalancer.getDns();
        String hostedZoneId = loadBalancer.getHostedZoneId();

        when(dnsManagementService.deleteDnsEntryWithCloudDns(eq(USER_CRN), eq("123"), eq(lbEndpointName), eq(envName), eq(cloudDns),
            eq(hostedZoneId))).thenReturn(Boolean.TRUE);
        when(loadBalancerService.findByStackId(anyLong())).thenReturn(Set.of(loadBalancer));

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.deleteLoadBalancerDnsEntry(stack, envName));

        verify(dnsManagementService, times(1))
            .deleteDnsEntryWithCloudDns(eq(USER_CRN), eq("123"), eq(lbEndpointName), eq(envName), eq(cloudDns),
                eq(hostedZoneId));
    }

    @Test
    void testDeleteDnsEntryForLoadBalancerWithIpAddress() {
        Cluster cluster = TestUtil.cluster();
        LoadBalancer loadBalancer = new LoadBalancer();
        loadBalancer.setEndpoint("gateway");
        loadBalancer.setIp("1.2.3.4");
        Stack stack = cluster.getStack();
        stack.setCluster(cluster);
        stack.setLoadBalancers(Set.of(loadBalancer));

        String lbEndpointName = loadBalancer.getEndpoint();
        String envName = "anEnvName";
        String ip = loadBalancer.getIp();

        when(dnsManagementService.deleteDnsEntryWithIp(eq(USER_CRN), eq("123"), eq(lbEndpointName), eq(envName), eq(Boolean.FALSE),
            eq(List.of(ip)))).thenReturn(Boolean.TRUE);
        when(loadBalancerService.findByStackId(anyLong())).thenReturn(Set.of(loadBalancer));

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.deleteLoadBalancerDnsEntry(stack, envName));

        verify(dnsManagementService, times(1))
            .deleteDnsEntryWithIp(eq(USER_CRN), eq("123"), eq(lbEndpointName), eq(envName), eq(Boolean.FALSE),
                eq(List.of(ip)));
    }
}