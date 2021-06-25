package com.sequenceiq.freeipa.service.image.userdata;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.ccm.cloudinit.CcmConnectivityMode;
import com.sequenceiq.cloudbreak.ccm.cloudinit.CcmConnectivityParameters;
import com.sequenceiq.cloudbreak.ccm.cloudinit.CcmParameterSupplier;
import com.sequenceiq.cloudbreak.ccm.cloudinit.CcmV2JumpgateParameterSupplier;
import com.sequenceiq.cloudbreak.ccm.cloudinit.CcmV2ParameterSupplier;
import com.sequenceiq.cloudbreak.ccm.cloudinit.DefaultCcmParameters;
import com.sequenceiq.cloudbreak.ccm.cloudinit.DefaultCcmV2Parameters;
import com.sequenceiq.cloudbreak.ccm.cloudinit.ServerParameters;
import com.sequenceiq.cloudbreak.common.service.HostDiscoveryService;
import com.sequenceiq.common.api.type.Tunnel;
import com.sequenceiq.freeipa.entity.FreeIpa;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.freeipa.FreeIpaService;
import com.sequenceiq.freeipa.service.stack.StackService;
import com.sequenceiq.freeipa.util.CrnService;

@RunWith(MockitoJUnitRunner.class)
public class CcmUserDataServiceTest {

    private static final String TEST_ACCOUNT_ID = "accid";

    private static final String TEST_USER_CRN = String.format("crn:cdp:iam:us-west-1:%s:user:mockuser@cloudera.com", TEST_ACCOUNT_ID);

    private static final String TEST_ENVIRONMENT_ID = "aa8997d3-527d-4e7f-af8a-7f7cd10eb8f6";

    private static final String TEST_ENVIRONMENT_CRN = String.format("crn:cdp:iam:us-west-1:%s:environment:%s", TEST_ACCOUNT_ID, TEST_ENVIRONMENT_ID);

    private static final String TEST_RESOURCE_ID = "aa8997d3-527d-4e7f-af8a-7f7cd10eb8f7";

    private static final String TEST_CLUSTER_CRN = String.format("crn:cdp:datahub:us-west-1:e7b1345f-4ae1-4594-9113-fc91f22ef8bd:cluster:%s", TEST_RESOURCE_ID);

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

    @Test
    public void testFetchAndSaveCcmParametersWhenCcmTunnelNotEnabled() {
        Stack stack = getAStack();
        CcmConnectivityParameters ccmNotEnabled = underTest.fetchAndSaveCcmParameters(stack);
        assertEquals(CcmConnectivityMode.NONE, ccmNotEnabled.getConnectivityMode(), "CCM should not be enabled.");
    }

    @Test
    public void testFetchAndSaveCcmParametersWhenCcmV1IsEnabled() {
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
    public void testFetchAndSaveCcmParametersWhenCcmV2IsEnabled() {
        Stack stack = getAStack();
        stack.setTunnel(Tunnel.CCMV2);
        DefaultCcmV2Parameters defaultCcmV2Parameters = mock(DefaultCcmV2Parameters.class);
        FreeIpa freeIpa = mock(FreeIpa.class);

        when(stackService.getStackById(stack.getId())).thenReturn(stack);
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

        assertEquals("testAgentCrn", stack.getCcmV2AgentCrn(), "Ccm V2 Config should be initialized");
        verify(stackService, times(1)).save(stack);
    }

    @Test
    public void testFetchAndSaveCcmParametersWhenCcmV2JumpgateIsEnabled() {
        Stack stack = getAStack();
        stack.setTunnel(Tunnel.CCMV2_JUMPGATE);
        DefaultCcmV2Parameters defaultCcmV2Parameters = mock(DefaultCcmV2Parameters.class);
        FreeIpa freeIpa = mock(FreeIpa.class);

        when(stackService.getStackById(stack.getId())).thenReturn(stack);
        when(freeIpaService.findByStack(stack)).thenReturn(freeIpa);
        when(freeIpa.getDomain()).thenReturn("cldr.work.site");
        when(ccmV2JumpgateParameterSupplier.getCcmV2JumpgateParameters(anyString(), any(Optional.class), anyString(), anyString()))
                .thenReturn(defaultCcmV2Parameters);
        when(defaultCcmV2Parameters.getAgentCrn()).thenReturn("testAgentCrn");
        when(hostDiscoveryService.determineGatewayFqdn(any(), any())).thenReturn("datahub.master0.cldr.work.site");

        CcmConnectivityParameters ccmParameters = ThreadBasedUserCrnProvider.doAs(TEST_USER_CRN, () -> underTest.fetchAndSaveCcmParameters(stack));
        assertEquals(CcmConnectivityMode.CCMV2_JUMPGATE, ccmParameters.getConnectivityMode(), "CCM V2 Jumpgate should be enabled.");
        assertEquals(defaultCcmV2Parameters, ccmParameters.getCcmV2JumpgateParameters(), "CCM V2 Jumpgate Parameters should match.");
        verify(ccmV2JumpgateParameterSupplier, times(1)).getCcmV2JumpgateParameters(anyString(), any(Optional.class), anyString(), anyString());
        verifyNoInteractions(ccmParameterSupplier);

        assertEquals("testAgentCrn", stack.getCcmV2AgentCrn(), "Ccm V2 Jumpgate Config should be initialized");
        verify(stackService, times(1)).save(stack);
    }

    private Stack getAStack() {
        Stack aStack = new Stack();
        aStack.setId(100L);
        aStack.setAccountId(TEST_ACCOUNT_ID);
        aStack.setResourceCrn(TEST_CLUSTER_CRN);
        aStack.setEnvironmentCrn(TEST_ENVIRONMENT_CRN);
        return aStack;
    }
}

