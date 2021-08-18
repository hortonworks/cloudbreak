package com.sequenceiq.cloudbreak.cmtemplate.configproviders.impala;

import static com.sequenceiq.cloudbreak.template.TemplatePreparationObject.Builder;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.domain.StorageLocation;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.filesystem.StorageLocationView;
import com.sequenceiq.cloudbreak.template.filesystem.s3.S3FileSystemConfigurationsView;
import com.sequenceiq.cloudbreak.template.views.HostgroupView;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;
import com.sequenceiq.common.api.filesystem.S3FileSystem;
import com.sequenceiq.common.api.type.InstanceGroupType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;


import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@ExtendWith(MockitoExtension.class)
class ImpalaCloudStorageServiceConfigProviderTest {

    private final ImpalaCloudStorageServiceConfigProvider underTest = new ImpalaCloudStorageServiceConfigProvider();

    @Test
    public void testImpalaCloudStorageServiceConfigs() {
        CmTemplateProcessor templateProcessor = new CmTemplateProcessor(getBlueprintText("input/clouderamanager-host-with-uppercase.bp"));
        TemplatePreparationObject templatePreparationObject = getTemplatePreparationObject(true);

        List<ApiClusterTemplateConfig> serviceConfigs = underTest.getServiceConfigs(templateProcessor, templatePreparationObject);

        assertEquals(1, serviceConfigs.size());
        assertEquals("impala_cmd_args_safety_valve", serviceConfigs.get(0).getName());
        assertEquals("--startup_filesystem_check_directories=" +
                "s3a://bucket/warehouse/tablespace/managed/hive," +
                "s3a://bucket/warehouse/tablespace/external/hive", serviceConfigs.get(0).getValue());
    }

    @Test
    public void testImpalaCloudStorageServiceConfigsWithoutStorageConfigured() {
        CmTemplateProcessor templateProcessor = new CmTemplateProcessor(getBlueprintText("input/clouderamanager-host-with-uppercase.bp"));
        TemplatePreparationObject templatePreparationObject = getTemplatePreparationObject(false);

        List<ApiClusterTemplateConfig> serviceConfigs = underTest.getServiceConfigs(templateProcessor, templatePreparationObject);

        assertEquals(0, serviceConfigs.size());
    }

    @Test
    public void testImpalaCloudStorageServiceConfigsWithLowerCdhVersion() {
        CmTemplateProcessor templateProcessor = new CmTemplateProcessor(getBlueprintText("input/cdp-data-mart.bp"));
        TemplatePreparationObject templatePreparationObject = getTemplatePreparationObject(true);

        List<ApiClusterTemplateConfig> serviceConfigs = underTest.getServiceConfigs(templateProcessor, templatePreparationObject);

        assertEquals(0, serviceConfigs.size());
    }

    private TemplatePreparationObject getTemplatePreparationObject(boolean includeLocations) {
        HostgroupView master = new HostgroupView("master", 1, InstanceGroupType.GATEWAY, 1);
        HostgroupView coordinator = new HostgroupView("coordinator", 1, InstanceGroupType.CORE, 1);
        HostgroupView executor = new HostgroupView("executor", 2, InstanceGroupType.CORE, 2);

        List<StorageLocationView> locations = new ArrayList<>();

        if (includeLocations) {
            locations.add(new StorageLocationView(getHiveWarehouseStorageLocation()));
            locations.add(new StorageLocationView(getHiveWarehouseExternalStorageLocation()));
        }
        S3FileSystemConfigurationsView fileSystemConfigurationsView =
                new S3FileSystemConfigurationsView(new S3FileSystem(), locations, false);
        return Builder.builder()
                .withFileSystemConfigurationView(fileSystemConfigurationsView)
                .withHostgroupViews(Set.of(master, coordinator, executor)).build();
    }

    private StorageLocation getHiveWarehouseStorageLocation() {
        StorageLocation managed = new StorageLocation();
        managed.setProperty("hive.metastore.warehouse.dir");
        managed.setValue("s3a://bucket/warehouse/tablespace/managed/hive");
        return managed;
    }

    private StorageLocation getHiveWarehouseExternalStorageLocation() {
        StorageLocation external = new StorageLocation();
        external.setProperty("hive.metastore.warehouse.external.dir");
        external.setValue("s3a://bucket/warehouse/tablespace/external/hive");
        return external;
    }

    private String getBlueprintText(String path) {
        return FileReaderUtils.readFileFromClasspathQuietly(path);
    }
}