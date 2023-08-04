package com.sequenceiq.freeipa.service.image.userdata;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.ccm.cloudinit.CcmConnectivityMode;
import com.sequenceiq.cloudbreak.ccm.cloudinit.CcmConnectivityParameters;
import com.sequenceiq.cloudbreak.ccm.cloudinit.CcmParameterSupplier;
import com.sequenceiq.cloudbreak.ccm.cloudinit.CcmV2JumpgateParameterSupplier;
import com.sequenceiq.cloudbreak.ccm.cloudinit.CcmV2ParameterSupplier;
import com.sequenceiq.cloudbreak.ccm.cloudinit.DefaultCcmParameters;
import com.sequenceiq.cloudbreak.ccm.cloudinit.DefaultCcmV2JumpgateParameters;
import com.sequenceiq.cloudbreak.ccm.cloudinit.DefaultCcmV2Parameters;
import com.sequenceiq.cloudbreak.ccm.cloudinit.ServerParameters;
import com.sequenceiq.cloudbreak.common.service.HostDiscoveryService;
import com.sequenceiq.common.api.type.CcmV2TlsType;
import com.sequenceiq.common.api.type.Tunnel;
import com.sequenceiq.freeipa.entity.FreeIpa;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.client.CachedEnvironmentClientService;
import com.sequenceiq.freeipa.service.freeipa.FreeIpaService;
import com.sequenceiq.freeipa.service.stack.StackService;
import com.sequenceiq.freeipa.util.CrnService;

@ExtendWith(MockitoExtension.class)
class CcmUserDataServiceTest {

    private static final String TEST_ACCOUNT_ID = "accid";

    private static final String TEST_USER_CRN = String.format("crn:cdp:iam:us-west-1:%s:user:mockuser@cloudera.com", TEST_ACCOUNT_ID);

    private static final String TEST_ENVIRONMENT_ID = "aa8997d3-527d-4e7f-af8a-7f7cd10eb8f6";

    private static final String TEST_ENVIRONMENT_CRN = String.format("crn:cdp:iam:us-west-1:%s:environment:%s", TEST_ACCOUNT_ID, TEST_ENVIRONMENT_ID);

    private static final String TEST_AGENT_CRN = "stackAgentCrn";

    private static final String TEST_RESOURCE_ID = "aa8997d3-527d-4e7f-af8a-7f7cd10eb8f7";

    private static final String TEST_CLUSTER_CRN = String.format("crn:cdp:datahub:us-west-1:e7b1345f-4ae1-4594-9113-fc91f22ef8bd:cluster:%s", TEST_RESOURCE_ID);

    private static final String NEW_ACCESS_KEY_ID = "newAccessKeyId";

    private static final String NEW_ENCIPHERED_ACCESS_KEY = "newEncipheredAccessKey";

    private static final String NEW_INITIALISATION_VECTOR = "newInitialisationVector";

    private static final String NEW_HMAC_KEY = "newHmacKey";

    private static final String NEW_HMAC_FOR_PRIVATE_KEY = "newHmacForPrivateKey";

    private static final String MODIFIED_USER_DATA = """
            export CCM_V2_AGENT_ACCESS_KEY_ID="%s"
            export CCM_V2_AGENT_ENCIPHERED_ACCESS_KEY="%s"
            export CCM_V2_IV="%s"
            export CCM_V2_AGENT_HMAC_KEY="%s"
            export CCM_V2_AGENT_HMAC_FOR_PRIVATE_KEY="%s"
            export CCM_V2_INVERTING_PROXY_CERTIFICATE="userDataInvertingProxyCertificate"
            export CCM_V2_INVERTING_PROXY_HOST="userDataInvertingProxyHost"
            export CCM_V2_AGENT_CERTIFICATE="userDataAgentCertificate"
            export CCM_V2_AGENT_ENCIPHERED_KEY="userDataAgentEncipheredPrivateKey"
            export CCM_V2_AGENT_KEY_ID="userDataAgentKeyId"
            export CCM_V2_AGENT_CRN="userDataAgentCrn"
            """.formatted(NEW_ACCESS_KEY_ID, NEW_ENCIPHERED_ACCESS_KEY, NEW_INITIALISATION_VECTOR, NEW_HMAC_KEY, NEW_HMAC_FOR_PRIVATE_KEY);

    @InjectMocks
    private CcmUserDataService underTest;

    @Mock
    private CcmParameterSupplier ccmParameterSupplier;

    @Mock
    private CcmV2ParameterSupplier ccmV2ParameterSupplier;

