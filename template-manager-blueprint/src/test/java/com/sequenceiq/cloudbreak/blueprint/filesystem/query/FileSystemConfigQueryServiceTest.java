package com.sequenceiq.cloudbreak.blueprint.filesystem.query;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.api.model.filesystem.FileSystemType;
import com.sequenceiq.cloudbreak.blueprint.BlueprintProcessorFactory;
import com.sequenceiq.cloudbreak.template.processor.BlueprintTextProcessor;
import com.sequenceiq.cloudbreak.template.filesystem.FileSystemConfigQueryObject;
import com.sequenceiq.cloudbreak.template.filesystem.FileSystemConfigQueryObject.Builder;
import com.sequenceiq.cloudbreak.service.CloudbreakResourceReaderService;
import com.sequenceiq.cloudbreak.template.filesystem.query.ConfigQueryEntry;
import com.sequenceiq.cloudbreak.blueprint.filesystem.FileSystemConfigQueryService;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;

@RunWith(MockitoJUnitRunner.class)
public class FileSystemConfigQueryServiceTest {

    private static final String BLUEPRINT_TEXT = "testblueprint";

    private static final String HIVE_METASTORE = "HIVE_METASTORE";

    private static final String RANGER_ADMIN = "RANGER_ADMIN";

    private static final String HIVE_SERVER = "HIVE_SERVER";

    private static final String CLUSTER_NAME = "bigCluster";

    private static final String STORAGE_NAME = "hwx-remote";

    @Mock
    private CloudbreakResourceReaderService cloudbreakResourceReaderService;

    @Mock
    private BlueprintProcessorFactory blueprintProcessorFactory;

    @InjectMocks
    private FileSystemConfigQueryService underTest;

    @Before
    public void before() throws IOException {
        String specifications = FileReaderUtils.readFileFromClasspath("definitions/cloud-storage-location-specification.json");
        when(cloudbreakResourceReaderService.resourceDefinition("cloud-storage-location-specification")).thenReturn(specifications);
        underTest.init();
    }

    @Test
    public void testWhenHiveMetasoreAndRangerAdminIsPresentedThenShouldReturnWithBothConfigs() {
        prepareBlueprintProcessorFactoryMock(HIVE_METASTORE, RANGER_ADMIN);
        FileSystemConfigQueryObject fileSystemConfigQueryObject = Builder.builder()
                .withStorageName(STORAGE_NAME)
                .withClusterName(CLUSTER_NAME)
                .withBlueprintText(BLUEPRINT_TEXT)
                .withFileSystemType(FileSystemType.ADLS.name())
                .build();
        Set<ConfigQueryEntry> bigCluster = underTest.queryParameters(fileSystemConfigQueryObject);

        Assert.assertEquals(2L, bigCluster.size());

        Optional<ConfigQueryEntry> hiveMetastore = serviceEntry(bigCluster, HIVE_METASTORE);
        Optional<ConfigQueryEntry> rangerAdmin = serviceEntry(bigCluster, RANGER_ADMIN);

        Assert.assertTrue(hiveMetastore.isPresent());
        Assert.assertTrue(rangerAdmin.isPresent());

        Assert.assertEquals("default-account-name.azuredatalakestore.net/hwx-remote/apps/hive/warehouse", hiveMetastore.get().getDefaultPath());
        Assert.assertEquals("default-account-name.azuredatalakestore.net/hwx-remote/apps/ranger/audit/bigCluster", rangerAdmin.get().getDefaultPath());
    }

    @Test
    public void testWhenHiveMetasoreAndRangerAdminIsPresentedDoubleThenShouldReturnWithBothConfigs() {
        Map<String, Set<String>> map = new HashMap<>();
        map.put("master", Sets.newHashSet(HIVE_METASTORE, RANGER_ADMIN));
        map.put("slave_1", Sets.newHashSet(HIVE_METASTORE, RANGER_ADMIN));

        prepareBlueprintProcessorFactoryMock(map);
        FileSystemConfigQueryObject fileSystemConfigQueryObject = Builder.builder()
                .withStorageName(STORAGE_NAME)
                .withClusterName(CLUSTER_NAME)
                .withBlueprintText(BLUEPRINT_TEXT)
                .withFileSystemType(FileSystemType.ADLS.name())
                .build();
        Set<ConfigQueryEntry> bigCluster = underTest.queryParameters(fileSystemConfigQueryObject);

        Assert.assertEquals(2L, bigCluster.size());

        Optional<ConfigQueryEntry> hiveMetastore = serviceEntry(bigCluster, HIVE_METASTORE);
        Optional<ConfigQueryEntry> rangerAdmin = serviceEntry(bigCluster, RANGER_ADMIN);

        Assert.assertTrue(hiveMetastore.isPresent());
        Assert.assertTrue(rangerAdmin.isPresent());

        Assert.assertEquals("default-account-name.azuredatalakestore.net/hwx-remote/apps/hive/warehouse", hiveMetastore.get().getDefaultPath());
        Assert.assertEquals("default-account-name.azuredatalakestore.net/hwx-remote/apps/ranger/audit/bigCluster", rangerAdmin.get().getDefaultPath());
    }

