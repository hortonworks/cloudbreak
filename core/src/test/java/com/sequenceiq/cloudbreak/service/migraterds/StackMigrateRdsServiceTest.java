package com.sequenceiq.cloudbreak.service.migraterds;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.dto.NameOrCrn;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.migraterds.MigrateDatabaseV1Response;
import com.sequenceiq.cloudbreak.api.model.MigrateDatabaseResponseType;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.BlueprintUpgradeOption;
import com.sequenceiq.cloudbreak.service.StackCommonService;
import com.sequenceiq.cloudbreak.service.blueprint.BlueprintService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.view.StackView;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.api.model.FlowType;

@ExtendWith(MockitoExtension.class)
class StackMigrateRdsServiceTest {

    private static final String ACCOUNTID = "test-account";

    private static final NameOrCrn NAMEORCRN = NameOrCrn.ofName("test-name-or-crn");

    @Mock
    private StackDtoService stackService;

    @Mock
    private StackCommonService stackCommonService;

    @Mock
    private EntitlementService entitlementService;

    @Mock
    private BlueprintService blueprintService;

    @InjectMocks
    private StackMigrateRdsService stackMigrateRdsService;

    private StackView mockStackView;

    @BeforeEach
    public void setUp() {
        mockStackView = mock(StackView.class);
        when(stackService.getStackViewByNameOrCrn(NAMEORCRN, ACCOUNTID)).thenReturn(mockStackView);
    }

    @Test
    void testMigrateRdsSuccessfulMigration() {
        when(entitlementService.cdpSkipRdsSslCertificateRollingRotationValidation(ACCOUNTID)).thenReturn(true);
        when(stackCommonService.triggerMigrateRdsToTls(mockStackView)).thenReturn(new FlowIdentifier(FlowType.FLOW, "migration-task-id"));

        MigrateDatabaseV1Response response = stackMigrateRdsService.migrateRds(NAMEORCRN, ACCOUNTID);

        assertNotNull(response);
        assertEquals(MigrateDatabaseResponseType.TRIGGERED, response.getResponseType());
        assertEquals("migration-task-id", response.getFlowIdentifier().getPollableId());
    }

    @Test
    void testMigrateRdsNotEntitledAndNoRollingUpgrade() {
        when(entitlementService.cdpSkipRdsSslCertificateRollingRotationValidation(ACCOUNTID)).thenReturn(false);
        when(blueprintService.getByClusterId(mockStackView.getClusterId())).thenReturn(Optional.empty());

        assertThrows(BadRequestException.class, () -> {
            stackMigrateRdsService.migrateRds(NAMEORCRN, ACCOUNTID);
        });
    }

    @Test
    void testMigrateRdsEntitledForSkippingValidation() {
        when(entitlementService.cdpSkipRdsSslCertificateRollingRotationValidation(ACCOUNTID)).thenReturn(true);
        when(stackCommonService.triggerMigrateRdsToTls(mockStackView)).thenReturn(new FlowIdentifier(FlowType.FLOW, "migration-task-id"));

        MigrateDatabaseV1Response response = stackMigrateRdsService.migrateRds(NAMEORCRN, ACCOUNTID);

        assertNotNull(response);
        assertEquals(MigrateDatabaseResponseType.TRIGGERED, response.getResponseType());
    }

    @Test
    void testMigrateRdsBlueprintSupportsRollingUpgrade() {
        Blueprint mockBlueprint = mock(Blueprint.class);
        BlueprintUpgradeOption upgradeOption = BlueprintUpgradeOption.ROLLING_UPGRADE_ENABLED;
        when(mockBlueprint.getBlueprintUpgradeOption()).thenReturn(upgradeOption);
        when(blueprintService.getByClusterId(mockStackView.getClusterId())).thenReturn(Optional.of(mockBlueprint));
        when(stackCommonService.triggerMigrateRdsToTls(mockStackView)).thenReturn(new FlowIdentifier(FlowType.FLOW, "migration-task-id"));

        MigrateDatabaseV1Response response = stackMigrateRdsService.migrateRds(NAMEORCRN, ACCOUNTID);

        assertNotNull(response);
        assertEquals(MigrateDatabaseResponseType.TRIGGERED, response.getResponseType());
    }
}