    @Mock
    private CcmV2JumpgateParameterSupplier ccmV2JumpgateParameterSupplier;

    @Mock
    private FreeIpaService freeIpaService;

    @Mock
    private CrnService crnService;

    @Mock
    private HostDiscoveryService hostDiscoveryService;

    @Mock
    private StackService stackService;

    @Mock
    private CcmV2TlsTypeDecider ccmV2TlsTypeDecider;

    @Mock
    private CachedEnvironmentClientService environmentService;

    @Test
    void testFetchAndSaveCcmParametersWhenCcmTunnelNotEnabled() {
        Stack stack = getAStack();
        CcmConnectivityParameters ccmNotEnabled = underTest.fetchAndSaveCcmParameters(stack);
        assertEquals(CcmConnectivityMode.NONE, ccmNotEnabled.getConnectivityMode(), "CCM should not be enabled.");
    }

    @Test
    void testFetchAndSaveCcmParametersWhenCcmV1IsEnabled() {
        Stack stack = getAStack();
        stack.setTunnel(Tunnel.CCM);
        DefaultCcmParameters defaultCcmParameters = mock(DefaultCcmParameters.class);
        ServerParameters serverParameters = mock(ServerParameters.class);

        when(crnService.getUserCrn()).thenReturn(TEST_USER_CRN);
        when(ccmParameterSupplier.getCcmParameters(anyString(), anyString(), anyString(), anyMap())).thenReturn(Optional.of(defaultCcmParameters));
        when(defaultCcmParameters.getServerParameters()).thenReturn(serverParameters);
        when(serverParameters.getMinaSshdServiceId()).thenReturn("minaSshServiceId");
        when(stackService.getStackById(stack.getId())).thenReturn(stack);

        CcmConnectivityParameters ccmParameters = ThreadBasedUserCrnProvider.doAs(TEST_USER_CRN, () -> underTest.fetchAndSaveCcmParameters(stack));
        assertEquals(CcmConnectivityMode.CCMV1, ccmParameters.getConnectivityMode(), "CCM V1 should be enabled.");
        assertEquals(defaultCcmParameters, ccmParameters.getCcmParameters(), "CCM Parameters should match.");
        verify(ccmParameterSupplier, times(1)).getCcmParameters(anyString(), anyString(), anyString(), anyMap());
        verifyNoInteractions(ccmV2ParameterSupplier);
        assertEquals("minaSshServiceId", stack.getMinaSshdServiceId(), "Ccm V1 Config should be initialized");
        verify(stackService, times(1)).save(stack);
    }

    @Test
    void testFetchAndSaveCcmParametersWhenCcmV2IsEnabled() {
        Stack stack = getAStack();
        stack.setTunnel(Tunnel.CCMV2);
        DefaultCcmV2Parameters defaultCcmV2Parameters = mock(DefaultCcmV2Parameters.class);
        FreeIpa freeIpa = mock(FreeIpa.class);

        when(freeIpaService.findByStack(stack)).thenReturn(freeIpa);
        when(freeIpa.getDomain()).thenReturn("cldr.work.site");
        when(ccmV2ParameterSupplier.getCcmV2Parameters(anyString(), any(Optional.class), anyString(), anyString())).thenReturn(defaultCcmV2Parameters);
        when(defaultCcmV2Parameters.getAgentCrn()).thenReturn("testAgentCrn");
        when(hostDiscoveryService.determineGatewayFqdn(any(), any())).thenReturn("datahub.master0.cldr.work.site");

        CcmConnectivityParameters ccmParameters = ThreadBasedUserCrnProvider.doAs(TEST_USER_CRN, () -> underTest.fetchAndSaveCcmParameters(stack));
        assertEquals(CcmConnectivityMode.CCMV2, ccmParameters.getConnectivityMode(), "CCM V2 should be enabled.");
        assertEquals(defaultCcmV2Parameters, ccmParameters.getCcmV2Parameters(), "CCM V2 Parameters should match.");
        verify(ccmV2ParameterSupplier, times(1)).getCcmV2Parameters(anyString(), any(Optional.class), anyString(), anyString());
        verifyNoInteractions(ccmParameterSupplier);

        verify(stackService, times(1)).setCcmV2AgentCrnByStackId(100L, "testAgentCrn");
    }

