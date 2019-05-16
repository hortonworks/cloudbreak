package com.sequenceiq.cloudbreak.converter.spi;

import static com.sequenceiq.cloudbreak.common.type.filesystem.FileSystemType.WASB;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.core.convert.ConversionService;

import com.sequenceiq.cloudbreak.api.endpoint.v4.filesystems.requests.CloudStorageParameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.storage.AdlsCloudStorageV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.storage.GcsCloudStorageV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.storage.S3CloudStorageV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.storage.WasbCloudStorageV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.storage.CloudStorageV4Request;
import com.sequenceiq.cloudbreak.cloud.model.SpiFileSystem;
import com.sequenceiq.cloudbreak.cloud.model.filesystem.CloudAdlsView;
import com.sequenceiq.cloudbreak.cloud.model.filesystem.CloudGcsView;
import com.sequenceiq.cloudbreak.cloud.model.filesystem.CloudS3View;
import com.sequenceiq.cloudbreak.cloud.model.filesystem.CloudWasbView;
import com.sequenceiq.cloudbreak.common.type.filesystem.FileSystemType;

@RunWith(MockitoJUnitRunner.class)
public class FileSystemRequestToSpiFileSystemConverterTest {

    private static final String TEST_NAME = "testname";

    @Rule
    public final ExpectedException expectedException = ExpectedException.none();

    @InjectMocks
    private CloudStorageV4RequestToSpiFileSystemConverter underTest;

    @Mock
    private ConversionService conversionService;

    @Mock
    private CloudStorageV4Request request;

    @Test
    public void testConvertWhenAdlsNotNullThenCloudAdlsShouldBeInReturningObject() {
        AdlsCloudStorageV4Parameters adls = mock(AdlsCloudStorageV4Parameters.class);
        CloudAdlsView expected = mock(CloudAdlsView.class);
        when(request.getAdls()).thenReturn(adls);
        when(conversionService.convert(adls, CloudAdlsView.class)).thenReturn(expected);

        SpiFileSystem result = underTest.convert(request);

        assertEquals(expected, result.getCloudFileSystem());
        assertEquals(FileSystemType.ADLS, result.getType());
        verify(conversionService, times(1)).convert(adls, CloudAdlsView.class);
        verify(conversionService, times(0)).convert(any(CloudStorageParameters.class), eq(CloudGcsView.class));
        verify(conversionService, times(0)).convert(any(CloudStorageParameters.class), eq(CloudS3View.class));
        verify(conversionService, times(0)).convert(any(CloudStorageParameters.class), eq(CloudWasbView.class));
    }

    @Test
    public void testConvertWhenGcsNotNullThenCloudGcsShouldBeInReturningObject() {
        GcsCloudStorageV4Parameters gcs = mock(GcsCloudStorageV4Parameters.class);
        CloudGcsView expected = mock(CloudGcsView.class);
        when(request.getGcs()).thenReturn(gcs);
        when(conversionService.convert(gcs, CloudGcsView.class)).thenReturn(expected);

        SpiFileSystem result = underTest.convert(request);

        assertEquals(expected, result.getCloudFileSystem());
        assertEquals(FileSystemType.GCS, result.getType());
        verify(conversionService, times(1)).convert(gcs, CloudGcsView.class);
        verify(conversionService, times(0)).convert(any(CloudStorageParameters.class), eq(CloudAdlsView.class));
        verify(conversionService, times(0)).convert(any(CloudStorageParameters.class), eq(CloudS3View.class));
        verify(conversionService, times(0)).convert(any(CloudStorageParameters.class), eq(CloudWasbView.class));
    }

    @Test
    public void testConvertWhenS3NotNullThenCloudS3ShouldBeInReturningObject() {
        S3CloudStorageV4Parameters s3 = mock(S3CloudStorageV4Parameters.class);
        CloudS3View expected = mock(CloudS3View.class);
        when(request.getS3()).thenReturn(s3);
        when(conversionService.convert(s3, CloudS3View.class)).thenReturn(expected);

        SpiFileSystem result = underTest.convert(request);

        assertEquals(expected, result.getCloudFileSystem());
        assertEquals(FileSystemType.S3, result.getType());
        verify(conversionService, times(1)).convert(s3, CloudS3View.class);
        verify(conversionService, times(0)).convert(any(CloudStorageParameters.class), eq(CloudAdlsView.class));
        verify(conversionService, times(0)).convert(any(CloudStorageParameters.class), eq(CloudGcsView.class));
        verify(conversionService, times(0)).convert(any(CloudStorageParameters.class), eq(CloudWasbView.class));
    }

    @Test
    public void testConvertWhenWasbNotNullThenCloudWasbShouldBeInReturningObject() {
        WasbCloudStorageV4Parameters wasb = mock(WasbCloudStorageV4Parameters.class);
        CloudWasbView expected = mock(CloudWasbView.class);
        when(request.getWasb()).thenReturn(wasb);
        when(conversionService.convert(wasb, CloudWasbView.class)).thenReturn(expected);

        SpiFileSystem result = underTest.convert(request);

        assertEquals(expected, result.getCloudFileSystem());
        assertEquals(WASB, result.getType());
        verify(conversionService, times(1)).convert(wasb, CloudWasbView.class);
        verify(conversionService, times(0)).convert(any(CloudStorageParameters.class), eq(CloudAdlsView.class));
        verify(conversionService, times(0)).convert(any(CloudStorageParameters.class), eq(CloudGcsView.class));
        verify(conversionService, times(0)).convert(any(CloudStorageParameters.class), eq(CloudS3View.class));
    }

    @Test
    public void testConvertWhenAllCloudStorageParametersAreNullThenNullCloudFileSystemViewShouldPlacedInResultInstance() {
        when(request.getWasb()).thenReturn(null);
        when(request.getAdls()).thenReturn(null);
        when(request.getS3()).thenReturn(null);
        when(request.getGcs()).thenReturn(null);

        SpiFileSystem result = underTest.convert(request);

        assertNull(result.getCloudFileSystem());
        verify(conversionService, times(0)).convert(any(CloudStorageParameters.class), eq(CloudWasbView.class));
        verify(conversionService, times(0)).convert(any(CloudStorageParameters.class), eq(CloudAdlsView.class));
        verify(conversionService, times(0)).convert(any(CloudStorageParameters.class), eq(CloudGcsView.class));
        verify(conversionService, times(0)).convert(any(CloudStorageParameters.class), eq(CloudS3View.class));
    }
}