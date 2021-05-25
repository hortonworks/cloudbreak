package com.sequenceiq.cloudbreak.cmtemplate.configproviders.hbase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.domain.StorageLocation;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject.Builder;
import com.sequenceiq.cloudbreak.template.filesystem.StorageLocationView;
import com.sequenceiq.cloudbreak.template.filesystem.s3.S3FileSystemConfigurationsView;
import com.sequenceiq.cloudbreak.template.views.BlueprintView;
import com.sequenceiq.cloudbreak.template.views.HostgroupView;
import com.sequenceiq.cloudbreak.template.views.SharedServiceConfigsView;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;
import com.sequenceiq.common.api.filesystem.S3FileSystem;
import com.sequenceiq.common.api.type.InstanceGroupType;

@RunWith(MockitoJUnitRunner.class)
public class HbaseCloudStorageServiceConfigProviderTest {

    @InjectMocks
    private final HbaseCloudStorageServiceConfigProvider underTest = new HbaseCloudStorageServiceConfigProvider();

    @Mock
    private EntitlementService entitlementService;

    @Before
    public void setUp() {
        if (StringUtils.isEmpty(ThreadBasedUserCrnProvider.getUserCrn())) {
            ThreadBasedUserCrnProvider.setUserCrn("crn:cdp:iam:us-west-1:1234:user:1");
        }
        when(entitlementService.sdxHbaseCloudStorageEnabled(anyString())).thenReturn(true);
    }

    @Test
    public void testGetHbaseStorageServiceConfigsWhenAttachedCluster() {
        TemplatePreparationObject preparationObject = getTemplatePreparationObject(true, false);
        String inputJson = getBlueprintText("input/clouderamanager.bp");
        CmTemplateProcessor cmTemplateProcessor = new CmTemplateProcessor(inputJson);

        List<ApiClusterTemplateConfig> serviceConfigs = underTest.getServiceConfigs(cmTemplateProcessor, preparationObject);

        assertEquals(1, serviceConfigs.size());
        assertEquals("hdfs_rootdir", serviceConfigs.get(0).getName());
        assertEquals("s3a://bucket/cluster1/hbase", serviceConfigs.get(0).getValue());
    }

    @Test
    public void testGetHbaseStorageServiceConfigsWhenDataLake721() {
        TemplatePreparationObject preparationObject = getTemplatePreparationObject(true, true, "7.2.1");
        String inputJson = getBlueprintText("input/clouderamanager.bp");
        CmTemplateProcessor cmTemplateProcessor = new CmTemplateProcessor(inputJson);

        List<ApiClusterTemplateConfig> serviceConfigs = underTest.getServiceConfigs(cmTemplateProcessor, preparationObject);

        assertEquals(1, serviceConfigs.size());
        assertEquals("hdfs_rootdir", serviceConfigs.get(0).getName());
        assertEquals("s3a://bucket/cluster1/hbase", serviceConfigs.get(0).getValue());
    }

    @Test
    public void testGetHbaseStorageServiceConfigsWhenDataLake722() {
        TemplatePreparationObject preparationObject = getTemplatePreparationObject(true, true, "7.2.2");
        String inputJson = getBlueprintText("input/clouderamanager.bp");
        CmTemplateProcessor cmTemplateProcessor = new CmTemplateProcessor(inputJson);

        List<ApiClusterTemplateConfig> serviceConfigs = underTest.getServiceConfigs(cmTemplateProcessor, preparationObject);

        assertEquals(1, serviceConfigs.size());
        assertEquals("hdfs_rootdir", serviceConfigs.get(0).getName());
        assertEquals("s3a://bucket/cluster1/hbase", serviceConfigs.get(0).getValue());
    }

    @Test
    public void testGetHbaseServiceConfigsWhenNoStorageConfiguredWithAttachedCluster() {
        TemplatePreparationObject preparationObject = getTemplatePreparationObject(false, false);
        String inputJson = getBlueprintText("input/clouderamanager.bp");
        CmTemplateProcessor cmTemplateProcessor = new CmTemplateProcessor(inputJson);

        List<ApiClusterTemplateConfig> serviceConfigs = underTest.getServiceConfigs(cmTemplateProcessor, preparationObject);
        assertEquals(0, serviceConfigs.size());
    }

