package com.sequenceiq.cloudbreak.clusterdefinition.template;

import java.io.IOException;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ImmutableMap;
import com.sequenceiq.cloudbreak.clusterdefinition.AmbariBlueprintProcessorFactory;
import com.sequenceiq.cloudbreak.template.processor.configuration.HostgroupConfigurations;
import com.sequenceiq.cloudbreak.template.processor.configuration.SiteConfigurations;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;
import com.sequenceiq.cloudbreak.util.JsonUtil;

public class JacksonBlueprintProcessorExtendTest {
    private final AmbariBlueprintProcessorFactory underTest = new AmbariBlueprintProcessorFactory();

    @Test
    public void testExtendBlueprintWithConfiguration() throws IOException {
        String json = FileReaderUtils.readFileFromClasspath("extend-blueprint/blueprint.json");
        String expected = FileReaderUtils.readFileFromClasspath("extend-blueprint/blueprint-config.json");

        Map<String, Map<String, String>> globalConfig = ImmutableMap.of("yarn-site", ImmutableMap.of("property-key", "property-value",
                "yarn.nodemanager.local-dirs", "/mnt/fs1/,/mnt/fs2/"),
                "hdfs-site", ImmutableMap.of("dfs.datanode.data.dir", "/mnt/fs1/,/mnt/fs2/"));
        String result = underTest.get(json).extendBlueprintGlobalConfiguration(SiteConfigurations.fromMap(globalConfig), false).asText();

        JsonNode expectedNode = JsonUtil.readTree(expected);
        JsonNode resultNode = JsonUtil.readTree(result);
        Assert.assertEquals(expectedNode, resultNode);
    }

    @Test
    public void testExtendBlueprintWithExistingConfigurationForced() throws IOException {
        String json = FileReaderUtils.readFileFromClasspath("extend-blueprint/multi-node-hdfs-yarn.json");
        String expected = FileReaderUtils.readFileFromClasspath("extend-blueprint/multi-node-hdfs-yarn-forced-config.json");

        Map<String, Map<String, String>> globalConfig = ImmutableMap.of("yarn-site",
                ImmutableMap.of("property-key", "property-value",
                        "yarn.nodemanager.local-dirs", "apple"),
                "hdfs-site", ImmutableMap.of("dfs.datanode.data.dir", "/mnt/fs1/,/mnt/fs2/"),
                "core-site", ImmutableMap.of("fs.defaultFS", "localhost:9000"));
        String result = underTest.get(json).extendBlueprintGlobalConfiguration(SiteConfigurations.fromMap(globalConfig), true).asText();

        JsonNode expectedNode = JsonUtil.readTree(expected);
        JsonNode resultNode = JsonUtil.readTree(result);
        Assert.assertEquals(expectedNode, resultNode);

    }

    @Test
    public void testExtendBlueprintWithExistingConfigurationNotForced() throws IOException {
        String json = FileReaderUtils.readFileFromClasspath("extend-blueprint/multi-node-hdfs-yarn.json");
        String expected = FileReaderUtils.readFileFromClasspath("extend-blueprint/multi-node-hdfs-yarn-config.json");

        Map<String, Map<String, String>> globalConfig = ImmutableMap.of("yarn-site",
                ImmutableMap.of("property-key", "property-value",
                        "yarn.nodemanager.local-dirs", "apple"),
                "hdfs-site", ImmutableMap.of("dfs.datanode.data.dir", "/mnt/fs1/,/mnt/fs2/"),
                "core-site", ImmutableMap.of("fs.defaultFS", "localhost:9000"));
        String result = underTest.get(json).extendBlueprintGlobalConfiguration(SiteConfigurations.fromMap(globalConfig), false).asText();

        JsonNode expectedNode = JsonUtil.readTree(expected);
        JsonNode resultNode = JsonUtil.readTree(result);
        Assert.assertEquals(expectedNode, resultNode);

    }

