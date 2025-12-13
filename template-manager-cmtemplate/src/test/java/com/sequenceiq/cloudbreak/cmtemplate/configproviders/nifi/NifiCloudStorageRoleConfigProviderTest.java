package com.sequenceiq.cloudbreak.cmtemplate.configproviders.nifi;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.domain.StorageLocation;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject.Builder;
import com.sequenceiq.cloudbreak.template.filesystem.BaseFileSystemConfigurationsView;
import com.sequenceiq.cloudbreak.template.filesystem.StorageLocationView;
import com.sequenceiq.cloudbreak.template.filesystem.adlsgen2.AdlsGen2FileSystemConfigurationsView;
import com.sequenceiq.cloudbreak.template.filesystem.gcs.GcsFileSystemConfigurationsView;
import com.sequenceiq.cloudbreak.template.filesystem.s3.S3FileSystemConfigurationsView;
import com.sequenceiq.cloudbreak.template.views.HostgroupView;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;
import com.sequenceiq.common.api.filesystem.AdlsGen2FileSystem;
import com.sequenceiq.common.api.filesystem.GcsFileSystem;
import com.sequenceiq.common.api.filesystem.S3FileSystem;
import com.sequenceiq.common.api.type.InstanceGroupType;

@ExtendWith(MockitoExtension.class)
class NifiCloudStorageRoleConfigProviderTest {

    private final NifiCloudStorageRoleConfigProvider underTest = new NifiCloudStorageRoleConfigProvider();

    @Test
    void testGetNifiStorageRoleConfigsForCloud() {
        assertNifiStorageValues("s3a://testbucket/test-cluster/ni-fi");
        assertNifiStorageValues("s3a://testbucket/basepath1/testcluster/nifi");
        assertNifiStorageValues("s3a://test.bucket/testcluster/nifi");
        assertNifiStorageValues("abfs://storage@test.dfs.core.windows.net/path1");
        assertNifiStorageValues("abfs://storage@test.dfs.core.windows.net/path1/path2/");
        assertNifiStorageValues("gs://storagebucket/path1/");
        assertNifiStorageValues("gs://storagebucket/path1/path2/");
    }

    @Test
    void testGetNifiStorageRoleConfigsWhenNoLocationConfigured() {
        TemplatePreparationObject preparationObject = getTemplatePreparationObject(null);
        String inputJson = getBlueprintText("input/clouderamanager-ds.bp");
        CmTemplateProcessor cmTemplateProcessor = new CmTemplateProcessor(inputJson);
        Map<String, List<ApiClusterTemplateConfig>> roleConfigs = underTest.getRoleConfigs(cmTemplateProcessor, preparationObject);

        List<ApiClusterTemplateConfig> nifiStorageConfigs = roleConfigs.get("nifi-NIFI_NODE-BASE");
        assertEquals(0, nifiStorageConfigs.size());
    }

    protected void assertNifiStorageValues(String storagePath) {
        List<StorageLocationView> locations = new ArrayList<>();
        StorageLocation nifiLogDir = new StorageLocation();
        nifiLogDir.setProperty("nifi.log.dir.copy.to.cloud.object.storage");
        nifiLogDir.setValue(storagePath);
        locations.add(new StorageLocationView(nifiLogDir));

        BaseFileSystemConfigurationsView fileSystemConfigurationsView;
        if (storagePath.startsWith("s3a")) {
            fileSystemConfigurationsView =
                    new S3FileSystemConfigurationsView(new S3FileSystem(), locations, false);
        } else if (storagePath.startsWith("gcs")) {
            fileSystemConfigurationsView =
                    new GcsFileSystemConfigurationsView(new GcsFileSystem(), locations, false);
        } else {
            fileSystemConfigurationsView =
                    new AdlsGen2FileSystemConfigurationsView(new AdlsGen2FileSystem(), locations, false);
        }

        TemplatePreparationObject preparationObject = getTemplatePreparationObject(fileSystemConfigurationsView);
        String inputJson = getBlueprintText("input/clouderamanager-ds.bp");
        CmTemplateProcessor cmTemplateProcessor = new CmTemplateProcessor(inputJson);
        Map<String, List<ApiClusterTemplateConfig>> roleConfigs = underTest.getRoleConfigs(cmTemplateProcessor, preparationObject);

        List<ApiClusterTemplateConfig> nifiStorageConfigs = roleConfigs.get("nifi-NIFI_NODE-BASE");
        assertEquals(1, nifiStorageConfigs.size());

        assertEquals("nifi.log.dir.copy.to.cloud.object.storage", nifiStorageConfigs.get(0).getName());
        assertEquals(storagePath, nifiStorageConfigs.get(0).getValue());
    }

    private TemplatePreparationObject getTemplatePreparationObject(BaseFileSystemConfigurationsView fileSystemConfigurationsView) {
        HostgroupView master = new HostgroupView("master", 1, InstanceGroupType.GATEWAY, 1);
        HostgroupView worker = new HostgroupView("worker", 2, InstanceGroupType.CORE, 2);

        return Builder.builder().withFileSystemConfigurationView(fileSystemConfigurationsView)
                .withHostgroupViews(Set.of(master, worker)).build();
    }

    private String getBlueprintText(String path) {
        return FileReaderUtils.readFileFromClasspathQuietly(path);
    }
}
