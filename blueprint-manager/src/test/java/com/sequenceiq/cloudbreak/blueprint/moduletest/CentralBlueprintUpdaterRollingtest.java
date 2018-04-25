package com.sequenceiq.cloudbreak.blueprint.moduletest;

import static com.sequenceiq.cloudbreak.blueprint.moduletest.BlueprintModelProvider.blueprintObjectForHbaseConfigurationForTwoHosts;
import static com.sequenceiq.cloudbreak.blueprint.moduletest.BlueprintModelProvider.blueprintObjectWhenAtlasAndLdapPresentedThenBothShouldConfigured;
import static com.sequenceiq.cloudbreak.blueprint.moduletest.BlueprintModelProvider.blueprintObjectWhenAtlasPresentedShouldConfigured;
import static com.sequenceiq.cloudbreak.blueprint.moduletest.BlueprintModelProvider.blueprintObjectWhenDefaultBlueprintConfigured;
import static com.sequenceiq.cloudbreak.blueprint.moduletest.BlueprintModelProvider.blueprintObjectWhenDruidAndRdsPresentedThenRdsDruidShouldConfigured;
import static com.sequenceiq.cloudbreak.blueprint.moduletest.BlueprintModelProvider.blueprintObjectWhenHiveAndRdsPresentedThenRdsHiveMetastoreShouldConfigured;
import static com.sequenceiq.cloudbreak.blueprint.moduletest.BlueprintModelProvider.blueprintObjectWhenHiveInteractivePresentedTheLlapShouldConfigured;
import static com.sequenceiq.cloudbreak.blueprint.moduletest.BlueprintModelProvider.blueprintObjectWhenKerberosPresentedThenKerberosShouldConfigured;
import static com.sequenceiq.cloudbreak.blueprint.moduletest.BlueprintModelProvider.blueprintObjectWhenLdapAndDruidRdsConfigured;
import static com.sequenceiq.cloudbreak.blueprint.moduletest.BlueprintModelProvider.blueprintObjectWhenLdapConfiguredWithRdsRanger;
import static com.sequenceiq.cloudbreak.blueprint.moduletest.BlueprintModelProvider.blueprintObjectWhenLdapPresentedThenRangerAndHadoopLdapShouldConfigured;
import static com.sequenceiq.cloudbreak.blueprint.moduletest.BlueprintModelProvider.blueprintObjectWhenNifiAndHdfPresentedThenHdfShouldConfigured;
import static com.sequenceiq.cloudbreak.blueprint.moduletest.BlueprintModelProvider.blueprintObjectWhenNothingSpecialThere;
import static com.sequenceiq.cloudbreak.blueprint.moduletest.BlueprintModelProvider.blueprintObjectWhenRangerAndRdsPresentedThenRdsRangerShouldConfigured;
import static com.sequenceiq.cloudbreak.blueprint.moduletest.BlueprintModelProvider.blueprintObjectWhenRdsConfiguredWithRdsOozie;
import static com.sequenceiq.cloudbreak.blueprint.moduletest.BlueprintModelProvider.blueprintObjectWhenSharedServiceConfigured;
import static com.sequenceiq.cloudbreak.blueprint.moduletest.BlueprintModelProvider.blueprintObjectWhenWebhcatConfigured;
import static com.sequenceiq.cloudbreak.blueprint.moduletest.BlueprintModelProvider.blueprintObjectWhereExecutioTypeHasConfiguredAsContainer;
import static com.sequenceiq.cloudbreak.blueprint.moduletest.BlueprintModelProvider.blueprintObjectWhereSmartSenseHasConfigured;
import static com.sequenceiq.cloudbreak.blueprint.moduletest.BlueprintModelProvider.blueprintObjectWithZepelinAndHdp25PresentedThenZeppelinShouldConfigured;
import static com.sequenceiq.cloudbreak.blueprint.moduletest.BlueprintModelProvider.blueprintObjectWithZepelinAndHdp26PresentedThenZeppelinShouldConfigured;
import static net.javacrumbs.jsonunit.JsonAssert.assertJsonEquals;
import static net.javacrumbs.jsonunit.JsonAssert.when;
import static net.javacrumbs.jsonunit.core.Option.IGNORING_ARRAY_ORDER;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.skyscreamer.jsonassert.JSONParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTestContextBootstrapper;
import org.springframework.test.context.BootstrapWith;
import org.springframework.test.context.TestContextManager;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.sequenceiq.cloudbreak.blueprint.BlueprintPreparationObject;
import com.sequenceiq.cloudbreak.blueprint.testrepeater.TestFile;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;

