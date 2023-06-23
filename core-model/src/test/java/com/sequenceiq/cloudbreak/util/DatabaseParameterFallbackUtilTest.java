package com.sequenceiq.cloudbreak.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.database.DatabaseAvailabilityType;
import com.sequenceiq.cloudbreak.domain.stack.Database;
import com.sequenceiq.cloudbreak.domain.stack.Stack;

public class DatabaseParameterFallbackUtilTest {

    @Test
    public void testSetupDatabaseInitParams() {
        Stack stack = new Stack();
        Database database = DatabaseParameterFallbackUtil.setupDatabaseInitParams(stack, DatabaseAvailabilityType.HA, "1.0");
        assertEquals(DatabaseAvailabilityType.HA, database.getExternalDatabaseAvailabilityType());
        assertEquals("1.0", database.getExternalDatabaseEngineVersion());
        assertEquals(DatabaseAvailabilityType.HA, stack.getExternalDatabaseCreationType());
        assertEquals("1.0", stack.getExternalDatabaseEngineVersion());
    }

    @Test
    public void testGetDatabaseEngineVersion() {
        Database database = new Database();
        database.setExternalDatabaseEngineVersion("2.0");
        String fallbackDatabaseEngineVersion = "fallback2.0";
        assertEquals("2.0", DatabaseParameterFallbackUtil.getExternalDatabaseEngineVersion(database, "2.0"));
        assertEquals(fallbackDatabaseEngineVersion, DatabaseParameterFallbackUtil.getExternalDatabaseEngineVersion(database, fallbackDatabaseEngineVersion));
        assertEquals(fallbackDatabaseEngineVersion, DatabaseParameterFallbackUtil.getExternalDatabaseEngineVersion(null, fallbackDatabaseEngineVersion));
        assertEquals(fallbackDatabaseEngineVersion, DatabaseParameterFallbackUtil.getExternalDatabaseEngineVersion(
                new Database(), fallbackDatabaseEngineVersion));
    }

    @Test
    public void testGetDatabaseEngineVersionNull() {
        Database database = new Database();
        String fallbackDatabaseEngineVersion = "fallback2.0";
        assertEquals("fallback2.0", DatabaseParameterFallbackUtil.getExternalDatabaseEngineVersion(database, fallbackDatabaseEngineVersion));
    }

    @Test
    public void testGetDatabaseAvailabilityType() {
        Database sdxDatabase = new Database();
        sdxDatabase.setExternalDatabaseAvailabilityType(DatabaseAvailabilityType.NONE);
        DatabaseAvailabilityType fallbackDatabaseAvailabilityType = DatabaseAvailabilityType.HA;
        assertEquals(DatabaseAvailabilityType.NONE,
                DatabaseParameterFallbackUtil.getExternalDatabaseCreationType(sdxDatabase, fallbackDatabaseAvailabilityType));
        assertEquals(DatabaseAvailabilityType.HA,
                DatabaseParameterFallbackUtil.getExternalDatabaseCreationType(null, fallbackDatabaseAvailabilityType));
    }

    @Test
    public void testGetOrCreateDatabaseNoDB() {
        Stack stack = new Stack();
        stack.setExternalDatabaseEngineVersion("2.0");
        stack.setExternalDatabaseCreationType(DatabaseAvailabilityType.HA);
        Database actualDatabase = DatabaseParameterFallbackUtil.getOrCreateDatabase(stack);
        assertEquals("2.0", actualDatabase.getExternalDatabaseEngineVersion());
        assertEquals(DatabaseAvailabilityType.HA, actualDatabase.getExternalDatabaseAvailabilityType());
    }

    @Test
    public void testGetOrCreateDatabaseWithDB() {
        Stack stack = new Stack();
        Database database = new Database();
        database.setExternalDatabaseEngineVersion("2.0");
        database.setExternalDatabaseAvailabilityType(DatabaseAvailabilityType.HA);
        stack.setDatabase(database);
        Database actualDatabase = DatabaseParameterFallbackUtil.getOrCreateDatabase(stack);
        assertEquals(database, actualDatabase);
    }

    @Test
    public void testGetOrCreateDatabaseWithDifferentVersion() {
        Stack stack = new Stack();
        stack.setExternalDatabaseEngineVersion("1.0");
        Database database = new Database();
        database.setExternalDatabaseEngineVersion("2.0");
        database.setExternalDatabaseAvailabilityType(DatabaseAvailabilityType.HA);
        stack.setDatabase(database);
        Database actualDatabase = DatabaseParameterFallbackUtil.getOrCreateDatabase(stack);
        assertEquals(actualDatabase.getExternalDatabaseEngineVersion(), "1.0");
    }

    @Test
    public void testGetOrCreateDatabaseViewNoDB() {
        Stack stack = new Stack();
        stack.setExternalDatabaseEngineVersion("2.0");
        stack.setExternalDatabaseCreationType(DatabaseAvailabilityType.HA);
        Database actualDatabase = DatabaseParameterFallbackUtil.getOrCreateDatabase(null, stack);
        assertEquals("2.0", actualDatabase.getExternalDatabaseEngineVersion());
        assertEquals(DatabaseAvailabilityType.HA, actualDatabase.getExternalDatabaseAvailabilityType());
    }

    @Test
    public void testGetOrCreateDatabaseViewWithDB() {
        Stack stack = new Stack();
        Database database = new Database();
        database.setExternalDatabaseEngineVersion("2.0");
        database.setExternalDatabaseAvailabilityType(DatabaseAvailabilityType.HA);
        Database actualDatabase = DatabaseParameterFallbackUtil.getOrCreateDatabase(database, stack);
        assertEquals(database, actualDatabase);
    }

    @Test
    public void testGetOrCreateDatabaseViewWithDifferentVersion() {
        Stack stack = new Stack();
        stack.setExternalDatabaseEngineVersion("1.0");
        Database database = new Database();
        database.setExternalDatabaseEngineVersion("2.0");
        database.setExternalDatabaseAvailabilityType(DatabaseAvailabilityType.HA);
        Database actualDatabase = DatabaseParameterFallbackUtil.getOrCreateDatabase(database, stack);
        assertEquals(actualDatabase.getExternalDatabaseEngineVersion(), "1.0");
    }
}
