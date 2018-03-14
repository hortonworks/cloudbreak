package com.sequenceiq.cloudbreak.blueprint.moduletest;

import static com.sequenceiq.cloudbreak.blueprint.moduletest.BlueprintModelProvider.blueprintObjectForHbaseConfigurationForTwoHosts;
import static com.sequenceiq.cloudbreak.blueprint.moduletest.BlueprintModelProvider.blueprintObjectWhenAtlasAndLdapPresentedThenBothShouldConfigured;
import static com.sequenceiq.cloudbreak.blueprint.moduletest.BlueprintModelProvider.blueprintObjectWhenAtlasPresentedShouldConfigured;
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
import static com.sequenceiq.cloudbreak.blueprint.moduletest.BlueprintModelProvider.blueprintObjectWhereExecutioTypeHasConfiguredAsContainer;
import static com.sequenceiq.cloudbreak.blueprint.moduletest.BlueprintModelProvider.blueprintObjectWhereSmartSenseHasConfigured;
import static com.sequenceiq.cloudbreak.blueprint.moduletest.BlueprintModelProvider.blueprintObjectWithZepelinAndHdp25PresentedThenZeppelinShouldConfigured;
import static com.sequenceiq.cloudbreak.blueprint.moduletest.BlueprintModelProvider.blueprintObjectWithZepelinAndHdp26PresentedThenZeppelinShouldConfigured;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.skyscreamer.jsonassert.JSONParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTestContextBootstrapper;
import org.springframework.test.context.BootstrapWith;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.sequenceiq.cloudbreak.blueprint.BlueprintPreparationObject;
import com.sequenceiq.cloudbreak.blueprint.testrepeater.Generator;
import com.sequenceiq.cloudbreak.blueprint.testrepeater.ListGenerator;

@RunWith(SpringJUnit4ClassRunner.class)
@BootstrapWith(SpringBootTestContextBootstrapper.class)
public class CentralBlueprintUpdaterRollingtest extends CentralBlueprintContext {

    private static final Logger LOGGER = LoggerFactory.getLogger(CentralBlueprintUpdaterRollingtest.class);

    @Rule
    public Generator<BlueprintDataProvider> params;

    public CentralBlueprintUpdaterRollingtest() throws IOException {
        params = new ListGenerator<>(ReadTestData.getInputOutputData(testConfig()));
    }

    private Map<String, BlueprintPreparationObject> testConfig() throws JsonProcessingException {
        return new HashMap<String, BlueprintPreparationObject>() {
            {
                put("rds-with-hive-metastore", blueprintObjectWhenHiveAndRdsPresentedThenRdsHiveMetastoreShouldConfigured());
                put("rds-with-ranger", blueprintObjectWhenRangerAndRdsPresentedThenRdsRangerShouldConfigured());
                put("rds-with-druid", blueprintObjectWhenDruidAndRdsPresentedThenRdsDruidShouldConfigured());
                put("ldap-with-ranger-hadoop", blueprintObjectWhenLdapPresentedThenRangerAndHadoopLdapShouldConfigured());
                put("kerberos", blueprintObjectWhenKerberosPresentedThenKerberosShouldConfigured());
                put("zeppelin-2-6", blueprintObjectWithZepelinAndHdp26PresentedThenZeppelinShouldConfigured());
                put("zeppelin-2-5", blueprintObjectWithZepelinAndHdp25PresentedThenZeppelinShouldConfigured());
                put("hdf", blueprintObjectWhenNifiAndHdfPresentedThenHdfShouldConfigured());
                put("llap", blueprintObjectWhenHiveInteractivePresentedTheLlapShouldConfigured());
                put("hbase", blueprintObjectWhenNothingSpecialThere());
                put("ldap-with-rds-ranger", blueprintObjectWhenLdapConfiguredWithRdsRanger());
                put("ldap-with-rds-druid", blueprintObjectWhenLdapAndDruidRdsConfigured());
                put("atlas-without-ldap", blueprintObjectWhenAtlasPresentedShouldConfigured());
                put("atlas-with-ldap", blueprintObjectWhenAtlasAndLdapPresentedThenBothShouldConfigured());
                put("hbase-master-in-all-host", blueprintObjectForHbaseConfigurationForTwoHosts());
                put("one-hbase-master-one-client-in-different-host", blueprintObjectForHbaseConfigurationForTwoHosts());
                put("execution-type-container", blueprintObjectWhereExecutioTypeHasConfiguredAsContainer());
                put("smartsense", blueprintObjectWhereSmartSenseHasConfigured());
                put("smartsense-when-no-hst-server", blueprintObjectWhereSmartSenseHasConfigured());
                put("oozie", blueprintObjectWhenRdsConfiguredWithRdsOozie());
            }
        };
    }

    @Test
    public void testGetBlueprintText() throws IOException, JSONException {
        BlueprintPreparationObject blueprintPreparationObject = prepareBlueprintPreparationObjectWithBlueprintText();

        JSONObject expected = toJSON(params.value().getOutput().getFileContent());
        JSONObject resultBlueprintText = toJSON(getUnderTest().getBlueprintText(blueprintPreparationObject));
        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append("The result has not matched with the expected output " + params.value().getOutput().getFileName());
        stringBuffer.append("\nexpected:\n");
        stringBuffer.append(new JSONObject(expected.toString()).toString(4));
        stringBuffer.append("\nactual:\n");
        stringBuffer.append(new JSONObject(resultBlueprintText.toString()).toString(4));

        assertWithExtendedExceptionHandling(stringBuffer.toString(), expected, resultBlueprintText);
    }

    private BlueprintPreparationObject prepareBlueprintPreparationObjectWithBlueprintText() {
        BlueprintPreparationObject blueprintPreparationObject = params.value().getModel();
        blueprintPreparationObject.getBlueprintView().setBlueprintText(params.value().getInput().getFileContent());
        return blueprintPreparationObject;
    }

    private void assertWithExtendedExceptionHandling(String message, JSONObject expected, JSONObject actual) throws JSONException {
        try {
            JSONAssert.assertEquals(expected, actual, JSONCompareMode.LENIENT);
        } catch (AssertionError ae) {
            throw new ModuleTestError(message, ae);
        }
    }

    private JSONObject toJSON(String jsonText) throws JSONException {
        return (JSONObject) JSONParser.parseJSON(jsonText);
    }

}