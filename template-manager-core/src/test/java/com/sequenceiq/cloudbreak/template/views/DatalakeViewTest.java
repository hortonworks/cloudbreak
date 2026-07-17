package com.sequenceiq.cloudbreak.template.views;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.sequenceiq.cloudbreak.constant.GcpConstants;
import com.sequenceiq.cloudbreak.sdx.TargetPlatform;

public class DatalakeViewTest {

    static Object[][] datalakeViewDataProvider() {
        return new Object[][]{
                // razEnabled, raz auth type, crn, externalDbRequested
                {true, GcpConstants.RAZ_AUTHENTICATION_TYPE_CAB, Map.of(), "crn:cdp:datalake:us-west-1:1234:datalake:1", true},
                {true, GcpConstants.RAZ_AUTHENTICATION_TYPE_HMAC, Map.of(), "crn:cdp:datalake:us-west-1:1234:datalake:1", false},
                {false, GcpConstants.RAZ_AUTHENTICATION_TYPE_CAB, Map.of(), "crn:cdp:datalake:us-west-1:1234:datalake:1", true},
                {false, GcpConstants.RAZ_AUTHENTICATION_TYPE_HMAC, Map.of(), "crn:cdp:datalake:us-west-1:1234:datalake:1", false}
        };
    }

    @ParameterizedTest()
    @MethodSource("datalakeViewDataProvider")
    void getDatalakeViewTest(boolean razEnabled, String razAuthenticationType, Map<String, String> userMappings, String crn, boolean externalDbRequested) {
        DatalakeView datalakeView = new DatalakeView(razEnabled, razAuthenticationType, userMappings, crn, externalDbRequested);
        DatabaseType expectedDb = externalDbRequested ? DatabaseType.EXTERNAL_DATABASE : DatabaseType.EMBEDDED_DATABASE;
        assertEquals(expectedDb, datalakeView.getDatabaseType());
        assertEquals(TargetPlatform.PAAS, datalakeView.getTargetPlatform());
        assertEquals(razEnabled, datalakeView.isRazEnabled());
    }
}
