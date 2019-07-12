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
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.hdfs.HdfsRoles;
import com.sequenceiq.common.api.type.InstanceGroupType;
import com.sequenceiq.common.api.filesystem.S3FileSystem;
import com.sequenceiq.cloudbreak.domain.StorageLocation;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject.Builder;
import com.sequenceiq.cloudbreak.template.filesystem.StorageLocationView;
import com.sequenceiq.cloudbreak.template.filesystem.s3.S3FileSystemConfigurationsView;
import com.sequenceiq.cloudbreak.template.model.GeneralClusterConfigs;
import com.sequenceiq.cloudbreak.template.views.BlueprintView;
import com.sequenceiq.cloudbreak.template.views.HostgroupView;

@RunWith(MockitoJUnitRunner.class)
public class RangerCloudStorageServiceConfigProviderTest {

    private final RangerCloudStorageServiceConfigProvider underTest = new RangerCloudStorageServiceConfigProvider();

    @Test
    public void testGetRangerCloudStorageServiceConfigs() {
        CmTemplateProcessor templateProcessor = mock(CmTemplateProcessor.class);
        TemplatePreparationObject preparationObject = getTemplatePreparationObject(true)
                .withBlueprintView(mock(BlueprintView.class))
                .build();

        List<ApiClusterTemplateConfig> serviceConfigs = underTest.getServiceConfigs(templateProcessor, preparationObject);

        assertEquals(1, serviceConfigs.size());
        assertEquals("ranger_plugin_hdfs_audit_url", serviceConfigs.get(0).getName());
        assertEquals("s3a://bucket/ranger/auditlogs", serviceConfigs.get(0).getValue());
    }

    @Test
    public void defaultRangerHdfsAuditUrlWithNamenodeHA() {
        CmTemplateProcessor templateProcessor = mock(CmTemplateProcessor.class);
        BlueprintView blueprintView = mock(BlueprintView.class);
        when(blueprintView.getProcessor()).thenReturn(templateProcessor);
        when(templateProcessor.getHostGroupsWithComponent(HdfsRoles.NAMENODE)).thenReturn(Set.of("master"));
        when(templateProcessor.getRoleConfig(HdfsRoles.HDFS, HdfsRoles.NAMENODE, "dfs_federation_namenode_nameservice"))
                .thenReturn(Optional.of(config("dfs_federation_namenode_nameservice", "ns")));
        TemplatePreparationObject preparationObject = getTemplatePreparationObject(false)
                .withBlueprintView(blueprintView)
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
        TemplatePreparationObject preparationObject = getTemplatePreparationObject(false)
                .withBlueprintView(blueprintView)
                .build();

        List<ApiClusterTemplateConfig> serviceConfigs = underTest.getServiceConfigs(templateProcessor, preparationObject);

        assertEquals(1, serviceConfigs.size());
        assertEquals("ranger_plugin_hdfs_audit_url", serviceConfigs.get(0).getName());
        assertEquals("hdfs://g", serviceConfigs.get(0).getValue());
    }

    private TemplatePreparationObject.Builder getTemplatePreparationObject(boolean includeLocations) {
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
}