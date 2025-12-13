package com.sequenceiq.cloudbreak.cmtemplate.cloudstorage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessorFactory;
import com.sequenceiq.cloudbreak.service.CloudbreakResourceReaderService;
import com.sequenceiq.cloudbreak.template.filesystem.CloudStorageConfigDetails;
import com.sequenceiq.cloudbreak.template.filesystem.FileSystemConfigQueryObject;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;
import com.sequenceiq.common.api.cloudstorage.query.ConfigQueryEntry;
import com.sequenceiq.common.model.FileSystemType;

@ExtendWith(MockitoExtension.class)
class CmCloudStorageConfigDetailsTest {

    private static final String BLUEPRINT_TEXT = "{}";

    private static final String HIVE_METASTORE = "HIVEMETASTORE";

    private static final String RANGER_ADMIN = "RANGER_ADMIN";

    private static final String ZEPPELIN_SERVER = "ZEPPELIN_SERVER";

    private static final String RESOURCEMANAGER = "RESOURCEMANAGER";

    private static final String HIVE_SERVER = "HIVE_SERVER";

    private static final String HBASE_MASTER = "MASTER";

    private static final String DATA_DISCOVERY_SERVICE_AGENT = "DATA_DISCOVERY_SERVICE_AGENT";

    private static final String PROFILER_ADMIN_AGENT = "PROFILER_ADMIN_AGENT";

    private static final String NAMENODE = "NAMENODE";

    private static final String PROFILER_METRICS_AGENT = "PROFILER_METRICS_AGENT";

    private static final String PROFILER_SCHEDULER_AGENT = "PROFILER_SCHEDULER_AGENT";

    private static final String CLUSTER_NAME = "bigCluster";

    private static final String STORAGE_NAME = "hwx-remote";

    @Mock
    private CloudbreakResourceReaderService cloudbreakResourceReaderService;

    @Mock
    private CmTemplateProcessorFactory cmTemplateProcessorFactory;

    private CloudStorageConfigDetails cloudStorageConfigDetails = new CloudStorageConfigDetails();

    private CmCloudStorageConfigProvider underTest;

    @BeforeEach
    public void before() throws IOException {
        String specifications = FileReaderUtils.readFileFromClasspath("definitions/cm-cloud-storage-location-specification.json");
        when(cloudbreakResourceReaderService.resourceDefinition("cm-cloud-storage-location-specification")).thenReturn(specifications);
        underTest = new CmCloudStorageConfigProvider(cloudbreakResourceReaderService, cloudStorageConfigDetails);
        ReflectionTestUtils.setField(underTest, null, cmTemplateProcessorFactory, CmTemplateProcessorFactory.class);
    }

    @Test
    void testWhenHiveMetasoreRangerAdminZeppelinRMIsPresentedAndNotAttachedThenShouldReturnWithBothConfigs() {
        prepareBlueprintProcessorFactoryMock(HIVE_METASTORE, RANGER_ADMIN, RESOURCEMANAGER, ZEPPELIN_SERVER, NAMENODE);
        FileSystemConfigQueryObject fileSystemConfigQueryObject = FileSystemConfigQueryObject.Builder.builder()
                .withStorageName(STORAGE_NAME)
                .withClusterName(CLUSTER_NAME)
                .withBlueprintText(BLUEPRINT_TEXT)
                .withAttachedCluster(false)
                .withFileSystemType(FileSystemType.S3.name())
                .build();
        Set<ConfigQueryEntry> bigCluster = underTest.queryParameters(fileSystemConfigQueryObject);

        assertEquals(6L, bigCluster.size());

        Set<ConfigQueryEntry> rangerAdmins = serviceEntry(bigCluster, RANGER_ADMIN);
        Set<ConfigQueryEntry> yarnSite = serviceEntry(bigCluster, RESOURCEMANAGER);
        Set<ConfigQueryEntry> zeppelin = serviceEntry(bigCluster, ZEPPELIN_SERVER);
        Set<ConfigQueryEntry> hive = serviceEntry(bigCluster, HIVE_METASTORE);

        assertEquals(1, rangerAdmins.size());
        assertTrue(rangerAdmins.stream()
                .anyMatch(cqe -> cqe.getDefaultPath().equals("hwx-remote/ranger/audit")));

        assertEquals(1, yarnSite.size());
        assertTrue(yarnSite.stream()
                .anyMatch(cqe -> cqe.getDefaultPath().equals("hwx-remote/bigCluster/oplogs/yarn-app-logs")));

        assertEquals(1, zeppelin.size());
        assertTrue(zeppelin.stream()
                .anyMatch(cqe -> cqe.getDefaultPath().equals("hwx-remote/bigCluster/zeppelin/notebook")));

        assertEquals(3, hive.size());
        assertTrue(hive.stream()
                .anyMatch(cqe -> cqe.getDefaultPath().equals("hwx-remote/warehouse/tablespace/managed/hive")));
        assertTrue(hive.stream()
                .anyMatch(cqe -> cqe.getDefaultPath().equals("hwx-remote/warehouse/tablespace/external/hive")));
    }

