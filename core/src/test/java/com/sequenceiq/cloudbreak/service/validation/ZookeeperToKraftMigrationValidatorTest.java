package com.sequenceiq.cloudbreak.service.validation;

import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_ENABLE_ZOOKEEPER_TO_KRAFT_MIGRATION;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.util.ReflectionUtils;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.cluster.status.KraftMigrationStatus;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.dto.StackDto;

@ExtendWith(MockitoExtension.class)
class ZookeeperToKraftMigrationValidatorTest {

    private static final String ACCOUNT_ID = "test-account-id";

    private static final String VALID_VERSION = "7.3.2";

    private static final String HIGHER_VERSION = "7.4.0";

    private static final String LOWER_VERSION = "7.3.1";

    private static final String INVALID_TEMPLATE = "Streaming Light Duty";

    private static final String STREAMS_MESSAGING_LIGHT_DUTY = "Streams Messaging Light Duty";

    private static final String STREAMS_MESSAGING_HIGH_AVAILABILITY = "Streams Messaging High Availability";

    private static final String STREAMS_MESSAGING_HEAVY_DUTY = "Streams Messaging Heavy Duty";

    @Mock
    private EntitlementService entitlementService;

    @Mock
    private StackDto stack;

    @Mock
    private Status status;

    @Mock
    private Blueprint blueprint;

    @InjectMocks
    private ZookeeperToKraftMigrationValidator underTest;

    @BeforeEach
    void setup() {
        initGlobalPrivateFields();
    }

    @ParameterizedTest
    @ValueSource(strings = {STREAMS_MESSAGING_LIGHT_DUTY, STREAMS_MESSAGING_HIGH_AVAILABILITY, STREAMS_MESSAGING_HEAVY_DUTY})
    void testValidateZookeeperToKraftMigrationEligibilityWithValidTemplates(String template) {
        when(stack.getStatus()).thenReturn(status);
        when(status.isAvailable()).thenReturn(true);
        when(stack.getBlueprint()).thenReturn(blueprint);
        when(blueprint.getName()).thenReturn(template);
        when(stack.getStackVersion()).thenReturn(VALID_VERSION);
        when(entitlementService.isZookeeperToKRaftMigrationEnabled(ACCOUNT_ID)).thenReturn(true);

        assertDoesNotThrow(() -> underTest.validateZookeeperToKraftMigrationEligibility(stack, ACCOUNT_ID));
    }

    @Test
    void testValidateZookeeperToKraftMigrationEligibilityWithHigherVersion() {
        when(stack.getStatus()).thenReturn(status);
        when(status.isAvailable()).thenReturn(true);
        when(stack.getBlueprint()).thenReturn(blueprint);
        when(blueprint.getName()).thenReturn("Streams Messaging Light Duty");
        when(stack.getStackVersion()).thenReturn(HIGHER_VERSION);
        when(entitlementService.isZookeeperToKRaftMigrationEnabled(ACCOUNT_ID)).thenReturn(true);

        assertDoesNotThrow(() -> underTest.validateZookeeperToKraftMigrationEligibility(stack, ACCOUNT_ID));
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
    void testValidateZookeeperToKraftMigrationEligibilityWithUnsupportedTemplateType() {
        when(stack.getStatus()).thenReturn(status);
        when(status.isAvailable()).thenReturn(true);
        when(stack.getBlueprint()).thenReturn(blueprint);
        when(blueprint.getName()).thenReturn("Unsupported Template Type");

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> underTest.validateZookeeperToKraftMigrationEligibility(stack, ACCOUNT_ID));

        String message = exception.getMessage();
        assertThat(message, containsString("Zookeeper to KRaft migration is supported only for the following template types:"));
        assertThat(message, containsString("Streams Messaging High Availability"));
        assertThat(message, containsString("Streams Messaging Light Duty"));
        assertThat(message, containsString("Streams Messaging Heavy Duty"));
    }

    @Test
    void testValidateZookeeperToKraftMigrationEligibilityWithNullBlueprint() {
        when(stack.getStatus()).thenReturn(status);
        when(status.isAvailable()).thenReturn(true);
        when(stack.getBlueprint()).thenReturn(null);

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> underTest.validateZookeeperToKraftMigrationEligibility(stack, ACCOUNT_ID));

