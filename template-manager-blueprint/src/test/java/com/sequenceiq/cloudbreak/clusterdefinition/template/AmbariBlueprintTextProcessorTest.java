package com.sequenceiq.cloudbreak.clusterdefinition.template;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Lists;
import com.sequenceiq.cloudbreak.template.processor.configuration.ClusterDefinitionConfigurationEntry;
import com.sequenceiq.cloudbreak.template.ClusterDefinitionProcessingException;
import com.sequenceiq.cloudbreak.clusterdefinition.AmbariBlueprintProcessorFactory;
import com.sequenceiq.cloudbreak.template.processor.AmbariBlueprintTextProcessor;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;
import com.sequenceiq.cloudbreak.util.JsonUtil;

public class AmbariBlueprintTextProcessorTest {

    private static final String HOST_GROUPS_NODE = "host_groups";

    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    private final List<String[]> rangerConfigurations = Lists.newArrayList(
            new String[]{AmbariBlueprintTextProcessor.CONFIGURATIONS_NODE, "admin-properties", "properties", "db_host"},
            new String[]{AmbariBlueprintTextProcessor.CONFIGURATIONS_NODE, "admin-properties", "properties", "db_user"},
            new String[]{AmbariBlueprintTextProcessor.CONFIGURATIONS_NODE, "admin-properties", "properties", "db_password"},
            new String[]{AmbariBlueprintTextProcessor.CONFIGURATIONS_NODE, "admin-properties", "properties", "db_name"});

    private final List<String[]> hivaConfigurations = Lists.newArrayList(
            new String[]{AmbariBlueprintTextProcessor.CONFIGURATIONS_NODE, "hive-site", "javax.jdo.option.ConnectionURL"},
            new String[]{AmbariBlueprintTextProcessor.CONFIGURATIONS_NODE, "hive-site", "javax.jdo.option.ConnectionDriverName"},
            new String[]{AmbariBlueprintTextProcessor.CONFIGURATIONS_NODE, "hive-site", "javax.jdo.option.ConnectionUserName"},
            new String[]{AmbariBlueprintTextProcessor.CONFIGURATIONS_NODE, "hive-site", "javax.jdo.option.ConnectionPassword"});

    private final AmbariBlueprintProcessorFactory underTest = new AmbariBlueprintProcessorFactory();

    @Test
    public void testAddConfigEntriesAddsRootConfigurationsNodeIfMissing() throws Exception {
        String testBlueprint = FileReaderUtils.readFileFromClasspath("blueprints-jackson/test-bp-without-config-block.bp");
        List<ClusterDefinitionConfigurationEntry> configurationEntries = new ArrayList<>();
        String result = underTest.get(testBlueprint).addConfigEntries(configurationEntries, true).asText();

        JsonNode configNode = JsonUtil.readTree(result).path("configurations");
        assertFalse(configNode.isMissingNode());
    }

    @Test
    public void testModifyStackVersionWithThreeTag() throws Exception {
        String testBlueprint = FileReaderUtils.readFileFromClasspath("blueprints-jackson/test-bp-without-config-block.bp");
        String result = underTest.get(testBlueprint).modifyHdpVersion("2.2.4.4").asText();

        JsonNode configNode = JsonUtil.readTree(result).path("Blueprints");
        String stackVersion = configNode.get("stack_version").asText();
        assertEquals("2.2", stackVersion);
    }

    @Test
    public void testModifyStackVersionWithTwoTag() throws Exception {
        String testBlueprint = FileReaderUtils.readFileFromClasspath("blueprints-jackson/test-bp-without-config-block.bp");
        String result = underTest.get(testBlueprint).modifyHdpVersion("2.6").asText();

        JsonNode configNode = JsonUtil.readTree(result).path("Blueprints");
        String stackVersion = configNode.get("stack_version").asText();
        assertEquals("2.6", stackVersion);
    }

    @Test
    public void testAddConfigEntriesAddsConfigFileEntryIfMissing() throws Exception {
        String testBlueprint = FileReaderUtils.readFileFromClasspath("blueprints-jackson/test-bp-with-empty-config-block.bp");
        List<ClusterDefinitionConfigurationEntry> configurationEntries = new ArrayList<>();
        configurationEntries.add(new ClusterDefinitionConfigurationEntry("core-site", "fs.AbstractFileSystem.wasb.impl",
                "org.apache.hadoop.fs.azure.Wasb"));
        String result = underTest.get(testBlueprint).addConfigEntries(configurationEntries, true).asText();

        JsonNode coreSiteNode = JsonUtil.readTree(result).findPath("core-site");
        assertFalse(coreSiteNode.isMissingNode());
    }

