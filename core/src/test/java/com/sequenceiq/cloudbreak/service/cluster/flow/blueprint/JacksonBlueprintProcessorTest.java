package com.sequenceiq.cloudbreak.service.cluster.flow.blueprint;

import static org.hamcrest.Matchers.isA;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonNode;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;
import com.sequenceiq.cloudbreak.util.JsonUtil;

public class JacksonBlueprintProcessorTest {

    private static final String HOST_GROUPS_NODE = "host_groups";

    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    private final JacksonBlueprintProcessor underTest = new JacksonBlueprintProcessor();

    @Test
    public void testAddConfigEntriesAddsRootConfigurationsNodeIfMissing() throws Exception {
        String testBlueprint = FileReaderUtils.readFileFromClasspath("blueprints/test-bp-without-config-block.bp");
        List<BlueprintConfigurationEntry> configurationEntries = new ArrayList<>();
        String result = underTest.addConfigEntries(testBlueprint, configurationEntries, true);

        JsonNode configNode = JsonUtil.readTree(result).path("configurations");
        Assert.assertFalse(configNode.isMissingNode());
    }

    @Test
    public void testModifyStackVersionWithThreeTag() throws Exception {
        String testBlueprint = FileReaderUtils.readFileFromClasspath("blueprints/test-bp-without-config-block.bp");
        String result = underTest.modifyHdpVersion(testBlueprint, "2.2.4.4");

        JsonNode configNode = JsonUtil.readTree(result).path("Blueprints");
        String stackVersion = configNode.get("stack_version").asText();
        Assert.assertEquals("2.2", stackVersion);
    }

    @Test
    public void testModifyStackVersionWithTwoTag() throws Exception {
        String testBlueprint = FileReaderUtils.readFileFromClasspath("blueprints/test-bp-without-config-block.bp");
        String result = underTest.modifyHdpVersion(testBlueprint, "2.6");

        JsonNode configNode = JsonUtil.readTree(result).path("Blueprints");
        String stackVersion = configNode.get("stack_version").asText();
        Assert.assertEquals("2.6", stackVersion);
    }

    @Test
    public void testAddConfigEntriesAddsConfigFileEntryIfMissing() throws Exception {
        String testBlueprint = FileReaderUtils.readFileFromClasspath("blueprints/test-bp-with-empty-config-block.bp");
        List<BlueprintConfigurationEntry> configurationEntries = new ArrayList<>();
        configurationEntries.add(new BlueprintConfigurationEntry("core-site", "fs.AbstractFileSystem.wasb.impl", "org.apache.hadoop.fs.azure.Wasb"));
        String result = underTest.addConfigEntries(testBlueprint, configurationEntries, true);

        JsonNode coreSiteNode = JsonUtil.readTree(result).findPath("core-site");
        Assert.assertFalse(coreSiteNode.isMissingNode());
    }

    @Test
    public void testAddConfigEntriesAddsConfigEntriesToCorrectConfigBlockWithCorrectValues() throws Exception {
        String testBlueprint = FileReaderUtils.readFileFromClasspath("blueprints/test-bp-with-empty-config-block.bp");
        List<BlueprintConfigurationEntry> configurationEntries = new ArrayList<>();
        configurationEntries.add(new BlueprintConfigurationEntry("core-site", "fs.AbstractFileSystem.wasb.impl", "org.apache.hadoop.fs.azure.Wasb"));
        configurationEntries.add(new BlueprintConfigurationEntry("hdfs-site", "dfs.blocksize", "134217728"));
        String result = underTest.addConfigEntries(testBlueprint, configurationEntries, true);

        String configValue1 = JsonUtil.readTree(result).findPath("core-site").findPath("fs.AbstractFileSystem.wasb.impl").textValue();
        Assert.assertEquals("org.apache.hadoop.fs.azure.Wasb", configValue1);

        String configValue2 = JsonUtil.readTree(result).findPath("hdfs-site").findPath("dfs.blocksize").textValue();
        Assert.assertEquals("134217728", configValue2);
    }

