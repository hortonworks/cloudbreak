package com.sequenceiq.cloudbreak.service.stackpatch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.database.DatabaseAvailabilityType;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.domain.stack.Database;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.StackPatchType;
import com.sequenceiq.cloudbreak.service.stack.flow.StackRotationService;

@ExtendWith(MockitoExtension.class)
class EmbeddedDbCertificateRotationPatchServiceTest {

    private static final String RESOURCE_CRN = "resource-crn";

    @Mock
    private StackRotationService stackRotationService;

    @InjectMocks
    private EmbeddedDbCertificateRotationPatchService underTest;

    @Test
    void testGetStackPatchType() {
        assertEquals(StackPatchType.EMBEDDED_DB_CERTIFICATE_ROTATION, underTest.getStackPatchType());
    }

    static Stream<Arguments> testIsAffectedParameters() {
        return Stream.of(
                Arguments.of(null, false),
                Arguments.of(getDatabase(false, null), false),
                Arguments.of(getDatabase(true, null), false),
                Arguments.of(getDatabase(true, DatabaseAvailabilityType.NON_HA), false),
                Arguments.of(getDatabase(true, DatabaseAvailabilityType.HA), false),

                Arguments.of(getDatabase(true, DatabaseAvailabilityType.NONE), true),
                Arguments.of(getDatabase(true, DatabaseAvailabilityType.ON_ROOT_VOLUME), true),
                Arguments.of(getDatabase(true, DatabaseAvailabilityType.NONE), true),
                Arguments.of(getDatabase(true, DatabaseAvailabilityType.NONE), true)
        );
    }

    @MethodSource("testIsAffectedParameters")
    @ParameterizedTest
    void testIsAffected(Database database, boolean expected) {
        Stack stack = mock();
        when(stack.getDatabase()).thenReturn(database);

        assertEquals(expected, underTest.isAffected(stack));
    }

    @Test
    void testDoApply() throws ExistingStackPatchApplyException {
        Stack stack = mock();
        when(stack.getResourceCrn()).thenReturn(RESOURCE_CRN);

        boolean result = underTest.doApply(stack);

        verify(stackRotationService).rotateSecrets(RESOURCE_CRN, List.of("EMBEDDED_DB_SSL_CERT"), null, Map.of());
        assertTrue(result);
    }

    @Test
    void testDoApplyWhenValidationError() throws ExistingStackPatchApplyException {
        Stack stack = mock();
        when(stack.getResourceCrn()).thenReturn(RESOURCE_CRN);
        when(stackRotationService.rotateSecrets(RESOURCE_CRN, List.of("EMBEDDED_DB_SSL_CERT"), null, Map.of())).thenThrow(BadRequestException.class);

        boolean result = underTest.doApply(stack);

        verify(stackRotationService).rotateSecrets(RESOURCE_CRN, List.of("EMBEDDED_DB_SSL_CERT"), null, Map.of());
        assertFalse(result);
    }

    @Test
    void testDoApplyWhenUnexpectedError() {
        Stack stack = mock();
        when(stack.getResourceCrn()).thenReturn(RESOURCE_CRN);
        when(stackRotationService.rotateSecrets(RESOURCE_CRN, List.of("EMBEDDED_DB_SSL_CERT"), null, Map.of())).thenThrow(RuntimeException.class);

        assertThrows(ExistingStackPatchApplyException.class, () -> underTest.doApply(stack));
    }

    private static Database getDatabase(boolean hasExternalDatabaseAvailabilityType, DatabaseAvailabilityType type) {
        Database database = mock();
        if (hasExternalDatabaseAvailabilityType) {
            when(database.getExternalDatabaseAvailabilityType()).thenReturn(type);
        } else {
            when(database.getExternalDatabaseAvailabilityType()).thenReturn(null);
        }
        return database;
    }
}
