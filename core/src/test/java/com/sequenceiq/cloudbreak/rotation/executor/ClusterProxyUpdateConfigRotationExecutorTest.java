package com.sequenceiq.cloudbreak.rotation.executor;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.clusterproxy.ClusterProxySecretProvider;
import com.sequenceiq.cloudbreak.core.flow2.cluster.provision.service.ClusterProxyService;
import com.sequenceiq.cloudbreak.domain.stack.cluster.gateway.Gateway;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.rotation.context.ClusterProxyUpdateConfigRotationContext;
import com.sequenceiq.cloudbreak.rotation.context.ClusterProxyUpdateConfigRotationContext.ClusterProxyUpdateConfigRotationContextBuilder;
import com.sequenceiq.cloudbreak.service.gateway.GatewayService;
import com.sequenceiq.cloudbreak.service.secret.domain.Secret;
import com.sequenceiq.cloudbreak.service.secret.service.UncachedSecretServiceForRotation;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.view.GatewayView;

@ExtendWith(MockitoExtension.class)
class ClusterProxyUpdateConfigRotationExecutorTest {

    private static final Long GATEWAY_ID = 1L;

    private static final Long STACK_ID = 100L;

    private static final String RESOURCE_CRN = "resource";

    private static final String TOKEN_CERT_SECRET = "tokenCertSecret";

    private static final String SIGN_CERT_SECRET = "signCertSecret";

    private static final String NEW_SIGN_CERT = "newSignCert";

    private static final String GENERATED_SECRET_FORMAT = "cb/path:secret";

    @Mock
    private ClusterProxyService clusterProxyService;

    @Mock
    private StackDtoService stackDtoService;

    @Mock
    private GatewayService gatewayService;

    @Mock
    private ClusterProxySecretProvider clusterProxySecretProvider;

    @Mock
    private UncachedSecretServiceForRotation uncachedSecretServiceForRotation;

    @Mock
    private StackDto stackDto;

    @Mock
    private GatewayView newGatewaySecrets;

    @InjectMocks
    private ClusterProxyUpdateConfigRotationExecutor underTest;

    @Test
    public void testRotation() throws Exception {
        Gateway gateway = createGateway();
        when(gatewayService.getById(GATEWAY_ID)).thenReturn(Optional.of(gateway));
        when(newGatewaySecrets.getSignCert()).thenReturn(NEW_SIGN_CERT);
        when(stackDtoService.getByCrn(RESOURCE_CRN)).thenReturn(stackDto);
        when(stackDto.getId()).thenReturn(STACK_ID);
        when(clusterProxySecretProvider.generateClusterProxySecretFormat(SIGN_CERT_SECRET)).thenReturn(GENERATED_SECRET_FORMAT);

        underTest.rotate(buildContext());

        verify(uncachedSecretServiceForRotation).putRotation(TOKEN_CERT_SECRET, NEW_SIGN_CERT);
        verify(clusterProxyService).updateClusterConfigWithKnoxSecretLocation(STACK_ID, GENERATED_SECRET_FORMAT);
    }

    @Test
    public void testRotationWhenGatewayNotFound() throws Exception {
        when(gatewayService.getById(GATEWAY_ID)).thenReturn(Optional.empty());

        underTest.rotate(buildContext());

        verifyNoInteractions(uncachedSecretServiceForRotation);
        verifyNoInteractions(clusterProxyService);
    }

    @Test
    public void testRollback() throws Exception {
        Gateway gateway = createGateway();
        when(gatewayService.getById(GATEWAY_ID)).thenReturn(Optional.of(gateway));
        when(newGatewaySecrets.getSignCert()).thenReturn(NEW_SIGN_CERT);
        when(stackDtoService.getByCrn(RESOURCE_CRN)).thenReturn(stackDto);
        when(stackDto.getId()).thenReturn(STACK_ID);
        when(clusterProxySecretProvider.generateClusterProxySecretFormat(SIGN_CERT_SECRET)).thenReturn(GENERATED_SECRET_FORMAT);

        underTest.rollback(buildContext());

        verify(uncachedSecretServiceForRotation).putRotation(TOKEN_CERT_SECRET, NEW_SIGN_CERT);
        verify(clusterProxyService).updateClusterConfigWithKnoxSecretLocation(STACK_ID, GENERATED_SECRET_FORMAT);
    }

    @Test
    public void testRollbackWhenGatewayNotFound() throws Exception {
        when(gatewayService.getById(GATEWAY_ID)).thenReturn(Optional.empty());

        underTest.rollback(buildContext());

        verifyNoInteractions(uncachedSecretServiceForRotation);
        verifyNoInteractions(clusterProxyService);
    }

    private ClusterProxyUpdateConfigRotationContext buildContext() {
        return new ClusterProxyUpdateConfigRotationContextBuilder()
                .withResourceCrn(RESOURCE_CRN)
                .withCurrentGatewayId(GATEWAY_ID)
                .withNewGatewaySecrets(newGatewaySecrets)
                .withKnoxSecretPath(() -> "secretPath")
                .build();
    }

    private Gateway createGateway() {
        Gateway gateway = new Gateway();
        gateway.setId(GATEWAY_ID);
        gateway.setTokenCertSecretJson(new Secret("tokenCertRaw", TOKEN_CERT_SECRET));
        gateway.setSignCertSecret(new Secret("signCertRaw", SIGN_CERT_SECRET));
        return gateway;
    }
}
