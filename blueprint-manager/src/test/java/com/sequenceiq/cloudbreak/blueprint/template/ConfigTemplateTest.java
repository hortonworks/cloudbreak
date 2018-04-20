package com.sequenceiq.cloudbreak.blueprint.template;

import static com.sequenceiq.cloudbreak.util.FileReaderUtils.readFileFromClasspath;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Template;
import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.TestUtil;
import com.sequenceiq.cloudbreak.api.model.rds.RdsType;
import com.sequenceiq.cloudbreak.blueprint.nifi.HdfConfigs;
import com.sequenceiq.cloudbreak.domain.RDSConfig;

@RunWith(Parameterized.class)
public class ConfigTemplateTest {

    private Handlebars handlebars = HandlebarUtils.handlebars();

    private String input;

    private String output;

    private Map<String, Object> model;

    public ConfigTemplateTest(String input, String output, Map<String, Object> model) {
        this.input = input;
        this.output = output;
        this.model = model;
    }

    @Parameterized.Parameters(name = "{index}: templateTest {0} with handlebar where the expected file is  {1}")
    public static Iterable<Object[]> data() throws JsonProcessingException {
        return Arrays.asList(new Object[][] {
                { "blueprints/configurations/atlas/ldap.handlebars", "configurations/atlas/atlas-with-ldap.json",
                        ldapConfigWhenLdapPresentedThenShouldReturnWithLdapConfig() },
                { "blueprints/configurations/atlas/ldap.handlebars", "configurations/atlas/atlas-without-ldap.json",
                        withoutLdapConfigWhenLdapNotPresentedThenShouldReturnWithoutLdapConfig() },
                { "blueprints/configurations/druid/rds.handlebars", "configurations/druid/druid-with-rds.json",
                        druidRdsConfigWhenRdsPresentedThenShouldReturnWithRdsConfig() },
                { "blueprints/configurations/druid/rds.handlebars", "configurations/druid/druid-without-rds.json",
                        druidWithoutRdsConfigWhenRdsNotPresentedThenShouldReturnWithoutRdsConfig() },
                { "blueprints/configurations/superset/rds.handlebars", "configurations/superset/superset-with-rds.json",
                        supersetRdsConfigWhenRdsPresentedThenShouldReturnWithRdsConfig() },
                { "blueprints/configurations/superset/rds.handlebars", "configurations/superset/superset-without-rds.json",
                        supersetWithoutRdsConfigWhenRdsNotPresentedThenShouldReturnWithoutRdsConfig() },
                { "blueprints/configurations/hadoop/ldap.handlebars", "configurations/hadoop/hadoop-with-ldap.json",
                        ldapConfigWhenLdapPresentedThenShouldReturnWithLdapConfig() },
                { "blueprints/configurations/hadoop/ldap.handlebars", "configurations/hadoop/hadoop-without-ldap.json",
                        withoutLdapConfigWhenLdapNotPresentedThenShouldReturnWithoutLdapConfig() },
                { "blueprints/configurations/hadoop/global.handlebars", "configurations/hadoop/global.json",
                        objectWithoutEverything() },
                { "blueprints/configurations/hive/rds.handlebars", "configurations/hive/hive-with-rds.json",
                        hiveRdsConfigWhenRdsPresentedThenShouldReturnWithRdsConfig() },
                { "blueprints/configurations/hive/rds.handlebars", "configurations/hive/hive-without-rds.json",
                        objectWithoutEverything() },
                { "blueprints/configurations/llap/global.handlebars", "configurations/llap/global.json",
                        llapObjectWhenNodeCountPresented() },
                { "blueprints/configurations/nifi/global.handlebars", "configurations/nifi/global-with-hdf-nifitargets.json",
                        nifiConfigWhenHdfPresentedThenShouldReturnWithNifiConfig(false) },
                { "blueprints/configurations/nifi/global.handlebars", "configurations/nifi/global-with-hdf-full.json",
                        nifiConfigWhenHdfPresentedThenShouldReturnWithNifiConfig(true) },
                { "blueprints/configurations/nifi/global.handlebars", "configurations/nifi/global-without-hdf.json",
                        nifiConfigWhenHdfNotPresentedThenShouldReturnWithNotNifiConfig() },
                { "blueprints/configurations/ranger/global.handlebars", "configurations/ranger/global.json",
                        objectWithoutEverything() },
                { "blueprints/configurations/ranger/ldap.handlebars", "configurations/ranger/ranger-with-ldap.json",
                        ldapConfigWhenLdapPresentedThenShouldReturnWithLdapConfig() },
                { "blueprints/configurations/ranger/ldap.handlebars", "configurations/ranger/ranger-without-ldap.json",
                        withoutLdapConfigWhenLdapNotPresentedThenShouldReturnWithoutLdapConfig() },
                { "blueprints/configurations/ranger/rds.handlebars", "configurations/ranger/ranger-with-rds.json",
                        rangerRdsConfigWhenRdsPresentedThenShouldReturnWithRdsConfig() },
                { "blueprints/configurations/ranger/rds.handlebars", "configurations/ranger/ranger-without-rds.json",
                        objectWithoutEverything() },
                { "blueprints/configurations/yarn/global.handlebars", "configurations/yarn/global-without-container.json",
                        objectContainerExecutorIsFalseThenShouldReturnWithoutContainerConfigs() },
                { "blueprints/configurations/yarn/global.handlebars", "configurations/yarn/global-with-container.json",
                        objectContainerExecutorIsTrueThenShouldReturnWithContainerConfigs() },
                { "blueprints/configurations/zeppelin/global.handlebars", "configurations/zeppelin/global-with-2_5.json",
                        zeppelinWhenStackVersionIs25ThenShouldReturnWithZeppelinEnvConfigs() },
                { "blueprints/configurations/zeppelin/global.handlebars", "configurations/zeppelin/global-without-2_5.json",
                        zeppelinWhenStackVersionIsNot25ThenShouldReturnWithZeppelinShiroIniConfigs() },
                { "blueprints/configurations/hive/ldap.handlebars", "configurations/hive/hive-with-ldap.json",
                        hiveWhenLdapPresentedThenShouldReturnWithLdapConfigs() },
                { "blueprints/configurations/hive/ldap.handlebars", "configurations/hive/hive-without-ldap.json",
                        hiveWhenLdapNotPresentedThenShouldReturnWithoutLdapConfigs() },
                { "blueprints/configurations/oozie/rds.handlebars", "configurations/oozie/oozie-with-rds.json",
                        oozieWhenRdsPresentedThenShouldReturnWithRdsConfigs() },
                { "blueprints/configurations/oozie/rds.handlebars", "configurations/oozie/oozie-without-rds.json",
                        objectWithoutEverything() },
                { "blueprints/configurations/webhcat/global.handlebars", "configurations/webhcat/webhcat.json",
                        objectWithoutEverything() },
        });
    }

