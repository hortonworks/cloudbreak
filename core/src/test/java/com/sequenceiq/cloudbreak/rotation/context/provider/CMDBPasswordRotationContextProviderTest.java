package com.sequenceiq.cloudbreak.rotation.context.provider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.database.base.DatabaseType;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.core.bootstrap.service.ClusterDeletionBasedExitCriteriaModel;
import com.sequenceiq.cloudbreak.core.bootstrap.service.container.postgres.PostgresConfigService;
import com.sequenceiq.cloudbreak.core.bootstrap.service.host.ClusterHostServiceRunner;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.rotation.CloudbreakSecretType;
import com.sequenceiq.cloudbreak.rotation.ExitCriteriaProvider;
import com.sequenceiq.cloudbreak.rotation.SecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.common.RotationContext;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;
import com.sequenceiq.cloudbreak.service.rdsconfig.RdsConfigService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.view.ClusterView;

@ExtendWith(MockitoExtension.class)
public class CMDBPasswordRotationContextProviderTest {

    private StackDto stackDto;

    @Mock
    private StackDtoService stackService;

    @Mock
    private RdsConfigService rdsConfigService;

    @Mock
    private GatewayConfigService gatewayConfigService;

    @Mock
    private PostgresConfigService postgresConfigService;

    @Mock
    private ClusterHostServiceRunner clusterHostServiceRunner;

    @Mock
    private ExitCriteriaProvider exitCriteriaProvider;

    @InjectMocks
    private CMDBPasswordRotationContextProvider underTest;

    @BeforeEach
    public void setup() throws IllegalAccessException {
        FieldUtils.writeDeclaredField(underTest, "defaultUserName", "cm", true);
        stackDto = mock(StackDto.class);
        ClusterView cluster = mock(ClusterView.class);
        when(stackDto.getCluster()).thenReturn(cluster);
        when(cluster.getId()).thenReturn(1L);
        lenient().when(exitCriteriaProvider.get(any())).thenReturn(ClusterDeletionBasedExitCriteriaModel.nonCancellableModel());
    }

    @Test
    public void testGetContext() {
        RDSConfig rdsConfig = mock(RDSConfig.class);
        when(rdsConfig.getConnectionUserNameSecret()).thenReturn("userpath");
        when(rdsConfig.getConnectionPasswordSecret()).thenReturn("passpath");
        when(rdsConfig.getType()).thenReturn(DatabaseType.CLOUDERA_MANAGER.name());
        when(rdsConfigService.findByClusterId(any())).thenReturn(Set.of(rdsConfig));
        when(stackService.getByCrn(anyString())).thenReturn(stackDto);
        GatewayConfig gatewayConfig = mock(GatewayConfig.class);
        when(gatewayConfigService.getPrimaryGatewayConfig(any())).thenReturn(gatewayConfig);
        when(gatewayConfig.getHostname()).thenReturn("host");

        Map<SecretRotationStep, RotationContext> contexts = underTest.getContexts("resource");

        assertEquals(4, contexts.size());
        assertTrue(CloudbreakSecretType.CLUSTER_CM_DB_PASSWORD.getSteps().stream().allMatch(contexts::containsKey));
    }

    @Test
    public void testGetContextWhenNoDb() {
        when(rdsConfigService.findByClusterId(any())).thenReturn(Set.of());
        when(stackService.getByCrn(anyString())).thenReturn(stackDto);

        assertThrows(CloudbreakServiceException.class, () -> underTest.getContexts("resource"));
    }
}