    @Test
    void testFetchAndSaveCcmParametersWhenCcmV2JumpgateIsEnabled() {
        Stack stack = getAStack();
        stack.setTunnel(Tunnel.CCMV2_JUMPGATE);
        DefaultCcmV2JumpgateParameters defaultCcmV2JumpgateParameters = mock(DefaultCcmV2JumpgateParameters.class);
        FreeIpa freeIpa = mock(FreeIpa.class);

        when(freeIpaService.findByStack(stack)).thenReturn(freeIpa);
        when(freeIpa.getDomain()).thenReturn("cldr.work.site");
        when(ccmV2JumpgateParameterSupplier.getCcmV2JumpgateParameters(anyString(), any(Optional.class), anyString(), anyString(), any(Optional.class)))
                .thenReturn(defaultCcmV2JumpgateParameters);
        when(defaultCcmV2JumpgateParameters.getAgentCrn()).thenReturn("testAgentCrn");
        when(hostDiscoveryService.determineGatewayFqdn(any(), any())).thenReturn("datahub.master0.cldr.work.site");
        when(ccmV2TlsTypeDecider.decide(any())).thenReturn(CcmV2TlsType.ONE_WAY_TLS);
        when(stackService.getStackById(eq(stack.getId()))).thenReturn(stack);

        CcmConnectivityParameters ccmParameters = ThreadBasedUserCrnProvider.doAs(TEST_USER_CRN, () -> underTest.fetchAndSaveCcmParameters(stack));
        assertEquals(CcmConnectivityMode.CCMV2_JUMPGATE, ccmParameters.getConnectivityMode(), "CCM V2 Jumpgate should be enabled.");
        assertEquals(defaultCcmV2JumpgateParameters, ccmParameters.getCcmV2JumpgateParameters(), "CCM V2 Jumpgate Parameters should match.");
        verify(ccmV2JumpgateParameterSupplier, times(1)).getCcmV2JumpgateParameters(
                anyString(), any(Optional.class), anyString(), anyString(), any(Optional.class));
        verifyNoInteractions(ccmParameterSupplier);

        verify(stackService, times(1)).setCcmV2AgentCrnByStackId(100L, "testAgentCrn");
        verify(stackService, times(1)).save(stack);
    }

    @Test
    void testSaveOrUpdateStackCcmParametersWhenCcmConnectivityParametersIsNotNull() {
        Stack stack = getAStack();
        stack.setTunnel(Tunnel.CCMV2_JUMPGATE);
        stack.setCcmParameters(new CcmConnectivityParameters(new DefaultCcmV2JumpgateParameters("ccmInvertingProxyHost",
                "ccmInvertingProxyCertificate", "ccmAgentCrn", "ccmAgentKeyId", "ccmAgentEncipheredPrivateKey",
                "ccmAgentCertificate", "ccmEnvironmentCrn", "ccmAgentMachineUserAccessKey",
                "ccmAgentMachineUserEncipheredAccessKey", "ccmHmacKey", "ccmInitialisationVector",
                "ccmHmacForPrivateKey")));

        ClusterConnectivityManagementV2Proto.InvertingProxyAgent invertingProxyAgent = ClusterConnectivityManagementV2Proto.InvertingProxyAgent.newBuilder()
                .setAccessKeyId(NEW_ACCESS_KEY_ID)
                .setEncipheredAccessKey(NEW_ENCIPHERED_ACCESS_KEY)
                .setInitialisationVector(NEW_INITIALISATION_VECTOR)
                .setHmacForPrivateKey(NEW_HMAC_FOR_PRIVATE_KEY)
                .build();

        underTest.saveOrUpdateStackCcmParameters(stack, invertingProxyAgent, null, Optional.of(NEW_HMAC_KEY));
        ArgumentCaptor<Stack> stackCaptor = ArgumentCaptor.forClass(Stack.class);
        verify(stackService, times(1)).save(stackCaptor.capture());
        Stack savedStack = stackCaptor.getValue();
        assertEquals(NEW_ACCESS_KEY_ID, savedStack.getCcmParameters().getCcmV2JumpgateParameters().getAgentMachineUserAccessKey());
        assertEquals(NEW_ENCIPHERED_ACCESS_KEY, savedStack.getCcmParameters().getCcmV2JumpgateParameters().getAgentMachineUserEncipheredAccessKey());
        assertEquals(NEW_INITIALISATION_VECTOR, savedStack.getCcmParameters().getCcmV2JumpgateParameters().getInitialisationVector());
        assertEquals(NEW_HMAC_FOR_PRIVATE_KEY, savedStack.getCcmParameters().getCcmV2JumpgateParameters().getHmacForPrivateKey());
        assertEquals(NEW_HMAC_KEY, savedStack.getCcmParameters().getCcmV2JumpgateParameters().getHmacKey());
        assertEquals(TEST_ENVIRONMENT_CRN, savedStack.getCcmParameters().getCcmV2JumpgateParameters().getEnvironmentCrn());
        assertEquals("stackAgentCrn", savedStack.getCcmParameters().getCcmV2JumpgateParameters().getAgentCrn());
        assertEquals("ccmAgentCertificate", savedStack.getCcmParameters().getCcmV2JumpgateParameters().getAgentCertificate());
        assertEquals("ccmAgentEncipheredPrivateKey", savedStack.getCcmParameters().getCcmV2JumpgateParameters().getAgentEncipheredPrivateKey());
        assertEquals("ccmAgentKeyId", savedStack.getCcmParameters().getCcmV2JumpgateParameters().getAgentKeyId());
        assertEquals("ccmInvertingProxyCertificate", savedStack.getCcmParameters().getCcmV2JumpgateParameters().getInvertingProxyCertificate());
        assertEquals("ccmInvertingProxyHost", savedStack.getCcmParameters().getCcmV2JumpgateParameters().getInvertingProxyHost());
    }

