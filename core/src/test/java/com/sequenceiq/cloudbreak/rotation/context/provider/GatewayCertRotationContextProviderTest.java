package com.sequenceiq.cloudbreak.rotation.context.provider;

import static com.sequenceiq.cloudbreak.rotation.CloudbreakSecretRotationStep.CM_SERVICE_ROLE_RESTART;
import static com.sequenceiq.cloudbreak.rotation.CommonSecretRotationStep.VAULT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.clusterproxy.ReadConfigResponse;
import com.sequenceiq.cloudbreak.core.bootstrap.service.host.ClusterHostServiceRunner;
import com.sequenceiq.cloudbreak.core.flow2.cluster.provision.service.ClusterProxyService;
import com.sequenceiq.cloudbreak.domain.stack.cluster.gateway.Gateway;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.rotation.CloudbreakSecretType;
import com.sequenceiq.cloudbreak.rotation.ExitCriteriaProvider;
import com.sequenceiq.cloudbreak.rotation.SecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.common.RotationContext;
import com.sequenceiq.cloudbreak.rotation.common.SecretRotationException;
import com.sequenceiq.cloudbreak.rotation.context.CMServiceRoleRestartRotationContext;
import com.sequenceiq.cloudbreak.rotation.secret.vault.VaultRotationContext;
import com.sequenceiq.cloudbreak.service.ClusterProxyRotationService;
import com.sequenceiq.cloudbreak.service.TokenCertInfo;
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

    @Mock
    private ClusterProxyService clusterProxyService;

    @Mock
    private ClusterProxyRotationService clusterProxyRotationService;

    @Mock
    private ReadConfigResponse readConfigResponse;

    @Mock
    private TokenCertInfo tokenCertInfo;

    @InjectMocks
    private GatewayCertRotationContextProvider underTest;

    @BeforeEach
    void setup() {
        when(stackService.getByCrnWithResources(any())).thenReturn(stackDto);
        when(stackDto.getCluster()).thenReturn(clusterView);
        lenient().when(stackDto.getResourceCrn()).thenReturn(RESOURCE_CRN);
        Gateway oldGateway = getGateway("Old");
        Gateway newGateway = getGateway("New");
        when(gatewayService.getByClusterId(any())).thenReturn(Optional.of(oldGateway));
        when(gatewayService.generateSignKeys(any())).thenReturn(newGateway);
        when(gatewayService.putLegacyFieldsIntoVaultIfNecessary(any())).thenAnswer(invocation -> {
            return invocation.getArgument(0);
        });
        when(gatewayService.putLegacyTokenCertIntoVaultIfNecessary(any(), any())).thenAnswer(invocation -> {
            return invocation.getArgument(0);
        });
        when(clusterProxyService.readConfig(any())).thenReturn(readConfigResponse);
    }

    @Test
    void testGetContexts() {
        Map<SecretRotationStep, RotationContext> contexts = underTest.getContexts(RESOURCE_CRN);

        assertEquals(4, contexts.size());
        assertTrue(CloudbreakSecretType.GATEWAY_CERT.getSteps().stream().allMatch(contexts::containsKey));

        CMServiceRoleRestartRotationContext roleRestartContext = (CMServiceRoleRestartRotationContext) contexts.get(CM_SERVICE_ROLE_RESTART);
        assertEquals("KNOX", roleRestartContext.getServiceType());
        assertEquals("KNOX_GATEWAY", roleRestartContext.getRoleType());
        VaultRotationContext vaultRotationContext = (VaultRotationContext) contexts.get(VAULT);
        assertEquals(3, vaultRotationContext.getVaultPathSecretMap().size());
    }

    @Test
    void testGetContextsWithTokenCertRotation() {
        when(readConfigResponse.getKnoxSecretRef()).thenReturn("cluster-proxy/path:field");
        when(clusterProxyRotationService.generateTokenCert()).thenReturn(new TokenCertInfo("private", "public", "cert"));

        Map<SecretRotationStep, RotationContext> contexts = underTest.getContexts(RESOURCE_CRN);

        VaultRotationContext vaultRotationContext = (VaultRotationContext) contexts.get(VAULT);
        assertEquals(6, vaultRotationContext.getVaultPathSecretMap().size());
    }

    @Test
    void testGetContextsWithTokenCertRotationKnoxSecretRefEmpty() {
        Gateway oldGateway = getGateway("Old");
        when(oldGateway.getTokenKeySecret()).thenReturn(Secret.EMPTY);
        when(gatewayService.getByClusterId(any())).thenReturn(Optional.of(oldGateway));
        when(readConfigResponse.getKnoxSecretRef()).thenReturn(null);

        SecretRotationException secretRotationException = assertThrows(SecretRotationException.class,
                () -> underTest.getContexts(RESOURCE_CRN));

        assertEquals("Cannot continue gateway cert rotation because knox token key cannot be found in cluster-proxy.",
                secretRotationException.getMessage());
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
        Secret tokenKeySecret = mock(Secret.class);
        lenient().when(result.getTokenKeySecret()).thenReturn(tokenKeySecret);
        lenient().when(tokenKeySecret.getSecret()).thenReturn("tokenKeyCert");
        Secret tokenCertSecret = mock(Secret.class);
        lenient().when(result.getTokenCertSecret()).thenReturn(tokenCertSecret);
        lenient().when(tokenCertSecret.getSecret()).thenReturn("tokenCert");
        Secret tokenPubSecret = mock(Secret.class);
        lenient().when(result.getTokenPubSecret()).thenReturn(tokenPubSecret);
        lenient().when(tokenPubSecret.getSecret()).thenReturn("tokenPub");
        return result;
    }
}