    @Test
    public void testGetHbaseServiceConfigsWhenNoStorageConfiguredWithDataLake() {
        TemplatePreparationObject preparationObject = getTemplatePreparationObject(false, true);
        String inputJson = getBlueprintText("input/clouderamanager.bp");
        CmTemplateProcessor cmTemplateProcessor = new CmTemplateProcessor(inputJson);

        List<ApiClusterTemplateConfig> serviceConfigs = underTest.getServiceConfigs(cmTemplateProcessor, preparationObject);
        assertEquals(0, serviceConfigs.size());
    }

    @Test
    public void testConfigurationNotNeededWhenDataLake721() {
        TemplatePreparationObject preparationObject = getTemplatePreparationObject(true, true, "7.2.1");
        String inputJson = getBlueprintText("input/clouderamanager.bp");
        CmTemplateProcessor cmTemplateProcessor = new CmTemplateProcessor(inputJson);

        boolean configurationNeeded = underTest.isConfigurationNeeded(cmTemplateProcessor, preparationObject);
        assertFalse(configurationNeeded);
    }

    @Test
    public void testConfigurationNotNeededWhenDataLake726() {
        TemplatePreparationObject preparationObject = getTemplatePreparationObject(true, true, "7.2.6");
        String inputJson = getBlueprintText("input/clouderamanager.bp");
        CmTemplateProcessor cmTemplateProcessor = new CmTemplateProcessor(inputJson);

        boolean configurationNeeded = underTest.isConfigurationNeeded(cmTemplateProcessor, preparationObject);
        assertFalse(configurationNeeded);
    }

    @Test
    public void testConfigurationNeededWhenDatalake727() {
        TemplatePreparationObject preparationObject = getTemplatePreparationObject(true, true, "7.2.7");
        String inputJson = getBlueprintText("input/clouderamanager.bp");
        CmTemplateProcessor cmTemplateProcessor = new CmTemplateProcessor(inputJson);

        boolean configurationNeeded = underTest.isConfigurationNeeded(cmTemplateProcessor, preparationObject);
        assertTrue(configurationNeeded);
    }

    @Test
    public void testConfigurationNeededWhenDatalake728() {
        TemplatePreparationObject preparationObject = getTemplatePreparationObject(true, true, "7.2.8");
        String inputJson = getBlueprintText("input/clouderamanager.bp");
        CmTemplateProcessor cmTemplateProcessor = new CmTemplateProcessor(inputJson);

        boolean configurationNeeded = underTest.isConfigurationNeeded(cmTemplateProcessor, preparationObject);
        assertTrue(configurationNeeded);
    }

    @Test
    public void testIsConfigurationNeededWhenAttachedCluster() {
        TemplatePreparationObject preparationObject = getTemplatePreparationObject(true, false);
        String inputJson = getBlueprintText("input/clouderamanager.bp");
        CmTemplateProcessor cmTemplateProcessor = new CmTemplateProcessor(inputJson);

        boolean configurationNeeded = underTest.isConfigurationNeeded(cmTemplateProcessor, preparationObject);
        assertTrue(configurationNeeded);
    }

    private TemplatePreparationObject getTemplatePreparationObject(boolean includeLocations, boolean datalakeCluster) {
        return getTemplatePreparationObject(includeLocations, datalakeCluster, "7.2.1");
    }

    private TemplatePreparationObject getTemplatePreparationObject(boolean includeLocations, boolean datalakeCluster, String cdhVersion) {
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

        return Builder.builder().withFileSystemConfigurationView(fileSystemConfigurationsView)
                .withBlueprintView(new BlueprintView(inputJson, "", "", cmTemplateProcessor))
                .withSharedServiceConfigs(sharedServicesConfigsView)
                .withHostgroupViews(Set.of(master, worker)).build();
    }

    private String getBlueprintText(String path) {
        return FileReaderUtils.readFileFromClasspathQuietly(path);
    }
}