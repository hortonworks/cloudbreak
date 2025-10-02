package com.sequenceiq.freeipa.service.rotation.nginxclusterkey;

import static com.sequenceiq.cloudbreak.rotation.CommonSecretRotationStep.CUSTOM_JOB;
import static org.apache.commons.codec.binary.Base64.decodeBase64;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;

import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.rotation.SecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.common.RotationContext;
import com.sequenceiq.cloudbreak.rotation.secret.custom.CustomJobRotationContext;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.GatewayConfigService;
import com.sequenceiq.freeipa.service.rotation.ExitCriteriaProvider;
import com.sequenceiq.freeipa.service.rotation.SecretRotationSaltService;
import com.sequenceiq.freeipa.service.rotation.nginxclustersslkey.NginxClusterSslCertPrivateKeyRotationContextProvider;
import com.sequenceiq.freeipa.service.stack.StackService;
import com.sequenceiq.freeipa.service.stack.instance.InstanceMetaDataService;

@ExtendWith(MockitoExtension.class)
public class NginxClusterSslCertPrivateKeyRotationContextProviderTest {

    private static final String ENVIRONMENT_CRN = "crn:cdp:environments:eu-1:1234:environment:91011";

    private static final Logger LOGGER = getLogger(NginxClusterSslCertPrivateKeyRotationContextProviderTest.class);

    @Mock
    private StackService stackService;

    @Mock
    private GatewayConfigService gatewayConfigService;

    @Mock
    private ExitCriteriaProvider exitCriteriaProvider;

    @Mock
    private SecretRotationSaltService secretRotationSaltService;

    @Mock
    private InstanceMetaDataService instanceMetaDataService;

    @InjectMocks
    private NginxClusterSslCertPrivateKeyRotationContextProvider underTest;

    @Test
    void testGetContext() {
        when(stackService.getByEnvironmentCrnAndAccountIdWithLists(any(), any())).thenReturn(mock(Stack.class));
        assertEquals(2, underTest.getContexts(ENVIRONMENT_CRN).size());
    }

    @Test
    void testSwitchCert() throws CloudbreakOrchestratorFailedException {
        when(stackService.getByEnvironmentCrnAndAccountIdWithLists(any(), any())).thenReturn(mock(Stack.class));
        when(gatewayConfigService.getGatewayConfigs(any(), any())).thenReturn(List.of(new GatewayConfig(null, null, null,
                "host", null, "instanceId", null, null, null, null, null, null, null, true, null, null, null, null, null, null, null, null)));
        when(secretRotationSaltService.executeCommand(any(), anySet(), any())).thenReturn(Map.of("host", "cert"));
        ArgumentCaptor<String> newCertCaptor = ArgumentCaptor.forClass(String.class);
        doNothing().when(instanceMetaDataService).updateServerCert(newCertCaptor.capture(), any(), any());

        Map<SecretRotationStep, ? extends RotationContext> contexts = underTest.getContexts(ENVIRONMENT_CRN);
        CustomJobRotationContext customJobRotationContext = (CustomJobRotationContext) contexts.entrySet().stream()
                .filter(entry -> CUSTOM_JOB.equals(entry.getKey())).map(Map.Entry::getValue).findFirst().orElseThrow();
        customJobRotationContext.getRotationJob().ifPresent(Runnable::run);

        verify(secretRotationSaltService).executeCommand(any(), anySet(), any());
        verify(secretRotationSaltService).executeSaltState(any(), anySet(), any(), any(), any(), any());
        verify(instanceMetaDataService).updateServerCert(any(), eq("instanceId"), eq("host"));
        assertEquals(new String(decodeBase64(newCertCaptor.getValue())), "cert");
    }
}