    @Test
    public void testAddConfigEntriesAddsConfigEntriesToCorrectConfigBlockWithCorrectValues() throws Exception {
        String testBlueprint = FileReaderUtils.readFileFromClasspath("blueprints-jackson/test-bp-with-empty-config-block.bp");
        List<ClusterDefinitionConfigurationEntry> configurationEntries = new ArrayList<>();
        configurationEntries.add(new ClusterDefinitionConfigurationEntry("core-site", "fs.AbstractFileSystem.wasb.impl",
                "org.apache.hadoop.fs.azure.Wasb"));
        configurationEntries.add(new ClusterDefinitionConfigurationEntry("hdfs-site", "dfs.blocksize", "134217728"));
        String result = underTest.get(testBlueprint).addConfigEntries(configurationEntries, true).asText();

        String configValue1 = JsonUtil.readTree(result).findPath("core-site").findPath("fs.AbstractFileSystem.wasb.impl").textValue();
        assertEquals("org.apache.hadoop.fs.azure.Wasb", configValue1);

        String configValue2 = JsonUtil.readTree(result).findPath("hdfs-site").findPath("dfs.blocksize").textValue();
        assertEquals("134217728", configValue2);
    }

    @Test
    public void testAddConfigEntriesAddsConfigEntriesToExistingConfigBlockAndKeepsExistingEntries() throws Exception {
        String testBlueprint = FileReaderUtils.readFileFromClasspath("blueprints-jackson/test-bp-with-core-site.bp");
        List<ClusterDefinitionConfigurationEntry> configurationEntries = new ArrayList<>();
        configurationEntries.add(new ClusterDefinitionConfigurationEntry("core-site", "fs.AbstractFileSystem.wasb.impl",
                "org.apache.hadoop.fs.azure.Wasb"));
        configurationEntries.add(new ClusterDefinitionConfigurationEntry("core-site", "io.serializations",
                "org.apache.hadoop.io.serializer.WritableSerialization"));
        String result = underTest.get(testBlueprint).addConfigEntries(configurationEntries, true).asText();

        String configValue1 = JsonUtil.readTree(result).findPath("core-site").path("fs.AbstractFileSystem.wasb.impl").textValue();
        assertEquals("org.apache.hadoop.fs.azure.Wasb", configValue1);

        String configValue2 = JsonUtil.readTree(result).findPath("core-site").path("io.serializations").textValue();
        assertEquals("org.apache.hadoop.io.serializer.WritableSerialization", configValue2);

        String configValue3 = JsonUtil.readTree(result).findPath("core-site").path("fs.trash.interval").textValue();
        assertEquals("360", configValue3);

        String configValue4 = JsonUtil.readTree(result).findPath("core-site").path("io.file.buffer.size").textValue();
        assertEquals("131072", configValue4);
    }

    @Test
    public void testAddConfigEntriesAddsConfigEntriesToExistingConfigPropertiesBlockAndKeepsExistingEntries() throws Exception {
        String testBlueprint = FileReaderUtils.readFileFromClasspath("blueprints-jackson/test-bp-with-core-site-properties.bp");
        List<ClusterDefinitionConfigurationEntry> configurationEntries = new ArrayList<>();
        configurationEntries.add(new ClusterDefinitionConfigurationEntry("core-site", "fs.AbstractFileSystem.wasb.impl",
                "org.apache.hadoop.fs.azure.Wasb"));
        configurationEntries.add(new ClusterDefinitionConfigurationEntry("core-site", "io.serializations",
                "org.apache.hadoop.io.serializer.WritableSerialization"));
        String result = underTest.get(testBlueprint).addConfigEntries(configurationEntries, true).asText();

        String configValue1 = JsonUtil.readTree(result).findPath("core-site").path("properties").path("fs.AbstractFileSystem.wasb.impl").textValue();
        assertEquals("org.apache.hadoop.fs.azure.Wasb", configValue1);

        String configValue2 = JsonUtil.readTree(result).findPath("core-site").path("properties").path("io.serializations").textValue();
        assertEquals("org.apache.hadoop.io.serializer.WritableSerialization", configValue2);

        String configValue3 = JsonUtil.readTree(result).findPath("core-site").path("properties").path("fs.trash.interval").textValue();
        assertEquals("360", configValue3);

        String configValue4 = JsonUtil.readTree(result).findPath("core-site").path("properties").path("io.file.buffer.size").textValue();
        assertEquals("131072", configValue4);
    }