    @Test
    public void testAddConfigEntriesAddsConfigEntriesToExistingConfigBlockAndKeepsExistingEntries() throws Exception {
        String testBlueprint = FileReaderUtils.readFileFromClasspath("blueprints/test-bp-with-core-site.bp");
        List<BlueprintConfigurationEntry> configurationEntries = new ArrayList<>();
        configurationEntries.add(new BlueprintConfigurationEntry("core-site", "fs.AbstractFileSystem.wasb.impl", "org.apache.hadoop.fs.azure.Wasb"));
        configurationEntries.add(new BlueprintConfigurationEntry("core-site", "io.serializations", "org.apache.hadoop.io.serializer.WritableSerialization"));
        String result = underTest.addConfigEntries(testBlueprint, configurationEntries, true);

        String configValue1 = JsonUtil.readTree(result).findPath("core-site").path("fs.AbstractFileSystem.wasb.impl").textValue();
        Assert.assertEquals("org.apache.hadoop.fs.azure.Wasb", configValue1);

        String configValue2 = JsonUtil.readTree(result).findPath("core-site").path("io.serializations").textValue();
        Assert.assertEquals("org.apache.hadoop.io.serializer.WritableSerialization", configValue2);

        String configValue3 = JsonUtil.readTree(result).findPath("core-site").path("fs.trash.interval").textValue();
        Assert.assertEquals("360", configValue3);

        String configValue4 = JsonUtil.readTree(result).findPath("core-site").path("io.file.buffer.size").textValue();
        Assert.assertEquals("131072", configValue4);
    }

    @Test
    public void testAddConfigEntriesAddsConfigEntriesToExistingConfigPropertiesBlockAndKeepsExistingEntries() throws Exception {
        String testBlueprint = FileReaderUtils.readFileFromClasspath("blueprints/test-bp-with-core-site-properties.bp");
        List<BlueprintConfigurationEntry> configurationEntries = new ArrayList<>();
        configurationEntries.add(new BlueprintConfigurationEntry("core-site", "fs.AbstractFileSystem.wasb.impl", "org.apache.hadoop.fs.azure.Wasb"));
        configurationEntries.add(new BlueprintConfigurationEntry("core-site", "io.serializations", "org.apache.hadoop.io.serializer.WritableSerialization"));
        String result = underTest.addConfigEntries(testBlueprint, configurationEntries, true);

        String configValue1 = JsonUtil.readTree(result).findPath("core-site").path("properties").path("fs.AbstractFileSystem.wasb.impl").textValue();
        Assert.assertEquals("org.apache.hadoop.fs.azure.Wasb", configValue1);

        String configValue2 = JsonUtil.readTree(result).findPath("core-site").path("properties").path("io.serializations").textValue();
        Assert.assertEquals("org.apache.hadoop.io.serializer.WritableSerialization", configValue2);

        String configValue3 = JsonUtil.readTree(result).findPath("core-site").path("properties").path("fs.trash.interval").textValue();
        Assert.assertEquals("360", configValue3);

        String configValue4 = JsonUtil.readTree(result).findPath("core-site").path("properties").path("io.file.buffer.size").textValue();
        Assert.assertEquals("131072", configValue4);
    }

    @Test
    public void testAddConfigEntriesAddsConfigEntriesToExistingConfigPropertiesBlockAndUpdatesExistingEntries() throws Exception {
        String testBlueprint = FileReaderUtils.readFileFromClasspath("blueprints/test-bp-with-core-site-properties-defaultfs.bp");
        List<BlueprintConfigurationEntry> configurationEntries = new ArrayList<>();
        configurationEntries.add(new BlueprintConfigurationEntry("core-site", "fs.defaultFS", "wasb://cloudbreak@dduihoab6jt1jl.cloudapp.net"));
        configurationEntries.add(new BlueprintConfigurationEntry("core-site", "io.serializations", "org.apache.hadoop.io.serializer.WritableSerialization"));
        String result = underTest.addConfigEntries(testBlueprint, configurationEntries, true);

        String configValue1 = JsonUtil.readTree(result).findPath("core-site").path("properties").path("fs.defaultFS").textValue();
        Assert.assertEquals("wasb://cloudbreak@dduihoab6jt1jl.cloudapp.net", configValue1);

        String configValue2 = JsonUtil.readTree(result).findPath("core-site").path("properties").path("io.serializations").textValue();
        Assert.assertEquals("org.apache.hadoop.io.serializer.WritableSerialization", configValue2);

        String configValue3 = JsonUtil.readTree(result).findPath("core-site").path("properties").path("fs.trash.interval").textValue();
        Assert.assertEquals("360", configValue3);

        String configValue4 = JsonUtil.readTree(result).findPath("core-site").path("properties").path("io.file.buffer.size").textValue();
        Assert.assertEquals("131072", configValue4);
    }

