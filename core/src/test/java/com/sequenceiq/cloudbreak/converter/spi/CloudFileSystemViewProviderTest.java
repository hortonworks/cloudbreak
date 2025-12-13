package com.sequenceiq.cloudbreak.converter.spi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.model.filesystem.CloudFileSystemView;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.knox.KnoxRoles;
import com.sequenceiq.cloudbreak.domain.FileSystem;
import com.sequenceiq.cloudbreak.domain.cloudstorage.CloudIdentity;
import com.sequenceiq.cloudbreak.domain.cloudstorage.CloudStorage;
import com.sequenceiq.cloudbreak.domain.cloudstorage.S3Identity;
import com.sequenceiq.cloudbreak.domain.cloudstorage.StorageLocation;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.service.stack.InstanceGroupService;
import com.sequenceiq.common.model.CloudIdentityType;
import com.sequenceiq.common.model.CloudStorageCdpService;
import com.sequenceiq.common.model.FileSystemType;

@ExtendWith(MockitoExtension.class)
class CloudFileSystemViewProviderTest {

    private static final String ID_BROKER_INSTANCE_PROFILE = "idBrokerInstanceProfile";

    private static final String LOG_INSTANCE_PROFILE = "logInstanceProfile";

    private static final CloudStorageCdpService SERVICE_1 = CloudStorageCdpService.ZEPPELIN_NOTEBOOK;

    private static final String PATH_1 = "path1";

    private static final CloudStorageCdpService SERVICE_2 = CloudStorageCdpService.HIVE_METASTORE_EXTERNAL_WAREHOUSE;

    private static final String PATH_2 = "path2";

    private static final CloudStorageCdpService SERVICE_3 = CloudStorageCdpService.HIVE_METASTORE_WAREHOUSE;

    private static final String PATH_3 = "path3";

    private static final CloudStorageCdpService SERVICE_4 = CloudStorageCdpService.RANGER_AUDIT;

    private static final String PATH_4 = "path4";

    private static final String ID_BROKER_INSTANCE_GROUP_NAME = "idBrokerGroup";

    private static final String COMPUTE_INSTANCE_GROUP_NAME = "computeGroup";

    @Spy
    private FileSystemConverter fileSystemConverter;

    @Spy
    private CloudIdentityTypeDecider cloudIdentityTypeDecider;

    @Mock
    private InstanceGroupService instanceGroupService;

    @InjectMocks
    private CloudFileSystemViewProvider cloudFileSystemViewProvider;

    @Test
    void testBuild() {
        List<CloudIdentity> cloudIdentities = getCloudIdentities();
        CloudStorage cloudStorage = getCloudStorage(cloudIdentities);
        FileSystem fileSystem = getFileSystem(cloudStorage);
        Map<String, Set<String>> componentsByHostGroup = new HashMap<>();

        InstanceGroup idBrokerGroup = getIdBrokerGroup(componentsByHostGroup);

        InstanceGroup computeGroup = new InstanceGroup();
        computeGroup.setGroupName(COMPUTE_INSTANCE_GROUP_NAME);
        componentsByHostGroup.put(COMPUTE_INSTANCE_GROUP_NAME, new HashSet<>());

        Optional<CloudFileSystemView> idBrokerGroupResult = cloudFileSystemViewProvider
                .getCloudFileSystemView(fileSystem, componentsByHostGroup, idBrokerGroup);
        assertEquals(idBrokerGroupResult.get().getCloudIdentityType(), CloudIdentityType.ID_BROKER);

        Optional<CloudFileSystemView> computeGroupResult = cloudFileSystemViewProvider
                .getCloudFileSystemView(fileSystem, componentsByHostGroup, computeGroup);
        assertEquals(computeGroupResult.get().getCloudIdentityType(), CloudIdentityType.LOG);

        verify(instanceGroupService, times(2)).setCloudIdentityType(any(), any());
    }

