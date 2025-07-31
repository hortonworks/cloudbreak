package com.sequenceiq.cloudbreak.cm.converter;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.cloudera.api.swagger.model.ApiClusterTemplate;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;

class ApiClusterTemplateToCmTemplateConverterTest {

    private final ApiClusterTemplateToCmTemplateConverter underTest = new ApiClusterTemplateToCmTemplateConverter();

    private static Stream<Arguments> testConversion() {
        return Stream.of(
                Arguments.of("hbaseandzookeeper"),
                Arguments.of("datalake")
        );
    }

    @ParameterizedTest
    @MethodSource("testConversion")
    public void testConversion(String scenario) throws IOException {
        assertEquals(
                getExpectedApiClusterTemplateFromExpectedJson(scenario),
                underTest.convert(getApiClusterTemplateFromInputJson(scenario)),
                "The converted template does not match the expected output."
        );
    }

    private String getExpectedApiClusterTemplateFromExpectedJson(String scenario) throws IOException {
        return JsonUtil.readValueIntoOneLine(getExpectedFileContent(scenario), ApiClusterTemplate.class);
    }

    private String getExpectedFileContent(String scenario) throws IOException {
        return FileReaderUtils.readFileFromClasspath(String.format("deployment/expected/%s.json", scenario));
    }

    private ApiClusterTemplate getApiClusterTemplateFromInputJson(String scenario) throws IOException {
        return JsonUtil.readValue(
                getInputFileContent(scenario),
                ApiClusterTemplate.class
        );
    }

    private String getInputFileContent(String scenario) throws IOException {
        return FileReaderUtils.readFileFromClasspath(String.format("deployment/input/%s.json", scenario));
    }

}