@RunWith(Parameterized.class)
@BootstrapWith(SpringBootTestContextBootstrapper.class)
public class CentralBlueprintUpdaterRollingtest extends CentralBlueprintContext {

    private static final Logger LOGGER = LoggerFactory.getLogger(CentralBlueprintUpdaterRollingtest.class);

    private static final String BLUEPRINT_UPDATER_TEST_INPUTS = "module-test/inputs";

    private static final String BLUEPRINT_UPDATER_TEST_OUTPUTS = "module-test/outputs";

    @Parameterized.Parameter(value = 0)
    public String inputFileName;

    @Parameterized.Parameter(value = 1)
    public String outputFileName;

    @Parameterized.Parameter(value = 2)
    public BlueprintPreparationObject testData;

    private TestContextManager testContextManager;

    @Parameterized.Parameters(name = "{index}: module-test/inputs/{0}.bp should equals module-test/outputs/{1}.bp")
    public static Collection<Object[]> data() throws JsonProcessingException {
        Collection<Object[]> params = new ArrayList<>();
        params.add(new Object[] { "hive-metastore", "rds-with-hive-metastore",
                blueprintObjectWhenHiveAndRdsPresentedThenRdsHiveMetastoreShouldConfigured() });
        params.add(new Object[] { "ranger", "rds-with-ranger",
                blueprintObjectWhenRangerAndRdsPresentedThenRdsRangerShouldConfigured() });
        params.add(new Object[] { "druid", "rds-with-druid",
                blueprintObjectWhenDruidAndRdsPresentedThenRdsDruidShouldConfigured() });
        params.add(new Object[] { "ranger", "ldap-with-ranger-hadoop",
                blueprintObjectWhenLdapPresentedThenRangerAndHadoopLdapShouldConfigured() });
        params.add(new Object[] { "kerberos", "kerberos",
                blueprintObjectWhenKerberosPresentedThenKerberosShouldConfigured() });
        params.add(new Object[] { "zeppelin-2-6", "zeppelin-2-6",
                blueprintObjectWithZepelinAndHdp26PresentedThenZeppelinShouldConfigured() });
        params.add(new Object[] { "zeppelin-2-5", "zeppelin-2-5",
                blueprintObjectWithZepelinAndHdp25PresentedThenZeppelinShouldConfigured() });
        params.add(new Object[] { "hdf", "hdf",
                blueprintObjectWhenNifiAndHdfPresentedThenHdfShouldConfigured() });
        params.add(new Object[] { "llap", "llap",
                blueprintObjectWhenHiveInteractivePresentedTheLlapShouldConfigured() });
        params.add(new Object[] { "hbase", "hbase",
                blueprintObjectWhenNothingSpecialThere() });
        params.add(new Object[] { "ranger", "ldap-with-rds-ranger",
                blueprintObjectWhenLdapConfiguredWithRdsRanger() });
        params.add(new Object[] { "druid", "ldap-with-rds-druid",
                blueprintObjectWhenLdapAndDruidRdsConfigured() });
        params.add(new Object[] { "atlas", "atlas-without-ldap",
                blueprintObjectWhenAtlasPresentedShouldConfigured() });
        params.add(new Object[] { "atlas", "atlas-with-ldap",
                blueprintObjectWhenAtlasAndLdapPresentedThenBothShouldConfigured() });
        params.add(new Object[] { "hbase-master-in-all-host", "hbase-master-in-all-host",
                blueprintObjectForHbaseConfigurationForTwoHosts() });
        params.add(new Object[] { "one-hbase-master-one-client-in-different-host", "one-hbase-master-one-client-in-different-host",
                blueprintObjectForHbaseConfigurationForTwoHosts() });
        params.add(new Object[] { "execution-type-container", "execution-type-container",
                blueprintObjectWhereExecutioTypeHasConfiguredAsContainer() });
        params.add(new Object[] { "smartsense", "smartsense",
                blueprintObjectWhereSmartSenseHasConfigured() });
        params.add(new Object[] { "smartsense-when-no-hst-server", "smartsense-when-no-hst-server",
                blueprintObjectWhereSmartSenseHasConfigured() });
        params.add(new Object[] { "oozie", "oozie",
                blueprintObjectWhenRdsConfiguredWithRdsOozie() });
        params.add(new Object[] { "webhcat", "webhcat-2-6",
                blueprintObjectWhenWebhcatConfigured() });
        params.add(new Object[] { "shared-service", "shared-service",
                blueprintObjectWhenSharedServiceConfigured() });
        params.add(new Object[] { "hdf31-flow-management", "hdf31-flow-management",
                blueprintObjectWhenNifiAndHdfPresentedThenHdfShouldConfigured() });
        params.add(new Object[] { "hdf31-messaging-kafka", "hdf31-messaging-kafka",
                blueprintObjectWhenNifiAndHdfPresentedThenHdfShouldConfigured() });
        params.add(new Object[] { "hdp26-data-science-spark2", "hdp26-data-science-spark2",
                blueprintObjectWhenDefaultBlueprintConfigured() });
        params.add(new Object[] { "hdp26-druid-bi", "hdp26-druid-bi",
                blueprintObjectWhenDefaultBlueprintConfigured() });
        params.add(new Object[] { "hdp26-edw-analytics", "hdp26-edw-analytics",
                blueprintObjectWhenDefaultBlueprintConfigured() });
        params.add(new Object[] { "hdp26-etl-edw-spark2", "hdp26-etl-edw-spark2",
                blueprintObjectWhenDefaultBlueprintConfigured() });
        params.add(new Object[] { "hdp30-data-science-spark2", "hdp30-data-science-spark2",
                blueprintObjectWhenDefaultBlueprintConfigured() });
        return params;
    }