    @Test
    public void testExtendBlueprintWithExistingConfigurationAdvancedNotForced() throws IOException {
        String json = FileReaderUtils.readFileFromClasspath("extend-blueprint/hdp-streaming.json");
        String expected = FileReaderUtils.readFileFromClasspath("extend-blueprint/hdp-streaming-modified.json");

        Map<String, Map<String, String>> globalConfig = ImmutableMap.of("falcon-startup.properties",
                ImmutableMap.of("*.falcon.graph.serialize.path", "/hadoopfs/fs1/falcon",
                        "*.falcon.graph.storage.directory", "/hadoopfs/fs1/falcon"),
                "zoo.cfg", ImmutableMap.of("dataDir", "/hadoopfs/fs1/zookeeper"));
        String result = underTest.get(json).extendBlueprintGlobalConfiguration(SiteConfigurations.fromMap(globalConfig), false).asText();

        JsonNode expectedNode = JsonUtil.readTree(expected);
        JsonNode resultNode = JsonUtil.readTree(result);
        Assert.assertEquals(expectedNode, resultNode);
    }

    @Test
    public void testExtendBlueprintWithExistingConfigurationAdvancedForced() throws IOException {
        String json = FileReaderUtils.readFileFromClasspath("extend-blueprint/hdp-streaming.json");
        String expected = FileReaderUtils.readFileFromClasspath("extend-blueprint/hdp-streaming-forced-modified.json");

        Map<String, Map<String, String>> globalConfig = ImmutableMap.of("falcon-startup.properties",
                ImmutableMap.of("*.falcon.graph.serialize.path", "/hadoopfs/fs1/falcon",
                        "*.falcon.graph.storage.directory", "/hadoopfs/fs1/falcon"),
                "zoo.cfg", ImmutableMap.of("dataDir", "/hadoopfs/fs1/zookeeper"));
        String result = underTest.get(json).extendBlueprintGlobalConfiguration(SiteConfigurations.fromMap(globalConfig), true).asText();

        JsonNode expectedNode = JsonUtil.readTree(expected);
        JsonNode resultNode = JsonUtil.readTree(result);
        Assert.assertEquals(expectedNode, resultNode);
    }

    @Test
    public void testExtendBlueprintWithEmptyConfiguration() throws IOException {
        String json = FileReaderUtils.readFileFromClasspath("extend-blueprint/blueprint.json");
        String result = underTest.get(json).extendBlueprintGlobalConfiguration(SiteConfigurations.getEmptyConfiguration(), false).asText();

        JsonNode expectedNode = JsonUtil.readTree(json);
        JsonNode resultNode = JsonUtil.readTree(result);
        Assert.assertEquals(expectedNode, resultNode);
    }

    @Test
    public void testExtendBlueprintWithMasterHeterogenConfigurationNotForced() throws IOException {
        String json = FileReaderUtils.readFileFromClasspath("extend-blueprint/multi-node-hdfs-yarn-heterogen.json");
        String expected = FileReaderUtils.readFileFromClasspath("extend-blueprint/multi-node-hdfs-yarn-heterogen-config.json");

        Map<String, Map<String, Map<String, String>>> hgConfig = ImmutableMap.of("master",
                ImmutableMap.of("yarn-site", ImmutableMap.of("property-key", "property-value",
                        "yarn.nodemanager.local-dirs", "/mnt/fs1/,/mnt/fs2/"),
                        "hdfs-site", ImmutableMap.of("dfs.datanode.data.dir", "/mnt/fs1/,/mnt/fs2/")));
        String result = underTest.get(json).extendBlueprintHostGroupConfiguration(HostgroupConfigurations.fromMap(hgConfig), false).asText();

        JsonNode expectedNode = JsonUtil.readTree(expected);
        JsonNode resultNode = JsonUtil.readTree(result);
        Assert.assertEquals(expectedNode, resultNode);
    }

    @Test
    public void testExtendBlueprintWithMasterHeterogenConfigurationForced() throws IOException {
        String json = FileReaderUtils.readFileFromClasspath("extend-blueprint/multi-node-hdfs-yarn-heterogen.json");
        String expected = FileReaderUtils.readFileFromClasspath("extend-blueprint/multi-node-hdfs-yarn-heterogen-forced-config.json");

        Map<String, Map<String, Map<String, String>>> hgConfig = ImmutableMap.of("master",
                ImmutableMap.of("yarn-site", ImmutableMap.of("property-key", "property-value",
                        "yarn.nodemanager.local-dirs", "/mnt/fs1/,/mnt/fs2/"),
                        "hdfs-site", ImmutableMap.of("dfs.datanode.data.dir", "/mnt/fs1/,/mnt/fs2/")));
        String result = underTest.get(json).extendBlueprintHostGroupConfiguration(HostgroupConfigurations.fromMap(hgConfig), true).asText();

        JsonNode expectedNode = JsonUtil.readTree(expected);
        JsonNode resultNode = JsonUtil.readTree(result);
        Assert.assertEquals(expectedNode, resultNode);
    }

