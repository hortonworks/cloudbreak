package com.sequenceiq.cloudbreak.service.cluster.flow.blueprint

import java.util.ArrayList
import java.util.Arrays
import java.util.HashSet

import org.junit.Assert
import org.junit.Test

import com.fasterxml.jackson.databind.JsonNode
import com.sequenceiq.cloudbreak.util.FileReaderUtils
import com.sequenceiq.cloudbreak.util.JsonUtil

class JacksonBlueprintProcessorTest {

    private val underTest = JacksonBlueprintProcessor()

    @Test
    @Throws(Exception::class)
    fun testAddConfigEntriesAddsRootConfigurationsNodeIfMissing() {
        val testBlueprint = FileReaderUtils.readFileFromClasspath("blueprints/test-bp-without-config-block.bp")
        val configurationEntries = ArrayList<BlueprintConfigurationEntry>()
        val result = underTest.addConfigEntries(testBlueprint, configurationEntries, true)

        val configNode = JsonUtil.readTree(result).path("configurations")
        Assert.assertFalse(configNode.isMissingNode)
    }

    @Test
    @Throws(Exception::class)
    fun testModifyStackVersionWithThreeTag() {
        val testBlueprint = FileReaderUtils.readFileFromClasspath("blueprints/test-bp-without-config-block.bp")
        val result = underTest.modifyHdpVersion(testBlueprint, "2.2.4.4")

        val configNode = JsonUtil.readTree(result).path("Blueprints")
        val stackVersion = configNode.get("stack_version").asText()
        Assert.assertEquals("2.2", stackVersion)
    }

    @Test
    @Throws(Exception::class)
    fun testModifyStackVersionWithTwoTag() {
        val testBlueprint = FileReaderUtils.readFileFromClasspath("blueprints/test-bp-without-config-block.bp")
        val result = underTest.modifyHdpVersion(testBlueprint, "2.6")

        val configNode = JsonUtil.readTree(result).path("Blueprints")
        val stackVersion = configNode.get("stack_version").asText()
        Assert.assertEquals("2.6", stackVersion)
    }

    @Test
    @Throws(Exception::class)
    fun testAddConfigEntriesAddsConfigFileEntryIfMissing() {
        val testBlueprint = FileReaderUtils.readFileFromClasspath("blueprints/test-bp-with-empty-config-block.bp")
        val configurationEntries = ArrayList<BlueprintConfigurationEntry>()
        configurationEntries.add(BlueprintConfigurationEntry("core-site", "fs.AbstractFileSystem.wasb.impl", "org.apache.hadoop.fs.azure.Wasb"))
        val result = underTest.addConfigEntries(testBlueprint, configurationEntries, true)

        val coreSiteNode = JsonUtil.readTree(result).findPath("core-site")
        Assert.assertFalse(coreSiteNode.isMissingNode)
    }

    @Test
    @Throws(Exception::class)
    fun testAddConfigEntriesAddsConfigEntriesToCorrectConfigBlockWithCorrectValues() {
        val testBlueprint = FileReaderUtils.readFileFromClasspath("blueprints/test-bp-with-empty-config-block.bp")
        val configurationEntries = ArrayList<BlueprintConfigurationEntry>()
        configurationEntries.add(BlueprintConfigurationEntry("core-site", "fs.AbstractFileSystem.wasb.impl", "org.apache.hadoop.fs.azure.Wasb"))
        configurationEntries.add(BlueprintConfigurationEntry("hdfs-site", "dfs.blocksize", "134217728"))
        val result = underTest.addConfigEntries(testBlueprint, configurationEntries, true)

        val configValue1 = JsonUtil.readTree(result).findPath("core-site").findPath("fs.AbstractFileSystem.wasb.impl").textValue()
        Assert.assertEquals("org.apache.hadoop.fs.azure.Wasb", configValue1)

        val configValue2 = JsonUtil.readTree(result).findPath("hdfs-site").findPath("dfs.blocksize").textValue()
        Assert.assertEquals("134217728", configValue2)
    }

