package com.sequenceiq.cloudbreak.blueprint.template;

import static com.sequenceiq.cloudbreak.util.FileReaderUtils.readFileFromCustomPath;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Template;
import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.TestUtil;
import com.sequenceiq.cloudbreak.api.model.rds.RdsType;

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
    public static Iterable<Object[]> data() {
        return Arrays.asList(new Object[][] {
                { "blueprints/atlas/ldap.handlebars", "blueprints/atlas/atlas-with-ldap.json",
                        ldapConfigWhenLdapPresentedThenShouldReturnWithLdapConfig() },
                { "blueprints/atlas/ldap.handlebars", "blueprints/atlas/atlas-without-ldap.json",
                        withoutLdapConfigWhenLdapNotPresentedThenShouldReturnWithoutLdapConfig() },
                { "blueprints/druid/rds.handlebars", "blueprints/druid/druid-with-rds.json",
                        druidRdsConfigWhenRdsPresentedThenShouldReturnWithRdsConfig() },
                { "blueprints/druid/rds.handlebars", "blueprints/druid/druid-without-rds.json",
                        druidWithoutRdsConfigWhenRdsNotPresentedThenShouldReturnWithoutRdsConfig() },
                { "blueprints/hadoop/ldap.handlebars", "blueprints/hadoop/hadoop-with-ldap.json",
                        ldapConfigWhenLdapPresentedThenShouldReturnWithLdapConfig() },
                { "blueprints/hadoop/ldap.handlebars", "blueprints/hadoop/hadoop-without-ldap.json",
                        withoutLdapConfigWhenLdapNotPresentedThenShouldReturnWithoutLdapConfig() },
                { "blueprints/hadoop/global.handlebars", "blueprints/hadoop/global.json",
                        objectWithoutEverything() },
                { "blueprints/hive/rds.handlebars", "blueprints/hive/hive-with-rds.json",
                        hiveRdsConfigWhenRdsPresentedThenShouldReturnWithRdsConfig() },
                { "blueprints/hive/rds.handlebars", "blueprints/hive/hive-without-rds.json",
                        objectWithoutEverything() },
                { "blueprints/llap/global.handlebars", "blueprints/llap/global.json",
                        llapObjectWhenNodeCountPresented() },
                { "blueprints/nifi/global.handlebars", "blueprints/nifi/global-with-hdf.json",
                        nifiConfigWhenHdfPresentedThenShouldReturnWithNifiConfig() },
                { "blueprints/nifi/global.handlebars", "blueprints/nifi/global-without-hdf.json",
                        nifiConfigWhenHdfNotPresentedThenShouldReturnWithNotNifiConfig() },
        });
    }

    @Test
    public void test() throws IOException {
        String actual = compileTemplate(input, model);
        String expected = readExpectedTemplate(output);
        String message = String.format("expected: %s \nactual: %s \n", expected, actual);

        Assert.assertEquals(message, expected, actual);
    }

    public static Map<String, Object> objectWithoutEverything() {
        return new BlueprintTemplateModelContextBuilder()
                .build();
    }

    public static Map<String, Object> llapObjectWhenNodeCountPresented() {
        return new BlueprintTemplateModelContextBuilder()
                .withLlapNodeCounts(5)
                .build();
    }

    public static Map<String, Object> ldapConfigWhenLdapPresentedThenShouldReturnWithLdapConfig() {
        return new BlueprintTemplateModelContextBuilder()
                .withLdap(TestUtil.ldapConfig())
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

    public static Map<String, Object> nifiConfigWhenHdfPresentedThenShouldReturnWithNifiConfig() {
        return new BlueprintTemplateModelContextBuilder()
                .withClusterAdminPassword("adminPassword")
                .withClusterAdminFirstname("firstname")
                .withNifiTargets("nifigtargets")
                .withStackType("HDF")
                .build();
    }

    public static Map<String, Object> nifiConfigWhenHdfNotPresentedThenShouldReturnWithNotNifiConfig() {
        return new BlueprintTemplateModelContextBuilder()
                .withClusterAdminPassword("adminPassword")
                .withClusterAdminFirstname("firstname")
                .withNifiTargets("nifigtargets")
                .withStackType("HDP")
                .build();
    }

    public static Map<String, Object> hiveRdsConfigWhenRdsPresentedThenShouldReturnWithRdsConfig() {
        return new BlueprintTemplateModelContextBuilder()
                .withRdsConfigs(Sets.newHashSet(TestUtil.rdsConfig(RdsType.HIVE)))
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
        return readFileFromCustomPath(String.format("src/test/resources/handlebar/%s", file));
    }

    private String readSourceTemplate(String file) throws IOException {
        return readFileFromCustomPath(String.format("src/main/resources/%s", file));
    }

}
