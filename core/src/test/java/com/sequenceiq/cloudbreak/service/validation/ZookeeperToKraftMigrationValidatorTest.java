package com.sequenceiq.cloudbreak.service.validation;

import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_ENABLE_ZOOKEEPER_TO_KRAFT_MIGRATION;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;
import com.sequenceiq.cloudbreak.cluster.service.ClouderaManagerProductsProvider;
import com.sequenceiq.cloudbreak.cluster.status.KraftMigrationStatus;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.view.ClusterComponentView;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.service.blueprint.BlueprintService;

@ExtendWith(MockitoExtension.class)
class ZookeeperToKraftMigrationValidatorTest {

    private static final String TEST_BP_JSON_TEXT = "{does not matter what is here}";

    private static final String KAFKA_SERVICE_TYPE = "KAFKA";

    private static final String ACCOUNT_ID = "test-account-id";

    private static final String VALID_VERSION = "7.3.2-1.cdh7.3.2.p10000.80393083";

    private static final String HIGHER_VERSION = "7.4.0-1.cdh7.4.0.p0.80393083";

    private static final String LOWER_VERSION = "7.3.1-1.cdh7.3.1.p10000.80393083";

    private static final String VALID_BLUEPRINT_VERSION = "7.2.17";

    private static final String HIGHER_BLUEPRINT_VERSION = "7.3.2";

    private static final String LOWER_BLUEPRINT_VERSION = "7.2.16";

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
        underTest = new ZookeeperToKraftMigrationValidator(entitlementService, mockBlueprintService, new ClouderaManagerProductsProvider());
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
    void testValidateZookeeperToKraftMigrationEligibilityForExistingMigrationWithOlderPatch() {
        when(status.isAvailable()).thenReturn(true);
        when(stack.getBlueprint()).thenReturn(blueprint);
        when(blueprint.getStackVersion()).thenReturn(VALID_BLUEPRINT_VERSION);
        when(entitlementService.isZookeeperToKRaftMigrationEnabled(ACCOUNT_ID)).thenReturn(true);
        when(mockBlueprintService.anyOfTheServiceTypesPresentOnBlueprint(TEST_BP_JSON_TEXT, List.of(KAFKA_SERVICE_TYPE))).thenReturn(true);
        lenient().when(stack.getClusterComponents()).thenReturn(Set.of(newCdhComponent("7.3.2-1.cdh7.3.2.p100.80393083")));

        assertDoesNotThrow(() -> underTest.validateZookeeperToKraftMigrationEligibility(stack, ACCOUNT_ID));
        verify(stack, never()).getClusterComponents();
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
        when(blueprint.getStackVersion()).thenReturn(VALID_BLUEPRINT_VERSION);

        assertDoesNotThrow(() -> underTest.validateZookeeperToKraftMigrationEligibility(stack, ACCOUNT_ID));
    }

    @ParameterizedTest
    @MethodSource("unsupportedCdhVersions")
    void testValidateZookeeperToKraftMigrationRuntimeVersionWithLowVersion(String version, String fullVersion) {
        setCdhVersion(version);

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> underTest.validateZookeeperToKraftMigrationRuntimeVersion(stack));

