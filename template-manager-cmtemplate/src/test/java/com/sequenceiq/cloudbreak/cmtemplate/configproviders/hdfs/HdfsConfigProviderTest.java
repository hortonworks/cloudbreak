package com.sequenceiq.cloudbreak.cmtemplate.configproviders.hdfs;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.ExposedService.NAMENODE;
import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.sequenceiq.cloudbreak.TestUtil;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.domain.StorageLocation;
import com.sequenceiq.cloudbreak.domain.stack.cluster.gateway.Gateway;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject.Builder;
import com.sequenceiq.cloudbreak.template.filesystem.BaseFileSystemConfigurationsView;
import com.sequenceiq.cloudbreak.template.filesystem.StorageLocationView;
import com.sequenceiq.cloudbreak.template.filesystem.adls.AdlsFileSystemConfigurationsView;
import com.sequenceiq.cloudbreak.template.filesystem.s3.S3FileSystemConfigurationsView;
import com.sequenceiq.cloudbreak.template.views.HostgroupView;
import com.sequenceiq.cloudbreak.template.views.PlacementView;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;
import com.sequenceiq.common.api.filesystem.AdlsFileSystem;
import com.sequenceiq.common.api.filesystem.S3FileSystem;
import com.sequenceiq.common.api.type.InstanceGroupType;

@RunWith(MockitoJUnitRunner.class)
public class HdfsConfigProviderTest {

    private final HdfsConfigProvider underTest = new HdfsConfigProvider();

    @Test
    public void testGetHdfsServiceConfigsWithoutS3Guard() {
        TemplatePreparationObject preparationObject = getTemplatePreparationObject(false, false, false);
        String inputJson = getBlueprintText("input/clouderamanager.bp");
        CmTemplateProcessor cmTemplateProcessor = new CmTemplateProcessor(inputJson);

        List<ApiClusterTemplateConfig> serviceConfigs = underTest.getServiceConfigs(cmTemplateProcessor, preparationObject);

        assertEquals(1, serviceConfigs.size());
        assertEquals("core_site_safety_valve", serviceConfigs.get(0).getName());
        assertEquals("<property><name>hadoop.http.filter.initializers</name>"
                +  "<value>org.apache.hadoop.security.HttpCrossOriginFilterInitializer,"
                +  "org.apache.hadoop.security.authentication.server.ProxyUserAuthenticationFilterInitializer"
                +  "</value></property>", serviceConfigs.get(0).getValue());
    }

    @Test
    public void testGetHdfsServiceConfigsWithS3FileSystemNoDynamoTable() {
        TemplatePreparationObject preparationObject = getTemplatePreparationObject(true, false, false);
        String inputJson = getBlueprintText("input/clouderamanager.bp");
        CmTemplateProcessor cmTemplateProcessor = new CmTemplateProcessor(inputJson);

        List<ApiClusterTemplateConfig> serviceConfigs = underTest.getServiceConfigs(cmTemplateProcessor, preparationObject);

        assertEquals(1, serviceConfigs.size());
        assertEquals("core_site_safety_valve", serviceConfigs.get(0).getName());
        assertEquals("<property><name>hadoop.http.filter.initializers</name>"
                +  "<value>org.apache.hadoop.security.HttpCrossOriginFilterInitializer,"
                +  "org.apache.hadoop.security.authentication.server.ProxyUserAuthenticationFilterInitializer"
                +  "</value></property>", serviceConfigs.get(0).getValue());
    }

    @Test
    public void testGetHdfsServiceConfigsWithS3GuardWithoutAuthoriative() {
        TemplatePreparationObject preparationObject = getTemplatePreparationObject(true, true, false);
        String inputJson = getBlueprintText("input/clouderamanager.bp");
        CmTemplateProcessor cmTemplateProcessor = new CmTemplateProcessor(inputJson);

        List<ApiClusterTemplateConfig> serviceConfigs = underTest.getServiceConfigs(cmTemplateProcessor, preparationObject);

        assertEquals(1, serviceConfigs.size());
        assertEquals("core_site_safety_valve", serviceConfigs.get(0).getName());
        assertEquals("<property><name>hadoop.http.filter.initializers</name>"
                + "<value>org.apache.hadoop.security.HttpCrossOriginFilterInitializer,"
                + "org.apache.hadoop.security.authentication.server.ProxyUserAuthenticationFilterInitializer</value></property>"
                + "<property><name>fs.s3a.metadatastore.impl</name><value>org.apache.hadoop.fs.s3a.s3guard.DynamoDBMetadataStore</value></property>"
                + "<property><name>fs.s3a.s3guard.ddb.table.tag.cdp_table_role</name><value>s3guard</value></property>"
                + "<property><name>fs.s3a.s3guard.ddb.table.create</name><value>true</value></property>"
                + "<property><name>fs.s3a.s3guard.ddb.table</name><value>dynamoTable</value></property>"
                + "<property><name>fs.s3a.s3guard.ddb.region</name><value>region</value></property>", serviceConfigs.get(0).getValue());
    }

