package com.sequenceiq.cloudbreak.cmtemplate.configproviders.ranger;

import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils.config;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ranger.RangerCloudStorageServiceConfigProvider.DEFAULT_BACKUP_DIR;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerRepo;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.hdfs.HdfsConfigHelper;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.hdfs.HdfsRoles;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.StorageLocation;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject.Builder;
import com.sequenceiq.cloudbreak.template.filesystem.StorageLocationView;
import com.sequenceiq.cloudbreak.template.filesystem.adlsgen2.AdlsGen2FileSystemConfigurationsView;
import com.sequenceiq.cloudbreak.template.filesystem.gcs.GcsFileSystemConfigurationsView;
import com.sequenceiq.cloudbreak.template.filesystem.s3.S3FileSystemConfigurationsView;
import com.sequenceiq.cloudbreak.template.model.GeneralClusterConfigs;
import com.sequenceiq.cloudbreak.template.views.BlueprintView;
import com.sequenceiq.cloudbreak.template.views.HostgroupView;
import com.sequenceiq.common.api.filesystem.AdlsGen2FileSystem;
import com.sequenceiq.common.api.filesystem.GcsFileSystem;
import com.sequenceiq.common.api.filesystem.S3FileSystem;
import com.sequenceiq.common.api.type.InstanceGroupType;

@ExtendWith(MockitoExtension.class)
public class RangerCloudStorageServiceConfigProviderTest {

    @Spy
    private HdfsConfigHelper hdfsConfigHelper;

    @InjectMocks
    private RangerCloudStorageServiceConfigProvider underTest;

    @Test
    public void testGetRangerAwsCloudStorageServiceConfigsForDataLake() {
        CmTemplateProcessor templateProcessor = mock(CmTemplateProcessor.class);
        TemplatePreparationObject preparationObject = getTemplatePreparationObjectForAws(true, true)
                .withBlueprintView(mock(BlueprintView.class))
                .withCloudPlatform(CloudPlatform.AWS)
                .withProductDetails(new ClouderaManagerRepo().withVersion("7.2.2"), List.of())
                .withStackType(StackType.DATALAKE)
                .build();

        List<ApiClusterTemplateConfig> serviceConfigs = underTest.getServiceConfigs(templateProcessor, preparationObject);

        assertEquals(2, serviceConfigs.size());
        assertEquals("ranger_plugin_hdfs_audit_url", serviceConfigs.get(0).getName());
        assertEquals("s3a://bucket/ranger/audit", serviceConfigs.get(0).getValue());
        assertEquals("cloud_storage_paths", serviceConfigs.get(1).getName());
        assertEquals("HIVE_METASTORE_WAREHOUSE=s3a://bucket/warehouse/tablespace/managed/hive," +
                "HIVE_REPLICA_WAREHOUSE=s3a://bucket/hive_replica_functions_dir," +
                "HIVE_METASTORE_EXTERNAL_WAREHOUSE=s3a://bucket/warehouse/tablespace/external/hive," +
                "RANGER_AUDIT=s3a://bucket/ranger/audit," +
                "HBASE_ROOT=s3a://bucket/hbase", serviceConfigs.get(1).getValue());
    }

    @Test
    public void testGetRangerAwsCloudStorageServiceConfigsForDataHub() {
        CmTemplateProcessor templateProcessor = mock(CmTemplateProcessor.class);
        TemplatePreparationObject preparationObject = getTemplatePreparationObjectForAws(true, true)
                .withBlueprintView(mock(BlueprintView.class))
                .withCloudPlatform(CloudPlatform.AWS)
                .withProductDetails(new ClouderaManagerRepo().withVersion("7.2.2"), List.of())
                .withStackType(StackType.WORKLOAD)
                .build();

        List<ApiClusterTemplateConfig> serviceConfigs = underTest.getServiceConfigs(templateProcessor, preparationObject);

        assertEquals(2, serviceConfigs.size());
        assertEquals("ranger_plugin_hdfs_audit_url", serviceConfigs.get(0).getName());
        assertEquals("s3a://bucket/ranger/audit", serviceConfigs.get(0).getValue());
        assertEquals("cloud_storage_paths", serviceConfigs.get(1).getName());
        assertEquals("HIVE_METASTORE_WAREHOUSE=s3a://bucket/warehouse/tablespace/managed/hive," +
                "HIVE_REPLICA_WAREHOUSE=s3a://bucket/hive_replica_functions_dir," +
                "HIVE_METASTORE_EXTERNAL_WAREHOUSE=s3a://bucket/warehouse/tablespace/external/hive," +
                "RANGER_AUDIT=s3a://bucket/ranger/audit," +
                "HBASE_ROOT=s3a://bucket/hbase", serviceConfigs.get(1).getValue());
    }

