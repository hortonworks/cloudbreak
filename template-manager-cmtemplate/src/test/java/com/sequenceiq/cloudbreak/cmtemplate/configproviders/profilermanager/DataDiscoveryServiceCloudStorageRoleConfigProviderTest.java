package com.sequenceiq.cloudbreak.cmtemplate.configproviders.profilermanager;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.domain.StorageLocation;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject.Builder;
import com.sequenceiq.cloudbreak.template.filesystem.StorageLocationView;
import com.sequenceiq.cloudbreak.template.filesystem.s3.S3FileSystemConfigurationsView;
import com.sequenceiq.cloudbreak.template.views.HostgroupView;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;
import com.sequenceiq.common.api.filesystem.S3FileSystem;
import com.sequenceiq.common.api.type.InstanceGroupType;

@ExtendWith(MockitoExtension.class)
class DataDiscoveryServiceCloudStorageRoleConfigProviderTest {
    private DataDiscoveryServiceCloudStorageRoleConfigProvider underTest;

    @BeforeEach
    public void setUp() {
        underTest = new DataDiscoveryServiceCloudStorageRoleConfigProvider();
    }

    @Test
    void testDataDiscoveryServiceCloudStorageRoleConfigs() {
        TemplatePreparationObject preparationObject = getTemplatePreparationObject(true);
        String inputJson = getBlueprintText("input/clouderamanager-profilers.bp");
        CmTemplateProcessor cmTemplateProcessor = new CmTemplateProcessor(inputJson);

        Map<String, List<ApiClusterTemplateConfig>> roleConfigs = underTest.getRoleConfigs(cmTemplateProcessor, preparationObject);

        List<ApiClusterTemplateConfig> serviceConfigs = roleConfigs.get("profiler_manager-DATA_DISCOVERY_SERVICE_AGENT-BASE");

        assertEquals(1, serviceConfigs.size());
        assertEquals("file_system_uri", serviceConfigs.get(0).getName());
        assertEquals("s3a://bucket/dpprofiler", serviceConfigs.get(0).getValue());
    }

    @Test
    void testDataDiscoveryServiceCloudStorageConfigsWhenNoCloudStorageProvided() {
        TemplatePreparationObject preparationObject = getTemplatePreparationObject(false);
        String inputJson = getBlueprintText("input/clouderamanager-profilers.bp");
        CmTemplateProcessor cmTemplateProcessor = new CmTemplateProcessor(inputJson);

        Map<String, List<ApiClusterTemplateConfig>> roleConfigs = underTest.getRoleConfigs(cmTemplateProcessor, preparationObject);
        List<ApiClusterTemplateConfig> dataDiscoveryServiceConfigs = roleConfigs.get("profiler_manager-DATA_DISCOVERY_SERVICE_AGENT-BASE");

        assertEquals(0, dataDiscoveryServiceConfigs.size());
    }

    private TemplatePreparationObject getTemplatePreparationObject(boolean includeLocations) {
        HostgroupView master = new HostgroupView("master", 1, InstanceGroupType.GATEWAY, 1);
        HostgroupView worker = new HostgroupView("worker", 2, InstanceGroupType.CORE, 2);

        List<StorageLocationView> locations = new ArrayList<>();

        if (includeLocations) {
            locations.add(new StorageLocationView(getDataDiscoveryServiceFileSystemUri()));
        }

        S3FileSystemConfigurationsView fileSystemConfigurationsView =
                new S3FileSystemConfigurationsView(new S3FileSystem(), locations, false);
        return Builder.builder().withFileSystemConfigurationView(fileSystemConfigurationsView)
                .withHostgroupViews(Set.of(master, worker)).build();
    }

    private StorageLocation getDataDiscoveryServiceFileSystemUri() {
        StorageLocation dataDiscoveryServiceFileSystemUri = new StorageLocation();
        dataDiscoveryServiceFileSystemUri.setProperty("file_system_uri");
        dataDiscoveryServiceFileSystemUri.setValue("s3a://bucket/dpprofiler");
        return dataDiscoveryServiceFileSystemUri;
    }

    private String getBlueprintText(String path) {
        return FileReaderUtils.readFileFromClasspathQuietly(path);
    }
}