    @Test
    public void testAddConfigEntriesAddsConfigEntriesToExistingConfigPropertiesBlockAndDoesNotUpdateExistingEntriesWhenOverrideIsFalse() throws Exception {
        String testBlueprint = FileReaderUtils.readFileFromClasspath("blueprints/test-bp-with-core-site-properties-defaultfs.bp");
        List<BlueprintConfigurationEntry> configurationEntries = new ArrayList<>();
        configurationEntries.add(new BlueprintConfigurationEntry("core-site", "fs.defaultFS", "wasb://cloudbreak@dduihoab6jt1jl.cloudapp.net"));
        configurationEntries.add(new BlueprintConfigurationEntry("core-site", "io.serializations", "org.apache.hadoop.io.serializer.WritableSerialization"));
        String result = underTest.addConfigEntries(testBlueprint, configurationEntries, false);

        String configValue1 = JsonUtil.readTree(result).findPath("core-site").path("properties").path("fs.defaultFS").textValue();
        Assert.assertEquals("hdfs://%HOSTGROUP::host_group_master_1%:8020", configValue1);

        String configValue2 = JsonUtil.readTree(result).findPath("core-site").path("properties").path("io.serializations").textValue();
        Assert.assertEquals("org.apache.hadoop.io.serializer.WritableSerialization", configValue2);

        String configValue3 = JsonUtil.readTree(result).findPath("core-site").path("properties").path("fs.trash.interval").textValue();
        Assert.assertEquals("360", configValue3);

        String configValue4 = JsonUtil.readTree(result).findPath("core-site").path("properties").path("io.file.buffer.size").textValue();
        Assert.assertEquals("131072", configValue4);
    }

    @Test
    public void testAddConfigEntriesBehavesCorrectlyWhenThereIsNoConfigurationsNode() throws Exception {
        String testBlueprint = FileReaderUtils.readFileFromClasspath("blueprints/test-bp-without-config-block.bp");
        List<BlueprintConfigurationEntry> configurationEntries = new ArrayList<>();
        configurationEntries.add(new BlueprintConfigurationEntry("core-site", "fs.defaultFS", "wasb://cloudbreak@dduihoab6jt1jl.cloudapp.net"));
        configurationEntries.add(new BlueprintConfigurationEntry("core-site", "io.serializations", "org.apache.hadoop.io.serializer.WritableSerialization"));
        String result = underTest.addConfigEntries(testBlueprint, configurationEntries, false);

        String configValue1 = JsonUtil.readTree(result).findPath("core-site").path("fs.defaultFS").textValue();
        Assert.assertEquals("wasb://cloudbreak@dduihoab6jt1jl.cloudapp.net", configValue1);

        String configValue2 = JsonUtil.readTree(result).findPath("core-site").path("io.serializations").textValue();
        Assert.assertEquals("org.apache.hadoop.io.serializer.WritableSerialization", configValue2);
    }

    @Test
    public void testRemoveComponentFromBlueprint() throws Exception {
        String testBlueprint = FileReaderUtils.readFileFromClasspath("blueprints/test-bp-without-config-block.bp");
        String result = underTest.removeComponentFromBlueprint("NAGIOS_SERVER", testBlueprint);
        Assert.assertFalse(result.contains("NAGIOS_SERVER"));
    }

    @Test
    public void testAddConfigEntriesThrowsExceptionIfBlueprintCannotBeParsed() throws Exception {
        String testBlueprint = FileReaderUtils.readFileFromClasspath("blueprints/test-bp-invalid.bp");
        List<BlueprintConfigurationEntry> configurationEntries = new ArrayList<>();
        thrown.expect(BlueprintProcessingException.class);
        thrown.expectCause(isA(JsonParseException.class));
        thrown.expectMessage("Failed to add config entries to original blueprint.");
        underTest.addConfigEntries(testBlueprint, configurationEntries, true);
    }

    @Test
    public void testGetServicesInHostgroup() throws Exception {
        String testBlueprint = FileReaderUtils.readFileFromClasspath("blueprints/test-bp-without-config-block.bp");
        Set<String> result = underTest.getComponentsInHostGroup(testBlueprint, "slave_1");

        Set<String> expected = new HashSet<>();
        expected.add("DATANODE");
        expected.add("HDFS_CLIENT");
        expected.add("NODEMANAGER");
        expected.add("YARN_CLIENT");
        expected.add("MAPREDUCE2_CLIENT");
        expected.add("ZOOKEEPER_CLIENT");

        Assert.assertEquals(expected, result);
    }

