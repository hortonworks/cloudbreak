package com.sequenceiq.cloudbreak.converter.v4.stacks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.database.DatabaseAvailabilityType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.database.DatabaseRequest;
import com.sequenceiq.cloudbreak.domain.stack.Stack;

class StackToExternalDatabaseRequestConverterTest {

    private static final DatabaseAvailabilityType TEST_EXTERNAL_DB_CREATION_TYPE = DatabaseAvailabilityType.HA;

    private static final String TEST_DB_ENGINE_VERSION = "someVersion";

    private StackToExternalDatabaseRequestConverter underTest;

    @BeforeEach
    void setUp() {
        underTest = new StackToExternalDatabaseRequestConverter();
    }

    @Test
    void testWhenEveryFieldIsNullThenNullShouldReturn() {
        assertNull(underTest.convert(new Stack()));
    }

    @Test
    void testWhenOnlyEngineVersionFilledThenOnlyThatFieldShouldBeFilledInTheResult() {
        Stack stack = new Stack();
        stack.setExternalDatabaseEngineVersion(TEST_DB_ENGINE_VERSION);

        DatabaseRequest result = underTest.convert(stack);

        assertEquals(TEST_DB_ENGINE_VERSION, result.getDatabaseEngineVersion());
        assertNull(result.getAvailabilityType());
    }

    @Test
    void testWhenOnlyCreationTypeFilledThenOnlyAvailabilityTypeFieldShouldBeFilledInTheResult() {
        Stack stack = new Stack();
        stack.setExternalDatabaseCreationType(TEST_EXTERNAL_DB_CREATION_TYPE);

        DatabaseRequest result = underTest.convert(stack);

        assertEquals(TEST_EXTERNAL_DB_CREATION_TYPE, result.getAvailabilityType());
        assertNull(result.getDatabaseEngineVersion());
    }

    @Test
    void testWhenBothFieldsAreFilledThenBothFieldsShouldBeFilledInTheResult() {
        Stack stack = new Stack();
        stack.setExternalDatabaseCreationType(TEST_EXTERNAL_DB_CREATION_TYPE);
        stack.setExternalDatabaseEngineVersion(TEST_DB_ENGINE_VERSION);

        DatabaseRequest result = underTest.convert(stack);

        assertEquals(TEST_EXTERNAL_DB_CREATION_TYPE, result.getAvailabilityType());
        assertEquals(TEST_DB_ENGINE_VERSION, result.getDatabaseEngineVersion());
    }

}