package com.sequenceiq.cloudbreak.clusterdefinition.moduletest;

import static com.sequenceiq.cloudbreak.clusterdefinition.moduletest.AmbariBlueprintModulTestModelProvider.blueprintObjectForHbaseConfigurationForTwoHosts;
import static com.sequenceiq.cloudbreak.clusterdefinition.moduletest.AmbariBlueprintModulTestModelProvider.blueprintObjectWhenADConfiguredWithRdsRanger;
import static com.sequenceiq.cloudbreak.clusterdefinition.moduletest.AmbariBlueprintModulTestModelProvider.blueprintObjectWhenADPresentedThenRangerAndHadoopADShouldConfigured;
import static com.sequenceiq.cloudbreak.clusterdefinition.moduletest.AmbariBlueprintModulTestModelProvider.blueprintObjectWhenAtlasAndADPresentedThenBothShouldConfigured;
import static com.sequenceiq.cloudbreak.clusterdefinition.moduletest.AmbariBlueprintModulTestModelProvider.blueprintObjectWhenAtlasAndLdapPresentedThenBothShouldConfigured;
import static com.sequenceiq.cloudbreak.clusterdefinition.moduletest.AmbariBlueprintModulTestModelProvider.blueprintObjectWhenAtlasPresentedShouldConfigured;
import static com.sequenceiq.cloudbreak.clusterdefinition.moduletest.AmbariBlueprintModulTestModelProvider.blueprintObjectWhenCustomPropertiesBlueprintConfigured;
import static com.sequenceiq.cloudbreak.clusterdefinition.moduletest.AmbariBlueprintModulTestModelProvider.blueprintObjectWhenDefaultBlueprintConfigured;
import static com.sequenceiq.cloudbreak.clusterdefinition.moduletest.AmbariBlueprintModulTestModelProvider.blueprintObjectWhenDlmBlueprintConfiguredAndAD;
import static com.sequenceiq.cloudbreak.clusterdefinition.moduletest.AmbariBlueprintModulTestModelProvider.blueprintObjectWhenDlmBlueprintConfiguredAndLdap;
import static com.sequenceiq.cloudbreak.clusterdefinition.moduletest.AmbariBlueprintModulTestModelProvider.blueprintObjectWhenDruidAndRdsPresentedThenRdsDruidShouldConfigured;
import static com.sequenceiq.cloudbreak.clusterdefinition.moduletest.AmbariBlueprintModulTestModelProvider.blueprintObjectWhenDuplicatedStorageLocationKey;
import static com.sequenceiq.cloudbreak.clusterdefinition.moduletest.AmbariBlueprintModulTestModelProvider.blueprintObjectWhenHiveAndRdsPresentedThenRdsHiveMetastoreShouldConfigured;
import static com.sequenceiq.cloudbreak.clusterdefinition.moduletest.AmbariBlueprintModulTestModelProvider.blueprintObjectWhenHiveInteractivePresentedTheLlapShouldConfigured;
import static com.sequenceiq.cloudbreak.clusterdefinition.moduletest.AmbariBlueprintModulTestModelProvider.blueprintObjectWhenKerberosPresentedThenKerberosShouldConfigured;
import static com.sequenceiq.cloudbreak.clusterdefinition.moduletest.AmbariBlueprintModulTestModelProvider.blueprintObjectWhenLdapAndDruidRdsConfigured;
import static com.sequenceiq.cloudbreak.clusterdefinition.moduletest.AmbariBlueprintModulTestModelProvider.blueprintObjectWhenLdapConfiguredWithRdsRanger;
import static com.sequenceiq.cloudbreak.clusterdefinition.moduletest.AmbariBlueprintModulTestModelProvider.blueprintObjectWhenLdapPresentedThenRangerAndHadoopLdapShouldConfigured;
import static com.sequenceiq.cloudbreak.clusterdefinition.moduletest.AmbariBlueprintModulTestModelProvider.blueprintObjectWhenNifiAndHdfAndLdapPresentedThenHdfShouldConfigured;
import static com.sequenceiq.cloudbreak.clusterdefinition.moduletest.AmbariBlueprintModulTestModelProvider.blueprintObjectWhenNifiAndHdfPresentedThenHdfShouldConfigured;
import static com.sequenceiq.cloudbreak.clusterdefinition.moduletest.AmbariBlueprintModulTestModelProvider.blueprintObjectWhenNothingSpecialThere;
import static com.sequenceiq.cloudbreak.clusterdefinition.moduletest.AmbariBlueprintModulTestModelProvider.blueprintObjectWhenRangerAndRdsPresentedThenRdsRangerShouldConfigured;
import static com.sequenceiq.cloudbreak.clusterdefinition.moduletest.AmbariBlueprintModulTestModelProvider.blueprintObjectWhenRdsConfiguredWithRdsOozie;
import static com.sequenceiq.cloudbreak.clusterdefinition.moduletest.AmbariBlueprintModulTestModelProvider.blueprintObjectWhenSharedServiceConfigured;
import static com.sequenceiq.cloudbreak.clusterdefinition.moduletest.AmbariBlueprintModulTestModelProvider.blueprintObjectWhenWebhcatConfigured;
import static com.sequenceiq.cloudbreak.clusterdefinition.moduletest.AmbariBlueprintModulTestModelProvider.blueprintObjectWhereExecutioTypeHasConfiguredAsContainer;
import static com.sequenceiq.cloudbreak.clusterdefinition.moduletest.AmbariBlueprintModulTestModelProvider.blueprintObjectWhereSmartSenseHasConfigured;
import static com.sequenceiq.cloudbreak.clusterdefinition.moduletest.AmbariBlueprintModulTestModelProvider.blueprintObjectWithZepelinAndHdp25PresentedThenZeppelinShouldConfigured;
import static com.sequenceiq.cloudbreak.clusterdefinition.moduletest.AmbariBlueprintModulTestModelProvider.blueprintObjectWithZepelinAndHdp26PresentedThenZeppelinShouldConfigured;
import static com.sequenceiq.cloudbreak.clusterdefinition.moduletest.AmbariBlueprintModulTestModelProvider.getFileName;
import static com.sequenceiq.cloudbreak.clusterdefinition.moduletest.AmbariBlueprintModulTestModelProvider.getTestFile;
import static net.javacrumbs.jsonunit.JsonAssert.assertJsonEquals;
import static net.javacrumbs.jsonunit.JsonAssert.when;
import static net.javacrumbs.jsonunit.core.Option.IGNORING_ARRAY_ORDER;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.skyscreamer.jsonassert.JSONParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.boot.test.context.SpringBootTestContextBootstrapper;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.test.context.BootstrapWith;
import org.springframework.test.context.TestContextManager;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.clusterdefinition.smartsense.SmartSenseConfigProvider;
import com.sequenceiq.cloudbreak.clusterdefinition.testrepeater.TestFile;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;

