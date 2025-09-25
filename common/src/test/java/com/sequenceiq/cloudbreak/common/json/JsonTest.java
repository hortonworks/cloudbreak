package com.sequenceiq.cloudbreak.common.json;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import com.sequenceiq.common.api.cloudstorage.AwsEfsParameters;
import com.sequenceiq.common.api.cloudstorage.AwsStorageParameters;
import com.sequenceiq.common.api.cloudstorage.S3Guard;
import com.sequenceiq.common.api.type.FeatureSetting;

class JsonTest {

    @Test
    void testgetStringWithNullString() {
        Json underTest = new Json(null);

        assertNull(underTest.getString("azure.subscriptionId"));
        assertNull(underTest.getString("azure"));
        assertNull(underTest.getString("azure.appBased.authenticationType"));
        assertNull(underTest.getString("azure.appBased.certificate.status"));
    }

    @Test
    void testGetStringWithEmptyJson() {
        Json underTest = new Json("{}");

        assertNull(underTest.getString("azure"));
        assertNull(underTest.getString("azure.subscriptionId"));
        assertNull(underTest.getString("azure.appBased.authenticationType"));
        assertNull(underTest.getString("azure.appBased.certificate.status"));
    }

    @Test
    void testGetStringWithNullAzure() {
        Json underTest = new Json("{\"aws\":null,\"yarn\":null,\"mock\":null,\"govCloud\":false}");

        assertNull(underTest.getString("azure.subscriptionId"));
        assertNull(underTest.getString("azure"));
        assertNull(underTest.getString("azure.appBased.authenticationType"));
        assertNull(underTest.getString("azure.appBased.certificate.status"));
    }

    @Test
    void testGetStringWithNullCertificate() {
        Json underTest = new Json("{\"aws\":null,\"azure\":{\"subscriptionId\":\"sid\",\"tenantId\":\"tid\",\"appBased\":" +
                "{\"accessKey\":\"ak\",\"secretKey\":\"sec\"},\"codeGrantFlowBased\":null},\"gcp\":null,\"yarn\":null,\"mock\":null,\"govCloud\":false}");

        assertEquals("sid", underTest.getString("azure.subscriptionId"));
        assertNotNull(underTest.getString("azure"));
        assertNull(underTest.getString("azure.appBased.authenticationType"));
        assertNull(underTest.getString("azure.appBased.certificate.status"));
    }

    @Test
    void testGetStringWithWithCertificate() {
        Json underTest = new Json("{\"aws\":null,\"azure\":{\"subscriptionId\":\"s\",\"tenantId\":\"t\",\"appBased\":" +
                "{\"certificate\":{\"status\":\"OK\"},\"authenticationType\":\"CERTIFICATE\"},\"codeGrantFlowBased\":null}," +
                "\"gcp\":null,\"yarn\":null,\"mock\":null,\"govCloud\":false}");

        assertEquals("s", underTest.getString("azure.subscriptionId"));
        assertNotNull(underTest.getString("azure"));
        assertEquals("CERTIFICATE", underTest.getString("azure.appBased.authenticationType"));
        assertEquals("OK", underTest.getString("azure.appBased.certificate.status"));
        assertNull(underTest.getString("azure.appBased.certificate.does.not.exist"));
    }

