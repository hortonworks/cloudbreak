package com.sequenceiq.cloudbreak.blueprint.filesystem;

import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.sequenceiq.cloudbreak.api.model.ExecutionType;
import com.sequenceiq.cloudbreak.api.model.FileSystemConfiguration;
import com.sequenceiq.cloudbreak.api.model.RecipeType;
import com.sequenceiq.cloudbreak.blueprint.BlueprintConfigurationEntry;
import com.sequenceiq.cloudbreak.common.model.recipe.RecipeScript;
import com.sequenceiq.cloudbreak.domain.Credential;

public class AbstractFileSystemConfiguratorTest {

    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    private final AbstractFileSystemConfiguratorImpl underTest = new AbstractFileSystemConfiguratorImpl();

    @Test
    public void testGetScripts() {
        Credential credential = new Credential();
        credential.setId(0L);
        FileSystemConfiguration fsConfig = new FileSystemConfiguration();
        List<RecipeScript> actual = underTest.getScripts(credential, fsConfig);

        List<RecipeScript> expected = singletonList(new RecipeScript("echo 'newContent'", ExecutionType.ALL_NODES, RecipeType.POST_AMBARI_START));
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testGetScriptsWhenNoReplace() {
        Credential credential = new Credential();
        credential.setId(1L);
        FileSystemConfiguration fsConfig = new FileSystemConfiguration();
        List<RecipeScript> actual = underTest.getScripts(credential, fsConfig);

        List<RecipeScript> expected = singletonList(new RecipeScript("echo '$replace'", ExecutionType.ALL_NODES, RecipeType.POST_AMBARI_START));
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testGetScriptsWhenFileNotFound() {
        Credential credential = new Credential();
        credential.setId(2L);
        FileSystemConfiguration fsConfig = new FileSystemConfiguration();

        thrown.expectMessage("Filesystem configuration scripts cannot be read.");
        thrown.expect(FileSystemConfigException.class);

        underTest.getScripts(credential, fsConfig);
    }

    @Test
    public void testGetDefaultFsProperties() {
        FileSystemConfiguration fsConfig = new FileSystemConfiguration();
        List<BlueprintConfigurationEntry> actual = underTest.getDefaultFsProperties(fsConfig);

        List<BlueprintConfigurationEntry> expected = Arrays.asList(new BlueprintConfigurationEntry("core-site", "fs.defaultFS", "default-fs-value"),
                new BlueprintConfigurationEntry("hbase-site", "hbase.rootdir", "default-fs-value/apps/hbase/data"),
                new BlueprintConfigurationEntry("accumulo-site", "instance.volumes", "default-fs-value/apps/accumulo/data"),
                new BlueprintConfigurationEntry("webhcat-site", "templeton.hive.archive", "default-fs-value/hdp/apps/${hdp.version}/hive/hive.tar.gz"),
                new BlueprintConfigurationEntry("webhcat-site", "templeton.pig.archive", "default-fs-value/hdp/apps/${hdp.version}/pig/pig.tar.gz"),
                new BlueprintConfigurationEntry("webhcat-site", "templeton.sqoop.archive", "default-fs-value/hdp/apps/${hdp.version}/sqoop/sqoop.tar.gz"),
                new BlueprintConfigurationEntry(
                        "webhcat-site", "templeton.streaming.jar", "default-fs-value/hdp/apps/${hdp.version}/mapreduce/hadoop-streaming.jar"),
                new BlueprintConfigurationEntry("oozie-site", "oozie.service.HadoopAccessorService.supported.filesystems", "*"));

        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testCreateResource() {
        FileSystemConfiguration fsConfig = new FileSystemConfiguration();
        Map<String, String> actual = underTest.createResources(fsConfig);

        Assert.assertEquals(emptyMap(), actual);
    }

    @Test
    public void testDelteResource() {
        FileSystemConfiguration fsConfig = new FileSystemConfiguration();
        Map<String, String> actual = underTest.deleteResources(fsConfig);

        Assert.assertEquals(emptyMap(), actual);
    }
}
