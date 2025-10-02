package com.sequenceiq.cloudbreak.rotation.context.provider;

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

import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.rotation.ExitCriteriaProvider;
import com.sequenceiq.cloudbreak.rotation.SecretRotationSaltService;
import com.sequenceiq.cloudbreak.rotation.SecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.common.RotationContext;
import com.sequenceiq.cloudbreak.rotation.secret.custom.CustomJobRotationContext;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;
import com.sequenceiq.cloudbreak.service.autoscale.PeriscopeClientService;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;

@ExtendWith(MockitoExtension.class)
public class NginxClusterSslCertPrivateKeyRotationContextProviderTest {

    private static final Logger LOGGER = getLogger(NginxClusterSslCertPrivateKeyRotationContextProviderTest.class);

    @Mock
    private StackDtoService stackDtoService;

    @Mock
    private GatewayConfigService gatewayConfigService;

    @Mock
    private ExitCriteriaProvider exitCriteriaProvider;

    @Mock
    private SecretRotationSaltService secretRotationSaltService;

    @Mock
    private InstanceMetaDataService instanceMetaDataService;

    @Mock
    private PeriscopeClientService periscopeClientService;

    @InjectMocks
    private NginxClusterSslCertPrivateKeyRotationContextProvider underTest;

    @Test
    void testGetContext() {
        when(stackDtoService.getByCrn(any())).thenReturn(mock(StackDto.class));
        assertEquals(2, underTest.getContexts("crn").size());
    }

    @Test
    void testSwitchCert() throws CloudbreakOrchestratorFailedException {
        when(stackDtoService.getByCrn(any())).thenReturn(mock(StackDto.class));
        when(gatewayConfigService.getAllGatewayConfigs(any())).thenReturn(List.of(new GatewayConfig(null, null, null,
                "host", null, "instanceId", null, null, null, null, null, null, null, true, null, null, null, null, null, null, null, null)));
        when(secretRotationSaltService.executeCommand(any(), anySet(), any())).thenReturn(Map.of("host", "cert"));
        doNothing().when(periscopeClientService).updateServerCertificateInPeriscope(any(), any());
        ArgumentCaptor<String> newCertCaptor = ArgumentCaptor.forClass(String.class);
        doNothing().when(instanceMetaDataService).updateServerCert(newCertCaptor.capture(), any(), any());

        Map<SecretRotationStep, ? extends RotationContext> contexts = underTest.getContexts("crn");
        CustomJobRotationContext customJobRotationContext = (CustomJobRotationContext) contexts.entrySet().stream()
                .filter(entry -> CUSTOM_JOB.equals(entry.getKey())).map(Map.Entry::getValue).findFirst().orElseThrow();
        customJobRotationContext.getRotationJob().ifPresent(Runnable::run);

        verify(secretRotationSaltService).executeCommand(any(), anySet(), any());
        verify(secretRotationSaltService).executeSaltState(any(), anySet(), any(), any(), any(), any());
        verify(instanceMetaDataService).updateServerCert(any(), eq("instanceId"), eq("host"));
        verify(periscopeClientService).updateServerCertificateInPeriscope(any(), any());
        assertEquals(new String(decodeBase64(newCertCaptor.getValue())), "cert");
    }
}