    @Test
    public void testExtendBlueprintWithMasterAndSlaveHeterogenConfigurationNotForced() throws IOException {
        String json = FileReaderUtils.readFileFromClasspath("extend-blueprint/multi-node-hdfs-yarn-heterogen.json");
        String expected = FileReaderUtils.readFileFromClasspath("extend-blueprint/multi-node-hdfs-yarn-heterogen-master-slave-config.json");

        Map<String, Map<String, Map<String, String>>> hgConfig = ImmutableMap.of("master",
                ImmutableMap.of("yarn-site", ImmutableMap.of("property-key", "property-value",
                        "yarn.nodemanager.local-dirs", "/mnt/fs1/,/mnt/fs2/"),
                        "hdfs-site", ImmutableMap.of("dfs.datanode.data.dir", "/mnt/fs1/,/mnt/fs2/")),
                "slave_1",
                ImmutableMap.of("yarn-site", ImmutableMap.of("property-key", "property-value",
                        "yarn.nodemanager.local-dirs", "/mnt/fs1/,/mnt/fs2/"),
                        "hdfs-site", ImmutableMap.of("dfs.datanode.data.dir", "/mnt/fs1/,/mnt/fs2/")));
        String result = underTest.get(json).extendBlueprintHostGroupConfiguration(HostgroupConfigurations.fromMap(hgConfig), false).asText();

        JsonNode expectedNode = JsonUtil.readTree(expected);
        JsonNode resultNode = JsonUtil.readTree(result);
        Assert.assertEquals(expectedNode, resultNode);
    }

    @Test
    public void testExtendBlueprintWithMasterAndSlaveHeterogenConfigurationForced() throws IOException {
        String json = FileReaderUtils.readFileFromClasspath("extend-blueprint/multi-node-hdfs-yarn-heterogen.json");
        String expected = FileReaderUtils.readFileFromClasspath("extend-blueprint/multi-node-hdfs-yarn-heterogen-master-slave-forced-config.json");

        Map<String, Map<String, Map<String, String>>> hgConfig = ImmutableMap.of("master",
                ImmutableMap.of("yarn-site", ImmutableMap.of("property-key", "property-value",
                        "yarn.nodemanager.local-dirs", "/mnt/fs1/,/mnt/fs2/"),
                        "hdfs-site", ImmutableMap.of("dfs.datanode.data.dir", "/mnt/fs1/,/mnt/fs2/")),
                "slave_1",
                ImmutableMap.of("yarn-site", ImmutableMap.of("property-key", "property-value",
                        "yarn.nodemanager.local-dirs", "/mnt/fs1/,/mnt/fs2/"),
                        "hdfs-site", ImmutableMap.of("dfs.datanode.data.dir", "/mnt/fs1/,/mnt/fs2/")));
        String result = underTest.get(json).extendBlueprintHostGroupConfiguration(HostgroupConfigurations.fromMap(hgConfig), true).asText();

        JsonNode expectedNode = JsonUtil.readTree(expected);
        JsonNode resultNode = JsonUtil.readTree(result);
        Assert.assertEquals(expectedNode, resultNode);
    }

    @Test
    public void testExtendBlueprintWithMasterAndSlaveHeterogenConfigurationWithExistingConfigNotForced() throws IOException {
        String json = FileReaderUtils.readFileFromClasspath("extend-blueprint/multi-node-hdfs-yarn-heterogen-with-config.json");
        String expected = FileReaderUtils.readFileFromClasspath("extend-blueprint/multi-node-hdfs-yarn-heterogen-with-config-result.json");

        Map<String, Map<String, Map<String, String>>> hgConfig = ImmutableMap.of("master",
                ImmutableMap.of("yarn-site", ImmutableMap.of("property-key", "property-value",
                        "yarn.nodemanager.local-dirs", "/mnt/fs1/,/mnt/fs2/")));
        String result = underTest.get(json).extendBlueprintHostGroupConfiguration(HostgroupConfigurations.fromMap(hgConfig), false).asText();

        JsonNode expectedNode = JsonUtil.readTree(expected);
        JsonNode resultNode = JsonUtil.readTree(result);
        Assert.assertEquals(expectedNode, resultNode);
    }

