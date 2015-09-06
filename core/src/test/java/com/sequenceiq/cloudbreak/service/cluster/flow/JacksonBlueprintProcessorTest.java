package com.sequenceiq.cloudbreak.service.cluster.flow;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.sequenceiq.cloudbreak.util.FileReaderUtils;

public class JacksonBlueprintProcessorTest {

    private JacksonBlueprintProcessor underTest = new JacksonBlueprintProcessor();

    @Test
    public void testAddConfigEntriesAddsRootConfigurationsNodeIfMissing() throws Exception {
        String testBlueprint = FileReaderUtils.readFileFromClasspath("bp-without-config-block.bp");
        List<BlueprintConfigurationEntry> configurationEntries = new ArrayList<>();
        configurationEntries.add(new BlueprintConfigurationEntry("core-site", "fs.defaultFs", "wasb://alma.cloudapp.net"));
        String result = underTest.addConfigEntries(testBlueprint, configurationEntries);
        // TODO: assert
    }

    @Test
    public void testAddConfigEntriesAddsConfigFileEntryIfMissing() throws Exception {
        String testBlueprint = FileReaderUtils.readFileFromClasspath("multi-node-hdfs-yarn-test.bp");
        List<BlueprintConfigurationEntry> configurationEntries = new ArrayList<>();
        configurationEntries.add(new BlueprintConfigurationEntry("nagios-env", "nagios_contact", "UPDATED"));
        String result = underTest.addConfigEntries(testBlueprint, configurationEntries);
        // TODO: assert
    }

}