    @Test
    public void testWhenOnlyRangerAdminIsPresentedThenShouldReturnWithOnlyRangerAdminConfigs() {
        prepareBlueprintProcessorFactoryMock(RANGER_ADMIN);
        FileSystemConfigQueryObject fileSystemConfigQueryObject = Builder.builder()
                .withStorageName(STORAGE_NAME)
                .withClusterName(CLUSTER_NAME)
                .withBlueprintText(BLUEPRINT_TEXT)
                .withFileSystemType(FileSystemType.ADLS.name())
                .build();
        Set<ConfigQueryEntry> bigCluster = underTest.queryParameters(fileSystemConfigQueryObject);

        Assert.assertEquals(1L, bigCluster.size());

        Optional<ConfigQueryEntry> hiveMetastore = serviceEntry(bigCluster, HIVE_METASTORE);
        Optional<ConfigQueryEntry> rangerAdmin = serviceEntry(bigCluster, RANGER_ADMIN);

        Assert.assertFalse(hiveMetastore.isPresent());
        Assert.assertTrue(rangerAdmin.isPresent());
        Assert.assertEquals("default-account-name.azuredatalakestore.net/hwx-remote/apps/ranger/audit/bigCluster", rangerAdmin.get().getDefaultPath());
    }

    @Test
    public void testWhenOnlyHiveMetastoreIsPresentedAndAttachedClusterThenShouldReturnWithOnlyRangerAdminConfigs() {
        prepareBlueprintProcessorFactoryMock(HIVE_METASTORE);
        FileSystemConfigQueryObject fileSystemConfigQueryObject = Builder.builder()
                .withStorageName(STORAGE_NAME)
                .withClusterName(CLUSTER_NAME)
                .withBlueprintText(BLUEPRINT_TEXT)
                .withAttachedCluster(true)
                .withFileSystemType(FileSystemType.ADLS.name())
                .build();
        Set<ConfigQueryEntry> bigCluster = underTest.queryParameters(fileSystemConfigQueryObject);

        Assert.assertEquals(2L, bigCluster.size());

        Optional<ConfigQueryEntry> hiveMetastore = serviceEntry(bigCluster, HIVE_METASTORE);
        Optional<ConfigQueryEntry> hiveServerRangerAdmin = serviceEntry(bigCluster, HIVE_SERVER);

        Assert.assertTrue(hiveMetastore.isPresent());
        Assert.assertTrue(hiveServerRangerAdmin.isPresent());
        Assert.assertEquals("default-account-name.azuredatalakestore.net/hwx-remote/apps/ranger/audit/bigCluster",
                hiveServerRangerAdmin.get().getDefaultPath());
        Assert.assertEquals("default-account-name.azuredatalakestore.net/hwx-remote/apps/hive/warehouse",
                hiveMetastore.get().getDefaultPath());
    }

    @Test
    public void testWhenOnlyHiveMetastoreIsPresentedThenShouldReturnWithOnlyHiveMetastoreConfigs() {
        prepareBlueprintProcessorFactoryMock(HIVE_METASTORE);
        FileSystemConfigQueryObject fileSystemConfigQueryObject = Builder.builder()
                .withStorageName(STORAGE_NAME)
                .withClusterName(CLUSTER_NAME)
                .withBlueprintText(BLUEPRINT_TEXT)
                .withFileSystemType(FileSystemType.ADLS.name())
                .build();
        Set<ConfigQueryEntry> bigCluster = underTest.queryParameters(fileSystemConfigQueryObject);

        Assert.assertEquals(1L, bigCluster.size());

        Optional<ConfigQueryEntry> hiveMetastore = serviceEntry(bigCluster, HIVE_METASTORE);
        Optional<ConfigQueryEntry> rangerAdmin = serviceEntry(bigCluster, RANGER_ADMIN);

        Assert.assertTrue(hiveMetastore.isPresent());
        Assert.assertFalse(rangerAdmin.isPresent());
        Assert.assertEquals("default-account-name.azuredatalakestore.net/hwx-remote/apps/hive/warehouse", hiveMetastore.get().getDefaultPath());
    }

    @Test
    public void testWhenHiveMetastoreAndRangerAdminIsNotPresentedThenShouldReturnWithOutThoseConfigs() {
        prepareBlueprintProcessorFactoryMock();
        FileSystemConfigQueryObject fileSystemConfigQueryObject = Builder.builder()
                .withStorageName(STORAGE_NAME)
                .withClusterName(CLUSTER_NAME)
                .withBlueprintText(BLUEPRINT_TEXT)
                .withFileSystemType(FileSystemType.ADLS.name())
                .build();
        Set<ConfigQueryEntry> bigCluster = underTest.queryParameters(fileSystemConfigQueryObject);

        Assert.assertEquals(0L, bigCluster.size());

        Optional<ConfigQueryEntry> hiveMetastore = serviceEntry(bigCluster, HIVE_METASTORE);
        Optional<ConfigQueryEntry> rangerAdmin = serviceEntry(bigCluster, RANGER_ADMIN);

        Assert.assertFalse(hiveMetastore.isPresent());
        Assert.assertFalse(rangerAdmin.isPresent());
    }

    private Optional<ConfigQueryEntry> serviceEntry(Set<ConfigQueryEntry> configQueryEntries, String serviceName) {
        return configQueryEntries.stream().filter(b -> b.getRelatedService().equals(serviceName)).findFirst();
    }

    private void prepareBlueprintProcessorFactoryMock(String... services) {
        Map<String, Set<String>> result = new HashMap<>();
        result.put("master", Sets.newHashSet(services));

        prepareBlueprintProcessorFactoryMock(result);
    }

    private void prepareBlueprintProcessorFactoryMock(Map<String, Set<String>> entries) {
        BlueprintTextProcessor blueprintTextProcessorMock = mock(BlueprintTextProcessor.class);
        when(blueprintTextProcessorMock.getComponentsByHostGroup()).thenReturn(entries);
        when(blueprintProcessorFactory.get(anyString())).thenReturn(blueprintTextProcessorMock);
    }
}