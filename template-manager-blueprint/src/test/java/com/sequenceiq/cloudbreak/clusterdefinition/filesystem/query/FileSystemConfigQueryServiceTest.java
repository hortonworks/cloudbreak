package com.sequenceiq.cloudbreak.clusterdefinition.filesystem.query;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.services.filesystem.FileSystemType;
import com.sequenceiq.cloudbreak.clusterdefinition.AmbariBlueprintProcessorFactory;
import com.sequenceiq.cloudbreak.clusterdefinition.filesystem.FileSystemConfigQueryService;
import com.sequenceiq.cloudbreak.service.CloudbreakResourceReaderService;
import com.sequenceiq.cloudbreak.template.filesystem.FileSystemConfigQueryObject;
import com.sequenceiq.cloudbreak.template.filesystem.FileSystemConfigQueryObject.Builder;
import com.sequenceiq.cloudbreak.template.filesystem.query.ConfigQueryEntry;
import com.sequenceiq.cloudbreak.template.processor.AmbariBlueprintTextProcessor;
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
    private AmbariBlueprintProcessorFactory ambariBlueprintProcessorFactory;

    private final FileSystemConfigQueryService underTest = new FileSystemConfigQueryService();

    private final FileSystemConfigQueryService underTestPlaceholders = new FileSystemConfigQueryService();

    @Before
    public void before() throws IOException {
        String specifications = FileReaderUtils.readFileFromClasspath("definitions/cloud-storage-location-specification.json");
        when(cloudbreakResourceReaderService.resourceDefinition("cloud-storage-location-specification")).thenReturn(specifications);
        ReflectionTestUtils.setField(underTest, null, cloudbreakResourceReaderService, CloudbreakResourceReaderService.class);
        ReflectionTestUtils.setField(underTest, null, ambariBlueprintProcessorFactory, AmbariBlueprintProcessorFactory.class);
        underTest.init();

        specifications = FileReaderUtils.readFileFromClasspath("filesystem-definitions/cloud-storage-location-specification-placeholders.json");
        when(cloudbreakResourceReaderServicePlaceholders.resourceDefinition("cloud-storage-location-specification")).thenReturn(specifications);
        ReflectionTestUtils.setField(underTestPlaceholders, null, cloudbreakResourceReaderServicePlaceholders, CloudbreakResourceReaderService.class);
        ReflectionTestUtils.setField(underTestPlaceholders, null, ambariBlueprintProcessorFactory, AmbariBlueprintProcessorFactory.class);
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

        Set<ConfigQueryEntry> hiveMetastores = serviceEntry(bigCluster, HIVE_METASTORE);
        Set<ConfigQueryEntry> rangerAdmins = serviceEntry(bigCluster, RANGER_ADMIN);

        Assert.assertEquals(hiveMetastores.size(), 2);
        Assert.assertEquals(rangerAdmins.size(), 1);

        Assert.assertTrue(hiveMetastores.stream()
                .anyMatch(cqe -> cqe.getDefaultPath().equals("default-account-name.azuredatalakestore.net/hwx-remote/warehouse/tablespace/managed/hive")));
        Assert.assertTrue(hiveMetastores.stream()
                .anyMatch(cqe -> cqe.getDefaultPath().equals("default-account-name.azuredatalakestore.net/hwx-remote/warehouse/tablespace/external/hive")));
        Assert.assertTrue(rangerAdmins.stream()
                .anyMatch(cqe -> cqe.getDefaultPath().equals("default-account-name.azuredatalakestore.net/hwx-remote/ranger/audit")));
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

        Set<ConfigQueryEntry> hiveMetastores = serviceEntry(bigCluster, HIVE_METASTORE);
        Set<ConfigQueryEntry> rangerAdmins = serviceEntry(bigCluster, RANGER_ADMIN);

        Assert.assertEquals(hiveMetastores.size(), 2);
        Assert.assertEquals(rangerAdmins.size(), 1);

        Assert.assertTrue(hiveMetastores.stream()
                .anyMatch(cqe -> cqe.getDefaultPath().equals("default-account-name.azuredatalakestore.net/hwx-remote/warehouse/tablespace/managed/hive")));
        Assert.assertTrue(hiveMetastores.stream()
                .anyMatch(cqe -> cqe.getDefaultPath().equals("default-account-name.azuredatalakestore.net/hwx-remote/warehouse/tablespace/external/hive")));
        Assert.assertTrue(rangerAdmins.stream()
                .anyMatch(cqe -> cqe.getDefaultPath().equals("default-account-name.azuredatalakestore.net/hwx-remote/ranger/audit")));
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

        Set<ConfigQueryEntry> hiveMetastores = serviceEntry(bigCluster, HIVE_METASTORE);
        Set<ConfigQueryEntry> rangerAdmins = serviceEntry(bigCluster, RANGER_ADMIN);

        Assert.assertEquals(hiveMetastores.size(), 0);
        Assert.assertEquals(rangerAdmins.size(), 1);
        Assert.assertTrue(rangerAdmins.stream()
                .anyMatch(cqe -> cqe.getDefaultPath().equals("default-account-name.azuredatalakestore.net/hwx-remote/ranger/audit")));
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

        Set<ConfigQueryEntry> hiveMetastore = serviceEntry(bigCluster, HIVE_METASTORE, HIVE_METASTORE_WAREHOUSE_DIR);
        Set<ConfigQueryEntry> hiveMetastoreExternal = serviceEntry(bigCluster, HIVE_METASTORE, HIVE_METASTORE_WAREHOUSE_EXTERNAL_DIR);
        Set<ConfigQueryEntry> hiveServerRangerAdmin = serviceEntry(bigCluster, HIVE_METASTORE, RANGER_HIVE_AUDIT_DIR);

        Assert.assertEquals(hiveMetastore.size(), 1);
        Assert.assertEquals(hiveMetastoreExternal.size(), 1);
        Assert.assertEquals(hiveServerRangerAdmin.size(), 0);

        Assert.assertTrue(hiveMetastore.stream().map(ConfigQueryEntry::getDefaultPath)
                .anyMatch("default-account-name.azuredatalakestore.net/hwx-remote/warehouse/tablespace/managed/hive"::equals));
        Assert.assertTrue(hiveMetastoreExternal.stream().map(ConfigQueryEntry::getDefaultPath)
                .anyMatch("default-account-name.azuredatalakestore.net/hwx-remote/warehouse/tablespace/external/hive"::equals));
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

        Set<ConfigQueryEntry> hiveMetastores = serviceEntry(bigCluster, HIVE_METASTORE);
        Set<ConfigQueryEntry> rangerAdmins = serviceEntry(bigCluster, RANGER_ADMIN);

        Assert.assertEquals(hiveMetastores.size(), 2);
        Assert.assertEquals(rangerAdmins.size(), 0);
        Assert.assertTrue(hiveMetastores.stream()
                .anyMatch(cqe -> cqe.getDefaultPath().equals("default-account-name.azuredatalakestore.net/hwx-remote/warehouse/tablespace/managed/hive")));
        Assert.assertTrue(hiveMetastores.stream()
                .anyMatch(cqe -> cqe.getDefaultPath().equals("default-account-name.azuredatalakestore.net/hwx-remote/warehouse/tablespace/external/hive")));
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

        Set<ConfigQueryEntry> hiveMetastores = serviceEntry(bigCluster, HIVE_METASTORE);
        Set<ConfigQueryEntry> rangerAdmins = serviceEntry(bigCluster, RANGER_ADMIN);

        Assert.assertEquals(hiveMetastores.size(), 0);
        Assert.assertEquals(rangerAdmins.size(), 0);
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

        Set<ConfigQueryEntry> rangerAdmin = serviceEntry(bigCluster, RANGER_ADMIN);
        Set<ConfigQueryEntry> yarnLogs = serviceEntry(bigCluster, NODEMANAGER);
        Set<ConfigQueryEntry> sparkEventLog = serviceEntry(bigCluster, SPARK2_JOBHISTORYSERVER, SPARK_EVENTLOG_DIR);
        Set<ConfigQueryEntry> sparkHistory = serviceEntry(bigCluster, SPARK2_JOBHISTORYSERVER, SPARK_HISTORY_DIR);

        Assert.assertEquals(rangerAdmin.size(), 1);
        Assert.assertEquals(yarnLogs.size(), 1);
        Assert.assertEquals(sparkEventLog.size(), 1);
        Assert.assertEquals(sparkHistory.size(), 1);

        Assert.assertTrue(rangerAdmin.stream().map(ConfigQueryEntry::getDefaultPath)
                .anyMatch("default-account-name.azuredatalakestore.net/hwx-remote/ranger/audit"::equals));
        Assert.assertTrue(yarnLogs.stream().map(ConfigQueryEntry::getDefaultPath)
                .anyMatch("default-account-name.azuredatalakestore.net/hwx-remote/oplogs/yarn-app-logs"::equals));
        Assert.assertTrue(sparkEventLog.stream().map(ConfigQueryEntry::getDefaultPath)
                .anyMatch("default-account-name.azuredatalakestore.net/hwx-remote/oplogs/spark2-history"::equals));
        Assert.assertTrue(sparkHistory.stream().map(ConfigQueryEntry::getDefaultPath)
                .anyMatch("default-account-name.azuredatalakestore.net/hwx-remote/oplogs/spark2-history"::equals));
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

        Set<ConfigQueryEntry> rangerAdmin = serviceEntry(bigCluster, RANGER_ADMIN);
        Set<ConfigQueryEntry> yarnLogs = serviceEntry(bigCluster, NODEMANAGER);
        Set<ConfigQueryEntry> sparkEventLog = serviceEntry(bigCluster, SPARK2_JOBHISTORYSERVER, SPARK_EVENTLOG_DIR);
        Set<ConfigQueryEntry> sparkHistory = serviceEntry(bigCluster, SPARK2_JOBHISTORYSERVER, SPARK_HISTORY_DIR);

        Assert.assertEquals(rangerAdmin.size(), 1);
        Assert.assertEquals(yarnLogs.size(), 1);
        Assert.assertEquals(sparkEventLog.size(), 1);
        Assert.assertEquals(sparkHistory.size(), 1);

        Assert.assertTrue(rangerAdmin.stream().map(ConfigQueryEntry::getDefaultPath)
                .anyMatch("default-account-name.azuredatalakestore.net/hwx-remote/ranger/audit"::equals));
        String yarnLogsPath = "default-account-name.azuredatalakestore.net/hwx-remote/" + CLUSTER_NAME + "/oplogs/yarn-app-logs";
        Assert.assertTrue(yarnLogs.stream().map(ConfigQueryEntry::getDefaultPath).anyMatch(yarnLogsPath::equals));
        String sparkEventLogPath = "default-account-name.azuredatalakestore.net/hwx-remote/" + CLUSTER_NAME + "/oplogs/spark2-history";
        Assert.assertTrue(sparkEventLog.stream().map(ConfigQueryEntry::getDefaultPath).anyMatch(sparkEventLogPath::equals));
        String sparkHistoryPath = "default-account-name.azuredatalakestore.net/hwx-remote/" + CLUSTER_NAME + "/oplogs/spark2-history";
        Assert.assertTrue(sparkHistory.stream().map(ConfigQueryEntry::getDefaultPath).anyMatch(sparkHistoryPath::equals));
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

        Set<ConfigQueryEntry> rangerAdmin = serviceEntry(bigCluster, RANGER_ADMIN);
        Set<ConfigQueryEntry> yarnLogs = serviceEntry(bigCluster, NODEMANAGER);
        Set<ConfigQueryEntry> sparkEventLog = serviceEntry(bigCluster, SPARK2_JOBHISTORYSERVER, SPARK_EVENTLOG_DIR);
        Set<ConfigQueryEntry> sparkHistory = serviceEntry(bigCluster, SPARK2_JOBHISTORYSERVER, SPARK_HISTORY_DIR);

        Assert.assertEquals(rangerAdmin.size(), 1);
        Assert.assertEquals(yarnLogs.size(), 1);
        Assert.assertEquals(sparkEventLog.size(), 1);
        Assert.assertEquals(sparkHistory.size(), 1);

        Assert.assertTrue(rangerAdmin.stream().map(ConfigQueryEntry::getDefaultPath)
                .anyMatch("default-account-name.azuredatalakestore.net/hwx-remote/ranger/audit"::equals));
        Assert.assertTrue(yarnLogs.stream().map(ConfigQueryEntry::getDefaultPath)
                .anyMatch("default-account-name.azuredatalakestore.net/hwx-remote/oplogs/yarn-app-logs"::equals));
        Assert.assertTrue(sparkEventLog.stream().map(ConfigQueryEntry::getDefaultPath)
                .anyMatch("default-account-name.azuredatalakestore.net/hwx-remote/oplogs/spark2-history"::equals));
        Assert.assertTrue(sparkHistory.stream().map(ConfigQueryEntry::getDefaultPath)
                .anyMatch("default-account-name.azuredatalakestore.net/hwx-remote/oplogs/spark2-history"::equals));
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

        Set<ConfigQueryEntry> hiveMetastore = serviceEntry(bigCluster, HIVE_METASTORE, HIVE_METASTORE_WAREHOUSE_DIR);
        Set<ConfigQueryEntry> hiveServerRangerAdmin = serviceEntry(bigCluster, HIVE_SERVER);
        Set<ConfigQueryEntry> rangerAdmin = serviceEntry(bigCluster, RANGER_ADMIN);

        Assert.assertEquals(hiveMetastore.size(), 1);
        Assert.assertEquals(hiveServerRangerAdmin.size(), 1);
        Assert.assertEquals(rangerAdmin.size(), 1);
        Assert.assertTrue(hiveServerRangerAdmin.stream().map(ConfigQueryEntry::getDefaultPath)
                .anyMatch("default-account-name.azuredatalakestore.net/hwx-remote/bigCluster/apps/ranger/audit"::equals));
        Assert.assertTrue(hiveMetastore.stream().map(ConfigQueryEntry::getDefaultPath)
                .anyMatch("default-account-name.azuredatalakestore.net/hwx-remote/apps/hive/warehouse"::equals));
        Assert.assertTrue(rangerAdmin.stream().map(ConfigQueryEntry::getDefaultPath)
                .anyMatch("default-account-name.azuredatalakestore.net/hwx-remote/bigCluster/apps/ranger/audit"::equals));
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

        Set<ConfigQueryEntry> hiveMetastore = serviceEntry(bigCluster, HIVE_METASTORE, HIVE_METASTORE_WAREHOUSE_DIR);
        Set<ConfigQueryEntry> hiveServerRangerAdmin = serviceEntry(bigCluster, HIVE_SERVER);
        Set<ConfigQueryEntry> rangerAdmin = serviceEntry(bigCluster, RANGER_ADMIN);

        Assert.assertEquals(hiveMetastore.size(), 1);
        Assert.assertEquals(hiveServerRangerAdmin.size(), 1);
        Assert.assertEquals(rangerAdmin.size(), 1);
        Assert.assertTrue(hiveServerRangerAdmin.stream().map(ConfigQueryEntry::getDefaultPath)
                .anyMatch("default-account-name.azuredatalakestore.net/hwx-remote/apps/ranger/audit"::equals));
        Assert.assertTrue(hiveMetastore.stream().map(ConfigQueryEntry::getDefaultPath)
                .anyMatch("default-account-name.azuredatalakestore.net/hwx-remote/apps/hive/warehouse"::equals));
        Assert.assertTrue(rangerAdmin.stream().map(ConfigQueryEntry::getDefaultPath)
                .anyMatch("default-account-name.azuredatalakestore.net/hwx-remote/bigCluster/apps/ranger/audit"::equals));
    }

    private Set<ConfigQueryEntry> serviceEntry(Set<ConfigQueryEntry> configQueryEntries, String serviceName) {
        return configQueryEntries.stream().filter(b -> b.getRelatedServices().stream()
                .anyMatch(service -> service.equals(serviceName))).collect(Collectors.toSet());
    }

    private Set<ConfigQueryEntry> serviceEntry(Set<ConfigQueryEntry> configQueryEntries, String serviceName, String propertyName) {
        return configQueryEntries.stream()
                .filter(b -> b.getRelatedServices().stream().anyMatch(service -> service.equals(serviceName)))
                .filter(b -> b.getPropertyName().equals(propertyName))
                .collect(Collectors.toSet());
    }

    @Test
    public void testCopyOfContigEntries() {
        prepareBlueprintProcessorFactoryMock(HIVE_SERVER, RANGER_ADMIN);

        Set<ConfigQueryEntry> bigCluster = getConfigQueryEntriesS3(STORAGE_NAME);

        Set<ConfigQueryEntry> hiveServerEntries = serviceEntry(bigCluster, HIVE_SERVER);
        Set<ConfigQueryEntry> rangerAdminEntries = serviceEntry(bigCluster, RANGER_ADMIN);

        Assert.assertEquals(hiveServerEntries.size(), 3);
        Assert.assertTrue(hiveServerEntries.stream().anyMatch(cqe -> cqe.getDefaultPath().contains(STORAGE_NAME + "/")));
        Assert.assertEquals(rangerAdminEntries.size(), 1);
        Assert.assertTrue(rangerAdminEntries.stream().anyMatch(cqe -> cqe.getDefaultPath().contains(STORAGE_NAME + "/")));

        bigCluster = getConfigQueryEntriesS3(STORAGE_NAME + "copy");

        hiveServerEntries = serviceEntry(bigCluster, HIVE_SERVER);
        rangerAdminEntries = serviceEntry(bigCluster, RANGER_ADMIN);

        Assert.assertEquals(hiveServerEntries.size(), 3);
        Assert.assertTrue(hiveServerEntries.stream().anyMatch(cqe -> cqe.getDefaultPath().contains(STORAGE_NAME + "copy/")));
        Assert.assertTrue(hiveServerEntries.stream().noneMatch(cqe -> cqe.getDefaultPath().contains(STORAGE_NAME + "/")));
        Assert.assertEquals(rangerAdminEntries.size(), 1);
        Assert.assertTrue(rangerAdminEntries.stream().anyMatch(cqe -> cqe.getDefaultPath().contains(STORAGE_NAME + "copy/")));
        Assert.assertTrue(rangerAdminEntries.stream().noneMatch(cqe -> cqe.getDefaultPath().contains(STORAGE_NAME + "/")));
    }

    private Set<ConfigQueryEntry> getConfigQueryEntriesS3(String storageName) {
        FileSystemConfigQueryObject fileSystemConfigQueryObject = Builder.builder()
                .withStorageName(storageName)
                .withClusterName(CLUSTER_NAME)
                .withBlueprintText(BLUEPRINT_TEXT)
                .withAttachedCluster(true)
                .withFileSystemType(FileSystemType.S3.name())
                .build();
        return underTest.queryParameters(fileSystemConfigQueryObject);
    }

    private void prepareBlueprintProcessorFactoryMock(String... services) {
        Map<String, Set<String>> result = new HashMap<>();
        result.put("master", Sets.newHashSet(services));

        prepareBlueprintProcessorFactoryMock(result);
    }

    private void prepareBlueprintProcessorFactoryMock(Map<String, Set<String>> entries) {
        AmbariBlueprintTextProcessor ambariBlueprintTextProcessorMock = mock(AmbariBlueprintTextProcessor.class);
        when(ambariBlueprintTextProcessorMock.getComponentsByHostGroup()).thenReturn(entries);
        when(ambariBlueprintProcessorFactory.get(anyString())).thenReturn(ambariBlueprintTextProcessorMock);
    }
}