package com.sequenceiq.cloudbreak.core.bootstrap.service.host;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.dto.KerberosConfig;
import com.sequenceiq.cloudbreak.orchestrator.model.SaltPillarProperties;
import com.sequenceiq.cloudbreak.service.freeipa.FreeIpaConfigProvider;
import com.sequenceiq.cloudbreak.template.kerberos.KerberosDetailService;

@ExtendWith(MockitoExtension.class)
class SssdConfigProviderTest {

    private static final int ENTRY_CACHE_TIMEOUT = 300;

    private static final int MEMCACHE_TIMEOUT = 400;

    private static final int HEARTBEAT_TIMEOUT = 60;

    private static final int NSS_TIMEOUT = 30;

    private static final int DNS_TTL = 30;

    private static final String ENV_CRN = "crn:cdp:environments:us-west-1:default:environment:f7563fc1-e8ff-486a-9260-4e54ccabbaa0";

    @Mock
    private FreeIpaConfigProvider freeIpaConfigProvider;

    @Mock
    private KerberosDetailService kerberosDetailService;

    @Mock
    private EntitlementService entitlementService;

    @InjectMocks
    private SssdConfigProvider underTest;

    @BeforeEach
    public void init() {
        ReflectionTestUtils.setField(underTest, "entryCacheTimeout", ENTRY_CACHE_TIMEOUT);
        ReflectionTestUtils.setField(underTest, "memcacheTimeout", MEMCACHE_TIMEOUT);
        ReflectionTestUtils.setField(underTest, "heartbeatTimeout", HEARTBEAT_TIMEOUT);
        ReflectionTestUtils.setField(underTest, "nssTimeout", NSS_TIMEOUT);
    }

    @Test
    public void testCreateIpaPillarNonIpaJoinable() {
        KerberosConfig kerberosConfig = new KerberosConfig();
        when(kerberosDetailService.isIpaJoinable(kerberosConfig)).thenReturn(Boolean.FALSE);

        Map<String, SaltPillarProperties> result = underTest.createSssdIpaPillar(kerberosConfig, Map.of(),
                "", "7.2.11", "");

        assertTrue(result.isEmpty());
    }

    @Test
    public void testCreateIpaPillarIpaJoinableWOEnumerate() {
        KerberosConfig kerberosConfig = createKerberosConfig();
        when(kerberosDetailService.isIpaJoinable(kerberosConfig)).thenReturn(Boolean.TRUE);
        when(kerberosDetailService.getDnsTtl()).thenReturn(DNS_TTL);
        Map<String, Object> freeIpaConfig = Map.of("random", "stuff");
        when(freeIpaConfigProvider.createFreeIpaConfig(ENV_CRN)).thenReturn(freeIpaConfig);

        Map<String, SaltPillarProperties> result = underTest.createSssdIpaPillar(kerberosConfig, Map.of(),
                ENV_CRN, "7.2.11", "");

        assertGenericConfig(result, kerberosConfig, freeIpaConfig);
        assertFalse((Boolean) ((Map<String, Object>) result.get("sssd-ipa").getProperties().get("sssd-ipa")).get("enumerate"));
    }

