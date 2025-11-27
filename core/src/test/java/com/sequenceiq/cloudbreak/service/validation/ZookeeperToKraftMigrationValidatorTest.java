package com.sequenceiq.cloudbreak.service.validation;

import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_ENABLE_ZOOKEEPER_TO_KRAFT_MIGRATION;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.cluster.status.KraftMigrationStatus;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.service.blueprint.BlueprintService;

@ExtendWith(MockitoExtension.class)
class ZookeeperToKraftMigrationValidatorTest {

    private static final String TEST_BP_JSON_TEXT = "{does not matter what is here}";

    private static final String KAFKA_SERVICE_TYPE = "KAFKA";

    private static final String ACCOUNT_ID = "test-account-id";

    private static final String VALID_VERSION = "7.3.2";

    private static final String HIGHER_VERSION = "7.4.0";

    private static final String LOWER_VERSION = "7.3.1";

    @Mock
    private EntitlementService entitlementService;

    @Mock
    private BlueprintService mockBlueprintService;

    @Mock
    private StackDto stack;

    @Mock
    private Status status;

    @Mock
    private Blueprint blueprint;

    private ZookeeperToKraftMigrationValidator underTest;

    @BeforeEach
    void setup() {
        lenient().when(stack.getStatus()).thenReturn(status);
        lenient().when(blueprint.getBlueprintJsonText()).thenReturn(TEST_BP_JSON_TEXT);
        underTest = new ZookeeperToKraftMigrationValidator(entitlementService, mockBlueprintService);
    }

    @Test
    void testValidateZookeeperToKraftMigrationEligibilityWithHigherVersion() {
        when(stack.getStatus()).thenReturn(status);
        when(status.isAvailable()).thenReturn(true);
        when(stack.getBlueprint()).thenReturn(blueprint);
        when(stack.getStackVersion()).thenReturn(HIGHER_VERSION);
        lenient().when(entitlementService.isZookeeperToKRaftMigrationEnabled(ACCOUNT_ID)).thenReturn(true);
        when(mockBlueprintService.anyOfTheServiceTypesPresentOnBlueprint(TEST_BP_JSON_TEXT, List.of(KAFKA_SERVICE_TYPE))).thenReturn(true);

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> underTest.validateZookeeperToKraftMigrationEligibility(stack, ACCOUNT_ID));