    @Test
    void testSaveOrUpdateStackCcmParametersWhenCcmConnectivityParametersIsNull() {
        Stack stack = getAStack();
        stack.setTunnel(Tunnel.CCMV2_JUMPGATE);

        ClusterConnectivityManagementV2Proto.InvertingProxyAgent invertingProxyAgent = ClusterConnectivityManagementV2Proto.InvertingProxyAgent.newBuilder()
                .setAccessKeyId(NEW_ACCESS_KEY_ID)
                .setEncipheredAccessKey(NEW_ENCIPHERED_ACCESS_KEY)
                .setInitialisationVector(NEW_INITIALISATION_VECTOR)
                .setHmacForPrivateKey(NEW_HMAC_FOR_PRIVATE_KEY)
                .build();

        underTest.saveOrUpdateStackCcmParameters(stack, invertingProxyAgent, MODIFIED_USER_DATA, Optional.of(NEW_HMAC_KEY));
        ArgumentCaptor<Stack> stackCaptor = ArgumentCaptor.forClass(Stack.class);
        verify(stackService, times(1)).save(stackCaptor.capture());
        Stack savedStack = stackCaptor.getValue();
        assertEquals(NEW_ACCESS_KEY_ID, savedStack.getCcmParameters().getCcmV2JumpgateParameters().getAgentMachineUserAccessKey());
        assertEquals(NEW_ENCIPHERED_ACCESS_KEY, savedStack.getCcmParameters().getCcmV2JumpgateParameters().getAgentMachineUserEncipheredAccessKey());
        assertEquals(NEW_INITIALISATION_VECTOR, savedStack.getCcmParameters().getCcmV2JumpgateParameters().getInitialisationVector());
        assertEquals(NEW_HMAC_FOR_PRIVATE_KEY, savedStack.getCcmParameters().getCcmV2JumpgateParameters().getHmacForPrivateKey());
        assertEquals(NEW_HMAC_KEY, savedStack.getCcmParameters().getCcmV2JumpgateParameters().getHmacKey());
        assertEquals(TEST_ENVIRONMENT_CRN, savedStack.getCcmParameters().getCcmV2JumpgateParameters().getEnvironmentCrn());
        assertEquals("stackAgentCrn", savedStack.getCcmParameters().getCcmV2JumpgateParameters().getAgentCrn());
        assertEquals("userDataAgentCertificate", savedStack.getCcmParameters().getCcmV2JumpgateParameters().getAgentCertificate());
        assertEquals("userDataAgentEncipheredPrivateKey", savedStack.getCcmParameters().getCcmV2JumpgateParameters().getAgentEncipheredPrivateKey());
        assertEquals("userDataAgentKeyId", savedStack.getCcmParameters().getCcmV2JumpgateParameters().getAgentKeyId());
        assertEquals("userDataInvertingProxyCertificate", savedStack.getCcmParameters().getCcmV2JumpgateParameters().getInvertingProxyCertificate());
        assertEquals("userDataInvertingProxyHost", savedStack.getCcmParameters().getCcmV2JumpgateParameters().getInvertingProxyHost());
    }

    private Stack getAStack() {
        Stack aStack = new Stack();
        aStack.setId(100L);
        aStack.setAccountId(TEST_ACCOUNT_ID);
        aStack.setResourceCrn(TEST_CLUSTER_CRN);
        aStack.setEnvironmentCrn(TEST_ENVIRONMENT_CRN);
        aStack.setCcmV2AgentCrn(TEST_AGENT_CRN);
        return aStack;
    }
}

