package com.sequenceiq.cloudbreak.service.sharedservice;


import static com.sequenceiq.cloudbreak.api.endpoint.v4.database.base.DatabaseType.HIVE;
import static com.sequenceiq.common.model.CloudStorageCdpService.DEFAULT_FS;
import static com.sequenceiq.common.model.CloudStorageCdpService.REMOTE_FS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.ResourceStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.database.base.DatabaseType;
import com.sequenceiq.cloudbreak.common.converter.ResourceNameGenerator;
import com.sequenceiq.cloudbreak.common.type.APIResourceType;
import com.sequenceiq.cloudbreak.domain.FileSystem;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.domain.cloudstorage.CloudStorage;
import com.sequenceiq.cloudbreak.domain.cloudstorage.StorageLocation;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.sdx.common.model.SdxFileSystemView;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.common.model.CloudStorageCdpService;
import com.sequenceiq.common.model.FileSystemType;

@ExtendWith(MockitoExtension.class)
public class RemoteDataContextWorkaroundServiceTest {

    private static final String DATALAKE_CRN = "crn:cdp:datalake:us-west-1:460c0d8f-ae8e-4dce-9cd7-2351762eb9ac:datalake:6b2b1600-8ac6-4c26-aa34-dab36f4bd243";

    private static final Random RANDOM = new Random();

    private static final String CLUSTER_NAME = "clusterName";

    @Mock
    private StackService stackService;

    @Mock
    private ResourceNameGenerator nameGenerator;

    @InjectMocks
    private RemoteDataContextWorkaroundService underTest;

    @Test
    public void testFileSystemWhenHivePathPresentedInDistroXButSdxDoesNotContainsItShouldReturnWithDistroXConfigs() throws IOException {
        FileSystem fileSystem = underTest
                .prepareFilesystem(
                        mockRequestedCluster(mockNonHiveStorageLocation(3), mockNonHiveStorageLocation(4)),
                        mockSdxFileSystemView(), DATALAKE_CRN);

        List<StorageLocation> locations = fileSystem.getCloudStorage().getLocations();
        assertEquals(2, locations.size());
        assertTrue(hasPropretyWithIndex(locations, 3));
        assertTrue(hasPropretyWithIndex(locations, 4));
    }

    @Test
    public void testFileSystemWhenHiveInSdxAndDistroXShouldReturnWithSdxHiveConfigs() throws IOException {
        FileSystem fileSystem = underTest
                .prepareFilesystem(
                        mockRequestedCluster(mockNonHiveStorageLocation(3), mockNonHiveStorageLocation(4)),
                        mockSdxFileSystemView(mockHiveStorageLocation(1), mockHiveStorageLocation(2)), DATALAKE_CRN);

        List<StorageLocation> locations = fileSystem.getCloudStorage().getLocations();
        assertEquals(4, locations.size());
        assertTrue(hasPropretyWithIndex(locations, 1));
        assertTrue(hasPropretyWithIndex(locations, 2));
    }

    @Test
    public void testFileSystemWhenHivePathNotPresentedInDistroXButSdxDoesContainsItShouldReturnWithSdxConfigs() throws IOException {
        when(nameGenerator.generateName(APIResourceType.FILESYSTEM)).thenReturn("appletree");

        FileSystem fileSystem = underTest
                .prepareFilesystem(
                        mockRequestedCluster(mockRds(HIVE)),
                        mockSdxFileSystemView(mockHiveStorageLocation(1), mockHiveStorageLocation(2)), DATALAKE_CRN);

        List<StorageLocation> locations = fileSystem.getCloudStorage().getLocations();
        assertEquals(2, locations.size());
        assertTrue(hasPropretyWithIndex(locations, 1));
        assertTrue(hasPropretyWithIndex(locations, 2));
    }

    @Test
    public void testFileSystemWhenHivePathNotPresentedInDistroXAndSdxDoesNotContainsItShouldReturnWithSdxConfigs() throws IOException {
        when(nameGenerator.generateName(APIResourceType.FILESYSTEM)).thenReturn("appletree");

        FileSystem fileSystem = underTest
                .prepareFilesystem(
                        mockRequestedCluster(mockRds(HIVE)),
                        mockSdxFileSystemView(), DATALAKE_CRN);

        List<StorageLocation> locations = fileSystem.getCloudStorage().getLocations();
        assertEquals(0, locations.size());
    }

    @Test
    public void testFileSystemWhenSdxHasDefaultFS() throws IOException {
        when(nameGenerator.generateName(APIResourceType.FILESYSTEM)).thenReturn("appletree");

        StorageLocation coreLocation = new StorageLocation();
        coreLocation.setType(DEFAULT_FS);
        String sdxDefaultFs = "sdxDefaultFs";
        coreLocation.setValue(sdxDefaultFs);
        FileSystem fileSystem = underTest
                .prepareFilesystem(
                        mockRequestedCluster(mockRds(HIVE)),
                        mockSdxFileSystemView(coreLocation), DATALAKE_CRN);

        List<StorageLocation> locations = fileSystem.getCloudStorage().getLocations();
        assertEquals(1, locations.size());
        assertEquals(sdxDefaultFs, locations.getFirst().getValue());
        assertEquals(REMOTE_FS, locations.getFirst().getType());
    }

    private RDSConfig get(Set<RDSConfig> rdsConfigs, DatabaseType databaseType) {
        return rdsConfigs
                .stream()
                .filter(r -> r.getType().equals(databaseType.name()))
                .collect(Collectors.toSet())
                .iterator()
                .next();
    }

    private boolean hasPropretyWithIndex(List<StorageLocation> locations, int index) {
        return !locations
                .stream()
                .filter(r -> r.getValue().equals("hive" + index))
                .collect(Collectors.toSet())
                .isEmpty();
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
        cluster.setName(CLUSTER_NAME);
        cluster.setRdsConfigs(new HashSet<>());
        for (RDSConfig r : rds) {
            cluster.getRdsConfigs().add(r);
        }
        return cluster;
    }

    private StorageLocation mockHiveStorageLocation(int index) {
        StorageLocation storageLocation = new StorageLocation();
        storageLocation.setType(Arrays.stream(CloudStorageCdpService.values()).filter(svc -> svc.name().contains("HIVE")).toList().get(index));
        storageLocation.setValue("hive" + index);
        return storageLocation;
    }

    private StorageLocation mockNonHiveStorageLocation(int index) {
        StorageLocation storageLocation = new StorageLocation();
        storageLocation.setType(Arrays.stream(CloudStorageCdpService.values()).filter(svc -> !svc.name().contains("HIVE")).toList().get(index));
        storageLocation.setValue("hive" + index);
        return storageLocation;
    }

    private Cluster mockRequestedCluster(StorageLocation... storageLocations) {
        Cluster cluster = new Cluster();
        cluster.setName(CLUSTER_NAME);
        FileSystem fileSystem = new FileSystem();
        fileSystem.setType(FileSystemType.S3);

        CloudStorage cloudStorage = new CloudStorage();
        cloudStorage.setLocations(Arrays.asList(storageLocations));
        fileSystem.setCloudStorage(cloudStorage);

        cluster.setFileSystem(fileSystem);
        cluster.setFileSystem(fileSystem);
        return cluster;
    }

    private SdxFileSystemView mockSdxFileSystemView(StorageLocation... storageLocations) {
        Map<String, String> locations = Arrays.stream(storageLocations)
                .collect(Collectors.toMap(location -> location.getType().name(), StorageLocation::getValue));
        return new SdxFileSystemView(FileSystemType.S3.name(), locations);
    }

}