package com.sequenceiq.cloudbreak.reactor.handler.cluster.upgrade.rds;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.database.TargetMajorVersion;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.cluster.rds.upgrade.AttachedDatahubsRdsSettingsMigrationService;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds.UpgradeRdsFailedEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds.UpgradeRdsMigrateAttachedDatahubsRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds.UpgradeRdsMigrateAttachedDatahubsResponse;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@ExtendWith(MockitoExtension.class)
public class MigrateAttachedDatahubsDBSettingsHandlerTest {

    @InjectMocks
    private MigrateAttachedDatahubsDBSettingsHandler underTest;

    @Mock
    private AttachedDatahubsRdsSettingsMigrationService attachedDatahubsRdsSettingsMigrationService;

    @Test
    void selector() {
        assertThat(underTest.selector()).isEqualTo("UPGRADERDSMIGRATEATTACHEDDATAHUBSREQUEST");
    }

    @Test
    void testDoAccept() throws Exception {
        UpgradeRdsMigrateAttachedDatahubsRequest request = new UpgradeRdsMigrateAttachedDatahubsRequest(1L, TargetMajorVersion.VERSION_14);

        Selectable result = underTest.doAccept(new HandlerEvent<>(new Event<>(request)));

        UpgradeRdsMigrateAttachedDatahubsResponse response = (UpgradeRdsMigrateAttachedDatahubsResponse) result;
        assertEquals(1L, response.getResourceId());

        verify(attachedDatahubsRdsSettingsMigrationService, times(1)).migrate(1L);
    }

    @Test
    void testDoAcceptFailure() throws Exception {
        UpgradeRdsMigrateAttachedDatahubsRequest request = new UpgradeRdsMigrateAttachedDatahubsRequest(1L, TargetMajorVersion.VERSION_14);
        Exception expectedException = new RuntimeException();
        doThrow(expectedException).when(attachedDatahubsRdsSettingsMigrationService).migrate(anyLong());

        Selectable result = underTest.doAccept(new HandlerEvent<>(new Event<>(request)));

        UpgradeRdsFailedEvent response = (UpgradeRdsFailedEvent) result;
        assertEquals(expectedException, response.getException());
    }
}