    @Test
    public void testAddConfigEntriesAddsConfigEntriesToExistingConfigPropertiesBlockAndUpdatesExistingEntries() throws Exception {
        String testBlueprint = FileReaderUtils.readFileFromClasspath("blueprints-jackson/test-bp-with-core-site-properties-defaultfs.bp");
        List<ClusterDefinitionConfigurationEntry> configurationEntries = new ArrayList<>();
        configurationEntries.add(new ClusterDefinitionConfigurationEntry("core-site", "fs.defaultFS",
                "wasb://cloudbreak@dduihoab6jt1jl.cloudapp.net"));
        configurationEntries.add(new ClusterDefinitionConfigurationEntry("core-site", "io.serializations",
                "org.apache.hadoop.io.serializer.WritableSerialization"));
        String result = underTest.get(testBlueprint).addConfigEntries(configurationEntries, true).asText();

        String configValue1 = JsonUtil.readTree(result).findPath("core-site").path("properties").path("fs.defaultFS").textValue();
        assertEquals("wasb://cloudbreak@dduihoab6jt1jl.cloudapp.net", configValue1);

        String configValue2 = JsonUtil.readTree(result).findPath("core-site").path("properties").path("io.serializations").textValue();
        assertEquals("org.apache.hadoop.io.serializer.WritableSerialization", configValue2);

        String configValue3 = JsonUtil.readTree(result).findPath("core-site").path("properties").path("fs.trash.interval").textValue();
        assertEquals("360", configValue3);

        String configValue4 = JsonUtil.readTree(result).findPath("core-site").path("properties").path("io.file.buffer.size").textValue();
        assertEquals("131072", configValue4);
    }

    @Test
    public void testAddConfigEntriesAddsConfigEntriesToExistingConfigPropertiesBlockAndDoesNotUpdateExistingEntriesWhenOverrideIsFalse() throws Exception {
        String testBlueprint = FileReaderUtils.readFileFromClasspath("blueprints-jackson/test-bp-with-core-site-properties-defaultfs.bp");
        List<ClusterDefinitionConfigurationEntry> configurationEntries = new ArrayList<>();
        configurationEntries.add(new ClusterDefinitionConfigurationEntry("core-site", "fs.defaultFS",
                "wasb://cloudbreak@dduihoab6jt1jl.cloudapp.net"));
        configurationEntries.add(new ClusterDefinitionConfigurationEntry("core-site", "io.serializations",
                "org.apache.hadoop.io.serializer.WritableSerialization"));
        String result = underTest.get(testBlueprint).addConfigEntries(configurationEntries, false).asText();

        String configValue1 = JsonUtil.readTree(result).findPath("core-site").path("properties").path("fs.defaultFS").textValue();
        assertEquals("hdfs://%HOSTGROUP::host_group_master_1%:8020", configValue1);

        String configValue2 = JsonUtil.readTree(result).findPath("core-site").path("properties").path("io.serializations").textValue();
        assertEquals("org.apache.hadoop.io.serializer.WritableSerialization", configValue2);

        String configValue3 = JsonUtil.readTree(result).findPath("core-site").path("properties").path("fs.trash.interval").textValue();
        assertEquals("360", configValue3);

        String configValue4 = JsonUtil.readTree(result).findPath("core-site").path("properties").path("io.file.buffer.size").textValue();
        assertEquals("131072", configValue4);
    }