    @Test
    void testWhenHiveMetasoreAndRangerAdminIsPresentedDoubleAndNotAttachedThenShouldReturnWithRangerConfigs() {
        Map<String, Set<String>> map = new HashMap<>();
        map.put("master", Sets.newHashSet(HIVE_METASTORE, RANGER_ADMIN, NAMENODE));
        map.put("slave_1", Sets.newHashSet(HIVE_METASTORE, RANGER_ADMIN));

        prepareBlueprintProcessorFactoryMock(map);
        FileSystemConfigQueryObject fileSystemConfigQueryObject = FileSystemConfigQueryObject.Builder.builder()
                .withStorageName(STORAGE_NAME)
                .withClusterName(CLUSTER_NAME)
                .withBlueprintText(BLUEPRINT_TEXT)
                .withAttachedCluster(false)
                .withFileSystemType(FileSystemType.S3.name())
                .build();
        Set<ConfigQueryEntry> bigCluster = underTest.queryParameters(fileSystemConfigQueryObject);

        assertEquals(4L, bigCluster.size());

        Set<ConfigQueryEntry> rangerAdmins = serviceEntry(bigCluster, RANGER_ADMIN);
        Set<ConfigQueryEntry> hive = serviceEntry(bigCluster, HIVE_METASTORE);

        assertEquals(1, rangerAdmins.size());
        assertTrue(rangerAdmins.stream()
                .anyMatch(cqe -> cqe.getDefaultPath().equals("hwx-remote/ranger/audit")));

        assertEquals(3, hive.size());
        assertTrue(hive.stream()
                .anyMatch(cqe -> cqe.getDefaultPath().equals("hwx-remote/warehouse/tablespace/managed/hive")));
        assertTrue(hive.stream()
                .anyMatch(cqe -> cqe.getDefaultPath().equals("hwx-remote/warehouse/tablespace/external/hive")));
    }

    @Test
    void testWhenHiveMetasoreAndRangerAdminIsPresentedDoubleAndAttachedThenShouldReturnWithRangerConfigs() {
        Map<String, Set<String>> map = new HashMap<>();
        map.put("master", Sets.newHashSet(HIVE_METASTORE, RANGER_ADMIN, NAMENODE));
        map.put("slave_1", Sets.newHashSet(HIVE_METASTORE, RANGER_ADMIN));

        prepareBlueprintProcessorFactoryMock(map);
        FileSystemConfigQueryObject fileSystemConfigQueryObject = FileSystemConfigQueryObject.Builder.builder()
                .withStorageName(STORAGE_NAME)
                .withClusterName(CLUSTER_NAME)
                .withBlueprintText(BLUEPRINT_TEXT)
                .withAttachedCluster(true)
                .withFileSystemType(FileSystemType.S3.name())
                .build();
        Set<ConfigQueryEntry> bigCluster = underTest.queryParameters(fileSystemConfigQueryObject);

        assertEquals(1L, bigCluster.size());

        Set<ConfigQueryEntry> rangerAdmins = serviceEntry(bigCluster, RANGER_ADMIN);

        assertEquals(1, rangerAdmins.size());
        assertTrue(rangerAdmins.stream()
                .anyMatch(cqe -> cqe.getDefaultPath().equals("hwx-remote/ranger/audit")));
    }