    @Test
    public void test() throws IOException {
        String actual = compileTemplate(input, model);
        String expected = readExpectedTemplate(output);
        String message = String.format("expected: [%s] %nactual: [%s] %n", expected, actual);

        Assert.assertEquals(message, expected, actual);
    }

    public static Map<String, Object> objectWithoutEverything() {
        return new BlueprintTemplateModelContextBuilder()
                .build();
    }

    public static Map<String, Object> oozieWhenRdsPresentedThenShouldReturnWithRdsConfigs() {
        return new BlueprintTemplateModelContextBuilder()
                .withRdsConfigs(Sets.newHashSet(TestUtil.rdsConfig(RdsType.OOZIE)))
                .build();
    }

    public static Map<String, Object> zeppelinWhenStackVersionIsNot25ThenShouldReturnWithZeppelinShiroIniConfigs() {
        Map<String, Object> properties = new HashMap<>();
        properties.put("blueprints_basics_zeppelin_shiro_ini_content", "testshiroini");

        return new BlueprintTemplateModelContextBuilder()
                .withStackVersion("2.6")
                .withCustomProperties(properties)
                .build();
    }

    public static Map<String, Object> zeppelinWhenStackVersionIs25ThenShouldReturnWithZeppelinEnvConfigs() {
        Map<String, Object> properties = new HashMap<>();
        properties.put("blueprints_basics_zeppelin_shiro_ini_content", "testshiroini");

        return new BlueprintTemplateModelContextBuilder()
                .withStackVersion("2.5")
                .withCustomProperties(properties)
                .build();
    }

    public static Map<String, Object> objectContainerExecutorIsTrueThenShouldReturnWithContainerConfigs() {
        return new BlueprintTemplateModelContextBuilder()
                .withContainerExecutor(true)
                .build();
    }

    public static Map<String, Object> objectContainerExecutorIsFalseThenShouldReturnWithoutContainerConfigs() {
        return new BlueprintTemplateModelContextBuilder()
                .withContainerExecutor(false)
                .build();
    }

    public static Map<String, Object> llapObjectWhenNodeCountPresented() {
        return new BlueprintTemplateModelContextBuilder()
                .withLlapNodeCounts(5)
                .build();
    }

    public static Map<String, Object> ldapConfigWhenLdapPresentedThenShouldReturnWithLdapConfig() throws JsonProcessingException {
        return new BlueprintTemplateModelContextBuilder()
                .withLdap(TestUtil.ldapConfig())
                .withGateway(TestUtil.gateway())
                .build();
    }

    public static Map<String, Object> withoutLdapConfigWhenLdapNotPresentedThenShouldReturnWithoutLdapConfig() {
        return new BlueprintTemplateModelContextBuilder()
                .build();
    }