    @Test
    public void testGetHdfsServiceConfigsWithS3GuardWithAuthoriativeWarehousePath() {
        TemplatePreparationObject preparationObject = getTemplatePreparationObject(true, true, true);
        String inputJson = getBlueprintText("input/clouderamanager.bp");
        CmTemplateProcessor cmTemplateProcessor = new CmTemplateProcessor(inputJson);

        List<ApiClusterTemplateConfig> serviceConfigs = underTest.getServiceConfigs(cmTemplateProcessor, preparationObject);

        assertEquals(1, serviceConfigs.size());
        assertEquals("core_site_safety_valve", serviceConfigs.get(0).getName());
        assertEquals("<property><name>hadoop.http.filter.initializers</name>"
                + "<value>org.apache.hadoop.security.HttpCrossOriginFilterInitializer,"
                + "org.apache.hadoop.security.authentication.server.ProxyUserAuthenticationFilterInitializer</value></property>"
                + "<property><name>fs.s3a.metadatastore.impl</name><value>org.apache.hadoop.fs.s3a.s3guard.DynamoDBMetadataStore</value></property>"
                + "<property><name>fs.s3a.s3guard.ddb.table.tag.cdp_table_role</name><value>s3guard</value></property>"
                + "<property><name>fs.s3a.s3guard.ddb.table.create</name><value>true</value></property>"
                + "<property><name>fs.s3a.s3guard.ddb.table</name><value>dynamoTable</value></property>"
                + "<property><name>fs.s3a.s3guard.ddb.region</name><value>region</value></property>"
                + "<property><name>fs.s3a.authoritative.path</name><value>s3a://bucket/warehouse/managed</value></property>", serviceConfigs.get(0).getValue());
    }

    private TemplatePreparationObject getTemplatePreparationObject(boolean useS3FileSystem, boolean fillDynamoTableName, boolean includeLocations) {
        HostgroupView master = new HostgroupView("master", 1, InstanceGroupType.GATEWAY, 1);
        HostgroupView worker = new HostgroupView("worker", 2, InstanceGroupType.CORE, 2);

        List<StorageLocationView> locations = new ArrayList<>();

        if (includeLocations) {
            locations.add(new StorageLocationView(getStorageLocation("hive.metastore.warehouse.dir", "s3a://bucket/warehouse/managed")));
            locations.add(new StorageLocationView(getStorageLocation("hive.metastore.warehouse.external.dir", "s3a://bucket/warehouse/external")));
        }

        BaseFileSystemConfigurationsView fileSystemConfigurationsView;
        if (useS3FileSystem) {
            S3FileSystem s3FileSystem = new S3FileSystem();
            if (fillDynamoTableName) {
                s3FileSystem.setS3GuardDynamoTableName("dynamoTable");
            }
            fileSystemConfigurationsView =
                    new S3FileSystemConfigurationsView(s3FileSystem, locations, false);
        } else {
            fileSystemConfigurationsView =
                    new AdlsFileSystemConfigurationsView(new AdlsFileSystem(), locations, false);
        }

        Gateway gateway = TestUtil.gatewayEnabledWithExposedKnoxServices(NAMENODE.getKnoxService());

        PlacementView placementView = new PlacementView("region", "az");

        return Builder.builder().withFileSystemConfigurationView(fileSystemConfigurationsView)
                .withHostgroupViews(Set.of(master, worker))
                .withGateway(gateway, "/cb/secret/signkey")
                .withPlacementView(placementView)
                .build();
    }

    protected StorageLocation getStorageLocation(String property, String value) {
        StorageLocation hmsWarehouseDir = new StorageLocation();
        hmsWarehouseDir.setProperty(property);
        hmsWarehouseDir.setValue(value);
        return hmsWarehouseDir;
    }

    private String getBlueprintText(String path) {
        return FileReaderUtils.readFileFromClasspathQuietly(path);
    }
}