    @Test
    void testWhenOnlyRangerAdminIsPresentedThenShouldReturnWithOnlyRangerAdminConfigs() {
        prepareBlueprintProcessorFactoryMock(RANGER_ADMIN, NAMENODE);
        FileSystemConfigQueryObject fileSystemConfigQueryObject = FileSystemConfigQueryObject.Builder.builder()
                .withStorageName(STORAGE_NAME)
                .withClusterName(CLUSTER_NAME)
                .withBlueprintText(BLUEPRINT_TEXT)
                .withAttachedCluster(false)
                .withFileSystemType(FileSystemType.S3.name())
                .build();
        Set<ConfigQueryEntry> bigCluster = underTest.queryParameters(fileSystemConfigQueryObject);

        assertEquals(1L, bigCluster.size());

        Set<ConfigQueryEntry> rangerAdmins = serviceEntry(bigCluster, RANGER_ADMIN);

        assertEquals(1, rangerAdmins.size());

        assertTrue(rangerAdmins.stream()
                .anyMatch(cqe -> cqe.getDefaultPath().equals("hwx-remote/ranger/audit")));
    }

    @Test
    void testWhenAttachedClusterAndHiveMetastorePresentedThenShouldReturnWithNothing() {
        prepareBlueprintProcessorFactoryMock(HIVE_METASTORE, NAMENODE);
        FileSystemConfigQueryObject fileSystemConfigQueryObject = FileSystemConfigQueryObject.Builder.builder()
                .withStorageName(STORAGE_NAME)
                .withClusterName(CLUSTER_NAME)
                .withBlueprintText(BLUEPRINT_TEXT)
                .withAttachedCluster(true)
                .withFileSystemType(FileSystemType.S3.name())
                .build();
        Set<ConfigQueryEntry> bigCluster = underTest.queryParameters(fileSystemConfigQueryObject);

        assertEquals(0L, bigCluster.size());
    }

    @Test
    void testWhenNotAttachedClusterAndOnlyHiveMetastoreIsPresentedThenShouldReturnWithOnlyHiveMetastoreConfigs() {
        prepareBlueprintProcessorFactoryMock(HIVE_METASTORE, NAMENODE);
        FileSystemConfigQueryObject fileSystemConfigQueryObject = FileSystemConfigQueryObject.Builder.builder()
                .withStorageName(STORAGE_NAME)
                .withClusterName(CLUSTER_NAME)
                .withBlueprintText(BLUEPRINT_TEXT)
                .withAttachedCluster(false)
                .withFileSystemType(FileSystemType.S3.name())
                .build();
        Set<ConfigQueryEntry> bigCluster = underTest.queryParameters(fileSystemConfigQueryObject);

        assertEquals(3L, bigCluster.size());

        Set<ConfigQueryEntry> hive = serviceEntry(bigCluster, HIVE_METASTORE);

        assertEquals(3, hive.size());
        assertTrue(hive.stream().map(ConfigQueryEntry::getDefaultPath)
                .anyMatch("hwx-remote/warehouse/tablespace/managed/hive"::equals));
        assertTrue(hive.stream().map(ConfigQueryEntry::getDefaultPath)
                .anyMatch("hwx-remote/warehouse/tablespace/external/hive"::equals));
        assertTrue(hive.stream().map(ConfigQueryEntry::getDefaultPath)
                .anyMatch("hwx-remote/warehouse/tablespace/external/hive"::equals));
    }

    @Test
    void testDatalakeClusterPaths() {
        prepareBlueprintProcessorFactoryMock(RANGER_ADMIN, RESOURCEMANAGER, NAMENODE);
        FileSystemConfigQueryObject fileSystemConfigQueryObject = FileSystemConfigQueryObject.Builder.builder()
                .withStorageName(STORAGE_NAME)
                .withClusterName(CLUSTER_NAME)
                .withBlueprintText(BLUEPRINT_TEXT)
                .withDatalakeCluster(true)
                .withFileSystemType(FileSystemType.S3.name())
                .build();
        Set<ConfigQueryEntry> bigCluster = underTest.queryParameters(fileSystemConfigQueryObject);
        assertEquals(2L, bigCluster.size());

        Set<ConfigQueryEntry> rangerAdmin = serviceEntry(bigCluster, RANGER_ADMIN);
        Set<ConfigQueryEntry> yarn = serviceEntry(bigCluster, RESOURCEMANAGER);

        assertEquals(1, rangerAdmin.size());
        assertTrue(rangerAdmin.stream().map(ConfigQueryEntry::getDefaultPath)
                .anyMatch("hwx-remote/ranger/audit"::equals));

        assertEquals(1, yarn.size());
        assertTrue(yarn.stream().map(ConfigQueryEntry::getDefaultPath)
                .anyMatch("hwx-remote/oplogs/yarn-app-logs"::equals));
    }

