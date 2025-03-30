package com.sequenceiq.cloudbreak.rotation.context.provider;

import static com.sequenceiq.cloudbreak.rotation.context.provider.CMDBContextProviderTestUtil.mockCluster;
import static com.sequenceiq.cloudbreak.rotation.context.provider.CMDBContextProviderTestUtil.mockGwConfig;
import static com.sequenceiq.cloudbreak.rotation.context.provider.CMDBContextProviderTestUtil.mockRdsConfig;
import static com.sequenceiq.cloudbreak.rotation.context.provider.CMDBContextProviderTestUtil.mockStack;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.util.List;
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
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.AbstractRdsRoleConfigProvider;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.core.bootstrap.service.ClusterDeletionBasedExitCriteriaModel;
import com.sequenceiq.cloudbreak.core.bootstrap.service.container.postgres.PostgresConfigService;
import com.sequenceiq.cloudbreak.core.bootstrap.service.host.ClusterHostServiceRunner;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.orchestrator.model.SaltPillarProperties;
import com.sequenceiq.cloudbreak.rotation.CloudbreakSecretType;
import com.sequenceiq.cloudbreak.rotation.ExitCriteriaProvider;
import com.sequenceiq.cloudbreak.rotation.SecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.common.RotationContext;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;
import com.sequenceiq.cloudbreak.service.rdsconfig.AbstractRdsConfigProvider;
import com.sequenceiq.cloudbreak.service.rdsconfig.RdsConfigService;
import com.sequenceiq.cloudbreak.service.secret.domain.RotationSecret;
import com.sequenceiq.cloudbreak.service.secret.service.UncachedSecretServiceForRotation;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;

@ExtendWith(MockitoExtension.class)
public class CMDBPasswordRotationContextProviderTest {

    private static final DatabaseType TEST_DB_TYPE = DatabaseType.CLOUDERA_MANAGER;

    @Mock
    private StackDtoService stackService;

    @Mock
    private RdsConfigService rdsConfigService;

    @Mock
    private GatewayConfigService gatewayConfigService;

    @Mock
    private AbstractRdsRoleConfigProvider rdsRoleConfigProvider;

    @Mock
    private AbstractRdsConfigProvider rdsConfigProvider;

    @Mock
    private ExitCriteriaProvider exitCriteriaProvider;

    @Mock
    private UncachedSecretServiceForRotation uncachedSecretServiceForRotation;

    @Mock
    private PostgresConfigService postgresConfigService;

    @Mock
    private ClusterHostServiceRunner clusterHostServiceRunner;

    @InjectMocks
    private CMDBPasswordRotationContextProvider underTest;

    private StackDto stackDto;

    private Cluster cluster;

    @BeforeEach
    public void setup() throws IllegalAccessException {
        lenient().when(exitCriteriaProvider.get(any())).thenReturn(ClusterDeletionBasedExitCriteriaModel.nonCancellableModel());
        FieldUtils.writeField(underTest, "rdsRoleConfigProviders", List.of(rdsRoleConfigProvider), true);
        FieldUtils.writeField(underTest, "rdsConfigProviders", List.of(rdsConfigProvider), true);
        cluster = mockCluster(1L);
        cluster.setDatabaseServerCrn("crn");
        stackDto = mockStack(cluster, "resource");
        lenient().when(rdsConfigProvider.getRdsType()).thenReturn(TEST_DB_TYPE);
        lenient().when(rdsConfigProvider.getDbUser()).thenReturn("user");
    }

    @Test
    public void testGetContext() {
        RDSConfig rdsConfig = mockRdsConfig(TEST_DB_TYPE);
        GatewayConfig gatewayConfig = mockGwConfig();
        when(rdsConfigService.findByClusterId(any())).thenReturn(Set.of(rdsConfig));
        when(stackService.getByCrn(anyString())).thenReturn(stackDto);
        when(gatewayConfigService.getPrimaryGatewayConfig(any())).thenReturn(gatewayConfig);
        when(uncachedSecretServiceForRotation.getRotation(any())).thenReturn(new RotationSecret("new", "old"));
        Map<SecretRotationStep, RotationContext> contexts = underTest.getContexts("resource");

        assertEquals(4, contexts.size());
        assertTrue(CloudbreakSecretType.CM_DB_PASSWORD.getSteps().stream().allMatch(contexts::containsKey));
    }

    @Test
    public void testGetPillarProperties() {
        when(stackDto.getCloudPlatform()).thenReturn("mock");
        Map<String, SaltPillarProperties> pillarProperties = underTest.getPillarProperties(stackDto);
        assertTrue(pillarProperties.containsKey(ClusterHostServiceRunner.CM_DATABASE_PILLAR_KEY));
    }

    @Test
    public void testGetPillarPropertiesThrowsException() throws CloudbreakOrchestratorFailedException {
        when(stackDto.getCloudPlatform()).thenReturn("mock");
        doThrow(new CloudbreakOrchestratorFailedException("TEST")).when(clusterHostServiceRunner)
                .getClouderaManagerDatabasePillarProperties(1L, "mock", "crn");
        CloudbreakServiceException exception = assertThrows(CloudbreakServiceException.class, () -> underTest.getPillarProperties(stackDto));
        assertEquals("Failed to generate pillar properties for CM DB username/password rotation.", exception.getMessage());
    }
}