    @Test
    public void testGetRangerAws720CloudStorageServiceConfigs() {
        CmTemplateProcessor templateProcessor = mock(CmTemplateProcessor.class);
        TemplatePreparationObject preparationObject = getTemplatePreparationObjectForAws(true, false)
                .withBlueprintView(mock(BlueprintView.class))
                .withCloudPlatform(CloudPlatform.AWS)
                .withProductDetails(new ClouderaManagerRepo().withVersion("7.2.1"), List.of())
                .withStackType(StackType.DATALAKE)
                .build();

        List<ApiClusterTemplateConfig> serviceConfigs = underTest.getServiceConfigs(templateProcessor, preparationObject);

        assertEquals(1, serviceConfigs.size());
        assertEquals("ranger_plugin_hdfs_audit_url", serviceConfigs.get(0).getName());
        assertEquals("s3a://bucket/ranger/audit", serviceConfigs.get(0).getValue());
    }

    @Test
    public void testGetRangerAzureCloudStorageServiceConfigsForDataLake() {
        CmTemplateProcessor templateProcessor = mock(CmTemplateProcessor.class);
        TemplatePreparationObject preparationObject = getTemplatePreparationObjectForAzure(true)
                .withBlueprintView(mock(BlueprintView.class))
                .withCloudPlatform(CloudPlatform.AZURE)
                .withProductDetails(new ClouderaManagerRepo().withVersion("7.2.2"), List.of())
                .withStackType(StackType.DATALAKE)
                .build();

        List<ApiClusterTemplateConfig> serviceConfigs = underTest.getServiceConfigs(templateProcessor, preparationObject);

        assertEquals(2, serviceConfigs.size());
        assertEquals("ranger_plugin_hdfs_audit_url", serviceConfigs.get(0).getName());
        assertEquals("abfs://data@your-san.dfs.core.windows.net/ranger/audit", serviceConfigs.get(0).getValue());
        assertEquals("cloud_storage_paths", serviceConfigs.get(1).getName());
        assertEquals("HIVE_METASTORE_WAREHOUSE=abfs://data@your-san.dfs.core.windows.net/warehouse/tablespace/managed/hive," +
                "HIVE_REPLICA_WAREHOUSE=abfs://data@your-san.dfs.core.windows.net/hive_replica_functions_dir," +
                "HIVE_METASTORE_EXTERNAL_WAREHOUSE=abfs://data@your-san.dfs.core.windows.net/warehouse/tablespace/external/hive," +
                "RANGER_AUDIT=abfs://data@your-san.dfs.core.windows.net/ranger/audit," +
                "HBASE_ROOT=abfs://data@your-san.dfs.core.windows.net/hbase", serviceConfigs.get(1).getValue());
    }

    @Test
    public void testGetRangerAzureCloudStorageServiceConfigsForDataHub() {
        CmTemplateProcessor templateProcessor = mock(CmTemplateProcessor.class);
        TemplatePreparationObject preparationObject = getTemplatePreparationObjectForAzure(true)
                .withBlueprintView(mock(BlueprintView.class))
                .withCloudPlatform(CloudPlatform.AZURE)
                .withProductDetails(new ClouderaManagerRepo().withVersion("7.2.2"), List.of())
                .withStackType(StackType.WORKLOAD)
                .build();

        List<ApiClusterTemplateConfig> serviceConfigs = underTest.getServiceConfigs(templateProcessor, preparationObject);

        assertEquals(2, serviceConfigs.size());
        assertEquals("ranger_plugin_hdfs_audit_url", serviceConfigs.get(0).getName());
        assertEquals("abfs://data@your-san.dfs.core.windows.net/ranger/audit", serviceConfigs.get(0).getValue());
        assertEquals("cloud_storage_paths", serviceConfigs.get(1).getName());
        assertEquals("HIVE_METASTORE_WAREHOUSE=abfs://data@your-san.dfs.core.windows.net/warehouse/tablespace/managed/hive," +
                "HIVE_REPLICA_WAREHOUSE=abfs://data@your-san.dfs.core.windows.net/hive_replica_functions_dir," +
                "HIVE_METASTORE_EXTERNAL_WAREHOUSE=abfs://data@your-san.dfs.core.windows.net/warehouse/tablespace/external/hive," +
                "RANGER_AUDIT=abfs://data@your-san.dfs.core.windows.net/ranger/audit," +
                "HBASE_ROOT=abfs://data@your-san.dfs.core.windows.net/hbase", serviceConfigs.get(1).getValue());
    }