        String message = exception.getMessage();
        assertThat(message, containsString("Zookeeper to KRaft migration is supported only for the following template types:"));
        assertThat(message, containsString("Streams Messaging High Availability"));
        assertThat(message, containsString("Streams Messaging Light Duty"));
        assertThat(message, containsString("Streams Messaging Heavy Duty"));
    }

    @Test
    void testValidateZookeeperToKraftMigrationEligibilityWithLowVersion() {
        when(stack.getStatus()).thenReturn(status);
        when(status.isAvailable()).thenReturn(true);
        when(stack.getBlueprint()).thenReturn(blueprint);
        when(blueprint.getName()).thenReturn("Streams Messaging Light Duty");
        when(stack.getStackVersion()).thenReturn(LOWER_VERSION);

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> underTest.validateZookeeperToKraftMigrationEligibility(stack, ACCOUNT_ID));

        assertEquals("Zookeeper to KRaft migration is supported only for CDP version 7.3.2 or higher",
                exception.getMessage());
    }

    @Test
    void testValidateZookeeperToKraftMigrationEligibilityWhenEntitlementNotEnabled() {
        when(stack.getStatus()).thenReturn(status);
        when(status.isAvailable()).thenReturn(true);
        when(stack.getBlueprint()).thenReturn(blueprint);
        when(blueprint.getName()).thenReturn("Streams Messaging Light Duty");
        when(stack.getStackVersion()).thenReturn(VALID_VERSION);
        when(entitlementService.isZookeeperToKRaftMigrationEnabled(ACCOUNT_ID)).thenReturn(false);

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
        when(blueprint.getName()).thenReturn("Streams Messaging Light Duty");
        when(stack.getStackVersion()).thenReturn("7.3.2");
        when(entitlementService.isZookeeperToKRaftMigrationEnabled(ACCOUNT_ID)).thenReturn(true);

        assertDoesNotThrow(() -> underTest.validateZookeeperToKraftMigrationEligibility(stack, ACCOUNT_ID));
    }

    @Test
    void testValidateZookeeperToKraftMigrationState() {
        assertDoesNotThrow(() -> underTest.validateZookeeperToKraftMigrationState(KraftMigrationStatus.ZOOKEEPER_INSTALLED.name()));
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
        when(blueprint.getName()).thenReturn(STREAMS_MESSAGING_HIGH_AVAILABILITY);
        when(stack.getStackVersion()).thenReturn(VALID_VERSION);
        when(entitlementService.isZookeeperToKRaftMigrationEnabled(ACCOUNT_ID)).thenReturn(true);

        assertTrue(underTest.isMigrationFromZookeeperToKraftSupported(stack, ACCOUNT_ID));
    }

    @ParameterizedTest
    @MethodSource("testIsMigrationFromZookeeperToKraftNotSupportedParameters")
    void testIsMigrationFromZookeeperToKraftNotSupported() {
        when(stack.getStatus()).thenReturn(status);
        when(status.isAvailable()).thenReturn(true);
        when(stack.getBlueprint()).thenReturn(blueprint);
        when(blueprint.getName()).thenReturn(STREAMS_MESSAGING_HIGH_AVAILABILITY);
        when(stack.getStackVersion()).thenReturn(VALID_VERSION);
        when(entitlementService.isZookeeperToKRaftMigrationEnabled(ACCOUNT_ID)).thenReturn(false);

        assertFalse(underTest.isMigrationFromZookeeperToKraftSupported(stack, ACCOUNT_ID));
    }

    private static Stream<Arguments> testIsMigrationFromZookeeperToKraftNotSupportedParameters() {
        return Stream.of(
                Arguments.of(false, INVALID_TEMPLATE, LOWER_VERSION, false),
                Arguments.of(true, STREAMS_MESSAGING_LIGHT_DUTY, VALID_VERSION, false),
                Arguments.of(true, STREAMS_MESSAGING_LIGHT_DUTY, LOWER_VERSION, true),
                Arguments.of(true, INVALID_TEMPLATE, VALID_VERSION, true),
                Arguments.of(false, STREAMS_MESSAGING_LIGHT_DUTY, VALID_VERSION, true)
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

    private void initGlobalPrivateFields() {
        Field kraftMigrationSupportedTemplates = ReflectionUtils.findField(ZookeeperToKraftMigrationValidator.class, "kraftMigrationSupportedTemplates");
        ReflectionUtils.makeAccessible(kraftMigrationSupportedTemplates);
        ReflectionUtils.setField(kraftMigrationSupportedTemplates, underTest, Set.of(STREAMS_MESSAGING_LIGHT_DUTY, STREAMS_MESSAGING_HIGH_AVAILABILITY,
                STREAMS_MESSAGING_HEAVY_DUTY));
    }
}