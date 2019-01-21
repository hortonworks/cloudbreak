package com.sequenceiq.cloudbreak.converter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.Collections;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.core.convert.ConversionService;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.storage.CloudStorageV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.storage.azure.AdlsCloudStorageParametersV4;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.storage.azure.AdlsGen2CloudStorageParametersV4;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.storage.azure.WasbCloudStorageParametersV4;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.storage.gcs.GcsCloudStorageParametersV4;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.storage.s3.S3CloudStorageParametersV4;
import com.sequenceiq.cloudbreak.common.type.APIResourceType;
import com.sequenceiq.cloudbreak.converter.v4.stacks.cluster.filesystem.CloudStorageV4RequestToFileSystemConverter;
import com.sequenceiq.cloudbreak.domain.FileSystem;
import com.sequenceiq.cloudbreak.service.MissingResourceNameGenerator;
import com.sequenceiq.cloudbreak.services.filesystem.AdlsFileSystem;
import com.sequenceiq.cloudbreak.services.filesystem.AdlsGen2FileSystem;
import com.sequenceiq.cloudbreak.services.filesystem.FileSystemType;
import com.sequenceiq.cloudbreak.services.filesystem.GcsFileSystem;
import com.sequenceiq.cloudbreak.services.filesystem.S3FileSystem;
import com.sequenceiq.cloudbreak.services.filesystem.WasbFileSystem;

public class FileSystemRequestToFileSystemConverterTest {

    @InjectMocks
    private CloudStorageV4RequestToFileSystemConverter underTest;

    @Mock
    private MissingResourceNameGenerator nameGenerator;

    @Mock
    private ConversionService conversionService;

    @Mock
    private CloudStorageV4Request request;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testConvertNameHasSetThroughGenerator() {
        String testName = "nameValue";
        when(nameGenerator.generateName(APIResourceType.FILESYSTEM)).thenReturn(testName);

        FileSystem result = underTest.convert(request);

        assertEquals(testName, result.getName());
    }

    @Test
    public void testConvertWhenAdlsNotNullThenItWillBeSetAsBaseFileSystem() {
        AdlsCloudStorageParametersV4 adls = new AdlsCloudStorageParametersV4();
        when(request.getAdls()).thenReturn(adls);
        when(conversionService.convert(adls, AdlsFileSystem.class)).thenReturn(new AdlsFileSystem());

        FileSystem result = underTest.convert(request);

        assertNotNull(result.getConfigurations());
        assertEquals(FileSystemType.ADLS, result.getType());
        verify(conversionService, times(1)).convert(adls, AdlsFileSystem.class);
        verify(conversionService, times(0)).convert(any(GcsCloudStorageParametersV4.class), eq(GcsFileSystem.class));
        verify(conversionService, times(0)).convert(any(S3CloudStorageParametersV4.class), eq(S3FileSystem.class));
        verify(conversionService, times(0)).convert(any(WasbCloudStorageParametersV4.class), eq(WasbFileSystem.class));
        verify(conversionService, times(0)).convert(any(AdlsGen2CloudStorageParametersV4.class), eq(AdlsGen2FileSystem.class));
    }

    @Test
    public void testConvertWhenGcsNotNullThenItWillBeSetAsBaseFileSystem() {
        GcsCloudStorageParametersV4 gcs = new GcsCloudStorageParametersV4();
        when(request.getGcs()).thenReturn(gcs);
        when(conversionService.convert(gcs, GcsFileSystem.class)).thenReturn(new GcsFileSystem());

        FileSystem result = underTest.convert(request);

        assertNotNull(result.getConfigurations());
        assertEquals(FileSystemType.GCS, result.getType());
        verify(conversionService, times(1)).convert(gcs, GcsFileSystem.class);
        verify(conversionService, times(0)).convert(any(AdlsCloudStorageParametersV4.class), eq(AdlsFileSystem.class));
        verify(conversionService, times(0)).convert(any(S3CloudStorageParametersV4.class), eq(S3FileSystem.class));
        verify(conversionService, times(0)).convert(any(WasbCloudStorageParametersV4.class), eq(WasbFileSystem.class));
        verify(conversionService, times(0)).convert(any(AdlsGen2CloudStorageParametersV4.class), eq(AdlsGen2FileSystem.class));
    }

