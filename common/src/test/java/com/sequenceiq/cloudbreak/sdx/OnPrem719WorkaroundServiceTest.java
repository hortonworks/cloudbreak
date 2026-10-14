package com.sequenceiq.cloudbreak.sdx;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class OnPrem719WorkaroundServiceTest {

    private final OnPrem719WorkaroundService underTest = new OnPrem719WorkaroundService();

    @ParameterizedTest
    @CsvSource({
            "7.1.9, 7.3.2",
            "7.3.1, 7.3.1",
            "7.3.2, 7.3.2",
            "7.1.9.1000, 7.1.9.1000"
    })
    void testReplaceRuntimeVersion(String runtimeVersion, String expected) {
        assertEquals(expected, underTest.replaceRuntimeVersion(runtimeVersion));
    }

    @Test
    void testReplaceRdcRuntimeVersion() {
        String rdc = "{\"clusterVersion\":\"CDH 7.1.9\",\"endPoints\":[{\"version\":\"CDH 7.1.9\"},{\"version\":\"CDH 7.3.1\"}]}";

        String result = underTest.replaceRdcRuntimeVersion(rdc);

        assertEquals("{\"clusterVersion\":\"CDH 7.3.2\",\"endPoints\":[{\"version\":\"CDH 7.3.2\"},{\"version\":\"CDH 7.3.1\"}]}", result);
    }
}