    public static Map<String, Object> druidRdsConfigWhenRdsPresentedThenShouldReturnWithRdsConfig() {
        return new BlueprintTemplateModelContextBuilder()
                .withRdsConfigs(Sets.newHashSet(TestUtil.rdsConfig(RdsType.DRUID)))
                .withClusterAdminPassword("adminPassword")
                .withClusterAdminLastname("lastname")
                .withClusterAdminFirstname("firstname")
                .withAdminEmail("admin@example.com")
                .build();
    }

    public static Map<String, Object> druidWithoutRdsConfigWhenRdsNotPresentedThenShouldReturnWithoutRdsConfig() {
        return new BlueprintTemplateModelContextBuilder()
                .withClusterAdminPassword("adminPassword")
                .withClusterAdminLastname("lastname")
                .withClusterAdminFirstname("firstname")
                .withAdminEmail("admin@example.com")
                .build();
    }

    public static Map<String, Object> supersetRdsConfigWhenRdsPresentedThenShouldReturnWithRdsConfig() {
        return new BlueprintTemplateModelContextBuilder()
                .withRdsConfigs(Sets.newHashSet(TestUtil.rdsConfig(RdsType.SUPERSET)))
                .withClusterAdminPassword("adminPassword")
                .withClusterAdminLastname("lastname")
                .withClusterAdminFirstname("firstname")
                .withAdminEmail("admin@example.com")
                .build();
    }

    public static Map<String, Object> supersetWithoutRdsConfigWhenRdsNotPresentedThenShouldReturnWithoutRdsConfig() {
        return new BlueprintTemplateModelContextBuilder()
                .withClusterAdminPassword("adminPassword")
                .withClusterAdminLastname("lastname")
                .withClusterAdminFirstname("firstname")
                .withAdminEmail("admin@example.com")
                .build();
    }

    public static Map<String, Object> nifiConfigWhenHdfPresentedThenShouldReturnWithNifiConfig(boolean withProxyHost) {
        return new BlueprintTemplateModelContextBuilder()
                .withClusterAdminPassword("adminPassword")
                .withClusterAdminFirstname("firstname")
                .withHdfConfigs(Optional.of(new HdfConfigs("nifigtargets", "nifigtargets", withProxyHost ? Optional.of("nifiproxyhost") : Optional.empty())))
                .withStackType("HDF")
                .build();
    }

    public static Map<String, Object> nifiConfigWhenHdfNotPresentedThenShouldReturnWithNotNifiConfig() {
        return new BlueprintTemplateModelContextBuilder()
                .withClusterAdminPassword("adminPassword")
                .withClusterAdminFirstname("firstname")
                .withHdfConfigs(Optional.of(new HdfConfigs("nifigtargets", "nifigtargets", Optional.empty())))
                .withStackType("HDP")
                .build();
    }

    public static Map<String, Object> hiveRdsConfigWhenRdsPresentedThenShouldReturnWithRdsConfig() {
        return new BlueprintTemplateModelContextBuilder()
                .withRdsConfigs(Sets.newHashSet(TestUtil.rdsConfig(RdsType.HIVE)))
                .build();
    }

    private static Object hiveWhenLdapPresentedThenShouldReturnWithLdapConfigs() {
        return new BlueprintTemplateModelContextBuilder()
                .withLdap(TestUtil.ldapConfig())
                .build();
    }

    private static Object hiveWhenLdapNotPresentedThenShouldReturnWithoutLdapConfigs() {
        return new BlueprintTemplateModelContextBuilder()
                .build();
    }

    public static Map<String, Object> rangerRdsConfigWhenRdsPresentedThenShouldReturnWithRdsConfig() throws JsonProcessingException {
        RDSConfig rdsConfig = TestUtil.rdsConfig(RdsType.RANGER);

        // TODO we should somehow handle this
        //Map<String, String> attributes = new HashMap<>();
        //attributes.put("rangerAdminPassword", "rangerAdminPassword");
        //rdsConfig.setAttributes(new Json(attributes));

        return new BlueprintTemplateModelContextBuilder()
                .withRdsConfigs(Sets.newHashSet(rdsConfig))
                .build();
    }

    private String compileTemplate(String sourceFile, Map<String, Object> model) {
        String result;
        try {
            Template template = handlebars.compileInline(readSourceTemplate(sourceFile), "{{{", "}}}");
            result = template.apply(model);
        } catch (IOException e) {
            result = "";
        }
        return result;
    }

    private String readExpectedTemplate(String file) throws IOException {
        return readFileFromClasspath(String.format("handlebar/%s", file));
    }

    private String readSourceTemplate(String file) throws IOException {
        return readFileFromClasspath(String.format("%s", file));
    }

}