    @Test
    public void testAddConfigEntriesBehavesCorrectlyWhenThereIsNoConfigurationsNode() throws Exception {
        String testBlueprint = FileReaderUtils.readFileFromClasspath("blueprints-jackson/test-bp-without-config-block.bp");
        List<ClusterDefinitionConfigurationEntry> configurationEntries = new ArrayList<>();
        configurationEntries.add(new ClusterDefinitionConfigurationEntry("core-site", "fs.defaultFS",
                "wasb://cloudbreak@dduihoab6jt1jl.cloudapp.net"));
        configurationEntries.add(new ClusterDefinitionConfigurationEntry("core-site", "io.serializations",
                "org.apache.hadoop.io.serializer.WritableSerialization"));
        String result = underTest.get(testBlueprint).addConfigEntries(configurationEntries, false).asText();

        String configValue1 = JsonUtil.readTree(result).findPath("core-site").path("fs.defaultFS").textValue();
        assertEquals("wasb://cloudbreak@dduihoab6jt1jl.cloudapp.net", configValue1);

        String configValue2 = JsonUtil.readTree(result).findPath("core-site").path("io.serializations").textValue();
        assertEquals("org.apache.hadoop.io.serializer.WritableSerialization", configValue2);
    }

    @Test
    public void testRemoveComponentFromBlueprint() throws Exception {
        String testBlueprint = FileReaderUtils.readFileFromClasspath("blueprints-jackson/test-bp-without-config-block.bp");
        String result = underTest.get(testBlueprint).removeComponentFromBlueprint("NAGIOS_SERVER").asText();
        assertFalse(result.contains("NAGIOS_SERVER"));
    }

    @Test
    public void testAddConfigEntriesThrowsExceptionIfBlueprintCannotBeParsed() throws Exception {
        String testBlueprint = FileReaderUtils.readFileFromClasspath("blueprints-jackson/test-bp-invalid.bp");
        List<ClusterDefinitionConfigurationEntry> configurationEntries = new ArrayList<>();
        thrown.expect(ClusterDefinitionProcessingException.class);
        //thrown.expect(JsonParseException.class);
        thrown.expectMessage("Failed to parse blueprint text.");
        underTest.get(testBlueprint).addConfigEntries(configurationEntries, true);
    }

    @Test
    public void testGetServicesInHostgroup() throws Exception {
        String testBlueprint = FileReaderUtils.readFileFromClasspath("blueprints-jackson/test-bp-without-config-block.bp");
        Set<String> result = underTest.get(testBlueprint).getComponentsInHostGroup("slave_1");

        Set<String> expected = new HashSet<>();
        expected.add("DATANODE");
        expected.add("HDFS_CLIENT");
        expected.add("NODEMANAGER");
        expected.add("YARN_CLIENT");
        expected.add("MAPREDUCE2_CLIENT");
        expected.add("ZOOKEEPER_CLIENT");

        assertEquals(expected, result);
    }

    @Test
    public void testGetServicesInHostgroupThrowsExceptionIfBlueprintCannotBeParsed() throws Exception {
        String testBlueprint = FileReaderUtils.readFileFromClasspath("blueprints-jackson/test-bp-invalid.bp");
        thrown.expect(ClusterDefinitionProcessingException.class);
        thrown.expectMessage("Failed to parse blueprint text.");
        underTest.get(testBlueprint).getComponentsInHostGroup("slave_1");
    }

    @Test
    public void testAddComponentToHostgroupsIfComponentIsMissing() throws Exception {
        String testBlueprint = FileReaderUtils.readFileFromClasspath("blueprints-jackson/test-bp-without-config-block.bp");

        String result = underTest.get(testBlueprint).addComponentToHostgroups("HST_SERVER", Collections.singletonList("slave_1")).asText();

        assertTrue(underTest.get(result).isComponentExistsInBlueprint("HST_SERVER"));
    }

    @Test
    public void testAddComponentToHostgroupsShouldAddComponentToEverySpecifiedHostGroupIfComponentIsMissing() throws Exception {
        String testBlueprint = FileReaderUtils.readFileFromClasspath("blueprints-jackson/test-bp-without-config-block.bp");
        String componentToAdd = "HST_AGENT";

        String result = underTest.get(testBlueprint).addComponentToHostgroups(componentToAdd, Arrays.asList("master", "slave_1")).asText();

        Iterator<JsonNode> hostGroups = JsonUtil.readTree(result).path(HOST_GROUPS_NODE).elements();
        while (hostGroups.hasNext()) {
            JsonNode hostGroup = hostGroups.next();
            assertTrue(isComponentExistsInHostgroup(componentToAdd, hostGroup));
        }
    }

    @Test
    public void testAddComponentToHostgroupsShouldNotModifyBlueprintIfComponentExists() throws Exception {
        String testBlueprint = FileReaderUtils.readFileFromClasspath("blueprints-jackson/test-bp-without-config-block.bp");

        String componentToAdd = "NAMENODE";

        String result = underTest.get(testBlueprint).addComponentToHostgroups(componentToAdd, Collections.singletonList("master")).asText();

        assertEquals(testBlueprint.replaceAll("\\s", ""), result);
    }

