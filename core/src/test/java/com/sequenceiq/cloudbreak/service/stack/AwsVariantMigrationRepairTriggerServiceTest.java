package com.sequenceiq.cloudbreak.service.stack;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.aws.common.AwsConstants;
import com.sequenceiq.cloudbreak.core.flow2.event.AwsVariantMigrationTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.stack.migration.AwsVariantMigrationEvent;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.ClusterRepairTriggerEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.ClusterRepairTriggerEvent.RepairType;
import com.sequenceiq.cloudbreak.view.StackView;

@ExtendWith(MockitoExtension.class)
class AwsVariantMigrationRepairTriggerServiceTest {

    private static final long STACK_ID = 1L;

    @Mock
    private StackUpgradeService stackUpgradeService;

    @Mock
    private StackDto stackDto;

    @Mock
    private StackView stackView;

    @InjectMocks
    private AwsVariantMigrationRepairTriggerService underTest;

    @Test
    void shouldNotRunWhenNotUpgrade() {
        when(stackUpgradeService.allNodesSelectedForRepair(any(), any())).thenReturn(false);
        ClusterRepairTriggerEvent triggerEvent = new ClusterRepairTriggerEvent(STACK_ID, Map.of(), RepairType.ALL_AT_ONCE, false, null);

        assertFalse(underTest.shouldRunAwsVariantMigration(triggerEvent, stackDto));
    }

    @Test
    void shouldNotRunWhenUpgradeButMigrationIsNotFeasible() {
        String triggeredVariant = "triggeredVariant";
        ClusterRepairTriggerEvent triggerEvent = new ClusterRepairTriggerEvent("eventname", STACK_ID, RepairType.ALL_AT_ONCE, Map.of(),
                false, triggeredVariant, false);
        when(stackUpgradeService.awsVariantMigrationIsFeasible(stackView, triggeredVariant)).thenReturn(false);
        when(stackDto.getStack()).thenReturn(stackView);

        assertFalse(underTest.shouldRunAwsVariantMigration(triggerEvent, stackDto));
    }

    @Test
    void shouldRunWhenUpgradeAndMigrationIsFeasible() {
        String triggeredVariant = "AWS_NATIVE";
        when(stackDto.getStack()).thenReturn(stackView);
        when(stackUpgradeService.awsVariantMigrationIsFeasible(stackView, triggeredVariant)).thenReturn(true);
        ClusterRepairTriggerEvent triggerEvent = new ClusterRepairTriggerEvent("eventname", STACK_ID, RepairType.ALL_AT_ONCE,
                Map.of(), false, triggeredVariant, false);

        assertTrue(underTest.shouldRunAwsVariantMigration(triggerEvent, stackDto));
    }

    @Test
    void shouldRunWhenRepairForAllNodesAndMigrationIsFeasible() {
        String triggeredVariant = "AWS_NATIVE";
        when(stackDto.getStack()).thenReturn(stackView);
        when(stackUpgradeService.allNodesSelectedForRepair(any(), any())).thenReturn(true);
        when(stackUpgradeService.awsVariantMigrationIsFeasible(stackView, triggeredVariant)).thenReturn(true);
        ClusterRepairTriggerEvent triggerEvent = new ClusterRepairTriggerEvent(STACK_ID, Map.of(), RepairType.ALL_AT_ONCE, false,
                triggeredVariant, false, false);

        assertTrue(underTest.shouldRunAwsVariantMigration(triggerEvent, stackDto));
    }

    @ParameterizedTest
    @ValueSource(strings = {"AWS"})
    @NullAndEmptySource
    void shouldNotRunWhenRepairForAllNodesAndMigrationIsNotFeasible(String triggeredVariant) {
        when(stackDto.getStack()).thenReturn(stackView);
        when(stackUpgradeService.allNodesSelectedForRepair(any(), any())).thenReturn(true);
        when(stackUpgradeService.awsVariantMigrationIsFeasible(stackView, triggeredVariant)).thenReturn(false);
        ClusterRepairTriggerEvent triggerEvent = new ClusterRepairTriggerEvent(STACK_ID, Map.of(), RepairType.ALL_AT_ONCE, false,
                triggeredVariant, false, false);

        assertFalse(underTest.shouldRunAwsVariantMigration(triggerEvent, stackDto));
    }

    @Test
    void shouldNotRunWhenPartialRepairRequested() {
        when(stackUpgradeService.allNodesSelectedForRepair(any(), any())).thenReturn(false);
        ClusterRepairTriggerEvent triggerEvent = new ClusterRepairTriggerEvent(STACK_ID, Map.of(), RepairType.ALL_AT_ONCE, false,
                "AWS", false, false);

        assertFalse(underTest.shouldRunAwsVariantMigration(triggerEvent, stackDto));
    }

    @Test
    void createMigrationTriggerEventUsesCreateResourcesSelector() {
        AwsVariantMigrationTriggerEvent event = underTest.createMigrationTriggerEvent(STACK_ID, "master");

        assertThat(event.selector()).isEqualTo(AwsVariantMigrationEvent.CREATE_RESOURCES_EVENT.event());
        assertThat(event.getResourceId()).isEqualTo(STACK_ID);
        assertThat(event.getHostGroupName()).isEqualTo("master");
    }

    @Test
    void shouldNotRunForAlreadyNativeVariantDuringUpgrade() {
        String triggeredVariant = AwsConstants.AwsVariant.AWS_NATIVE_VARIANT.variant().value();
        when(stackDto.getStack()).thenReturn(stackView);
        when(stackUpgradeService.awsVariantMigrationIsFeasible(stackView, triggeredVariant)).thenReturn(false);
        ClusterRepairTriggerEvent triggerEvent = new ClusterRepairTriggerEvent("eventname", STACK_ID, RepairType.ALL_AT_ONCE,
                Map.of(), false, triggeredVariant, false);

        assertFalse(underTest.shouldRunAwsVariantMigration(triggerEvent, stackDto));
    }
}
