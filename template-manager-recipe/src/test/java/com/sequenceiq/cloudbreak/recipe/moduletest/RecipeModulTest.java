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
import static com.sequenceiq.cloudbreak.recipe.moduletest.RecipeModulTestModelProvider.testTemplateWithSingleAdlsGen2Storage;
import static com.sequenceiq.cloudbreak.recipe.moduletest.RecipeModulTestModelProvider.testTemplateWithSingleAdlsStorage;
import static com.sequenceiq.cloudbreak.recipe.moduletest.RecipeModulTestModelProvider.testTemplateWithSingleGcsStorage;
import static com.sequenceiq.cloudbreak.recipe.moduletest.RecipeModulTestModelProvider.testTemplateWithSingleS3Storage;
import static com.sequenceiq.cloudbreak.recipe.moduletest.RecipeModulTestModelProvider.testTemplateWithSingleWasbStorage;
import static com.sequenceiq.cloudbreak.recipe.moduletest.RecipeModulTestModelProvider.testTemplateWithTwoAdlsGen2Storage;
import static com.sequenceiq.cloudbreak.recipe.moduletest.RecipeModulTestModelProvider.testTemplateWithTwoAdlsStorage;
import static com.sequenceiq.cloudbreak.recipe.moduletest.RecipeModulTestModelProvider.testTemplateWithTwoGcsStorage;
import static com.sequenceiq.cloudbreak.recipe.moduletest.RecipeModulTestModelProvider.testTemplateWithTwoS3Storage;
import static com.sequenceiq.cloudbreak.recipe.moduletest.RecipeModulTestModelProvider.testTemplateWithTwoWasbStorage;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTestContextBootstrapper;
import org.springframework.test.context.BootstrapWith;
import org.springframework.test.context.TestContextManager;

import com.sequenceiq.cloudbreak.recipe.testrepeater.TestFile;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;

@BootstrapWith(SpringBootTestContextBootstrapper.class)
class RecipeModulTest extends CentralRecipeContext {

    static final String RECIPE_UPDATER_TEST_INPUTS = "module-test/inputs";

    private static final String RECIPE_UPDATER_TEST_OUTPUTS = "module-test/outputs";

    private static final Logger LOGGER = LoggerFactory.getLogger(RecipeModulTest.class);

    static Stream<Arguments> data() {
        return Stream.of(
                Arguments.of("install-test1", "install-test1", testTemplatePreparationObject()),
                Arguments.of("install-test2", "install-test2", testTemplatePreparationObject()),
                Arguments.of("install-test3", "install-test3", testTemplatePreparationObject()),
                Arguments.of("install-test4", "install-test4", testTemplateWithLocalLdap()),
                Arguments.of("install-test4", "urlLongTest", testTemplateWithLongLdapUrl()),
                Arguments.of("install-test4", "invalidUrlTest", testTemplateWithInvalidLdapUrl()),
                Arguments.of("SingleCloudStorageInsertion", "SingleS3CloudStorageInsertion", testTemplateWithSingleS3Storage()),
                Arguments.of("SingleCloudStorageInsertion", "SingleGcsCloudStorageInsertion", testTemplateWithSingleGcsStorage()),
                Arguments.of("SingleCloudStorageInsertion", "SingleAbfsCloudStorageInsertion", testTemplateWithSingleAdlsGen2Storage()),
                Arguments.of("SingleCloudStorageInsertion", "SingleAdlsCloudStorageInsertion", testTemplateWithSingleAdlsStorage()),
                Arguments.of("SingleCloudStorageInsertion", "SingleWasbCloudStorageInsertion", testTemplateWithSingleWasbStorage()),
                Arguments.of("MultiCloudStorageInsertion", "MultiS3CloudStorageInsertion", testTemplateWithTwoS3Storage()),
                Arguments.of("MultiCloudStorageInsertion", "MultiGcsCloudStorageInsertion", testTemplateWithTwoGcsStorage()),
                Arguments.of("MultiCloudStorageInsertion", "MultiAbfsCloudStorageInsertion", testTemplateWithTwoAdlsGen2Storage()),
                Arguments.of("MultiCloudStorageInsertion", "MultiAdlsCloudStorageInsertion", testTemplateWithTwoAdlsStorage()),
                Arguments.of("MultiCloudStorageInsertion", "MultiWasbCloudStorageInsertion", testTemplateWithTwoWasbStorage()),
                Arguments.of("druidPropertyValidator", "druidPropertyValidator", testTemplateWithDruidRds()),
                Arguments.of("sharedServiceCheckRds", "sharedServiceCheckRdsWithBothRdsExists", testTemplateWhenSharedServiceIsOnWithRangerAndHiveRds()),
                Arguments.of("sharedServiceCheckRds", "sharedServiceCheckRdsWhenNoSharedService", testTemplateWithNoSharedServiceAndRds()),
                Arguments.of("sharedServiceCheckRds", "sharedServiceCheckRdsWithOnlyHiveRdsExists", testTemplateWhenSharedServiceIsOnWithOnlyHiveRds()),
                Arguments.of("sharedServiceCheckRds", "sharedServiceCheckRdsWithOnlyRangerRdsExists", testTemplateWhenSharedServiceIsOnWithOnlyRangerRds()),
                Arguments.of("checkBlueprintVersionExpectiong25", "checkBlueprintVersionExpectiong25", testTemplateWhenBlueprintVersionIs25()),
                Arguments.of("checkBlueprintVersionExpectiongNot25", "checkBlueprintVersionExpectiongNot25", testTemplateWhenBlueprintVersionIs25())
        );
    }

    @BeforeEach
    void setUp() throws Exception {
        TestContextManager testContextManager = new TestContextManager(getClass());
        testContextManager.prepareTestInstance(this);
    }

    @MethodSource("data")
    @ParameterizedTest
    void testGetRecipeText(String inputFileName, String outputFileName, TemplatePreparationObject testData) throws IOException {
        TestFile outputFile = getTestFile(getFileName(RECIPE_UPDATER_TEST_OUTPUTS, outputFileName));

        String inputRecipeText = getTestFile(getFileName(RECIPE_UPDATER_TEST_INPUTS, inputFileName)).getFileContent();

        String expected = outputFile.getFileContent();
        String resultRecipeText = getUnderTest().getRecipeText(testData, inputRecipeText);
        LOGGER.info(String.format("Comparing expected and result recipe content. Expected content in file: %s%nexpected:%n%s%n%nactual:%n%s",
                outputFile.getFileName(), expected, resultRecipeText));

        assertEquals(expected, resultRecipeText);
    }

}