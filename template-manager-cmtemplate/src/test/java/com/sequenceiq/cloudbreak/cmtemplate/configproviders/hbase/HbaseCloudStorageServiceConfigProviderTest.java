package com.sequenceiq.cloudbreak.cmtemplate.configproviders.hbase;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.StorageLocation;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject.Builder;
import com.sequenceiq.cloudbreak.template.filesystem.StorageLocationView;
import com.sequenceiq.cloudbreak.template.filesystem.s3.S3FileSystemConfigurationsView;
import com.sequenceiq.cloudbreak.template.model.GeneralClusterConfigs;
import com.sequenceiq.cloudbreak.template.views.BlueprintView;
import com.sequenceiq.cloudbreak.template.views.HostgroupView;
import com.sequenceiq.cloudbreak.template.views.SharedServiceConfigsView;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;
import com.sequenceiq.common.api.filesystem.S3FileSystem;
import com.sequenceiq.common.api.type.InstanceGroupType;

@ExtendWith(MockitoExtension.class)
public class HbaseCloudStorageServiceConfigProviderTest {

    @InjectMocks
    private final HbaseCloudStorageServiceConfigProvider underTest = new HbaseCloudStorageServiceConfigProvider();

    @Mock
    private EntitlementService entitlementService;

    @BeforeEach
    public void setUp() {
        if (StringUtils.isEmpty(ThreadBasedUserCrnProvider.getUserCrn())) {
            ThreadBasedUserCrnProvider.setUserCrn("crn:cdp:iam:us-west-1:1234:user:1");
        }
    }

    static Object[][] templateConfigParameters() {
        return new Object[][]{
            { true, false, "7.2.1", CloudPlatform.AWS, false, true, true, 1, "hdfs_rootdir", "s3a://bucket/cluster1/hbase" },
            { true, false, "7.2.1", CloudPlatform.AWS, false, false, true, 1, "hdfs_rootdir", "s3a://bucket/cluster1/hbase" },
            { true, true, "7.2.1", CloudPlatform.AWS, false, true, false, 1, "hdfs_rootdir", "s3a://bucket/cluster1/hbase" },
            { false, false, "7.2.1", CloudPlatform.AWS, false, true, true, 0, "", "" },
            { false, true, "7.2.1", CloudPlatform.AWS, false, true, false, 0, "", "" },
            { true, true, "7.2.6", CloudPlatform.AWS, false, true, false, 1, "hdfs_rootdir", "s3a://bucket/cluster1/hbase" },
            { true, true, "7.2.7", CloudPlatform.AWS, false, true, true, 1, "hdfs_rootdir", "s3a://bucket/cluster1/hbase" },
            { true, true, "7.2.8", CloudPlatform.AWS, false, true, true, 1, "hdfs_rootdir", "s3a://bucket/cluster1/hbase" },
            { true, true, "7.2.7", CloudPlatform.AWS, false, false, true, 1, "hdfs_rootdir", "s3a://bucket/cluster1/hbase" },
            { true, true, "7.2.7", CloudPlatform.AWS, true, false, false, 1, "hdfs_rootdir", "s3a://bucket/cluster1/hbase" },
            { true, true, "7.2.7", CloudPlatform.AZURE, false, false, false, 1, "hdfs_rootdir", "s3a://bucket/cluster1/hbase" },
            { true, true, "7.2.7", CloudPlatform.AZURE, false, true, true, 1, "hdfs_rootdir", "s3a://bucket/cluster1/hbase" },
        };
    }

    @ParameterizedTest
    @MethodSource("templateConfigParameters")
    public void testGetHbaseStorageServiceConfigs(boolean includeLocation, boolean datalakeCluster, String cdhVersion,
        CloudPlatform cloudPlatform, boolean enableRaz, boolean enableHbaseCloudStorage, boolean configurationNeeded, int configurationSize,
        String configName, String configValue) {
        when(entitlementService.sdxHbaseCloudStorageEnabled(anyString())).thenReturn(enableHbaseCloudStorage);
        TemplatePreparationObject preparationObject = getTemplatePreparationObject(includeLocation, datalakeCluster, cdhVersion, cloudPlatform, enableRaz);
        String inputJson = getBlueprintText("input/clouderamanager.bp");
        CmTemplateProcessor cmTemplateProcessor = new CmTemplateProcessor(inputJson);

        List<ApiClusterTemplateConfig> serviceConfigs = underTest.getServiceConfigs(cmTemplateProcessor, preparationObject);

        assertEquals(configurationSize, serviceConfigs.size());

        if (configurationSize > 0) {
            assertEquals(configName, serviceConfigs.get(0).getName());
            assertEquals(configValue, serviceConfigs.get(0).getValue());
        }

        boolean ifConfigurationNeeded = underTest.isConfigurationNeeded(cmTemplateProcessor, preparationObject);
        assertEquals(configurationNeeded, ifConfigurationNeeded);
    }

    private TemplatePreparationObject getTemplatePreparationObject(boolean includeLocations, boolean datalakeCluster, String cdhVersion,
            CloudPlatform cloudPlatform, boolean enableRaz) {
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
        generalClusterConfigs.setEnableRangerRaz(enableRaz);

        return Builder.builder().withFileSystemConfigurationView(fileSystemConfigurationsView)
                .withBlueprintView(new BlueprintView(inputJson, "", "", cmTemplateProcessor))
                .withSharedServiceConfigs(sharedServicesConfigsView)
                .withHostgroupViews(Set.of(master, worker))
                .withCloudPlatform(cloudPlatform)
                .withGeneralClusterConfigs(generalClusterConfigs)
                .build();
    }

    private String getBlueprintText(String path) {
        return FileReaderUtils.readFileFromClasspathQuietly(path);
    }
}