package com.sequenceiq.cloudbreak.service.sharedservice;


import static com.sequenceiq.cloudbreak.api.endpoint.v4.database.base.DatabaseType.HIVE;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.database.base.DatabaseType.RANGER;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.ResourceStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.database.base.DatabaseType;
import com.sequenceiq.cloudbreak.common.converter.MissingResourceNameGenerator;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.type.APIResourceType;
import com.sequenceiq.cloudbreak.domain.FileSystem;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.domain.StorageLocation;
import com.sequenceiq.cloudbreak.domain.StorageLocations;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.DatalakeResources;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.common.api.filesystem.FileSystemType;

@RunWith(MockitoJUnitRunner.class)
public class RemoteDataContextWorkaroundServiceTest {

    private static final Random RANDOM = new Random();

    @Mock
    private StackService stackService;

    @Mock
    private MissingResourceNameGenerator nameGenerator;

    @InjectMocks
    private RemoteDataContextWorkaroundService underTest;

    @Test
    public void testRdsConfigsWhenHivePresentedShouldReturnWithHiveConfigs() {
        Set<RDSConfig> rdsConfigs = underTest
                .prepareRdsConfigs(
                        mockRequestedCluster(mockRds(HIVE), mockRds(RANGER)),
                        mockDatalakeResources(mockRds(HIVE, true)));

        Assert.assertEquals(2, rdsConfigs.size());
        Assert.assertTrue(get(rdsConfigs, HIVE).getStatus().equals(ResourceStatus.DEFAULT));
    }

    @Test
    public void testRdsConfigsWhenHiveNotPresentedShouldReturnWithHiveConfigs() {
        Set<RDSConfig> rdsConfigs = underTest
                .prepareRdsConfigs(
                        mockRequestedCluster(mockRds(RANGER)),
                        mockDatalakeResources(mockRds(HIVE, true)));

        Assert.assertEquals(2, rdsConfigs.size());
        Assert.assertTrue(get(rdsConfigs, HIVE).getStatus().equals(ResourceStatus.DEFAULT));
    }

    @Test
    public void testRdsConfigsWhenHiveNotInDatalakeShouldReturnWithDistroXHiveConfigs() {
        Set<RDSConfig> rdsConfigs = underTest
                .prepareRdsConfigs(
                        mockRequestedCluster(mockRds(RANGER), mockRds(HIVE)),
                        mockDatalakeResources());

        Assert.assertEquals(2, rdsConfigs.size());
        Assert.assertTrue(get(rdsConfigs, HIVE).getStatus().equals(ResourceStatus.USER_MANAGED));
    }

    @Test
    public void testFileSystemWhenHivePathPresentedInDistroXButSdxDoesNotContainsItShouldReturnWithDistroXConfigs() throws IOException {
        when(nameGenerator.generateName(APIResourceType.FILESYSTEM)).thenReturn("appletree");
        when(stackService.getById(anyLong())).thenReturn(mockStack());

        FileSystem fileSystem = underTest
                .prepareFilesytem(
                        mockRequestedCluster(mockStorageLocation(3), mockStorageLocation(4)),
                        mockDatalakeResources());

        Set<StorageLocation> locations = fileSystem.getLocations().get(StorageLocations.class).getLocations();
        Assert.assertEquals(2, locations.size());
        Assert.assertTrue(hasPropretyWithIndex(locations, 3));
        Assert.assertTrue(hasPropretyWithIndex(locations, 4));
    }

    @Test
    public void testFileSystemWhenHiveInSdxAndDistroXShouldReturnWithSdxHiveConfigs() throws IOException {
        when(nameGenerator.generateName(APIResourceType.FILESYSTEM)).thenReturn("appletree");
        when(stackService.getById(anyLong())).thenReturn(mockStack(mockStorageLocation(1), mockStorageLocation(2)));

        FileSystem fileSystem = underTest
                .prepareFilesytem(
                        mockRequestedCluster(mockStorageLocation(3), mockStorageLocation(4)),
                        mockDatalakeResources());

        Set<StorageLocation> locations = fileSystem.getLocations().get(StorageLocations.class).getLocations();
        Assert.assertEquals(2, locations.size());
        Assert.assertTrue(hasPropretyWithIndex(locations, 1));
        Assert.assertTrue(hasPropretyWithIndex(locations, 2));
    }