    @Test
    @Throws(Exception::class)
    fun testAddConfigEntriesAddsConfigEntriesToExistingConfigBlockAndKeepsExistingEntries() {
        val testBlueprint = FileReaderUtils.readFileFromClasspath("blueprints/test-bp-with-core-site.bp")
        val configurationEntries = ArrayList<BlueprintConfigurationEntry>()
        configurationEntries.add(BlueprintConfigurationEntry("core-site", "fs.AbstractFileSystem.wasb.impl", "org.apache.hadoop.fs.azure.Wasb"))
        configurationEntries.add(BlueprintConfigurationEntry("core-site", "io.serializations", "org.apache.hadoop.io.serializer.WritableSerialization"))
        val result = underTest.addConfigEntries(testBlueprint, configurationEntries, true)

        val configValue1 = JsonUtil.readTree(result).findPath("core-site").path("fs.AbstractFileSystem.wasb.impl").textValue()
        Assert.assertEquals("org.apache.hadoop.fs.azure.Wasb", configValue1)

        val configValue2 = JsonUtil.readTree(result).findPath("core-site").path("io.serializations").textValue()
        Assert.assertEquals("org.apache.hadoop.io.serializer.WritableSerialization", configValue2)

        val configValue3 = JsonUtil.readTree(result).findPath("core-site").path("fs.trash.interval").textValue()
        Assert.assertEquals("360", configValue3)

        val configValue4 = JsonUtil.readTree(result).findPath("core-site").path("io.file.buffer.size").textValue()
        Assert.assertEquals("131072", configValue4)
    }

    @Test
    @Throws(Exception::class)
    fun testAddConfigEntriesAddsConfigEntriesToExistingConfigPropertiesBlockAndKeepsExistingEntries() {
        val testBlueprint = FileReaderUtils.readFileFromClasspath("blueprints/test-bp-with-core-site-properties.bp")
        val configurationEntries = ArrayList<BlueprintConfigurationEntry>()
        configurationEntries.add(BlueprintConfigurationEntry("core-site", "fs.AbstractFileSystem.wasb.impl", "org.apache.hadoop.fs.azure.Wasb"))
        configurationEntries.add(BlueprintConfigurationEntry("core-site", "io.serializations", "org.apache.hadoop.io.serializer.WritableSerialization"))
        val result = underTest.addConfigEntries(testBlueprint, configurationEntries, true)

        val configValue1 = JsonUtil.readTree(result).findPath("core-site").path("properties").path("fs.AbstractFileSystem.wasb.impl").textValue()
        Assert.assertEquals("org.apache.hadoop.fs.azure.Wasb", configValue1)

        val configValue2 = JsonUtil.readTree(result).findPath("core-site").path("properties").path("io.serializations").textValue()
        Assert.assertEquals("org.apache.hadoop.io.serializer.WritableSerialization", configValue2)

        val configValue3 = JsonUtil.readTree(result).findPath("core-site").path("properties").path("fs.trash.interval").textValue()
        Assert.assertEquals("360", configValue3)

        val configValue4 = JsonUtil.readTree(result).findPath("core-site").path("properties").path("io.file.buffer.size").textValue()
        Assert.assertEquals("131072", configValue4)
    }

