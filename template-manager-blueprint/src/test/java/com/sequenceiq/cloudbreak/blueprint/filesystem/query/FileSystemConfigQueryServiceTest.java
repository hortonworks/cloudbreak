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
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.api.model.filesystem.FileSystemType;
import com.sequenceiq.cloudbreak.blueprint.BlueprintProcessorFactory;
import com.sequenceiq.cloudbreak.blueprint.filesystem.FileSystemConfigQueryService;
import com.sequenceiq.cloudbreak.service.CloudbreakResourceReaderService;
import com.sequenceiq.cloudbreak.template.filesystem.FileSystemConfigQueryObject;
import com.sequenceiq.cloudbreak.template.filesystem.FileSystemConfigQueryObject.Builder;
import com.sequenceiq.cloudbreak.template.filesystem.query.ConfigQueryEntry;
import com.sequenceiq.cloudbreak.template.processor.BlueprintTextProcessor;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;

@RunWith(MockitoJUnitRunner.class)
public class FileSystemConfigQueryServiceTest {

    private static final String BLUEPRINT_TEXT = "testblueprint";

    private static final String HIVE_METASTORE = "HIVE_METASTORE";

    private static final String RANGER_ADMIN = "RANGER_ADMIN";

    private static final String NODEMANAGER = "NODEMANAGER";

    private static final String SPARK2_JOBHISTORYSERVER = "SPARK2_JOBHISTORYSERVER";

    private static final String HIVE_SERVER = "HIVE_SERVER";

    private static final String CLUSTER_NAME = "bigCluster";

    private static final String STORAGE_NAME = "hwx-remote";

    private static final String HIVE_METASTORE_WAREHOUSE_DIR = "hive.metastore.warehouse.dir";

    private static final String HIVE_METASTORE_WAREHOUSE_EXTERNAL_DIR = "hive.metastore.warehouse.external.dir";

    private static final String RANGER_HIVE_AUDIT_DIR = "xasecure.audit.destination.hdfs.dir";

    private static final String SPARK_EVENTLOG_DIR = "spark.eventLog.dir";

    private static final String SPARK_HISTORY_DIR = "spark.history.fs.logDirectory";

    @Mock
    private CloudbreakResourceReaderService cloudbreakResourceReaderService;

    @Mock
    private CloudbreakResourceReaderService cloudbreakResourceReaderServicePlaceholders;

    @Mock
    private BlueprintProcessorFactory blueprintProcessorFactory;

    private final FileSystemConfigQueryService underTest = new FileSystemConfigQueryService();

    private final FileSystemConfigQueryService underTestPlaceholders = new FileSystemConfigQueryService();

