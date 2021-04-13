package com.sequenceiq.cloudbreak.cmtemplate.generator.template;


import static net.javacrumbs.jsonunit.JsonAssert.assertJsonEquals;
import static net.javacrumbs.jsonunit.JsonAssert.when;
import static net.javacrumbs.jsonunit.core.Option.IGNORING_ARRAY_ORDER;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Set;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.springframework.boot.test.context.SpringBootTestContextBootstrapper;
import org.springframework.test.context.BootstrapWith;
import org.springframework.test.context.TestContextManager;

import com.sequenceiq.cloudbreak.cmtemplate.generator.CentralTemplateGeneratorContext;
import com.sequenceiq.cloudbreak.cmtemplate.generator.template.domain.GeneratedCmTemplate;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;

@RunWith(Parameterized.class)
@BootstrapWith(SpringBootTestContextBootstrapper.class)
public class GeneratedClusterTemplateServiceTest extends CentralTemplateGeneratorContext {

    private static final String TEMPLATE_GENERATOR_TEST_OUTPUTS = "module-test/outputs";

    private static final String CDH = "CDH";

    private static final String CDH_6_1 = "6.1";

    private static final String UUID = "uuid";

    @Parameterized.Parameter
    public Set<String> inputs;

    @Parameterized.Parameter(1)
    public String stackType;

    @Parameterized.Parameter(2)
    public String version;

    @Parameterized.Parameter(3)
    public String outputPath;

    @Before
    public void setUp() throws Exception {
        TestContextManager testContextManager = new TestContextManager(getClass());
        testContextManager.prepareTestInstance(this);
    }

    @Test
    public void testTemplateGeneration() throws IOException, JSONException {
        TestFile outputFile = getTestFile(getFileName(TEMPLATE_GENERATOR_TEST_OUTPUTS, outputPath));

        GeneratedCmTemplate generatedCmTemplate = generatedClusterTemplateService().prepareClouderaManagerTemplate(inputs, stackType, version, UUID);
        JSONObject expected = toJSON(outputFile.getFileContent());
        JSONObject result = toJSON(generatedCmTemplate.getTemplate());

        assertJsonEquals(expected.toString(), result.toString(), when(IGNORING_ARRAY_ORDER));
    }

    @Parameterized.Parameters(name = "{index}: testTemplateGeneration(get {0} with {1} {2}) = output is {3}")
    public static Iterable<Object[]> data() {
        return Arrays.asList(new Object[][] {
                //{ Set.of("OOZIE"), CDH, CDH_6_1, "result_1" }
        });
    }

    static TestFile getTestFile(String fileName) throws IOException {
        return new TestFile(new File(fileName).toPath(), FileReaderUtils.readFileFromClasspath(fileName));
    }

    static String getFileName(String folder, String filename) {
        return folder + '/' + filename + ".json";
    }

    private JSONObject toJSON(String jsonText) throws JSONException {
        return null;
//        return (JSONObject) JSONParser.parseJSON(jsonText);
    }
}