    @Test
    @Throws(Exception::class)
    fun testAddConfigEntriesAddsConfigEntriesToExistingConfigPropertiesBlockAndUpdatesExistingEntries() {
        val testBlueprint = FileReaderUtils.readFileFromClasspath("blueprints/test-bp-with-core-site-properties-defaultfs.bp")
        val configurationEntries = ArrayList<BlueprintConfigurationEntry>()
        configurationEntries.add(BlueprintConfigurationEntry("core-site", "fs.defaultFS", "wasb://cloudbreak@dduihoab6jt1jl.cloudapp.net"))
        configurationEntries.add(BlueprintConfigurationEntry("core-site", "io.serializations", "org.apache.hadoop.io.serializer.WritableSerialization"))
        val result = underTest.addConfigEntries(testBlueprint, configurationEntries, true)

        val configValue1 = JsonUtil.readTree(result).findPath("core-site").path("properties").path("fs.defaultFS").textValue()
        Assert.assertEquals("wasb://cloudbreak@dduihoab6jt1jl.cloudapp.net", configValue1)

        val configValue2 = JsonUtil.readTree(result).findPath("core-site").path("properties").path("io.serializations").textValue()
        Assert.assertEquals("org.apache.hadoop.io.serializer.WritableSerialization", configValue2)

        val configValue3 = JsonUtil.readTree(result).findPath("core-site").path("properties").path("fs.trash.interval").textValue()
        Assert.assertEquals("360", configValue3)

        val configValue4 = JsonUtil.readTree(result).findPath("core-site").path("properties").path("io.file.buffer.size").textValue()
        Assert.assertEquals("131072", configValue4)
    }

    @Test
    @Throws(Exception::class)
    fun testAddConfigEntriesAddsConfigEntriesToExistingConfigPropertiesBlockAndDoesNotUpdateExistingEntriesWhenOverrideIsFalse() {
        val testBlueprint = FileReaderUtils.readFileFromClasspath("blueprints/test-bp-with-core-site-properties-defaultfs.bp")
        val configurationEntries = ArrayList<BlueprintConfigurationEntry>()
        configurationEntries.add(BlueprintConfigurationEntry("core-site", "fs.defaultFS", "wasb://cloudbreak@dduihoab6jt1jl.cloudapp.net"))
        configurationEntries.add(BlueprintConfigurationEntry("core-site", "io.serializations", "org.apache.hadoop.io.serializer.WritableSerialization"))
        val result = underTest.addConfigEntries(testBlueprint, configurationEntries, false)

        val configValue1 = JsonUtil.readTree(result).findPath("core-site").path("properties").path("fs.defaultFS").textValue()
        Assert.assertEquals("hdfs://%HOSTGROUP::host_group_master_1%:8020", configValue1)

        val configValue2 = JsonUtil.readTree(result).findPath("core-site").path("properties").path("io.serializations").textValue()
        Assert.assertEquals("org.apache.hadoop.io.serializer.WritableSerialization", configValue2)

        val configValue3 = JsonUtil.readTree(result).findPath("core-site").path("properties").path("fs.trash.interval").textValue()
        Assert.assertEquals("360", configValue3)

        val configValue4 = JsonUtil.readTree(result).findPath("core-site").path("properties").path("io.file.buffer.size").textValue()
        Assert.assertEquals("131072", configValue4)
    }

    @Test
    @Throws(Exception::class)
    fun testAddConfigEntriesBehavesCorrectlyWhenThereIsNoConfigurationsNode() {
        val testBlueprint = FileReaderUtils.readFileFromClasspath("blueprints/test-bp-without-config-block.bp")
        val configurationEntries = ArrayList<BlueprintConfigurationEntry>()
        configurationEntries.add(BlueprintConfigurationEntry("core-site", "fs.defaultFS", "wasb://cloudbreak@dduihoab6jt1jl.cloudapp.net"))
        configurationEntries.add(BlueprintConfigurationEntry("core-site", "io.serializations", "org.apache.hadoop.io.serializer.WritableSerialization"))
        val result = underTest.addConfigEntries(testBlueprint, configurationEntries, false)

        val configValue1 = JsonUtil.readTree(result).findPath("core-site").path("fs.defaultFS").textValue()
        Assert.assertEquals("wasb://cloudbreak@dduihoab6jt1jl.cloudapp.net", configValue1)

        val configValue2 = JsonUtil.readTree(result).findPath("core-site").path("io.serializations").textValue()
        Assert.assertEquals("org.apache.hadoop.io.serializer.WritableSerialization", configValue2)
    }

