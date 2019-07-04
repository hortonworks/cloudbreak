package com.sequenceiq.cloudbreak.converter.v4.stacks.cluster.filesystem;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import javax.ws.rs.BadRequestException;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.core.convert.ConversionService;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.storage.CloudStorageV4Request;
import com.sequenceiq.cloudbreak.cloud.model.storage.AdlsCloudStorageParameters;
import com.sequenceiq.cloudbreak.cloud.model.storage.AdlsGen2CloudStorageParameters;
import com.sequenceiq.cloudbreak.cloud.model.storage.GcsCloudStorageParameters;
import com.sequenceiq.cloudbreak.cloud.model.storage.WasbCloudStorageParameters;
import com.sequenceiq.cloudbreak.common.converter.MissingResourceNameGenerator;
import com.sequenceiq.cloudbreak.common.type.APIResourceType;
import com.sequenceiq.cloudbreak.common.type.filesystem.AdlsFileSystem;
import com.sequenceiq.cloudbreak.common.type.filesystem.AdlsGen2FileSystem;
import com.sequenceiq.cloudbreak.common.type.filesystem.FileSystemType;
import com.sequenceiq.cloudbreak.common.type.filesystem.GcsFileSystem;
import com.sequenceiq.cloudbreak.common.type.filesystem.WasbFileSystem;
import com.sequenceiq.cloudbreak.common.user.CloudbreakUser;
import com.sequenceiq.cloudbreak.domain.FileSystem;
import com.sequenceiq.cloudbreak.service.filesystem.FileSystemResolver;

public class CloudStorageV4RequestToFileSystemConverterTest {

    private static final String USER_ACCOUNT = "fa431902-74fb-4f61-b643-35003f680f6a";

    private static final String USER_ID = "fa431902-74fb-4f61-b643-35003f680f6a";

    private static final String FILE_SYSTEM_NAME = "fsName";

    private static final String TEST_DESCRIPTION = "some description for the file system entry";

    private static final boolean EXAMPLE_IS_DEFAULT_FS_VALUE = true;

    @Rule
    public final ExpectedException expectedException = ExpectedException.none();

    @InjectMocks
    private CloudStorageV4RequestToFileSystemConverter underTest;

    @Mock
    private MissingResourceNameGenerator nameGenerator;

    @Mock
    private FileSystemResolver fileSystemResolver;

    @Mock
    private ConversionService conversionService;

