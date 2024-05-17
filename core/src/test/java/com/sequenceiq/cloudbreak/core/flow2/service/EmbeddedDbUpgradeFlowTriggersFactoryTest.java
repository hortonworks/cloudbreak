package com.sequenceiq.cloudbreak.core.flow2.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.common.database.TargetMajorVersion;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.embeddeddb.UpgradeEmbeddedDBPreparationTriggerRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds.UpgradeRdsTriggerRequest;
import com.sequenceiq.cloudbreak.service.cluster.EmbeddedDatabaseService;
import com.sequenceiq.cloudbreak.view.StackView;

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

    @BeforeEach
    public void setUp() {
        StackView stackView = mock(StackView.class);
        when(stackDto.getStack()).thenReturn(stackView);
        ReflectionTestUtils.setField(underTest, "targetMajorVersion", TargetMajorVersion.VERSION14);
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

}
