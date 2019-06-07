package com.sequenceiq.cloudbreak.cmtemplate.configproviders.spark;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
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
public class SparkOnYarnCloudStorageServiceConfigProviderTest {

    private final SparkOnYarnCloudStorageServiceConfigProvider underTest = new SparkOnYarnCloudStorageServiceConfigProvider();

    @Test
    public void testGetSparkCloudStorageServiceConfigs() {
        TemplatePreparationObject preparationObject = getTemplatePreparationObject(true);
        List<ApiClusterTemplateConfig> serviceConfigs = underTest.getServiceConfigs(preparationObject);

        assertEquals(1, serviceConfigs.size());
        assertEquals("spark-conf/spark-env.sh_service_safety_valve", serviceConfigs.get(0).getName());
        assertEquals("spark.sql.warehouse.dir=s3a://bucket/spark/warehouse", serviceConfigs.get(0).getValue());
    }

    @Test
    public void testGetSparkCloudStorageServiceConfigsWhenNoStorageProvided() {
        TemplatePreparationObject preparationObject = getTemplatePreparationObject(false);

        List<ApiClusterTemplateConfig> serviceConfigs = underTest.getServiceConfigs(preparationObject);
        assertEquals(0, serviceConfigs.size());
    }

    private TemplatePreparationObject getTemplatePreparationObject(boolean includeLocations) {
        HostgroupView master = new HostgroupView("master", 1, InstanceGroupType.GATEWAY, 1);
        HostgroupView worker = new HostgroupView("worker", 2, InstanceGroupType.CORE, 2);

        List<StorageLocationView> locations = new ArrayList();

        if (includeLocations) {
            locations.add(new StorageLocationView(getSparkCloudStorageConfigDir()));
        }
        S3FileSystemConfigurationsView fileSystemConfigurationsView =
                new S3FileSystemConfigurationsView(new S3FileSystem(), locations, false);


        return Builder.builder().withFileSystemConfigurationView(fileSystemConfigurationsView)
                .withHostgroupViews(Set.of(master, worker)).build();
    }

    protected StorageLocation getSparkCloudStorageConfigDir() {
        StorageLocation sparkWarehouseDir = new StorageLocation();
        sparkWarehouseDir.setProperty("spark.sql.warehouse.dir");
        sparkWarehouseDir.setValue("s3a://bucket/spark/warehouse");
        return sparkWarehouseDir;
    }

    private String getBlueprintText(String path) {
        return FileReaderUtils.readFileFromClasspathQuietly(path);
    }
}