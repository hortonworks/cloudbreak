package com.sequenceiq.cloudbreak.converter.v2;

import com.sequenceiq.cloudbreak.api.model.filesystem.AdlsFileSystem;
import com.sequenceiq.cloudbreak.api.model.filesystem.FileSystemType;
import com.sequenceiq.cloudbreak.api.model.filesystem.GcsFileSystem;
import com.sequenceiq.cloudbreak.api.model.filesystem.WasbFileSystem;
import com.sequenceiq.cloudbreak.api.model.v2.CloudStorageRequest;
import com.sequenceiq.cloudbreak.api.model.v2.filesystem.AdlsCloudStorageParameters;
import com.sequenceiq.cloudbreak.api.model.v2.filesystem.GcsCloudStorageParameters;
import com.sequenceiq.cloudbreak.api.model.v2.filesystem.WasbCloudStorageParameters;
import com.sequenceiq.cloudbreak.common.model.user.IdentityUser;
import com.sequenceiq.cloudbreak.common.type.APIResourceType;
import com.sequenceiq.cloudbreak.converter.v2.filesystem.CloudStorageRequestToFileSystemConverter;
import com.sequenceiq.cloudbreak.domain.FileSystem;
import com.sequenceiq.cloudbreak.service.AuthenticatedUserService;
import com.sequenceiq.cloudbreak.service.MissingResourceNameGenerator;
import com.sequenceiq.cloudbreak.service.filesystem.FileSystemResolver;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.core.convert.ConversionService;

import javax.ws.rs.BadRequestException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class CloudStorageRequestToFileSystemConverterTest {

    private static final String USER_ACCOUNT = "fa431902-74fb-4f61-b643-35003f680f6a";

    private static final String USER_ID = "fa431902-74fb-4f61-b643-35003f680f6a";

    private static final String FILE_SYSTEM_NAME = "fsName";

    private static final String TEST_DESCRIPTION = "some description for the file system entry";

    private static final boolean EXAMPLE_IS_DEFAULT_FS_VALUE = true;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @InjectMocks
    private CloudStorageRequestToFileSystemConverter underTest;

    @Mock
    private AuthenticatedUserService authenticatedUserService;

    @Mock
    private MissingResourceNameGenerator nameGenerator;

    @Mock
    private FileSystemResolver fileSystemResolver;

    @Mock
    private ConversionService conversionService;

    @Mock
    private IdentityUser user;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        when(user.getUserId()).thenReturn(USER_ID);
        when(user.getAccount()).thenReturn(USER_ACCOUNT);
        when(authenticatedUserService.getCbUser()).thenReturn(user);
        when(nameGenerator.generateName(APIResourceType.FILESYSTEM)).thenReturn(FILE_SYSTEM_NAME);
        underTest = spy(underTest);
    }

    @Test
    public void testConvertWhenAdlsParametersNotNullThenItsValuesShouldBePlacedIntoTheResultInstance() {
        CloudStorageRequest request = createV2Request();
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
        verify(authenticatedUserService, times(1)).getCbUser();
        verify(fileSystemResolver, times(1)).propagateConfiguration(request);
    }

    @Test
    public void testConvertWhenGcsParametersNotNullThenItsValuesShouldBePlacedIntoTheResultInstance() {
        CloudStorageRequest request = createV2Request();
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
        verify(authenticatedUserService, times(1)).getCbUser();
        verify(fileSystemResolver, times(1)).propagateConfiguration(request);
    }

    @Test
    public void testConvertWhenWasbParametersNotNullThenItsValuesShouldBePlacedIntoTheResultInstance() {
        CloudStorageRequest request = createV2Request();
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
        verify(authenticatedUserService, times(1)).getCbUser();
        verify(fileSystemResolver, times(1)).propagateConfiguration(request);
    }

    @Test
    public void testConvertWhenNoFileSystemParameterInstanceHasPassedThroughTheRequestThenExceptionShouldComeIndicatingThatTheFileSystemTypeIsUndecidable() {
        CloudStorageRequest request = createV2Request();
        String message = "Unable to decide file system, none of the supported file system type has provided!";
        when(fileSystemResolver.propagateConfiguration(request)).thenThrow(new BadRequestException(message));

        expectedException.expect(BadRequestException.class);
        expectedException.expectMessage(message);

        underTest.convert(request);
        verify(authenticatedUserService, times(1)).getCbUser();
    }

    private void checkWhetherTheBasicDataHasPassedOrNot(FileSystem fileSystem) {
        assertEquals(USER_ACCOUNT, fileSystem.getAccount());
        assertEquals(USER_ID, fileSystem.getOwner());
        assertFalse(fileSystem.isDefaultFs());
        assertNotNull(fileSystem.getName());
    }

    private CloudStorageRequest createV2Request() {
        return new CloudStorageRequest();
    }

}