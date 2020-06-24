package com.sequenceiq.cloudbreak.cmtemplate.configproviders.ranger;

import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils.config;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerRepo;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.hdfs.HdfsRoles;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.StorageLocation;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject.Builder;
import com.sequenceiq.cloudbreak.template.filesystem.StorageLocationView;
import com.sequenceiq.cloudbreak.template.filesystem.adlsgen2.AdlsGen2FileSystemConfigurationsView;
import com.sequenceiq.cloudbreak.template.filesystem.s3.S3FileSystemConfigurationsView;
import com.sequenceiq.cloudbreak.template.model.GeneralClusterConfigs;
import com.sequenceiq.cloudbreak.template.views.BlueprintView;
import com.sequenceiq.cloudbreak.template.views.HostgroupView;
import com.sequenceiq.common.api.filesystem.AdlsGen2FileSystem;
import com.sequenceiq.common.api.filesystem.S3FileSystem;
import com.sequenceiq.common.api.type.InstanceGroupType;

@RunWith(MockitoJUnitRunner.class)
public class RangerCloudStorageServiceConfigProviderTest {

    private final RangerCloudStorageServiceConfigProvider underTest = new RangerCloudStorageServiceConfigProvider();

    @Test
    public void testGetRangerAWSCloudStorageServiceConfigs() {
        CmTemplateProcessor templateProcessor = mock(CmTemplateProcessor.class);
        TemplatePreparationObject preparationObject = getTemplatePreparationObjectForAWS(true)
                .withBlueprintView(mock(BlueprintView.class))
                .withProductDetails(new ClouderaManagerRepo().withVersion("7.2.1"), List.of())
                .build();

        List<ApiClusterTemplateConfig> serviceConfigs = underTest.getServiceConfigs(templateProcessor, preparationObject);

        assertEquals(1, serviceConfigs.size());
        assertEquals("ranger_plugin_hdfs_audit_url", serviceConfigs.get(0).getName());
        assertEquals("s3a://bucket/ranger/auditlogs", serviceConfigs.get(0).getValue());
    }

    @Test
    public void testGetRangerAzureCloudStorageServiceConfigs() {
        CmTemplateProcessor templateProcessor = mock(CmTemplateProcessor.class);
        GeneralClusterConfigs generalClusterConfigs = new GeneralClusterConfigs();
        generalClusterConfigs.setEnableRangerRaz(true);
        TemplatePreparationObject preparationObject = getTemplatePreparationObjectForAzure(true)
                .withBlueprintView(mock(BlueprintView.class))
                .withCloudPlatform(CloudPlatform.AZURE)
                .withGeneralClusterConfigs(generalClusterConfigs)
                .withProductDetails(new ClouderaManagerRepo().withVersion("7.2.1"), List.of())
                .build();

        List<ApiClusterTemplateConfig> serviceConfigs = underTest.getServiceConfigs(templateProcessor, preparationObject);

        assertEquals(2, serviceConfigs.size());
        assertEquals("ranger_plugin_hdfs_audit_url", serviceConfigs.get(0).getName());
        assertEquals("abfs://data@your-san.dfs.core.windows.net/ranger/audit/", serviceConfigs.get(0).getValue());
        assertEquals("cloud_data_location_url", serviceConfigs.get(1).getName());
        assertEquals("abfs://data@your-san.dfs.core.windows.net/", serviceConfigs.get(1).getValue());
    }

    @Test
    public void testGetRangerAzureCloudStorageServiceConfigsNoRaz() {
        CmTemplateProcessor templateProcessor = mock(CmTemplateProcessor.class);
        GeneralClusterConfigs generalClusterConfigs = new GeneralClusterConfigs();
        generalClusterConfigs.setEnableRangerRaz(false);
        TemplatePreparationObject preparationObject = getTemplatePreparationObjectForAzure(true)
                .withBlueprintView(mock(BlueprintView.class))
                .withCloudPlatform(CloudPlatform.AZURE)
                .withGeneralClusterConfigs(generalClusterConfigs)
                .withProductDetails(new ClouderaManagerRepo().withVersion("7.2.1"), List.of())
                .build();

        List<ApiClusterTemplateConfig> serviceConfigs = underTest.getServiceConfigs(templateProcessor, preparationObject);

        assertEquals(1, serviceConfigs.size());
        assertEquals("ranger_plugin_hdfs_audit_url", serviceConfigs.get(0).getName());
        assertEquals("abfs://data@your-san.dfs.core.windows.net/ranger/audit/", serviceConfigs.get(0).getValue());
    }

    @Test
    public void testGetRangerAzure720CloudStorageServiceConfigs() {
        CmTemplateProcessor templateProcessor = mock(CmTemplateProcessor.class);
        TemplatePreparationObject preparationObject = getTemplatePreparationObjectForAzure(false)
                .withBlueprintView(mock(BlueprintView.class))
                .withProductDetails(new ClouderaManagerRepo().withVersion("7.2.0"), List.of())
                .build();

        List<ApiClusterTemplateConfig> serviceConfigs = underTest.getServiceConfigs(templateProcessor, preparationObject);

        assertEquals(1, serviceConfigs.size());
        assertEquals("ranger_plugin_hdfs_audit_url", serviceConfigs.get(0).getName());
        assertEquals("abfs://data@your-san.dfs.core.windows.net/ranger/audit/", serviceConfigs.get(0).getValue());
    }

