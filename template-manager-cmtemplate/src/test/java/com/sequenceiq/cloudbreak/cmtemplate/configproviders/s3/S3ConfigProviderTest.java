package com.sequenceiq.cloudbreak.cmtemplate.configproviders.s3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.TestUtil;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerRepo;
import com.sequenceiq.cloudbreak.cloud.storage.LocationHelper;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.S3ExpressBucketValidator;
import com.sequenceiq.cloudbreak.domain.StorageLocation;
import com.sequenceiq.cloudbreak.domain.stack.cluster.gateway.Gateway;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject.Builder;
import com.sequenceiq.cloudbreak.template.filesystem.BaseFileSystemConfigurationsView;
import com.sequenceiq.cloudbreak.template.filesystem.StorageLocationView;
import com.sequenceiq.cloudbreak.template.filesystem.adls.AdlsFileSystemConfigurationsView;
import com.sequenceiq.cloudbreak.template.filesystem.s3.S3FileSystemConfigurationsView;
import com.sequenceiq.cloudbreak.template.model.GeneralClusterConfigs;
import com.sequenceiq.cloudbreak.template.views.BlueprintView;
import com.sequenceiq.cloudbreak.template.views.HostgroupView;
import com.sequenceiq.cloudbreak.template.views.PlacementView;
import com.sequenceiq.common.api.filesystem.AdlsFileSystem;
import com.sequenceiq.common.api.filesystem.S3FileSystem;
import com.sequenceiq.common.api.type.InstanceGroupType;

@ExtendWith(MockitoExtension.class)
public class S3ConfigProviderTest {

    @Mock
    private LocationHelper locationHelper;

    @Mock
    private S3ExpressBucketValidator s3ExpressBucketValidator;

    @InjectMocks
    private S3ConfigProvider underTest;

    @Mock
    private ClouderaManagerProduct products;

    void setUp() {
        ReflectionTestUtils.setField(s3ExpressBucketValidator, "s3expressBucketSuffixPattern", "--x-s3");
        doCallRealMethod().when(s3ExpressBucketValidator).isS3ExpressBucket(anyString());
    }

    @Test
    void testGetHdfsServiceConfigsDoesNotContainsS3guard() {
        TemplatePreparationObject preparationObject = getTemplatePreparationObject(true, true, false, "", "7.2.17");
        StringBuilder sb = new StringBuilder();

        underTest.getServiceConfigs(preparationObject, sb);

        assertFalse(sb.toString().contains("s3guard"));
    }

    @Test
    void testGetHdfsServiceConfigsWithAuthoriativeWarehousePath() {
        setUp();
        when(locationHelper.parseS3BucketName(anyString())).thenCallRealMethod();
        TemplatePreparationObject preparationObject = getTemplatePreparationObject(true, true, true, "", "7.2.17");
        StringBuilder sb = new StringBuilder();

        underTest.getServiceConfigs(preparationObject, sb);

        assertEquals(
                "<property><name>fs.s3a.bucket.bucket-first.endpoint</name><value>s3.region.amazonaws.com</value></property>" +
                        "<property><name>fs.s3a.bucket.bucket-second.endpoint</name><value>s3.region.amazonaws.com</value></property>", sb.toString());
    }

    @Test
    void testGetHdfsServiceConfigsWithS3ExpressGuardIncompatibleVersion() {
        setUp();
        when(locationHelper.parseS3BucketName(anyString())).thenCallRealMethod();
        TemplatePreparationObject preparationObject = getTemplatePreparationObject(true, true, true, "--x-s3", "7.2.17");
        StringBuilder sb = new StringBuilder();
        doReturn(false).when(s3ExpressBucketValidator).validateVersionForS3ExpressBucket(any());
        RuntimeException exception = assertThrows(RuntimeException.class, () -> underTest.getServiceConfigs(preparationObject, sb));

        assertEquals("S3 Express buckets are only supported for non-gov CDH versions >= 7.2.18", exception.getMessage());
    }

    private TemplatePreparationObject getTemplatePreparationObject(boolean useS3FileSystem, boolean fillDynamoTableName, boolean includeLocations,
            String bucketNameSuffix, String cdhVersion) {
        HostgroupView master = new HostgroupView("master", 1, InstanceGroupType.GATEWAY, 1);
        HostgroupView worker = new HostgroupView("worker", 2, InstanceGroupType.CORE, 2);

        List<StorageLocationView> locations = new ArrayList<>();

        if (includeLocations) {
            locations.add(new StorageLocationView(getStorageLocation("hive.metastore.warehouse.dir",
                    "s3a://bucket-first" + bucketNameSuffix + "/warehouse/managed")));
            locations.add(new StorageLocationView(getStorageLocation("hive.metastore.warehouse.external.dir",
                    "s3a://bucket-first" + bucketNameSuffix + "/warehouse/external")));
            locations.add(new StorageLocationView(getStorageLocation("ranger_plugin_hdfs_audit_url",
                    "s3a://bucket-second" + bucketNameSuffix + "/ranger/audit")));
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

        Gateway gateway = TestUtil.gatewayEnabledWithExposedKnoxServices("NAMENODE");

        PlacementView placementView = new PlacementView("region", "az");

        GeneralClusterConfigs generalClusterConfigs = new GeneralClusterConfigs();
        generalClusterConfigs.setGovCloud(false);

        ClouderaManagerRepo cm = mock(ClouderaManagerRepo.class);

        BlueprintView blueprintView = new BlueprintView();
        blueprintView.setVersion(cdhVersion);

        return Builder.builder().withFileSystemConfigurationView(fileSystemConfigurationsView)
                .withHostgroupViews(Set.of(master, worker))
                .withGateway(gateway, "/cb/secret/signkey", new HashSet<>())
                .withPlacementView(placementView)
                .withGeneralClusterConfigs(generalClusterConfigs)
                .withDefaultTags(Map.of("apple", "apple1"))
                .withBlueprintView(blueprintView)
                .withProductDetails(cm, List.of(products))
                .build();
    }

    protected StorageLocation getStorageLocation(String property, String value) {
        StorageLocation hmsWarehouseDir = new StorageLocation();
        hmsWarehouseDir.setProperty(property);
        hmsWarehouseDir.setValue(value);
        return hmsWarehouseDir;
    }
}