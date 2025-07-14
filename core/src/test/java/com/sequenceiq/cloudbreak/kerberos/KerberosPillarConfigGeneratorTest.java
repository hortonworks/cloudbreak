package com.sequenceiq.cloudbreak.kerberos;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.dto.KerberosConfig;
import com.sequenceiq.cloudbreak.orchestrator.model.SaltPillarProperties;
import com.sequenceiq.cloudbreak.service.freeipa.FreeipaClientService;
import com.sequenceiq.cloudbreak.template.kerberos.KerberosDetailService;
import com.sequenceiq.common.api.type.EnvironmentType;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.describe.DescribeFreeIpaResponse;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.describe.TrustResponse;

@ExtendWith(MockitoExtension.class)
public class KerberosPillarConfigGeneratorTest {

    private static final String TEST_REALM = "TEST.REALM";

    private static final String TEST_URL = "test-url";

    private static final String TEST_ADMIN_URL = "test-admin-url";

    private static final String TEST_CONTAINER_DN = "test-container-dn";

    private static final String TEST_ENCRYPTION_TYPE = "test-encryption-type";

    private static final String TEST_CCACHE_LOCATION = "test-ccache-location";

    private static final String TEST_SECRET_LOCATION = "test-secret-location";

    private static final String TEST_CRN = "test-crn";

    @Mock
    private KerberosDetailService kerberosDetailService;

    @Mock
    private FreeipaClientService freeipaClient;

    @InjectMocks
    private KerberosPillarConfigGenerator underTest;

    @BeforeEach
    public void setUp() {
        ReflectionTestUtils.setField(underTest, "defaultKerberosEncryptionType", TEST_ENCRYPTION_TYPE);
        ReflectionTestUtils.setField(underTest, "defaultKerberosCcacheSecretLocation", TEST_CCACHE_LOCATION);
        ReflectionTestUtils.setField(underTest, "kerberosSecretLocation", TEST_SECRET_LOCATION);
    }

    @Test
    public void testCreateKerberosPillarWhenKerberosIsNotNeeded() throws IOException {
        // GIVEN
        KerberosConfig kerberosConfig = KerberosConfig.KerberosConfigBuilder.aKerberosConfig().build();
        DetailedEnvironmentResponse environmentResponse = createEnvironmentResponse(EnvironmentType.PUBLIC_CLOUD.name());

        when(kerberosDetailService.areClusterManagerManagedKerberosPackages(kerberosConfig)).thenReturn(false);

        // WHEN
        Map<String, SaltPillarProperties> result = underTest.createKerberosPillar(kerberosConfig, environmentResponse);

        // THEN
        assertTrue(result.isEmpty());
    }

    @Test
    public void testCreateKerberosPillarWhenDescriptorIsEmpty() throws IOException {
        // GIVEN
        KerberosConfig kerberosConfig = KerberosConfig.KerberosConfigBuilder.aKerberosConfig()
                .withUrl(TEST_URL)
                .withRealm(TEST_REALM)
                .withVerifyKdcTrust(true)
                .withContainerDn(TEST_CONTAINER_DN)
                .build();
        DetailedEnvironmentResponse environmentResponse = createEnvironmentResponse(EnvironmentType.PUBLIC_CLOUD.name());

        when(kerberosDetailService.areClusterManagerManagedKerberosPackages(kerberosConfig)).thenReturn(true);
        when(kerberosDetailService.isClusterManagerManagedKrb5Config(kerberosConfig)).thenReturn(false);
        when(kerberosDetailService.resolveHostForKdcAdmin(kerberosConfig, TEST_URL)).thenReturn(TEST_ADMIN_URL);

        // WHEN
        Map<String, SaltPillarProperties> result = underTest.createKerberosPillar(kerberosConfig, environmentResponse);

        // THEN
        assertNotNull(result);
        assertEquals(1, result.size());
        assertTrue(result.containsKey("kerberos"));

        SaltPillarProperties saltPillarProperties = result.get("kerberos");
        assertEquals("/kerberos/init.sls", saltPillarProperties.getPath());

        Map<String, Object> properties = saltPillarProperties.getProperties();
        assertNotNull(properties);
        assertEquals(2, properties.size());

        Map<String, String> kerberosProperties = (Map<String, String>) properties.get("kerberos");
        assertNotNull(kerberosProperties);
        assertEquals(TEST_URL, kerberosProperties.get("url"));
        assertEquals(TEST_ADMIN_URL, kerberosProperties.get("adminUrl"));
        assertEquals(TEST_REALM, kerberosProperties.get("realm"));
        assertEquals(TEST_ENCRYPTION_TYPE, kerberosProperties.get("encryptionType"));
        assertEquals(TEST_CCACHE_LOCATION, kerberosProperties.get("cCacheSecretLocation"));
        assertEquals(TEST_SECRET_LOCATION, kerberosProperties.get("kerberosSecretLocation"));
        assertEquals("true", kerberosProperties.get("verifyKdcTrust"));
        assertEquals(TEST_CONTAINER_DN, kerberosProperties.get("container-dn"));

        Map<String, Object> trustProperties = (Map<String, Object>) properties.get("trust");
        assertNotNull(trustProperties);
        assertTrue(trustProperties.isEmpty());
    }

