package com.sequenceiq.cloudbreak.common.json;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

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

    static Stream<Arguments> provideJsonForEqualityTest() {
        return Stream.of(
                Arguments.of(new Json("{\"key\":\"value\"}"), new Json("{\"key\":\"value\"}"), true),
                Arguments.of(new Json("{\"key\":\"value\"}"), new Json("{\"key\":\"differentValue\"}"), false),
                Arguments.of(new Json("{\"availabilitySet\":{\"faultDomainCount\":2,\"name\":\"mmolnar-azurenv-freeipa-master-as-std\"}}"),
                        new Json("{\"availabilitySet\":{\"name\":\"mmolnar-azurenv-freeipa-master-as\",\"faultDomainCount\":2}}"), false),
                Arguments.of(new Json("{\"availabilitySet\":{\"faultDomainCount\":2,\"name\":\"mmolnar-azurenv-freeipa-master-as\"}}"),
                        new Json("{\"availabilitySet\":{\"name\":\"mmolnar-azurenv-freeipa-master-as\",\"faultDomainCount\":2}}"), true),
                Arguments.of(new Json("{\"availabilitySet\":{\"faultDomainCount\":2,\"name\":\"mmolnar-azurenv-freeipa-master-as\"}}"),
                        new Json("{\"availabilitySet\":{\"name\":\"mmolnar-azurenv-freeipa-master-as-std\",\"faultDomainCount\":2}}"), false),
                Arguments.of(new Json("{\"key\":\"value\"}"), null, false),
                Arguments.of(new Json("{\"key\":\"value\"}"), "SomeString", false),
                Arguments.of(new Json("[\"value1\",\"value2\"]"), new Json("[\"value1\",\"value2\"]"), true),
                Arguments.of(new Json("[\"value1\",\"value2\"]"), new Json("[\"value2\",\"value1\"]"), false),
                Arguments.of(new Json("[\"value2\",\"value1\"]"), new Json("[\"value2\",\"value1\",\"value3\"]"), false),
                Arguments.of(new Json("{\"key\":\"value\"}"), new Json("[\"key\", \"value\"]"), false)
        );
    }

    @ParameterizedTest
    @MethodSource("provideJsonForEqualityTest")
    public void testEquals(Json json1, Object json2, boolean expected) {
        assertEquals(expected, json1.equals(json2));
    }

    @Test
    public void testEqualsWithSameInstance() {
        Json json = new Json("{\"key\":\"value\"}");
        assertTrue(json.equals(json));
    }
}
