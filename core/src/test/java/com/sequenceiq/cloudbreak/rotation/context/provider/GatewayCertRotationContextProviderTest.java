package com.sequenceiq.cloudbreak.rotation.context.provider;

import static com.sequenceiq.cloudbreak.rotation.CloudbreakSecretRotationStep.CM_SERVICE_ROLE_RESTART;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.core.bootstrap.service.host.ClusterHostServiceRunner;
import com.sequenceiq.cloudbreak.domain.stack.cluster.gateway.Gateway;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.rotation.CloudbreakSecretType;
import com.sequenceiq.cloudbreak.rotation.ExitCriteriaProvider;
import com.sequenceiq.cloudbreak.rotation.SecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.common.RotationContext;
import com.sequenceiq.cloudbreak.rotation.context.CMServiceRoleRestartRotationContext;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;
import com.sequenceiq.cloudbreak.service.gateway.GatewayService;
import com.sequenceiq.cloudbreak.service.secret.domain.Secret;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.view.ClusterView;

@ExtendWith(MockitoExtension.class)
class GatewayCertRotationContextProviderTest {

    private static final String RESOURCE_CRN = "resourceCrn";

    @Mock
    private StackDtoService stackService;

    @Mock
    private GatewayService gatewayService;

    @Spy
    private ClusterHostServiceRunner clusterHostServiceRunner;

    @Mock
    private ExitCriteriaProvider exitCriteriaProvider;

    @Mock
    private ClusterApiConnectors clusterApiConnectors;

    @Mock
    private StackDto stackDto;

    @Mock
    private ClusterView clusterView;

    @InjectMocks
    private GatewayCertRotationContextProvider underTest;

    @Test
    void testGetContexts() {
        when(stackService.getByCrn(any())).thenReturn(stackDto);
        when(stackDto.getCluster()).thenReturn(clusterView);
        when(stackDto.getResourceCrn()).thenReturn(RESOURCE_CRN);
        Gateway oldGateway = getGateway("Old");
        Gateway newGateway = getGateway("New");
        when(gatewayService.getByClusterId(any())).thenReturn(Optional.of(oldGateway));
        when(gatewayService.generateSignKeys(any())).thenReturn(newGateway);
        when(gatewayService.putLegacyFieldsIntoVaultIfNecessary(any())).thenAnswer(invocation -> {
            return invocation.getArgument(0);
        });

        Map<SecretRotationStep, RotationContext> contexts = underTest.getContexts(RESOURCE_CRN);

        assertEquals(3, contexts.size());
        assertTrue(CloudbreakSecretType.GATEWAY_CERT.getSteps().stream().allMatch(contexts::containsKey));

        CMServiceRoleRestartRotationContext roleRestartContext = (CMServiceRoleRestartRotationContext) contexts.get(CM_SERVICE_ROLE_RESTART);
        assertEquals("KNOX", roleRestartContext.getServiceType());
        assertEquals("KNOX_GATEWAY", roleRestartContext.getRoleType());
    }

    private Gateway getGateway(String suffix) {
        Gateway result = mock(Gateway.class);
        lenient().when(result.getKnoxMaster()).thenReturn(String.format("masterSecret%s", suffix));
        lenient().when(result.getSignCert()).thenReturn(String.format("signCert%s", suffix));
        lenient().when(result.getSignKey()).thenReturn(String.format("signKey%s", suffix));
        lenient().when(result.getSignPub()).thenReturn(String.format("signPub%s", suffix));
        Secret signKeySecret = mock(Secret.class);
        lenient().when(result.getSignKeySecret()).thenReturn(signKeySecret);
        lenient().when(signKeySecret.getSecret()).thenReturn("signKey");
        Secret signPubSecret = mock(Secret.class);
        lenient().when(result.getSignPubSecret()).thenReturn(signPubSecret);
        lenient().when(signPubSecret.getSecret()).thenReturn("signPub");
        Secret signCertSecret = mock(Secret.class);
        lenient().when(result.getSignCertSecret()).thenReturn(signCertSecret);
        lenient().when(signCertSecret.getSecret()).thenReturn("signCert");
        return result;
    }
}