package com.sequenceiq.cloudbreak.recipe.moduletest;

import static com.sequenceiq.cloudbreak.recipe.moduletest.RecipeModulTestModelProvider.getFileName;
import static com.sequenceiq.cloudbreak.recipe.moduletest.RecipeModulTestModelProvider.getTestFile;
import static com.sequenceiq.cloudbreak.recipe.moduletest.RecipeModulTestModelProvider.testTemplatePreparationObject;
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTestContextBootstrapper;
import org.springframework.test.context.BootstrapWith;
import org.springframework.test.context.TestContextManager;

import com.sequenceiq.cloudbreak.recipe.testrepeater.TestFile;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;

@RunWith(Parameterized.class)
@BootstrapWith(SpringBootTestContextBootstrapper.class)
public class RecipeModulTest extends CentralRecipeContext {

    static final String RECIPE_UPDATER_TEST_INPUTS = "module-test/inputs";

    private static final String RECIPE_UPDATER_TEST_OUTPUTS = "module-test/outputs";

    private static final Logger LOGGER = LoggerFactory.getLogger(RecipeModulTest.class);

    @Parameter
    public String inputFileName;

    @Parameter(1)
    public String outputFileName;

    @Parameter(2)
    public TemplatePreparationObject testData;

    @Parameters(name = "{index}: module-test/inputs/{0}.recipe should equals module-test/outputs/{1}.recipe")
    public static Collection<Object[]> data() throws IOException {
        Collection<Object[]> params = new ArrayList<>();
        params.add(new Object[]{"install-test1", "install-test1", testTemplatePreparationObject()});
        params.add(new Object[]{"install-test2", "install-test2", testTemplatePreparationObject()});
        params.add(new Object[]{"install-test3", "install-test3", testTemplatePreparationObject()});
        return params;
    }

    @Before
    public void setUp() throws Exception {
        TestContextManager testContextManager = new TestContextManager(getClass());
        testContextManager.prepareTestInstance(this);
    }

    @Test
    public void testGetRecipeText() throws IOException {
        TestFile inputFile = getTestFile(getFileName(RECIPE_UPDATER_TEST_INPUTS, inputFileName));
        TestFile outputFile = getTestFile(getFileName(RECIPE_UPDATER_TEST_OUTPUTS, outputFileName));

        String inputRecipeText = getRecipeInputText(inputFile);

        String expected = outputFile.getFileContent();
        String resultRecipeText = getUnderTest().getRecipeText(testData, inputRecipeText);
        StringBuilder messageBuilder = new StringBuilder();
        messageBuilder.append("The result has not matched with the expected output ").append(outputFile.getFileName());
        messageBuilder.append("\nexpected:\n");
        messageBuilder.append(expected);
        messageBuilder.append("\nactual:\n");
        messageBuilder.append(resultRecipeText);
        LOGGER.info(messageBuilder.toString());

        assertEquals(expected, resultRecipeText);
    }

    private String getRecipeInputText(TestFile inputFile) {
        return inputFile.getFileContent();
    }

}