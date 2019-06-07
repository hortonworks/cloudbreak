package com.sequenceiq.cloudbreak.cmtemplate.configproviders.yarn;

import static org.junit.Assert.assertEquals;

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
import com.sequenceiq.cloudbreak.common.type.filesystem.S3FileSystem;
import com.sequenceiq.cloudbreak.domain.StorageLocation;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject.Builder;
import com.sequenceiq.cloudbreak.template.filesystem.StorageLocationView;
import com.sequenceiq.cloudbreak.template.filesystem.s3.S3FileSystemConfigurationsView;
import com.sequenceiq.cloudbreak.template.views.HostgroupView;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;

@RunWith(MockitoJUnitRunner.class)
public class YarnCloudStorageRoleConfigProviderTest {

    private final YarnCloudStorageRoleConfigProvider underTest = new YarnCloudStorageRoleConfigProvider();

    @Test
    public void testGetYarnCloudStorageRoleConfigs() {
        TemplatePreparationObject preparationObject = getTemplatePreparationObject(true);
        String inputJson = getBlueprintText("input/clouderamanager.bp");
        CmTemplateProcessor cmTemplateProcessor = new CmTemplateProcessor(inputJson);

        Map<String, List<ApiClusterTemplateConfig>> roleConfigs = underTest.getRoleConfigs(cmTemplateProcessor, preparationObject);

        List<ApiClusterTemplateConfig> yarnStorageConfigs = roleConfigs.get("yarn-NODEMANAGER-BASE");

        assertEquals(1, yarnStorageConfigs.size());
        assertEquals("nodemanager_config_safety_valve", yarnStorageConfigs.get(0).getName());
        String expected = "<property><name>yarn.nodemanager.remote-app-log-dir</name><value>"
                + getYarnRemoteAppLogDir().getValue()
                + "</value></property>";
        assertEquals(expected, yarnStorageConfigs.get(0).getValue());
    }

    @Test
    public void testGetYarnCloudStorageRoleConfigsWhenNoCloudStorageProvided() {
        TemplatePreparationObject preparationObject = getTemplatePreparationObject(false);
        String inputJson = getBlueprintText("input/clouderamanager.bp");
        CmTemplateProcessor cmTemplateProcessor = new CmTemplateProcessor(inputJson);

        Map<String, List<ApiClusterTemplateConfig>> roleConfigs = underTest.getRoleConfigs(cmTemplateProcessor, preparationObject);
        List<ApiClusterTemplateConfig> yarnStorageConfigs = roleConfigs.get("yarn-NODEMANAGER-BASE");

        assertEquals(0, yarnStorageConfigs.size());
    }

    private TemplatePreparationObject getTemplatePreparationObject(boolean includeLocations) {
        HostgroupView master = new HostgroupView("master", 1, InstanceGroupType.GATEWAY, 1);
        HostgroupView worker = new HostgroupView("worker", 2, InstanceGroupType.CORE, 2);

        List<StorageLocationView> locations = new ArrayList<>();

        if (includeLocations) {
            locations.add(new StorageLocationView(getYarnRemoteAppLogDir()));
        }

        S3FileSystemConfigurationsView fileSystemConfigurationsView =
                new S3FileSystemConfigurationsView(new S3FileSystem(), locations, false);
        return Builder.builder().withFileSystemConfigurationView(fileSystemConfigurationsView)
                .withHostgroupViews(Set.of(master, worker)).build();
    }

    protected StorageLocation getYarnRemoteAppLogDir() {
        StorageLocation yarnRemoteAppLogDir = new StorageLocation();
        yarnRemoteAppLogDir.setProperty("yarn.nodemanager.remote-app-log-dir");
        yarnRemoteAppLogDir.setValue("s3a://bucket/yarn/logs");
        return yarnRemoteAppLogDir;
    }

    private String getBlueprintText(String path) {
        return FileReaderUtils.readFileFromClasspathQuietly(path);
    }
}
