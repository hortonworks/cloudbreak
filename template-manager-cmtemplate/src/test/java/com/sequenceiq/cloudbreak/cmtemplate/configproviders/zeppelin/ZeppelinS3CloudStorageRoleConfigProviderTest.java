package com.sequenceiq.cloudbreak.cmtemplate.configproviders.zeppelin;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.common.type.InstanceGroupType;
import com.sequenceiq.cloudbreak.common.type.filesystem.AdlsFileSystem;
import com.sequenceiq.cloudbreak.common.type.filesystem.S3FileSystem;
import com.sequenceiq.cloudbreak.domain.StorageLocation;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject.Builder;
import com.sequenceiq.cloudbreak.template.filesystem.BaseFileSystemConfigurationsView;
import com.sequenceiq.cloudbreak.template.filesystem.StorageLocationView;
import com.sequenceiq.cloudbreak.template.filesystem.adls.AdlsFileSystemConfigurationsView;
import com.sequenceiq.cloudbreak.template.filesystem.s3.S3FileSystemConfigurationsView;
import com.sequenceiq.cloudbreak.template.model.GeneralClusterConfigs;
import com.sequenceiq.cloudbreak.template.views.HostgroupView;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;

@RunWith(MockitoJUnitRunner.class)
public class ZeppelinS3CloudStorageRoleConfigProviderTest {

    private final ZeppelinS3CloudStorageRoleConfigProvider underTest = new ZeppelinS3CloudStorageRoleConfigProvider();

    @Test
    public void testGetZeppelinStorageRoleConfigsWithVariousBucketValues() {
        assertZeppelinStorageValues("s3a://testbucket/test-cluster/zep-pelin",
                "testbucket", "test-cluster/zep-pelin");
        assertZeppelinStorageValues("s3a://testbucket/basepath1/testcluster/zeppelin",
                "testbucket", "basepath1/testcluster/zeppelin");
        assertZeppelinStorageValues("s3a://testbucket/basepath1/basepath2/testcluster/zeppelin",
                "testbucket", "basepath1/basepath2/testcluster/zeppelin");
        assertZeppelinStorageValues("s3a://test-bucket/testcluster/zeppelin",
                "test-bucket", "testcluster/zeppelin");
        assertZeppelinStorageValues("s3a://test.bucket/testcluster/zeppelin",
                "test.bucket", "testcluster/zeppelin");
        assertZeppelinStorageValues("s3a://test-bucket",
                "test-bucket", "");
    }

    protected void assertZeppelinStorageValues(String s3Path, String bucketName, String userPath) {
        TemplatePreparationObject preparationObject = getTemplatePreparationObject(true, false,
                true, s3Path);
        String inputJson = getBlueprintText("input/clouderamanager-ds.bp");
        CmTemplateProcessor cmTemplateProcessor = new CmTemplateProcessor(inputJson);
        Map<String, List<ApiClusterTemplateConfig>> roleConfigs = underTest.getRoleConfigs(cmTemplateProcessor, preparationObject);

        List<ApiClusterTemplateConfig> zeppelinStorageConfigs = roleConfigs.get("zeppelin-ZEPPELIN_SERVER-BASE");
        assertEquals(2, zeppelinStorageConfigs.size());

        assertEquals("zeppelin-conf/zeppelin-site.xml_role_safety_valve", zeppelinStorageConfigs.get(0).getName());
        String expected = "<property><name>zeppelin.notebook.s3.bucket</name><value>" + bucketName + "</value></property>"
                + "<property><name>zeppelin.notebook.s3.user</name><value>" + userPath + "</value></property>";
        assertEquals(expected, zeppelinStorageConfigs.get(0).getValue());

        assertEquals("org.apache.zeppelin.notebook.repo.S3NotebookRepo", zeppelinStorageConfigs.get(1).getValue());
    }

    @Test
    public void testIsConfigurationNeededWhenS3FileSystem() {
        TemplatePreparationObject preparationObject = getTemplatePreparationObject(true, true, true, "");
        String inputJson = getBlueprintText("input/clouderamanager-ds.bp");
        CmTemplateProcessor cmTemplateProcessor = new CmTemplateProcessor(inputJson);
        boolean configurationNeeded = underTest.isConfigurationNeeded(cmTemplateProcessor, preparationObject);
        assertTrue(configurationNeeded);
    }

    @Test
    public void testIsConfigurationNeededWhenNotS3FileSystem() {
        TemplatePreparationObject preparationObject = getTemplatePreparationObject(true, true, false, "");
        String inputJson = getBlueprintText("input/clouderamanager-ds.bp");
        CmTemplateProcessor cmTemplateProcessor = new CmTemplateProcessor(inputJson);
        boolean configurationNeeded = underTest.isConfigurationNeeded(cmTemplateProcessor, preparationObject);
        assertFalse(configurationNeeded);
    }

    private TemplatePreparationObject getTemplatePreparationObject(boolean includeBucket, boolean includeUser,
            boolean useS3FileSystem, String bucketLocation) {
        HostgroupView master = new HostgroupView("master", 1, InstanceGroupType.GATEWAY, 1);
        HostgroupView worker = new HostgroupView("worker", 2, InstanceGroupType.CORE, 2);

        List<StorageLocationView> locations = new ArrayList<>();

        if (includeBucket) {
            locations.add(new StorageLocationView(getZeppelinS3Bucket(bucketLocation)));
        }

        BaseFileSystemConfigurationsView fileSystemConfigurationsView;
        if (useS3FileSystem) {
            fileSystemConfigurationsView =
                    new S3FileSystemConfigurationsView(new S3FileSystem(), locations, false);
        } else {
            fileSystemConfigurationsView =
                    new AdlsFileSystemConfigurationsView(new AdlsFileSystem(), locations, false);
        }

        GeneralClusterConfigs clusterConfigs = new GeneralClusterConfigs();
        clusterConfigs.setClusterName("zeppelincluster");
        return Builder.builder().withGeneralClusterConfigs(clusterConfigs).withFileSystemConfigurationView(fileSystemConfigurationsView)
                .withHostgroupViews(Set.of(master, worker)).build();
    }

    protected StorageLocation getZeppelinS3Bucket(String bucketValue) {
        StorageLocation zeppelinS3Bucket = new StorageLocation();
        zeppelinS3Bucket.setProperty("zeppelin.notebook.s3.bucket");
        zeppelinS3Bucket.setValue(bucketValue);
        return zeppelinS3Bucket;
    }

    private String getBlueprintText(String path) {
        return FileReaderUtils.readFileFromClasspathQuietly(path);
    }
}
