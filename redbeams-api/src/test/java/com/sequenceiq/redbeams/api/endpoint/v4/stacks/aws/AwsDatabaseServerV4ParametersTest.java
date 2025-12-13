package com.sequenceiq.redbeams.api.endpoint.v4.stacks.aws;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;

class AwsDatabaseServerV4ParametersTest {

    private AwsDatabaseServerV4Parameters underTest;

    @BeforeEach
    public void setUp() {
        underTest = new AwsDatabaseServerV4Parameters();
    }

    @Test
    void testGettersAndSetters() {
        underTest.setBackupRetentionPeriod(3);
        assertEquals(3, underTest.getBackupRetentionPeriod().intValue());

        underTest.setEngineVersion("1.2.3");
        assertEquals("1.2.3", underTest.getEngineVersion());

        underTest.setMultiAZ("true");
        assertEquals("true", underTest.getMultiAZ());

        underTest.setStorageType("gp2");
        assertEquals("gp2", underTest.getStorageType());
    }

    @Test
    void testAsMap() {
        underTest.setBackupRetentionPeriod(3);
        underTest.setEngineVersion("1.2.3");
        underTest.setMultiAZ("true");
        underTest.setStorageType("gp2");

        Map<String, Object> map = underTest.asMap();

        assertEquals(3, ((Integer) map.get("backupRetentionPeriod")).intValue());
        assertEquals("1.2.3", map.get("engineVersion"));
        assertEquals("true", map.get("multiAZ"));
        assertEquals("gp2", map.get("storageType"));
    }

    @Test
    void testGetCloudPlatform() {
        assertEquals(CloudPlatform.AWS, underTest.getCloudPlatform());
    }

    @Test
    void testParse() {
        Map<String, Object> parameters = Map.of("backupRetentionPeriod", 3, "engineVersion", "1.2.3",
            "multiAZ", "true", "storageType", "gp2");

        underTest.parse(parameters);

        assertEquals(3, underTest.getBackupRetentionPeriod().intValue());
        assertEquals("1.2.3", underTest.getEngineVersion());
        assertEquals("true", underTest.getMultiAZ());
        assertEquals("gp2", underTest.getStorageType());
    }

}
