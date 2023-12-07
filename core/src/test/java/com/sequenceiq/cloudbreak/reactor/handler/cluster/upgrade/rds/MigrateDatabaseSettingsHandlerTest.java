package com.sequenceiq.cloudbreak.reactor.handler.cluster.upgrade.rds;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.database.TargetMajorVersion;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.bootstrap.service.container.postgres.PostgresConfigService;
import com.sequenceiq.cloudbreak.core.bootstrap.service.host.ClusterHostServiceRunner;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.orchestrator.model.SaltConfig;
import com.sequenceiq.cloudbreak.orchestrator.model.SaltPillarProperties;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds.UpgradeRdsFailedEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds.UpgradeRdsMigrateDatabaseSettingsRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds.UpgradeRdsMigrateDatabaseSettingsResponse;
import com.sequenceiq.cloudbreak.rotation.ExitCriteriaProvider;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;
import com.sequenceiq.cloudbreak.service.salt.SaltStateParamsService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.view.ClusterView;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@ExtendWith(MockitoExtension.class)
public class MigrateDatabaseSettingsHandlerTest {

    @InjectMocks
    private MigrateDatabaseSettingsHandler underTest;

    @Mock
    private HostOrchestrator hostOrchestrator;

    @Mock
    private ClusterHostServiceRunner clusterHostServiceRunner;

    @Mock
    private SaltStateParamsService saltStateParamsService;

    @Mock
    private StackDtoService stackDtoService;

    @Mock
    private ExitCriteriaProvider exitCriteriaProvider;

    @Mock
    private GatewayConfigService gatewayConfigService;

    @Mock
    private PostgresConfigService postgresConfigService;

    @Mock
    private StackDto stackDto;

    @Mock
    private ClusterView clusterView;

    @Mock
    private GatewayConfig primaryGatewayConfig;

    @Mock
    private RdsConfigUpdateService rdsConfigUpdateService;

    @Test
    void testDoAccept() throws Exception {
        UpgradeRdsMigrateDatabaseSettingsRequest request = new UpgradeRdsMigrateDatabaseSettingsRequest(1L, TargetMajorVersion.VERSION_14);

        when(stackDto.getCluster()).thenReturn(clusterView);
        when(primaryGatewayConfig.getHostname()).thenReturn("hostname");

        when(stackDtoService.getById(anyLong())).thenReturn(stackDto);
        when(clusterHostServiceRunner.getClouderaManagerDatabasePillarProperties(any(ClusterView.class))).thenReturn(new SaltPillarProperties("path", Map.of()));
        when(postgresConfigService.getPostgreSQLServerPropertiesForRotation(stackDto)).thenReturn(new SaltPillarProperties("path1", Map.of()));
        when(gatewayConfigService.getPrimaryGatewayConfig(stackDto)).thenReturn(primaryGatewayConfig);
        Selectable result = underTest.doAccept(new HandlerEvent<>(new Event<>(request)));

        UpgradeRdsMigrateDatabaseSettingsResponse response = (UpgradeRdsMigrateDatabaseSettingsResponse) result;
        assertEquals(1L, response.getResourceId());

        verify(stackDtoService, times(1)).getById(anyLong());
        verify(rdsConfigUpdateService).updateRdsConnectionUserName(stackDto);
        ArgumentCaptor<SaltConfig> saltConfigArgumentCaptor = ArgumentCaptor.forClass(SaltConfig.class);
        verify(hostOrchestrator, times(1)).saveCustomPillars(saltConfigArgumentCaptor.capture(), isNull(), isNull());
        SaltConfig saltConfig = saltConfigArgumentCaptor.getValue();
        assertEquals("path", saltConfig.getServicePillarConfig().get(ClusterHostServiceRunner.CM_DATABASE_PILLAR_KEY).getPath());
        assertEquals("path1", saltConfig.getServicePillarConfig().get(PostgresConfigService.POSTGRESQL_SERVER).getPath());
        verify(gatewayConfigService).getPrimaryGatewayConfig(stackDto);

    }

    @Test
    void testDoAcceptFailure() {
        UpgradeRdsMigrateDatabaseSettingsRequest request = new UpgradeRdsMigrateDatabaseSettingsRequest(1L, TargetMajorVersion.VERSION_14);
        Exception expectedException = new RuntimeException();
        when(stackDtoService.getById(anyLong())).thenThrow(expectedException);

        Selectable result = underTest.doAccept(new HandlerEvent<>(new Event<>(request)));
        UpgradeRdsFailedEvent response = (UpgradeRdsFailedEvent) result;
        assertEquals(expectedException, response.getException());
    }
}
