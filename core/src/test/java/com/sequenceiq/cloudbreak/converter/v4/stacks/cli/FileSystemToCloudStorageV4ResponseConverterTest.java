package com.sequenceiq.cloudbreak.converter.v4.stacks.cli;

import static com.sequenceiq.cloudbreak.services.filesystem.FileSystemType.ADLS;
import static com.sequenceiq.cloudbreak.services.filesystem.FileSystemType.ADLS_GEN_2;
import static com.sequenceiq.cloudbreak.services.filesystem.FileSystemType.GCS;
import static com.sequenceiq.cloudbreak.services.filesystem.FileSystemType.S3;
import static com.sequenceiq.cloudbreak.services.filesystem.FileSystemType.WASB;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Collections;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.core.convert.ConversionService;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.storage.AdlsCloudStorageV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.storage.AdlsGen2CloudStorageV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.storage.GcsCloudStorageV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.storage.S3CloudStorageV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.storage.WasbCloudStorageV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.storage.CloudStorageV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.storage.location.StorageLocationV4Response;
import com.sequenceiq.cloudbreak.domain.FileSystem;
import com.sequenceiq.cloudbreak.domain.StorageLocation;
import com.sequenceiq.cloudbreak.domain.StorageLocations;
import com.sequenceiq.cloudbreak.domain.json.Json;
import com.sequenceiq.cloudbreak.services.filesystem.AdlsFileSystem;
import com.sequenceiq.cloudbreak.services.filesystem.AdlsGen2FileSystem;
import com.sequenceiq.cloudbreak.services.filesystem.BaseFileSystem;
import com.sequenceiq.cloudbreak.services.filesystem.FileSystemType;
import com.sequenceiq.cloudbreak.services.filesystem.GcsFileSystem;
import com.sequenceiq.cloudbreak.services.filesystem.S3FileSystem;
import com.sequenceiq.cloudbreak.services.filesystem.WasbFileSystem;

public class FileSystemToCloudStorageV4ResponseConverterTest {

    private static final Long FILE_SYSTEM_ID = 1L;

    private static final String FILE_SYSTEM_NAME = "fsName";

    private static final FileSystemType EXAMPLE_FILE_SYSTEM_TYPE = GCS;

    private static final boolean EXAMPLE_IS_DEFAULT_FS_VALUE = true;

    @InjectMocks
    private FileSystemToCloudStorageV4ResponseConverter underTest;

    @Mock
    private ConversionService conversionService;

    @Mock
    private FileSystem fileSystem;

