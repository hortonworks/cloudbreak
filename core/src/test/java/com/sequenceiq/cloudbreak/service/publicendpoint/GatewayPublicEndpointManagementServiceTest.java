package com.sequenceiq.cloudbreak.service.publicendpoint;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.UPDATE_IN_PROGRESS;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.STACK_LB_REGISTER_PUBLIC_DNS_FAILED;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.security.Security;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.bouncycastle.asn1.ASN1OctetString;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.jcajce.provider.BouncyCastleFipsProvider;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.PemDnsEntryCreateOrUpdateException;
import com.sequenceiq.cloudbreak.TestUtil;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.certificate.service.CertificateCreationService;
import com.sequenceiq.cloudbreak.certificate.service.DnsManagementService;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerRepo;
import com.sequenceiq.cloudbreak.cluster.service.ClusterComponentConfigProvider;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.domain.SecurityConfig;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.stack.loadbalancer.LoadBalancer;
import com.sequenceiq.cloudbreak.message.FlowMessageService;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.environment.EnvironmentService;
import com.sequenceiq.cloudbreak.service.loadbalancer.LoadBalancerConfigService;
import com.sequenceiq.cloudbreak.service.securityconfig.SecurityConfigService;
import com.sequenceiq.cloudbreak.service.stack.LoadBalancerPersistenceService;
import com.sequenceiq.cloudbreak.view.InstanceMetadataView;
import com.sequenceiq.common.api.type.LoadBalancerType;
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
    private EnvironmentService environmentClientService;

    @Mock
    private SecurityConfigService securityConfigService;

    @Mock
    private ClusterService clusterService;

    @Mock
    private DnsManagementService dnsManagementService;

    @Mock
    private EnvironmentBasedDomainNameProvider domainNameProvider;

    @Mock
    private CertificateCreationService certificateCreationService;

    @Mock
    private LoadBalancerPersistenceService loadBalancerPersistenceService;

    @Mock
    private LoadBalancerConfigService loadBalancerConfigService;

    @Mock
    private TransactionService transactionService;

    @Mock
    private FlowMessageService flowMessageService;

    @Mock
    private EntitlementService entitlementService;

    @Mock
    private ClusterComponentConfigProvider clusterComponentConfigProvider;

    @InjectMocks
    private GatewayPublicEndpointManagementService underTest;

    @BeforeEach
    void setUp() throws TransactionService.TransactionExecutionException {
        ReflectionTestUtils.setField(underTest, "certGenerationEnabled", Boolean.TRUE);
        ReflectionTestUtils.setField(underTest, "certGenerationEnabledForMock", Boolean.TRUE);
        lenient().when(transactionService.required(any(Supplier.class))).then(invocationOnMock -> ((Supplier) invocationOnMock.getArgument(0)).get());
    }

    @Test
    void testRenewCertificateWhenCertGenerationFeatureIsDisabled() {
        ReflectionTestUtils.setField(underTest, "certGenerationEnabled", Boolean.FALSE);

        underTest.renewCertificate(null);

        verify(environmentClientService, times(0)).getByCrn(Mockito.anyString());
    }

    @Test
    void testRenewCertificateWhenCertGenerationFeatureIsEnabledButClusterDoesNotHaveUserFacingCert() {
        Stack stack = TestUtil.stack();

        underTest.renewCertificate(stack);

        verify(environmentClientService, times(0)).getByCrn(Mockito.anyString());
    }

    @Test
    void testRenewCertificateWhenCertGenerationIsTriggerableButGenerationFails() throws PemDnsEntryCreateOrUpdateException {
        SecurityConfig securityConfig = new SecurityConfig();
        securityConfig.setUserFacingCert("CERT");
        securityConfig.setUserFacingKey(USER_FACING_PRIVATE_KEY);
        Cluster cluster = TestUtil.cluster();
        Stack stack = cluster.getStack();
        stack.setSecurityConfig(securityConfig);
        stack.setCluster(cluster);
        when(environmentClientService.getByCrn(Mockito.anyString())).thenThrow(new RuntimeException("any error"));

        CloudbreakServiceException actual = assertThrows(CloudbreakServiceException.class,
                () -> ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.renewCertificate(stack)));

        assertEquals("The public certificate could not be generated by Public Endpoint Management service: any error", actual.getMessage());
        verify(environmentClientService, times(1)).getByCrn(anyString());
        verify(dnsManagementService, times(0)).createOrUpdateDnsEntryWithIp(anyString(), anyString(), anyString(),
                anyBoolean(), any());
        verify(clusterService, times(0)).save(cluster);
        verifyNoMoreInteractions(environmentClientService);
        verifyNoMoreInteractions(domainNameProvider);
        verifyNoMoreInteractions(dnsManagementService);
        verifyNoMoreInteractions(securityConfigService);
    }

    @Test
    void testRenewCertificateWhenCertGenerationIsTriggerableAndDnsEntryShouldBeCreated() throws IOException, TransactionService.TransactionExecutionException,
            PemDnsEntryCreateOrUpdateException {
        doAnswer((invocation) -> {
            ((Runnable) invocation.getArgument(0)).run();
            return null;
        }).when(transactionService).required(any(Runnable.class));

        SecurityConfig securityConfig = new SecurityConfig();
        securityConfig.setUserFacingCert("CERT");
        securityConfig.setUserFacingKey(USER_FACING_PRIVATE_KEY);
        Cluster cluster = TestUtil.cluster();
        Stack stack = cluster.getStack();
        stack.setSecurityConfig(securityConfig);
        stack.setCluster(cluster);

        String envName = "anEnvName";
        String environmentDomain = "anenvname.xcu2-8y8x.dev.cldr.work";
        DetailedEnvironmentResponse environment = DetailedEnvironmentResponse.builder()
                .withName(envName)
                .withEnvironmentDomain(environmentDomain)
                .build();
        when(environmentClientService.getByCrn(Mockito.anyString())).thenReturn(environment);
        String endpointName = stack.getPrimaryGatewayInstance().getShortHostname();
        String commonName = "hashofshorthostname.anenvname.xcu2-8y8x.dev.cldr.work";
        when(domainNameProvider.getCommonName(endpointName, environment)).thenReturn(commonName);
        String fqdn = endpointName + "." + environmentDomain;
        when(domainNameProvider.getFullyQualifiedEndpointName(Set.of(), endpointName, environment)).thenReturn(fqdn);
        when(certificateCreationService.create(eq("123"), eq(endpointName), eq(envName), any(PKCS10CertificationRequest.class),
                eq(stack.getResourceCrn()))).thenReturn(List.of("aCertificate"));

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.renewCertificate(stack));

        verify(environmentClientService, times(2)).getByCrn(anyString());
        verify(domainNameProvider, times(1)).getCommonName(endpointName, environment);
        verify(domainNameProvider, times(2)).getFullyQualifiedEndpointName(Set.of(), endpointName, environment);
        verify(certificateCreationService, times(1))
                .create(eq("123"), eq(endpointName), eq(envName), any(PKCS10CertificationRequest.class), eq(stack.getResourceCrn()));
        verify(securityConfigService, times(1)).save(any(SecurityConfig.class));
        verify(dnsManagementService, times(1)).createOrUpdateDnsEntryWithIp(anyString(), anyString(), anyString(),
                anyBoolean(), any());
        verify(clusterService, times(1)).updateFqdnOnCluster(cluster.getId(), fqdn);
    }

    @Test
    void testRenewCertificateWhenCertGenerationIsTriggerableAndDnsEntryUpdateFails() throws IOException, PemDnsEntryCreateOrUpdateException {
        SecurityConfig securityConfig = new SecurityConfig();
        securityConfig.setUserFacingCert("CERT");
        securityConfig.setUserFacingKey(USER_FACING_PRIVATE_KEY);
        Cluster cluster = TestUtil.cluster();
        Stack stack = cluster.getStack();
        stack.setSecurityConfig(securityConfig);
        stack.setCluster(cluster);

        String envName = "anEnvName";
        String environmentDomain = "anenvname.xcu2-8y8x.dev.cldr.work";
        DetailedEnvironmentResponse environment = DetailedEnvironmentResponse.builder()
                .withName(envName)
                .withEnvironmentDomain(environmentDomain)
                .build();
        when(environmentClientService.getByCrn(Mockito.anyString())).thenReturn(environment);

        String endpointName = stack.getPrimaryGatewayInstance().getShortHostname();
        String commonName = "hashofshorthostname." + environmentDomain;
        when(domainNameProvider.getCommonName(endpointName, environment)).thenReturn(commonName);

        String fqdn = endpointName + "." + environmentDomain;
        when(domainNameProvider.getFullyQualifiedEndpointName(Set.of(), endpointName, environment)).thenReturn(fqdn);

        when(certificateCreationService.create(eq("123"), eq(endpointName), eq(envName), any(PKCS10CertificationRequest.class),
                eq(stack.getResourceCrn()))).thenReturn(List.of("aCertificate"));
        cluster.setFqdn(fqdn);
        doThrow(new PemDnsEntryCreateOrUpdateException("Uh-Oh"))
                .when(dnsManagementService).createOrUpdateDnsEntryWithIp(any(), anyString(), anyString(), anyBoolean(), anyList());

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> {
            CloudbreakServiceException actual = assertThrows(CloudbreakServiceException.class, () -> underTest.renewCertificate(stack));
            assertEquals("Failed to create or update DNS entry for endpoint 'test-is1-1-1' and environment name 'anEnvName' and IP: '2.2.1.1'",
                    actual.getMessage());
        });

        verify(environmentClientService, times(2)).getByCrn(anyString());
        verify(domainNameProvider, times(1)).getCommonName(endpointName, environment);
        verify(domainNameProvider, times(1)).getFullyQualifiedEndpointName(Set.of(), endpointName, environment);
        verify(certificateCreationService, times(1))
                .create(eq("123"), eq(endpointName), eq(envName), any(PKCS10CertificationRequest.class), eq(stack.getResourceCrn()));
        verify(securityConfigService, times(1)).save(any(SecurityConfig.class));
    }

    @Test
    void testGenerateCertAndSaveForStackAndUpdateDnsEntryWhenStackIsNull() throws IOException {
        Stack stack = null;

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> assertDoesNotThrow(() -> underTest.generateCertAndSaveForStackAndUpdateDnsEntry(stack)));
    }

    @Test
    void testGenerateCertAndSaveForStackAndUpdateDnsEntryWhenUserFacingKeyNeedsToBeGenerated() throws IOException, PemDnsEntryCreateOrUpdateException {
        SecurityConfig securityConfig = new SecurityConfig();
        Cluster cluster = TestUtil.cluster();
        Stack stack = cluster.getStack();
        stack.setSecurityConfig(securityConfig);
        stack.setCluster(cluster);

        String environmentDomain = "anenvname.xcu2-8y8x.dev.cldr.work";
        String envName = "anEnvName";
        DetailedEnvironmentResponse environment = DetailedEnvironmentResponse.builder()
                .withName(envName)
                .withEnvironmentDomain(environmentDomain)
                .build();
        when(environmentClientService.getByCrn(Mockito.anyString())).thenReturn(environment);

        InstanceMetadataView primaryGatewayInstance = stack.getPrimaryGatewayInstance();
        String primaryGatewayEndpointName = primaryGatewayInstance.getShortHostname();
        String commonName = "hashofshorthostname." + environmentDomain;
        when(domainNameProvider.getCommonName(primaryGatewayEndpointName, environment)).thenReturn(commonName);
        String primaryGatewayFQDNEndpointName = primaryGatewayEndpointName + "." + environmentDomain;
        when(domainNameProvider.getFullyQualifiedEndpointName(Set.of(), primaryGatewayEndpointName, environment)).thenReturn(primaryGatewayFQDNEndpointName);
        stack.getGatewayHostGroup().ifPresent(group -> {
            InstanceMetaData instanceMetaData = new InstanceMetaData();
            instanceMetaData.setInstanceGroup(group);
            instanceMetaData.setInstanceGroupId(group.getId());
            instanceMetaData.setDiscoveryFQDN("nonprimarygateway.cldr.work");
            group.getInstanceMetaData().add(instanceMetaData);
        });
        when(domainNameProvider.getFullyQualifiedEndpointName(Set.of(), "nonprimarygateway", environment))
                .thenReturn("nonprimarygateway." + environmentDomain);

        when(certificateCreationService.create(eq("123"), eq(primaryGatewayEndpointName), eq(envName), any(PKCS10CertificationRequest.class),
                eq(stack.getResourceCrn()))).thenReturn(List.of("aCertificate"));
        ClouderaManagerRepo clouderaManagerRepo = mock(ClouderaManagerRepo.class);
        when(entitlementService.isConfigureEncryptionProfileEnabled(anyString())).thenReturn(false);

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.generateCertAndSaveForStackAndUpdateDnsEntry(stack));

        ArgumentCaptor<PKCS10CertificationRequest> csrCaptor = ArgumentCaptor.forClass(PKCS10CertificationRequest.class);
        verify(environmentClientService, times(2)).getByCrn(anyString());
        verify(domainNameProvider, times(1)).getCommonName(primaryGatewayEndpointName, environment);
        verify(domainNameProvider, times(3)).getFullyQualifiedEndpointName(eq(Set.of()), anyString(), eq(environment));
        verify(certificateCreationService, times(1))
                .create(eq("123"), eq(primaryGatewayEndpointName), eq(envName), csrCaptor.capture(), eq(stack.getResourceCrn()));
        verify(dnsManagementService, times(1))
                .createOrUpdateDnsEntryWithIp(eq("123"), eq(primaryGatewayEndpointName), eq(envName), eq(Boolean.FALSE),
                        eq(List.of(primaryGatewayInstance.getPublicIpWrapper())));
        verify(securityConfigService, times(1)).save(any(SecurityConfig.class));
        Set<String> expectedSANs = Set.of("hashofshorthostname.anenvname.xcu2-8y8x.dev.cldr.work", primaryGatewayFQDNEndpointName,
                "nonprimarygateway.anenvname.xcu2-8y8x.dev.cldr.work");
        csrCaptor.getAllValues().forEach(csr -> {
            ASN1OctetString asn1OctetString = csr.getRequestedExtensions()
                    .getExtension(Extension.subjectAlternativeName).getExtnValue();
            Set<String> actualSANs = Arrays.stream(GeneralNames.getInstance(asn1OctetString.getOctets()).getNames())
                    .map(generalName -> generalName.getName().toString())
                    .collect(Collectors.toSet());
            assertTrue(actualSANs.containsAll(expectedSANs));
        });
    }

    @Test
    void testGenerateCertAndSaveForStackAndUpdateDnsEntryWhenCertGenerationFails() throws IOException {
        SecurityConfig securityConfig = new SecurityConfig();
        Cluster cluster = TestUtil.cluster();
        Stack stack = cluster.getStack();
        stack.setSecurityConfig(securityConfig);
        stack.setCluster(cluster);

        InstanceMetadataView primaryGatewayInstance = stack.getPrimaryGatewayInstance();
        String endpointName = primaryGatewayInstance.getShortHostname();
        String environmentDomain = "anenvname.xcu2-8y8x.dev.cldr.work";
        String commonName = "hashofshorthostname." + environmentDomain;
        String fqdn = endpointName + "." + environmentDomain;
        String envName = "anEnvName";

        DetailedEnvironmentResponse environment = DetailedEnvironmentResponse.builder()
                .withName(envName)
                .withEnvironmentDomain(environmentDomain)
                .build();
        when(environmentClientService.getByCrn(Mockito.anyString())).thenReturn(environment);
        when(domainNameProvider.getCommonName(endpointName, environment)).thenReturn(commonName);
        when(domainNameProvider.getFullyQualifiedEndpointName(Set.of(), endpointName, environment)).thenReturn(fqdn);
        when(certificateCreationService.create(eq("123"), eq(endpointName), eq(envName), any(PKCS10CertificationRequest.class),
                eq(stack.getResourceCrn()))).thenThrow(new RuntimeException("Uh-Oh"));

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> {
            CloudbreakServiceException exception = assertThrows(CloudbreakServiceException.class,
                    () -> underTest.generateCertAndSaveForStackAndUpdateDnsEntry(stack));
            assertEquals("The public certificate could not be generated by Public Endpoint Management service: Uh-Oh", exception.getMessage());
        });

        verify(environmentClientService, times(1)).getByCrn(anyString());
        verify(domainNameProvider, times(1)).getCommonName(endpointName, environment);
        verify(domainNameProvider, times(1)).getFullyQualifiedEndpointName(Set.of(), endpointName, environment);
        verify(certificateCreationService, times(1))
                .create(eq("123"), eq(endpointName), eq(envName), any(PKCS10CertificationRequest.class), eq(stack.getResourceCrn()));
    }

    @Test
    void testGenerateCertAndSaveForStackAndUpdateDnsEntryWhenCertGenerationFailsAndExcpetionIsNull() throws IOException {
        SecurityConfig securityConfig = new SecurityConfig();
        Cluster cluster = TestUtil.cluster();
        Stack stack = cluster.getStack();
        stack.setSecurityConfig(securityConfig);
        stack.setCluster(cluster);

        InstanceMetadataView primaryGatewayInstance = stack.getPrimaryGatewayInstance();
        String endpointName = primaryGatewayInstance.getShortHostname();
        String environmentDomain = "anenvname.xcu2-8y8x.dev.cldr.work";
        String commonName = "hashofshorthostname." + environmentDomain;
        String fqdn = endpointName + "." + environmentDomain;
        String envName = "anEnvName";

        DetailedEnvironmentResponse environment = DetailedEnvironmentResponse.builder()
                .withName(envName)
                .withEnvironmentDomain(environmentDomain)
                .build();
        when(environmentClientService.getByCrn(Mockito.anyString())).thenReturn(environment);
        when(domainNameProvider.getCommonName(endpointName, environment)).thenReturn(commonName);
        when(domainNameProvider.getFullyQualifiedEndpointName(Set.of(), endpointName, environment)).thenReturn(fqdn);
        when(certificateCreationService.create(eq("123"), eq(endpointName), eq(envName), any(PKCS10CertificationRequest.class),
                eq(stack.getResourceCrn()))).thenThrow(new RuntimeException());

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> {
            CloudbreakServiceException exception = assertThrows(CloudbreakServiceException.class,
                    () -> underTest.generateCertAndSaveForStackAndUpdateDnsEntry(stack));
            assertEquals("The public certificate could not be generated by Public Endpoint Management service", exception.getMessage());
        });

        verify(environmentClientService, times(1)).getByCrn(anyString());
        verify(domainNameProvider, times(1)).getCommonName(endpointName, environment);
        verify(domainNameProvider, times(1)).getFullyQualifiedEndpointName(Set.of(), endpointName, environment);
        verify(certificateCreationService, times(1))
                .create(eq("123"), eq(endpointName), eq(envName), any(PKCS10CertificationRequest.class), eq(stack.getResourceCrn()));
    }

    @Test
    void testGenerateCertAndSaveForStackAndUpdateDnsEntryWhenPublicDnsEntryCouldNotBeCreated() throws IOException, PemDnsEntryCreateOrUpdateException {
        SecurityConfig securityConfig = new SecurityConfig();
        Cluster cluster = TestUtil.cluster();
        Stack stack = cluster.getStack();
        stack.setSecurityConfig(securityConfig);
        stack.setCluster(cluster);

        InstanceMetadataView primaryGw = stack.getPrimaryGatewayInstance();
        String endpointName = primaryGw.getShortHostname();
        String environmentDomain = "anenvname.xcu2-8y8x.dev.cldr.work";
        String commonName = "hashofshorthostname." + environmentDomain;
        String fqdn = endpointName + "." + environmentDomain;
        String envName = "anEnvName";

        DetailedEnvironmentResponse environment = DetailedEnvironmentResponse.builder()
                .withName(envName)
                .withEnvironmentDomain(environmentDomain)
                .build();
        when(environmentClientService.getByCrn(Mockito.anyString())).thenReturn(environment);
        when(domainNameProvider.getCommonName(endpointName, environment)).thenReturn(commonName);
        when(domainNameProvider.getFullyQualifiedEndpointName(Set.of(), endpointName, environment)).thenReturn(fqdn);
        when(certificateCreationService.create(eq("123"), eq(endpointName), eq(envName), any(PKCS10CertificationRequest.class),
                eq(stack.getResourceCrn()))).thenReturn(List.of("aCertificate"));
        doThrow(new PemDnsEntryCreateOrUpdateException("Uh-Oh")).when(dnsManagementService)
                .createOrUpdateDnsEntryWithIp(eq("123"), eq(endpointName), eq(envName), eq(Boolean.FALSE), eq(List.of(primaryGw.getPublicIpWrapper())));

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> {
            CloudbreakServiceException exception = assertThrows(CloudbreakServiceException.class,
                    () -> underTest.generateCertAndSaveForStackAndUpdateDnsEntry(stack));
            assertEquals("Failed to create or update DNS entry for endpoint 'test-is1-1-1' and environment name 'anEnvName' and IP: '2.2.1.1'",
                    exception.getMessage());
        });

        verify(environmentClientService, times(2)).getByCrn(anyString());
        verify(domainNameProvider, times(1)).getCommonName(endpointName, environment);
        verify(domainNameProvider, times(1)).getFullyQualifiedEndpointName(Set.of(), endpointName, environment);
        verify(certificateCreationService, times(1))
                .create(eq("123"), eq(endpointName), eq(envName), any(PKCS10CertificationRequest.class), eq(stack.getResourceCrn()));
        verify(dnsManagementService, times(1))
                .createOrUpdateDnsEntryWithIp(eq("123"), eq(endpointName), eq(envName), eq(Boolean.FALSE),
                        eq(List.of(primaryGw.getPublicIpWrapper())));
    }

    @Test
    void testGenerateCertAndSaveForStackAndUpdateDnsEntryWhenUserFacingKeyHasAlreadyBeenGenerated() throws IOException, PemDnsEntryCreateOrUpdateException {
        SecurityConfig securityConfig = new SecurityConfig();
        securityConfig.setUserFacingKey(USER_FACING_PRIVATE_KEY);
        Cluster cluster = TestUtil.cluster();
        Stack stack = cluster.getStack();
        stack.setSecurityConfig(securityConfig);
        stack.setCluster(cluster);

        String envName = "anEnvName";
        String environmentDomain = "anenvname.xcu2-8y8x.dev.cldr.work";
        DetailedEnvironmentResponse environment = DetailedEnvironmentResponse.builder()
                .withName(envName)
                .withEnvironmentDomain(environmentDomain)
                .build();
        when(environmentClientService.getByCrn(Mockito.anyString())).thenReturn(environment);

        InstanceMetadataView primaryGatewayInstance = stack.getPrimaryGatewayInstance();
        String endpointName = primaryGatewayInstance.getShortHostname();
        String commonName = "hashofshorthostname." + environmentDomain;
        String fqdn = endpointName + "." + environmentDomain;
        when(domainNameProvider.getCommonName(endpointName, environment)).thenReturn(commonName);
        when(domainNameProvider.getFullyQualifiedEndpointName(Set.of(), endpointName, environment)).thenReturn(fqdn);
        when(certificateCreationService.create(eq("123"), eq(endpointName), eq(envName), any(PKCS10CertificationRequest.class),
                eq(stack.getResourceCrn()))).thenReturn(List.of("aCertificate"));

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.generateCertAndSaveForStackAndUpdateDnsEntry(stack));

        verify(environmentClientService, times(2)).getByCrn(anyString());
        verify(domainNameProvider, times(1)).getCommonName(endpointName, environment);
        verify(domainNameProvider, times(2)).getFullyQualifiedEndpointName(Set.of(), endpointName, environment);
        verify(certificateCreationService, times(1))
                .create(eq("123"), eq(endpointName), eq(envName), any(PKCS10CertificationRequest.class), eq(stack.getResourceCrn()));
        verify(dnsManagementService, times(1))
                .createOrUpdateDnsEntryWithIp(eq("123"), eq(endpointName), eq(envName), eq(Boolean.FALSE),
                        eq(List.of(primaryGatewayInstance.getPublicIpWrapper())));
        verify(securityConfigService, times(1)).save(any(SecurityConfig.class));
    }

    @Test
    void testUpdateDnsEntryWhenEnvironmentServiceClientFails() throws PemDnsEntryCreateOrUpdateException {
        Cluster cluster = TestUtil.cluster();
        Stack stack = cluster.getStack();
        stack.setCluster(cluster);
        InstanceMetadataView primaryGatewayInstance = stack.getPrimaryGatewayInstance();
        String endpointName = primaryGatewayInstance.getShortHostname();
        String envName = "anEnvName";
        DetailedEnvironmentResponse environment = DetailedEnvironmentResponse.builder()
                .withName(envName)
                .build();
        when(environmentClientService.getByCrn(Mockito.anyString())).thenReturn(environment);
        doThrow(new PemDnsEntryCreateOrUpdateException("uh oh"))
                .when(dnsManagementService).createOrUpdateDnsEntryWithIp(eq("123"), eq(endpointName), eq(envName), eq(Boolean.FALSE),
                        eq(List.of(primaryGatewayInstance.getPublicIpWrapper())));

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> {
            assertThrows(CloudbreakServiceException.class, () -> underTest.updateDnsEntry(stack, null));
        });
    }

    @Test
    void testUpdateDnsEntryWhenUmsServiceCallFails() {
        Cluster cluster = TestUtil.cluster();
        Stack stack = cluster.getStack();
        stack.setCluster(cluster);
        InstanceMetadataView primaryGatewayInstance = stack.getPrimaryGatewayInstance();
        String endpointName = primaryGatewayInstance.getShortHostname();
        String envName = "anEnvName";
        DetailedEnvironmentResponse environment = DetailedEnvironmentResponse.builder()
                .withName(envName)
                .build();
        when(environmentClientService.getByCrn(Mockito.anyString())).thenReturn(environment);

        String result = ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.updateDnsEntry(stack, null));

        assertNull(result);
    }

    @Test
    void testUpdateDnsEntryWhenPublicEndpointManagementServiceCallFails() {
        Cluster cluster = TestUtil.cluster();
        Stack stack = cluster.getStack();
        stack.setCluster(cluster);
        InstanceMetadataView primaryGatewayInstance = stack.getPrimaryGatewayInstance();
        String endpointName = primaryGatewayInstance.getShortHostname();
        String envName = "anEnvName";
        String environmentDomain = "anenvname.xcu2-8y8x.dev.cldr.work";
        DetailedEnvironmentResponse environment = DetailedEnvironmentResponse.builder()
                .withName(envName)
                .withEnvironmentDomain(environmentDomain)
                .build();
        when(environmentClientService.getByCrn(Mockito.anyString())).thenReturn(environment);
        when(domainNameProvider.getFullyQualifiedEndpointName(Set.of(), endpointName, environment)).thenThrow(new IllegalStateException("Fail"));

        IllegalStateException result = assertThrows(IllegalStateException.class, () ->
                ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.updateDnsEntry(stack, null)));

        assertEquals("Fail", result.getMessage());
    }

    @Test
    void testUpdateDnsEntryShouldReturnWithFqdnInCaseOfSuccess() throws PemDnsEntryCreateOrUpdateException {
        Cluster cluster = TestUtil.cluster();
        Stack stack = cluster.getStack();
        stack.setCluster(cluster);
        InstanceMetadataView primaryGatewayInstance = stack.getPrimaryGatewayInstance();
        String endpointName = primaryGatewayInstance.getShortHostname();
        String envName = "anEnvName";
        String environmentDomain = "anenvname.xcu2-8y8x.dev.cldr.work";
        DetailedEnvironmentResponse environment = DetailedEnvironmentResponse.builder()
                .withName(envName)
                .withEnvironmentDomain(environmentDomain)
                .build();
        when(environmentClientService.getByCrn(Mockito.anyString())).thenReturn(environment);
        String fqdn = endpointName + "." + environmentDomain;
        when(domainNameProvider.getFullyQualifiedEndpointName(Set.of(), endpointName, environment)).thenReturn(fqdn);

        String result = ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.updateDnsEntry(stack, null));

        assertNotNull(result);
        assertEquals(fqdn, result);
        verify(dnsManagementService, times(1))
                .createOrUpdateDnsEntryWithIp(eq("123"), eq(endpointName), eq(envName), eq(Boolean.FALSE),
                        eq(List.of(primaryGatewayInstance.getPublicIpWrapper())));
    }

    @Test
    void testUpdateDnsEntryShouldReturnWithFqdnInCaseOfSuccessAndSetEntryToGatewayIpWhenGatewayIpIsSpecified() throws PemDnsEntryCreateOrUpdateException {
        String gatewayIp = "10.191.192.193";
        Cluster cluster = TestUtil.cluster();
        Stack stack = cluster.getStack();
        stack.setCluster(cluster);
        InstanceMetadataView primaryGatewayInstance = stack.getPrimaryGatewayInstance();
        String endpointName = primaryGatewayInstance.getShortHostname();
        String envName = "anEnvName";
        String environmentDomain = "anenvname.xcu2-8y8x.dev.cldr.work";
        DetailedEnvironmentResponse environment = DetailedEnvironmentResponse.builder()
                .withName(envName)
                .withEnvironmentDomain(environmentDomain)
                .build();
        when(environmentClientService.getByCrn(Mockito.anyString())).thenReturn(environment);
        String fqdn = endpointName + "." + environmentDomain;
        when(domainNameProvider.getFullyQualifiedEndpointName(Set.of(), endpointName, environment)).thenReturn(fqdn);

        String result = ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.updateDnsEntry(stack, gatewayIp));

        assertNotNull(result);
        assertEquals(fqdn, result);
        verify(dnsManagementService, times(1))
                .createOrUpdateDnsEntryWithIp(eq("123"), eq(endpointName), eq(envName), eq(Boolean.FALSE),
                        eq(List.of(gatewayIp)));
    }

    @Test
    void testUpdateDnsEntryShouldCallDnsManagementServiceWithGatewayInstanceShortHostnameAsEndpointName() throws PemDnsEntryCreateOrUpdateException {
        String gatewayIp = "10.191.192.193";
        Cluster cluster = TestUtil.cluster();
        Stack stack = cluster.getStack();
        stack.setCluster(cluster);
        InstanceMetadataView primaryGatewayInstance = stack.getPrimaryGatewayInstance();
        String endpointName = primaryGatewayInstance.getShortHostname();
        String envName = "anEnvName";
        String environmentDomain = "anenvname.xcu2-8y8x.dev.cldr.work";
        DetailedEnvironmentResponse environment = DetailedEnvironmentResponse.builder()
                .withName(envName)
                .withEnvironmentDomain(environmentDomain)
                .build();
        when(environmentClientService.getByCrn(Mockito.anyString())).thenReturn(environment);
        String fqdn = endpointName + ".anenvname.xcu2-8y8x.dev.cldr.work";
        when(domainNameProvider.getFullyQualifiedEndpointName(Set.of(), endpointName, environment)).thenReturn(fqdn);

        String result = ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.updateDnsEntry(stack, gatewayIp));

        assertNotNull(result);
        assertEquals(fqdn, result);
        verify(dnsManagementService, times(1))
                .createOrUpdateDnsEntryWithIp(eq("123"), eq(endpointName), eq(envName), eq(Boolean.FALSE),
                        eq(List.of(gatewayIp)));
    }

    @Test
    void testDeleteDnsEntryShouldCallDnsManagementServiceWithGatewayInstanceShortHostnameAsEndpointNameWhenEnvNameIsSpecified() {
        Cluster cluster = TestUtil.cluster();
        Stack stack = cluster.getStack();
        stack.setCluster(cluster);
        InstanceMetadataView primaryGatewayInstance = stack.getPrimaryGatewayInstance();
        String endpointName = primaryGatewayInstance.getShortHostname();
        String gatewayIp = primaryGatewayInstance.getPublicIpWrapper();
        String envName = "anEnvName";
        when(dnsManagementService.deleteDnsEntryWithIp(eq("123"), eq(endpointName), eq(envName), eq(Boolean.FALSE),
                eq(List.of(gatewayIp)))).thenReturn(Boolean.TRUE);

        String result = ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.deleteDnsEntry(stack, envName));

        assertNotNull(result);
        assertEquals(gatewayIp, result);
        verify(dnsManagementService, times(1))
                .deleteDnsEntryWithIp(eq("123"), eq(endpointName), eq(envName), eq(Boolean.FALSE),
                        eq(List.of(gatewayIp)));
    }

    @Test
    void testDeleteDnsEntryShouldCallDnsManagementServiceWithGatewayInstanceShortHostnameAsEndpointNameWhenEnvNameIsNotSpecified() {
        Cluster cluster = TestUtil.cluster();
        Stack stack = cluster.getStack();
        stack.setCluster(cluster);
        InstanceMetadataView primaryGatewayInstance = stack.getPrimaryGatewayInstance();
        String endpointName = primaryGatewayInstance.getShortHostname();
        String gatewayIp = primaryGatewayInstance.getPublicIpWrapper();
        String envName = "anEnvName";
        DetailedEnvironmentResponse environment = DetailedEnvironmentResponse.builder()
                .withName(envName)
                .build();
        when(environmentClientService.getByCrn(Mockito.anyString())).thenReturn(environment);
        when(dnsManagementService.deleteDnsEntryWithIp(eq("123"), eq(endpointName), eq(envName), eq(Boolean.FALSE),
                eq(List.of(gatewayIp)))).thenReturn(Boolean.TRUE);

        String result = ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.deleteDnsEntry(stack, null));

        assertNotNull(result);
        assertEquals(gatewayIp, result);
        verify(dnsManagementService, times(1))
                .deleteDnsEntryWithIp(eq("123"), eq(endpointName), eq(envName), eq(Boolean.FALSE),
                        eq(List.of(gatewayIp)));
    }

    @Test
    void testDeleteDnsEntryShouldNotBeCalledWhenEndpointNameIsNotSpecified() {
        Stack stackMock = mock(Stack.class);
        InstanceMetadataView instanceMetadataViewMock = mock(InstanceMetadataView.class);
        when(stackMock.getPrimaryGatewayInstance()).thenReturn(instanceMetadataViewMock);
        when(instanceMetadataViewMock.getPublicIpWrapper()).thenReturn("publicIpWrapper");
        when(instanceMetadataViewMock.getShortHostname()).thenReturn(null);
        String envName = "anEnvName";

        String result = ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.deleteDnsEntry(stackMock, envName));

        assertNull(result);
        verifyNoInteractions(dnsManagementService);
    }

    @Test
    void testGenerateCertAndSaveForStackWithLoadBalancerWithCloudDns() throws IOException, PemDnsEntryCreateOrUpdateException {
        SecurityConfig securityConfig = new SecurityConfig();
        Cluster cluster = TestUtil.cluster();
        LoadBalancer loadBalancer = new LoadBalancer();
        loadBalancer.setEndpoint("gateway");
        loadBalancer.setDns("http://cloud.dns");
        loadBalancer.setHostedZoneId("1");
        loadBalancer.setType(LoadBalancerType.PUBLIC);
        Stack stack = cluster.getStack();
        stack.setSecurityConfig(securityConfig);
        stack.setCluster(cluster);
        stack.setLoadBalancers(Set.of(loadBalancer));

        InstanceMetadataView primaryGatewayInstance = stack.getPrimaryGatewayInstance();
        String endpointName = primaryGatewayInstance.getShortHostname();
        String lbEndpointName = loadBalancer.getEndpoint();
        String environmentDomain = "anenvname.xcu2-8y8x.dev.cldr.work";
        String commonName = "hashofshorthostname." + environmentDomain;
        String fqdn = endpointName + "." + environmentDomain;
        String lbfqdn = lbEndpointName + "." + environmentDomain;
        String envName = "anEnvName";
        String cloudDns = loadBalancer.getDns();
        String hostedZoneId = loadBalancer.getHostedZoneId();

        DetailedEnvironmentResponse environment = DetailedEnvironmentResponse.builder()
                .withName(envName)
                .withEnvironmentDomain(environmentDomain)
                .build();
        when(environmentClientService.getByCrn(anyString())).thenReturn(environment);
        when(domainNameProvider.getCommonName(endpointName, environment)).thenReturn(commonName);
        when(domainNameProvider.getFullyQualifiedEndpointName(Set.of(), endpointName, environment)).thenReturn(fqdn);
        when(domainNameProvider.getFullyQualifiedEndpointName(Set.of(), lbEndpointName, environment)).thenReturn(lbfqdn);
        when(certificateCreationService.create(eq("123"), eq(endpointName), eq(envName), any(PKCS10CertificationRequest.class),
                eq(stack.getResourceCrn()))).thenReturn(List.of("aCertificate"));
        when(loadBalancerPersistenceService.findByStackId(anyLong())).thenReturn(Set.of(loadBalancer));
        when(loadBalancerConfigService.selectLoadBalancerForFrontend(any(), any())).thenReturn(Optional.of(loadBalancer));

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.generateCertAndSaveForStackAndUpdateDnsEntry(stack));

        verify(environmentClientService, times(3)).getByCrn(anyString());
        verify(domainNameProvider, times(1)).getCommonName(endpointName, environment);
        verify(domainNameProvider, times(2)).getFullyQualifiedEndpointName(Set.of(), endpointName, environment);
        verify(certificateCreationService, times(1))
                .create(eq("123"), eq(endpointName), eq(envName), any(PKCS10CertificationRequest.class), eq(stack.getResourceCrn()));
        verify(dnsManagementService, times(1))
                .createOrUpdateDnsEntryWithIp(eq("123"), eq(endpointName), eq(envName), eq(Boolean.FALSE),
                        eq(List.of(primaryGatewayInstance.getPublicIpWrapper())));
        verify(dnsManagementService, times(1))
                .createOrUpdateDnsEntryWithCloudDns(eq("123"), eq(lbEndpointName), eq(envName), eq(cloudDns),
                        eq(hostedZoneId));
        verify(securityConfigService, times(1)).save(any(SecurityConfig.class));
    }

    @Test
    void testGenerateCertAndSaveForStackWithLoadBalancerWithIpAddress() throws IOException, PemDnsEntryCreateOrUpdateException {
        SecurityConfig securityConfig = new SecurityConfig();
        Cluster cluster = TestUtil.cluster();
        LoadBalancer loadBalancer = new LoadBalancer();
        loadBalancer.setEndpoint("gateway");
        loadBalancer.setIp("1.2.3.4");
        loadBalancer.setType(LoadBalancerType.PUBLIC);
        Stack stack = cluster.getStack();
        stack.setSecurityConfig(securityConfig);
        stack.setCluster(cluster);
        stack.setLoadBalancers(Set.of(loadBalancer));

        InstanceMetadataView primaryGatewayInstance = stack.getPrimaryGatewayInstance();
        String endpointName = primaryGatewayInstance.getShortHostname();
        String lbEndpointName = loadBalancer.getEndpoint();
        String environmentDomain = "anenvname.xcu2-8y8x.dev.cldr.work";
        String commonName = "hashofshorthostname." + environmentDomain;
        String fqdn = endpointName + "." + environmentDomain;
        String lbfqdn = lbEndpointName + "." + environmentDomain;
        String envName = "anEnvName";
        String lbIp = loadBalancer.getIp();

        DetailedEnvironmentResponse environment = DetailedEnvironmentResponse.builder()
                .withName(envName)
                .withEnvironmentDomain(environmentDomain)
                .build();
        when(environmentClientService.getByCrn(anyString())).thenReturn(environment);
        when(domainNameProvider.getCommonName(endpointName, environment)).thenReturn(commonName);
        when(domainNameProvider.getFullyQualifiedEndpointName(Set.of(), endpointName, environment)).thenReturn(fqdn);
        when(domainNameProvider.getFullyQualifiedEndpointName(Set.of(), lbEndpointName, environment)).thenReturn(lbfqdn);
        when(certificateCreationService.create(eq("123"), eq(endpointName), eq(envName), any(PKCS10CertificationRequest.class),
                eq(stack.getResourceCrn()))).thenReturn(List.of("aCertificate"));
        when(loadBalancerPersistenceService.findByStackId(anyLong())).thenReturn(Set.of(loadBalancer));
        when(loadBalancerConfigService.selectLoadBalancerForFrontend(any(), any())).thenReturn(Optional.of(loadBalancer));

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.generateCertAndSaveForStackAndUpdateDnsEntry(stack));

        verify(environmentClientService, times(3)).getByCrn(anyString());
        verify(domainNameProvider, times(1)).getCommonName(endpointName, environment);
        verify(domainNameProvider, times(2)).getFullyQualifiedEndpointName(Set.of(), endpointName, environment);
        verify(certificateCreationService, times(1))
                .create(eq("123"), eq(endpointName), eq(envName), any(PKCS10CertificationRequest.class), eq(stack.getResourceCrn()));
        verify(dnsManagementService, times(1))
                .createOrUpdateDnsEntryWithIp(eq("123"), eq(endpointName), eq(envName), eq(Boolean.FALSE),
                        eq(List.of(primaryGatewayInstance.getPublicIpWrapper())));
        verify(dnsManagementService, times(1))
                .createOrUpdateDnsEntryWithIp(eq("123"), eq(lbEndpointName), eq(envName), eq(Boolean.FALSE),
                        eq(List.of(lbIp)));
        verify(securityConfigService, times(1)).save(any(SecurityConfig.class));
    }

    @Test
    void testDeleteDnsEntryForLoadBalancerWithCloudDns() {
        Cluster cluster = TestUtil.cluster();
        LoadBalancer loadBalancer = new LoadBalancer();
        loadBalancer.setEndpoint("gateway");
        loadBalancer.setDns("http://cloud.dns");
        loadBalancer.setHostedZoneId("1");
        loadBalancer.setType(LoadBalancerType.PUBLIC);
        Stack stack = cluster.getStack();
        stack.setCluster(cluster);
        stack.setLoadBalancers(Set.of(loadBalancer));

        String lbEndpointName = loadBalancer.getEndpoint();
        String envName = "anEnvName";
        String cloudDns = loadBalancer.getDns();
        String hostedZoneId = loadBalancer.getHostedZoneId();

        when(dnsManagementService.deleteDnsEntryWithCloudDns(eq("123"), eq(lbEndpointName), eq(envName), eq(cloudDns),
                eq(hostedZoneId))).thenReturn(Boolean.TRUE);
        when(loadBalancerPersistenceService.findByStackId(anyLong())).thenReturn(Set.of(loadBalancer));
        when(loadBalancerConfigService.selectLoadBalancerForFrontend(any(), any())).thenReturn(Optional.of(loadBalancer));

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.deleteLoadBalancerDnsEntry(stack, envName));

        verify(dnsManagementService, times(1))
                .deleteDnsEntryWithCloudDns(eq("123"), eq(lbEndpointName), eq(envName), eq(cloudDns),
                        eq(hostedZoneId));
    }

    @Test
    void testDeleteDnsEntryForLoadBalancerWithIpAddress() {
        Cluster cluster = TestUtil.cluster();
        LoadBalancer loadBalancer = new LoadBalancer();
        loadBalancer.setEndpoint("gateway");
        loadBalancer.setIp("1.2.3.4");
        loadBalancer.setType(LoadBalancerType.PUBLIC);
        Stack stack = cluster.getStack();
        stack.setCluster(cluster);
        stack.setLoadBalancers(Set.of(loadBalancer));

        String lbEndpointName = loadBalancer.getEndpoint();
        String envName = "anEnvName";
        String ip = loadBalancer.getIp();

        when(dnsManagementService.deleteDnsEntryWithIp(eq("123"), eq(lbEndpointName), eq(envName), eq(Boolean.FALSE),
                eq(List.of(ip)))).thenReturn(Boolean.TRUE);
        when(loadBalancerPersistenceService.findByStackId(anyLong())).thenReturn(Set.of(loadBalancer));
        when(loadBalancerConfigService.selectLoadBalancerForFrontend(any(), any())).thenReturn(Optional.of(loadBalancer));

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.deleteLoadBalancerDnsEntry(stack, envName));

        verify(dnsManagementService, times(1))
                .deleteDnsEntryWithIp(eq("123"), eq(lbEndpointName), eq(envName), eq(Boolean.FALSE),
                        eq(List.of(ip)));
    }

    @Test
    void testSkipLoadBalancersWithMissingEndpoints() {
        String validEndpoint = "valid-endpoint";
        LoadBalancer nullEndpointLb = new LoadBalancer();
        LoadBalancer emptyEndpointLb = new LoadBalancer();
        emptyEndpointLb.setEndpoint("");
        LoadBalancer validEndpointLb = new LoadBalancer();
        emptyEndpointLb.setEndpoint(validEndpoint);
        Stack stack = new Stack();
        stack.setId(1L);

        when(loadBalancerPersistenceService.findByStackId(anyLong())).thenReturn(Set.of(nullEndpointLb, emptyEndpointLb, validEndpointLb));

        Set<String> loadBalancerEndpoints = underTest.getLoadBalancerNamesForStack(stack);

        assertEquals(Set.of(validEndpoint), loadBalancerEndpoints);
    }

    @Test
    void testUpdateDnsEntryForLoadBalancers() {
        SecurityConfig securityConfig = new SecurityConfig();
        Cluster cluster = TestUtil.cluster();
        LoadBalancer loadBalancer = new LoadBalancer();
        loadBalancer.setEndpoint("gateway");
        loadBalancer.setDns("http://cloud.dns");
        loadBalancer.setHostedZoneId("1");
        loadBalancer.setType(LoadBalancerType.PUBLIC);
        Stack stack = cluster.getStack();
        stack.setSecurityConfig(securityConfig);
        stack.setCluster(cluster);
        stack.setLoadBalancers(Set.of(loadBalancer));
        String envName = "anEnvName";
        DetailedEnvironmentResponse environment = DetailedEnvironmentResponse.builder()
                .withName(envName)
                .build();

        when(loadBalancerPersistenceService.findByStackId(anyLong())).thenReturn(Set.of(loadBalancer));
        when(loadBalancerConfigService.selectLoadBalancerForFrontend(any(), any())).thenReturn(Optional.of(loadBalancer));
        when(environmentClientService.getByCrn(anyString())).thenReturn(environment);

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> {
            underTest.updateDnsEntryForLoadBalancers(stack);
        });
    }

    @Test
    void testUpdateDnsEntryForLoadBalancersWhenDnsEntryRegistrationFailsForCloudDns() throws PemDnsEntryCreateOrUpdateException {
        SecurityConfig securityConfig = new SecurityConfig();
        Cluster cluster = TestUtil.cluster();
        LoadBalancer loadBalancer = new LoadBalancer();
        loadBalancer.setEndpoint("gateway");
        loadBalancer.setDns("http://cloud.dns");
        loadBalancer.setHostedZoneId("1");
        loadBalancer.setType(LoadBalancerType.PUBLIC);
        Stack stack = cluster.getStack();
        stack.setSecurityConfig(securityConfig);
        stack.setCluster(cluster);
        stack.setLoadBalancers(Set.of(loadBalancer));
        String envName = "anEnvName";
        DetailedEnvironmentResponse environment = DetailedEnvironmentResponse.builder()
                .withName(envName)
                .build();

        when(loadBalancerPersistenceService.findByStackId(anyLong())).thenReturn(Set.of(loadBalancer));
        when(loadBalancerConfigService.selectLoadBalancerForFrontend(any(), any())).thenReturn(Optional.of(loadBalancer));
        when(environmentClientService.getByCrn(anyString())).thenReturn(environment);
        doThrow(new PemDnsEntryCreateOrUpdateException("Uh-Oh"))
                .when(dnsManagementService).createOrUpdateDnsEntryWithCloudDns(any(), anyString(), anyString(), any(), anyString());

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> {
            CloudbreakServiceException exception = assertThrows(CloudbreakServiceException.class,
                    () -> underTest.updateDnsEntryForLoadBalancers(stack));

            String expectedMsg = "Failed to create or update DNS entry for load balancer with endpoint 'gateway' and environment name 'anEnvName'";
            assertEquals(expectedMsg, exception.getMessage());
        });

        verify(flowMessageService, times(1)).fireEventAndLog(stack.getId(), UPDATE_IN_PROGRESS.name(), STACK_LB_REGISTER_PUBLIC_DNS_FAILED);
    }

    @Test
    void testUpdateDnsEntryForLoadBalancersWhenDnsEntryRegistrationFailsForLbIp() throws PemDnsEntryCreateOrUpdateException {
        SecurityConfig securityConfig = new SecurityConfig();
        Cluster cluster = TestUtil.cluster();
        LoadBalancer loadBalancer = new LoadBalancer();
        loadBalancer.setEndpoint("gateway");
        loadBalancer.setIp("10.0.0.1");
        loadBalancer.setHostedZoneId("1");
        loadBalancer.setType(LoadBalancerType.PUBLIC);
        Stack stack = cluster.getStack();
        stack.setSecurityConfig(securityConfig);
        stack.setCluster(cluster);
        stack.setLoadBalancers(Set.of(loadBalancer));
        String envName = "anEnvName";
        DetailedEnvironmentResponse environment = DetailedEnvironmentResponse.builder()
                .withName(envName)
                .build();

        when(loadBalancerPersistenceService.findByStackId(anyLong())).thenReturn(Set.of(loadBalancer));
        when(loadBalancerConfigService.selectLoadBalancerForFrontend(any(), any())).thenReturn(Optional.of(loadBalancer));
        when(environmentClientService.getByCrn(anyString())).thenReturn(environment);
        doThrow(new PemDnsEntryCreateOrUpdateException("Uh-Oh"))
                .when(dnsManagementService).createOrUpdateDnsEntryWithIp(anyString(), anyString(), anyString(), anyBoolean(), anyList());

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> {
            CloudbreakServiceException exception = assertThrows(CloudbreakServiceException.class,
                    () -> underTest.updateDnsEntryForLoadBalancers(stack));

            String expectedMsg = "Failed to create or update DNS entry for load balancer with endpoint 'gateway' and environment name 'anEnvName'";
            assertEquals(expectedMsg, exception.getMessage());
        });

        verify(flowMessageService, times(1)).fireEventAndLog(stack.getId(), UPDATE_IN_PROGRESS.name(), STACK_LB_REGISTER_PUBLIC_DNS_FAILED);
    }

    @Test
    void testUpdateDnsEntryForLoadBalancersWheNoIpOrCloudDnsExistsForLb() {
        SecurityConfig securityConfig = new SecurityConfig();
        Cluster cluster = TestUtil.cluster();
        LoadBalancer loadBalancer = new LoadBalancer();
        loadBalancer.setEndpoint("gateway");
        loadBalancer.setHostedZoneId("1");
        loadBalancer.setType(LoadBalancerType.PUBLIC);
        Stack stack = cluster.getStack();
        stack.setSecurityConfig(securityConfig);
        stack.setCluster(cluster);
        stack.setLoadBalancers(Set.of(loadBalancer));
        String envName = "anEnvName";
        DetailedEnvironmentResponse environment = DetailedEnvironmentResponse.builder()
                .withName(envName)
                .build();

        when(loadBalancerPersistenceService.findByStackId(anyLong())).thenReturn(Set.of(loadBalancer));
        when(loadBalancerConfigService.selectLoadBalancerForFrontend(any(), any())).thenReturn(Optional.of(loadBalancer));
        when(environmentClientService.getByCrn(anyString())).thenReturn(environment);

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> {
            CloudbreakServiceException exception = assertThrows(CloudbreakServiceException.class,
                    () -> underTest.updateDnsEntryForLoadBalancers(stack));

            String expectedMsg = "Could not find IP or cloud DNS info for load balancer with endpoint 'gateway' and environment name: 'anEnvName'. " +
                    "DNS registration could not be executed.";
            assertEquals(expectedMsg, exception.getMessage());
        });

    }

    @Test
    void testUpdateDnsEntryForLoadBalancersMissingLb() throws PemDnsEntryCreateOrUpdateException {
        Stack stack = new Stack();
        stack.setId(1L);
        when(loadBalancerPersistenceService.findByStackId(anyLong())).thenReturn(Set.of());

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.updateDnsEntryForLoadBalancers(stack));

        verifyNoInteractions(dnsManagementService);
    }

    @Test
    void testUpdateDnsEntryForLoadBalancersPemDisabled() {
        ReflectionTestUtils.setField(underTest, "certGenerationEnabled", Boolean.FALSE);

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> {
            underTest.updateDnsEntryForLoadBalancers(new Stack());
        });

        verifyNoInteractions(dnsManagementService);
    }

    @Test
    void testGenerateCertAndSaveForStackAndUpdateDnsEntryWithAlternativeCertificate() throws IOException, PemDnsEntryCreateOrUpdateException {
        Security.addProvider(new BouncyCastleFipsProvider());
        SecurityConfig securityConfig = new SecurityConfig();
        Cluster cluster = TestUtil.cluster();
        Stack stack = cluster.getStack();
        stack.setSecurityConfig(securityConfig);
        stack.setCluster(cluster);

        InstanceMetadataView primaryGatewayInstance = stack.getPrimaryGatewayInstance();
        String endpointName = primaryGatewayInstance.getShortHostname();
        String environmentDomain = "anenvname.xcu2-8y8x.dev.cldr.work";
        String commonName = "hashofshorthostname." + environmentDomain;
        String fqdn = endpointName + '.' + environmentDomain;
        String envName = "anEnvName";

        DetailedEnvironmentResponse environment = DetailedEnvironmentResponse.builder()
                .withName(envName)
                .withEnvironmentDomain(environmentDomain)
                .build();
        when(environmentClientService.getByCrn(anyString())).thenReturn(environment);
        when(domainNameProvider.getCommonName(endpointName, environment)).thenReturn(commonName);
        when(domainNameProvider.getFullyQualifiedEndpointName(Set.of(), endpointName, environment)).thenReturn(fqdn);
        when(certificateCreationService.create(eq("123"), eq(endpointName), eq(envName), any(PKCS10CertificationRequest.class),
                eq(stack.getResourceCrn()))).thenReturn(List.of("aCertificate"));
        ClouderaManagerRepo clouderaManagerRepo = mock(ClouderaManagerRepo.class);
        when(clouderaManagerRepo.getVersion()).thenReturn("7.13.2.0");
        when(clusterComponentConfigProvider.getClouderaManagerRepoDetails(anyLong())).thenReturn(clouderaManagerRepo);
        when(entitlementService.isConfigureEncryptionProfileEnabled(anyString())).thenReturn(true);

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.generateAlternativeCertAndSaveForStack(stack));

        verify(environmentClientService, times(1)).getByCrn(anyString());
        verify(domainNameProvider, times(1)).getCommonName(endpointName, environment);
        verify(domainNameProvider, times(1)).getFullyQualifiedEndpointName(Set.of(), endpointName, environment);
        verify(certificateCreationService, times(1))
                .create(eq("123"), eq(endpointName), eq(envName), any(PKCS10CertificationRequest.class), eq(stack.getResourceCrn()));
        verify(securityConfigService, times(1)).save(any(SecurityConfig.class));
    }
}
