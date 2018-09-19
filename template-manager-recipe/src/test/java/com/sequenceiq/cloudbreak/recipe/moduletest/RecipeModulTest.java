package com.sequenceiq.cloudbreak.recipe.moduletest;

import static com.sequenceiq.cloudbreak.recipe.moduletest.RecipeModulTestModelProvider.getFileName;
import static com.sequenceiq.cloudbreak.recipe.moduletest.RecipeModulTestModelProvider.getTestFile;
import static com.sequenceiq.cloudbreak.recipe.moduletest.RecipeModulTestModelProvider.testTemplatePreparationObject;
import static com.sequenceiq.cloudbreak.recipe.moduletest.RecipeModulTestModelProvider.testTemplateWhenBlueprintVersionIs25;
import static com.sequenceiq.cloudbreak.recipe.moduletest.RecipeModulTestModelProvider.testTemplateWhenSharedServiceIsOnWithOnlyHiveRds;
import static com.sequenceiq.cloudbreak.recipe.moduletest.RecipeModulTestModelProvider.testTemplateWhenSharedServiceIsOnWithOnlyRangerRds;
import static com.sequenceiq.cloudbreak.recipe.moduletest.RecipeModulTestModelProvider.testTemplateWhenSharedServiceIsOnWithRangerAndHiveRds;
import static com.sequenceiq.cloudbreak.recipe.moduletest.RecipeModulTestModelProvider.testTemplateWithDruidRds;
import static com.sequenceiq.cloudbreak.recipe.moduletest.RecipeModulTestModelProvider.testTemplateWithInvalidLdapUrl;
import static com.sequenceiq.cloudbreak.recipe.moduletest.RecipeModulTestModelProvider.testTemplateWithLocalLdap;
import static com.sequenceiq.cloudbreak.recipe.moduletest.RecipeModulTestModelProvider.testTemplateWithLongLdapUrl;
import static com.sequenceiq.cloudbreak.recipe.moduletest.RecipeModulTestModelProvider.testTemplateWithNoSharedServiceAndRds;
import static com.sequenceiq.cloudbreak.recipe.moduletest.RecipeModulTestModelProvider.testTemplateWithSingleAbfsStorage;
import static com.sequenceiq.cloudbreak.recipe.moduletest.RecipeModulTestModelProvider.testTemplateWithSingleAdlsStorage;
import static com.sequenceiq.cloudbreak.recipe.moduletest.RecipeModulTestModelProvider.testTemplateWithSingleGcsStorage;
import static com.sequenceiq.cloudbreak.recipe.moduletest.RecipeModulTestModelProvider.testTemplateWithSingleS3Storage;
import static com.sequenceiq.cloudbreak.recipe.moduletest.RecipeModulTestModelProvider.testTemplateWithSingleWasbStorage;
import static com.sequenceiq.cloudbreak.recipe.moduletest.RecipeModulTestModelProvider.testTemplateWithTwoAbfsStorage;
import static com.sequenceiq.cloudbreak.recipe.moduletest.RecipeModulTestModelProvider.testTemplateWithTwoAdlsStorage;
import static com.sequenceiq.cloudbreak.recipe.moduletest.RecipeModulTestModelProvider.testTemplateWithTwoGcsStorage;
import static com.sequenceiq.cloudbreak.recipe.moduletest.RecipeModulTestModelProvider.testTemplateWithTwoS3Storage;
import static com.sequenceiq.cloudbreak.recipe.moduletest.RecipeModulTestModelProvider.testTemplateWithTwoWasbStorage;
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
    public static Collection<Object[]> data() {
        Collection<Object[]> params = new ArrayList<>();
        params.add(new Object[]{"install-test1", "install-test1", testTemplatePreparationObject()});
        params.add(new Object[]{"install-test2", "install-test2", testTemplatePreparationObject()});
        params.add(new Object[]{"install-test3", "install-test3", testTemplatePreparationObject()});
        params.add(new Object[]{"install-test4", "install-test4", testTemplateWithLocalLdap()});
        params.add(new Object[]{"install-test4", "urlLongTest", testTemplateWithLongLdapUrl()});
        params.add(new Object[]{"install-test4", "invalidUrlTest", testTemplateWithInvalidLdapUrl()});
        params.add(new Object[]{"SingleCloudStorageInsertion", "SingleS3CloudStorageInsertion", testTemplateWithSingleS3Storage()});
        params.add(new Object[]{"SingleCloudStorageInsertion", "SingleGcsCloudStorageInsertion", testTemplateWithSingleGcsStorage()});
        params.add(new Object[]{"SingleCloudStorageInsertion", "SingleAbfsCloudStorageInsertion", testTemplateWithSingleAbfsStorage()});
        params.add(new Object[]{"SingleCloudStorageInsertion", "SingleAdlsCloudStorageInsertion", testTemplateWithSingleAdlsStorage()});
        params.add(new Object[]{"SingleCloudStorageInsertion", "SingleWasbCloudStorageInsertion", testTemplateWithSingleWasbStorage()});
        params.add(new Object[]{"MultiCloudStorageInsertion", "MultiS3CloudStorageInsertion", testTemplateWithTwoS3Storage()});
        params.add(new Object[]{"MultiCloudStorageInsertion", "MultiGcsCloudStorageInsertion", testTemplateWithTwoGcsStorage()});
        params.add(new Object[]{"MultiCloudStorageInsertion", "MultiAbfsCloudStorageInsertion", testTemplateWithTwoAbfsStorage()});
        params.add(new Object[]{"MultiCloudStorageInsertion", "MultiAdlsCloudStorageInsertion", testTemplateWithTwoAdlsStorage()});
        params.add(new Object[]{"MultiCloudStorageInsertion", "MultiWasbCloudStorageInsertion", testTemplateWithTwoWasbStorage()});
        params.add(new Object[]{"druidPropertyValidator", "druidPropertyValidator", testTemplateWithDruidRds()});
        params.add(new Object[]{"sharedServiceCheckRds", "sharedServiceCheckRdsWithBothRdsExists", testTemplateWhenSharedServiceIsOnWithRangerAndHiveRds()});
        params.add(new Object[]{"sharedServiceCheckRds", "sharedServiceCheckRdsWhenNoSharedService", testTemplateWithNoSharedServiceAndRds()});
        params.add(new Object[]{"sharedServiceCheckRds", "sharedServiceCheckRdsWithOnlyHiveRdsExists", testTemplateWhenSharedServiceIsOnWithOnlyHiveRds()});
        params.add(new Object[]{"sharedServiceCheckRds", "sharedServiceCheckRdsWithOnlyRangerRdsExists", testTemplateWhenSharedServiceIsOnWithOnlyRangerRds()});
        params.add(new Object[]{"checkBlueprintVersionExpectiong25", "checkBlueprintVersionExpectiong25", testTemplateWhenBlueprintVersionIs25()});
        params.add(new Object[]{"checkBlueprintVersionExpectiongNot25", "checkBlueprintVersionExpectiongNot25", testTemplateWhenBlueprintVersionIs25()});
        return params;
    }

    @Before
    public void setUp() throws Exception {
        TestContextManager testContextManager = new TestContextManager(getClass());
        testContextManager.prepareTestInstance(this);
    }

    @Test
    public void testGetRecipeText() throws IOException {
        TestFile outputFile = getTestFile(getFileName(RECIPE_UPDATER_TEST_OUTPUTS, outputFileName));

        String inputRecipeText = getTestFile(getFileName(RECIPE_UPDATER_TEST_INPUTS, inputFileName)).getFileContent();

        String expected = outputFile.getFileContent();
        String resultRecipeText = getUnderTest().getRecipeText(testData, inputRecipeText);
        LOGGER.info(String.format("%s %s%nexpected:%n%s%n%nactual:%n%s", "Comparing expected and result recipe content. Expected content in file:",
                outputFile.getFileName(), expected, resultRecipeText));

        assertEquals(expected, resultRecipeText);
    }

}