    @Test
    public void defaultRangerHdfsAuditUrlWithNamenodeHA() {
        CmTemplateProcessor templateProcessor = mock(CmTemplateProcessor.class);
        BlueprintView blueprintView = mock(BlueprintView.class);
        when(blueprintView.getProcessor()).thenReturn(templateProcessor);
        when(templateProcessor.getHostGroupsWithComponent(HdfsRoles.NAMENODE)).thenReturn(Set.of("master"));
        when(templateProcessor.getRoleConfig(HdfsRoles.HDFS, HdfsRoles.NAMENODE, "dfs_federation_namenode_nameservice"))
                .thenReturn(Optional.of(config("dfs_federation_namenode_nameservice", "ns")));
        TemplatePreparationObject preparationObject = getTemplatePreparationObjectForAWS(false)
                .withBlueprintView(blueprintView)
                .withProductDetails(new ClouderaManagerRepo().withVersion("7.2.0"), List.of())
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
        TemplatePreparationObject preparationObject = getTemplatePreparationObjectForAWS(false)
                .withBlueprintView(blueprintView)
                .withProductDetails(new ClouderaManagerRepo().withVersion("7.2.0"), List.of())
                .build();

        List<ApiClusterTemplateConfig> serviceConfigs = underTest.getServiceConfigs(templateProcessor, preparationObject);

        assertEquals(1, serviceConfigs.size());
        assertEquals("ranger_plugin_hdfs_audit_url", serviceConfigs.get(0).getName());
        assertEquals("hdfs://g", serviceConfigs.get(0).getValue());
    }

    private TemplatePreparationObject.Builder getTemplatePreparationObjectForAzure(boolean above721) {
        HostgroupView gateway = new HostgroupView("gateway", 1, InstanceGroupType.GATEWAY, Set.of("g"));
        HostgroupView master = new HostgroupView("master", 1, InstanceGroupType.CORE, Set.of("m1", "m2"));
        HostgroupView worker = new HostgroupView("worker", 2, InstanceGroupType.CORE, Set.of("w1", "w2", "w3"));

        List<StorageLocationView> locations = new ArrayList<>();

        locations.add(new StorageLocationView(getRangerAuditCloudStorageDirAzure()));
        if (above721) {
            locations.add(new StorageLocationView(getRangerDataCloudStorageDir()));
        }
        AdlsGen2FileSystemConfigurationsView fileSystemConfigurationsView =
                new AdlsGen2FileSystemConfigurationsView(new AdlsGen2FileSystem(), locations, false);

        GeneralClusterConfigs generalClusterConfigs =  new GeneralClusterConfigs();
        generalClusterConfigs.setPrimaryGatewayInstanceDiscoveryFQDN(Optional.of("fqdn"));

        return Builder.builder()
                .withFileSystemConfigurationView(fileSystemConfigurationsView)
                .withHostgroupViews(Set.of(gateway, master, worker))
                .withGeneralClusterConfigs(generalClusterConfigs);
    }

    private TemplatePreparationObject.Builder getTemplatePreparationObjectForAWS(boolean includeLocations) {
        HostgroupView gateway = new HostgroupView("gateway", 1, InstanceGroupType.GATEWAY, Set.of("g"));
        HostgroupView master = new HostgroupView("master", 1, InstanceGroupType.CORE, Set.of("m1", "m2"));
        HostgroupView worker = new HostgroupView("worker", 2, InstanceGroupType.CORE, Set.of("w1", "w2", "w3"));

        List<StorageLocationView> locations = new ArrayList<>();

        if (includeLocations) {
            locations.add(new StorageLocationView(getRangerAuditCloudStorageDir()));
        }
        S3FileSystemConfigurationsView fileSystemConfigurationsView =
                new S3FileSystemConfigurationsView(new S3FileSystem(), locations, false);

        GeneralClusterConfigs generalClusterConfigs =  new GeneralClusterConfigs();
        generalClusterConfigs.setPrimaryGatewayInstanceDiscoveryFQDN(Optional.of("fqdn"));

        return Builder.builder()
                .withFileSystemConfigurationView(fileSystemConfigurationsView)
                .withHostgroupViews(Set.of(gateway, master, worker))
                .withGeneralClusterConfigs(generalClusterConfigs);
    }

    protected StorageLocation getRangerAuditCloudStorageDir() {
        StorageLocation rangerAuditLogLocation = new StorageLocation();
        rangerAuditLogLocation.setProperty("ranger_plugin_hdfs_audit_url");
        rangerAuditLogLocation.setValue("s3a://bucket/ranger/auditlogs");
        return rangerAuditLogLocation;
    }

    protected StorageLocation getRangerAuditCloudStorageDirAzure() {
        StorageLocation rangerAuditLogLocation = new StorageLocation();
        rangerAuditLogLocation.setProperty("ranger_plugin_hdfs_audit_url");
        rangerAuditLogLocation.setValue("abfs://data@your-san.dfs.core.windows.net/ranger/audit/");
        return rangerAuditLogLocation;
    }

    protected StorageLocation getRangerDataCloudStorageDir() {
        StorageLocation rangerAuditLogLocation = new StorageLocation();
        rangerAuditLogLocation.setProperty("cloud_data_location_url");
        rangerAuditLogLocation.setValue("abfs://data@your-san.dfs.core.windows.net/");
        return rangerAuditLogLocation;
    }
}