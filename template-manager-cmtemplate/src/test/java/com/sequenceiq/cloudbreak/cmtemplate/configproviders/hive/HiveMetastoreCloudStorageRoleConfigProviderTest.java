package com.sequenceiq.cloudbreak.cmtemplate.configproviders.hive;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

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
public class HiveMetastoreCloudStorageRoleConfigProviderTest {

    private final HiveMetastoreCloudStorageRoleConfigProvider underTest = new HiveMetastoreCloudStorageRoleConfigProvider();

    @Test
    public void testGetHMSStorageRoleConfigs() {
        TemplatePreparationObject preparationObject = getTemplatePreparationObject(true);
        String inputJson = getBlueprintText("input/clouderamanager.bp");
        CmTemplateProcessor cmTemplateProcessor = new CmTemplateProcessor(inputJson);

        Map<String, List<ApiClusterTemplateConfig>> roleConfigs = underTest.getRoleConfigs(cmTemplateProcessor, preparationObject);

        List<ApiClusterTemplateConfig> hmsStorageConfigs = roleConfigs.get("hive-HIVEMETASTORE-BASE");

        assertEquals(1, hmsStorageConfigs.size());
        assertEquals("hive_metastore_config_safety_valve", hmsStorageConfigs.get(0).getName());
        String expected = "<property><name>hive.metastore.warehouse.dir</name><value>"
                + getHiveWarehouseStorageLocation().getValue()
                + "</value></property>"
                + "<property><name>hive.metastore.warehouse.external.dir</name><value>"
                + getHiveWarehouseExternalStorageLocation().getValue() + "</value></property>";
        assertEquals(expected, hmsStorageConfigs.get(0).getValue());
    }

    @Test
    public void testGetHMSStorageRoleConfigsWhenNoStorageConfigured() {
        TemplatePreparationObject preparationObject = getTemplatePreparationObject(false);
        String inputJson = getBlueprintText("input/clouderamanager.bp");
        CmTemplateProcessor cmTemplateProcessor = new CmTemplateProcessor(inputJson);

        Map<String, List<ApiClusterTemplateConfig>> roleConfigs = underTest.getRoleConfigs(cmTemplateProcessor, preparationObject);

        List<ApiClusterTemplateConfig> hmsStorageConfigs = roleConfigs.get("hive-HIVEMETASTORE-BASE");
        assertEquals(0, hmsStorageConfigs.size());
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

    private String getBlueprintText(String path) {
        return FileReaderUtils.readFileFromClasspathQuietly(path);
    }
}