    @Test
    public void testCreateKerberosPillarWhenDescriptorIsNotEmpty() throws IOException {
        // GIVEN
        String descriptor = "{\"kerberos-env\":{\"properties\":{\"kdc_hosts\":\"" + TEST_URL + "\",\"admin_server_host\":\"" + TEST_ADMIN_URL
                + "\",\"realm\":\"" + TEST_REALM + "\"}}}";
        KerberosConfig kerberosConfig = KerberosConfig.KerberosConfigBuilder.aKerberosConfig()
                .withDescriptor(descriptor)
                .withVerifyKdcTrust(true)
                .withContainerDn(TEST_CONTAINER_DN)
                .build();
        DetailedEnvironmentResponse environmentResponse = createEnvironmentResponse(EnvironmentType.PUBLIC_CLOUD.name());

        Map<String, Object> properties = new HashMap<>();
        properties.put("kdc_hosts", TEST_URL);
        properties.put("admin_server_host", TEST_ADMIN_URL);
        properties.put("realm", TEST_REALM);

        when(kerberosDetailService.areClusterManagerManagedKerberosPackages(kerberosConfig)).thenReturn(true);
        when(kerberosDetailService.isClusterManagerManagedKrb5Config(kerberosConfig)).thenReturn(false);
        when(kerberosDetailService.getKerberosEnvProperties(kerberosConfig)).thenReturn(properties);

        // WHEN
        Map<String, SaltPillarProperties> result = underTest.createKerberosPillar(kerberosConfig, environmentResponse);

        // THEN
        assertNotNull(result);
        assertEquals(1, result.size());
        assertTrue(result.containsKey("kerberos"));

        SaltPillarProperties saltPillarProperties = result.get("kerberos");
        assertEquals("/kerberos/init.sls", saltPillarProperties.getPath());

        Map<String, Object> resultProperties = saltPillarProperties.getProperties();
        assertNotNull(resultProperties);
        assertEquals(2, resultProperties.size());

        Map<String, String> kerberosProperties = (Map<String, String>) resultProperties.get("kerberos");
        assertNotNull(kerberosProperties);
        assertEquals(TEST_URL, kerberosProperties.get("url"));
        assertEquals(TEST_ADMIN_URL, kerberosProperties.get("adminUrl"));
        assertEquals(TEST_REALM, kerberosProperties.get("realm"));
        assertEquals(TEST_ENCRYPTION_TYPE, kerberosProperties.get("encryptionType"));
        assertEquals(TEST_CCACHE_LOCATION, kerberosProperties.get("cCacheSecretLocation"));
        assertEquals(TEST_SECRET_LOCATION, kerberosProperties.get("kerberosSecretLocation"));
        assertEquals("true", kerberosProperties.get("verifyKdcTrust"));
        assertEquals(TEST_CONTAINER_DN, kerberosProperties.get("container-dn"));

        Map<String, Object> trustProperties = (Map<String, Object>) resultProperties.get("trust");
        assertNotNull(trustProperties);
        assertTrue(trustProperties.isEmpty());
    }

    @Test
    public void testCreateKerberosPillarWhenEnvironmentIsHybridAndTrustResponseHasRealm() throws IOException {
        // GIVEN
        KerberosConfig kerberosConfig = KerberosConfig.KerberosConfigBuilder.aKerberosConfig()
                .withUrl(TEST_URL)
                .withRealm(TEST_REALM)
                .withVerifyKdcTrust(true)
                .withContainerDn(TEST_CONTAINER_DN)
                .build();
        DetailedEnvironmentResponse environmentResponse = createEnvironmentResponse(EnvironmentType.HYBRID.name());

        DescribeFreeIpaResponse freeIpaResponse = new DescribeFreeIpaResponse();
        TrustResponse trustResponse = new TrustResponse();
        trustResponse.setRealm(TEST_REALM);
        freeIpaResponse.setTrust(trustResponse);

        when(kerberosDetailService.areClusterManagerManagedKerberosPackages(kerberosConfig)).thenReturn(true);
        when(kerberosDetailService.isClusterManagerManagedKrb5Config(kerberosConfig)).thenReturn(false);
        when(kerberosDetailService.resolveHostForKdcAdmin(kerberosConfig, TEST_URL)).thenReturn(TEST_ADMIN_URL);
        when(freeipaClient.findByEnvironmentCrn(TEST_CRN)).thenReturn(Optional.of(freeIpaResponse));

        // WHEN
        Map<String, SaltPillarProperties> result = underTest.createKerberosPillar(kerberosConfig, environmentResponse);

        // THEN
        assertNotNull(result);
        assertEquals(1, result.size());
        assertTrue(result.containsKey("kerberos"));

        SaltPillarProperties saltPillarProperties = result.get("kerberos");
        assertEquals("/kerberos/init.sls", saltPillarProperties.getPath());

        Map<String, Object> properties = saltPillarProperties.getProperties();
        assertNotNull(properties);
        assertEquals(2, properties.size());

        Map<String, String> kerberosProperties = (Map<String, String>) properties.get("kerberos");
        assertNotNull(kerberosProperties);

        Map<String, Object> trustProperties = (Map<String, Object>) properties.get("trust");
        assertNotNull(trustProperties);
        assertEquals(TEST_REALM.toUpperCase(Locale.ROOT), trustProperties.get("realm"));
        assertEquals(TEST_REALM.toLowerCase(Locale.ROOT), trustProperties.get("domain"));
    }