    @Test
    public void testGetRangerGcpStorageServiceConfigsForDataLake() {
        CmTemplateProcessor templateProcessor = mock(CmTemplateProcessor.class);
        TemplatePreparationObject preparationObject = getTemplatePreparationObjectForGcp(true)
                .withBlueprintView(mock(BlueprintView.class))
                .withCloudPlatform(CloudPlatform.GCP)
                .withProductDetails(new ClouderaManagerRepo().withVersion("7.11.0"), List.of())
                .withStackType(StackType.DATALAKE)
                .build();

        List<ApiClusterTemplateConfig> serviceConfigs = underTest.getServiceConfigs(templateProcessor, preparationObject);

        assertEquals(2, serviceConfigs.size());
        assertEquals("ranger_plugin_hdfs_audit_url", serviceConfigs.get(0).getName());
        assertEquals("gs://perf-team-west2-bucket/ranger/audit", serviceConfigs.get(0).getValue());
        assertEquals("cloud_storage_paths", serviceConfigs.get(1).getName());
        assertEquals("HIVE_METASTORE_WAREHOUSE=gs://perf-team-west2-bucket/warehouse/tablespace/managed/hive," +
                "HIVE_REPLICA_WAREHOUSE=gs://perf-team-west2-bucket/hive_replica_functions_dir," +
                "HIVE_METASTORE_EXTERNAL_WAREHOUSE=gs://perf-team-west2-bucket/warehouse/tablespace/external/hive," +
                "RANGER_AUDIT=gs://perf-team-west2-bucket/ranger/audit,HBASE_ROOT=gs://perf-team-west2-bucket" +
                "/adarsh-raz-gcp-dl/hbase", serviceConfigs.get(1).getValue());
    }

    @Test
    public void testGetRangerAGcpCloudStorageServiceConfigsForDataHub() {
        CmTemplateProcessor templateProcessor = mock(CmTemplateProcessor.class);
        TemplatePreparationObject preparationObject = getTemplatePreparationObjectForGcp(true)
                .withBlueprintView(mock(BlueprintView.class))
                .withCloudPlatform(CloudPlatform.GCP)
                .withProductDetails(new ClouderaManagerRepo().withVersion("7.9.0"), List.of())
                .withStackType(StackType.WORKLOAD)
                .build();

        List<ApiClusterTemplateConfig> serviceConfigs = underTest.getServiceConfigs(templateProcessor, preparationObject);

        assertEquals(1, serviceConfigs.size());
        assertEquals("ranger_plugin_hdfs_audit_url", serviceConfigs.get(0).getName());
        assertEquals("gs://perf-team-west2-bucket/ranger/audit", serviceConfigs.get(0).getValue());
    }

    @Test
    public void testGetRangerAzure720CloudStorageServiceConfigs() {
        CmTemplateProcessor templateProcessor = mock(CmTemplateProcessor.class);
        TemplatePreparationObject preparationObject = getTemplatePreparationObjectForAzure(false)
                .withBlueprintView(mock(BlueprintView.class))
                .withCloudPlatform(CloudPlatform.AZURE)
                .withProductDetails(new ClouderaManagerRepo().withVersion("7.2.0"), List.of())
                .withStackType(StackType.DATALAKE)
                .build();

        List<ApiClusterTemplateConfig> serviceConfigs = underTest.getServiceConfigs(templateProcessor, preparationObject);

        assertEquals(1, serviceConfigs.size());
        assertEquals("ranger_plugin_hdfs_audit_url", serviceConfigs.get(0).getName());
        assertEquals("abfs://data@your-san.dfs.core.windows.net/ranger/audit", serviceConfigs.get(0).getValue());
    }

