package com.sequenceiq.cloudbreak.rotation.context.provider;

import static com.sequenceiq.cloudbreak.rotation.CommonSecretRotationStep.CUSTOM_JOB;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.database.DatabaseAvailabilityType;
import com.sequenceiq.cloudbreak.core.bootstrap.service.ClusterDeletionBasedExitCriteriaModel;
import com.sequenceiq.cloudbreak.domain.stack.Database;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.rotation.CloudbreakSecretType;
import com.sequenceiq.cloudbreak.rotation.ExitCriteriaProvider;
import com.sequenceiq.cloudbreak.rotation.SecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.common.RotationContext;
import com.sequenceiq.cloudbreak.rotation.common.SecretRotationException;
import com.sequenceiq.cloudbreak.rotation.secret.custom.CustomJobRotationContext;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.view.ClusterView;

@ExtendWith(MockitoExtension.class)
public class EmbeddedDbSslCertRotationContextProviderTest {

    @Mock
    private StackDtoService stackDtoService;

    @Mock
    private GatewayConfigService gatewayConfigService;

    @Mock
    private ExitCriteriaProvider exitCriteriaProvider;

    @InjectMocks
    private EmbeddedDbSslCertRotationContextProvider underTest;

    @Test
    void testPrevalidateIfNotEmbedded() {
        when(gatewayConfigService.getPrimaryGatewayConfig(any())).thenReturn(
                GatewayConfig.builder()
                        .withConnectionAddress("host1")
                        .withPublicAddress("1.1.1.1")
                        .withPrivateAddress("1.1.1.1")
                        .withGatewayPort(22)
                        .withInstanceId("i-1839")
                        .withKnoxGatewayEnabled(false)
                        .build()
        );
        when(exitCriteriaProvider.get(any())).thenReturn(ClusterDeletionBasedExitCriteriaModel.nonCancellableModel());
        StackDto stack = mock(StackDto.class);
        when(stackDtoService.getByCrn(any())).thenReturn(stack);
        when(stack.getPrimaryGatewayFQDN()).thenReturn(Optional.of("asd"));
        Database database = new Database();
        database.setExternalDatabaseAvailabilityType(DatabaseAvailabilityType.NON_HA);
        when(stack.getDatabase()).thenReturn(database);

        Map<SecretRotationStep, ? extends RotationContext> contexts = underTest.getContexts("crn");
        assertEquals(2, contexts.size());
        assertTrue(contexts.containsKey(CUSTOM_JOB));
        Optional<Runnable> preValidateJob = ((CustomJobRotationContext) contexts.get(CUSTOM_JOB)).getPreValidateJob();
        assertTrue(preValidateJob.isPresent());
        assertThrows(SecretRotationException.class, () -> preValidateJob.get().run());
        assertEquals(CloudbreakSecretType.EMBEDDED_DB_SSL_CERT, underTest.getSecret());
    }

    @Test
    void testPrevalidateIfSslDisabled() {
        when(gatewayConfigService.getPrimaryGatewayConfig(any())).thenReturn(
                GatewayConfig.builder()
                        .withConnectionAddress("host1")
                        .withPublicAddress("1.1.1.1")
                        .withPrivateAddress("1.1.1.1")
                        .withGatewayPort(22)
                        .withInstanceId("i-1839")
                        .withKnoxGatewayEnabled(false)
                        .build()
        );
        when(exitCriteriaProvider.get(any())).thenReturn(ClusterDeletionBasedExitCriteriaModel.nonCancellableModel());
        StackDto stack = mock(StackDto.class);
        when(stackDtoService.getByCrn(any())).thenReturn(stack);
        Database database = new Database();
        database.setExternalDatabaseAvailabilityType(DatabaseAvailabilityType.NONE);
        when(stack.getDatabase()).thenReturn(database);
        when(stack.getPrimaryGatewayFQDN()).thenReturn(Optional.of("asd"));
        ClusterView cluster = mock(ClusterView.class);
        when(cluster.getDbSslEnabled()).thenReturn(Boolean.FALSE);
        when(stack.getCluster()).thenReturn(cluster);

        Map<SecretRotationStep, ? extends RotationContext> contexts = underTest.getContexts("crn");
        assertEquals(2, contexts.size());
        assertTrue(contexts.containsKey(CUSTOM_JOB));
        Optional<Runnable> preValidateJob = ((CustomJobRotationContext) contexts.get(CUSTOM_JOB)).getPreValidateJob();
        assertTrue(preValidateJob.isPresent());
        assertThrows(SecretRotationException.class, () -> preValidateJob.get().run());
        assertEquals(CloudbreakSecretType.EMBEDDED_DB_SSL_CERT, underTest.getSecret());
    }
}