    @Test
    public void testCreateKerberosPillarWhenEnvironmentIsHybridButTrustResponseHasNoRealm() throws IOException {
        // GIVEN
        KerberosConfig kerberosConfig = KerberosConfig.KerberosConfigBuilder.aKerberosConfig()
                .withUrl(TEST_URL)
                .withRealm(TEST_REALM)
                .withVerifyKdcTrust(true)
                .withContainerDn(TEST_CONTAINER_DN)
                .build();
        DetailedEnvironmentResponse environmentResponse = createEnvironmentResponse(EnvironmentType.HYBRID.name());

        DescribeFreeIpaResponse freeIpaResponse = new DescribeFreeIpaResponse();
        TrustResponse trustResponse = new TrustResponse();
        freeIpaResponse.setTrust(trustResponse);

        when(kerberosDetailService.areClusterManagerManagedKerberosPackages(kerberosConfig)).thenReturn(true);
        when(kerberosDetailService.isClusterManagerManagedKrb5Config(kerberosConfig)).thenReturn(false);
        when(kerberosDetailService.resolveHostForKdcAdmin(kerberosConfig, TEST_URL)).thenReturn(TEST_ADMIN_URL);
        when(freeipaClient.findByEnvironmentCrn(TEST_CRN)).thenReturn(Optional.of(freeIpaResponse));

        // WHEN
        Map<String, SaltPillarProperties> result = underTest.createKerberosPillar(kerberosConfig, environmentResponse);

        // THEN
        assertNotNull(result);
        assertEquals(1, result.size());
        assertTrue(result.containsKey("kerberos"));

        SaltPillarProperties saltPillarProperties = result.get("kerberos");
        assertEquals("/kerberos/init.sls", saltPillarProperties.getPath());

        Map<String, Object> properties = saltPillarProperties.getProperties();
        assertNotNull(properties);
        assertEquals(2, properties.size());

        Map<String, String> kerberosProperties = (Map<String, String>) properties.get("kerberos");
        assertNotNull(kerberosProperties);

        Map<String, Object> trustProperties = (Map<String, Object>) properties.get("trust");
        assertNotNull(trustProperties);
        assertTrue(trustProperties.isEmpty());
    }

    @Test
    public void testCreateKerberosPillarWhenEnvironmentIsHybridButTrustResponseIsNull() throws IOException {
        // GIVEN
        KerberosConfig kerberosConfig = KerberosConfig.KerberosConfigBuilder.aKerberosConfig()
                .withUrl(TEST_URL)
                .withRealm(TEST_REALM)
                .withVerifyKdcTrust(true)
                .withContainerDn(TEST_CONTAINER_DN)
                .build();
        DetailedEnvironmentResponse environmentResponse = createEnvironmentResponse(EnvironmentType.HYBRID.name());

        DescribeFreeIpaResponse freeIpaResponse = new DescribeFreeIpaResponse();

        when(kerberosDetailService.areClusterManagerManagedKerberosPackages(kerberosConfig)).thenReturn(true);
        when(kerberosDetailService.isClusterManagerManagedKrb5Config(kerberosConfig)).thenReturn(false);
        when(kerberosDetailService.resolveHostForKdcAdmin(kerberosConfig, TEST_URL)).thenReturn(TEST_ADMIN_URL);
        when(freeipaClient.findByEnvironmentCrn(TEST_CRN)).thenReturn(Optional.of(freeIpaResponse));

        // WHEN
        Map<String, SaltPillarProperties> result = underTest.createKerberosPillar(kerberosConfig, environmentResponse);

        // THEN
        assertNotNull(result);
        assertEquals(1, result.size());
        assertTrue(result.containsKey("kerberos"));

        SaltPillarProperties saltPillarProperties = result.get("kerberos");
        assertEquals("/kerberos/init.sls", saltPillarProperties.getPath());

        Map<String, Object> properties = saltPillarProperties.getProperties();
        assertNotNull(properties);
        assertEquals(2, properties.size());

        Map<String, String> kerberosProperties = (Map<String, String>) properties.get("kerberos");
        assertNotNull(kerberosProperties);

        Map<String, Object> trustProperties = (Map<String, Object>) properties.get("trust");
        assertNotNull(trustProperties);
        assertTrue(trustProperties.isEmpty());
    }

    private DetailedEnvironmentResponse createEnvironmentResponse(String environmentType) {
        DetailedEnvironmentResponse response = new DetailedEnvironmentResponse();
        response.setCrn(TEST_CRN);
        response.setEnvironmentType(environmentType);
        return response;
    }
}