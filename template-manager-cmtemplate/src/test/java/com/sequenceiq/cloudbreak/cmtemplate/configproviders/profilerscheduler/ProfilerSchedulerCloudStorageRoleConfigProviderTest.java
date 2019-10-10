package com.sequenceiq.cloudbreak.cmtemplate.configproviders.profilerscheduler;

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
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;

@RunWith(MockitoJUnitRunner.class)
public class ProfilerSchedulerCloudStorageRoleConfigProviderTest {
    private ProfilerSchedulerCloudStorageRoleConfigProvider underTest;

    @Before
    public void setUp() {
        underTest = new ProfilerSchedulerCloudStorageRoleConfigProvider();
    }

    @Test
    public void testProfilerSchedulerCloudStorageRoleConfigs() {
        TemplatePreparationObject preparationObject = getTemplatePreparationObject(true);
        String inputJson = getBlueprintText("input/clouderamanager-profilers.bp");
        CmTemplateProcessor cmTemplateProcessor = new CmTemplateProcessor(inputJson);

        Map<String, List<ApiClusterTemplateConfig>> roleConfigs = underTest.getRoleConfigs(cmTemplateProcessor, preparationObject);

        List<ApiClusterTemplateConfig> serviceConfigs = roleConfigs.get("profiler_scheduler-PROFILER_SCHEDULER_AGENT-BASE");

        assertEquals(1, serviceConfigs.size());
        assertEquals("file_system_uri", serviceConfigs.get(0).getName());
        assertEquals("s3a://bucket/dpprofiler", serviceConfigs.get(0).getValue());
    }

    @Test
    public void testProfilerSchedulerCloudStorageConfigsWhenNoCloudStorageProvided() {
        TemplatePreparationObject preparationObject = getTemplatePreparationObject(false);
        String inputJson = getBlueprintText("input/clouderamanager-profilers.bp");
        CmTemplateProcessor cmTemplateProcessor = new CmTemplateProcessor(inputJson);

        Map<String, List<ApiClusterTemplateConfig>> roleConfigs = underTest.getRoleConfigs(cmTemplateProcessor, preparationObject);
        List<ApiClusterTemplateConfig> profilerSchedulerConfigs = roleConfigs.get("profiler_scheduler-PROFILER_SCHEDULER_AGENT-BASE");

        assertEquals(0, profilerSchedulerConfigs.size());
    }

    private TemplatePreparationObject getTemplatePreparationObject(boolean includeLocations) {
        HostgroupView master = new HostgroupView("master", 1, InstanceGroupType.GATEWAY, 1);
        HostgroupView worker = new HostgroupView("worker", 2, InstanceGroupType.CORE, 2);

        List<StorageLocationView> locations = new ArrayList<>();

        if (includeLocations) {
            locations.add(new StorageLocationView(getProfilerSchedulerFileSystemUri()));
        }

        S3FileSystemConfigurationsView fileSystemConfigurationsView =
                new S3FileSystemConfigurationsView(new S3FileSystem(), locations, false);
        return Builder.builder().withFileSystemConfigurationView(fileSystemConfigurationsView)
                .withHostgroupViews(Set.of(master, worker)).build();
    }

    private StorageLocation getProfilerSchedulerFileSystemUri() {
        StorageLocation profilerSchedulerFileSystemUri = new StorageLocation();
        profilerSchedulerFileSystemUri.setProperty("file_system_uri");
        profilerSchedulerFileSystemUri.setValue("s3a://bucket/dpprofiler");
        return profilerSchedulerFileSystemUri;
    }

    private String getBlueprintText(String path) {
        return FileReaderUtils.readFileFromClasspathQuietly(path);
    }
}