        assertEquals("Zookeeper to KRaft migration is supported only for Cloudera Runtime version 7.3.2.10000 or higher. "
                + "Current Cloudera Runtime version is: " + fullVersion, exception.getMessage());
    }

    @Test
    void testValidateZookeeperToKraftMigrationEligibilityWhenEntitlementNotEnabled() {
        when(stack.getStatus()).thenReturn(status);
        when(status.isAvailable()).thenReturn(true);
        when(stack.getBlueprint()).thenReturn(blueprint);
        when(blueprint.getStackVersion()).thenReturn(VALID_BLUEPRINT_VERSION);
        when(entitlementService.isZookeeperToKRaftMigrationEnabled(ACCOUNT_ID)).thenReturn(false);
        when(mockBlueprintService.anyOfTheServiceTypesPresentOnBlueprint(TEST_BP_JSON_TEXT, List.of(KAFKA_SERVICE_TYPE))).thenReturn(true);

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> underTest.validateZookeeperToKraftMigrationEligibility(stack, ACCOUNT_ID));

        assertEquals(String.format("Your account is not entitled to perform Zookeeper to KRaft migration. Please contact Cloudera to enable '%s' " +
                "entitlement for your account.", CDP_ENABLE_ZOOKEEPER_TO_KRAFT_MIGRATION), exception.getMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = {VALID_VERSION, "7.3.2-1.cdh7.3.2.p10001.80393083", "7.3.2-1.cdh7.3.2.p20000.80393083",
            "7.3.3-1.cdh7.3.3.p0.80393083", HIGHER_VERSION})
    void testValidateZookeeperToKraftMigrationRuntimeVersionWithSupportedVersion(String version) {
        setCdhVersion(version);

        assertDoesNotThrow(() -> underTest.validateZookeeperToKraftMigrationRuntimeVersion(stack));
    }

    @Test
    void testRuntimeVersionSupportDoesNotCheckEntitlementOrBlueprint() {
        setCdhVersion(VALID_VERSION);

        assertTrue(underTest.isZookeeperToKRaftMigrationSupportedForRuntimeVersion(stack));
        verifyNoInteractions(entitlementService, mockBlueprintService);
    }

    @Test
    void testValidateZookeeperToKraftMigrationEligibilityWithLowerBlueprintVersion() {
        when(stack.getStatus()).thenReturn(status);
        when(status.isAvailable()).thenReturn(true);
        when(stack.getBlueprint()).thenReturn(blueprint);
        when(blueprint.getStackVersion()).thenReturn(LOWER_BLUEPRINT_VERSION);
        when(mockBlueprintService.anyOfTheServiceTypesPresentOnBlueprint(TEST_BP_JSON_TEXT, List.of(KAFKA_SERVICE_TYPE))).thenReturn(true);

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> underTest.validateZookeeperToKraftMigrationEligibility(stack, ACCOUNT_ID));

        assertEquals("Zookeeper to KRaft migration is currently unavailable for clusters originally created with Runtime " + LOWER_BLUEPRINT_VERSION
                + ". Please contact Cloudera Support for assistance.", exception.getMessage());
    }

    @Test
    void testValidateZookeeperToKraftMigrationEligibilityWithMinimumBlueprintVersion() {
        when(stack.getStatus()).thenReturn(status);
        when(status.isAvailable()).thenReturn(true);
        when(stack.getBlueprint()).thenReturn(blueprint);
        when(blueprint.getStackVersion()).thenReturn(VALID_BLUEPRINT_VERSION);
        when(entitlementService.isZookeeperToKRaftMigrationEnabled(ACCOUNT_ID)).thenReturn(true);
        when(mockBlueprintService.anyOfTheServiceTypesPresentOnBlueprint(TEST_BP_JSON_TEXT, List.of(KAFKA_SERVICE_TYPE))).thenReturn(true);

        assertDoesNotThrow(() -> underTest.validateZookeeperToKraftMigrationEligibility(stack, ACCOUNT_ID));
    }

    @Test
    void testValidateZookeeperToKraftMigrationEligibilityWithHigherBlueprintVersion() {
        when(stack.getStatus()).thenReturn(status);
        when(status.isAvailable()).thenReturn(true);
        when(stack.getBlueprint()).thenReturn(blueprint);
        when(blueprint.getStackVersion()).thenReturn(HIGHER_BLUEPRINT_VERSION);
        when(entitlementService.isZookeeperToKRaftMigrationEnabled(ACCOUNT_ID)).thenReturn(true);
        when(mockBlueprintService.anyOfTheServiceTypesPresentOnBlueprint(TEST_BP_JSON_TEXT, List.of(KAFKA_SERVICE_TYPE))).thenReturn(true);

        assertDoesNotThrow(() -> underTest.validateZookeeperToKraftMigrationEligibility(stack, ACCOUNT_ID));
    }

    @Test
    void testValidateZookeeperToKraftMigrationState() {
        assertDoesNotThrow(() -> underTest.validateZookeeperToKraftMigrationState(KraftMigrationStatus.ZOOKEEPER_INSTALLED));
        assertDoesNotThrow(() -> underTest.validateZookeeperToKraftMigrationState(KraftMigrationStatus.PRE_MIGRATION));
        assertDoesNotThrow(() -> underTest.validateZookeeperToKraftMigrationState(KraftMigrationStatus.BROKERS_IN_KRAFT));
        assertDoesNotThrow(() -> underTest.validateZookeeperToKraftMigrationState(KraftMigrationStatus.KRAFT_INSTALLED));
    }

    @Test
    void testValidateZookeeperToKraftMigrationStateWhenMigrationInProgress() {
        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> underTest.validateZookeeperToKraftMigrationState(KraftMigrationStatus.BROKERS_IN_MIGRATION));

        assertEquals("Cannot start KRaft migration. The cluster has [BROKERS_IN_MIGRATION] KRaft migration status.",
                exception.getMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = {VALID_VERSION, "7.3.2-1.cdh7.3.2.p10001.80393083", "7.3.2-1.cdh7.3.2.p20000.80393083",
            "7.3.3-1.cdh7.3.3.p0.80393083", HIGHER_VERSION})
    void testIsMigrationFromZookeeperToKraftSupported(String version) {
        when(stack.getStackVersion()).thenReturn("7.3.2");
        when(stack.getBlueprint()).thenReturn(blueprint);
        setCdhVersion(version);
        when(blueprint.getStackVersion()).thenReturn(VALID_BLUEPRINT_VERSION);
        when(entitlementService.isZookeeperToKRaftMigrationEnabled(ACCOUNT_ID)).thenReturn(true);
        when(mockBlueprintService.anyOfTheServiceTypesPresentOnBlueprint(TEST_BP_JSON_TEXT, List.of(KAFKA_SERVICE_TYPE))).thenReturn(true);

        assertTrue(underTest.isMigrationFromZookeeperToKraftSupported(stack, ACCOUNT_ID));
    }

    @ParameterizedTest
    @MethodSource("testIsMigrationFromZookeeperToKraftNotSupportedParameters")
    void testIsMigrationFromZookeeperToKraftNotSupported(String version, String blueprintVersion, boolean migrationEnabled) {
        when(stack.getStackVersion()).thenReturn("7.3.2");
        lenient().when(stack.getBlueprint()).thenReturn(blueprint);
        lenient().when(stack.getClusterComponents()).thenReturn(Set.of(newCdhComponent(version)));
        lenient().when(blueprint.getStackVersion()).thenReturn(blueprintVersion);
        lenient().when(entitlementService.isZookeeperToKRaftMigrationEnabled(ACCOUNT_ID)).thenReturn(migrationEnabled);
        when(mockBlueprintService.anyOfTheServiceTypesPresentOnBlueprint(TEST_BP_JSON_TEXT, List.of(KAFKA_SERVICE_TYPE))).thenReturn(true);

        assertFalse(underTest.isMigrationFromZookeeperToKraftSupported(stack, ACCOUNT_ID));
    }

    private static Stream<Arguments> testIsMigrationFromZookeeperToKraftNotSupportedParameters() {
        return Stream.of(
                Arguments.of(LOWER_VERSION, VALID_BLUEPRINT_VERSION, false),
                Arguments.of(VALID_VERSION, VALID_BLUEPRINT_VERSION, false),
                Arguments.of(LOWER_VERSION, VALID_BLUEPRINT_VERSION, true),
                Arguments.of("7.3.2", VALID_BLUEPRINT_VERSION, true),
                Arguments.of("7.3.2-1.cdh7.3.2.p0.80393083", VALID_BLUEPRINT_VERSION, true),
                Arguments.of("7.3.2-1.cdh7.3.2.p100.80393083", VALID_BLUEPRINT_VERSION, true),
                Arguments.of("7.3.2-1.cdh7.3.2.p999.80393083", VALID_BLUEPRINT_VERSION, true),
                Arguments.of("7.3.2-1.cdh7.3.2.p1000.80393083", VALID_BLUEPRINT_VERSION, true),
                Arguments.of("7.3.2-1.cdh7.3.2.p9999.80393083", VALID_BLUEPRINT_VERSION, true),
                Arguments.of(VALID_VERSION, LOWER_BLUEPRINT_VERSION, true)
        );
    }

    private static Stream<Arguments> unsupportedCdhVersions() {
        return Stream.of(
                Arguments.of(LOWER_VERSION, "7.3.1.10000"),
                Arguments.of("7.3.2-1.cdh7.3.2.p0.80393083", "7.3.2.0"),
                Arguments.of("7.3.2-1.cdh7.3.2.p100.80393083", "7.3.2.100"),
                Arguments.of("7.3.2-1.cdh7.3.2.p999.80393083", "7.3.2.999"),
                Arguments.of("7.3.2-1.cdh7.3.2.p1000.80393083", "7.3.2.1000"),
                Arguments.of("7.3.2-1.cdh7.3.2.p9999.80393083", "7.3.2.9999")
        );
    }

    @Test
    void testValidateZookeeperToKraftMigrationRuntimeVersionWithoutCdhProduct() {
        when(stack.getClusterComponents()).thenReturn(Set.of());

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> underTest.validateZookeeperToKraftMigrationRuntimeVersion(stack));

        assertEquals("Cannot determine the Cloudera Runtime version including the patch version. Zookeeper to KRaft migration cannot be started.",
                exception.getMessage());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "invalid-version", "7.3.2", "7.4.0"})
    void testValidateZookeeperToKraftMigrationRuntimeVersionWithoutFullCdhVersion(String version) {
        setCdhVersion(version);

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> underTest.validateZookeeperToKraftMigrationRuntimeVersion(stack));

        assertEquals("Cannot determine the Cloudera Runtime version including the patch version. Zookeeper to KRaft migration cannot be started.",
                exception.getMessage());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "invalid-version", "7.3.2", "7.4.0"})
    void testIsMigrationFromZookeeperToKraftNotSupportedWithoutFullCdhVersion(String version) {
        when(stack.getStackVersion()).thenReturn("7.3.2");
        when(stack.getBlueprint()).thenReturn(blueprint);
        when(blueprint.getStackVersion()).thenReturn(VALID_BLUEPRINT_VERSION);
        setCdhVersion(version);
        when(entitlementService.isZookeeperToKRaftMigrationEnabled(ACCOUNT_ID)).thenReturn(true);
        when(mockBlueprintService.anyOfTheServiceTypesPresentOnBlueprint(TEST_BP_JSON_TEXT, List.of(KAFKA_SERVICE_TYPE))).thenReturn(true);

        assertFalse(underTest.isMigrationFromZookeeperToKraftSupported(stack, ACCOUNT_ID));
    }

    @Test
    void testIsMigrationFromZookeeperToKraftNotSupportedWithoutCdhProduct() {
        when(stack.getStackVersion()).thenReturn("7.3.2");
        when(stack.getBlueprint()).thenReturn(blueprint);
        when(blueprint.getStackVersion()).thenReturn(VALID_BLUEPRINT_VERSION);
        when(stack.getClusterComponents()).thenReturn(Set.of());
        when(entitlementService.isZookeeperToKRaftMigrationEnabled(ACCOUNT_ID)).thenReturn(true);
        when(mockBlueprintService.anyOfTheServiceTypesPresentOnBlueprint(TEST_BP_JSON_TEXT, List.of(KAFKA_SERVICE_TYPE))).thenReturn(true);

        assertFalse(underTest.isMigrationFromZookeeperToKraftSupported(stack, ACCOUNT_ID));
    }

    @ParameterizedTest
    @ValueSource(strings = {"7.3.2", "7.4.0"})
    void testKraftMigrationStatusSupportedWithoutCheckingPatchVersion(String stackVersion) {
        when(stack.getStackVersion()).thenReturn(stackVersion);
        when(stack.getBlueprint()).thenReturn(blueprint);
        when(blueprint.getStackVersion()).thenReturn(VALID_BLUEPRINT_VERSION);
        when(entitlementService.isZookeeperToKRaftMigrationEnabled(ACCOUNT_ID)).thenReturn(true);
        when(mockBlueprintService.anyOfTheServiceTypesPresentOnBlueprint(TEST_BP_JSON_TEXT, List.of(KAFKA_SERVICE_TYPE))).thenReturn(true);

        assertTrue(underTest.isKraftMigrationStatusSupported(stack, ACCOUNT_ID));
        verify(stack, never()).getClusterComponents();
    }

    @Test
    void testKraftMigrationStatusNotSupportedForRuntimeBeforeKraftMigration() {
        when(stack.getStackVersion()).thenReturn("7.2.18");
        when(stack.getBlueprint()).thenReturn(blueprint);
        when(entitlementService.isZookeeperToKRaftMigrationEnabled(ACCOUNT_ID)).thenReturn(true);
        when(mockBlueprintService.anyOfTheServiceTypesPresentOnBlueprint(TEST_BP_JSON_TEXT, List.of(KAFKA_SERVICE_TYPE))).thenReturn(true);

        assertFalse(underTest.isKraftMigrationStatusSupported(stack, ACCOUNT_ID));
        verify(stack, never()).getClusterComponents();
    }

    private void setCdhVersion(String version) {
        when(stack.getClusterComponents()).thenReturn(Set.of(newCdhComponent(version)));
    }

    private ClusterComponentView newCdhComponent(String version) {
        ClusterComponentView cdhComponent = new ClusterComponentView();
        cdhComponent.setName("CDH");
        cdhComponent.setAttributes(new Json(new ClouderaManagerProduct().withName("CDH").withVersion(version)));
        return cdhComponent;
    }

    @Test
    void testValidateZookeeperToKraftMigrationStateForFinalization() {
        assertDoesNotThrow(() -> underTest.validateZookeeperToKraftMigrationStateForFinalization(KraftMigrationStatus.BROKERS_IN_KRAFT));
        assertDoesNotThrow(() -> underTest.validateZookeeperToKraftMigrationStateForFinalization(KraftMigrationStatus.KRAFT_INSTALLED));
    }

    @Test
    void testValidateZookeeperToKraftMigrationStateForFinalizationWhenNotMigratedYet() {
        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> underTest.validateZookeeperToKraftMigrationStateForFinalization(KraftMigrationStatus.ZOOKEEPER_INSTALLED));

        assertEquals("Cannot finalize KRaft migration. The cluster has [ZOOKEEPER_INSTALLED] KRaft migration status.",
                exception.getMessage());
    }
}