    @Test
    public void testExtendBlueprintWithMasterAndSlaveHeterogenConfigurationWithExistingConfigForced() throws IOException {
        String json = FileReaderUtils.readFileFromClasspath("extend-blueprint/multi-node-hdfs-yarn-heterogen-with-config.json");
        String expected = FileReaderUtils.readFileFromClasspath("extend-blueprint/multi-node-hdfs-yarn-heterogen-with-forced-config-result.json");

        Map<String, Map<String, Map<String, String>>> hgConfig = ImmutableMap.of("master",
                ImmutableMap.of("yarn-site", ImmutableMap.of("property-key", "property-value",
                        "yarn.nodemanager.local-dirs", "/mnt/fs1/,/mnt/fs2/")));
        String result = underTest.get(json).extendBlueprintHostGroupConfiguration(HostgroupConfigurations.fromMap(hgConfig), true).asText();

        JsonNode expectedNode = JsonUtil.readTree(expected);
        JsonNode resultNode = JsonUtil.readTree(result);
        Assert.assertEquals(expectedNode, resultNode);
    }

    @Test
    public void testExtendBlueprintGlobalConfigurationWithExistingConfigAndPropertyBlock() throws IOException {
        String json = FileReaderUtils.readFileFromClasspath("extend-blueprint/hdp-blueprint-inner-properties.json");
        String expected = FileReaderUtils.readFileFromClasspath("extend-blueprint/hdp-blueprint-inner-properties-result.json");

        Map<String, Map<String, String>> globalConfig = ImmutableMap.of("hdfs-site",
                ImmutableMap.of("property-key", "property-value", "yarn.nodemanager.local-dirs", "/mnt/fs1/,/mnt/fs2/"));
        String result = underTest.get(json).extendBlueprintGlobalConfiguration(SiteConfigurations.fromMap(globalConfig), false).asText();

        JsonNode expectedNode = JsonUtil.readTree(expected);
        JsonNode resultNode = JsonUtil.readTree(result);
        Assert.assertEquals(expectedNode, resultNode);
    }

    @Test
    public void testExtendBlueprintMasterConfigurationWithExistingConfigAndPropertyBlock() throws IOException {
        String json = FileReaderUtils.readFileFromClasspath("extend-blueprint/hdp-blueprint-inner-properties2.json");
        String expected = FileReaderUtils.readFileFromClasspath("extend-blueprint/hdp-blueprint-inner-properties2-result.json");

        Map<String, Map<String, Map<String, String>>> hgConfig = ImmutableMap.of("master",
                ImmutableMap.of("hdfs-site",
                        ImmutableMap.of("property-key", "property-value", "yarn.nodemanager.local-dirs", "/mnt/fs1/,/mnt/fs2/"),
                        "kafka-broker",
                        ImmutableMap.of("property-key", "property-value", "yarn.nodemanager.local-dirs", "/mnt/fs1/,/mnt/fs2/")));
        String result = underTest.get(json).extendBlueprintHostGroupConfiguration(HostgroupConfigurations.fromMap(hgConfig), true).asText();

        JsonNode expectedNode = JsonUtil.readTree(expected);
        JsonNode resultNode = JsonUtil.readTree(result);
        Assert.assertEquals(expectedNode, resultNode);
    }