@RunWith(Parameterized.class)
@BootstrapWith(SpringBootTestContextBootstrapper.class)
public class AmbariBlueprintModulTest extends CentralBlueprintContext implements ApplicationContextAware {

    static final String BLUEPRINT_UPDATER_TEST_INPUTS = "module-test/inputs";

    private static final String BLUEPRINT_UPDATER_TEST_OUTPUTS = "module-test/outputs";

    private static final Logger LOGGER = LoggerFactory.getLogger(AmbariBlueprintModulTest.class);

    @Parameter
    public String inputFileName;

    @Parameter(1)
    public String outputFileName;

    @Parameter(2)
    public TemplatePreparationObject testData;

    private ApplicationContext applicationContext;

    @Parameters(name = "{index}: module-test/inputs/{0}.bp should equals module-test/outputs/{1}.bp")
    public static Collection<Object[]> data() throws IOException {
        Collection<Object[]> params = new ArrayList<>();
        params.add(new Object[]{"hive-metastore", "rds-with-hive-metastore",
                blueprintObjectWhenHiveAndRdsPresentedThenRdsHiveMetastoreShouldConfigured()});
        params.add(new Object[]{"ranger", "rds-with-ranger",
                blueprintObjectWhenRangerAndRdsPresentedThenRdsRangerShouldConfigured()});
        params.add(new Object[]{"druid", "rds-with-druid",
                blueprintObjectWhenDruidAndRdsPresentedThenRdsDruidShouldConfigured()});
        params.add(new Object[]{"ranger", "ldap-with-ranger-hadoop",
                blueprintObjectWhenLdapPresentedThenRangerAndHadoopLdapShouldConfigured()});
        params.add(new Object[]{"ranger", "ad-with-ranger-hadoop",
                blueprintObjectWhenADPresentedThenRangerAndHadoopADShouldConfigured()});
        params.add(new Object[]{"kerberos", "kerberos",
                blueprintObjectWhenKerberosPresentedThenKerberosShouldConfigured()});
        params.add(new Object[]{"zeppelin-2-6", "zeppelin-2-6",
                blueprintObjectWithZepelinAndHdp26PresentedThenZeppelinShouldConfigured()});
        params.add(new Object[]{"zeppelin-2-5", "zeppelin-2-5",
                blueprintObjectWithZepelinAndHdp25PresentedThenZeppelinShouldConfigured()});
        params.add(new Object[]{"hdf", "hdf",
                blueprintObjectWhenNifiAndHdfPresentedThenHdfShouldConfigured()});
        params.add(new Object[]{"hdf", "hdf-ldap",
                blueprintObjectWhenNifiAndHdfAndLdapPresentedThenHdfShouldConfigured()});
        params.add(new Object[]{"llap", "llap",
                blueprintObjectWhenHiveInteractivePresentedTheLlapShouldConfigured()});
        params.add(new Object[]{"hbase", "hbase",
                blueprintObjectWhenNothingSpecialThere()});
        params.add(new Object[]{"ranger", "ldap-with-rds-ranger",
                blueprintObjectWhenLdapConfiguredWithRdsRanger()});
        params.add(new Object[]{"ranger", "ad-with-rds-ranger",
                blueprintObjectWhenADConfiguredWithRdsRanger()});
        params.add(new Object[]{"druid", "ldap-with-rds-druid",
                blueprintObjectWhenLdapAndDruidRdsConfigured()});
        params.add(new Object[]{"atlas", "atlas-without-ldap",
                blueprintObjectWhenAtlasPresentedShouldConfigured()});
        params.add(new Object[]{"atlas", "atlas-with-ldap",
                blueprintObjectWhenAtlasAndLdapPresentedThenBothShouldConfigured()});
        params.add(new Object[]{"atlas", "atlas-with-ad",
                blueprintObjectWhenAtlasAndADPresentedThenBothShouldConfigured()});
        params.add(new Object[]{"hbase-master-in-all-host", "hbase-master-in-all-host",
                blueprintObjectForHbaseConfigurationForTwoHosts()});
        params.add(new Object[]{"one-hbase-master-one-client-in-different-host", "one-hbase-master-one-client-in-different-host",
                blueprintObjectForHbaseConfigurationForTwoHosts()});
        params.add(new Object[]{"execution-type-container", "execution-type-container",
                blueprintObjectWhereExecutioTypeHasConfiguredAsContainer()});
        params.add(new Object[]{"smartsense", "smartsense",
                blueprintObjectWhereSmartSenseHasConfigured()});
        params.add(new Object[]{"smartsense-when-no-hst-server", "smartsense-when-no-hst-server",
                blueprintObjectWhereSmartSenseHasConfigured()});
        params.add(new Object[]{"oozie", "oozie",
                blueprintObjectWhenRdsConfiguredWithRdsOozie()});
        params.add(new Object[]{"webhcat", "webhcat-2-6",
                blueprintObjectWhenWebhcatConfigured()});
        params.add(new Object[]{"shared-service", "shared-service",
                blueprintObjectWhenSharedServiceConfigured()});
        params.add(new Object[]{"hdf31-flow-management", "hdf31-flow-management",
                blueprintObjectWhenNifiAndHdfPresentedThenHdfShouldConfigured()});
        params.add(new Object[]{"hdf31-messaging-kafka", "hdf31-messaging-kafka",
                blueprintObjectWhenNifiAndHdfPresentedThenHdfShouldConfigured()});
        params.add(new Object[]{"hdp26-data-science-spark2", "hdp26-data-science-spark2",
                blueprintObjectWhenDefaultBlueprintConfigured()});
        params.add(new Object[]{"hdp26-druid-bi", "hdp26-druid-bi",
                blueprintObjectWhenDefaultBlueprintConfigured()});
        params.add(new Object[]{"hdp26-edw-analytics", "hdp26-edw-analytics",
                blueprintObjectWhenDefaultBlueprintConfigured()});
        params.add(new Object[]{"hdp26-etl-edw-spark2", "hdp26-etl-edw-spark2",
                blueprintObjectWhenDefaultBlueprintConfigured()});
        params.add(new Object[]{"hdp30-data-science-spark2", "hdp30-data-science-spark2",
                blueprintObjectWhenDefaultBlueprintConfigured()});
        params.add(new Object[]{"dlm", "ldap-dlm",
                blueprintObjectWhenDlmBlueprintConfiguredAndLdap("dlm")});
        params.add(new Object[]{"dlm", "ad-dlm",
                blueprintObjectWhenDlmBlueprintConfiguredAndAD("dlm")});
        params.add(new Object[]{"custom-properties", "custom-properties",
                blueprintObjectWhenCustomPropertiesBlueprintConfigured()});
        params.add(new Object[]{"s3-duplicated-key", "s3-duplicated-key",
                blueprintObjectWhenDuplicatedStorageLocationKey("s3-duplicated-key")});
        return params;
    }