    @Before
    public void setUp() throws Exception {
        this.testContextManager = new TestContextManager(getClass());
        this.testContextManager.prepareTestInstance(this);
    }

    @Test
    public void testGetBlueprintText() throws IOException, JSONException {
        TestFile inputFile = getTestFile(getFileName(BLUEPRINT_UPDATER_TEST_INPUTS, inputFileName));
        TestFile outputFile = getTestFile(getFileName(BLUEPRINT_UPDATER_TEST_OUTPUTS, outputFileName));

        BlueprintPreparationObject blueprintPreparationObject =
                prepareBlueprintPreparationObjectWithBlueprintText(inputFile);

        JSONObject expected = toJSON(outputFile.getFileContent());
        JSONObject resultBlueprintText = toJSON(getUnderTest().getBlueprintText(blueprintPreparationObject));
        StringBuilder messageBuilder = new StringBuilder();
        messageBuilder.append("The result has not matched with the expected output " + outputFile.getFileName());
        messageBuilder.append("\nexpected:\n");
        messageBuilder.append(expected.toString());
        messageBuilder.append("\nactual:\n");
        messageBuilder.append(resultBlueprintText.toString());
        LOGGER.info(messageBuilder.toString());

        assertJsonEquals(expected.toString(), resultBlueprintText.toString(), when(IGNORING_ARRAY_ORDER));
    }

    private BlueprintPreparationObject prepareBlueprintPreparationObjectWithBlueprintText(TestFile inputFile) {
        BlueprintPreparationObject blueprintPreparationObject = testData;
        blueprintPreparationObject.getBlueprintView().setBlueprintText(inputFile.getFileContent());
        return blueprintPreparationObject;
    }

    private JSONObject toJSON(String jsonText) throws JSONException {
        return (JSONObject) JSONParser.parseJSON(jsonText);
    }

    private static TestFile getTestFile(String fileName) throws IOException {
        return new TestFile(new File(fileName).toPath(), FileReaderUtils.readFileFromClasspath(fileName));
    }

    private static String getFileName(String folder, String filename) {
        return folder + "/" + filename + ".bp";
    }

}