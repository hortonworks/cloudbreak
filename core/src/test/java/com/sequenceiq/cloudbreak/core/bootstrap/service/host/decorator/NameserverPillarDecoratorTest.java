package com.sequenceiq.cloudbreak.core.bootstrap.service.host.decorator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.dto.KerberosConfig;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.orchestrator.model.SaltPillarProperties;
import com.sequenceiq.cloudbreak.service.sharedservice.DatalakeService;
import com.sequenceiq.cloudbreak.type.KerberosType;
import com.sequenceiq.cloudbreak.view.StackView;

@ExtendWith(MockitoExtension.class)
class NameserverPillarDecoratorTest {

    private static final String SERVICE_KEY = "forwarder-zones";

    private static final String SERVICE_PATH = "/unbound/forwarders.sls";

    private static final String KERBEROS_NAMESERVER_1 = "1.1.1.1";

    private static final String KERBEROS_NAMESERVER_2 = "2.2.2.2";

    private static final String DL_PRIVATE_IP = "3.3.3.3";

    private static final String NAMESERVERS_KEY = "nameservers";

    private static final String DOMAIN = "cloudera.com";

    @InjectMocks
    private NameserverPillarDecorator underTest;

    @Mock
    private DatalakeService datalakeService;

    @Mock
    private StackDto stackDto;

    @Mock
    private StackView stack;

    @Mock
    private Stack datalakeStack;

    @Test
    void testShouldPopulateThePillarWhenKerberosConfigIsAvailable() {
        Map<String, SaltPillarProperties> servicePillar = new HashMap<>();
        KerberosConfig kerberosConfig = KerberosConfig.KerberosConfigBuilder.aKerberosConfig()
                .withDomain(DOMAIN)
                .withNameServers(String.join(",", "null", KERBEROS_NAMESERVER_1, KERBEROS_NAMESERVER_2))
                .build();
        underTest.decorateServicePillarWithNameservers(stackDto, kerberosConfig, servicePillar);

        SaltPillarProperties pillarProperties = servicePillar.get(SERVICE_KEY);
        Map<String, Map<String, List<String>>> nameservers = (Map<String, Map<String, List<String>>>) pillarProperties.getProperties().get(SERVICE_KEY);
        assertNotNull(pillarProperties);
        assertEquals(SERVICE_PATH, pillarProperties.getPath());
        assertTrue(nameservers.get(kerberosConfig.getDomain()).get(NAMESERVERS_KEY).contains(KERBEROS_NAMESERVER_1));
        assertTrue(nameservers.get(kerberosConfig.getDomain()).get(NAMESERVERS_KEY).contains(KERBEROS_NAMESERVER_2));
        assertEquals(2, nameservers.get(kerberosConfig.getDomain()).get(NAMESERVERS_KEY).size());
        verifyNoInteractions(datalakeService);
    }

    @Test
    void testShouldThrowExceptionThenKerberosNameserverIpListIsEmpty() {
        Map<String, SaltPillarProperties> servicePillar = new HashMap<>();
        KerberosConfig kerberosConfig = KerberosConfig.KerberosConfigBuilder.aKerberosConfig()
                .withDomain(DOMAIN)
                .withNameServers(String.join(",", "null", null))
                .build();

        Exception exception = assertThrows(CloudbreakServiceException.class,
                () -> underTest.decorateServicePillarWithNameservers(stackDto, kerberosConfig, servicePillar));

        assertEquals("Unable to setup nameservers because there is no IP address present.", exception.getMessage());
        assertTrue(servicePillar.isEmpty());
    }

    @Test
    void testShouldPopulateThePillarWithDatalakeNameserverWhenKerberosConfigIsNotPresent() {
        Map<String, SaltPillarProperties> servicePillar = new HashMap<>();

        when(stackDto.getStack()).thenReturn(stack);
        when(datalakeService.getDatalakeStackByDatahubStack(stack)).thenReturn(Optional.of(datalakeStack));
        when(datalakeStack.getNotTerminatedAndNotZombieGatewayInstanceMetadata()).thenReturn(Collections.singletonList(createInstanceMetaData(DL_PRIVATE_IP)));

        underTest.decorateServicePillarWithNameservers(stackDto, null, servicePillar);

        SaltPillarProperties pillarProperties = servicePillar.get(SERVICE_KEY);
        Map<String, Map<String, List<String>>> nameservers = (Map<String, Map<String, List<String>>>) pillarProperties.getProperties().get(SERVICE_KEY);
        assertNotNull(pillarProperties);
        assertEquals(SERVICE_PATH, pillarProperties.getPath());
        assertTrue(nameservers.get(DOMAIN).get(NAMESERVERS_KEY).contains(DL_PRIVATE_IP));
        verify(datalakeService).getDatalakeStackByDatahubStack(stack);
    }

    @Test
    void testShouldThrowExceptionWhenDatalakeNameserverAddressIsNull() {
        Map<String, SaltPillarProperties> servicePillar = new HashMap<>();

        when(stackDto.getStack()).thenReturn(stack);
        when(datalakeService.getDatalakeStackByDatahubStack(stack)).thenReturn(Optional.of(datalakeStack));
        when(datalakeStack.getNotTerminatedAndNotZombieGatewayInstanceMetadata()).thenReturn(Collections.singletonList(createInstanceMetaData(null)));

        Exception exception = assertThrows(CloudbreakServiceException.class,
                () -> underTest.decorateServicePillarWithNameservers(stackDto, null, servicePillar));

        assertEquals("Unable to setup nameservers because there is no IP address present.", exception.getMessage());
        assertTrue(servicePillar.isEmpty());
        verify(datalakeService).getDatalakeStackByDatahubStack(stack);
    }

    @Test
    void testShouldNotPopulateServicePillarWhenKerberosTypeIsFreeipaAndKerberosNameserverIsMissing() {
        Map<String, SaltPillarProperties> servicePillar = new HashMap<>();
        KerberosConfig kerberosConfig = KerberosConfig.KerberosConfigBuilder.aKerberosConfig().withType(KerberosType.FREEIPA).build();

        underTest.decorateServicePillarWithNameservers(stackDto, kerberosConfig, servicePillar);

        assertTrue(servicePillar.isEmpty());
        verifyNoInteractions(datalakeService);
    }

    @Test
    void testShouldNotPopulateServicePillarWhenKerberosSettingsAreMissingAndDatalakeIsNotPresent() {
        Map<String, SaltPillarProperties> servicePillar = new HashMap<>();
        when(stackDto.getStack()).thenReturn(stack);
        when(datalakeService.getDatalakeStackByDatahubStack(stack)).thenReturn(Optional.empty());

        underTest.decorateServicePillarWithNameservers(stackDto, null, servicePillar);

        assertTrue(servicePillar.isEmpty());
        verify(datalakeService).getDatalakeStackByDatahubStack(stack);
    }

    private InstanceMetaData createInstanceMetaData(String privateIp) {
        InstanceMetaData instanceMetaData = new InstanceMetaData();
        instanceMetaData.setDiscoveryFQDN("test." + DOMAIN);
        instanceMetaData.setPrivateIp(privateIp);
        return instanceMetaData;
    }

}