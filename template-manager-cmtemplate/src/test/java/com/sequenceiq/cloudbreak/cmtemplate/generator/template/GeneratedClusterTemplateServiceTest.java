package com.sequenceiq.cloudbreak.cmtemplate.generator.template;


import static net.javacrumbs.jsonunit.JsonAssert.assertJsonEquals;
import static net.javacrumbs.jsonunit.JsonAssert.when;
import static net.javacrumbs.jsonunit.core.Option.IGNORING_ARRAY_ORDER;

import java.io.File;
import java.io.IOException;
import java.util.Set;
import java.util.stream.Stream;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.skyscreamer.jsonassert.JSONParser;
import org.springframework.boot.test.context.SpringBootTestContextBootstrapper;
import org.springframework.test.context.BootstrapWith;
import org.springframework.test.context.TestContextManager;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.sequenceiq.cloudbreak.cmtemplate.generator.CentralTemplateGeneratorContext;
import com.sequenceiq.cloudbreak.cmtemplate.generator.template.domain.GeneratedCmTemplate;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;

@Disabled
@ExtendWith(SpringExtension.class)
@BootstrapWith(SpringBootTestContextBootstrapper.class)
class GeneratedClusterTemplateServiceTest extends CentralTemplateGeneratorContext {

    private static final String TEMPLATE_GENERATOR_TEST_OUTPUTS = "module-test/outputs";

    private static final String CDH = "CDH";

    private static final String CDH_6_1 = "6.1";

    private static final String UUID = "uuid";

    @BeforeEach
    void setUp() throws Exception {
        TestContextManager testContextManager = new TestContextManager(getClass());
        testContextManager.prepareTestInstance(this);
    }

    @MethodSource("data")
    @ParameterizedTest
    void testTemplateGeneration(Set<String> inputs, String stackType, String version, String outputPath) throws IOException, JSONException {
        TestFile outputFile = getTestFile(getFileName(TEMPLATE_GENERATOR_TEST_OUTPUTS, outputPath));

        GeneratedCmTemplate generatedCmTemplate = generatedClusterTemplateService().prepareClouderaManagerTemplate(inputs, stackType, version, UUID);
        JSONObject expected = toJSON(outputFile.getFileContent());
        JSONObject result = toJSON(generatedCmTemplate.getTemplate());

        assertJsonEquals(expected.toString(), result.toString(), when(IGNORING_ARRAY_ORDER));
    }

    static Stream<Arguments> data() {
        return Stream.of(
                Arguments.of(Set.of("OOZIE"), CDH, CDH_6_1, "result_1")
        );
    }

    static TestFile getTestFile(String fileName) throws IOException {
        return new TestFile(new File(fileName).toPath(), FileReaderUtils.readFileFromClasspath(fileName));
    }

    static String getFileName(String folder, String filename) {
        return folder + '/' + filename + ".json";
    }

    private JSONObject toJSON(String jsonText) throws JSONException {
        return (JSONObject) JSONParser.parseJSON(jsonText);
    }
}