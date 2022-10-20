package com.sequenceiq.redbeams.api.endpoint.v4.stacks;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Before;
import org.junit.Test;

import com.sequenceiq.redbeams.api.endpoint.v4.stacks.aws.AwsDatabaseServerV4Parameters;

public class DatabaseServerV4StackBaseTest {

    private DatabaseServerV4StackBase underTest;

    @Before
    public void setUp() throws Exception {
        underTest = new DatabaseServerV4StackBase();
    }

    @Test
    public void testGettersAndSetters() {
        underTest.setInstanceType("db.m3.medium");
        assertEquals("db.m3.medium", underTest.getInstanceType());

        underTest.setDatabaseVendor("postgresql");
        assertEquals("postgresql", underTest.getDatabaseVendor());

        underTest.setConnectionDriver("org.postgresql.Driver");
        assertEquals("org.postgresql.Driver", underTest.getConnectionDriver());

        underTest.setStorageSize(50L);
        assertEquals(50L, underTest.getStorageSize().longValue());

        underTest.setRootUserName("root");
        assertEquals("root", underTest.getRootUserName());

        underTest.setRootUserPassword("cloudera");
        assertEquals("cloudera", underTest.getRootUserPassword());
    }

    @Test
    public void testAwsParameters() {
        assertNull(underTest.getAws());

        AwsDatabaseServerV4Parameters parameters = underTest.createAws();
        assertNotNull(parameters);

        parameters = new AwsDatabaseServerV4Parameters();
        underTest.setAws(parameters);
        assertEquals(parameters, underTest.createAws());
        assertEquals(parameters, underTest.getAws());
    }

}
