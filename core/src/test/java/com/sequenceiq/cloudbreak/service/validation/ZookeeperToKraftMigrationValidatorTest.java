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
        when(stack.getStatus()).thenReturn(status);
    }

    @ParameterizedTest
    @ValueSource(strings = {STREAMS_MESSAGING_LIGHT_DUTY, STREAMS_MESSAGING_HIGH_AVAILABILITY, STREAMS_MESSAGING_HEAVY_DUTY})
    void testValidateZookeeperToKraftMigrationWithValidTemplates(String template) {
        when(status.isAvailable()).thenReturn(true);
        when(stack.getBlueprint()).thenReturn(blueprint);
        when(blueprint.getName()).thenReturn(template);
        when(stack.getStackVersion()).thenReturn(VALID_VERSION);
        when(entitlementService.isZookeeperToKRaftMigrationEnabled(ACCOUNT_ID)).thenReturn(true);

        assertDoesNotThrow(() -> underTest.validateZookeeperToKraftMigration(stack, ACCOUNT_ID));
    }

    @Test
    void testValidateZookeeperToKraftMigrationWithHigherVersion() {
        when(status.isAvailable()).thenReturn(true);
        when(stack.getBlueprint()).thenReturn(blueprint);
        when(blueprint.getName()).thenReturn("Streams Messaging Light Duty");
        when(stack.getStackVersion()).thenReturn(HIGHER_VERSION);
        when(entitlementService.isZookeeperToKRaftMigrationEnabled(ACCOUNT_ID)).thenReturn(true);

        assertDoesNotThrow(() -> underTest.validateZookeeperToKraftMigration(stack, ACCOUNT_ID));
    }

    @Test
    void testValidateZookeeperToKraftMigrationWhenClusterNotAvailable() {
        when(status.isAvailable()).thenReturn(false);

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> underTest.validateZookeeperToKraftMigration(stack, ACCOUNT_ID));

        assertEquals("Zookeeper to KRaft migration can only be performed when the cluster is in Available state. Please ensure the cluster is " +
                        "fully operational before starting the migration.", exception.getMessage());
    }

    @Test
    void testValidateZookeeperToKraftMigrationWithUnsupportedTemplateType() {
        when(status.isAvailable()).thenReturn(true);
        when(stack.getBlueprint()).thenReturn(blueprint);
        when(blueprint.getName()).thenReturn("Unsupported Template Type");

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> underTest.validateZookeeperToKraftMigration(stack, ACCOUNT_ID));

        String message = exception.getMessage();
        assertThat(message, containsString("Zookeeper to KRaft migration is supported only for the following template types:"));
        assertThat(message, containsString("Streams Messaging High Availability"));
        assertThat(message, containsString("Streams Messaging Light Duty"));
        assertThat(message, containsString("Streams Messaging Heavy Duty"));
    }

    @Test
    void testValidateZookeeperToKraftMigrationWithLowVersion() {
        when(status.isAvailable()).thenReturn(true);
        when(stack.getBlueprint()).thenReturn(blueprint);
        when(blueprint.getName()).thenReturn("Streams Messaging Light Duty");
        when(stack.getStackVersion()).thenReturn(LOWER_VERSION);

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> underTest.validateZookeeperToKraftMigration(stack, ACCOUNT_ID));

        assertEquals("Zookeeper to KRaft migration is supported only for CDP version 7.3.2 or higher",
                exception.getMessage());
    }

    @Test
    void testValidateZookeeperToKraftMigrationWhenEntitlementNotEnabled() {
        when(status.isAvailable()).thenReturn(true);
        when(stack.getBlueprint()).thenReturn(blueprint);
        when(blueprint.getName()).thenReturn("Streams Messaging Light Duty");
        when(stack.getStackVersion()).thenReturn(VALID_VERSION);
        when(entitlementService.isZookeeperToKRaftMigrationEnabled(ACCOUNT_ID)).thenReturn(false);

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> underTest.validateZookeeperToKraftMigration(stack, ACCOUNT_ID));

        assertEquals(String.format("Your account is not entitled to perform Zookeeper to KRaft migration. Please contact Cloudera to enable '%s' " +
                        "entitlement for your account.", CDP_ENABLE_ZOOKEEPER_TO_KRAFT_MIGRATION), exception.getMessage());
    }

    @Test
    void testValidateZookeeperToKraftMigrationWithMinimumVersion() {
        when(status.isAvailable()).thenReturn(true);
        when(stack.getBlueprint()).thenReturn(blueprint);
        when(blueprint.getName()).thenReturn("Streams Messaging Light Duty");
        when(stack.getStackVersion()).thenReturn("7.3.2");
        when(entitlementService.isZookeeperToKRaftMigrationEnabled(ACCOUNT_ID)).thenReturn(true);

        assertDoesNotThrow(() -> underTest.validateZookeeperToKraftMigration(stack, ACCOUNT_ID));
    }

    @Test
    void testIsMigrationFromZookeeperToKraftSupported() {
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

    private void initGlobalPrivateFields() {
        Field kraftMigrationSupportedTemplates = ReflectionUtils.findField(ZookeeperToKraftMigrationValidator.class, "kraftMigrationSupportedTemplates");
        ReflectionUtils.makeAccessible(kraftMigrationSupportedTemplates);
        ReflectionUtils.setField(kraftMigrationSupportedTemplates, underTest, Set.of(STREAMS_MESSAGING_LIGHT_DUTY, STREAMS_MESSAGING_HIGH_AVAILABILITY,
                STREAMS_MESSAGING_HEAVY_DUTY));
    }
}