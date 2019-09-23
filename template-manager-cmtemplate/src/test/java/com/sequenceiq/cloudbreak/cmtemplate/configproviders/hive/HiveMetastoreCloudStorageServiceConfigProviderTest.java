package com.sequenceiq.cloudbreak.cmtemplate.configproviders.hive;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.common.api.type.InstanceGroupType;
import com.sequenceiq.common.api.filesystem.S3FileSystem;
import com.sequenceiq.cloudbreak.domain.StorageLocation;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject.Builder;
import com.sequenceiq.cloudbreak.template.filesystem.StorageLocationView;
import com.sequenceiq.cloudbreak.template.filesystem.s3.S3FileSystemConfigurationsView;
import com.sequenceiq.cloudbreak.template.views.HostgroupView;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;

@RunWith(MockitoJUnitRunner.class)
public class HiveMetastoreCloudStorageServiceConfigProviderTest {

    private final HiveMetastoreCloudStorageServiceConfigProvider underTest = new HiveMetastoreCloudStorageServiceConfigProvider();

    @Test
    public void testGetHMSStorageServiceConfigs() {
        TemplatePreparationObject preparationObject = getTemplatePreparationObject(true);
        String inputJson = getBlueprintText("input/clouderamanager.bp");
        CmTemplateProcessor cmTemplateProcessor = new CmTemplateProcessor(inputJson);

        List<ApiClusterTemplateConfig> serviceConfigs = underTest.getServiceConfigs(cmTemplateProcessor, preparationObject);

        assertEquals(3, serviceConfigs.size());
        assertEquals("hive_warehouse_directory", serviceConfigs.get(0).getName());
        assertEquals("s3a://bucket/hive/warehouse", serviceConfigs.get(0).getValue());

        assertEquals("hive_warehouse_external_directory", serviceConfigs.get(1).getName());
        assertEquals("s3a://bucket/hive/warehouse/external", serviceConfigs.get(1).getValue());

        assertEquals("hive_repl_replica_functions_root_dir", serviceConfigs.get(2).getName());
        assertEquals("s3a://bucket/hive/replica", serviceConfigs.get(2).getValue());
    }

    @Test
    public void testGetHMSStorageServiceConfigsWhenNoStorageConfigured() {
        TemplatePreparationObject preparationObject = getTemplatePreparationObject(false);
        String inputJson = getBlueprintText("input/clouderamanager.bp");
        CmTemplateProcessor cmTemplateProcessor = new CmTemplateProcessor(inputJson);

        List<ApiClusterTemplateConfig> serviceConfigs = underTest.getServiceConfigs(cmTemplateProcessor, preparationObject);
        assertEquals(0, serviceConfigs.size());
    }

    @Test
    public void testIsConfigurationNeededShouldReturnFalseWhenNoHMSRole() {
        TemplatePreparationObject preparationObject = getTemplatePreparationObject(true);
        String inputJson = getBlueprintText("input/clouderamanager-nometastore.bp");
        CmTemplateProcessor cmTemplateProcessor = new CmTemplateProcessor(inputJson);

        boolean configNeeded = underTest.isConfigurationNeeded(cmTemplateProcessor, preparationObject);
        assertFalse(configNeeded);
    }

    @Test
    public void testIsConfigurationNeededShouldReturnTrueWhenHMSRoleProvided() {
        TemplatePreparationObject preparationObject = getTemplatePreparationObject(true);
        String inputJson = getBlueprintText("input/clouderamanager.bp");
        CmTemplateProcessor cmTemplateProcessor = new CmTemplateProcessor(inputJson);

        boolean configNeeded = underTest.isConfigurationNeeded(cmTemplateProcessor, preparationObject);
        assertTrue(configNeeded);
    }

    private TemplatePreparationObject getTemplatePreparationObject(boolean includeLocations) {
        HostgroupView master = new HostgroupView("master", 1, InstanceGroupType.GATEWAY, 1);
        HostgroupView worker = new HostgroupView("worker", 2, InstanceGroupType.CORE, 2);

        List<StorageLocationView> locations = new ArrayList<>();

        if (includeLocations) {
            locations.add(new StorageLocationView(getHiveWarehouseStorageLocation()));
            locations.add(new StorageLocationView(getHiveWarehouseExternalStorageLocation()));
            locations.add(new StorageLocationView(getHiveWarehouseReplicaStorageLocation()));
        }
        S3FileSystemConfigurationsView fileSystemConfigurationsView =
                new S3FileSystemConfigurationsView(new S3FileSystem(), locations, false);


        return Builder.builder().withFileSystemConfigurationView(fileSystemConfigurationsView)
                .withHostgroupViews(Set.of(master, worker)).build();
    }

    protected StorageLocation getHiveWarehouseStorageLocation() {
        StorageLocation hmsWarehouseDir = new StorageLocation();
        hmsWarehouseDir.setProperty("hive.metastore.warehouse.dir");
        hmsWarehouseDir.setValue("s3a://bucket/hive/warehouse");
        return hmsWarehouseDir;
    }

    protected StorageLocation getHiveWarehouseExternalStorageLocation() {
        StorageLocation hmsExternalWarehouseDir = new StorageLocation();
        hmsExternalWarehouseDir.setProperty("hive.metastore.warehouse.external.dir");
        hmsExternalWarehouseDir.setValue("s3a://bucket/hive/warehouse/external");
        return hmsExternalWarehouseDir;
    }

    protected StorageLocation getHiveWarehouseReplicaStorageLocation() {
        StorageLocation hmsExternalWarehouseDir = new StorageLocation();
        hmsExternalWarehouseDir.setProperty("hive.repl.replica.functions.root.dir");
        hmsExternalWarehouseDir.setValue("s3a://bucket/hive/replica");
        return hmsExternalWarehouseDir;
    }

    private String getBlueprintText(String path) {
        return FileReaderUtils.readFileFromClasspathQuietly(path);
    }
}
