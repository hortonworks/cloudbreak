package com.sequenceiq.cloudbreak.cmtemplate.configproviders.flink;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigTestUtil;
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
public class FlinkCloudStorageConfigProviderTest {

    private static final String HISTORY_SERVER_ARCHIVE_FS_DIR_NAME = "historyserver_archive_fs_dir";

    private static final String HISTORY_SERVER_ARCHIVE_FS_DIR_VALUE = "s3a://bucket/flink/applicationHistory";

    private static final String JOBMANAGER_ARCHIVE_FS_DIR_NAME = "jobmanager_archive_fs_dir";

    private static final String JOBMANAGER_ARCHIVE_FS_DIR_VALUE = "s3a://bucket/flink/applicationHistory";

    private static final String STATE_CHECKPOINTS_DIR_NAME = "state_checkpoints_dir";

    private static final String STATE_CHECKPOINTS_DIR_VALUE = "s3a://bucket/flink/checkpoints";

    private static final String STATE_SAVEPOINTS_DIR_NAME = "state_savepoints_dir";

    private static final String STATE_SAVEPOINTS_DIR_VALUE = "s3a://bucket/flink/savepoints";

    private static final String HIGH_AVAILABILITY_STORAGE_DIR_NAME = "high_availability_storage_dir";

    private static final String HIGH_AVAILABILITY_STORAGE_DIR_VALUE = "s3a://bucket/flink/ha";

    private static final String ATLAS_COLLECTION_ENABLED_NAME = "atlas_collection_enabled";

    private static final String ATLAS_COLLECTION_ENABLED_VALUE = "true";

    private final FlinkRoleConfigProvider underTest = new FlinkRoleConfigProvider();

    @Test
    public void testGetFlinkCloudStorageRoleConfigs() {
        TemplatePreparationObject preparationObject = getTemplatePreparationObject(true);
        String inputJson = getFlinkBlueprintText();
        CmTemplateProcessor cmTemplateProcessor = new CmTemplateProcessor(inputJson);

        Map<String, List<ApiClusterTemplateConfig>> roleConfigs = underTest.getRoleConfigs(cmTemplateProcessor, preparationObject);
        List<ApiClusterTemplateConfig> flinkRoleConfigs = roleConfigs.get("flink-FLINK_HISTORY_SERVER-BASE");
        Map<String, String> actual = ConfigTestUtil.getConfigNameToValueMap(flinkRoleConfigs);

        Map<String, String> expectedRoleConfigs = Map.of(
                HISTORY_SERVER_ARCHIVE_FS_DIR_NAME, HISTORY_SERVER_ARCHIVE_FS_DIR_VALUE);

        assertThat(actual).containsExactlyInAnyOrderEntriesOf(expectedRoleConfigs);
    }

    @Test
    public void testGetFlinkServiceConfigs() {
        TemplatePreparationObject preparationObject = getTemplatePreparationObject(true);
        String inputJson = getFlinkBlueprintText();
        CmTemplateProcessor cmTemplateProcessor = new CmTemplateProcessor(inputJson);

        List<ApiClusterTemplateConfig> serviceConfigs = underTest.getServiceConfigs(cmTemplateProcessor, preparationObject);
        Map<String, String> actual = ConfigTestUtil.getConfigNameToValueMap(serviceConfigs);

        Map<String, String> expectedServiceConfigs = Map.of(
                JOBMANAGER_ARCHIVE_FS_DIR_NAME, JOBMANAGER_ARCHIVE_FS_DIR_VALUE,
                STATE_CHECKPOINTS_DIR_NAME, STATE_CHECKPOINTS_DIR_VALUE,
                STATE_SAVEPOINTS_DIR_NAME, STATE_SAVEPOINTS_DIR_VALUE,
                HIGH_AVAILABILITY_STORAGE_DIR_NAME, HIGH_AVAILABILITY_STORAGE_DIR_VALUE,
                ATLAS_COLLECTION_ENABLED_NAME, ATLAS_COLLECTION_ENABLED_VALUE);

        assertThat(actual).containsExactlyInAnyOrderEntriesOf(expectedServiceConfigs);
    }

    @Test
    public void testGetFlinkCloudStorageRoleConfigsWhenNoCloudStorageProvided() {
        TemplatePreparationObject preparationObject = getTemplatePreparationObject(false);
        String inputJson = getFlinkBlueprintText();
        CmTemplateProcessor cmTemplateProcessor = new CmTemplateProcessor(inputJson);

        Map<String, List<ApiClusterTemplateConfig>> roleConfigs = underTest.getRoleConfigs(cmTemplateProcessor, preparationObject);
        List<ApiClusterTemplateConfig> flinkRoleConfigs = roleConfigs.get("flink-FLINK_HISTORY_SERVER-BASE");

        assertThat(flinkRoleConfigs).isEmpty();
    }

    @Test
    public void testGetFlinkServiceConfigsWhenNoCloudStorageProvided() {
        TemplatePreparationObject preparationObject = getTemplatePreparationObject(false);
        String inputJson = getFlinkBlueprintText();
        CmTemplateProcessor cmTemplateProcessor = new CmTemplateProcessor(inputJson);

        List<ApiClusterTemplateConfig> serviceConfigs = underTest.getServiceConfigs(cmTemplateProcessor, preparationObject);
        Map<String, String> actual = ConfigTestUtil.getConfigNameToValueMap(serviceConfigs);

        assertThat(actual).containsOnly(Map.entry(ATLAS_COLLECTION_ENABLED_NAME, ATLAS_COLLECTION_ENABLED_VALUE));
    }

    private TemplatePreparationObject getTemplatePreparationObject(boolean includeLocations) {
        HostgroupView master = new HostgroupView("master", 1, InstanceGroupType.GATEWAY, 1);
        HostgroupView worker = new HostgroupView("worker", 2, InstanceGroupType.CORE, 2);

        List<StorageLocationView> locations = new ArrayList<>();

        if (includeLocations) {
            locations.add(getStorageLocationView(HISTORY_SERVER_ARCHIVE_FS_DIR_NAME, HISTORY_SERVER_ARCHIVE_FS_DIR_VALUE));
            locations.add(getStorageLocationView(JOBMANAGER_ARCHIVE_FS_DIR_NAME, JOBMANAGER_ARCHIVE_FS_DIR_VALUE));
            locations.add(getStorageLocationView(STATE_CHECKPOINTS_DIR_NAME, STATE_CHECKPOINTS_DIR_VALUE));
            locations.add(getStorageLocationView(STATE_SAVEPOINTS_DIR_NAME, STATE_SAVEPOINTS_DIR_VALUE));
            locations.add(getStorageLocationView(HIGH_AVAILABILITY_STORAGE_DIR_NAME, HIGH_AVAILABILITY_STORAGE_DIR_VALUE));
        }

        S3FileSystemConfigurationsView fileSystemConfigurationsView =
                new S3FileSystemConfigurationsView(new S3FileSystem(), locations, false);
        return Builder.builder().withFileSystemConfigurationView(fileSystemConfigurationsView)
                .withHostgroupViews(Set.of(master, worker)).build();
    }

    private StorageLocationView getStorageLocationView(String property, String value) {
        StorageLocation storageLocation = new StorageLocation();
        storageLocation.setProperty(property);
        storageLocation.setValue(value);
        return new StorageLocationView(storageLocation);
    }

    private String getFlinkBlueprintText() {
        return FileReaderUtils.readFileFromClasspathQuietly("input/flink.bp");
    }
}
