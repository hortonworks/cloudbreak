package com.sequenceiq.cloudbreak.converter.spi;

import com.sequenceiq.cloudbreak.api.model.filesystem.AdlsFileSystem;
import com.sequenceiq.cloudbreak.api.model.filesystem.GcsFileSystem;
import com.sequenceiq.cloudbreak.api.model.filesystem.S3FileSystem;
import com.sequenceiq.cloudbreak.api.model.filesystem.WasbFileSystem;
import com.sequenceiq.cloudbreak.cloud.model.SpiFileSystem;
import com.sequenceiq.cloudbreak.cloud.model.filesystem.CloudAdlsView;
import com.sequenceiq.cloudbreak.cloud.model.filesystem.CloudFileSystemView;
import com.sequenceiq.cloudbreak.cloud.model.filesystem.CloudGcsView;
import com.sequenceiq.cloudbreak.cloud.model.filesystem.CloudS3View;
import com.sequenceiq.cloudbreak.cloud.model.filesystem.CloudWasbView;
import com.sequenceiq.cloudbreak.domain.FileSystem;
import com.sequenceiq.cloudbreak.domain.json.Json;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.core.convert.ConversionService;

import java.io.IOException;

import static com.sequenceiq.cloudbreak.api.model.filesystem.FileSystemType.ADLS;
import static com.sequenceiq.cloudbreak.api.model.filesystem.FileSystemType.GCS;
import static com.sequenceiq.cloudbreak.api.model.filesystem.FileSystemType.S3;
import static com.sequenceiq.cloudbreak.api.model.filesystem.FileSystemType.WASB;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class FileSystemToSpiFileSystemConverterTest {

    private static final String TEST_NAME = "testName";

    @InjectMocks
    private FileSystemToSpiFileSystemConverter underTest;

    @Mock
    private ConversionService conversionService;

    @Mock
    private FileSystem fileSystem;

    @Mock
    private Json configuration;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        when(fileSystem.getName()).thenReturn(TEST_NAME);
        when(fileSystem.getConfigurations()).thenReturn(configuration);
    }

    @Test
    public void testConvertWhenTypeIsAdlsThenCloudFileSystemViewShouldAlsoAdlsType() throws IOException {
        when(fileSystem.getType()).thenReturn(ADLS);
        AdlsFileSystem adls = mock(AdlsFileSystem.class);
        CloudFileSystemView expected = mock(CloudAdlsView.class);
        when(configuration.get(AdlsFileSystem.class)).thenReturn(adls);
        when(conversionService.convert(adls, CloudAdlsView.class)).thenReturn((CloudAdlsView) expected);

        SpiFileSystem result = underTest.convert(fileSystem);

        assertEquals(ADLS, result.getType());
        assertEquals(TEST_NAME, result.getName());
        assertEquals(expected, result.getCloudFileSystem());
    }

    @Test
    public void testConvertWhenTypeIsGcsThenCloudFileSystemViewShouldAlsoGcsType() throws IOException {
        when(fileSystem.getType()).thenReturn(GCS);
        GcsFileSystem adls = mock(GcsFileSystem.class);
        CloudFileSystemView expected = mock(CloudGcsView.class);
        when(configuration.get(GcsFileSystem.class)).thenReturn(adls);
        when(conversionService.convert(adls, CloudGcsView.class)).thenReturn((CloudGcsView) expected);

        SpiFileSystem result = underTest.convert(fileSystem);

        assertEquals(GCS, result.getType());
        assertEquals(TEST_NAME, result.getName());
        assertEquals(expected, result.getCloudFileSystem());
    }

    @Test
    public void testConvertWhenTypeIsS3ThenCloudFileSystemViewShouldAlsoS3Type() throws IOException {
        when(fileSystem.getType()).thenReturn(S3);
        S3FileSystem adls = mock(S3FileSystem.class);
        CloudFileSystemView expected = mock(CloudS3View.class);
        when(configuration.get(S3FileSystem.class)).thenReturn(adls);
        when(conversionService.convert(adls, CloudS3View.class)).thenReturn((CloudS3View) expected);

        SpiFileSystem result = underTest.convert(fileSystem);

        assertEquals(S3, result.getType());
        assertEquals(TEST_NAME, result.getName());
        assertEquals(expected, result.getCloudFileSystem());
    }

    @Test
    public void testConvertWhenTypeIsWasbThenCloudFileSystemViewShouldAlsoWasbType() throws IOException {
        when(fileSystem.getType()).thenReturn(WASB);
        WasbFileSystem adls = mock(WasbFileSystem.class);
        CloudFileSystemView expected = mock(CloudWasbView.class);
        when(configuration.get(WasbFileSystem.class)).thenReturn(adls);
        when(conversionService.convert(adls, CloudWasbView.class)).thenReturn((CloudWasbView) expected);

        SpiFileSystem result = underTest.convert(fileSystem);

        assertEquals(WASB, result.getType());
        assertEquals(TEST_NAME, result.getName());
        assertEquals(expected, result.getCloudFileSystem());
    }

    @Test
    public void testConvertWhenGettingConfigurationThrowsIOExceptionThenNullCloudFileSystemViewShouldBePlaced() throws IOException {
        //wasb but just for test purpose, it could be any other type
        when(fileSystem.getType()).thenReturn(WASB);
        when(configuration.get(any(Class.class))).thenThrow(new IOException("any message"));

        SpiFileSystem result = underTest.convert(fileSystem);

        assertEquals(WASB, result.getType());
        assertNull(result.getCloudFileSystem());
        assertEquals(TEST_NAME, result.getName());
    }

}