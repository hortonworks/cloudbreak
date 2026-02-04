package com.sequenceiq.cloudbreak.template.filesystem;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.domain.FileSystem;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.cloudstorage.CloudStorage;
import com.sequenceiq.cloudbreak.domain.cloudstorage.StorageLocation;
import com.sequenceiq.cloudbreak.template.filesystem.s3.S3FileSystemConfigurationsView;
import com.sequenceiq.cloudbreak.view.StackView;
import com.sequenceiq.common.api.cloudstorage.query.ConfigQueryEntries;
import com.sequenceiq.common.api.type.ResourceType;
import com.sequenceiq.common.model.CloudStorageCdpService;

@ExtendWith(MockitoExtension.class)
class FileSystemConfigurationProviderTest {

    @Mock
    private AzureFileSystemConfigProvider azureFileSystemConfigProvider;

    @Mock
    private FileSystemConfigurationsViewProvider fileSystemConfigurationsViewProvider;

    @InjectMocks
    private FileSystemConfigurationProvider underTest;

    @Mock
    private FileSystem fileSystem;

    @Mock
    private StackView stack;

    @Mock
    private Function<ResourceType, Collection<Resource>> resourceFuction;

    @Mock
    private Json credentialAttributes;

    @Mock
    private ConfigQueryEntries configQueryEntries;

    @Mock
    private CloudStorage cloudStorage;

    @BeforeEach
    void setUp() throws Exception {
        S3FileSystemConfigurationsView fileSystemConfigurationsView = new S3FileSystemConfigurationsView(mock(), new ArrayList<>(), true);
        lenient().when(fileSystemConfigurationsViewProvider.propagateConfigurationsView(fileSystem, configQueryEntries))
                .thenReturn(fileSystemConfigurationsView);
    }

    @Test
    void fileSystemConfigurationNoCloudStorage() throws Exception {
        BaseFileSystemConfigurationsView result =
                underTest.fileSystemConfiguration(fileSystem, stack, resourceFuction, credentialAttributes, configQueryEntries);

        assertThat(result.getLocations()).isEmpty();
    }

    @Test
    void fileSystemConfigurationNoRemoteFs() throws Exception {
        lenient().when(fileSystem.getCloudStorage()).thenReturn(cloudStorage);
        lenient().when(cloudStorage.getLocations()).thenReturn(List.of());
        BaseFileSystemConfigurationsView result =
                underTest.fileSystemConfiguration(fileSystem, stack, resourceFuction, credentialAttributes, configQueryEntries);

        assertThat(result.getLocations()).isEmpty();
    }

    @Test
    void fileSystemConfigurationContainsRemoteFs() throws Exception {
        String remoteFs = "hdfs://remote-fs";
        lenient().when(fileSystem.getCloudStorage()).thenReturn(cloudStorage);
        StorageLocation storageLocation = new StorageLocation();
        storageLocation.setType(CloudStorageCdpService.REMOTE_FS);
        storageLocation.setValue(remoteFs);
        when(cloudStorage.getLocations()).thenReturn(List.of(storageLocation));

        BaseFileSystemConfigurationsView result =
                underTest.fileSystemConfiguration(fileSystem, stack, resourceFuction, credentialAttributes, configQueryEntries);

        assertThat(result.getLocations())
                .hasSize(1)
                .first()
                .returns(CloudStorageCdpService.REMOTE_FS.name(), StorageLocationView::getProperty)
                .returns(remoteFs, StorageLocationView::getValue);
    }

}