    @Test
    public void testExtendBlueprintWithMasterAndSlaveHeterogenConfigurationWithGlobalConfigAndExistingConfig() throws IOException {
        String json = FileReaderUtils.readFileFromClasspath("extend-blueprint/multi-node-hdfs-yarn-heterogen-with-config.json");
        String expected = FileReaderUtils.readFileFromClasspath("extend-blueprint/multi-node-hdfs-yarn-heterogen-with-multi-config.json");

        Map<String, Map<String, Map<String, String>>> hgConfig = ImmutableMap.of("master",
                ImmutableMap.of("yarn-site", ImmutableMap.of("property-key", "property-value",
                        "yarn.nodemanager.local-dirs", "/mnt/fs1/,/mnt/fs2/")));

        Map<String, Map<String, String>> globalConfig = ImmutableMap.of("yarn-site",
                ImmutableMap.of("property-key", "property-value",
                        "yarn.nodemanager.local-dirs", "apple"),
                "hdfs-site", ImmutableMap.of("dfs.datanode.data.dir", "/mnt/fs1/,/mnt/fs2/"),
                "core-site", ImmutableMap.of("fs.defaultFS", "localhost:9000"));

        String result = underTest.get(json)
                .extendBlueprintHostGroupConfiguration(HostgroupConfigurations.fromMap(hgConfig), false)
                .extendBlueprintGlobalConfiguration(SiteConfigurations.fromMap(globalConfig), false)
                .asText();

        JsonNode expectedNode = JsonUtil.readTree(expected);
        JsonNode resultNode = JsonUtil.readTree(result);
        Assert.assertEquals(expectedNode, resultNode);
    }

    @Test
    public void testAddConfigEntryStringToBlueprint() throws IOException {
        String json = FileReaderUtils.readFileFromClasspath("extend-blueprint/blueprint.json");
        String expected = FileReaderUtils.readFileFromClasspath("extend-blueprint/blueprint-config.json");
        String config = FileReaderUtils.readFileFromClasspath("extend-blueprint/config.json");

        String result = underTest.get(json).addConfigEntryStringToBlueprint(config, false).asText();

        JsonNode expectedNode = JsonUtil.readTree(expected);
        JsonNode resultNode = JsonUtil.readTree(result);
        Assert.assertEquals(expectedNode, resultNode);
    }

    @Test
    public void testAddConfigArrayEntryStringToBlueprint() throws IOException {
        String json = FileReaderUtils.readFileFromClasspath("extend-blueprint/blueprint.json");
        String expected = FileReaderUtils.readFileFromClasspath("extend-blueprint/blueprint-array-config.json");
        String config = FileReaderUtils.readFileFromClasspath("extend-blueprint/config-array.json");

        String result = underTest.get(json).addConfigEntryStringToBlueprint(config, false).asText();

        JsonNode expectedNode = JsonUtil.readTree(expected);
        JsonNode resultNode = JsonUtil.readTree(result);
        Assert.assertEquals(expectedNode, resultNode);
    }

    @Test
    public void testAddSettingsEntryStringToBlueprint() throws IOException {
        String json = FileReaderUtils.readFileFromClasspath("extend-blueprint/blueprint.json");
        String expected = FileReaderUtils.readFileFromClasspath("extend-blueprint/blueprint-settings.json");
        String config = FileReaderUtils.readFileFromClasspath("extend-blueprint/settings.json");

        String result = underTest.get(json).addSettingsEntryStringToBlueprint(config, false).asText();

        JsonNode expectedNode = JsonUtil.readTree(expected);
        JsonNode resultNode = JsonUtil.readTree(result);
        Assert.assertEquals(expectedNode, resultNode);
    }

    @Test
    public void testAddMultipleSettingsEntryStringToBlueprint() throws IOException {
        String json = FileReaderUtils.readFileFromClasspath("extend-blueprint/blueprint.json");
        String expected = FileReaderUtils.readFileFromClasspath("extend-blueprint/blueprint-settings-multiple.json");
        String config = FileReaderUtils.readFileFromClasspath("extend-blueprint/settings-multiple.json");

        String result = underTest.get(json).addSettingsEntryStringToBlueprint(config, false).asText();

        JsonNode expectedNode = JsonUtil.readTree(expected);
        JsonNode resultNode = JsonUtil.readTree(result);
        Assert.assertEquals(expectedNode, resultNode);
    }

