package com.sequenceiq.cloudbreak.template.views;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public class DatalakeViewTest {

    static Object[][] datalakeViewDataProvider() {
        return new Object[][]{
                // razEnabled, crn, externalDbRequested
                {true, "crn", true},
                {true, "crn", false},
                {false, "crn", true},
                {false, "crn", false}
        };
    }

    @ParameterizedTest()
    @MethodSource("datalakeViewDataProvider")
    void getDatalakeViewTest(boolean razEnabled, String crn, boolean externalDbRequested) {
        DatalakeView datalakeView = new DatalakeView(razEnabled, crn, externalDbRequested);
        DatabaseType expectedDb = externalDbRequested ? DatabaseType.EXTERNAL_DATABASE : DatabaseType.EMBEDDED_DATABASE;
        assertEquals(expectedDb, datalakeView.getDatabaseType());
        assertEquals(crn, datalakeView.getCrn());
        assertEquals(razEnabled, datalakeView.isRazEnabled());
    }
}
