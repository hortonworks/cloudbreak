package com.sequenceiq.cloudbreak.core.bootstrap.service.host.decorator;

import static com.sequenceiq.common.api.type.EnvironmentType.PUBLIC_CLOUD;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

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
import com.sequenceiq.common.api.type.EnvironmentType;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.describe.DescribeFreeIpaResponse;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.describe.TrustResponse;

@ExtendWith(MockitoExtension.class)
class NameserverPillarDecoratorTest {

    private static final String SERVICE_KEY = "forwarder-zones";

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
    void testShouldPopulateThePillarWhenKerberosConfigIsAvailable() {
        KerberosConfig kerberosConfig = KerberosConfig.KerberosConfigBuilder.aKerberosConfig()
                .withDomain(DOMAIN)
                .withNameServers(String.join(",", "null", KERBEROS_NAMESERVER_1, KERBEROS_NAMESERVER_2))
                .build();

        Map<String, SaltPillarProperties> servicePillar = underTest.createPillarForNameservers(kerberosConfig, ENVIRONMENT_CRN, PUBLIC_CLOUD.name());

        SaltPillarProperties pillarProperties = servicePillar.get(SERVICE_KEY);
        Map<String, Map<String, List<String>>> nameservers = (Map<String, Map<String, List<String>>>) pillarProperties.getProperties().get(SERVICE_KEY);
        assertNotNull(pillarProperties);
        assertEquals(SERVICE_PATH, pillarProperties.getPath());
        assertTrue(nameservers.get(kerberosConfig.getDomain()).get(NAMESERVERS_KEY).contains(KERBEROS_NAMESERVER_1));
        assertTrue(nameservers.get(kerberosConfig.getDomain()).get(NAMESERVERS_KEY).contains(KERBEROS_NAMESERVER_2));
        assertEquals(2, nameservers.get(kerberosConfig.getDomain()).get(NAMESERVERS_KEY).size());

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
        when(freeipaClient.getByEnvironmentCrn(ENVIRONMENT_CRN)).thenReturn(freeIpaResponse);

        Map<String, SaltPillarProperties> servicePillar = underTest.createPillarForNameservers(kerberosConfig, ENVIRONMENT_CRN, EnvironmentType.HYBRID.name());

        SaltPillarProperties pillarProperties = servicePillar.get(SERVICE_KEY);
        Map<String, Map<String, List<String>>> nameservers = (Map<String, Map<String, List<String>>>) pillarProperties.getProperties().get(SERVICE_KEY);
        assertNotNull(pillarProperties);
        assertEquals(SERVICE_PATH, pillarProperties.getPath());
        assertTrue(nameservers.get(kerberosConfig.getDomain()).get(NAMESERVERS_KEY).contains(KERBEROS_NAMESERVER_1));
        assertTrue(nameservers.get(kerberosConfig.getDomain()).get(NAMESERVERS_KEY).contains(KERBEROS_NAMESERVER_2));
        assertTrue(nameservers.get("ad.domain").get(NAMESERVERS_KEY).contains(KERBEROS_NAMESERVER_1));
        assertTrue(nameservers.get("ad.domain").get(NAMESERVERS_KEY).contains(KERBEROS_NAMESERVER_2));

        assertEquals(2, nameservers.get("ad.domain").get(NAMESERVERS_KEY).size());
        assertEquals(2, nameservers.get(kerberosConfig.getDomain()).get(NAMESERVERS_KEY).size());
    }

    @Test
    void testShouldThrowExceptionThenKerberosNameserverIpListIsEmpty() {
        KerberosConfig kerberosConfig = KerberosConfig.KerberosConfigBuilder.aKerberosConfig()
                .withDomain(DOMAIN)
                .withNameServers(String.join(",", "null", null))
                .build();

        Exception exception = assertThrows(CloudbreakServiceException.class,
                () -> underTest.createPillarForNameservers(kerberosConfig, ENVIRONMENT_CRN, PUBLIC_CLOUD.name()));

        assertEquals("Unable to setup nameservers because there is no IP address present.", exception.getMessage());
    }

    @Test
    void testShouldNotPopulateServicePillarWhenKerberosTypeIsFreeipaAndKerberosNameserverIsMissing() {
        KerberosConfig kerberosConfig = KerberosConfig.KerberosConfigBuilder.aKerberosConfig().withType(KerberosType.FREEIPA).build();

        Map<String, SaltPillarProperties> servicePillar = underTest.createPillarForNameservers(kerberosConfig, ENVIRONMENT_CRN, PUBLIC_CLOUD.name());

        assertTrue(servicePillar.isEmpty());
    }

}