    @Test
    @Throws(Exception::class)
    fun testRemoveComponentFromBlueprint() {
        val testBlueprint = FileReaderUtils.readFileFromClasspath("blueprints/test-bp-without-config-block.bp")
        val result = underTest.removeComponentFromBlueprint("NAGIOS_SERVER", testBlueprint)
        Assert.assertFalse(result.contains("NAGIOS_SERVER"))
    }

    @Test(expected = BlueprintProcessingException::class)
    @Throws(Exception::class)
    fun testAddConfigEntriesThrowsExceptionIfBlueprintCannotBeParsed() {
        val testBlueprint = FileReaderUtils.readFileFromClasspath("blueprints/test-bp-invalid.bp")
        val configurationEntries = ArrayList<BlueprintConfigurationEntry>()
        underTest.addConfigEntries(testBlueprint, configurationEntries, true)
    }

    @Test
    @Throws(Exception::class)
    fun testGetServicesInHostgroup() {
        val testBlueprint = FileReaderUtils.readFileFromClasspath("blueprints/test-bp-without-config-block.bp")
        val result = underTest.getComponentsInHostGroup(testBlueprint, "slave_1")

        val expected = HashSet<String>()
        expected.add("DATANODE")
        expected.add("HDFS_CLIENT")
        expected.add("NODEMANAGER")
        expected.add("YARN_CLIENT")
        expected.add("MAPREDUCE2_CLIENT")
        expected.add("ZOOKEEPER_CLIENT")

        Assert.assertEquals(expected, result)
    }

    @Test(expected = BlueprintProcessingException::class)
    @Throws(Exception::class)
    fun testGetServicesInHostgroupThrowsExceptionIfBlueprintCannotBeParsed() {
        val testBlueprint = FileReaderUtils.readFileFromClasspath("blueprints/test-bp-invalid.bp")
        underTest.getComponentsInHostGroup(testBlueprint, "slave_1")
    }

    @Test
    @Throws(Exception::class)
    fun testAddComponentToHostgroupsIfComponentIsMissing() {
        val testBlueprint = FileReaderUtils.readFileFromClasspath("blueprints/test-bp-without-config-block.bp")

        val result = underTest.addComponentToHostgroups("HST_SERVER", Arrays.asList("slave_1"), testBlueprint)

        Assert.assertTrue(underTest.componentExistsInBlueprint("HST_SERVER", result))
    }

    @Test
    @Throws(Exception::class)
    fun testAddComponentToHostgroupsShouldAddComponentToEverySpecifiedHostGroupIfComponentIsMissing() {
        val testBlueprint = FileReaderUtils.readFileFromClasspath("blueprints/test-bp-without-config-block.bp")
        val componentToAdd = "HST_AGENT"

        val result = underTest.addComponentToHostgroups(componentToAdd, Arrays.asList("master", "slave_1"), testBlueprint)

        val hostGroups = JsonUtil.readTree(result).path(HOST_GROUPS_NODE).elements()
        while (hostGroups.hasNext()) {
            val hostGroup = hostGroups.next()
            Assert.assertTrue(componentExistsInHostgroup(componentToAdd, hostGroup))
        }
    }

    @Test
    @Throws(Exception::class)
    fun testAddComponentToHostgroupsShouldNotModifyBlueprintIfComponentExists() {
        val testBlueprint = FileReaderUtils.readFileFromClasspath("blueprints/test-bp-without-config-block.bp")
        val componentToAdd = "NAMENODE"

        val result = underTest.addComponentToHostgroups(componentToAdd, Arrays.asList("master"), testBlueprint)

        Assert.assertEquals(testBlueprint.replace("\\s".toRegex(), ""), result)
    }

    private fun componentExistsInHostgroup(component: String, hostGroupNode: JsonNode): Boolean {
        var componentExists = false
        val components = hostGroupNode.path("components").elements()
        while (components.hasNext()) {
            if (component == components.next().path("name").textValue()) {
                componentExists = true
                break
            }
        }
        return componentExists
    }

    companion object {

        private val HOST_GROUPS_NODE = "host_groups"
    }
}