    @Test
    public void testFileSystemWhenHivePathNotPresentedInDistroXButSdxDoesContainsItShouldReturnWithSdxConfigs() throws IOException {
        when(nameGenerator.generateName(APIResourceType.FILESYSTEM)).thenReturn("appletree");
        when(stackService.getById(anyLong())).thenReturn(mockStack(mockStorageLocation(1), mockStorageLocation(2)));

        FileSystem fileSystem = underTest
                .prepareFilesytem(
                        mockRequestedCluster(mockRds(HIVE)),
                        mockDatalakeResources());

        Set<StorageLocation> locations = fileSystem.getLocations().get(StorageLocations.class).getLocations();
        Assert.assertEquals(2, locations.size());
        Assert.assertTrue(hasPropretyWithIndex(locations, 1));
        Assert.assertTrue(hasPropretyWithIndex(locations, 2));
    }

    @Test
    public void testFileSystemWhenHivePathNotPresentedInDistroXAndSdxDoesNotContainsItShouldReturnWithSdxConfigs() throws IOException {
        when(nameGenerator.generateName(APIResourceType.FILESYSTEM)).thenReturn("appletree");
        when(stackService.getById(anyLong())).thenReturn(mockStack());

        FileSystem fileSystem = underTest
                .prepareFilesytem(
                        mockRequestedCluster(mockRds(HIVE)),
                        mockDatalakeResources());

        Set<StorageLocation> locations = fileSystem.getLocations().get(StorageLocations.class).getLocations();
        Assert.assertEquals(0, locations.size());
    }

    private RDSConfig get(Set<RDSConfig> rdsConfigs, DatabaseType databaseType) {
        return rdsConfigs
                .stream()
                .filter(r -> r.getType().equals(databaseType.name()))
                .collect(Collectors.toSet())
                .iterator()
                .next();
    }

    private boolean hasPropretyWithIndex(Set<StorageLocation> locations, int index) {
        return !locations
                .stream()
                .filter(r -> r.getValue().equals("hive" + index))
                .collect(Collectors.toSet())
                .isEmpty();
    }

    private DatalakeResources mockDatalakeResources(RDSConfig... rds) {
        DatalakeResources datalakeResources = new DatalakeResources();
        datalakeResources.setId(1L);
        datalakeResources.setDatalakeStackId(1L);
        datalakeResources.setRdsConfigs(new HashSet<>());
        for (RDSConfig r : rds) {
            datalakeResources.getRdsConfigs().add(r);
        }

        return datalakeResources;
    }

    private RDSConfig mockRds(DatabaseType type, boolean defaultType) {
        RDSConfig rds = new RDSConfig();
        long random = RANDOM.nextLong();
        rds.setId(random);
        rds.setName(type.name() + random);
        rds.setType(type.name());
        rds.setStatus(defaultType ? ResourceStatus.DEFAULT : ResourceStatus.USER_MANAGED);
        return rds;
    }

    private RDSConfig mockRds(DatabaseType type) {
        return mockRds(type, false);
    }

    private Cluster mockRequestedCluster(RDSConfig... rds) {
        Cluster cluster = new Cluster();
        cluster.setRdsConfigs(new HashSet<>());
        for (RDSConfig r : rds) {
            cluster.getRdsConfigs().add(r);
        }
        return cluster;
    }

    private StorageLocation mockStorageLocation(int index) {
        StorageLocation storageLocation = new StorageLocation();
        storageLocation.setConfigFile("hive" + index);
        storageLocation.setValue("hive" + index);
        storageLocation.setProperty("hive" + index);
        return storageLocation;
    }

    private Cluster mockRequestedCluster(StorageLocation... storageLocations) {
        Cluster cluster = new Cluster();
        FileSystem fileSystem = new FileSystem();
        fileSystem.setType(FileSystemType.S3);
        StorageLocations sl = new StorageLocations();
        Set<StorageLocation> locations = new HashSet<>();
        for (StorageLocation storageLocation : storageLocations) {
            locations.add(storageLocation);
        }
        sl.setLocations(locations);
        fileSystem.setLocations(new Json(sl));
        cluster.setFileSystem(fileSystem);
        return cluster;
    }

    private Stack mockStack(StorageLocation... storageLocations) {
        Stack stack = new Stack();
        Cluster cluster = new Cluster();
        cluster.setRdsConfigs(new HashSet<>());
        FileSystem fileSystem = new FileSystem();
        fileSystem.setType(FileSystemType.S3);
        StorageLocations sl = new StorageLocations();
        Set<StorageLocation> locations = new HashSet<>();

        for (StorageLocation storageLocation : storageLocations) {
            locations.add(storageLocation);
        }

        sl.setLocations(locations);
        fileSystem.setLocations(new Json(sl));
        cluster.setFileSystem(fileSystem);
        stack.setCluster(cluster);
        return stack;
    }

}