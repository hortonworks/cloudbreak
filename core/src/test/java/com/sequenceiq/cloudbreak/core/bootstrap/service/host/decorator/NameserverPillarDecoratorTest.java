package com.sequenceiq.cloudbreak.core.bootstrap.service.host.decorator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.dto.KerberosConfig;
import com.sequenceiq.cloudbreak.orchestrator.model.SaltPillarProperties;
import com.sequenceiq.cloudbreak.service.freeipa.FreeipaClientService;
import com.sequenceiq.cloudbreak.type.KerberosType;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.describe.DescribeFreeIpaResponse;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.describe.TrustResponse;

@ExtendWith(MockitoExtension.class)
class NameserverPillarDecoratorTest {

    private static final String FORWARDER_ZONES = "forwarder-zones";

    private static final String DEFAULT_REVERSE_ZONE = "default-reverse-zone";

    private static final String SERVICE_PATH = "/unbound/forwarders.sls";

    private static final String KERBEROS_NAMESERVER_1 = "1.1.1.1";

    private static final String KERBEROS_NAMESERVER_2 = "2.2.2.2";

    private static final String DL_PRIVATE_IP = "3.3.3.3";

    private static final String NAMESERVERS_KEY = "nameservers";

    private static final String DOMAIN = "cloudera.com";

    private static final String ENVIRONMENT_CRN = "envCrn";

    @Mock
    private FreeipaClientService freeipaClient;

    @InjectMocks
    private NameserverPillarDecorator underTest;

    @Test
    void testNullKerberosConfig() {
        Map<String, SaltPillarProperties> result = underTest.createPillarForNameservers(null, ENVIRONMENT_CRN);
        assertEquals(0, result.size());
    }

    @Test
    void testShouldPopulateThePillarWhenKerberosConfigIsAvailable() {
        KerberosConfig kerberosConfig = KerberosConfig.KerberosConfigBuilder.aKerberosConfig()
                .withDomain(DOMAIN)
                .withNameServers(String.join(",", "null", KERBEROS_NAMESERVER_1, KERBEROS_NAMESERVER_2))
                .build();

        Map<String, SaltPillarProperties> servicePillar = underTest.createPillarForNameservers(kerberosConfig, ENVIRONMENT_CRN);

        SaltPillarProperties pillarProperties = servicePillar.get(FORWARDER_ZONES);
        Map<String, Map<String, List<String>>> nameservers = getForwarderZones(pillarProperties);
        Map<String, List<String>> defaultReverseZone = getDefaultReverseZone(pillarProperties);
        assertNotNull(pillarProperties);
        assertEquals(SERVICE_PATH, pillarProperties.getPath());
        assertTrue(nameservers.get(kerberosConfig.getDomain()).get(NAMESERVERS_KEY).contains(KERBEROS_NAMESERVER_1));
        assertTrue(nameservers.get(kerberosConfig.getDomain()).get(NAMESERVERS_KEY).contains(KERBEROS_NAMESERVER_2));
        assertEquals(2, nameservers.get(kerberosConfig.getDomain()).get(NAMESERVERS_KEY).size());
        assertTrue(defaultReverseZone.isEmpty());
    }

