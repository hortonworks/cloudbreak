package com.sequenceiq.cloudbreak.blueprint.filesystem.gcs;

import static com.sequenceiq.cloudbreak.api.model.ExecutionType.ALL_NODES;
import static com.sequenceiq.cloudbreak.api.model.ExecutionType.ONE_NODE;
import static com.sequenceiq.cloudbreak.api.model.RecipeType.POST_AMBARI_START;
import static com.sequenceiq.cloudbreak.api.model.RecipeType.POST_CLUSTER_INSTALL;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonMap;

import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.sequenceiq.cloudbreak.api.model.FileSystemType;
import com.sequenceiq.cloudbreak.api.model.GcsFileSystemConfiguration;
import com.sequenceiq.cloudbreak.blueprint.BlueprintConfigurationEntry;
import com.sequenceiq.cloudbreak.blueprint.filesystem.FileSystemScriptConfig;
import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.domain.json.Json;

public class GcsFileSystemConfiguratorTest {

    private final GcsFileSystemConfigurator underTest = new GcsFileSystemConfigurator();

    @Test
    public void testGetDefaultFsValue() {
        GcsFileSystemConfiguration fsConfig = new GcsFileSystemConfiguration();
        fsConfig.setDefaultBucketName("bucket-name");
        String actual = underTest.getDefaultFsValue(fsConfig);

        Assert.assertEquals("gs://bucket-name/", actual);
    }

    @Test
    public void testGetFsProperties() {
        GcsFileSystemConfiguration fsConfig = new GcsFileSystemConfiguration();
        List<BlueprintConfigurationEntry> actual = underTest.getFsProperties(fsConfig, emptyMap());

        List<BlueprintConfigurationEntry> expected = Arrays.asList(
                new BlueprintConfigurationEntry("core-site", "fs.gs.impl", "com.google.cloud.hadoop.fs.gcs.GoogleHadoopFileSystem"),
                new BlueprintConfigurationEntry("core-site",
                        "fs.AbstractFileSystem.gs.impl", "com.google.cloud.hadoop.fs.gcs.GoogleHadoopFS"),
                new BlueprintConfigurationEntry("core-site", "fs.gs.working.dir", "/"),
                new BlueprintConfigurationEntry("core-site", "fs.gs.system.bucket", fsConfig.getDefaultBucketName()),
                new BlueprintConfigurationEntry("core-site", "fs.gs.auth.service.account.enable", "true"),
                new BlueprintConfigurationEntry("core-site", "fs.gs.auth.service.account.keyfile", "/usr/lib/hadoop/lib/gcp.p12"),
                new BlueprintConfigurationEntry("core-site", "fs.gs.auth.service.account.email", fsConfig.getServiceAccountEmail()),
                new BlueprintConfigurationEntry("core-site", "fs.gs.project.id", fsConfig.getProjectId()));

        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testGetScriptConfigs() throws JsonProcessingException {

        Credential credential = new Credential();
        credential.setAttributes(new Json(singletonMap("serviceAccountPrivateKey", "private-key")));

        GcsFileSystemConfiguration notProcessed = null;
        List<FileSystemScriptConfig> actual = underTest.getScriptConfigs(credential, notProcessed);

        List<FileSystemScriptConfig> expected = Arrays.asList(
                new FileSystemScriptConfig("scripts/gcs-p12.sh", POST_AMBARI_START, ALL_NODES, singletonMap("P12KEY", "private-key")),
                new FileSystemScriptConfig("scripts/gcs-connector-local.sh", POST_AMBARI_START, ALL_NODES),
                new FileSystemScriptConfig("scripts/gcs-connector-hdfs.sh", POST_CLUSTER_INSTALL, ONE_NODE));

        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testGetScriptConfigsWhenPrivateKeyNotSet() throws JsonProcessingException {

        Credential credential = new Credential();
        credential.setAttributes(new Json(emptyMap()));

        GcsFileSystemConfiguration notProcessed = null;
        List<FileSystemScriptConfig> actual = underTest.getScriptConfigs(credential, notProcessed);

        List<FileSystemScriptConfig> expected = Arrays.asList(
                new FileSystemScriptConfig("scripts/gcs-p12.sh", POST_AMBARI_START, ALL_NODES, singletonMap("P12KEY", "")),
                new FileSystemScriptConfig("scripts/gcs-connector-local.sh", POST_AMBARI_START, ALL_NODES),
                new FileSystemScriptConfig("scripts/gcs-connector-hdfs.sh", POST_CLUSTER_INSTALL, ONE_NODE));

        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testGetFileSystemType() {
        Assert.assertEquals(FileSystemType.GCS, underTest.getFileSystemType());
    }

}
