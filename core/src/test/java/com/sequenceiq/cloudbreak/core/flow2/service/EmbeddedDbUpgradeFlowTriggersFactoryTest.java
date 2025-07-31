package com.sequenceiq.cloudbreak.core.flow2.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.common.database.TargetMajorVersion;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.embeddeddb.UpgradeEmbeddedDBPreparationTriggerRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds.UpgradeRdsTriggerRequest;
import com.sequenceiq.cloudbreak.service.cluster.EmbeddedDatabaseService;
import com.sequenceiq.cloudbreak.service.database.DatabaseDefaultVersionProvider;

@ExtendWith(MockitoExtension.class)
public class EmbeddedDbUpgradeFlowTriggersFactoryTest {
    @Mock
    private EntitlementService entitlementService;

    @Mock
    private EmbeddedDatabaseService embeddedDatabaseService;

    @InjectMocks
    private EmbeddedDbUpgradeFlowTriggersFactory underTest;

    @Mock
    private StackDto stackDto;

    @Mock
    private DatabaseDefaultVersionProvider databaseDefaultVersionProvider;

    @BeforeEach
    public void setUp() {
        when(stackDto.getStackVersion()).thenReturn("7.3.1");
        when(databaseDefaultVersionProvider.calculateDbVersionBasedOnRuntime(stackDto.getStackVersion(), null))
                .thenReturn(TargetMajorVersion.VERSION14.getMajorVersion());
    }

    @Test
    public void testCreateFlowTriggersEmbeddedDBUpgradeNeededReturnsFlowTriggers() {
        when(embeddedDatabaseService.isAttachedDiskForEmbeddedDatabaseCreated(any())).thenReturn(true);
        when(stackDto.getId()).thenReturn(1L);


        List<Selectable> flowTriggers = underTest.createFlowTriggers(stackDto, true);

        assertEquals(2, flowTriggers.size());
        assertEquals(UpgradeEmbeddedDBPreparationTriggerRequest.class, flowTriggers.get(0).getClass());
        assertEquals(UpgradeRdsTriggerRequest.class, flowTriggers.get(1).getClass());
    }

    @Test
    public void testCreateFlowTriggersWithInvalidTargetVersionReturnsEmptyList() {
        when(embeddedDatabaseService.isAttachedDiskForEmbeddedDatabaseCreated(any())).thenReturn(true);
        // Return an invalid version string
        when(databaseDefaultVersionProvider.calculateDbVersionBasedOnRuntime(stackDto.getStackVersion(), null))
                .thenReturn("invalid");

        List<Selectable> flowTriggers = underTest.createFlowTriggers(stackDto, true);

        assertEquals(0, flowTriggers.size());
    }

    @Test
    public void testCreateFlowTriggersWhenUpgradeNotNeededReturnsEmptyList() {
        when(embeddedDatabaseService.isAttachedDiskForEmbeddedDatabaseCreated(any())).thenReturn(true);
        // Make targetVersion equal to currentDbVersion so versionsAreDifferent is false
        when(databaseDefaultVersionProvider.calculateDbVersionBasedOnRuntime(stackDto.getStackVersion(), null))
                .thenReturn("11");
        when(stackDto.getExternalDatabaseEngineVersion()).thenReturn("11");

        List<Selectable> flowTriggers = underTest.createFlowTriggers(stackDto, true);

        assertEquals(0, flowTriggers.size());
    }
}
