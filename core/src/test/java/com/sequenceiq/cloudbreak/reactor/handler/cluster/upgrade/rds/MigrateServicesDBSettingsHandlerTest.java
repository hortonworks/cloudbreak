package com.sequenceiq.cloudbreak.reactor.handler.cluster.upgrade.rds;

import static com.sequenceiq.cloudbreak.cluster.model.CMConfigUpdateStrategy.FALLBACK_TO_ROLLCONFIG;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Set;
import java.util.function.Predicate;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.sequenceiq.cloudbreak.common.database.TargetMajorVersion;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.cluster.rds.upgrade.RdsSettingsMigrationService;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds.UpgradeRdsFailedEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds.UpgradeRdsMigrateServicesDBSettingsRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds.UpgradeRdsMigrateServicesDBSettingsResponse;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.view.ClusterView;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@ExtendWith(MockitoExtension.class)
class MigrateServicesDBSettingsHandlerTest {

    @InjectMocks
    private MigrateServicesDBSettingsHandler underTest;

    @Mock
    private StackDtoService stackDtoService;

    @Mock
    private RdsSettingsMigrationService rdsSettingsMigrationService;

    @Mock
    private StackDto stackDto;

    @Mock
    private ClusterView clusterView;

    @Test
    void selector() {
        assertThat(underTest.selector()).isEqualTo("UPGRADERDSMIGRATESERVICESDBSETTINGSREQUEST");
    }

    @Test
    void testDoAccept() throws Exception {
        UpgradeRdsMigrateServicesDBSettingsRequest request = new UpgradeRdsMigrateServicesDBSettingsRequest(1L, TargetMajorVersion.VERSION_14);

        when(stackDto.getCluster()).thenReturn(clusterView);

        when(stackDtoService.getById(anyLong())).thenReturn(stackDto);
        Set<RDSConfig> rdsConfigs = Set.of();
        when(rdsSettingsMigrationService.collectRdsConfigs(anyLong(), any(Predicate.class))).thenReturn(rdsConfigs);
        when(rdsSettingsMigrationService.updateRdsConfigs(stackDto, rdsConfigs)).thenReturn(rdsConfigs);
        Table<String, String, String> cmServiceConfigs = HashBasedTable.create();
        when(rdsSettingsMigrationService.collectCMServiceConfigs(rdsConfigs)).thenReturn(cmServiceConfigs);
        Selectable result = underTest.doAccept(new HandlerEvent<>(new Event<>(request)));

        UpgradeRdsMigrateServicesDBSettingsResponse response = (UpgradeRdsMigrateServicesDBSettingsResponse) result;
        assertEquals(1L, response.getResourceId());

        verify(stackDtoService, times(1)).getById(anyLong());
        verify(rdsSettingsMigrationService, times(1)).updateCMServiceConfigs(stackDto, cmServiceConfigs, FALLBACK_TO_ROLLCONFIG, false);
    }

    @Test
    void testDoAcceptFailure() {
        UpgradeRdsMigrateServicesDBSettingsRequest request = new UpgradeRdsMigrateServicesDBSettingsRequest(1L, TargetMajorVersion.VERSION_14);
        Exception expectedException = new RuntimeException();
        when(stackDtoService.getById(anyLong())).thenThrow(expectedException);

        Selectable result = underTest.doAccept(new HandlerEvent<>(new Event<>(request)));
        UpgradeRdsFailedEvent response = (UpgradeRdsFailedEvent) result;
        assertEquals(expectedException, response.getException());
    }
}