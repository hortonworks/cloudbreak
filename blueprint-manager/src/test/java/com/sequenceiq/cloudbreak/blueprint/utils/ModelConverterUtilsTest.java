package com.sequenceiq.cloudbreak.blueprint.utils;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;

import net.sf.json.JSONObject;

@RunWith(Parameterized.class)
public class ModelConverterUtilsTest {

    public static final String MAP_CONVERTER_INPUTS = "model-converter-test/inputs/";

    public static final String MAP_CONVERTER_OUTPUTS = "model-converter-test/outputs/";

    private String fileName;

    public ModelConverterUtilsTest(String fileName) {
        this.fileName = fileName;
    }

    @Parameterized.Parameters(name = "{index}: modelConverterUtilsTest with file: {0}")
    public static Iterable<Object[]> data() throws JsonProcessingException {
        return Arrays.asList(new Object[][]{
                {"test_1.json"},
                {"test_2.json"},
        });
    }

    @Test
    public void test() throws IOException {
        JSONObject input = getJson(MAP_CONVERTER_INPUTS, fileName);
        JSONObject output = getJson(MAP_CONVERTER_OUTPUTS, fileName);

        Map<String, Object> result = ModelConverterUtils.convert(input);

        Assert.assertEquals(output, JSONObject.fromObject(result));
    }

    private JSONObject getJson(String folder, String fileName) throws IOException {
        String content = FileReaderUtils.readFileFromClasspath(folder + fileName);
        return JSONObject.fromObject(content);
    }
}