    @Test
    void testShouldPopulateThePillarWhenKerberosConfigIsAvailableForHybrid() {
        KerberosConfig kerberosConfig = KerberosConfig.KerberosConfigBuilder.aKerberosConfig()
                .withDomain(DOMAIN)
                .withNameServers(String.join(",", "null", KERBEROS_NAMESERVER_1, KERBEROS_NAMESERVER_2))
                .build();
        DescribeFreeIpaResponse freeIpaResponse = new DescribeFreeIpaResponse();
        TrustResponse trust = new TrustResponse();
        trust.setRealm("AD.DOMAIN");
        freeIpaResponse.setTrust(trust);
        when(freeipaClient.findByEnvironmentCrn(ENVIRONMENT_CRN)).thenReturn(Optional.of(freeIpaResponse));

        Map<String, SaltPillarProperties> servicePillar = underTest.createPillarForNameservers(kerberosConfig, ENVIRONMENT_CRN);

        SaltPillarProperties pillarProperties = servicePillar.get(FORWARDER_ZONES);
        Map<String, Map<String, List<String>>> nameservers = getForwarderZones(pillarProperties);
        Map<String, List<String>> defaultReverseZone = getDefaultReverseZone(pillarProperties);
        assertNotNull(pillarProperties);
        assertEquals(SERVICE_PATH, pillarProperties.getPath());
        assertTrue(nameservers.get(kerberosConfig.getDomain()).get(NAMESERVERS_KEY).contains(KERBEROS_NAMESERVER_1));
        assertTrue(nameservers.get(kerberosConfig.getDomain()).get(NAMESERVERS_KEY).contains(KERBEROS_NAMESERVER_2));
        assertTrue(nameservers.get("ad.domain").get(NAMESERVERS_KEY).contains(KERBEROS_NAMESERVER_1));
        assertTrue(nameservers.get("ad.domain").get(NAMESERVERS_KEY).contains(KERBEROS_NAMESERVER_2));

        assertEquals(2, nameservers.get("ad.domain").get(NAMESERVERS_KEY).size());
        assertEquals(2, nameservers.get(kerberosConfig.getDomain()).get(NAMESERVERS_KEY).size());
        assertTrue(defaultReverseZone.get(NAMESERVERS_KEY).containsAll(Set.of(KERBEROS_NAMESERVER_1, KERBEROS_NAMESERVER_2)));
    }

    @Test
    void testShouldNotFailPopulateThePillarWhenNoFreeipaIsAvailableForHybrid() {
        KerberosConfig kerberosConfig = KerberosConfig.KerberosConfigBuilder.aKerberosConfig()
                .withDomain(DOMAIN)
                .withNameServers(String.join(",", "null", KERBEROS_NAMESERVER_1, KERBEROS_NAMESERVER_2))
                .build();
        when(freeipaClient.findByEnvironmentCrn(ENVIRONMENT_CRN)).thenReturn(Optional.empty());

        Map<String, SaltPillarProperties> servicePillar = underTest.createPillarForNameservers(kerberosConfig, ENVIRONMENT_CRN);

        SaltPillarProperties pillarProperties = servicePillar.get(FORWARDER_ZONES);
        Map<String, Map<String, List<String>>> nameservers = getForwarderZones(pillarProperties);
        assertNotNull(pillarProperties);
        assertEquals(SERVICE_PATH, pillarProperties.getPath());
        assertTrue(nameservers.get(kerberosConfig.getDomain()).get(NAMESERVERS_KEY).contains(KERBEROS_NAMESERVER_1));
        assertTrue(nameservers.get(kerberosConfig.getDomain()).get(NAMESERVERS_KEY).contains(KERBEROS_NAMESERVER_2));
        assertEquals(2, nameservers.get(kerberosConfig.getDomain()).get(NAMESERVERS_KEY).size());
    }

    @Test
    void testShouldThrowExceptionThenKerberosNameserverIpListIsEmpty() {
        KerberosConfig kerberosConfig = KerberosConfig.KerberosConfigBuilder.aKerberosConfig()
                .withDomain(DOMAIN)
                .withNameServers(String.join(",", "null", null))
                .build();

        Exception exception = assertThrows(CloudbreakServiceException.class,
                () -> underTest.createPillarForNameservers(kerberosConfig, ENVIRONMENT_CRN));

        assertEquals("Unable to setup nameservers because there is no IP address present.", exception.getMessage());
    }

    @Test
    void testShouldNotPopulateServicePillarWhenKerberosTypeIsFreeipaAndKerberosNameserverIsMissing() {
        KerberosConfig kerberosConfig = KerberosConfig.KerberosConfigBuilder.aKerberosConfig().withType(KerberosType.FREEIPA).build();

        Map<String, SaltPillarProperties> servicePillar = underTest.createPillarForNameservers(kerberosConfig, ENVIRONMENT_CRN);

        assertTrue(servicePillar.isEmpty());
    }

    private static Map<String, List<String>> getDefaultReverseZone(SaltPillarProperties pillarProperties) {
        return (Map<String, List<String>>) pillarProperties.getProperties().get(DEFAULT_REVERSE_ZONE);
    }

    private static Map<String, Map<String, List<String>>> getForwarderZones(SaltPillarProperties pillarProperties) {
        return (Map<String, Map<String, List<String>>>) pillarProperties.getProperties().get(FORWARDER_ZONES);
    }

}