    @Test
    public void addToBlueprint() throws Exception {
        String testBlueprint = FileReaderUtils.readFileFromClasspath("blueprints-jackson/test-bp-zeppelin-shiro.bp");

        List<ClusterDefinitionConfigurationEntry> configs = new ArrayList<>();
        configs.add(new ClusterDefinitionConfigurationEntry("zeppelin-env", "shiro_ini_content", "changed"));
        String result = underTest.get(testBlueprint).addConfigEntries(configs, false).asText();

        assertEquals(testBlueprint.replaceAll("\\s", ""), result);
    }

    @Test
    public void addSettingsToBlueprintWhenNoSettingsBlock() throws Exception {
        String testBlueprint = FileReaderUtils.readFileFromClasspath("blueprints-jackson/bp-without-settings-array.bp");
        String res = FileReaderUtils.readFileFromClasspath("blueprints-jackson/settings-bp-result.bp");

        List<ClusterDefinitionConfigurationEntry> configs = new ArrayList<>();
        configs.add(new ClusterDefinitionConfigurationEntry("recovery_settings", "recovery_enabled", "true"));
        configs.add(new ClusterDefinitionConfigurationEntry("cluster-env", "recovery_enabled", "true"));
        configs.add(new ClusterDefinitionConfigurationEntry("cluster-env", "recovery_type", "AUTO_START"));
        String result = underTest.get(testBlueprint).addSettingsEntries(configs, false).asText();

        assertEquals(res.replaceAll("\\s", ""), result.replaceAll("\\s", ""));
    }

    @Test
    public void addSettingsToBlueprintWhenSettingsBlockExists() throws Exception {
        String testBlueprint = FileReaderUtils.readFileFromClasspath("blueprints-jackson/bp-with-settings-array.bp");
        String res = FileReaderUtils.readFileFromClasspath("blueprints-jackson/with-settings-bp-result.bp");


        List<ClusterDefinitionConfigurationEntry> configs = new ArrayList<>();
        configs.add(new ClusterDefinitionConfigurationEntry("recovery_settings", "recovery_enabled", "true"));
        configs.add(new ClusterDefinitionConfigurationEntry("cluster-env", "recovery_enabled", "true"));
        configs.add(new ClusterDefinitionConfigurationEntry("cluster-env", "recovery_type", "AUTO_START"));
        String result = underTest.get(testBlueprint).addSettingsEntries(configs, false).asText();

        assertEquals(res.replaceAll("\\s", ""), result.replaceAll("\\s", ""));
    }

    @Test
    public void testPathValueShouldReturnPathValue() throws IOException {
        // GIVEN
        String testBlueprint = FileReaderUtils.readFileFromClasspath("blueprints-jackson/test-bp-with-core-site-properties.bp");
        // WHEN
        Optional<String> value = underTest.get(testBlueprint).pathValue("configurations", "core-site", "properties", "fs.trash.interval");
        //THEN
        assertTrue(value.isPresent());
        assertEquals(value.get(), "360");
    }

    @Test
    public void testPathValueShouldReturnNullWhenPathIsInvalid() throws IOException {
        // GIVEN
        String testBlueprint = FileReaderUtils.readFileFromClasspath("blueprints-jackson/test-bp-with-core-site-properties.bp");
        // WHEN
        Optional<String> value = underTest.get(testBlueprint).pathValue("configuration", "core-site", "properties", "fs.trash.interval");
        //THEN
        assertFalse(value.isPresent());
    }

    @Test
    public void testPathValueShouldReturnNullWhenJsonNodeIsAnArray() throws IOException {
        // GIVEN
        String testBlueprint = FileReaderUtils.readFileFromClasspath("blueprints-jackson/test-bp-with-core-site-properties.bp");
        // WHEN
        Optional<String> value = underTest.get(testBlueprint).pathValue("configuration");
        //THEN
        assertFalse(value.isPresent());
    }

    @Test
    public void testPathValueShouldReturnNullWhenJsonNodeIsAnObject() throws IOException {
        // GIVEN
        String testBlueprint = FileReaderUtils.readFileFromClasspath("blueprints-jackson/test-bp-with-core-site-properties.bp");
        // WHEN
        Optional<String> value = underTest.get(testBlueprint).pathValue("configuration", "core-site");
        //THEN
        assertFalse(value.isPresent());
    }