    @Test
    public void testGetServicesInHostgroupThrowsExceptionIfBlueprintCannotBeParsed() throws Exception {
        String testBlueprint = FileReaderUtils.readFileFromClasspath("blueprints/test-bp-invalid.bp");
        thrown.expect(BlueprintProcessingException.class);
        thrown.expectMessage("Failed to get components for hostgroup 'slave_1' from blueprint.");
        underTest.getComponentsInHostGroup(testBlueprint, "slave_1");
    }

    @Test
    public void testAddComponentToHostgroupsIfComponentIsMissing() throws Exception {
        String testBlueprint = FileReaderUtils.readFileFromClasspath("blueprints/test-bp-without-config-block.bp");

        String result = underTest.addComponentToHostgroups("HST_SERVER", Collections.singletonList("slave_1"), testBlueprint);

        Assert.assertTrue(underTest.componentExistsInBlueprint("HST_SERVER", result));
    }

    @Test
    public void testAddComponentToHostgroupsShouldAddComponentToEverySpecifiedHostGroupIfComponentIsMissing() throws Exception {
        String testBlueprint = FileReaderUtils.readFileFromClasspath("blueprints/test-bp-without-config-block.bp");
        String componentToAdd = "HST_AGENT";

        String result = underTest.addComponentToHostgroups(componentToAdd, Arrays.asList("master", "slave_1"), testBlueprint);

        Iterator<JsonNode> hostGroups = JsonUtil.readTree(result).path(HOST_GROUPS_NODE).elements();
        while (hostGroups.hasNext()) {
            JsonNode hostGroup = hostGroups.next();
            Assert.assertTrue(componentExistsInHostgroup(componentToAdd, hostGroup));
        }
    }

    @Test
    public void testAddComponentToHostgroupsShouldNotModifyBlueprintIfComponentExists() throws Exception {
        String testBlueprint = FileReaderUtils.readFileFromClasspath("blueprints/test-bp-without-config-block.bp");
        String componentToAdd = "NAMENODE";

        String result = underTest.addComponentToHostgroups(componentToAdd, Collections.singletonList("master"), testBlueprint);

        Assert.assertEquals(testBlueprint.replaceAll("\\s", ""), result);
    }

    @Test
    public void addToBlueprint() throws Exception {
        String testBlueprint = FileReaderUtils.readFileFromClasspath("blueprints/test-bp-zeppelin-shiro.bp");
        List<BlueprintConfigurationEntry> configs = new ArrayList<>();
        configs.add(new BlueprintConfigurationEntry("zeppelin-env", "shiro_ini_content", "changed"));
        String result = underTest.addConfigEntries(testBlueprint, configs, false);

        Assert.assertEquals(testBlueprint.replaceAll("\\s", ""), result);
    }

    @Test
    public void addSettingsToBlueprintWhenNoSettingsBlock() throws Exception {
        String testBlueprint = FileReaderUtils.readFileFromClasspath("blueprints/bp-without-settings-array.bp");
        String res = FileReaderUtils.readFileFromClasspath("blueprints/settings-bp-result.bp");

        List<BlueprintConfigurationEntry> configs = new ArrayList<>();
        configs.add(new BlueprintConfigurationEntry("recovery_settings", "recovery_enabled", "true"));
        configs.add(new BlueprintConfigurationEntry("cluster-env", "recovery_enabled", "true"));
        configs.add(new BlueprintConfigurationEntry("cluster-env", "recovery_type", "AUTO_START"));
        String result = underTest.addSettingsEntries(testBlueprint, configs, false);

        Assert.assertEquals(res.replaceAll("\\s", ""), result.replaceAll("\\s", ""));
    }

    @Test
    public void addSettingsToBlueprintWhenSettingsBlockExists() throws Exception {
        String testBlueprint = FileReaderUtils.readFileFromClasspath("blueprints/bp-with-settings-array.bp");
        String res = FileReaderUtils.readFileFromClasspath("blueprints/with-settings-bp-result.bp");

        List<BlueprintConfigurationEntry> configs = new ArrayList<>();
        configs.add(new BlueprintConfigurationEntry("recovery_settings", "recovery_enabled", "true"));
        configs.add(new BlueprintConfigurationEntry("cluster-env", "recovery_enabled", "true"));
        configs.add(new BlueprintConfigurationEntry("cluster-env", "recovery_type", "AUTO_START"));
        String result = underTest.addSettingsEntries(testBlueprint, configs, false);

        Assert.assertEquals(res.replaceAll("\\s", ""), result.replaceAll("\\s", ""));
    }

    private boolean componentExistsInHostgroup(String component, JsonNode hostGroupNode) {
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
