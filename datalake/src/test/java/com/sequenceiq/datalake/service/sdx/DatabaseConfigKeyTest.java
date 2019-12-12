package com.sequenceiq.datalake.service.sdx;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.datalake.service.sdx.database.DatabaseConfigKey;
import com.sequenceiq.sdx.api.model.SdxClusterShape;

public class DatabaseConfigKeyTest {

    private DatabaseConfigKey underTest;

    @BeforeEach
    public void setUp() throws Exception {
        underTest = new DatabaseConfigKey(CloudPlatform.AWS, SdxClusterShape.LIGHT_DUTY);
    }

    @Test
    public void testGetters() {
        assertEquals(CloudPlatform.AWS, underTest.getCloudPlatform());
        assertEquals(SdxClusterShape.LIGHT_DUTY, underTest.getSdxClusterShape());
    }

    @Test
    public void testEquals() {
        assertTrue(underTest.equals(underTest));

        DatabaseConfigKey underTest2 = new DatabaseConfigKey(CloudPlatform.AWS, SdxClusterShape.LIGHT_DUTY);
        assertTrue(underTest2.equals(underTest));
        assertTrue(underTest.equals(underTest2));

        underTest2 = new DatabaseConfigKey(CloudPlatform.AZURE, SdxClusterShape.LIGHT_DUTY);
        assertFalse(underTest.equals(underTest2));

        underTest2 = new DatabaseConfigKey(CloudPlatform.AWS, SdxClusterShape.CUSTOM);
        assertFalse(underTest.equals(underTest2));

        assertFalse(underTest.equals(null));
        assertFalse(underTest.equals(1234));
    }

}
