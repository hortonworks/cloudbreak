package com.sequenceiq.cloudbreak.cmtemplate.configproviders.tez;

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
import com.sequenceiq.cloudbreak.template.filesystem.StorageLocationView;
import com.sequenceiq.cloudbreak.template.filesystem.s3.S3FileSystemConfigurationsView;
import com.sequenceiq.cloudbreak.template.views.HostgroupView;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;
import com.sequenceiq.common.api.filesystem.S3FileSystem;
import com.sequenceiq.common.api.type.InstanceGroupType;

@ExtendWith(MockitoExtension.class)
class TezRoleConfigProviderTest {

    private final TezRoleConfigProvider underTest = new TezRoleConfigProvider();

    @Test
    void testGetTezClientRoleConfigs() {
        validateClientConfig("s3a://hive/warehouse/external", "s3a://hive/warehouse/external/sys.db");
        validateClientConfig("s3a://hive/warehouse/external/", "s3a://hive/warehouse/external/sys.db");
    }

    @Test
    void testGetTezClientRoleConfigsWhenNoStorageConfigured() {
        TemplatePreparationObject preparationObject = getTemplatePreparationObject();
        String inputJson = getBlueprintText("input/clouderamanager-ds.bp");
        CmTemplateProcessor cmTemplateProcessor = new CmTemplateProcessor(inputJson);

        Map<String, List<ApiClusterTemplateConfig>> roleConfigs = underTest.getRoleConfigs(cmTemplateProcessor, preparationObject);
        List<ApiClusterTemplateConfig> tezConfigs = roleConfigs.get("tez-GATEWAY-BASE");
        assertEquals(0, tezConfigs.size());
    }

    protected void validateClientConfig(String hmsExternalDirLocation, String protoDirLocation) {
        TemplatePreparationObject preparationObject = getTemplatePreparationObject(hmsExternalDirLocation);
        String inputJson = getBlueprintText("input/clouderamanager-ds.bp");
        CmTemplateProcessor cmTemplateProcessor = new CmTemplateProcessor(inputJson);

        Map<String, List<ApiClusterTemplateConfig>> roleConfigs = underTest.getRoleConfigs(cmTemplateProcessor, preparationObject);
        List<ApiClusterTemplateConfig> tezConfigs = roleConfigs.get("tez-GATEWAY-BASE");

        assertEquals(1, tezConfigs.size());
        assertEquals("tez-conf/tez-site.xml_client_config_safety_valve", tezConfigs.get(0).getName());
        assertEquals("<property><name>tez.history.logging.proto-base-dir</name><value>"
                + protoDirLocation + "</value></property>", tezConfigs.get(0).getValue());
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
