package com.sequenceiq.cloudbreak.cmtemplate.configproviders.ranger;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.sequenceiq.cloudbreak.common.type.InstanceGroupType;
import com.sequenceiq.cloudbreak.common.type.filesystem.S3FileSystem;
import com.sequenceiq.cloudbreak.domain.StorageLocation;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject.Builder;
import com.sequenceiq.cloudbreak.template.filesystem.StorageLocationView;
import com.sequenceiq.cloudbreak.template.filesystem.s3.S3FileSystemConfigurationsView;
import com.sequenceiq.cloudbreak.template.views.HostgroupView;

@RunWith(MockitoJUnitRunner.class)
public class RangerCloudStorageServiceConfigProviderTest {

    private final RangerCloudStorageServiceConfigProvider underTest = new RangerCloudStorageServiceConfigProvider();

    @Test
    public void testGetRangerCloudStorageServiceConfigs() {
        TemplatePreparationObject preparationObject = getTemplatePreparationObject(true);
        List<ApiClusterTemplateConfig> serviceConfigs = underTest.getServiceConfigs(preparationObject);

        assertEquals(1, serviceConfigs.size());
        assertEquals("ranger_plugin_hdfs_audit_url", serviceConfigs.get(0).getName());
        assertEquals("s3a://bucket/ranger/auditlogs", serviceConfigs.get(0).getValue());
    }

    @Test
    public void testGetRangerCloudStorageServiceConfigsWhenNoStorageProvided() {
        TemplatePreparationObject preparationObject = getTemplatePreparationObject(false);

        List<ApiClusterTemplateConfig> serviceConfigs = underTest.getServiceConfigs(preparationObject);
        assertEquals(0, serviceConfigs.size());
    }

    private TemplatePreparationObject getTemplatePreparationObject(boolean includeLocations) {
        HostgroupView master = new HostgroupView("master", 1, InstanceGroupType.GATEWAY, 1);
        HostgroupView worker = new HostgroupView("worker", 2, InstanceGroupType.CORE, 2);

        List<StorageLocationView> locations = new ArrayList<>();

        if (includeLocations) {
            locations.add(new StorageLocationView(getRangerAuditCloudStorageDir()));
        }
        S3FileSystemConfigurationsView fileSystemConfigurationsView =
                new S3FileSystemConfigurationsView(new S3FileSystem(), locations, false);


        return Builder.builder().withFileSystemConfigurationView(fileSystemConfigurationsView)
                .withHostgroupViews(Set.of(master, worker)).build();
    }

    protected StorageLocation getRangerAuditCloudStorageDir() {
        StorageLocation rangerAuditLogLocation = new StorageLocation();
        rangerAuditLogLocation.setProperty("ranger_plugin_hdfs_audit_url");
        rangerAuditLogLocation.setValue("s3a://bucket/ranger/auditlogs");
        return rangerAuditLogLocation;
    }
}