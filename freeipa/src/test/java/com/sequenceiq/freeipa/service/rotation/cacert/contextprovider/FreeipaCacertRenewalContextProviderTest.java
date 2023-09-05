package com.sequenceiq.freeipa.service.rotation.cacert.contextprovider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.rotation.CommonSecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.SecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.common.RotationContext;
import com.sequenceiq.cloudbreak.rotation.common.SecretRotationException;
import com.sequenceiq.cloudbreak.rotation.secret.custom.CustomJobRotationContext;
import com.sequenceiq.freeipa.client.FreeIpaClientException;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.RootCert;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.GatewayConfigService;
import com.sequenceiq.freeipa.service.config.RootCertRegisterService;
import com.sequenceiq.freeipa.service.freeipa.cert.root.RootCertService;
import com.sequenceiq.freeipa.service.stack.StackService;

@ExtendWith(MockitoExtension.class)
public class FreeipaCacertRenewalContextProviderTest {

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:accid:user:mockuser@cloudera.com";

    private static final String ENVIRONMENT_CRN = "crn:cdp:environments:eu-1:1234:environment:91011";

    @Mock
    private StackService stackService;

    @Mock
    private HostOrchestrator orchestrator;

    @Mock
    private RootCertService rootCertService;

    @Mock
    private RootCertRegisterService rootCertRegisterService;

    @Mock
    private GatewayConfigService gatewayConfigService;

    @InjectMocks
    private FreeipaCacertRenewalContextProvider underTest;

    @Test
    void testGetContextsAndCustomJobs() {
        Map<SecretRotationStep, RotationContext> contexts = underTest.getContexts(ENVIRONMENT_CRN);

        assertEquals(1, contexts.size());
        assertEquals(CommonSecretRotationStep.CUSTOM_JOB, contexts.keySet().iterator().next());
    }

    @Test
    void testRenewRootCertIfRootCertInDb() throws CloudbreakOrchestratorFailedException, FreeIpaClientException {
        Stack stack = mock(Stack.class);
        InstanceMetaData instanceMetaData = new InstanceMetaData();
        instanceMetaData.setDiscoveryFQDN("fqdn");
        when(stack.getPrimaryGateway()).thenReturn(Optional.of(instanceMetaData));
        when(stackService.getByEnvironmentCrnAndAccountIdWithListsAndMdcContext(any(), any())).thenReturn(stack);
        when(gatewayConfigService.getNotDeletedGatewayConfigs(any())).thenReturn(List.of());
        when(orchestrator.runCommandOnHosts(any(), any(), any())).thenReturn(Map.of());
        when(rootCertRegisterService.getRootCertFromFreeIpa(any())).thenReturn("fake");
        when(rootCertService.findByStackId(any())).thenReturn(Optional.of(new RootCert()));
        when(rootCertService.save(any())).thenReturn(new RootCert());

        ((CustomJobRotationContext) underTest.getContexts(ENVIRONMENT_CRN).values().iterator().next()).getRotationJob().ifPresent(job ->
                ThreadBasedUserCrnProvider.doAs(USER_CRN, job::run));

        verify(rootCertService).save(any());
        verify(orchestrator).runCommandOnHosts(any(), any(), any());
    }

    @Test
    void testRenewRootCertIfRootCertNotInDb() throws CloudbreakOrchestratorFailedException, FreeIpaClientException {
        Stack stack = mock(Stack.class);
        InstanceMetaData instanceMetaData = new InstanceMetaData();
        instanceMetaData.setDiscoveryFQDN("fqdn");
        when(stack.getPrimaryGateway()).thenReturn(Optional.of(instanceMetaData));
        when(stackService.getByEnvironmentCrnAndAccountIdWithListsAndMdcContext(any(), any())).thenReturn(stack);
        when(gatewayConfigService.getNotDeletedGatewayConfigs(any())).thenReturn(List.of());
        when(orchestrator.runCommandOnHosts(any(), any(), any())).thenReturn(Map.of());
        when(rootCertRegisterService.getRootCertFromFreeIpa(any())).thenReturn("fake");
        when(rootCertService.findByStackId(any())).thenReturn(Optional.empty());

        assertThrows(SecretRotationException.class, () -> ((CustomJobRotationContext) underTest.getContexts(ENVIRONMENT_CRN).values().iterator().next())
                .getRotationJob().ifPresent(job -> ThreadBasedUserCrnProvider.doAs(USER_CRN, job::run)));

        verify(rootCertService, times(0)).save(any());
        verify(orchestrator).runCommandOnHosts(any(), any(), any());
    }

    @Test
    void testCleanupRootCertIfRootCertInDb() throws FreeIpaClientException {
        Stack stack = mock(Stack.class);
        when(stackService.getByEnvironmentCrnAndAccountId(any(), any())).thenReturn(stack);
        when(rootCertRegisterService.getRootCertFromFreeIpa(any())).thenReturn("fake");
        when(rootCertService.findByStackId(any())).thenReturn(Optional.of(new RootCert()));
        when(rootCertService.save(any())).thenReturn(new RootCert());

        ((CustomJobRotationContext) underTest.getContexts(ENVIRONMENT_CRN).values().iterator().next()).getFinalizeJob().ifPresent(job ->
                ThreadBasedUserCrnProvider.doAs(USER_CRN, job::run));

        verify(rootCertService).save(any());
    }

    @Test
    void testCleanupRootCertIfRootCertNotInDb() throws FreeIpaClientException {
        Stack stack = mock(Stack.class);
        when(stackService.getByEnvironmentCrnAndAccountId(any(), any())).thenReturn(stack);
        when(rootCertRegisterService.getRootCertFromFreeIpa(any())).thenReturn("fake");
        when(rootCertService.findByStackId(any())).thenReturn(Optional.empty());

        assertThrows(SecretRotationException.class, () -> ((CustomJobRotationContext) underTest.getContexts(ENVIRONMENT_CRN).values().iterator().next())
                .getFinalizeJob().ifPresent(job -> ThreadBasedUserCrnProvider.doAs(USER_CRN, job::run)));

        verify(rootCertService, times(0)).save(any());
    }
}