    @Mock
    private CloudbreakUser user;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        when(user.getUserId()).thenReturn(USER_ID);
        when(nameGenerator.generateName(APIResourceType.FILESYSTEM)).thenReturn(FILE_SYSTEM_NAME);
        underTest = spy(underTest);
    }

    @Test
    public void testConvertWhenAdlsParametersNotNullThenItsValuesShouldBePlacedIntoTheResultInstance() {
        CloudStorageV4Request request = createV4Request();
        AdlsCloudStorageParameters adlsFileSystemParameters = new AdlsCloudStorageParameters();
        adlsFileSystemParameters.setAccountName("dummy account name");
        adlsFileSystemParameters.setClientId("1234");
        adlsFileSystemParameters.setCredential("123456");
        adlsFileSystemParameters.setTenantId("1111111");
        request.setAdls(adlsFileSystemParameters);
        AdlsCloudStorageParameters adlsCloudStorageParameters = new AdlsCloudStorageParameters();
        when(fileSystemResolver.propagateConfiguration(request)).thenReturn(adlsCloudStorageParameters);
        when(underTest.getConversionService()).thenReturn(conversionService);
        when(conversionService.convert(adlsFileSystemParameters, AdlsFileSystem.class)).thenReturn(new AdlsFileSystem());

        FileSystem result = underTest.convert(request);

        checkWhetherTheBasicDataHasPassedOrNot(result);
        assertEquals(FileSystemType.ADLS, result.getType());
        verify(fileSystemResolver, times(1)).propagateConfiguration(request);
    }

    @Test
    public void testConvertWhenGcsParametersNotNullThenItsValuesShouldBePlacedIntoTheResultInstance() {
        CloudStorageV4Request request = createV4Request();
        GcsCloudStorageParameters gcsFileSystemParameters = new GcsCloudStorageParameters();
        gcsFileSystemParameters.setServiceAccountEmail("some@email.com");
        request.setGcs(gcsFileSystemParameters);
        GcsCloudStorageParameters gcsCloudStorageParameters = new GcsCloudStorageParameters();
        when(fileSystemResolver.propagateConfiguration(request)).thenReturn(gcsCloudStorageParameters);
        when(underTest.getConversionService()).thenReturn(conversionService);
        when(conversionService.convert(gcsCloudStorageParameters, GcsFileSystem.class)).thenReturn(new GcsFileSystem());
        when(fileSystemResolver.propagateConfiguration(request)).thenReturn(new GcsCloudStorageParameters());

        FileSystem result = underTest.convert(request);

        checkWhetherTheBasicDataHasPassedOrNot(result);
        assertEquals(FileSystemType.GCS, result.getType());
        verify(fileSystemResolver, times(1)).propagateConfiguration(request);
    }

    @Test
    public void testConvertWhenWasbParametersNotNullThenItsValuesShouldBePlacedIntoTheResultInstance() {
        CloudStorageV4Request request = createV4Request();
        WasbCloudStorageParameters wasbFileSystemParameters = new WasbCloudStorageParameters();
        wasbFileSystemParameters.setAccountKey("123456789");
        wasbFileSystemParameters.setAccountName("accountNameValue");
        request.setWasb(wasbFileSystemParameters);
        WasbCloudStorageParameters wasbCloudStorageParameters = new WasbCloudStorageParameters();
        when(fileSystemResolver.propagateConfiguration(request)).thenReturn(wasbCloudStorageParameters);
        when(underTest.getConversionService()).thenReturn(conversionService);
        when(conversionService.convert(wasbCloudStorageParameters, WasbFileSystem.class)).thenReturn(new WasbFileSystem());
        when(fileSystemResolver.propagateConfiguration(request)).thenReturn(new WasbCloudStorageParameters());

        FileSystem result = underTest.convert(request);

        checkWhetherTheBasicDataHasPassedOrNot(result);
        assertEquals(FileSystemType.WASB, result.getType());
        verify(fileSystemResolver, times(1)).propagateConfiguration(request);
    }

    @Test
    public void testConvertWhenAdlsGen2ParametersNotNullThenItsValuesShouldBePlacedIntoTheResultInstance() {
        CloudStorageV4Request request = createV4Request();
        AdlsGen2CloudStorageParameters adlsGen2FileSystemParameters = new AdlsGen2CloudStorageParameters();
        adlsGen2FileSystemParameters.setAccountKey("123456789");
        adlsGen2FileSystemParameters.setAccountName("accountNameValue");
        request.setAdlsGen2(adlsGen2FileSystemParameters);
        AdlsGen2CloudStorageParameters adlsGen2CloudStorageParameters = new AdlsGen2CloudStorageParameters();
        when(fileSystemResolver.propagateConfiguration(request)).thenReturn(adlsGen2CloudStorageParameters);
        when(underTest.getConversionService()).thenReturn(conversionService);
        when(conversionService.convert(adlsGen2CloudStorageParameters, AdlsGen2FileSystem.class)).thenReturn(new AdlsGen2FileSystem());
        when(fileSystemResolver.propagateConfiguration(request)).thenReturn(new AdlsGen2CloudStorageParameters());

        FileSystem result = underTest.convert(request);

        checkWhetherTheBasicDataHasPassedOrNot(result);
        assertEquals(FileSystemType.ADLS_GEN_2, result.getType());
        verify(fileSystemResolver, times(1)).propagateConfiguration(request);
    }

    @Test
    public void testConvertWhenNoFileSystemParameterInstanceHasPassedThroughTheRequestThenExceptionShouldComeIndicatingThatTheFileSystemTypeIsUndecidable() {
        CloudStorageV4Request request = createV4Request();
        String message = "Unable to decide file system, none of the supported file system type has provided!";
        when(fileSystemResolver.propagateConfiguration(request)).thenThrow(new BadRequestException(message));

        expectedException.expect(BadRequestException.class);
        expectedException.expectMessage(message);

        underTest.convert(request);
    }

    private void checkWhetherTheBasicDataHasPassedOrNot(FileSystem fileSystem) {
        assertNotNull(fileSystem.getName());
    }

    private CloudStorageV4Request createV4Request() {
        return new CloudStorageV4Request();
    }

}