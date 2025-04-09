package com.sequenceiq.cloudbreak.cmtemplate.configproviders.hbase;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.StorageLocation;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.filesystem.StorageLocationView;
import com.sequenceiq.cloudbreak.template.filesystem.s3.S3FileSystemConfigurationsView;
import com.sequenceiq.cloudbreak.template.model.GeneralClusterConfigs;
import com.sequenceiq.cloudbreak.template.views.BlueprintView;
import com.sequenceiq.cloudbreak.template.views.HostgroupView;
import com.sequenceiq.cloudbreak.template.views.SharedServiceConfigsView;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;
import com.sequenceiq.common.api.filesystem.S3FileSystem;
import com.sequenceiq.common.api.type.InstanceGroupType;

public class AbstractHbaseConfigProviderTest {

    protected TemplatePreparationObject getTemplatePreparationObject(boolean includeLocations, boolean datalakeCluster) {
        return getTemplatePreparationObject(includeLocations, datalakeCluster, "7.2.1");
    }

    protected TemplatePreparationObject getTemplatePreparationObject(boolean includeLocations, boolean datalakeCluster, String cdhVersion) {
        return getTemplatePreparationObject(includeLocations, datalakeCluster, cdhVersion, Map.of(), CloudPlatform.AWS);
    }

    protected TemplatePreparationObject getTemplatePreparationObject(boolean includeLocations, boolean datalakeCluster,
                                                                    String cdhVersion, Map<String, String> defaultTags,
                                                                    CloudPlatform cloudPlatform) {
        HostgroupView master = new HostgroupView("master", 1, InstanceGroupType.GATEWAY, 1);
        HostgroupView worker = new HostgroupView("worker", 2, InstanceGroupType.CORE, 2);

        List<StorageLocationView> locations = new ArrayList<>();
        if (includeLocations) {
            StorageLocation hbaseRootDir = new StorageLocation();
            hbaseRootDir.setProperty("hbase.rootdir");
            hbaseRootDir.setValue("s3a://bucket/cluster1/hbase");
            locations.add(new StorageLocationView(hbaseRootDir));
        }
        S3FileSystemConfigurationsView fileSystemConfigurationsView =
                new S3FileSystemConfigurationsView(new S3FileSystem(), locations, false);

        SharedServiceConfigsView sharedServicesConfigsView = new SharedServiceConfigsView();
        sharedServicesConfigsView.setDatalakeCluster(datalakeCluster);

        String inputJson = getBlueprintText("input/clouderamanager.bp");
        CmTemplateProcessor cmTemplateProcessor = new CmTemplateProcessor(inputJson);
        cmTemplateProcessor.setCdhVersion(cdhVersion);

        GeneralClusterConfigs generalClusterConfigs = new GeneralClusterConfigs();
        generalClusterConfigs.setAccountId(Optional.of("1234"));

        return TemplatePreparationObject.Builder.builder().withFileSystemConfigurationView(fileSystemConfigurationsView)
                .withBlueprintView(new BlueprintView(inputJson, "", "", null, cmTemplateProcessor))
                .withSharedServiceConfigs(sharedServicesConfigsView)
                .withHostgroupViews(Set.of(master, worker))
                .withGeneralClusterConfigs(generalClusterConfigs)
                .withDefaultTags(defaultTags)
                .withCloudPlatform(cloudPlatform)
                .build();
    }

    protected String getBlueprintText(String path) {
        return FileReaderUtils.readFileFromClasspathQuietly(path);
    }

}