    @Before
    public void before() throws IOException {
        String specifications = FileReaderUtils.readFileFromClasspath("definitions/cloud-storage-location-specification.json");
        when(cloudbreakResourceReaderService.resourceDefinition("cloud-storage-location-specification")).thenReturn(specifications);
        ReflectionTestUtils.setField(underTest, null, cloudbreakResourceReaderService, CloudbreakResourceReaderService.class);
        ReflectionTestUtils.setField(underTest, null, blueprintProcessorFactory, BlueprintProcessorFactory.class);
        underTest.init();

        specifications = FileReaderUtils.readFileFromClasspath("filesystem-definitions/cloud-storage-location-specification-placeholders.json");
        when(cloudbreakResourceReaderServicePlaceholders.resourceDefinition("cloud-storage-location-specification")).thenReturn(specifications);
        ReflectionTestUtils.setField(underTestPlaceholders, null, cloudbreakResourceReaderServicePlaceholders, CloudbreakResourceReaderService.class);
        ReflectionTestUtils.setField(underTestPlaceholders, null, blueprintProcessorFactory, BlueprintProcessorFactory.class);
        underTestPlaceholders.init();
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

        Assert.assertEquals(3L, bigCluster.size());

        Optional<ConfigQueryEntry> hiveMetastore = serviceEntry(bigCluster, HIVE_METASTORE, HIVE_METASTORE_WAREHOUSE_DIR);
        Optional<ConfigQueryEntry> hiveMetastoreExternal = serviceEntry(bigCluster, HIVE_METASTORE, HIVE_METASTORE_WAREHOUSE_EXTERNAL_DIR);
        Optional<ConfigQueryEntry> rangerAdmin = serviceEntry(bigCluster, RANGER_ADMIN);


        Assert.assertTrue(hiveMetastore.isPresent());
        Assert.assertTrue(hiveMetastoreExternal.isPresent());
        Assert.assertTrue(rangerAdmin.isPresent());


        Assert.assertEquals("default-account-name.azuredatalakestore.net/hwx-remote/warehouse/tablespace/managed/hive", hiveMetastore.get().getDefaultPath());
        Assert.assertEquals("default-account-name.azuredatalakestore.net/hwx-remote/warehouse/tablespace/external/hive",
                hiveMetastoreExternal.get().getDefaultPath());
        Assert.assertEquals("default-account-name.azuredatalakestore.net/hwx-remote/ranger/audit", rangerAdmin.get().getDefaultPath());
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

        Assert.assertEquals(3L, bigCluster.size());

        Optional<ConfigQueryEntry> hiveMetastore = serviceEntry(bigCluster, HIVE_METASTORE, HIVE_METASTORE_WAREHOUSE_DIR);
        Optional<ConfigQueryEntry> hiveMetastoreExternal = serviceEntry(bigCluster, HIVE_METASTORE, HIVE_METASTORE_WAREHOUSE_EXTERNAL_DIR);
        Optional<ConfigQueryEntry> rangerAdmin = serviceEntry(bigCluster, RANGER_ADMIN);

        Assert.assertTrue(hiveMetastore.isPresent());
        Assert.assertTrue(hiveMetastoreExternal.isPresent());
        Assert.assertTrue(rangerAdmin.isPresent());

        Assert.assertEquals("default-account-name.azuredatalakestore.net/hwx-remote/warehouse/tablespace/managed/hive", hiveMetastore.get().getDefaultPath());
        Assert.assertEquals("default-account-name.azuredatalakestore.net/hwx-remote/warehouse/tablespace/external/hive",
                hiveMetastoreExternal.get().getDefaultPath());
        Assert.assertEquals("default-account-name.azuredatalakestore.net/hwx-remote/ranger/audit", rangerAdmin.get().getDefaultPath());
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
        Assert.assertEquals("default-account-name.azuredatalakestore.net/hwx-remote/ranger/audit", rangerAdmin.get().getDefaultPath());
    }

