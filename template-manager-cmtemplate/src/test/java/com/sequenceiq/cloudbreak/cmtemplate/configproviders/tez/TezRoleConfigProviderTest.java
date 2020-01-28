package com.sequenceiq.cloudbreak.cmtemplate.configproviders.tez;

import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.core.CoreConfigProvider.CORE_DEFAULTFS;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerRepo;

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

public class TezRoleConfigProviderTest {

    private final TezRoleConfigProvider underTest = new TezRoleConfigProvider();

    @Test
    public void testGetTezClientRoleConfigs() {
        validateClientConfig("s3a://hive/warehouse/external",
                "s3a://datahubName", "s3a://hive/warehouse/external/sys.db",
                "s3a://datahubName/user/tez/7.0.2-1.cdh7.0.2.p2.1711788/tez.tar.gz");
        validateClientConfig("s3a://hive/warehouse/external/",
                "s3a://datahubName/", "s3a://hive/warehouse/external/sys.db",
                "s3a://datahubName/user/tez/7.0.2-1.cdh7.0.2.p2.1711788/tez.tar.gz");
    }

    @Test
    public void testGetTezClientRoleConfigsWhenNoStorageConfigured() {
        TemplatePreparationObject preparationObject = getTemplatePreparationObject();
        String inputJson = getBlueprintText("input/clouderamanager-ds.bp");
        CmTemplateProcessor cmTemplateProcessor = new CmTemplateProcessor(inputJson);

        Map<String, List<ApiClusterTemplateConfig>> roleConfigs = underTest.getRoleConfigs(cmTemplateProcessor, preparationObject);
        List<ApiClusterTemplateConfig> tezConfigs = roleConfigs.get("tez-GATEWAY-BASE");
        assertThat(tezConfigs).hasSize(0);
    }

    protected void validateClientConfig(String hmsExternalDirLocation, String datahubDirLocation, String protoDirLocation, String tezLibUri) {
        TemplatePreparationObject preparationObject = getTemplatePreparationObject(hmsExternalDirLocation, datahubDirLocation);
        String inputJson = getBlueprintText("input/clouderamanager-ds.bp");
        CmTemplateProcessor cmTemplateProcessor = new CmTemplateProcessor(inputJson);

        Map<String, List<ApiClusterTemplateConfig>> roleConfigs = underTest.getRoleConfigs(cmTemplateProcessor, preparationObject);
        List<ApiClusterTemplateConfig> tezConfigs = roleConfigs.get("tez-GATEWAY-BASE");

        assertThat(tezConfigs).hasSize(1);
        assertThat(tezConfigs.get(0).getName()).isEqualTo("tez-conf/tez-site.xml_client_config_safety_valve");
        assertThat(tezConfigs.get(0).getValue()).contains("<property><name>tez.history.logging.proto-base-dir</name><value>"
                + protoDirLocation + "</value></property>");
        assertThat(tezConfigs.get(0).getValue()).contains("<property><name>tez.lib.uris</name><value>"
                + tezLibUri + "</value></property>");
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

            StorageLocation datahubDirLocation = new StorageLocation();
            datahubDirLocation.setProperty(CORE_DEFAULTFS);
            datahubDirLocation.setValue(locations[1]);
            storageLocations.add(new StorageLocationView(datahubDirLocation));
        }
        S3FileSystemConfigurationsView fileSystemConfigurationsView =
                new S3FileSystemConfigurationsView(new S3FileSystem(), storageLocations, false);

        ArrayList<ClouderaManagerProduct> products = new ArrayList<>();
        ClouderaManagerProduct cdh = new ClouderaManagerProduct();
        cdh.setName("cdh");
        cdh.setVersion("7.0.2-1.cdh7.0.2.p2.1711788");
        products.add(cdh);

        return Builder.builder().withFileSystemConfigurationView(fileSystemConfigurationsView)
                .withProductDetails(new ClouderaManagerRepo(), products)
                .withHostgroupViews(Set.of(master, worker)).build();
    }

    private String getBlueprintText(String path) {
        return FileReaderUtils.readFileFromClasspathQuietly(path);
    }
}