    @Test
    void testBuildWithNullComponents() {
        List<CloudIdentity> cloudIdentities = getCloudIdentities();
        CloudStorage cloudStorage = getCloudStorage(cloudIdentities);
        FileSystem fileSystem = getFileSystem(cloudStorage);
        Map<String, Set<String>> componentsByHostGroup = new HashMap<>();

        InstanceGroup idBrokerGroup = getIdBrokerGroup(componentsByHostGroup);

        InstanceGroup computeGroup = new InstanceGroup();
        computeGroup.setGroupName(COMPUTE_INSTANCE_GROUP_NAME);
        componentsByHostGroup.put(COMPUTE_INSTANCE_GROUP_NAME, null);

        Optional<CloudFileSystemView> idBrokerGroupResult = cloudFileSystemViewProvider
                .getCloudFileSystemView(fileSystem, componentsByHostGroup, idBrokerGroup);
        assertEquals(idBrokerGroupResult.get().getCloudIdentityType(), CloudIdentityType.ID_BROKER);

        Optional<CloudFileSystemView> computeGroupResult = cloudFileSystemViewProvider
                .getCloudFileSystemView(fileSystem, componentsByHostGroup, computeGroup);
        assertEquals(computeGroupResult.get().getCloudIdentityType(), CloudIdentityType.LOG);
    }

    private List<CloudIdentity> getCloudIdentities() {
        List<CloudIdentity> cloudIdentities = new ArrayList<>();
        CloudIdentity idBroker = new CloudIdentity();
        idBroker.setIdentityType(CloudIdentityType.ID_BROKER);
        S3Identity idBrokerS3Identity = new S3Identity();
        idBrokerS3Identity.setInstanceProfile(ID_BROKER_INSTANCE_PROFILE);
        idBroker.setS3Identity(idBrokerS3Identity);
        cloudIdentities.add(idBroker);

        CloudIdentity log = new CloudIdentity();
        log.setIdentityType(CloudIdentityType.LOG);
        S3Identity logS3Identity = new S3Identity();
        logS3Identity.setInstanceProfile(LOG_INSTANCE_PROFILE);
        log.setS3Identity(logS3Identity);
        cloudIdentities.add(log);
        return cloudIdentities;
    }

    private CloudStorage getCloudStorage(List<CloudIdentity> cloudIdentities) {
        List<StorageLocation> storageLocations = new ArrayList<>();

        StorageLocation storageLocation1 = new StorageLocation();
        storageLocation1.setType(SERVICE_1);
        storageLocation1.setValue(PATH_1);
        storageLocations.add(storageLocation1);

        StorageLocation storageLocation2 = new StorageLocation();
        storageLocation2.setType(SERVICE_2);
        storageLocation2.setValue(PATH_2);
        storageLocations.add(storageLocation2);

        StorageLocation storageLocation3 = new StorageLocation();
        storageLocation3.setType(SERVICE_3);
        storageLocation3.setValue(PATH_3);
        storageLocations.add(storageLocation3);

        StorageLocation storageLocation4 = new StorageLocation();
        storageLocation4.setType(SERVICE_4);
        storageLocation4.setValue(PATH_4);
        storageLocations.add(storageLocation4);

        CloudStorage cloudStorage = new CloudStorage();
        cloudStorage.setCloudIdentities(cloudIdentities);
        cloudStorage.setLocations(storageLocations);
        return cloudStorage;
    }

    private FileSystem getFileSystem(CloudStorage cloudStorage) {
        FileSystem fileSystem = new FileSystem();
        fileSystem.setType(FileSystemType.S3);
        fileSystem.setCloudStorage(cloudStorage);
        return fileSystem;
    }

    private InstanceGroup getIdBrokerGroup(Map<String, Set<String>> componentsByHostGroup) {
        InstanceGroup idBrokerGroup = new InstanceGroup();
        idBrokerGroup.setGroupName(ID_BROKER_INSTANCE_GROUP_NAME);
        Set<String> idBrokerComponents = new HashSet<>();
        idBrokerComponents.add(KnoxRoles.IDBROKER);
        componentsByHostGroup.put(ID_BROKER_INSTANCE_GROUP_NAME, idBrokerComponents);
        return idBrokerGroup;
    }
}