    @Before
    public void setUp() throws Exception {
        TestContextManager testContextManager = new TestContextManager(getClass());
        testContextManager.prepareTestInstance(this);
        SmartSenseConfigProvider smartSenseConfigProvider = applicationContext.getBean(SmartSenseConfigProvider.class);
        ReflectionTestUtils.setField(smartSenseConfigProvider, "cbVersion", "custom.cb.version");
    }

    @Test
    public void testGetBlueprintText() throws IOException, JSONException {
        TestFile inputFile = getTestFile(getFileName(BLUEPRINT_UPDATER_TEST_INPUTS, inputFileName));
        TestFile outputFile = getTestFile(getFileName(BLUEPRINT_UPDATER_TEST_OUTPUTS, outputFileName));

        TemplatePreparationObject templatePreparationObject =
                prepareBlueprintPreparationObjectWithBlueprintText(inputFile);

        JSONObject expected = toJSON(outputFile.getFileContent());
        JSONObject resultBlueprintText = toJSON(getUnderTest().getBlueprintText(templatePreparationObject));
        StringBuilder messageBuilder = new StringBuilder();
        messageBuilder.append("The result has not matched with the expected output ").append(outputFile.getFileName());
        messageBuilder.append("\nexpected:\n");
        messageBuilder.append(expected);
        messageBuilder.append("\nactual:\n");
        messageBuilder.append(resultBlueprintText);
        LOGGER.info(messageBuilder.toString());

        assertJsonEquals(expected.toString(), resultBlueprintText.toString(), when(IGNORING_ARRAY_ORDER));
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    private TemplatePreparationObject prepareBlueprintPreparationObjectWithBlueprintText(TestFile inputFile) {
        TemplatePreparationObject templatePreparationObject = testData;
        templatePreparationObject.getClusterDefinitionView().setBlueprintText(inputFile.getFileContent());
        return templatePreparationObject;
    }

    private JSONObject toJSON(String jsonText) throws JSONException {
        return (JSONObject) JSONParser.parseJSON(jsonText);
    }
}