    @Test
    public void testConvertWhenS3NotNullThenItWillBeSetAsBaseFileSystem() {
        S3CloudStorageParametersV4 s3 = new S3CloudStorageParametersV4();
        when(request.getS3()).thenReturn(s3);
        when(conversionService.convert(s3, S3FileSystem.class)).thenReturn(new S3FileSystem());

        FileSystem result = underTest.convert(request);

        assertNotNull(result.getConfigurations());
        assertEquals(FileSystemType.S3, result.getType());
        verify(conversionService, times(1)).convert(s3, S3FileSystem.class);
        verify(conversionService, times(0)).convert(any(GcsCloudStorageParametersV4.class), eq(GcsFileSystem.class));
        verify(conversionService, times(0)).convert(any(AdlsCloudStorageParametersV4.class), eq(AdlsFileSystem.class));
        verify(conversionService, times(0)).convert(any(WasbCloudStorageParametersV4.class), eq(WasbFileSystem.class));
        verify(conversionService, times(0)).convert(any(AdlsGen2CloudStorageParametersV4.class), eq(AdlsGen2FileSystem.class));
    }

    @Test
    public void testConvertWhenWasbNotNullThenItWillBeSetAsBaseFileSystem() {
        WasbCloudStorageParametersV4 wasb = new WasbCloudStorageParametersV4();
        when(request.getWasb()).thenReturn(wasb);
        when(conversionService.convert(wasb, WasbFileSystem.class)).thenReturn(new WasbFileSystem());

        FileSystem result = underTest.convert(request);

        assertNotNull(result.getConfigurations());
        assertEquals(FileSystemType.WASB, result.getType());
        verify(conversionService, times(1)).convert(wasb, WasbFileSystem.class);
        verify(conversionService, times(0)).convert(any(GcsCloudStorageParametersV4.class), eq(GcsFileSystem.class));
        verify(conversionService, times(0)).convert(any(AdlsCloudStorageParametersV4.class), eq(AdlsFileSystem.class));
        verify(conversionService, times(0)).convert(any(S3CloudStorageParametersV4.class), eq(S3FileSystem.class));
        verify(conversionService, times(0)).convert(any(AdlsGen2CloudStorageParametersV4.class), eq(AdlsGen2FileSystem.class));
    }

    @Test
    public void testConvertWhenAsbfNotNullThenItWillBeSetAsBaseFileSystem() {
        AdlsGen2CloudStorageParametersV4 adlsGen2 = new AdlsGen2CloudStorageParametersV4();
        when(request.getAdlsGen2()).thenReturn(adlsGen2);
        when(conversionService.convert(adlsGen2, AdlsGen2FileSystem.class)).thenReturn(new AdlsGen2FileSystem());

        FileSystem result = underTest.convert(request);

        assertNotNull(result.getConfigurations());
        assertEquals(FileSystemType.ADLS_GEN_2, result.getType());
        verify(conversionService, times(1)).convert(adlsGen2, AdlsGen2FileSystem.class);
        verify(conversionService, times(0)).convert(any(GcsCloudStorageParametersV4.class), eq(GcsFileSystem.class));
        verify(conversionService, times(0)).convert(any(AdlsCloudStorageParametersV4.class), eq(AdlsFileSystem.class));
        verify(conversionService, times(0)).convert(any(S3CloudStorageParametersV4.class), eq(S3FileSystem.class));
        verify(conversionService, times(0)).convert(any(WasbCloudStorageParametersV4.class), eq(WasbFileSystem.class));
    }

    @Test
    public void testConvertWhenSourceHasNoStorageLocationThenLocationJsonShouldBeEmpty() {
        when(request.getLocations()).thenReturn(Collections.emptySet());

        FileSystem result = underTest.convert(request);

        assertTrue(result.getLocations().getMap().containsKey("locations"));
        assertTrue(((Collection<?>) result.getLocations().getMap().get("locations")).isEmpty());
    }

}