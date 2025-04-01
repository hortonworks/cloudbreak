package com.sequenceiq.cloudbreak.template.views;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.sequenceiq.cloudbreak.sdx.TargetPlatform;

public class DatalakeViewTest {

    static Object[][] datalakeViewDataProvider() {
        return new Object[][]{
                // razEnabled, crn, externalDbRequested
                {true, "crn:cdp:datalake:us-west-1:1234:datalake:1", true},
                {true, "crn:cdp:datalake:us-west-1:1234:datalake:1", false},
                {false, "crn:cdp:datalake:us-west-1:1234:datalake:1", true},
                {false, "crn:cdp:datalake:us-west-1:1234:datalake:1", false}
        };
    }

    @ParameterizedTest()
    @MethodSource("datalakeViewDataProvider")
    void getDatalakeViewTest(boolean razEnabled, String crn, boolean externalDbRequested) {
        DatalakeView datalakeView = new DatalakeView(razEnabled, crn, externalDbRequested);
        DatabaseType expectedDb = externalDbRequested ? DatabaseType.EXTERNAL_DATABASE : DatabaseType.EMBEDDED_DATABASE;
        assertEquals(expectedDb, datalakeView.getDatabaseType());
        assertEquals(TargetPlatform.PAAS, datalakeView.getTargetPlatform());
        assertEquals(razEnabled, datalakeView.isRazEnabled());
    }
}
