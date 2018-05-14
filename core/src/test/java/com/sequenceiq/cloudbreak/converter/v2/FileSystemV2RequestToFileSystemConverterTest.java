package com.sequenceiq.cloudbreak.converter.v2;

import static org.junit.Assert.assertEquals;
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

import com.sequenceiq.cloudbreak.api.model.FileSystemType;
import com.sequenceiq.cloudbreak.api.model.filesystem.AdlsFileSystemParameters;
import com.sequenceiq.cloudbreak.api.model.filesystem.GcsFileSystemParameters;
import com.sequenceiq.cloudbreak.api.model.filesystem.WasbFileSystemParameters;
import com.sequenceiq.cloudbreak.api.model.v2.FileSystemV2Request;
import com.sequenceiq.cloudbreak.common.model.user.IdentityUser;
import com.sequenceiq.cloudbreak.common.type.APIResourceType;
import com.sequenceiq.cloudbreak.domain.FileSystem;
import com.sequenceiq.cloudbreak.service.AuthenticatedUserService;
import com.sequenceiq.cloudbreak.service.MissingResourceNameGenerator;

public class FileSystemV2RequestToFileSystemConverterTest {

    private static final String USER_ACCOUNT = "fa431902-74fb-4f61-b643-35003f680f6a";

    private static final String USER_ID = "fa431902-74fb-4f61-b643-35003f680f6a";

    private static final String FILE_SYSTEM_NAME = "fsName";

    private static final String TEST_DESCRIPTION = "some description for the file system entry";

    private static final boolean EXAMPLE_IS_DEFAULT_FS_VALUE = true;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @InjectMocks
    private FileSystemV2RequestToFileSystemConverter underTest;

    @Mock
    private AuthenticatedUserService authenticatedUserService;

    @Mock
    private MissingResourceNameGenerator nameGenerator;

    @Mock
    private IdentityUser user;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        when(user.getUserId()).thenReturn(USER_ID);
        when(user.getAccount()).thenReturn(USER_ACCOUNT);
        when(authenticatedUserService.getCbUser()).thenReturn(user);
        when(nameGenerator.generateName(APIResourceType.FILESYSTEM)).thenReturn(FILE_SYSTEM_NAME);
    }

    @Test
    public void testConvertWhenAdlsParametersNotNullThenItsValuesShouldBePlacedIntoTheResultInstance() {
        FileSystemV2Request request = createV2Request();
        AdlsFileSystemParameters adlsFileSystemParameters = new AdlsFileSystemParameters();
        adlsFileSystemParameters.setAccountName("dummy account name");
        adlsFileSystemParameters.setClientId("1234");
        adlsFileSystemParameters.setCredential("123456");
        adlsFileSystemParameters.setTenantId("1111111");
        request.setAdls(adlsFileSystemParameters);

        FileSystem result = underTest.convert(request);

        checkWhetherTheBasicDataHasPassedOrNot(request, result);
        assertEquals(FileSystemType.ADLS.name(), result.getType());
        assertEquals(adlsFileSystemParameters.getAsMap(), result.getProperties());
        verify(authenticatedUserService, times(1)).getCbUser();
    }

    @Test
    public void testConvertWhenGcsParametersNotNullThenItsValuesShouldBePlacedIntoTheResultInstance() {
        FileSystemV2Request request = createV2Request();
        GcsFileSystemParameters gcsFileSystemParameters = new GcsFileSystemParameters();
        gcsFileSystemParameters.setDefaultBucketName("bucket name");
        gcsFileSystemParameters.setProjectId("123");
        gcsFileSystemParameters.setServiceAccountEmail("some@email.com");
        request.setGcs(gcsFileSystemParameters);

        FileSystem result = underTest.convert(request);

        checkWhetherTheBasicDataHasPassedOrNot(request, result);
        assertEquals(FileSystemType.GCS.name(), result.getType());
        assertEquals(gcsFileSystemParameters.getAsMap(), result.getProperties());
        verify(authenticatedUserService, times(1)).getCbUser();
    }

    @Test
    public void testConvertWhenWasbParametersNotNullThenItsValuesShouldBePlacedIntoTheResultInstance() {
        FileSystemV2Request request = createV2Request();
        WasbFileSystemParameters wasbFileSystemParameters = new WasbFileSystemParameters();
        wasbFileSystemParameters.setAccountKey("123456789");
        wasbFileSystemParameters.setAccountName("accountNameValue");
        request.setWasb(wasbFileSystemParameters);

        FileSystem result = underTest.convert(request);

        checkWhetherTheBasicDataHasPassedOrNot(request, result);
        assertEquals(FileSystemType.WASB.name(), result.getType());
        assertEquals(wasbFileSystemParameters.getAsMap(), result.getProperties());
        verify(authenticatedUserService, times(1)).getCbUser();
    }

    @Test
    public void testConvertWhenNoFileSystemParameterInstanceHasPassedThroughTheRequestThenExceptionShouldComeIndicatingThatTheFileSystemTypeIsUndecidable() {
        FileSystemV2Request request = createV2Request();

        expectedException.expect(BadRequestException.class);
        expectedException.expectMessage("Unable to decide file system, none of the supported file system type has provided!");

        underTest.convert(request);
        verify(authenticatedUserService, times(1)).getCbUser();
    }

    private void checkWhetherTheBasicDataHasPassedOrNot(FileSystemV2Request request, FileSystem fileSystem) {
        assertEquals(USER_ID, fileSystem.getOwner());
        assertEquals(USER_ACCOUNT, fileSystem.getAccount());
        assertEquals(request.getDescription(), fileSystem.getDescription());
    }

    private FileSystemV2Request createV2Request() {
        FileSystemV2Request request = new FileSystemV2Request();
        request.setDescription(TEST_DESCRIPTION);
        return request;
    }

}