        assertEquals("Zookeeper to KRaft migration is supported only for CDP version 7.3.2",
                exception.getMessage());
    }

    @Test
    void testValidateZookeeperToKraftMigrationEligibilityWhenClusterNotAvailable() {
        when(stack.getStatus()).thenReturn(status);
        when(status.isAvailable()).thenReturn(false);

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> underTest.validateZookeeperToKraftMigrationEligibility(stack, ACCOUNT_ID));

        assertEquals("Zookeeper to KRaft migration can only be performed when the cluster is in Available state. Please ensure the cluster is " +
                "fully operational before starting the migration.", exception.getMessage());
    }

    @Test
    void testValidateZookeeperToKraftMigrationNoKafkaServiceInBp() {
        when(stack.getStatus()).thenReturn(status);
        when(status.isAvailable()).thenReturn(true);
        when(stack.getBlueprint()).thenReturn(blueprint);
        when(mockBlueprintService.anyOfTheServiceTypesPresentOnBlueprint(TEST_BP_JSON_TEXT, List.of(KAFKA_SERVICE_TYPE))).thenReturn(false);

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> underTest.validateZookeeperToKraftMigrationEligibility(stack, ACCOUNT_ID));

        String message = exception.getMessage();
        assertEquals("Zookeeper to KRaft migration is supported only for templates where Kafka is present.", message);
    }

    @Test
    void testValidateZookeeperToKraftMigrationWithUnsupportedTemplateTypeAndKafkaServiceInBp() {
        when(status.isAvailable()).thenReturn(true);
        when(stack.getBlueprint()).thenReturn(blueprint);
        when(mockBlueprintService.anyOfTheServiceTypesPresentOnBlueprint(TEST_BP_JSON_TEXT, List.of(KAFKA_SERVICE_TYPE))).thenReturn(true);
        when(entitlementService.isZookeeperToKRaftMigrationEnabled(ACCOUNT_ID)).thenReturn(true);
        when(stack.getStackVersion()).thenReturn(VALID_VERSION);

        assertDoesNotThrow(() -> underTest.validateZookeeperToKraftMigrationEligibility(stack, ACCOUNT_ID));
    }

    @Test
    void testValidateZookeeperToKraftMigrationWithLowVersion() {
        when(stack.getStatus()).thenReturn(status);
        when(status.isAvailable()).thenReturn(true);
        when(stack.getBlueprint()).thenReturn(blueprint);
        when(stack.getStackVersion()).thenReturn(LOWER_VERSION);
        when(mockBlueprintService.anyOfTheServiceTypesPresentOnBlueprint(TEST_BP_JSON_TEXT, List.of(KAFKA_SERVICE_TYPE))).thenReturn(true);

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> underTest.validateZookeeperToKraftMigrationEligibility(stack, ACCOUNT_ID));

        assertEquals("Zookeeper to KRaft migration is supported only for CDP version 7.3.2",
                exception.getMessage());
    }

    @Test
    void testValidateZookeeperToKraftMigrationEligibilityWhenEntitlementNotEnabled() {
        when(stack.getStatus()).thenReturn(status);
        when(status.isAvailable()).thenReturn(true);
        when(stack.getBlueprint()).thenReturn(blueprint);
        when(stack.getStackVersion()).thenReturn(VALID_VERSION);
        when(entitlementService.isZookeeperToKRaftMigrationEnabled(ACCOUNT_ID)).thenReturn(false);
        when(mockBlueprintService.anyOfTheServiceTypesPresentOnBlueprint(TEST_BP_JSON_TEXT, List.of(KAFKA_SERVICE_TYPE))).thenReturn(true);

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> underTest.validateZookeeperToKraftMigrationEligibility(stack, ACCOUNT_ID));

        assertEquals(String.format("Your account is not entitled to perform Zookeeper to KRaft migration. Please contact Cloudera to enable '%s' " +
                "entitlement for your account.", CDP_ENABLE_ZOOKEEPER_TO_KRAFT_MIGRATION), exception.getMessage());
    }

    @Test
    void testValidateZookeeperToKraftMigrationEligibilityWithMinimumVersion() {
        when(stack.getStatus()).thenReturn(status);
        when(status.isAvailable()).thenReturn(true);
        when(stack.getBlueprint()).thenReturn(blueprint);
        when(stack.getStackVersion()).thenReturn(VALID_VERSION);
        when(entitlementService.isZookeeperToKRaftMigrationEnabled(ACCOUNT_ID)).thenReturn(true);
        when(mockBlueprintService.anyOfTheServiceTypesPresentOnBlueprint(TEST_BP_JSON_TEXT, List.of(KAFKA_SERVICE_TYPE))).thenReturn(true);

        assertDoesNotThrow(() -> underTest.validateZookeeperToKraftMigrationEligibility(stack, ACCOUNT_ID));
    }

    @Test
    void testValidateZookeeperToKraftMigrationState() {
        assertDoesNotThrow(() -> underTest.validateZookeeperToKraftMigrationState(KraftMigrationStatus.ZOOKEEPER_INSTALLED.name()));
        assertDoesNotThrow(() -> underTest.validateZookeeperToKraftMigrationState(KraftMigrationStatus.PRE_MIGRATION.name()));
    }

    @Test
    void testValidateZookeeperToKraftMigrationStateWhenAlreadyMigratedNotFinalized() {
        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> underTest.validateZookeeperToKraftMigrationState(KraftMigrationStatus.BROKERS_IN_KRAFT.name()));

        assertEquals("Cannot start KRaft migration. The cluster has been migrated already to KRaft.",
                exception.getMessage());
    }

    @Test
    void testValidateZookeeperToKraftMigrationStateWhenAlreadyMigrated() {
        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> underTest.validateZookeeperToKraftMigrationState(KraftMigrationStatus.KRAFT_INSTALLED.name()));

        assertEquals("Cannot start KRaft migration. The cluster has been migrated already to KRaft.",
                exception.getMessage());
    }

    @Test
    void testValidateZookeeperToKraftMigrationStateWhenMigrationInProgress() {
        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> underTest.validateZookeeperToKraftMigrationState(KraftMigrationStatus.BROKERS_IN_MIGRATION.name()));

        assertEquals("Cannot start KRaft migration. The cluster is being migrated to KRaft and has the status: BROKERS_IN_MIGRATION.",
                exception.getMessage());
    }

    @Test
    void testIsMigrationFromZookeeperToKraftSupported() {
        when(stack.getStatus()).thenReturn(status);
        when(status.isAvailable()).thenReturn(true);
        when(stack.getBlueprint()).thenReturn(blueprint);
        when(stack.getStackVersion()).thenReturn(VALID_VERSION);
        when(entitlementService.isZookeeperToKRaftMigrationEnabled(ACCOUNT_ID)).thenReturn(true);
        when(mockBlueprintService.anyOfTheServiceTypesPresentOnBlueprint(TEST_BP_JSON_TEXT, List.of(KAFKA_SERVICE_TYPE))).thenReturn(true);

        assertTrue(underTest.isMigrationFromZookeeperToKraftSupported(stack, ACCOUNT_ID));
    }

    @ParameterizedTest
    @MethodSource("testIsMigrationFromZookeeperToKraftNotSupportedParameters")
    void testIsMigrationFromZookeeperToKraftNotSupported(Boolean available, String version, Boolean migrationEnabled) {
        lenient().when(status.isAvailable()).thenReturn(available);
        lenient().when(stack.getBlueprint()).thenReturn(blueprint);
        lenient().when(stack.getStackVersion()).thenReturn(version);
        lenient().when(entitlementService.isZookeeperToKRaftMigrationEnabled(ACCOUNT_ID)).thenReturn(migrationEnabled);

        assertFalse(underTest.isMigrationFromZookeeperToKraftSupported(stack, ACCOUNT_ID));
    }

    private static Stream<Arguments> testIsMigrationFromZookeeperToKraftNotSupportedParameters() {
        return Stream.of(
                Arguments.of(false, LOWER_VERSION, false),
                Arguments.of(true, VALID_VERSION, false),
                Arguments.of(true, LOWER_VERSION, true),
                Arguments.of(false, VALID_VERSION, true),
                Arguments.of(false, LOWER_VERSION, true)
        );
    }

    @Test
    void testValidateZookeeperToKraftMigrationStateForFinalization() {
        assertDoesNotThrow(() -> underTest.validateZookeeperToKraftMigrationStateForFinalization(KraftMigrationStatus.BROKERS_IN_KRAFT.name()));
    }

    @Test
    void testValidateZookeeperToKraftMigrationStateForFinalizationWhenAlreadyFinalized() {
        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> underTest.validateZookeeperToKraftMigrationStateForFinalization(KraftMigrationStatus.KRAFT_INSTALLED.name()));

        assertEquals("Cannot finalize KRaft migration. KRaft migration is already finalized for this cluster.",
                exception.getMessage());
    }

    @Test
    void testValidateZookeeperToKraftMigrationStateForFinalizationWhenNotMigratedYet() {
        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> underTest.validateZookeeperToKraftMigrationStateForFinalization(KraftMigrationStatus.ZOOKEEPER_INSTALLED.name()));

        assertEquals("Cannot finalize KRaft migration. The cluster has not been migrated to KRaft yet.",
                exception.getMessage());
    }

    @Test
    void testValidateZookeeperToKraftMigrationStateForRollback() {
        assertDoesNotThrow(() -> underTest.validateZookeeperToKraftMigrationStateForRollback(KraftMigrationStatus.BROKERS_IN_KRAFT.name()));
    }

    @Test
    void testValidateZookeeperToKraftMigrationStateForRollbackWhenAlreadyFinalized() {
        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> underTest.validateZookeeperToKraftMigrationStateForRollback(KraftMigrationStatus.KRAFT_INSTALLED.name()));

        assertEquals("Cannot rollback KRaft migration. KRaft migration is already finalized for this cluster.",
                exception.getMessage());
    }

    @Test
    void testValidateZookeeperToKraftMigrationStateForRollbackWhenNotMigratedYet() {
        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> underTest.validateZookeeperToKraftMigrationStateForRollback(KraftMigrationStatus.ZOOKEEPER_INSTALLED.name()));

        assertEquals("Cannot rollback KRaft migration. The cluster has not been migrated to KRaft yet.",
                exception.getMessage());
    }

}