    public static String skipLine(String originalBlueprint, String toSkipString) {
        String[] split = originalBlueprint.split(System.lineSeparator());
        StringBuilder sb = new StringBuilder();
        for (String line : split) {
            if (!line.contains(toSkipString)) {
                sb.append(line);
            } else if (!line.endsWith(",")) {
                sb.append("\"fixJson\": \"random\"");
            }
        }
        return sb.toString();
    }

    @Test
    public void testRangerDbConfigExists() throws Exception {
        String testBlueprint = FileReaderUtils.readFileFromClasspath("blueprints-jackson/test-bp-existing-ranger-db.bp");

        boolean result = underTest.get(testBlueprint).isAllConfigurationExistsInPathUnderConfigurationNode(rangerConfigurations);
        assertTrue(result);
    }

    @Test
    public void testRangerDbConfigExistsWithoutConfigBlock() throws Exception {
        String testBlueprint = FileReaderUtils.readFileFromClasspath("blueprints-jackson/test-bp-without-config-block.bp");

        boolean result = underTest.get(testBlueprint).isAllConfigurationExistsInPathUnderConfigurationNode(rangerConfigurations);
        assertFalse(result);
    }

    @Test
    public void testRangerDbConfigExistsWithEmptyConfigBlock() throws Exception {
        String testBlueprint = FileReaderUtils.readFileFromClasspath("blueprints-jackson/test-bp-with-empty-config-block.bp");

        boolean result = underTest.get(testBlueprint).isAllConfigurationExistsInPathUnderConfigurationNode(rangerConfigurations);
        assertFalse(result);
    }

    @Test
    public void testHiveDbConfigExists() throws Exception {
        String testBlueprint = FileReaderUtils.readFileFromClasspath("blueprints-jackson/test-bp-existing-hive-db.bp");

        boolean result = underTest.get(testBlueprint).isAllConfigurationExistsInPathUnderConfigurationNode(hivaConfigurations);
        assertTrue(result);
    }

    @Test
    public void testHiveDbConfigExistsWithoutConfigBlock() throws Exception {
        String testBlueprint = FileReaderUtils.readFileFromClasspath("blueprints-jackson/test-bp-without-config-block.bp");

        boolean result = underTest.get(testBlueprint).isAllConfigurationExistsInPathUnderConfigurationNode(hivaConfigurations);
        assertFalse(result);
    }

    @Test
    public void testHiveDbConfigExistsWithEmptyConfigBlock() throws Exception {
        String testBlueprint = FileReaderUtils.readFileFromClasspath("blueprints-jackson/test-bp-with-empty-config-block.bp");

        boolean result = underTest.get(testBlueprint).isAllConfigurationExistsInPathUnderConfigurationNode(hivaConfigurations);
        assertFalse(result);
    }

    @Test
    public void testHiveDbConfigExistsMissingConfig() throws Exception {
        String originalBlueprint = FileReaderUtils.readFileFromClasspath("blueprints-jackson/test-bp-existing-hive-db.bp");
        for (String[] config : hivaConfigurations) {
            String testBlueprint = skipLine(originalBlueprint, config[config.length - 1]);
            boolean result = underTest.get(testBlueprint).isAllConfigurationExistsInPathUnderConfigurationNode(hivaConfigurations);
            assertFalse(result);
        }
    }

    @Test
    public void testGetConfigurationEntries() throws IOException {
        String testBlueprint = FileReaderUtils.readFileFromClasspath("blueprints-jackson/bp-config-properties.bp");
        Map<String, Map<String, String>> result = underTest.get(testBlueprint).getConfigurationEntries();
        assertEquals(11L, result.size());
        assertEquals("true", result.get("hive-site").get("hive.exec.compress.output"));
        assertEquals("0.7", result.get("mapred-site").get("mapreduce.job.reduce.slowstart.completedmaps"));
    }

    private boolean isComponentExistsInHostgroup(String component, JsonNode hostGroupNode) {
        boolean componentExists = false;
        Iterator<JsonNode> components = hostGroupNode.path("components").elements();
        while (components.hasNext()) {
            if (component.equals(components.next().path("name").textValue())) {
                componentExists = true;
                break;
            }
        }
        return componentExists;
    }
}