    @Test
    public void defaultRangerHdfsAuditUrlWithNamenodeHA() {
        CmTemplateProcessor templateProcessor = mock(CmTemplateProcessor.class);
        BlueprintView blueprintView = mock(BlueprintView.class);
        when(blueprintView.getProcessor()).thenReturn(templateProcessor);
        when(templateProcessor.getHostGroupsWithComponent(HdfsRoles.NAMENODE)).thenReturn(Set.of("master"));
        when(templateProcessor.getRoleConfig(HdfsRoles.HDFS, HdfsRoles.NAMENODE, "dfs_federation_namenode_nameservice"))
                .thenReturn(Optional.of(config("dfs_federation_namenode_nameservice", "ns")));
        TemplatePreparationObject preparationObject = getTemplatePreparationObjectForAws(false, false)
                .withBlueprintView(blueprintView)
                .withProductDetails(new ClouderaManagerRepo().withVersion("7.2.0"), List.of())
                .withStackType(StackType.DATALAKE)
                .build();

        List<ApiClusterTemplateConfig> serviceConfigs = underTest.getServiceConfigs(templateProcessor, preparationObject);

        assertEquals(1, serviceConfigs.size());
        assertEquals("ranger_plugin_hdfs_audit_url", serviceConfigs.get(0).getName());
        assertEquals("hdfs://ns", serviceConfigs.get(0).getValue());
    }

    @Test
    public void defaultRangerHdfsAuditUrlWithSingleNamenode() {
        CmTemplateProcessor templateProcessor = mock(CmTemplateProcessor.class);
        BlueprintView blueprintView = mock(BlueprintView.class);
        when(blueprintView.getProcessor()).thenReturn(templateProcessor);
        when(templateProcessor.getHostGroupsWithComponent(HdfsRoles.NAMENODE)).thenReturn(Set.of("gateway"));
        TemplatePreparationObject preparationObject = getTemplatePreparationObjectForAws(false, false)
                .withBlueprintView(blueprintView)
                .withProductDetails(new ClouderaManagerRepo().withVersion("7.2.0"), List.of())
                .withStackType(StackType.DATALAKE)
                .build();

        List<ApiClusterTemplateConfig> serviceConfigs = underTest.getServiceConfigs(templateProcessor, preparationObject);

        assertEquals(1, serviceConfigs.size());
        assertEquals("ranger_plugin_hdfs_audit_url", serviceConfigs.get(0).getName());
        assertEquals("hdfs://g", serviceConfigs.get(0).getValue());
    }

    @Test
    public void testGetRangerCloudStorageServiceConfigsWithBackup() {
        CmTemplateProcessor templateProcessor = mock(CmTemplateProcessor.class);
        TemplatePreparationObject preparationObject = getTemplatePreparationObjectForAws(true, true, true)
                .withBlueprintView(mock(BlueprintView.class))
                .withCloudPlatform(CloudPlatform.AWS)
                .withProductDetails(new ClouderaManagerRepo().withVersion("7.2.2"), List.of())
                .withStackType(StackType.DATALAKE)
                .build();

        List<ApiClusterTemplateConfig> serviceConfigs = underTest.getServiceConfigs(templateProcessor, preparationObject);

        assertEquals(2, serviceConfigs.size());
        assertEquals("ranger_plugin_hdfs_audit_url", serviceConfigs.get(0).getName());
        assertEquals("s3a://bucket/ranger/audit", serviceConfigs.get(0).getValue());
        assertEquals("cloud_storage_paths", serviceConfigs.get(1).getName());
        assertEquals("HIVE_METASTORE_WAREHOUSE=s3a://bucket/warehouse/tablespace/managed/hive," +
                "HIVE_REPLICA_WAREHOUSE=s3a://bucket/hive_replica_functions_dir," +
                "HIVE_METASTORE_EXTERNAL_WAREHOUSE=s3a://bucket/warehouse/tablespace/external/hive," +
                "RANGER_AUDIT=s3a://bucket/ranger/audit," +
                "HBASE_ROOT=s3a://bucket/hbase," +
                "BACKUP_LOCATION=s3a://bucket/backup/location", serviceConfigs.get(1).getValue());
    }

