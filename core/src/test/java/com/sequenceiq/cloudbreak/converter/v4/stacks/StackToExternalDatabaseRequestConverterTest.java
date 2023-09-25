package com.sequenceiq.cloudbreak.converter.v4.stacks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.database.DatabaseAvailabilityType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.database.DatabaseRequest;
import com.sequenceiq.cloudbreak.domain.stack.Database;
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
        Stack stack = getStack(TEST_DB_ENGINE_VERSION, null);

        DatabaseRequest result = underTest.convert(stack);

        assertEquals(TEST_DB_ENGINE_VERSION, result.getDatabaseEngineVersion());
        assertNull(result.getAvailabilityType());
        assertNull(result.getDatalakeDatabaseAvailabilityType());
    }

    @Test
    void testWhenOnlyCreationTypeFilledThenOnlyAvailabilityTypeFieldShouldBeFilledInTheResult() {
        Stack stack = getStack(null, TEST_EXTERNAL_DB_CREATION_TYPE);

        DatabaseRequest result = underTest.convert(stack);

        assertEquals(TEST_EXTERNAL_DB_CREATION_TYPE, result.getAvailabilityType());
        assertNull(result.getDatabaseEngineVersion());
        assertNull(result.getDatalakeDatabaseAvailabilityType());
    }

    @Test
    void testWhenBothFieldsAreFilledThenBothFieldsShouldBeFilledInTheResult() {
        Stack stack = getStack(TEST_DB_ENGINE_VERSION, TEST_EXTERNAL_DB_CREATION_TYPE);
        stack.getDatabase().setDatalakeDatabaseAvailabilityType(TEST_EXTERNAL_DB_CREATION_TYPE);

        DatabaseRequest result = underTest.convert(stack);

        assertEquals(TEST_EXTERNAL_DB_CREATION_TYPE, result.getAvailabilityType());
        assertEquals(TEST_EXTERNAL_DB_CREATION_TYPE, result.getDatalakeDatabaseAvailabilityType());
        assertEquals(TEST_DB_ENGINE_VERSION, result.getDatabaseEngineVersion());
    }

    private Stack getStack(String dbEngineVersion, DatabaseAvailabilityType dbAvailabilityType) {
        Stack stack = new Stack();
        Database database = new Database();
        database.setExternalDatabaseEngineVersion(dbEngineVersion);
        database.setExternalDatabaseAvailabilityType(dbAvailabilityType);
        stack.setDatabase(database);
        return stack;
    }
}