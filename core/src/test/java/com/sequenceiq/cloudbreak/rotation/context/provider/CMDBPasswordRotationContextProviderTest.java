package com.sequenceiq.cloudbreak.rotation.context.provider;

import static com.sequenceiq.cloudbreak.rotation.context.provider.CMDBContextProviderTestUtil.mockCluster;
import static com.sequenceiq.cloudbreak.rotation.context.provider.CMDBContextProviderTestUtil.mockGwConfig;
import static com.sequenceiq.cloudbreak.rotation.context.provider.CMDBContextProviderTestUtil.mockRdsConfig;
import static com.sequenceiq.cloudbreak.rotation.context.provider.CMDBContextProviderTestUtil.mockStack;
import static org.junit.jupiter.api.Assertions.assertEquals;
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
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.rotation.CloudbreakSecretType;
import com.sequenceiq.cloudbreak.rotation.ExitCriteriaProvider;
import com.sequenceiq.cloudbreak.rotation.SecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.common.RotationContext;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;
import com.sequenceiq.cloudbreak.service.rdsconfig.AbstractRdsConfigProvider;
import com.sequenceiq.cloudbreak.service.rdsconfig.RdsConfigService;
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

    @InjectMocks
    private CMDBPasswordRotationContextProvider underTest;

    @BeforeEach
    public void setup() throws IllegalAccessException {
        lenient().when(exitCriteriaProvider.get(any())).thenReturn(ClusterDeletionBasedExitCriteriaModel.nonCancellableModel());
        FieldUtils.writeField(underTest, "rdsRoleConfigProviders", List.of(rdsRoleConfigProvider), true);
        FieldUtils.writeField(underTest, "rdsConfigProviders", List.of(rdsConfigProvider), true);
        Cluster cluster = mockCluster(1L);
        StackDto stackDto = mockStack(cluster, "resource");
        RDSConfig rdsConfig = mockRdsConfig(TEST_DB_TYPE);
        GatewayConfig gatewayConfig = mockGwConfig();
        when(rdsConfigProvider.getRdsType()).thenReturn(TEST_DB_TYPE);
        when(rdsConfigProvider.getDbUser()).thenReturn("user");
        when(rdsConfigService.findByClusterId(any())).thenReturn(Set.of(rdsConfig));
        when(rdsConfigService.getClustersUsingResource(any())).thenReturn(Set.of(cluster));
        when(stackService.getByCrn(anyString())).thenReturn(stackDto);
        when(gatewayConfigService.getPrimaryGatewayConfig(any())).thenReturn(gatewayConfig);
    }

    @Test
    public void testGetContext() {
        Map<SecretRotationStep, RotationContext> contexts = underTest.getContexts("resource");

        assertEquals(4, contexts.size());
        assertTrue(CloudbreakSecretType.CLUSTER_CM_DB_PASSWORD.getSteps().stream().allMatch(contexts::containsKey));
    }
}