    private TemplatePreparationObject.Builder getTemplatePreparationObjectForAzure(boolean above721) {
        HostgroupView gateway = new HostgroupView("gateway", 1, InstanceGroupType.GATEWAY, Set.of("g"));
        HostgroupView master = new HostgroupView("master", 1, InstanceGroupType.CORE, Set.of("m1", "m2"));
        HostgroupView worker = new HostgroupView("worker", 2, InstanceGroupType.CORE, Set.of("w1", "w2", "w3"));

        List<StorageLocationView> locations = new ArrayList<>();

        locations.add(new StorageLocationView(getRangerAuditCloudStorageDirAzure()));
        if (above721) {
            locations.add(new StorageLocationView(buildStorageLocation("hive.metastore.warehouse.dir",
                    "abfs://data@your-san.dfs.core.windows.net/warehouse/tablespace/managed/hive")));
            locations.add(new StorageLocationView(buildStorageLocation("hive.repl.replica.functions.root.dir",
                    "abfs://data@your-san.dfs.core.windows.net/hive_replica_functions_dir")));
            locations.add(new StorageLocationView(buildStorageLocation("hive.metastore.warehouse.external.dir",
                    "abfs://data@your-san.dfs.core.windows.net/warehouse/tablespace/external/hive")));
            locations.add(new StorageLocationView(buildStorageLocation("hbase.rootdir",
                    "abfs://data@your-san.dfs.core.windows.net/hbase")));
        }
        AdlsGen2FileSystemConfigurationsView fileSystemConfigurationsView =
                new AdlsGen2FileSystemConfigurationsView(new AdlsGen2FileSystem(), locations, false);

        GeneralClusterConfigs generalClusterConfigs = new GeneralClusterConfigs();
        generalClusterConfigs.setPrimaryGatewayInstanceDiscoveryFQDN(Optional.of("fqdn"));

        return Builder.builder()
                .withFileSystemConfigurationView(fileSystemConfigurationsView)
                .withHostgroupViews(Set.of(gateway, master, worker))
                .withStackType(StackType.DATALAKE)
                .withGeneralClusterConfigs(generalClusterConfigs);
    }

    private TemplatePreparationObject.Builder getTemplatePreparationObjectForGcp(boolean above790) {
        HostgroupView gateway = new HostgroupView("gateway", 1, InstanceGroupType.GATEWAY, Set.of("g"));
        HostgroupView master = new HostgroupView("master", 1, InstanceGroupType.CORE, Set.of("m1", "m2"));
        HostgroupView worker = new HostgroupView("worker", 2, InstanceGroupType.CORE, Set.of("w1", "w2", "w3"));

        List<StorageLocationView> locations = new ArrayList<>();

        locations.add(new StorageLocationView(getRangerAuditCloudStorageDirGcp()));
        if (above790) {
            locations.add(new StorageLocationView(buildStorageLocation("hive.metastore.warehouse.dir",
                    "gs://perf-team-west2-bucket/warehouse/tablespace/managed/hive")));
            locations.add(new StorageLocationView(buildStorageLocation("hive.repl.replica.functions.root.dir",
                    "gs://perf-team-west2-bucket/hive_replica_functions_dir")));
            locations.add(new StorageLocationView(buildStorageLocation("hive.metastore.warehouse.external.dir",
                    "gs://perf-team-west2-bucket/warehouse/tablespace/external/hive")));
            locations.add(new StorageLocationView(buildStorageLocation("ranger_plugin_hdfs_audit_url",
                    "gs://perf-team-west2-bucket/ranger/audit")));
            locations.add(new StorageLocationView(buildStorageLocation("hbase.rootdir",
                    "gs://perf-team-west2-bucket/adarsh-raz-gcp-dl/hbase")));
        }
        GcsFileSystemConfigurationsView fileSystemConfigurationsView =
                new GcsFileSystemConfigurationsView(new GcsFileSystem(), locations, false);

        GeneralClusterConfigs generalClusterConfigs = new GeneralClusterConfigs();
        generalClusterConfigs.setPrimaryGatewayInstanceDiscoveryFQDN(Optional.of("fqdn"));

        return Builder.builder()
                .withFileSystemConfigurationView(fileSystemConfigurationsView)
                .withHostgroupViews(Set.of(gateway, master, worker))
                .withStackType(StackType.DATALAKE)
                .withGeneralClusterConfigs(generalClusterConfigs);
    }

