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
import com.sequenceiq.common.api.type.InstanceGroupType;
import com.sequenceiq.common.api.filesystem.AdlsFileSystem;
import com.sequenceiq.common.api.filesystem.S3FileSystem;
import com.sequenceiq.cloudbreak.domain.StorageLocation;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject.Builder;
import com.sequenceiq.cloudbreak.template.filesystem.BaseFileSystemConfigurationsView;
import com.sequenceiq.cloudbreak.template.filesystem.StorageLocationView;
import com.sequenceiq.cloudbreak.template.filesystem.adls.AdlsFileSystemConfigurationsView;
import com.sequenceiq.cloudbreak.template.filesystem.s3.S3FileSystemConfigurationsView;
import com.sequenceiq.cloudbreak.template.views.HostgroupView;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;

@RunWith(MockitoJUnitRunner.class)
public class ZeppelinCloudStorageRoleConfigProviderTest {

    private final ZeppelinCloudStorageRoleConfigProvider underTest = new ZeppelinCloudStorageRoleConfigProvider();

    @Test
    public void testGetZeppelinStorageRoleConfigs() {
        TemplatePreparationObject preparationObject = getTemplatePreparationObject(true, true);
        String inputJson = getBlueprintText("input/clouderamanager-ds.bp");
        CmTemplateProcessor cmTemplateProcessor = new CmTemplateProcessor(inputJson);
        Map<String, List<ApiClusterTemplateConfig>> roleConfigs = underTest.getRoleConfigs(cmTemplateProcessor, preparationObject);

        List<ApiClusterTemplateConfig> zeppelinStorageConfigs = roleConfigs.get("zeppelin-ZEPPELIN_SERVER-BASE");
        assertEquals(1, zeppelinStorageConfigs.size());
        assertEquals(getZeppelinNotebookStorage().getValue(), zeppelinStorageConfigs.get(0).getValue());
    }

    @Test
    public void testGetZeppelinStorageRoleConfigsWhenNoLocationConfigured() {
        TemplatePreparationObject preparationObject = getTemplatePreparationObject(false, true);
        String inputJson = getBlueprintText("input/clouderamanager-ds.bp");
        CmTemplateProcessor cmTemplateProcessor = new CmTemplateProcessor(inputJson);
        Map<String, List<ApiClusterTemplateConfig>> roleConfigs = underTest.getRoleConfigs(cmTemplateProcessor, preparationObject);

        List<ApiClusterTemplateConfig> zeppelinStorageConfigs = roleConfigs.get("zeppelin-ZEPPELIN_SERVER-BASE");
        assertEquals(0, zeppelinStorageConfigs.size());
    }

    @Test
    public void testIsConfigurationNeededWhenS3FileSystem() {
        TemplatePreparationObject preparationObject = getTemplatePreparationObject(true, false);
        String inputJson = getBlueprintText("input/clouderamanager-ds.bp");
        CmTemplateProcessor cmTemplateProcessor = new CmTemplateProcessor(inputJson);
        boolean configurationNeeded = underTest.isConfigurationNeeded(cmTemplateProcessor, preparationObject);
        assertFalse(configurationNeeded);
    }

    @Test
    public void testIsConfigurationNeededWhenAzureFileSystem() {
        TemplatePreparationObject preparationObject = getTemplatePreparationObject(true, true);
        String inputJson = getBlueprintText("input/clouderamanager-ds.bp");
        CmTemplateProcessor cmTemplateProcessor = new CmTemplateProcessor(inputJson);
        boolean configurationNeeded = underTest.isConfigurationNeeded(cmTemplateProcessor, preparationObject);
        assertTrue(configurationNeeded);
    }

    private TemplatePreparationObject getTemplatePreparationObject(boolean includeLocation, boolean useAzureFileSystem) {
        HostgroupView master = new HostgroupView("master", 1, InstanceGroupType.GATEWAY, 1);
        HostgroupView worker = new HostgroupView("worker", 2, InstanceGroupType.CORE, 2);

        List<StorageLocationView> locations = new ArrayList<>();

        if (includeLocation) {
            locations.add(new StorageLocationView(getZeppelinNotebookStorage()));
        }

        BaseFileSystemConfigurationsView fileSystemConfigurationsView;
        if (useAzureFileSystem) {
            fileSystemConfigurationsView =
                    new AdlsFileSystemConfigurationsView(new AdlsFileSystem(), locations, false);
        } else {
            fileSystemConfigurationsView =
                    new S3FileSystemConfigurationsView(new S3FileSystem(), locations, false);
        }

        return Builder.builder().withFileSystemConfigurationView(fileSystemConfigurationsView)
                .withHostgroupViews(Set.of(master, worker)).build();
    }

    protected StorageLocation getZeppelinNotebookStorage() {
        StorageLocation zeppelinNotebookDir = new StorageLocation();
        zeppelinNotebookDir.setProperty("zeppelin.notebook.dir");
        zeppelinNotebookDir.setValue("abfs://zeppelin/file");
        return zeppelinNotebookDir;
    }

    private String getBlueprintText(String path) {
        return FileReaderUtils.readFileFromClasspathQuietly(path);
    }
}
