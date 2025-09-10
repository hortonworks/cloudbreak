package com.sequenceiq.cloudbreak.template.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;

class ModelConverterUtilsTest {

    private static final String MAP_CONVERTER_INPUTS = "model-converter-test/inputs/";

    private static final String MAP_CONVERTER_OUTPUTS = "model-converter-test/outputs/";

    static Stream<Arguments> testConvertArguments() {
        return Stream.of(
                Arguments.of("test_1.json"),
                Arguments.of("test_2.json"),
                Arguments.of("test_3.json")
        );
    }

    @MethodSource("testConvertArguments")
    @ParameterizedTest
    void testConvert(String fileName) throws IOException {
        ObjectNode input = getJson(MAP_CONVERTER_INPUTS, fileName);
        ObjectNode output = getJson(MAP_CONVERTER_OUTPUTS, fileName);

        Map<String, Object> result = ModelConverterUtils.convert(JsonUtil.readValue(input, Map.class));

        assertEquals(output, JsonUtil.createJsonTree(result));
    }

    @Test
    void testConvertNull() {
        Map<String, Object> result = ModelConverterUtils.convert(null);

        assertEquals(Map.of(), result);
    }

    private ObjectNode getJson(String folder, String fileName) throws IOException {
        String content = FileReaderUtils.readFileFromClasspath(folder + fileName);
        return (ObjectNode) JsonUtil.readTree(content);
    }
}