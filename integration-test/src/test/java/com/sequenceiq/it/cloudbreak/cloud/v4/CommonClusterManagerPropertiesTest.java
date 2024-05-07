package com.sequenceiq.it.cloudbreak.cloud.v4;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CommonClusterManagerPropertiesTest {

    private CommonClusterManagerProperties underTest;

    @BeforeEach
    void setUp() {
        underTest = new CommonClusterManagerProperties();
        underTest.setDataEngDistroXBlueprintName("%s - Data Engineering: Apache Spark%s, Apache Hive, Apache Oozie");
    }

    @Test
    void getInternalDistroXBlueprintNameFor7217() {
        underTest.setRuntimeVersion("7.2.17");
        String result = underTest.getDataEngDistroXBlueprintNameForCurrentRuntime();
        assertEquals(result, "7.2.17 - Data Engineering: Apache Spark, Apache Hive, Apache Oozie");
    }

    @Test
    void getInternalDistroXBlueprintNameFor7218() {
        underTest.setRuntimeVersion("7.2.18");
        String result = underTest.getDataEngDistroXBlueprintNameForCurrentRuntime();
        assertEquals(result, "7.2.18 - Data Engineering: Apache Spark3, Apache Hive, Apache Oozie");
    }

}