    @Test
    void testGetNullMap() {
        Json underTest = new Json(null);

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
                Arguments.of(new Json("{\"key\":\"value\"}"), new Json("[\"key\", \"value\"]"), false),
                Arguments.of(new Json("invalid json"), new Json("invalid json"), true),
                Arguments.of(new Json("invalid json"), new Json("{\"key\":\"value\"}"), false),
                Arguments.of(new Json("{\"key\":\"value\"}"), new Json("invalid json"), false),
                Arguments.of(new Json(null), null, false),
                Arguments.of(new Json(null), new Json(null), true),
                Arguments.of(new Json(null), new Json(""), false)
        );
    }

    @ParameterizedTest
    @MethodSource("provideJsonForEqualityTest")
    void testEquals(Json json1, Object json2, boolean expected) {
        assertEquals(expected, json1.equals(json2));
    }

    @Test
    void testEqualsWithSameInstance() {
        Json json = new Json("{\"key\":\"value\"}");
        assertTrue(json.equals(json));
    }

    static Stream<Arguments> testConstructorFromObjectArguments() {
        FeatureSetting featureSetting = new FeatureSetting();
        featureSetting.setEnabled(true);
        return Stream.of(
                Arguments.of(Map.of("key1", "value1", "key2", 2), "{\"key1\":\"value1\",\"key2\":2}"),
                Arguments.of(Map.of("nested", Map.of("innerKey", "innerValue")), "{\"nested\":{\"innerKey\":\"innerValue\"}}"),
                Arguments.of(Map.of("list", java.util.List.of(1, 2, 3)), "{\"list\":[1,2,3]}"),
                Arguments.of(featureSetting, "{\"enabled\":true}")
        );
    }

    @MethodSource("testConstructorFromObjectArguments")
    @ParameterizedTest
    void testConstructorFromObject(Object input, String expected) {
        Json json = new Json(input);

        assertEquals(new Json(expected), json);
    }

    @ValueSource(booleans = {true, false})
    @ParameterizedTest
    void testConstructorFromInvalidObject(boolean silentConstructor) {
        Object objectWithThrowingGetter = new Object() {
            public String getValue() {
                throw new RuntimeException("Getter exception");
            }
        };
        if (silentConstructor) {
            assertDoesNotThrow(() -> Json.silent(objectWithThrowingGetter));
        } else {
            assertThrows(IllegalArgumentException.class, () -> new Json(objectWithThrowingGetter));
        }
    }

    @ValueSource(booleans = {true, false})
    @ParameterizedTest
    void testGet(boolean shouldThrow) {
        if (shouldThrow) {
            Json underTest = new Json("{\"key\":\"value\"}");
            assertThrows(IOException.class, () -> underTest.get(List.class));
        } else {
            Json underTest = new Json("{\"key\":\"value\"}");
            Map<String, String> result = assertDoesNotThrow(() -> underTest.get(Map.class));
            assertThat(result).containsExactlyEntriesOf(Map.of("key", "value"));
        }
    }

    @ValueSource(booleans = {true, false})
    @ParameterizedTest
    void testGetUnchecked(boolean shouldThrow) {
        if (shouldThrow) {
            Json underTest = new Json("{\"key\":\"value\"}");
            assertThrows(IllegalStateException.class, () -> underTest.getUnchecked(List.class));
        } else {
            Json underTest = new Json("{\"key\":\"value\"}");
            Map<String, String> result = assertDoesNotThrow(() -> underTest.getUnchecked(Map.class));
            assertThat(result).containsExactlyEntriesOf(Map.of("key", "value"));
        }
    }

    @Test
    void testGenericGetByPath() {
        Json underTest = new Json("{\"aws\":{\"s3Guard\":{}," +
                "\"efsParameters\":{\"fileSystemTags\":{\"key1\":\"value1\",\"key2\":\"value2\"}}}}");

        assertInstanceOf(AwsStorageParameters.class, underTest.get("aws", AwsStorageParameters.class));
        assertInstanceOf(S3Guard.class, underTest.get("aws.s3Guard", S3Guard.class));
        assertNull(underTest.get("aws.s3Guard.dynamoDbTableName", String.class));
        assertInstanceOf(AwsEfsParameters.class, underTest.get("aws.efsParameters", AwsEfsParameters.class));
        assertInstanceOf(Map.class, underTest.get("aws.efsParameters.fileSystemTags", Map.class));
        assertThat(underTest.get("aws.efsParameters.fileSystemTags", Map.class))
                .containsExactlyInAnyOrderEntriesOf(Map.of("key1", "value1", "key2", "value2"));
        assertNull(underTest.get("aws.efsParameters.non.existent.field", String.class));
    }

    @Test
    void testGetIntByPath() {
        Json underTest = new Json("{\"level1\":{\"level2\":{\"intValue\":42,\"doubleValue\":42.2}}}");

        assertEquals(42, underTest.getInt("level1.level2.intValue"));
        assertEquals(42, underTest.getInt("level1.level2.doubleValue"));
        assertNull(underTest.getInt("level1.level2.nonExistentIntValue"));
        assertNull(new Json(null).getInt("level1.level2.intValue"));
    }

    @Test
    void testGetDoubleByPath() {
        Json underTest = new Json("{\"level1\":{\"level2\":{\"intValue\":42,\"doubleValue\":42.2}}}");

        assertEquals(42.2, underTest.getDouble("level1.level2.doubleValue"));
        assertEquals(42.0, underTest.getDouble("level1.level2.intValue"));
        assertNull(underTest.getDouble("level1.level2.nonExistentDoubleValue"));
        assertNull(new Json(null).getDouble("level1.level2.doubleValue"));
    }

    @Test
    void testGetBooleanByPath() {
        Json underTest = new Json("{\"level1\":{\"level2\":{\"trueValue\":true,\"falseValue\":false}}}");

        assertEquals(true, underTest.getBoolean("level1.level2.trueValue"));
        assertEquals(false, underTest.getBoolean("level1.level2.falseValue"));
        assertNull(underTest.getBoolean("level1.level2.nonExistentBooleanValue"));
        assertNull(new Json(null).getBoolean("level1.level2.trueValue"));
    }

    @Test
    void testGetJsonNodeByPath() {
        Json underTest = new Json("{\"level1\":{\"level2\":{\"key\":\"value\"}}}");

        assertNotNull(underTest.getJsonNode("level1"));
        assertNotNull(underTest.getJsonNode("level1.level2"));
        assertNotNull(underTest.getJsonNode("level1.level2.key"));
        assertEquals(underTest.getJsonNode("level1.level2.key").asText(), "value");
        assertNull(underTest.getJsonNode("level1.level2.nonExistentKey"));
        assertNull(new Json(null).getJsonNode("level1.level2.key"));
    }

    @Test
    void testGetMap() {
        Json underTest = new Json("{\"key1\":\"value1\",\"key2\":2,\"nested\":{\"innerKey\":\"innerValue\",\"innerList\":[1,2,3]}}");
        Map<String, Object> result = underTest.getMap();
        assertThat(result).containsEntry("key1", "value1");
        assertThat(result).containsEntry("key2", 2);
        assertThat(result).containsEntry("nested", Map.of("innerKey", "innerValue", "innerList", List.of(1, 2, 3)));

        Json nullJson = new Json(null);
        Map<String, Object> nullResult = nullJson.getMap();
        assertNotNull(nullResult);
        assertThat(nullResult).isEmpty();

        Json nonMapJson = new Json("[\"value1\",\"value2\"]");
        Map<String, Object> nonMapResult = nonMapJson.getMap();
        assertNotNull(nonMapResult);
        assertThat(nonMapResult).isEmpty();
    }

    @Test
    void testFlatPaths() {
        Json underTest = new Json("{\"key1\":\"value1\",\"key2\":2,\"emptyNested\":{}," +
                "\"nested\":{\"innerKey\":\"innerValue\",\"innerList\":[1,2,3],\"deeplyNested\":{\"deeplyNestedKey\":\"deeplyNestedValue\"}}," +
                "\"nullKey\":null}");
        assertThat(underTest.flatPaths())
                .containsExactlyInAnyOrder("key1", "key2", "nested.innerKey", "nested.innerList", "nested.deeplyNested.deeplyNestedKey");

        Json nullJson = new Json(null);
        assertThat(nullJson.flatPaths()).isEmpty();

        Json nonMapJson = new Json("[\"value1\",\"value2\"]");
        assertThat(nonMapJson.flatPaths()).isEmpty();
    }

    static Stream<Arguments> testRemoveArguments() {
        String startingJsonString = "{\"key1\":\"value1\",\"key2\":2,\"emptyNested\":{},\"nested\":{\"innerKey\":\"innerValue\",\"innerList\":[1,2,3]," +
                "\"deeplyNested\":{\"deeplyNestedKey\":\"deeplyNestedValue\"}},\"nullKey\":null}";

        return Stream.of(
                Arguments.of(new Json(startingJsonString), List.of("key1", "nested.deeplyNested"),
                        new Json("{\"key2\":2,\"emptyNested\":{},\"nested\":{\"innerKey\":\"innerValue\",\"innerList\":[1,2,3]},\"nullKey\":null}")),
                Arguments.of(new Json(startingJsonString), List.of("key2", "nested.innerList", "nullKey"),
                        new Json("{\"key1\":\"value1\",\"emptyNested\":{},\"nested\":{\"innerKey\":\"innerValue\"," +
                                "\"deeplyNested\":{\"deeplyNestedKey\":\"deeplyNestedValue\"}}}")),
                Arguments.of(new Json(startingJsonString), List.of("emptyNested", "nested.innerKey", "nested.nonExistentKey", "nonExistentKey", "nullKey"),
                        new Json("{\"key1\":\"value1\",\"key2\":2,\"nested\":{\"innerList\":[1,2,3]," +
                                "\"deeplyNested\":{\"deeplyNestedKey\":\"deeplyNestedValue\"}}}")),
                Arguments.of(new Json(startingJsonString), Arrays.asList(""), new Json(startingJsonString))
        );
    }

    @MethodSource("testRemoveArguments")
    @ParameterizedTest
    void testRemove(Json underTest, List<String> pathsToRemove, Json expected) {
        for (String path : pathsToRemove) {
            underTest.remove(path);
        }
        assertEquals(expected, underTest);
    }

    @Test
    void testRemoveFromInvalidJson() {
        assertThrows(CloudbreakJsonProcessingException.class, () -> new Json("invalid json").remove("anything"));
    }

    static Stream<Arguments> testReplaceValueArguments() {
        String startingJsonString = "{\"key1\":\"value1\",\"key2\":2,\"emptyNested\":{},\"nested\":{\"innerKey\":\"innerValue\",\"innerList\":[1,2,3]," +
                "\"deeplyNested\":{\"deeplyNestedKey\":\"deeplyNestedValue\"}},\"nullKey\":null}";

        return Stream.of(
                Arguments.of(new Json(startingJsonString), Map.of("key1", "newValue1", "key2", 42,
                                "nested.deeplyNested.deeplyNestedKey", "newDeeplyNestedValue"),
                        new Json("{\"key1\":\"newValue1\",\"key2\":42,\"emptyNested\":{},\"nested\":{\"innerKey\":\"innerValue\",\"innerList\":[1,2,3]," +
                                "\"deeplyNested\":{\"deeplyNestedKey\":\"newDeeplyNestedValue\"}},\"nullKey\":null}")),
                Arguments.of(new Json(startingJsonString), Map.of("emptyNested", Map.of("newKey", "newValue"), "nested.deeplyNested", "justAString",
                                "nested.innerList", List.of(4, 5, 6), "nullKey", "nowNotNull"),
                        new Json("{\"key1\":\"value1\",\"key2\":2,\"emptyNested\":{\"newKey\":\"newValue\"}," +
                                "\"nested\":{\"innerKey\":\"innerValue\",\"innerList\":[4,5,6],\"deeplyNested\":\"justAString\"},\"nullKey\":\"nowNotNull\"}")),
                Arguments.of(new Json(startingJsonString), Map.of("nonExistentKey", "someValue", "nested.nonExistentKey", 123,
                        "nested.deeplyNested.nonExistentKey", true), new Json(startingJsonString))
        );
    }

    @MethodSource("testReplaceValueArguments")
    @ParameterizedTest
    void testReplaceValue(Json underTest, Map<String, Object> replacements, Json expected) {
        for (Map.Entry<String, Object> entry : replacements.entrySet()) {
            underTest.replaceValue(entry.getKey(), entry.getValue());
        }
        assertEquals(expected, underTest);
    }

    @Test
    void testReplaceValueInInvalidJson() {
        assertThrows(CloudbreakJsonProcessingException.class, () -> new Json("invalid json").replaceValue("anything", "anyValue"));
    }

    static Stream<Arguments> testIsObjectArguments() {
        return Stream.of(
                Arguments.of(new Json("{\"key\":\"value\"}"), true),
                Arguments.of(new Json("null"), false),
                Arguments.of(new Json("42"), false),
                Arguments.of(new Json("\"a string\""), false),
                Arguments.of(new Json("[1, 2, 3]"), false),
                Arguments.of(new Json("invalid json"), false)
        );
    }

    @MethodSource("testIsObjectArguments")
    @ParameterizedTest
    void testIsObject(Json underTest, boolean expected) {
        assertEquals(expected, underTest.isObject());
    }

    static Stream<Arguments> testIsArrayArguments() {
        return Stream.of(
                Arguments.of(new Json("[1, 2, 3]"), true),
                Arguments.of(new Json("null"), false),
                Arguments.of(new Json("42"), false),
                Arguments.of(new Json("\"a string\""), false),
                Arguments.of(new Json("{\"key\":\"value\"}"), false),
                Arguments.of(new Json("invalid json"), false)
        );
    }

    @MethodSource("testIsArrayArguments")
    @ParameterizedTest
    void testIsArray(Json underTest, boolean expected) {
        assertEquals(expected, underTest.isArray());
    }
}