    @Test
    public void testWhenOnlyHiveMetastoreIsPresentedAndAttachedClusterThenShouldReturnWithHiveMetastoreConfigs() {
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

        Optional<ConfigQueryEntry> hiveMetastore = serviceEntry(bigCluster, HIVE_METASTORE, HIVE_METASTORE_WAREHOUSE_DIR);
        Optional<ConfigQueryEntry> hiveMetastoreExternal = serviceEntry(bigCluster, HIVE_METASTORE, HIVE_METASTORE_WAREHOUSE_EXTERNAL_DIR);
        Optional<ConfigQueryEntry> hiveServerRangerAdmin = serviceEntry(bigCluster, HIVE_METASTORE, RANGER_HIVE_AUDIT_DIR);

        Assert.assertTrue(hiveMetastore.isPresent());
        Assert.assertTrue(hiveMetastoreExternal.isPresent());
        Assert.assertFalse(hiveServerRangerAdmin.isPresent());

        Assert.assertEquals("default-account-name.azuredatalakestore.net/hwx-remote/warehouse/tablespace/managed/hive",
                hiveMetastore.get().getDefaultPath());
        Assert.assertEquals("default-account-name.azuredatalakestore.net/hwx-remote/warehouse/tablespace/external/hive",
                hiveMetastoreExternal.get().getDefaultPath());
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

        Assert.assertEquals(2L, bigCluster.size());

        Optional<ConfigQueryEntry> hiveMetastore = serviceEntry(bigCluster, HIVE_METASTORE, HIVE_METASTORE_WAREHOUSE_DIR);
        Optional<ConfigQueryEntry> hiveMetastoreExternal = serviceEntry(bigCluster, HIVE_METASTORE, HIVE_METASTORE_WAREHOUSE_EXTERNAL_DIR);
        Optional<ConfigQueryEntry> rangerAdmin = serviceEntry(bigCluster, RANGER_ADMIN);

        Assert.assertTrue(hiveMetastore.isPresent());
        Assert.assertTrue(hiveMetastoreExternal.isPresent());
        Assert.assertFalse(rangerAdmin.isPresent());
        Assert.assertEquals("default-account-name.azuredatalakestore.net/hwx-remote/warehouse/tablespace/managed/hive", hiveMetastore.get().getDefaultPath());
        Assert.assertEquals("default-account-name.azuredatalakestore.net/hwx-remote/warehouse/tablespace/external/hive",
                hiveMetastoreExternal.get().getDefaultPath());
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

    @Test
    public void testDatalakeClusterPaths() {
        prepareBlueprintProcessorFactoryMock(RANGER_ADMIN, SPARK2_JOBHISTORYSERVER, NODEMANAGER);
        FileSystemConfigQueryObject fileSystemConfigQueryObject = Builder.builder()
                .withStorageName(STORAGE_NAME)
                .withClusterName(CLUSTER_NAME)
                .withBlueprintText(BLUEPRINT_TEXT)
                .withDatalakeCluster(true)
                .withFileSystemType(FileSystemType.ADLS.name())
                .build();
        Set<ConfigQueryEntry> bigCluster = underTest.queryParameters(fileSystemConfigQueryObject);
        Assert.assertEquals(4L, bigCluster.size());

        Optional<ConfigQueryEntry> rangerAdmin = serviceEntry(bigCluster, RANGER_ADMIN);
        Optional<ConfigQueryEntry> yarnLogs = serviceEntry(bigCluster, NODEMANAGER);
        Optional<ConfigQueryEntry> sparkEventLog = serviceEntry(bigCluster, SPARK2_JOBHISTORYSERVER, SPARK_EVENTLOG_DIR);
        Optional<ConfigQueryEntry> sparkHistory = serviceEntry(bigCluster, SPARK2_JOBHISTORYSERVER, SPARK_HISTORY_DIR);

        Assert.assertTrue(rangerAdmin.isPresent());
        Assert.assertTrue(yarnLogs.isPresent());
        Assert.assertTrue(sparkEventLog.isPresent());
        Assert.assertTrue(sparkHistory.isPresent());

        Assert.assertEquals("default-account-name.azuredatalakestore.net/hwx-remote/ranger/audit", rangerAdmin.get().getDefaultPath());
        Assert.assertEquals("default-account-name.azuredatalakestore.net/hwx-remote/oplogs/yarn-app-logs", yarnLogs.get().getDefaultPath());
        Assert.assertEquals("default-account-name.azuredatalakestore.net/hwx-remote/oplogs/spark2-history", sparkEventLog.get().getDefaultPath());
        Assert.assertEquals("default-account-name.azuredatalakestore.net/hwx-remote/oplogs/spark2-history", sparkHistory.get().getDefaultPath());
    }

    @Test
    public void testStandaloneClusterPaths() {
        prepareBlueprintProcessorFactoryMock(RANGER_ADMIN, SPARK2_JOBHISTORYSERVER, NODEMANAGER);
        FileSystemConfigQueryObject fileSystemConfigQueryObject = Builder.builder()
                .withStorageName(STORAGE_NAME)
                .withClusterName(CLUSTER_NAME)
                .withBlueprintText(BLUEPRINT_TEXT)
                .withDatalakeCluster(false)
                .withAttachedCluster(false)
                .withFileSystemType(FileSystemType.ADLS.name())
                .build();
        Set<ConfigQueryEntry> bigCluster = underTest.queryParameters(fileSystemConfigQueryObject);
        Assert.assertEquals(4L, bigCluster.size());

        Optional<ConfigQueryEntry> rangerAdmin = serviceEntry(bigCluster, RANGER_ADMIN);
        Optional<ConfigQueryEntry> yarnLogs = serviceEntry(bigCluster, NODEMANAGER);
        Optional<ConfigQueryEntry> sparkEventLog = serviceEntry(bigCluster, SPARK2_JOBHISTORYSERVER, SPARK_EVENTLOG_DIR);
        Optional<ConfigQueryEntry> sparkHistory = serviceEntry(bigCluster, SPARK2_JOBHISTORYSERVER, SPARK_HISTORY_DIR);

        Assert.assertTrue(rangerAdmin.isPresent());
        Assert.assertTrue(yarnLogs.isPresent());
        Assert.assertTrue(sparkEventLog.isPresent());
        Assert.assertTrue(sparkHistory.isPresent());

        Assert.assertEquals("default-account-name.azuredatalakestore.net/hwx-remote/ranger/audit", rangerAdmin.get().getDefaultPath());
        Assert.assertEquals("default-account-name.azuredatalakestore.net/hwx-remote/" + CLUSTER_NAME + "/oplogs/yarn-app-logs", yarnLogs.get().getDefaultPath());
        Assert.assertEquals("default-account-name.azuredatalakestore.net/hwx-remote/" + CLUSTER_NAME + "/oplogs/spark2-history",
                sparkEventLog.get().getDefaultPath());
        Assert.assertEquals("default-account-name.azuredatalakestore.net/hwx-remote/" + CLUSTER_NAME + "/oplogs/spark2-history",
                sparkHistory.get().getDefaultPath());
    }

    @Test
    public void testAttachedClusterPaths() {
        prepareBlueprintProcessorFactoryMock(RANGER_ADMIN, SPARK2_JOBHISTORYSERVER, NODEMANAGER);
        FileSystemConfigQueryObject fileSystemConfigQueryObject = Builder.builder()
                .withStorageName(STORAGE_NAME)
                .withClusterName(CLUSTER_NAME)
                .withBlueprintText(BLUEPRINT_TEXT)
                .withDatalakeCluster(false)
                .withAttachedCluster(true)
                .withFileSystemType(FileSystemType.ADLS.name())
                .build();
        Set<ConfigQueryEntry> bigCluster = underTest.queryParameters(fileSystemConfigQueryObject);
        Assert.assertEquals(4L, bigCluster.size());

        Optional<ConfigQueryEntry> rangerAdmin = serviceEntry(bigCluster, RANGER_ADMIN);
        Optional<ConfigQueryEntry> yarnLogs = serviceEntry(bigCluster, NODEMANAGER);
        Optional<ConfigQueryEntry> sparkEventLog = serviceEntry(bigCluster, SPARK2_JOBHISTORYSERVER, SPARK_EVENTLOG_DIR);
        Optional<ConfigQueryEntry> sparkHistory = serviceEntry(bigCluster, SPARK2_JOBHISTORYSERVER, SPARK_HISTORY_DIR);

        Assert.assertTrue(rangerAdmin.isPresent());
        Assert.assertTrue(yarnLogs.isPresent());
        Assert.assertTrue(sparkEventLog.isPresent());
        Assert.assertTrue(sparkHistory.isPresent());

        Assert.assertEquals("default-account-name.azuredatalakestore.net/hwx-remote/ranger/audit", rangerAdmin.get().getDefaultPath());
        Assert.assertEquals("default-account-name.azuredatalakestore.net/hwx-remote/oplogs/yarn-app-logs", yarnLogs.get().getDefaultPath());
        Assert.assertEquals("default-account-name.azuredatalakestore.net/hwx-remote/oplogs/spark2-history", sparkEventLog.get().getDefaultPath());
        Assert.assertEquals("default-account-name.azuredatalakestore.net/hwx-remote/oplogs/spark2-history", sparkHistory.get().getDefaultPath());
    }

    @Test
    public void testPathPlaceholders() {
        prepareBlueprintProcessorFactoryMock(HIVE_METASTORE, HIVE_SERVER, RANGER_ADMIN);
        FileSystemConfigQueryObject fileSystemConfigQueryObject = Builder.builder()
                .withStorageName(STORAGE_NAME)
                .withClusterName(CLUSTER_NAME)
                .withBlueprintText(BLUEPRINT_TEXT)
                .withAttachedCluster(false)
                .withFileSystemType(FileSystemType.ADLS.name())
                .build();
        Set<ConfigQueryEntry> bigCluster = underTestPlaceholders.queryParameters(fileSystemConfigQueryObject);

        Assert.assertEquals(3L, bigCluster.size());

        Optional<ConfigQueryEntry> hiveMetastore = serviceEntry(bigCluster, HIVE_METASTORE, HIVE_METASTORE_WAREHOUSE_DIR);
        Optional<ConfigQueryEntry> hiveServerRangerAdmin = serviceEntry(bigCluster, HIVE_SERVER);
        Optional<ConfigQueryEntry> rangerAdmin = serviceEntry(bigCluster, RANGER_ADMIN);

        Assert.assertTrue(hiveMetastore.isPresent());
        Assert.assertTrue(hiveServerRangerAdmin.isPresent());
        Assert.assertTrue(rangerAdmin.isPresent());
        Assert.assertEquals("default-account-name.azuredatalakestore.net/hwx-remote/bigCluster/apps/ranger/audit",
                hiveServerRangerAdmin.get().getDefaultPath());
        Assert.assertEquals("default-account-name.azuredatalakestore.net/hwx-remote/apps/hive/warehouse",
                hiveMetastore.get().getDefaultPath());
        Assert.assertEquals("default-account-name.azuredatalakestore.net/hwx-remote/bigCluster/apps/ranger/audit", rangerAdmin.get().getDefaultPath());
    }

    @Test
    public void testPathPlaceholdersWhenAttachedCluster() {
        prepareBlueprintProcessorFactoryMock(HIVE_METASTORE, HIVE_SERVER, RANGER_ADMIN);
        FileSystemConfigQueryObject fileSystemConfigQueryObject = Builder.builder()
                .withStorageName(STORAGE_NAME)
                .withClusterName(CLUSTER_NAME)
                .withBlueprintText(BLUEPRINT_TEXT)
                .withAttachedCluster(true)
                .withFileSystemType(FileSystemType.ADLS.name())
                .build();
        Set<ConfigQueryEntry> bigCluster = underTestPlaceholders.queryParameters(fileSystemConfigQueryObject);

        Assert.assertEquals(3L, bigCluster.size());

        Optional<ConfigQueryEntry> hiveMetastore = serviceEntry(bigCluster, HIVE_METASTORE, HIVE_METASTORE_WAREHOUSE_DIR);
        Optional<ConfigQueryEntry> hiveServerRangerAdmin = serviceEntry(bigCluster, HIVE_SERVER);
        Optional<ConfigQueryEntry> rangerAdmin = serviceEntry(bigCluster, RANGER_ADMIN);

        Assert.assertTrue(hiveMetastore.isPresent());
        Assert.assertTrue(hiveServerRangerAdmin.isPresent());
        Assert.assertTrue(rangerAdmin.isPresent());
        Assert.assertEquals("default-account-name.azuredatalakestore.net/hwx-remote/apps/ranger/audit",
                hiveServerRangerAdmin.get().getDefaultPath());
        Assert.assertEquals("default-account-name.azuredatalakestore.net/hwx-remote/apps/hive/warehouse",
                hiveMetastore.get().getDefaultPath());
        Assert.assertEquals("default-account-name.azuredatalakestore.net/hwx-remote/bigCluster/apps/ranger/audit", rangerAdmin.get().getDefaultPath());
    }

    private Optional<ConfigQueryEntry> serviceEntry(Set<ConfigQueryEntry> configQueryEntries, String serviceName) {
        return configQueryEntries.stream().filter(b -> b.getRelatedServices().stream().anyMatch(service -> service.equals(serviceName))).findFirst();
    }

    private Optional<ConfigQueryEntry> serviceEntry(Set<ConfigQueryEntry> configQueryEntries, String serviceName, String propertyName) {
        return configQueryEntries.stream()
                .filter(b -> b.getRelatedServices().stream().anyMatch(service -> service.equals(serviceName)))
                .filter(b -> b.getPropertyName().equals(propertyName))
                .findFirst();
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