    @Test
    void testStandaloneClusterPaths() {
        prepareBlueprintProcessorFactoryMock(RANGER_ADMIN, ZEPPELIN_SERVER, HBASE_MASTER, NAMENODE);
        FileSystemConfigQueryObject fileSystemConfigQueryObject = FileSystemConfigQueryObject.Builder.builder()
                .withStorageName(STORAGE_NAME)
                .withClusterName(CLUSTER_NAME)
                .withBlueprintText(BLUEPRINT_TEXT)
                .withDatalakeCluster(false)
                .withAttachedCluster(false)
                .withFileSystemType(FileSystemType.S3.name())
                .build();
        Set<ConfigQueryEntry> bigCluster = underTest.queryParameters(fileSystemConfigQueryObject);
        assertEquals(3L, bigCluster.size());

        Set<ConfigQueryEntry> rangerAdmin = serviceEntry(bigCluster, RANGER_ADMIN);
        Set<ConfigQueryEntry> zeppelin = serviceEntry(bigCluster, ZEPPELIN_SERVER);
        Set<ConfigQueryEntry> hbaseMaster = serviceEntry(bigCluster, HBASE_MASTER);


        assertEquals(1, rangerAdmin.size());
        assertTrue(rangerAdmin.stream().map(ConfigQueryEntry::getDefaultPath)
                .anyMatch("hwx-remote/ranger/audit"::equals));

        assertEquals(1, zeppelin.size());
        assertTrue(zeppelin.stream().map(ConfigQueryEntry::getDefaultPath)
                .anyMatch("hwx-remote/bigCluster/zeppelin/notebook"::equals));

        assertEquals(1, hbaseMaster.size());
        assertTrue(hbaseMaster.stream().map(ConfigQueryEntry::getDefaultPath)
                .anyMatch("hwx-remote/bigCluster/hbase"::equals));
    }

    @Test
    void testAttachedClusterPaths() {
        prepareBlueprintProcessorFactoryMock(RANGER_ADMIN, RESOURCEMANAGER, ZEPPELIN_SERVER, HBASE_MASTER, NAMENODE);
        FileSystemConfigQueryObject fileSystemConfigQueryObject = FileSystemConfigQueryObject.Builder.builder()
                .withStorageName(STORAGE_NAME)
                .withClusterName(CLUSTER_NAME)
                .withBlueprintText(BLUEPRINT_TEXT)
                .withDatalakeCluster(false)
                .withAttachedCluster(true)
                .withFileSystemType(FileSystemType.S3.name())
                .build();
        Set<ConfigQueryEntry> bigCluster = underTest.queryParameters(fileSystemConfigQueryObject);
        assertEquals(4L, bigCluster.size());

        Set<ConfigQueryEntry> rangerAdmin = serviceEntry(bigCluster, RANGER_ADMIN);
        Set<ConfigQueryEntry> yarnLogs = serviceEntry(bigCluster, RESOURCEMANAGER);
        Set<ConfigQueryEntry> zeppelin = serviceEntry(bigCluster, ZEPPELIN_SERVER);
        Set<ConfigQueryEntry> hbaseMaster = serviceEntry(bigCluster, HBASE_MASTER);

        assertEquals(1, rangerAdmin.size());
        assertTrue(rangerAdmin.stream().map(ConfigQueryEntry::getDefaultPath)
                .anyMatch("hwx-remote/ranger/audit"::equals));

        assertEquals(1, yarnLogs.size());
        assertTrue(yarnLogs.stream().map(ConfigQueryEntry::getDefaultPath)
                .anyMatch("hwx-remote/oplogs/yarn-app-logs"::equals));

        assertEquals(1, zeppelin.size());
        assertTrue(zeppelin.stream().map(ConfigQueryEntry::getDefaultPath)
                .anyMatch("hwx-remote/bigCluster/zeppelin/notebook"::equals));

        assertEquals(1, hbaseMaster.size());
        assertTrue(hbaseMaster.stream().map(ConfigQueryEntry::getDefaultPath)
                .anyMatch("hwx-remote/bigCluster/hbase"::equals));
    }