    @Test
    public void testAddMultipleSettingsEntryStringToNotEmptyBlueprint() throws IOException {
        String json = FileReaderUtils.readFileFromClasspath("extend-blueprint/blueprint-settings-notempty.json");
        String expected = FileReaderUtils.readFileFromClasspath("extend-blueprint/blueprint-settings-notempty-result.json");
        String config = FileReaderUtils.readFileFromClasspath("extend-blueprint/settings-multiple.json");

        String result = underTest.get(json).addSettingsEntryStringToBlueprint(config, false).asText();

        JsonNode expectedNode = JsonUtil.readTree(expected);
        JsonNode resultNode = JsonUtil.readTree(result);
        Assert.assertEquals(expectedNode, resultNode);
    }

    @Test
    public void testModifySettingsEntryStringToBlueprintForced() throws IOException {
        String json = FileReaderUtils.readFileFromClasspath("extend-blueprint/blueprint-settings-multiple.json");
        String expected = FileReaderUtils.readFileFromClasspath("extend-blueprint/blueprint-settings-modify-forced.json");
        String config = FileReaderUtils.readFileFromClasspath("extend-blueprint/settings-modify.json");

        String result = underTest.get(json).addSettingsEntryStringToBlueprint(config, true).asText();

        JsonNode expectedNode = JsonUtil.readTree(expected);
        JsonNode resultNode = JsonUtil.readTree(result);
        Assert.assertEquals(expectedNode, resultNode);
    }

    @Test
    public void testModifySettingsEntryStringToBlueprintNotForced() throws IOException {
        String json = FileReaderUtils.readFileFromClasspath("extend-blueprint/blueprint-settings-multiple.json");
        String expected = FileReaderUtils.readFileFromClasspath("extend-blueprint/blueprint-settings-modify-not-forced.json");
        String config = FileReaderUtils.readFileFromClasspath("extend-blueprint/settings-modify.json");

        String result = underTest.get(json).addSettingsEntryStringToBlueprint(config, false).asText();

        JsonNode expectedNode = JsonUtil.readTree(expected);
        JsonNode resultNode = JsonUtil.readTree(result);
        Assert.assertEquals(expectedNode, resultNode);
    }

    @Test
    public void testMultipleModifySettingsEntryStringToBlueprintForced() throws IOException {
        String json = FileReaderUtils.readFileFromClasspath("extend-blueprint/blueprint-settings-multiple.json");
        String expected = FileReaderUtils.readFileFromClasspath("extend-blueprint/blueprint-settings-multiple-modify-forced.json");
        String config = FileReaderUtils.readFileFromClasspath("extend-blueprint/settings-modify-multiple.json");

        String result = underTest.get(json).addSettingsEntryStringToBlueprint(config, true).asText();

        JsonNode expectedNode = JsonUtil.readTree(expected);
        JsonNode resultNode = JsonUtil.readTree(result);
        Assert.assertEquals(expectedNode, resultNode);
    }

    @Test
    public void testMultipleModifySettingsEntryStringToBlueprintNotForced() throws IOException {
        String json = FileReaderUtils.readFileFromClasspath("extend-blueprint/blueprint-settings-multiple.json");
        String expected = FileReaderUtils.readFileFromClasspath("extend-blueprint/blueprint-settings-multiple-modify-not-forced.json");
        String config = FileReaderUtils.readFileFromClasspath("extend-blueprint/settings-modify-multiple.json");

        String result = underTest.get(json).addSettingsEntryStringToBlueprint(config, false).asText();

        JsonNode expectedNode = JsonUtil.readTree(expected);
        JsonNode resultNode = JsonUtil.readTree(result);
        Assert.assertEquals(expectedNode, resultNode);
    }

    @Test
    public void testSettingsEntryStringToEmptyBlueprintNotForced() throws IOException {
        String json = FileReaderUtils.readFileFromClasspath("extend-blueprint/blueprint-settings-empty.json");
        String expected = FileReaderUtils.readFileFromClasspath("extend-blueprint/blueprint-settings.json");
        String config = FileReaderUtils.readFileFromClasspath("extend-blueprint/settings.json");

        String result = underTest.get(json).addSettingsEntryStringToBlueprint(config, false).asText();

        JsonNode expectedNode = JsonUtil.readTree(expected);
        JsonNode resultNode = JsonUtil.readTree(result);
        Assert.assertEquals(expectedNode, resultNode);
    }
}
