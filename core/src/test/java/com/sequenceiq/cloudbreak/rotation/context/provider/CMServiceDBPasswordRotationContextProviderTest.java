package com.sequenceiq.cloudbreak.rotation.context.provider;

import static com.sequenceiq.cloudbreak.rotation.CloudbreakSecretRotationStep.CM_SERVICE;
import static com.sequenceiq.cloudbreak.rotation.context.provider.CMDBContextProviderTestUtil.mockCluster;
import static com.sequenceiq.cloudbreak.rotation.context.provider.CMDBContextProviderTestUtil.mockGwConfig;
import static com.sequenceiq.cloudbreak.rotation.context.provider.CMDBContextProviderTestUtil.mockRdsConfig;
import static com.sequenceiq.cloudbreak.rotation.context.provider.CMDBContextProviderTestUtil.mockStack;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.reflect.FieldUtils;
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
import com.sequenceiq.cloudbreak.rotation.ExitCriteriaProvider;
import com.sequenceiq.cloudbreak.rotation.SecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.common.RotationContext;
import com.sequenceiq.cloudbreak.rotation.context.CMServiceConfigRotationContext;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;
import com.sequenceiq.cloudbreak.service.rdsconfig.AbstractRdsConfigProvider;
import com.sequenceiq.cloudbreak.service.rdsconfig.RdsConfigService;
import com.sequenceiq.cloudbreak.service.secret.domain.RotationSecret;
import com.sequenceiq.cloudbreak.service.secret.service.UncachedSecretServiceForRotation;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;

@ExtendWith(MockitoExtension.class)
public class CMServiceDBPasswordRotationContextProviderTest {

    private static final DatabaseType TEST_DB_TYPE = DatabaseType.HUE;

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

    @InjectMocks
    private CMServiceDBPasswordRotationContextProvider underTest;

    @Test
    public void testGetContext() throws IllegalAccessException {
        when(exitCriteriaProvider.get(any())).thenReturn(ClusterDeletionBasedExitCriteriaModel.nonCancellableModel());
        FieldUtils.writeField(underTest, "rdsRoleConfigProviders", List.of(rdsRoleConfigProvider), true);
        FieldUtils.writeField(underTest, "rdsConfigProviders", List.of(rdsConfigProvider), true);
        Cluster cluster = mockCluster(1L);
        StackDto stackDto = mockStack(cluster, "resource");
        RDSConfig rdsConfig = mockRdsConfig(TEST_DB_TYPE);
        GatewayConfig gatewayConfig = mockGwConfig();
        when(rdsConfigProvider.getRdsType()).thenReturn(TEST_DB_TYPE);
        when(rdsRoleConfigProvider.dbType()).thenReturn(TEST_DB_TYPE);
        lenient().when(rdsConfigProvider.getDbUser()).thenReturn("user");
        when(rdsRoleConfigProvider.dbUserKey()).thenReturn("userconfig");
        when(rdsRoleConfigProvider.dbPasswordKey()).thenReturn("passwordconfig");
        when(rdsRoleConfigProvider.getServiceType()).thenReturn(TEST_DB_TYPE.name());
        when(rdsConfigService.findByClusterId(any())).thenReturn(Set.of(rdsConfig));
        when(stackService.getByCrn(anyString())).thenReturn(stackDto);
        when(gatewayConfigService.getPrimaryGatewayConfig(any())).thenReturn(gatewayConfig);
        when(uncachedSecretServiceForRotation.getRotation(any())).thenReturn(new RotationSecret("new", "old"));

        Map<SecretRotationStep, RotationContext> contexts = underTest.getContexts("resource");

        assertEquals(2, ((CMServiceConfigRotationContext) contexts.get(CM_SERVICE)).getCmServiceConfigTable().cellSet().size());
    }
}
