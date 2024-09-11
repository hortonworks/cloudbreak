package com.sequenceiq.cloudbreak.rotation.context.provider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.core.bootstrap.service.host.ClusterHostServiceRunner;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.rotation.CloudbreakSecretType;
import com.sequenceiq.cloudbreak.rotation.ExitCriteriaProvider;
import com.sequenceiq.cloudbreak.rotation.SecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.common.RotationContext;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;

@ExtendWith(MockitoExtension.class)
class ClusterLdapBindPasswordContextProviderTest {

    private static final String RESOURCE_CRN = "resourceCrn";

    @Mock
    private StackDtoService stackDtoService;

    @Mock
    private ClusterHostServiceRunner clusterHostServiceRunner;

    @Mock
    private GatewayConfigService gatewayConfigService;

    @Mock
    private ExitCriteriaProvider exitCriteriaProvider;

    @InjectMocks
    private ClusterLdapBindPasswordContextProvider underTest;

    @Mock
    private StackDto stackDto;

    @Mock
    private GatewayConfig gatewayConfig;

    @Test
    void testGetContext() {
        when(stackDtoService.getByCrn(any())).thenReturn(stackDto);
        when(stackDto.getResourceCrn()).thenReturn(RESOURCE_CRN);
        when(stackDto.getName()).thenReturn(RESOURCE_CRN);
        when(gatewayConfigService.getPrimaryGatewayConfig(stackDto)).thenReturn(gatewayConfig);
        when(gatewayConfig.getHostname()).thenReturn("hostName");

        Map<SecretRotationStep, RotationContext> contexts = underTest.getContexts(RESOURCE_CRN);
        assertEquals(3, contexts.size());
        assertTrue(CloudbreakSecretType.LDAP_BIND_PASSWORD.getSteps().stream().allMatch(contexts::containsKey));
    }

}