    @Test
    void testProfilerServicesWithAttachedCluster() {
        prepareBlueprintProcessorFactoryMock(DATA_DISCOVERY_SERVICE_AGENT, PROFILER_ADMIN_AGENT, PROFILER_METRICS_AGENT, PROFILER_SCHEDULER_AGENT, NAMENODE);
        FileSystemConfigQueryObject fileSystemConfigQueryObject = FileSystemConfigQueryObject.Builder.builder()
                .withStorageName(STORAGE_NAME)
                .withClusterName(CLUSTER_NAME)
                .withBlueprintText(BLUEPRINT_TEXT)
                .withAttachedCluster(true)
                .withFileSystemType(FileSystemType.S3.name())
                .build();
        Set<ConfigQueryEntry> bigCluster = underTest.queryParameters(fileSystemConfigQueryObject);
        assertEquals(1L, bigCluster.size());

        Set<ConfigQueryEntry> dataDiscoveryService = serviceEntry(bigCluster, DATA_DISCOVERY_SERVICE_AGENT);
        Set<ConfigQueryEntry> profilerAdmin = serviceEntry(bigCluster, PROFILER_ADMIN_AGENT);
        Set<ConfigQueryEntry> profilerMetrics = serviceEntry(bigCluster, PROFILER_METRICS_AGENT);
        Set<ConfigQueryEntry> profilerScheduler = serviceEntry(bigCluster, PROFILER_SCHEDULER_AGENT);

        assertEquals(1, dataDiscoveryService.size());
        assertTrue(dataDiscoveryService.stream().map(ConfigQueryEntry::getDefaultPath)
                .anyMatch("hwx-remote/dpprofiler"::equals));
        assertTrue(dataDiscoveryService.stream().map(ConfigQueryEntry::getPropertyName)
                .anyMatch("file_system_uri"::equals));

        assertEquals(1, profilerAdmin.size());
        assertTrue(profilerAdmin.stream().map(ConfigQueryEntry::getDefaultPath)
                .anyMatch("hwx-remote/dpprofiler"::equals));
        assertTrue(profilerAdmin.stream().map(ConfigQueryEntry::getPropertyName)
                .anyMatch("file_system_uri"::equals));


        assertEquals(1, profilerMetrics.size());
        assertTrue(profilerMetrics.stream().map(ConfigQueryEntry::getDefaultPath)
                .anyMatch("hwx-remote/dpprofiler"::equals));
        assertTrue(profilerMetrics.stream().map(ConfigQueryEntry::getPropertyName)
                .anyMatch("file_system_uri"::equals));

        assertEquals(1, profilerScheduler.size());
        assertTrue(profilerScheduler.stream().map(ConfigQueryEntry::getDefaultPath)
                .anyMatch("hwx-remote/dpprofiler"::equals));
        assertTrue(profilerScheduler.stream().map(ConfigQueryEntry::getPropertyName)
                .anyMatch("file_system_uri"::equals));
    }

    @Test
    void testBasePathWhenNoHDFSServicesWithAttachedCluster() {
        prepareBlueprintProcessorFactoryMock();
        FileSystemConfigQueryObject fileSystemConfigQueryObject = FileSystemConfigQueryObject.Builder.builder()
                .withStorageName(STORAGE_NAME)
                .withClusterName(CLUSTER_NAME)
                .withBlueprintText(BLUEPRINT_TEXT)
                .withAttachedCluster(true)
                .withFileSystemType(FileSystemType.S3.name())
                .build();
        Set<ConfigQueryEntry> bigCluster = underTest.queryParameters(fileSystemConfigQueryObject);
        assertEquals(1L, bigCluster.size());

        Set<ConfigQueryEntry> defaultFS = missingServiceEntry(bigCluster, NAMENODE);

        assertEquals(1, defaultFS.size());
        assertTrue(defaultFS.stream().map(ConfigQueryEntry::getDefaultPath)
                .anyMatch("hwx-remote/bigCluster"::equals));
        assertTrue(defaultFS.stream().map(ConfigQueryEntry::getPropertyName)
                .anyMatch("core_defaultfs"::equals));
    }

