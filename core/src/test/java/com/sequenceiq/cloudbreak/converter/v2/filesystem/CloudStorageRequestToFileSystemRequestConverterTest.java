package com.sequenceiq.cloudbreak.converter.v2.filesystem;

import static com.sequenceiq.cloudbreak.api.model.filesystem.FileSystemType.WASB;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.LinkedHashSet;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.sequenceiq.cloudbreak.api.model.FileSystemRequest;
import com.sequenceiq.cloudbreak.api.model.filesystem.FileSystemType;
import com.sequenceiq.cloudbreak.api.model.v2.CloudStorageRequest;
import com.sequenceiq.cloudbreak.api.model.v2.StorageLocationRequest;
import com.sequenceiq.cloudbreak.api.model.v2.filesystem.AbfsCloudStorageParameters;
import com.sequenceiq.cloudbreak.api.model.v2.filesystem.AdlsCloudStorageParameters;
import com.sequenceiq.cloudbreak.api.model.v2.filesystem.CloudStorageParameters;
import com.sequenceiq.cloudbreak.common.type.APIResourceType;
import com.sequenceiq.cloudbreak.service.MissingResourceNameGenerator;
import com.sequenceiq.cloudbreak.service.filesystem.FileSystemResolver;

public class CloudStorageRequestToFileSystemRequestConverterTest {

    private static final String TEST_FILE_SYSTEM_NAME = "fsName";

    private static final FileSystemType TEST_FILE_SYSTEM = WASB;

    @InjectMocks
    private CloudStorageRequestToFileSystemRequestConverter underTest;

    @Mock
    private MissingResourceNameGenerator nameGenerator;

    @Mock
    private FileSystemResolver fileSystemResolver;

    @Mock
    private CloudStorageRequest request;

    @Mock
    private CloudStorageParameters cloudStorageParameters;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        when(nameGenerator.generateName(APIResourceType.FILESYSTEM)).thenReturn(TEST_FILE_SYSTEM_NAME);
    }

    @Test
    public void testConvertCheckAllParamsHasPassedProperlyWhenOneOfTheFileSystemTypeIsNotNull() {
        AdlsCloudStorageParameters adls = new AdlsCloudStorageParameters();
        Set<StorageLocationRequest> storageLocations = new LinkedHashSet<>();
        when(request.getAdls()).thenReturn(adls);
        when(request.getWasb()).thenReturn(null);
        when(request.getGcs()).thenReturn(null);
        when(request.getS3()).thenReturn(null);
        when(request.getAbfs()).thenReturn(null);
        when(fileSystemResolver.propagateConfiguration(request)).thenReturn(cloudStorageParameters);
        when(cloudStorageParameters.getType()).thenReturn(TEST_FILE_SYSTEM);

        FileSystemRequest result = underTest.convert(request);

        assertEquals(TEST_FILE_SYSTEM_NAME, result.getName());
        assertFalse(result.isDefaultFs());
        assertEquals(adls, result.getAdls());
        assertNull(result.getGcs());
        assertNull(result.getS3());
        assertNull(result.getWasb());
        assertNull(result.getAbfs());
        assertEquals(storageLocations, result.getLocations());
        assertEquals(TEST_FILE_SYSTEM.name(), result.getType());
        verify(nameGenerator, times(1)).generateName(APIResourceType.FILESYSTEM);
        verify(fileSystemResolver, times(1)).propagateConfiguration(request);
    }

    @Test
    public void testConvertCheckAllParamsHasPassedProperlyWhenAbfsIsNotNull() {
        AbfsCloudStorageParameters abfs = new AbfsCloudStorageParameters();
        Set<StorageLocationRequest> storageLocations = new LinkedHashSet<>();
        when(request.getAbfs()).thenReturn(abfs);
        when(request.getAdls()).thenReturn(null);
        when(request.getWasb()).thenReturn(null);
        when(request.getGcs()).thenReturn(null);
        when(request.getS3()).thenReturn(null);
        when(fileSystemResolver.propagateConfiguration(request)).thenReturn(cloudStorageParameters);
        when(cloudStorageParameters.getType()).thenReturn(TEST_FILE_SYSTEM);

        FileSystemRequest result = underTest.convert(request);

        assertEquals(TEST_FILE_SYSTEM_NAME, result.getName());
        assertFalse(result.isDefaultFs());
        assertEquals(abfs, result.getAbfs());
        assertNull(result.getGcs());
        assertNull(result.getS3());
        assertNull(result.getWasb());
        assertNull(result.getAdls());
        assertEquals(storageLocations, result.getLocations());
        assertEquals(TEST_FILE_SYSTEM.name(), result.getType());
        verify(nameGenerator, times(1)).generateName(APIResourceType.FILESYSTEM);
        verify(fileSystemResolver, times(1)).propagateConfiguration(request);
    }

}