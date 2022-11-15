package com.sequenceiq.cloudbreak.common.json;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class JsonTest {

    private Json underTest;

    @Test
    public void testValueWithNullString() {
        underTest = new Json(null);

        assertNull(underTest.getValue("azure.subscriptionId"));
        assertNull(underTest.getValue("azure"));
        assertNull(underTest.getValue("azure.appBased.authenticationType"));
        assertNull(underTest.getValue("azure.appBased.certificate.status"));
    }

    @Test
    public void testGetValueWithEmptyJson() {
        underTest = new Json("{}");

        assertNull(underTest.getValue("azure"));
        assertNull(underTest.getValue("azure.subscriptionId"));
        assertNull(underTest.getValue("azure.appBased.authenticationType"));
        assertNull(underTest.getValue("azure.appBased.certificate.status"));
    }

    @Test
    public void testGetValueWithNullAzure() {
        underTest = new Json("{\"aws\":null,\"yarn\":null,\"mock\":null,\"govCloud\":false}");

        assertNull(underTest.getValue("azure.subscriptionId"));
        assertNull(underTest.getValue("azure"));
        assertNull(underTest.getValue("azure.appBased.authenticationType"));
        assertNull(underTest.getValue("azure.appBased.certificate.status"));
    }

    @Test
    public void testGetValueWithNullCertificate() {
        underTest = new Json("{\"aws\":null,\"azure\":{\"subscriptionId\":\"sid\",\"tenantId\":\"tid\",\"appBased\":" +
                "{\"accessKey\":\"ak\",\"secretKey\":\"sec\"},\"codeGrantFlowBased\":null},\"gcp\":null,\"yarn\":null,\"mock\":null,\"govCloud\":false}");

        assertEquals("sid", underTest.getValue("azure.subscriptionId"));
        assertNotNull(underTest.getValue("azure"));
        assertNull(underTest.getValue("azure.appBased.authenticationType"));
        assertNull(underTest.getValue("azure.appBased.certificate.status"));
    }

    @Test
    public void testGetValueWithWithCertificate() {
        underTest = new Json("{\"aws\":null,\"azure\":{\"subscriptionId\":\"s\",\"tenantId\":\"t\",\"appBased\":" +
                "{\"certificate\":{\"status\":\"OK\"},\"authenticationType\":\"CERTIFICATE\"},\"codeGrantFlowBased\":null}," +
                "\"gcp\":null,\"yarn\":null,\"mock\":null,\"govCloud\":false}");

        assertEquals("s", underTest.getValue("azure.subscriptionId"));
        assertNotNull(underTest.getValue("azure"));
        assertEquals("CERTIFICATE", underTest.getValue("azure.appBased.authenticationType"));
        assertEquals("OK", underTest.getValue("azure.appBased.certificate.status"));
        assertNull(underTest.getValue("azure.appBased.certificate.does.not.exist"));
    }

    @Test
    public void testGetNullMap() {
        underTest = new Json(null);

        assertTrue(underTest.getMap().isEmpty());
    }
}