    private TemplatePreparationObject.Builder getTemplatePreparationObjectForAws(boolean includeLocations, boolean above721) {
        return getTemplatePreparationObjectForAws(includeLocations, above721, false);
    }

    private TemplatePreparationObject.Builder getTemplatePreparationObjectForAws(boolean includeLocations, boolean above721, boolean includeBackup) {
        HostgroupView gateway = new HostgroupView("gateway", 1, InstanceGroupType.GATEWAY, Set.of("g"));
        HostgroupView master = new HostgroupView("master", 1, InstanceGroupType.CORE, Set.of("m1", "m2"));
        HostgroupView worker = new HostgroupView("worker", 2, InstanceGroupType.CORE, Set.of("w1", "w2", "w3"));

        List<StorageLocationView> locations = new ArrayList<>();

        if (includeLocations) {
            locations.add(new StorageLocationView(getRangerAuditCloudStorageDirAws()));
            if (above721) {
                locations.add(new StorageLocationView(buildStorageLocation("hive.metastore.warehouse.dir",
                        "s3a://bucket/warehouse/tablespace/managed/hive")));
                locations.add(new StorageLocationView(buildStorageLocation("hive.repl.replica.functions.root.dir",
                        "s3a://bucket/hive_replica_functions_dir")));
                locations.add(new StorageLocationView(buildStorageLocation("hive.metastore.warehouse.external.dir",
                        "s3a://bucket/warehouse/tablespace/external/hive")));
                locations.add(new StorageLocationView(buildStorageLocation("hbase.rootdir",
                        "s3a://bucket/hbase")));
                if (includeBackup) {
                    locations.add(new StorageLocationView(buildStorageLocation(DEFAULT_BACKUP_DIR,
                            "s3a://bucket/backup/location")));
                }
            }
        }
        S3FileSystemConfigurationsView fileSystemConfigurationsView =
                new S3FileSystemConfigurationsView(new S3FileSystem(), locations, false);

        GeneralClusterConfigs generalClusterConfigs = new GeneralClusterConfigs();
        generalClusterConfigs.setPrimaryGatewayInstanceDiscoveryFQDN(Optional.of("fqdn"));

        return Builder.builder()
                .withCloudPlatform(CloudPlatform.AWS)
                .withFileSystemConfigurationView(fileSystemConfigurationsView)
                .withHostgroupViews(Set.of(gateway, master, worker))
                .withStackType(StackType.DATALAKE)
                .withGeneralClusterConfigs(generalClusterConfigs);
    }

    protected StorageLocation getRangerAuditCloudStorageDirAws() {
        StorageLocation rangerAuditLogLocation = new StorageLocation();
        rangerAuditLogLocation.setProperty("ranger_plugin_hdfs_audit_url");
        rangerAuditLogLocation.setValue("s3a://bucket/ranger/audit");
        return rangerAuditLogLocation;
    }

    protected StorageLocation getRangerAuditCloudStorageDirAzure() {
        StorageLocation rangerAuditLogLocation = new StorageLocation();
        rangerAuditLogLocation.setProperty("ranger_plugin_hdfs_audit_url");
        rangerAuditLogLocation.setValue("abfs://data@your-san.dfs.core.windows.net/ranger/audit");
        return rangerAuditLogLocation;
    }

    protected StorageLocation getRangerAuditCloudStorageDirGcp() {
        StorageLocation rangerAuditLogLocation = new StorageLocation();
        rangerAuditLogLocation.setProperty("ranger_plugin_hdfs_audit_url");
        rangerAuditLogLocation.setValue("gs://perf-team-west2-bucket/ranger/audit");
        return rangerAuditLogLocation;
    }

    protected StorageLocation buildStorageLocation(String property, String value) {
        StorageLocation rangerAuditLogLocation = new StorageLocation();
        rangerAuditLogLocation.setProperty(property);
        rangerAuditLogLocation.setValue(value);
        return rangerAuditLogLocation;
    }

}
