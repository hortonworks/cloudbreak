package com.sequenceiq.cloudbreak.service.cluster.flow.blueprint;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;
import com.sequenceiq.cloudbreak.util.JsonUtil;

public class JacksonBlueprintProcessorTest {

    private JacksonBlueprintProcessor underTest = new JacksonBlueprintProcessor();

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

    @Test(expected = BlueprintProcessingException.class)
    public void testAddConfigEntriesThrowsExceptionIfBlueprintCannotBeParsed() throws Exception {
        String testBlueprint = FileReaderUtils.readFileFromClasspath("blueprints/test-bp-invalid.bp");
        List<BlueprintConfigurationEntry> configurationEntries = new ArrayList<>();
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

    @Test(expected = BlueprintProcessingException.class)
    public void testGetServicesInHostgroupThrowsExceptionIfBlueprintCannotBeParsed() throws Exception {
        String testBlueprint = FileReaderUtils.readFileFromClasspath("blueprints/test-bp-invalid.bp");
        underTest.getComponentsInHostGroup(testBlueprint, "slave_1");
    }

}
