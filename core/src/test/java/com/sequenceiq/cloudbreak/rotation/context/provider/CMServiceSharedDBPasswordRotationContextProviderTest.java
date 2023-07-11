package com.sequenceiq.cloudbreak.rotation.context.provider;

import static com.sequenceiq.cloudbreak.rotation.CloudbreakSecretRotationStep.CM_SERVICE;
import static com.sequenceiq.cloudbreak.rotation.CloudbreakSecretRotationStep.SALT_STATE_APPLY;
import static com.sequenceiq.cloudbreak.rotation.CommonSecretRotationStep.VAULT;
import static com.sequenceiq.cloudbreak.rotation.context.provider.CMDBContextProviderTestUtil.mockCluster;
import static com.sequenceiq.cloudbreak.rotation.context.provider.CMDBContextProviderTestUtil.mockGwConfig;
import static com.sequenceiq.cloudbreak.rotation.context.provider.CMDBContextProviderTestUtil.mockRdsConfig;
import static com.sequenceiq.cloudbreak.rotation.context.provider.CMDBContextProviderTestUtil.mockStack;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
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
import com.sequenceiq.cloudbreak.core.bootstrap.service.ClusterDeletionBasedExitCriteriaModel;
import com.sequenceiq.cloudbreak.core.bootstrap.service.container.postgres.PostgresConfigService;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.rotation.ExitCriteriaProvider;
import com.sequenceiq.cloudbreak.rotation.SecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.common.RotationContext;
import com.sequenceiq.cloudbreak.rotation.context.CMServiceConfigRotationContext;
import com.sequenceiq.cloudbreak.rotation.context.SaltStateApplyRotationContext;
import com.sequenceiq.cloudbreak.rotation.secret.vault.VaultRotationContext;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;
import com.sequenceiq.cloudbreak.service.rdsconfig.AbstractRdsConfigProvider;
import com.sequenceiq.cloudbreak.service.rdsconfig.RdsConfigService;
import com.sequenceiq.cloudbreak.service.secret.service.SecretService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;

@ExtendWith(MockitoExtension.class)
public class CMServiceSharedDBPasswordRotationContextProviderTest {

    private static final DatabaseType TEST_DB_TYPE = DatabaseType.HIVE;

    private static final String DATALAKE_CRN = "crn:cdp:datalake:us-west-1:acc1:datalake:cluster1";

    private static final String DATAHUB_CRN = "crn:cdp:datahub:us-west-1:tenant:cluster:resource1";

    @Mock
    private StackDtoService stackService;

    @Mock
    private RdsConfigService rdsConfigService;

    @Mock
    private GatewayConfigService gatewayConfigService;

    @Mock
    private PostgresConfigService postgresConfigService;

    @Mock
    private AbstractRdsRoleConfigProvider rdsRoleConfigProvider;

    @Mock
    private AbstractRdsConfigProvider rdsConfigProvider;

    @Mock
    private SecretService secretService;

    @Mock
    private ExitCriteriaProvider exitCriteriaProvider;

    @InjectMocks
    private CMServiceSharedDBPasswordRotationContextProvider underTest;

    @BeforeEach
    public void setuo() throws IllegalAccessException {
        when(exitCriteriaProvider.get(any())).thenReturn(ClusterDeletionBasedExitCriteriaModel.nonCancellableModel());
        FieldUtils.writeField(underTest, "rdsRoleConfigProviders", List.of(rdsRoleConfigProvider), true);
        FieldUtils.writeField(underTest, "rdsConfigProviders", List.of(rdsConfigProvider), true);
        RDSConfig rdsConfig = mockRdsConfig(TEST_DB_TYPE);
        GatewayConfig gatewayConfig = mockGwConfig();
        when(rdsConfigProvider.getRdsType()).thenReturn(TEST_DB_TYPE);
        when(rdsRoleConfigProvider.dbType()).thenReturn(TEST_DB_TYPE);
        lenient().when(rdsConfigProvider.getDbUser()).thenReturn("user");
        when(rdsRoleConfigProvider.dbUserKey()).thenReturn("userconfig");
        when(rdsRoleConfigProvider.dbPasswordKey()).thenReturn("passwordconfig");
        when(rdsRoleConfigProvider.getServiceType()).thenReturn(TEST_DB_TYPE.name());
        when(rdsConfigService.findByClusterId(any())).thenReturn(Set.of(rdsConfig));
        when(gatewayConfigService.getPrimaryGatewayConfig(any())).thenReturn(gatewayConfig);
    }

    @Test
    public void testGetContextForDatahub() {
        Cluster cluster = mockCluster(1L);
        Cluster otherCluster = mockCluster(2L);
        StackDto stackDto = mockStack(cluster, DATAHUB_CRN);
        when(rdsConfigService.getClustersUsingResource(any())).thenReturn(Set.of(cluster, otherCluster));
        when(stackService.getByCrn(anyString())).thenReturn(stackDto);
        when(secretService.get(anyString())).thenAnswer(i -> String.valueOf(i.getArguments()[0]));

        Map<SecretRotationStep, RotationContext> contexts = underTest.getContexts(DATAHUB_CRN);

        assertEquals(2, ((CMServiceConfigRotationContext) contexts.get(CM_SERVICE)).getCmServiceConfigTable().cellSet().size());
        assertTrue(((SaltStateApplyRotationContext) contexts.get(SALT_STATE_APPLY)).getStates().isEmpty());
        assertTrue(((VaultRotationContext) contexts.get(VAULT)).getVaultPathSecretMap().isEmpty());
    }

    @Test
    public void testGetContextForDatalake() {
        Cluster cluster = mockCluster(1L);
        Cluster otherCluster = mockCluster(2L);
        StackDto stackDto = mockStack(cluster, DATAHUB_CRN);
        when(rdsConfigService.getClustersUsingResource(any())).thenReturn(Set.of(cluster, otherCluster));
        when(stackService.getByCrn(anyString())).thenReturn(stackDto);

        Map<SecretRotationStep, RotationContext> contexts = underTest.getContexts(DATALAKE_CRN);

        assertEquals(2, ((CMServiceConfigRotationContext) contexts.get(CM_SERVICE)).getCmServiceConfigTable().cellSet().size());
        assertFalse(((SaltStateApplyRotationContext) contexts.get(SALT_STATE_APPLY)).getStates().isEmpty());
        assertFalse(((VaultRotationContext) contexts.get(VAULT)).getVaultPathSecretMap().isEmpty());
    }
}
