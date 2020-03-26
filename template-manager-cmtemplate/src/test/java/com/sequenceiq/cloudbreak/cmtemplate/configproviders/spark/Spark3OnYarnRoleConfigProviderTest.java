package com.sequenceiq.cloudbreak.cmtemplate.configproviders.spark;

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
import com.sequenceiq.cloudbreak.domain.StorageLocation;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject.Builder;
import com.sequenceiq.cloudbreak.template.filesystem.StorageLocationView;
import com.sequenceiq.cloudbreak.template.filesystem.s3.S3FileSystemConfigurationsView;
import com.sequenceiq.cloudbreak.template.views.HostgroupView;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;
import com.sequenceiq.common.api.filesystem.S3FileSystem;
import com.sequenceiq.common.api.type.InstanceGroupType;

@RunWith(MockitoJUnitRunner.class)
public class Spark3OnYarnRoleConfigProviderTest {

    private final Spark3OnYarnRoleConfigProvider underTest = new Spark3OnYarnRoleConfigProvider();

    @Test
    public void testGetSpark3OnYarnRoleConfigs() {
        validateClientConfig("s3a://bucket/hive/warehouse/external",
                "s3a://bucket");
        validateClientConfig("s3a://bucket/hive/warehouse/external/",
                "s3a://bucket");
        validateClientConfig("abfs://storage@test.dfs.core.windows.net/path1/path2/",
                "abfs://storage@test.dfs.core.windows.net");
        validateClientConfig("adl://storage.azuredatalakestore.net/path1/path2/",
                "adl://storage.azuredatalakestore.net");
        validateClientConfig("gs://storagebucket/test",
                "gs://storagebucket");
    }

    @Test
    public void testGetSpark3OnYarnRoleConfigsWhenNoStorageConfigured() {
        TemplatePreparationObject preparationObject = getTemplatePreparationObject();
        String inputJson = getBlueprintText("input/clouderamanager-ds.bp");
        CmTemplateProcessor cmTemplateProcessor = new CmTemplateProcessor(inputJson);

        Map<String, List<ApiClusterTemplateConfig>> roleConfigs = underTest.getRoleConfigs(cmTemplateProcessor, preparationObject);
        List<ApiClusterTemplateConfig> sparkOnYarnConfigs = roleConfigs.get("spark3_on_yarn-GATEWAY-BASE");
        assertEquals(0, sparkOnYarnConfigs.size());
    }

    protected void validateClientConfig(String hmsExternalDirLocation, String clientConfigDirLocation) {
        TemplatePreparationObject preparationObject = getTemplatePreparationObject(hmsExternalDirLocation);
        String inputJson = getBlueprintText("input/clouderamanager-ds.bp");
        CmTemplateProcessor cmTemplateProcessor = new CmTemplateProcessor(inputJson);

        Map<String, List<ApiClusterTemplateConfig>> roleConfigs = underTest.getRoleConfigs(cmTemplateProcessor, preparationObject);
        List<ApiClusterTemplateConfig> sparkOnYarnConfigs = roleConfigs.get("spark3_on_yarn-GATEWAY-BASE");

        assertEquals(1, sparkOnYarnConfigs.size());
        assertEquals("spark-conf/spark-defaults.conf_client_config_safety_valve", sparkOnYarnConfigs.get(0).getName());
        assertEquals("spark.kerberos.access.hadoopFileSystems=" + clientConfigDirLocation, sparkOnYarnConfigs.get(0).getValue());
    }

    private TemplatePreparationObject getTemplatePreparationObject(String... locations) {
        HostgroupView master = new HostgroupView("master", 1, InstanceGroupType.GATEWAY, 1);
        HostgroupView worker = new HostgroupView("worker", 2, InstanceGroupType.CORE, 2);

        List<StorageLocationView> storageLocations = new ArrayList<>();
        if (locations.length >= 1) {
            StorageLocation hmsExternalWarehouseDir = new StorageLocation();
            hmsExternalWarehouseDir.setProperty("hive.metastore.warehouse.external.dir");
            hmsExternalWarehouseDir.setValue(locations[0]);
            storageLocations.add(new StorageLocationView(hmsExternalWarehouseDir));
        }
        S3FileSystemConfigurationsView fileSystemConfigurationsView =
                new S3FileSystemConfigurationsView(new S3FileSystem(), storageLocations, false);

        return Builder.builder().withFileSystemConfigurationView(fileSystemConfigurationsView)
                .withHostgroupViews(Set.of(master, worker)).build();
    }

    private String getBlueprintText(String path) {
        return FileReaderUtils.readFileFromClasspathQuietly(path);
    }
}