    static Object[][] scenarios() {
        return new Object[][]{
                {
                        "Empty Services 7.2.11",
                        Map.of("RANGER_ADMIN", List.of(), "NIFI_REGISTRY_SERVER", List.of(), "NIFI_NODE", List.of()),
                        false,
                        "7.2.11",
                        "",
                        false
                },
                {
                        "Ranger present 7.2.11",
                        Map.of("RANGER_ADMIN", List.of("asf"), "NIFI_REGISTRY_SERVER", List.of(), "NIFI_NODE", List.of()),
                        true,
                        "7.2.11",
                        "",
                        false
                },
                {
                        "Ranger present alone 7.2.11",
                        Map.of("RANGER_ADMIN", List.of("asf")),
                        true,
                        "7.2.11",
                        "",
                        false
                },
                {
                        "NifiNode present 7.2.11",
                        Map.of("RANGER_ADMIN", List.of(), "NIFI_REGISTRY_SERVER", List.of(), "NIFI_NODE", List.of("asf")),
                        true,
                        "7.2.11",
                        "",
                        false
                },
                {
                        "NifiNode present alone 7.2.11",
                        Map.of("NIFI_NODE", List.of("asf")),
                        true,
                        "7.2.11",
                        "",
                        false
                },
                {
                        "NifiRegistry present 7.2.11",
                        Map.of("RANGER_ADMIN", List.of(), "NIFI_REGISTRY_SERVER", List.of("asf"), "NIFI_NODE", List.of()),
                        true,
                        "7.2.11",
                        "",
                        false
                },
                {
                        "NifiRegistry present alone 7.2.11",
                        Map.of("NIFI_REGISTRY_SERVER", List.of("asf")),
                        true,
                        "7.2.11",
                        "",
                        false
                },
                {
                        "All present 7.2.11",
                        Map.of("RANGER_ADMIN", List.of("asf"), "NIFI_REGISTRY_SERVER", List.of("asf"), "NIFI_NODE", List.of("asf")),
                        true,
                        "7.2.11",
                        "",
                        false
                },
                {
                        "Empty Services 7.2.11",
                        Map.of("RANGER_ADMIN", List.of()),
                        false,
                        "7.2.11",
                        "",
                        false
                },
                {
                        "Ranger present 7.2.11",
                        Map.of("RANGER_ADMIN", List.of("asf")),
                        true,
                        "7.2.11",
                        "",
                        false
                },



                {
                        "Ranger present alone 7.2.18",
                        Map.of("RANGER_ADMIN", List.of("asf")),
                        true,
                        "7.2.18",
                        "",
                        false
                },
                {
                        "NifiNode present 7.2.18",
                        Map.of("RANGER_ADMIN", List.of(), "NIFI_REGISTRY_SERVER", List.of(), "NIFI_NODE", List.of("asf")),
                        false,
                        "7.2.18",
                        "",
                        false
                },
                {
                        "NifiNode present alone 7.2.18",
                        Map.of("NIFI_NODE", List.of("asf")),
                        false,
                        "7.2.18",
                        "",
                        false
                },
                {
                        "NifiRegistry present 7.2.18",
                        Map.of("RANGER_ADMIN", List.of(), "NIFI_REGISTRY_SERVER", List.of("asf"), "NIFI_NODE", List.of()),
                        false,
                        "7.2.18",
                        "",
                        false
                },
                {
                        "NifiRegistry present alone 7.2.18",
                        Map.of("NIFI_REGISTRY_SERVER", List.of("asf")),
                        false,
                        "7.2.18",
                        "",
                        false
                },
                {
                        "All present 7.2.18",
                        Map.of("RANGER_ADMIN", List.of("asf"), "NIFI_REGISTRY_SERVER", List.of("asf"), "NIFI_NODE", List.of("asf")),
                        true,
                        "7.2.18",
                        "",
                        false
                },
                {
                        "Empty Services 7.2.18",
                        Map.of("RANGER_ADMIN", List.of()),
                        false,
                        "7.2.18",
                        "",
                        false
                },
                {
                        "Ranger present 7.2.18",
                        Map.of("RANGER_ADMIN", List.of("asf")),
                        true,
                        "7.2.18",
                        "",
                        false
                },
                {
                        "Ranger present 7.2.18, LDAP enabled",
                        Map.of("RANGER_ADMIN", List.of("asf")),
                        false,
                        "7.2.18",
                        "org.apache.ranger.ldapusersync.process.LdapUserGroupBuilder",
                        true
                },
                {
                        "Ranger present 7.2.18, LDAP enabled",
                        Map.of("RANGER_ADMIN", List.of("asf")),
                        false,
                        "7.2.18",
                        null,
                        true
                },
        };
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("scenarios")
    public void testCreateIpaPillarIpaJoinableWithEnumerateCheck(String name, Map<String, List<String>> serviceLocations,
            boolean expectedEnumerate, String version, String extendedBlueprint, boolean rangerLdapUsersyncEnabled) {
        KerberosConfig kerberosConfig = createKerberosConfig();
        when(kerberosDetailService.isIpaJoinable(kerberosConfig)).thenReturn(Boolean.TRUE);
        when(kerberosDetailService.getDnsTtl()).thenReturn(DNS_TTL);
        Map<String, Object> freeIpaConfig = Map.of("random", "stuff");
        when(freeIpaConfigProvider.createFreeIpaConfig(ENV_CRN)).thenReturn(freeIpaConfig);
        lenient().when(entitlementService.isRangerLdapUsersyncEnabled(any())).thenReturn(rangerLdapUsersyncEnabled);

        Map<String, SaltPillarProperties> result = underTest.createSssdIpaPillar(kerberosConfig, serviceLocations,
                ENV_CRN, version, extendedBlueprint);

        assertGenericConfig(result, kerberosConfig, freeIpaConfig);
        assertEquals(expectedEnumerate, ((Map<String, Object>) result.get("sssd-ipa").getProperties().get("sssd-ipa")).get("enumerate"));
    }

    private void assertGenericConfig(Map<String, SaltPillarProperties> result, KerberosConfig kerberosConfig, Map<String, Object> freeIpaConfig) {
        SaltPillarProperties pillarProperties = result.get("sssd-ipa");
        assertEquals("/sssd/ipa.sls", pillarProperties.getPath());
        Map<String, Object> properties = pillarProperties.getProperties();

        Map<String, Object> sssdConfig = (Map<String, Object>) properties.get("sssd-ipa");
        assertEquals(kerberosConfig.getPrincipal(), sssdConfig.get("principal"));
        assertEquals(kerberosConfig.getRealm().toUpperCase(Locale.ROOT), sssdConfig.get("realm"));
        assertEquals(kerberosConfig.getDomain(), sssdConfig.get("domain"));
        assertEquals(kerberosConfig.getPassword(), sssdConfig.get("password"));
        assertEquals(kerberosConfig.getUrl(), sssdConfig.get("server"));
        assertEquals(DNS_TTL, sssdConfig.get("dns_ttl"));
        assertEquals(ENTRY_CACHE_TIMEOUT, sssdConfig.get("entryCacheTimeout"));
        assertEquals(MEMCACHE_TIMEOUT, sssdConfig.get("memcacheTimeout"));
        assertEquals(HEARTBEAT_TIMEOUT, sssdConfig.get("heartbeatTimeout"));
        assertEquals(NSS_TIMEOUT, sssdConfig.get("nssTimeout"));

        Map<String, Object> freeIpaConfigResult = (Map<String, Object>) properties.get("freeipa");
        assertEquals(freeIpaConfig, freeIpaConfigResult);
    }

    private KerberosConfig createKerberosConfig() {
        return KerberosConfig.KerberosConfigBuilder.aKerberosConfig()
                .withPrincipal("principal")
                .withRealm("realm")
                .withDomain("domain")
                .withPassword("pass")
                .withUrl("url")
                .build();
    }
}