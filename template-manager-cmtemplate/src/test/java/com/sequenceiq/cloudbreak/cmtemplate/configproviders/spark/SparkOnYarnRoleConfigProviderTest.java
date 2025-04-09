package com.sequenceiq.cloudbreak.cmtemplate.configproviders.spark;

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
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.StorageLocation;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject.Builder;
import com.sequenceiq.cloudbreak.template.filesystem.StorageLocationView;
import com.sequenceiq.cloudbreak.template.filesystem.s3.S3FileSystemConfigurationsView;
import com.sequenceiq.cloudbreak.template.views.BlueprintView;
import com.sequenceiq.cloudbreak.template.views.HostgroupView;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;
import com.sequenceiq.common.api.filesystem.S3FileSystem;
import com.sequenceiq.common.api.type.InstanceGroupType;

@ExtendWith(MockitoExtension.class)
public class SparkOnYarnRoleConfigProviderTest {

    private final SparkOnYarnRoleConfigProvider underTest = new SparkOnYarnRoleConfigProvider();

    @Test
    public void testGetSparkOnYarnRoleConfigs() {
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
    public void testGetSparkOnYarnRoleConfigsWhenNoStorageConfigured() {
        TemplatePreparationObject preparationObject = getTemplatePreparationObject(CloudPlatform.GCP, null);
        String inputJson = getBlueprintText("input/clouderamanager-ds.bp");
        CmTemplateProcessor cmTemplateProcessor = new CmTemplateProcessor(inputJson);


        Map<String, List<ApiClusterTemplateConfig>> roleConfigs = underTest.getRoleConfigs(cmTemplateProcessor, preparationObject);
        List<ApiClusterTemplateConfig> sparkOnYarnConfigs = roleConfigs.get("spark_on_yarn-GATEWAY-BASE");
        assertEquals(0, sparkOnYarnConfigs.size());
    }

    @Test
    public void testGetSparkOnYarnRoleConfigsForAwsWhenNoStorageConfigured() {
        TemplatePreparationObject preparationObject = getTemplatePreparationObject(CloudPlatform.AWS, "AWS", new String[0]);
        String inputJson = getBlueprintText("input/clouderamanager-ds.bp");
        CmTemplateProcessor cmTemplateProcessor = new CmTemplateProcessor(inputJson);


        Map<String, List<ApiClusterTemplateConfig>> roleConfigs = underTest.getRoleConfigs(cmTemplateProcessor, preparationObject);
        List<ApiClusterTemplateConfig> sparkOnYarnConfigs = roleConfigs.get("spark_on_yarn-GATEWAY-BASE");
        assertEquals(1, sparkOnYarnConfigs.size());
        assertEquals("spark-conf/spark-defaults.conf_client_config_safety_valve", sparkOnYarnConfigs.get(0).getName());
        assertEquals("spark.hadoop.fs.s3a.ssl.channel.mode=openssl", sparkOnYarnConfigs.get(0).getValue());
    }

    protected void validateClientConfig(String hmsExternalDirLocation, String clientConfigDirLocation) {
        TemplatePreparationObject preparationObject = getTemplatePreparationObject(CloudPlatform.AWS, "AWS", hmsExternalDirLocation);
        String inputJson = getBlueprintText("input/clouderamanager-ds.bp");
        CmTemplateProcessor cmTemplateProcessor = new CmTemplateProcessor(inputJson);

        Map<String, List<ApiClusterTemplateConfig>> roleConfigs = underTest.getRoleConfigs(cmTemplateProcessor, preparationObject);
        List<ApiClusterTemplateConfig> sparkOnYarnConfigs = roleConfigs.get("spark_on_yarn-GATEWAY-BASE");

        assertEquals(1, sparkOnYarnConfigs.size());
        assertEquals("spark-conf/spark-defaults.conf_client_config_safety_valve", sparkOnYarnConfigs.get(0).getName());
        assertEquals("spark.yarn.access.hadoopFileSystems=" + clientConfigDirLocation +
                "\nspark.hadoop.fs.s3a.ssl.channel.mode=openssl", sparkOnYarnConfigs.get(0).getValue());

    }

    @Test
    public void testGetSparkOnYarnRoleConfigsForAwsGov() {
        TemplatePreparationObject preparationObject = getTemplatePreparationObject(CloudPlatform.AWS, "AWS_NATIVE_GOV", "s3a://bucket/hive/warehouse/external");
        String inputJson = getBlueprintText("input/clouderamanager-ds.bp");
        CmTemplateProcessor cmTemplateProcessor = new CmTemplateProcessor(inputJson);

        Map<String, List<ApiClusterTemplateConfig>> roleConfigs = underTest.getRoleConfigs(cmTemplateProcessor, preparationObject);
        List<ApiClusterTemplateConfig> sparkOnYarnConfigs = roleConfigs.get("spark_on_yarn-GATEWAY-BASE");

        assertEquals(1, sparkOnYarnConfigs.size());
        assertEquals("spark-conf/spark-defaults.conf_client_config_safety_valve", sparkOnYarnConfigs.get(0).getName());
        assertEquals("spark.yarn.access.hadoopFileSystems=s3a://bucket", sparkOnYarnConfigs.get(0).getValue());
    }

    private TemplatePreparationObject getTemplatePreparationObject(CloudPlatform cloudPlatform, String platformVariant, String... locations) {
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

        String inputJson = getBlueprintText("input/clouderamanager-ds.bp");
        CmTemplateProcessor cmTemplateProcessor = new CmTemplateProcessor(inputJson);
        cmTemplateProcessor.setCdhVersion("7.2.16");

        return Builder.builder()
                .withFileSystemConfigurationView(fileSystemConfigurationsView)
                .withHostgroupViews(Set.of(master, worker))
                .withBlueprintView(new BlueprintView(null, null, null, null, cmTemplateProcessor))
                .withCloudPlatform(cloudPlatform)
                .withPlatformVariant(platformVariant)
                .build();
    }

    private String getBlueprintText(String path) {
        return FileReaderUtils.readFileFromClasspathQuietly(path);
    }
}
