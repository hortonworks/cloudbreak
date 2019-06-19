package com.sequenceiq.redbeams.api.endpoint.v4.stacks;

import static org.junit.Assert.assertEquals;

import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;

import java.util.Map;

import org.junit.Before;
import org.junit.Test;

public class AwsDatabaseServerV4ParametersTest {

    private AwsDatabaseServerV4Parameters underTest;

    @Before
    public void setUp() {
        underTest = new AwsDatabaseServerV4Parameters();
    }

    @Test
    public void testGettersAndSetters() {
        underTest.setBackupRetentionPeriod(3);
        assertEquals(3, underTest.getBackupRetentionPeriod().intValue());

        underTest.setEngineVersion("1.2.3");
        assertEquals("1.2.3", underTest.getEngineVersion());
    }

    @Test
    public void testAsMap() {
        underTest.setBackupRetentionPeriod(3);
        underTest.setEngineVersion("1.2.3");

        Map<String, Object> map = underTest.asMap();

        assertEquals(3, ((Integer) map.get("backupRetentionPeriod")).intValue());
        assertEquals("1.2.3", map.get("engineVersion"));
    }

    @Test
    public void testGetCloudPlatform() {
        assertEquals(CloudPlatform.AWS, underTest.getCloudPlatform());
    }

    @Test
    public void testParse() {
        Map<String, Object> parameters = Map.of("backupRetentionPeriod", 3, "engineVersion", "1.2.3");

        underTest.parse(parameters);

        assertEquals(3, underTest.getBackupRetentionPeriod().intValue());
        assertEquals("1.2.3", underTest.getEngineVersion());
    }

}