    private Set<ConfigQueryEntry> serviceEntry(Set<ConfigQueryEntry> configQueryEntries, String serviceName) {
        return configQueryEntries.stream().filter(b -> b.getRelatedServices().stream()
                .anyMatch(service -> service.equals(serviceName))).collect(Collectors.toSet());
    }

    private Set<ConfigQueryEntry> missingServiceEntry(Set<ConfigQueryEntry> configQueryEntries, String serviceName) {
        return configQueryEntries.stream().filter(b -> b.getRelatedMissingServices().stream()
                .anyMatch(service -> service.equals(serviceName))).collect(Collectors.toSet());
    }

    @Test
    void testCopyOfContigEntries() {
        prepareBlueprintProcessorFactoryMock(HIVE_SERVER, RANGER_ADMIN);

        Set<ConfigQueryEntry> bigCluster = getConfigQueryEntriesS3(STORAGE_NAME);

        Set<ConfigQueryEntry> hiveServerEntries = serviceEntry(bigCluster, HIVE_SERVER);
        Set<ConfigQueryEntry> rangerAdminEntries = serviceEntry(bigCluster, RANGER_ADMIN);

        assertEquals(0, hiveServerEntries.size());
        assertEquals(1, rangerAdminEntries.size());

        bigCluster = getConfigQueryEntriesS3(STORAGE_NAME + "copy");

        hiveServerEntries = serviceEntry(bigCluster, HIVE_SERVER);
        rangerAdminEntries = serviceEntry(bigCluster, RANGER_ADMIN);

        assertEquals(0, hiveServerEntries.size());
        assertEquals(1, rangerAdminEntries.size());
    }

    @Test
    void testSubFolder() {
        prepareBlueprintProcessorFactoryMock(RANGER_ADMIN, ZEPPELIN_SERVER, HBASE_MASTER, NAMENODE);

        FileSystemConfigQueryObject fileSystemConfigQueryObject = FileSystemConfigQueryObject.Builder.builder()
                .withStorageName("hwx-remote.dfs.core.windows.net/subfolder")
                .withClusterName(CLUSTER_NAME)
                .withBlueprintText(BLUEPRINT_TEXT)
                .withDatalakeCluster(false)
                .withAttachedCluster(false)
                .withFileSystemType(FileSystemType.ADLS_GEN_2.name())
                .build();
        Set<ConfigQueryEntry> bigCluster = underTest.queryParameters(fileSystemConfigQueryObject);
        assertEquals(3L, bigCluster.size());

        Set<ConfigQueryEntry> rangerAdmin = serviceEntry(bigCluster, RANGER_ADMIN);
        Set<ConfigQueryEntry> zeppelin = serviceEntry(bigCluster, ZEPPELIN_SERVER);
        Set<ConfigQueryEntry> hbaseMaster = serviceEntry(bigCluster, HBASE_MASTER);


        assertEquals(1, rangerAdmin.size());
        assertTrue(rangerAdmin.stream().map(ConfigQueryEntry::getDefaultPath)
                .anyMatch("hwx-remote.dfs.core.windows.net/subfolder/ranger/audit"::equals));

        assertEquals(1, zeppelin.size());
        assertTrue(zeppelin.stream().map(ConfigQueryEntry::getDefaultPath)
                .anyMatch("hwx-remote.dfs.core.windows.net/subfolder/bigCluster/zeppelin/notebook"::equals));

        assertEquals(1, hbaseMaster.size());
        assertTrue(hbaseMaster.stream().map(ConfigQueryEntry::getDefaultPath)
                .anyMatch("hwx-remote.dfs.core.windows.net/subfolder/bigCluster/hbase"::equals));
    }

    private Set<ConfigQueryEntry> getConfigQueryEntriesS3(String storageName) {
        FileSystemConfigQueryObject fileSystemConfigQueryObject = FileSystemConfigQueryObject.Builder.builder()
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
        CmTemplateProcessor cmTemplateProcessor = mock(CmTemplateProcessor.class);
        when(cmTemplateProcessor.getComponentsByHostGroup()).thenReturn(entries);
        when(cmTemplateProcessorFactory.get(anyString())).thenReturn(cmTemplateProcessor);
    }
}
