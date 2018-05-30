package com.sequenceiq.cloudbreak.blueprint.filesystem.query;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
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
import com.sequenceiq.cloudbreak.blueprint.BlueprintTextProcessor;
import com.sequenceiq.cloudbreak.blueprint.filesystem.FileSystemConfigQueryObject;
import com.sequenceiq.cloudbreak.service.CloudbreakResourceReaderService;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;

@RunWith(MockitoJUnitRunner.class)
public class FileSystemConfigQueryServiceTest {

    private static final String BLUEPRINT_TEXT = "testblueprint";

    private static final String HIVE_METASTORE = "HIVE_METASTORE";

    private static final String RANGER_ADMIN = "RANGER_ADMIN";

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
        prepareBlueprintProcessorFactoryMock(new String[]{HIVE_METASTORE, RANGER_ADMIN});
        FileSystemConfigQueryObject fileSystemConfigQueryObject = FileSystemConfigQueryObject.Builder.builder()
                .withStorageName(STORAGE_NAME)
                .withClusterName(CLUSTER_NAME)
                .withBlueprintText(BLUEPRINT_TEXT)
                .withFileSystemType(FileSystemType.ADLS.name())
                .build();
        List<ConfigQueryEntry> bigCluster = underTest.queryParameters(fileSystemConfigQueryObject);

        Assert.assertEquals(2, bigCluster.size());

        Optional<ConfigQueryEntry> hiveMetastore = serviceEntry(bigCluster, HIVE_METASTORE);
        Optional<ConfigQueryEntry> rangerAdmin = serviceEntry(bigCluster, RANGER_ADMIN);

        Assert.assertTrue(hiveMetastore.isPresent());
        Assert.assertTrue(rangerAdmin.isPresent());

        Assert.assertEquals(hiveMetastore.get().getDefaultPath(), "hwx-remote/bigCluster/apps/hive/warehouse");
        Assert.assertEquals(rangerAdmin.get().getDefaultPath(), "hwx-remote/bigCluster/apps/ranger/audit/bigCluster");
    }

    @Test
    public void testWhenOnlyRangerAdminIsPresentedThenShouldReturnWithOnlyRangerAdminConfigs() {
        prepareBlueprintProcessorFactoryMock(new String[]{RANGER_ADMIN});
        FileSystemConfigQueryObject fileSystemConfigQueryObject = FileSystemConfigQueryObject.Builder.builder()
                .withStorageName(STORAGE_NAME)
                .withClusterName(CLUSTER_NAME)
                .withBlueprintText(BLUEPRINT_TEXT)
                .withFileSystemType(FileSystemType.ADLS.name())
                .build();
        List<ConfigQueryEntry> bigCluster = underTest.queryParameters(fileSystemConfigQueryObject);

        Assert.assertEquals(1, bigCluster.size());

        Optional<ConfigQueryEntry> hiveMetastore = serviceEntry(bigCluster, HIVE_METASTORE);
        Optional<ConfigQueryEntry> rangerAdmin = serviceEntry(bigCluster, RANGER_ADMIN);

        Assert.assertFalse(hiveMetastore.isPresent());
        Assert.assertTrue(rangerAdmin.isPresent());
        Assert.assertEquals(rangerAdmin.get().getDefaultPath(), "hwx-remote/bigCluster/apps/ranger/audit/bigCluster");
    }

    @Test
    public void testWhenOnlyHiveMetastoreIsPresentedThenShouldReturnWithOnlyHiveMetastoreConfigs() {
        prepareBlueprintProcessorFactoryMock(new String[]{HIVE_METASTORE});
        FileSystemConfigQueryObject fileSystemConfigQueryObject = FileSystemConfigQueryObject.Builder.builder()
                .withStorageName(STORAGE_NAME)
                .withClusterName(CLUSTER_NAME)
                .withBlueprintText(BLUEPRINT_TEXT)
                .withFileSystemType(FileSystemType.ADLS.name())
                .build();
        List<ConfigQueryEntry> bigCluster = underTest.queryParameters(fileSystemConfigQueryObject);

        Assert.assertEquals(1, bigCluster.size());

        Optional<ConfigQueryEntry> hiveMetastore = serviceEntry(bigCluster, HIVE_METASTORE);
        Optional<ConfigQueryEntry> rangerAdmin = serviceEntry(bigCluster, RANGER_ADMIN);

        Assert.assertTrue(hiveMetastore.isPresent());
        Assert.assertFalse(rangerAdmin.isPresent());
        Assert.assertEquals(hiveMetastore.get().getDefaultPath(), "hwx-remote/bigCluster/apps/hive/warehouse");
    }

    @Test
    public void testWhenHiveMetastoreAndRangerAdminIsNotPresentedThenShouldReturnWithOutThoseConfigs() {
        prepareBlueprintProcessorFactoryMock(new String[]{});
        FileSystemConfigQueryObject fileSystemConfigQueryObject = FileSystemConfigQueryObject.Builder.builder()
                .withStorageName(STORAGE_NAME)
                .withClusterName(CLUSTER_NAME)
                .withBlueprintText(BLUEPRINT_TEXT)
                .withFileSystemType(FileSystemType.ADLS.name())
                .build();
        List<ConfigQueryEntry> bigCluster = underTest.queryParameters(fileSystemConfigQueryObject);

        Assert.assertEquals(0, bigCluster.size());

        Optional<ConfigQueryEntry> hiveMetastore = serviceEntry(bigCluster, HIVE_METASTORE);
        Optional<ConfigQueryEntry> rangerAdmin = serviceEntry(bigCluster, RANGER_ADMIN);

        Assert.assertFalse(hiveMetastore.isPresent());
        Assert.assertFalse(rangerAdmin.isPresent());
    }

    private Optional<ConfigQueryEntry> serviceEntry(List<ConfigQueryEntry> configQueryEntries, String serviceName) {
        return configQueryEntries.stream().filter(b -> b.getRelatedService().equals(serviceName)).findFirst();
    }

    private void prepareBlueprintProcessorFactoryMock(String... services) {
        Map<String, Set<String>> result = new HashMap<>();
        result.put("master", Sets.newHashSet(services));

        BlueprintTextProcessor blueprintTextProcessorMock = mock(BlueprintTextProcessor.class);
        when(blueprintTextProcessorMock.getComponentsByHostGroup()).thenReturn(result);
        when(blueprintProcessorFactory.get(anyString())).thenReturn(blueprintTextProcessorMock);
    }
}