    @Mock
    private Json configurations;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        when(fileSystem.getConfigurations()).thenReturn(configurations);
    }

    @Test
    public void testConvertWhenSourceContainsValidDataThenThisShouldBeConvertedIntoResponse() throws IOException {
        FileSystem fileSystem = createFileSystemSource();
        when(conversionService.convert(fileSystem.getConfigurations().get(BaseFileSystem.class), AdlsCloudStorageV4Parameters.class))
                .thenReturn(new AdlsCloudStorageV4Parameters());

        CloudStorageV4Response result = underTest.convert(fileSystem);

        assertEquals(FILE_SYSTEM_ID, result.getId());
//        assertEquals(FILE_SYSTEM_NAME, result.getName());
//        assertEquals(EXAMPLE_FILE_SYSTEM_TYPE.name(), result.getType());
        assertEquals(EXAMPLE_IS_DEFAULT_FS_VALUE, result.isDefaultFs());
    }

    @Test
    public void testConvertWhenLocationIsNullThenEmptySetShouldBeSet() {
        // wasb just for testing reason
        when(fileSystem.getType()).thenReturn(WASB);
        when(fileSystem.getLocations()).thenReturn(null);

        CloudStorageV4Response result = underTest.convert(fileSystem);

        assertNotNull(result.getLocations());
        assertTrue(result.getLocations().isEmpty());
    }

    @Test
    public void testConvertWhenLocationNotNullButItsValueNullThenEmptySetShouldBeSet() {
        // wasb just for testing reason
        when(fileSystem.getType()).thenReturn(WASB);
        Json locations = mock(Json.class);
        when(locations.getValue()).thenReturn(null);
        when(fileSystem.getLocations()).thenReturn(locations);

        CloudStorageV4Response result = underTest.convert(fileSystem);

        assertNotNull(result.getLocations());
        assertTrue(result.getLocations().isEmpty());
    }

    @Test
    public void testConvertWhenLocationHasValidValueButStorageLocationsIsNullThenLocationsShouldBeEmpty() throws IOException {
        // wasb just for testing reason
        when(fileSystem.getType()).thenReturn(WASB);
        Json locations = mock(Json.class);
        when(locations.getValue()).thenReturn("some value");
        when(locations.get(StorageLocations.class)).thenReturn(null);
        when(fileSystem.getLocations()).thenReturn(locations);

        CloudStorageV4Response result = underTest.convert(fileSystem);

        assertNotNull(result.getLocations());
        assertTrue(result.getLocations().isEmpty());
    }

    @Test
    public void testConvertWhelLocationsAreExistsThenTheseLocationsShouldBeStored() throws IOException {
        // wasb just for testing reason
        when(fileSystem.getType()).thenReturn(WASB);

        StorageLocations storageLocations = new StorageLocations();
        StorageLocation location = new StorageLocation();
        storageLocations.setLocations(Collections.singleton(location));
        Json locations = mock(Json.class);

        when(locations.getValue()).thenReturn("some value");
        when(locations.get(StorageLocations.class)).thenReturn(storageLocations);
        when(fileSystem.getLocations()).thenReturn(locations);
        when(conversionService.convert(location, StorageLocationV4Response.class)).thenReturn(new StorageLocationV4Response());

        CloudStorageV4Response result = underTest.convert(fileSystem);

        assertNotNull(result.getLocations());
        assertEquals(1L, result.getLocations().size());
        verify(conversionService, times(1)).convert(any(StorageLocation.class), eq(StorageLocationV4Response.class));
    }

    @Test
    public void testConvertWhenTypeIsAdlsThenExpectedAdlsFileSystemShouldBeSet() throws IOException {
        when(fileSystem.getType()).thenReturn(ADLS);
        AdlsFileSystem adls = mock(AdlsFileSystem.class);
        when(configurations.get(AdlsFileSystem.class)).thenReturn(adls);
        AdlsCloudStorageV4Parameters expected = mock(AdlsCloudStorageV4Parameters.class);
        when(conversionService.convert(adls, AdlsCloudStorageV4Parameters.class)).thenReturn(expected);

        CloudStorageV4Response result = underTest.convert(fileSystem);

        assertEquals(expected, result.getAdls());
        verify(conversionService, times(1)).convert(any(AdlsFileSystem.class), eq(AdlsCloudStorageV4Parameters.class));
        verify(conversionService, times(0)).convert(any(GcsFileSystem.class), eq(GcsCloudStorageV4Parameters.class));
        verify(conversionService, times(0)).convert(any(S3FileSystem.class), eq(S3CloudStorageV4Parameters.class));
        verify(conversionService, times(0)).convert(any(WasbFileSystem.class), eq(WasbCloudStorageV4Parameters.class));
        verify(conversionService, times(0)).convert(any(AdlsGen2FileSystem.class), eq(AdlsGen2CloudStorageV4Parameters.class));
    }

    @Test
    public void testConvertWhenTypeIsGcsThenExpectedGcsFileSystemShouldBeSet() throws IOException {
        when(fileSystem.getType()).thenReturn(GCS);
        GcsFileSystem gcs = mock(GcsFileSystem.class);
        when(configurations.get(GcsFileSystem.class)).thenReturn(gcs);
        GcsCloudStorageV4Parameters expected = mock(GcsCloudStorageV4Parameters.class);
        when(conversionService.convert(gcs, GcsCloudStorageV4Parameters.class)).thenReturn(expected);

        CloudStorageV4Response result = underTest.convert(fileSystem);

        assertEquals(expected, result.getGcs());
        verify(conversionService, times(1)).convert(any(GcsFileSystem.class), eq(GcsCloudStorageV4Parameters.class));
        verify(conversionService, times(0)).convert(any(AdlsFileSystem.class), eq(AdlsCloudStorageV4Parameters.class));
        verify(conversionService, times(0)).convert(any(S3FileSystem.class), eq(S3CloudStorageV4Parameters.class));
        verify(conversionService, times(0)).convert(any(WasbFileSystem.class), eq(WasbCloudStorageV4Parameters.class));
        verify(conversionService, times(0)).convert(any(AdlsGen2FileSystem.class), eq(AdlsGen2CloudStorageV4Parameters.class));
    }

    @Test
    public void testConvertWhenTypeIsS3ThenExpectedS3FileSystemShouldBeSet() throws IOException {
        when(fileSystem.getType()).thenReturn(S3);
        S3FileSystem s3 = mock(S3FileSystem.class);
        when(configurations.get(S3FileSystem.class)).thenReturn(s3);
        S3CloudStorageV4Parameters expected = mock(S3CloudStorageV4Parameters.class);
        when(conversionService.convert(s3, S3CloudStorageV4Parameters.class)).thenReturn(expected);

        CloudStorageV4Response result = underTest.convert(fileSystem);

        assertEquals(expected, result.getS3());
        verify(conversionService, times(1)).convert(any(S3FileSystem.class), eq(S3CloudStorageV4Parameters.class));
        verify(conversionService, times(0)).convert(any(GcsFileSystem.class), eq(GcsCloudStorageV4Parameters.class));
        verify(conversionService, times(0)).convert(any(AdlsFileSystem.class), eq(AdlsCloudStorageV4Parameters.class));
        verify(conversionService, times(0)).convert(any(WasbFileSystem.class), eq(WasbCloudStorageV4Parameters.class));
        verify(conversionService, times(0)).convert(any(AdlsGen2FileSystem.class), eq(AdlsGen2CloudStorageV4Parameters.class));
    }

    @Test
    public void testConvertWhenTypeIsWasbThenExpectedWasbFileSystemShouldBeSet() throws IOException {
        when(fileSystem.getType()).thenReturn(WASB);
        WasbFileSystem wasb = mock(WasbFileSystem.class);
        when(configurations.get(WasbFileSystem.class)).thenReturn(wasb);
        WasbCloudStorageV4Parameters expected = mock(WasbCloudStorageV4Parameters.class);
        when(conversionService.convert(wasb, WasbCloudStorageV4Parameters.class)).thenReturn(expected);

        CloudStorageV4Response result = underTest.convert(fileSystem);

        assertEquals(expected, result.getWasb());
        verify(conversionService, times(1)).convert(any(WasbFileSystem.class), eq(WasbCloudStorageV4Parameters.class));
        verify(conversionService, times(0)).convert(any(S3FileSystem.class), eq(S3CloudStorageV4Parameters.class));
        verify(conversionService, times(0)).convert(any(GcsFileSystem.class), eq(GcsCloudStorageV4Parameters.class));
        verify(conversionService, times(0)).convert(any(AdlsFileSystem.class), eq(AdlsCloudStorageV4Parameters.class));
        verify(conversionService, times(0)).convert(any(AdlsGen2FileSystem.class), eq(AdlsGen2CloudStorageV4Parameters.class));
    }

    @Test
    public void testConvertWhenTypeIsAdlsGen2ThenExpectedAdlsGen2FileSystemShouldBeSet() throws IOException {
        when(fileSystem.getType()).thenReturn(ADLS_GEN_2);
        AdlsGen2FileSystem adlsGen2 = mock(AdlsGen2FileSystem.class);
        when(configurations.get(AdlsGen2FileSystem.class)).thenReturn(adlsGen2);
        AdlsGen2CloudStorageV4Parameters expected = mock(AdlsGen2CloudStorageV4Parameters.class);
        when(conversionService.convert(adlsGen2, AdlsGen2CloudStorageV4Parameters.class)).thenReturn(expected);

        CloudStorageV4Response result = underTest.convert(fileSystem);

        assertEquals(expected, result.getAdlsGen2());
        verify(conversionService, times(1)).convert(any(AdlsGen2FileSystem.class), eq(AdlsGen2CloudStorageV4Parameters.class));
        verify(conversionService, times(0)).convert(any(WasbFileSystem.class), eq(WasbCloudStorageV4Parameters.class));
        verify(conversionService, times(0)).convert(any(S3FileSystem.class), eq(S3CloudStorageV4Parameters.class));
        verify(conversionService, times(0)).convert(any(GcsFileSystem.class), eq(GcsCloudStorageV4Parameters.class));
        verify(conversionService, times(0)).convert(any(AdlsFileSystem.class), eq(AdlsCloudStorageV4Parameters.class));
    }

    @Test
    public void testConvertWhenGettingFileSystemFromConfigurationThrowsExceptionThenIWillBeCatchedAndNoFileSystemWillBeSet() throws IOException {
        // adls just for testing reason
        when(fileSystem.getType()).thenReturn(ADLS);
        when(configurations.get(any(Class.class))).thenThrow(new IOException("some message"));

        CloudStorageV4Response result = underTest.convert(fileSystem);

        assertNull(result.getAdls());
        assertNull(result.getGcs());
        assertNull(result.getS3());
        assertNull(result.getWasb());
        assertNull(result.getAdlsGen2());
    }

    private FileSystem createFileSystemSource() throws JsonProcessingException {
        FileSystem fileSystem = new FileSystem();
        fileSystem.setId(FILE_SYSTEM_ID);
        fileSystem.setName(FILE_SYSTEM_NAME);
        fileSystem.setType(EXAMPLE_FILE_SYSTEM_TYPE);
        fileSystem.setDefaultFs(EXAMPLE_IS_DEFAULT_FS_VALUE);
        fileSystem.setConfigurations(new Json(new AdlsFileSystem()));
        return fileSystem;
    }

}