package com.sequenceiq.cloudbreak.service.filesystem;

import static com.sequenceiq.cloudbreak.common.model.user.IdentityUserRole.ADMIN;
import static com.sequenceiq.cloudbreak.common.model.user.IdentityUserRole.USER;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.security.access.AccessDeniedException;

import com.sequenceiq.cloudbreak.common.model.user.IdentityUser;
import com.sequenceiq.cloudbreak.controller.exception.NotFoundException;
import com.sequenceiq.cloudbreak.domain.FileSystem;
import com.sequenceiq.cloudbreak.repository.FileSystemRepository;
import com.sequenceiq.cloudbreak.service.AuthorizationService;

public class FileSystemConfigServiceTest {

    private static final String NOT_FOUND_EXCEPTION_MESSAGE = "No record found for %s:%s";

    private static final String NO_SUCH_FS_BY_ID_FORMAT_MESSAGE = "There is no such file system with the id of [%s]";

    private static final String TEST_ACCESS_DENIED_EXCEPTION_MESSAGE = "Access denied!";

    private static final String USER_ACCOUNT = "fa431902-74fb-4f61-b643-35003f680f6a";

    private static final String USER_ID = "fa431902-74fb-4f61-b643-35003f680f6a";

    private static final String TEST_FILE_SYSTEM_NAME = "fsName";

    private static final Long NOT_EXISTING_ID = Long.MAX_VALUE;

    private static final Long TEST_FILES_SYSTEM_ID = 1L;

    private static final int TEST_QUANTITY = 3;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @InjectMocks
    private FileSystemConfigService underTest;

    @Mock
    private FileSystemRepository fileSystemRepository;

    @Mock
    private AuthorizationService authService;

    @Mock
    private IdentityUser user;

    @Mock
    private FileSystem fileSystem;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        when(user.getAccount()).thenReturn(USER_ACCOUNT);
        when(user.getUserId()).thenReturn(USER_ID);
        when(fileSystem.getId()).thenReturn(TEST_FILES_SYSTEM_ID);
    }

    @Test
    public void testCreateWhenUserHasRightToCreateAndProvideValidFileSystemThenAnotherOneWithSameDataAndFilledIdShouldReturn() {
        FileSystem expected = createFileSystem();
        doNothing().when(authService).hasWritePermission(expected);
        when(fileSystemRepository.save(expected)).thenAnswer((Answer<FileSystem>) this::setIdForCreatedFileSystemEntry);

        FileSystem actual = underTest.create(user, expected);

        Assert.assertEquals(TEST_FILES_SYSTEM_ID, actual.getId());
        Assert.assertEquals(USER_ACCOUNT, actual.getAccount());
        Assert.assertEquals(USER_ID, actual.getOwner());
        verify(authService, times(1)).hasWritePermission(expected);
        verify(fileSystemRepository, times(1)).save(expected);
    }

    @Test
    public void testCreateWhenUserHasNoWriteAccessThenNothinWillCatchTheIncomingException() {
        doThrow(new AccessDeniedException(TEST_ACCESS_DENIED_EXCEPTION_MESSAGE)).when(authService).hasWritePermission(fileSystem);

        expectedException.expect(AccessDeniedException.class);
        expectedException.expectMessage(TEST_ACCESS_DENIED_EXCEPTION_MESSAGE);

        underTest.create(user, fileSystem);

        verify(authService, times(1)).hasWritePermission(fileSystem);
    }

    @Test
    public void testGetPrivateFileSystemWhenUserHasRightToGetFileSystemThenItShouldReturn() {
        when(fileSystemRepository.findByNameAndOwner(TEST_FILE_SYSTEM_NAME, USER_ID)).thenReturn(fileSystem);
        doNothing().when(authService).hasReadPermission(fileSystem);

        FileSystem actual = underTest.getPrivateFileSystem(TEST_FILE_SYSTEM_NAME, user);

        Assert.assertEquals(fileSystem, actual);
        verify(authService, times(1)).hasReadPermission(fileSystem);
        verify(fileSystemRepository, times(1)).findByNameAndOwner(TEST_FILE_SYSTEM_NAME, USER_ID);
    }

    @Test
    public void testGetPrivateFileSystemWhenUserHasNoReadAccessThenNothinWillCatchTheIncomingException() {
        when(fileSystemRepository.findByNameAndOwner(TEST_FILE_SYSTEM_NAME, USER_ID)).thenReturn(fileSystem);
        doThrow(new AccessDeniedException(TEST_ACCESS_DENIED_EXCEPTION_MESSAGE)).when(authService).hasReadPermission(fileSystem);

        expectedException.expect(AccessDeniedException.class);
        expectedException.expectMessage(TEST_ACCESS_DENIED_EXCEPTION_MESSAGE);

        underTest.getPrivateFileSystem(TEST_FILE_SYSTEM_NAME, user);

        verify(authService, times(1)).hasReadPermission(fileSystem);
        verify(fileSystemRepository, times(1)).findByNameAndOwner(TEST_FILE_SYSTEM_NAME, USER_ID);
    }

    @Test
    public void testGetWhenDatabaseHasEntryWithProvidedIdThenThatactualShouldReturn() {
        FileSystem expected = createFileSystem();
        fileSystem.setId(TEST_FILES_SYSTEM_ID);
        when(fileSystemRepository.findOne(TEST_FILES_SYSTEM_ID)).thenReturn(expected);

        FileSystem actual = underTest.get(TEST_FILES_SYSTEM_ID);

        Assert.assertEquals(expected, actual);
        Assert.assertEquals(expected.getId(), actual.getId());
        verify(fileSystemRepository, times(1)).findOne(TEST_FILES_SYSTEM_ID);
    }

    @Test
    public void testGetWhenThereIsNoEntryWithGivenIdThenFileSystemConfigExceptionShouldComeInsteadOfNull() {
        when(fileSystemRepository.findOne(NOT_EXISTING_ID)).thenReturn(null);

        expectedException.expect(NotFoundException.class);
        expectedException.expectMessage(String.format(NOT_FOUND_EXCEPTION_MESSAGE, "id", NOT_EXISTING_ID));

        underTest.get(NOT_EXISTING_ID);
        verify(fileSystemRepository, times(1)).findOne(NOT_EXISTING_ID);
    }

    @Test
    public void testRetrieveAccountFileSystemsWhenUserHasAdminRoleThenSearchWouldBeExecutedByOnlyAccount() {
        Set<FileSystem> expectedFileSystems = createFileSystems(TEST_QUANTITY);
        when(user.getRoles()).thenReturn(Collections.singleton(ADMIN));
        when(fileSystemRepository.findByAccount(USER_ACCOUNT)).thenReturn(expectedFileSystems);

        Set<FileSystem> actual = underTest.retrieveAccountFileSystems(user);

        Assert.assertEquals(expectedFileSystems, actual);
        verify(fileSystemRepository, times(1)).findByAccount(USER_ACCOUNT);
        verify(fileSystemRepository, times(0)).findByAccountAndOwner(USER_ACCOUNT, USER_ID);
    }

    @Test
    public void testRetrieveAccountFileSystemsWhenUserHasNotAdminRoleThenSearchWouldBeExecutedByAccountAndOwner() {
        Set<FileSystem> expectedFileSystems = createFileSystems(TEST_QUANTITY);
        when(user.getRoles()).thenReturn(Collections.singleton(USER));
        when(fileSystemRepository.findByAccountAndOwner(USER_ACCOUNT, USER_ID)).thenReturn(expectedFileSystems);

        Set<FileSystem> actual = underTest.retrieveAccountFileSystems(user);

        Assert.assertEquals(expectedFileSystems, actual);
        verify(fileSystemRepository, times(0)).findByAccount(USER_ACCOUNT);
        verify(fileSystemRepository, times(1)).findByAccountAndOwner(USER_ACCOUNT, USER_ID);
    }

    @Test
    public void testDeleteByIdWhenUserHasRightToDeleteAndThereIsARecordWithIdThenDeleteOperationWouldBeCalled() {
        when(fileSystemRepository.findOne(TEST_FILES_SYSTEM_ID)).thenReturn(fileSystem);
        doNothing().when(authService).hasWritePermission(fileSystem);
        doNothing().when(fileSystemRepository).delete(TEST_FILES_SYSTEM_ID);

        underTest.delete(TEST_FILES_SYSTEM_ID, user);

        verify(fileSystemRepository, times(1)).findOne(TEST_FILES_SYSTEM_ID);
        verify(authService, times(1)).hasWritePermission(fileSystem);
        verify(fileSystemRepository, times(1)).delete(TEST_FILES_SYSTEM_ID);
    }

    @Test
    public void testDeleteByIdWhenThereIsNoRecordToDeleteWithIdThenExceptionWouldComeAndNothingCatchesIt() {
        when(fileSystemRepository.findOne(NOT_EXISTING_ID)).thenReturn(null);

        expectedException.expect(NotFoundException.class);
        expectedException.expectMessage(String.format(NOT_FOUND_EXCEPTION_MESSAGE, "id", NOT_EXISTING_ID));

        underTest.delete(NOT_EXISTING_ID, user);

        verify(fileSystemRepository, times(1)).findOne(NOT_EXISTING_ID);
        verify(fileSystemRepository, times(0)).delete(NOT_EXISTING_ID);
    }

    @Test
    public void testDeleteByIdWhenUserHasNoWriteAccessToDeleteThenAccessDeniedShouldComeAndNothingCatchesIt() {
        when(fileSystemRepository.findOne(TEST_FILES_SYSTEM_ID)).thenReturn(fileSystem);
        doThrow(new AccessDeniedException(TEST_ACCESS_DENIED_EXCEPTION_MESSAGE)).when(authService).hasWritePermission(fileSystem);

        expectedException.expect(AccessDeniedException.class);
        expectedException.expectMessage(TEST_ACCESS_DENIED_EXCEPTION_MESSAGE);

        underTest.delete(TEST_FILES_SYSTEM_ID, user);

        verify(fileSystemRepository, times(1)).findOne(TEST_FILES_SYSTEM_ID);
        verify(authService, times(1)).hasWritePermission(fileSystem);
        verify(fileSystemRepository, times(0)).delete(TEST_FILES_SYSTEM_ID);
    }

    @Test
    public void testDeleteByNameWhenUserHasRightToDeleteAndThereIsARecordWithNameThenDeleteOperationWouldBeCalled() {
        when(fileSystemRepository.findByNameAndAccountAndOwner(TEST_FILE_SYSTEM_NAME, USER_ACCOUNT, USER_ID)).thenReturn(fileSystem);
        doNothing().when(authService).hasWritePermission(fileSystem);
        doNothing().when(fileSystemRepository).delete(TEST_FILES_SYSTEM_ID);

        underTest.delete(TEST_FILE_SYSTEM_NAME, user);

        verify(fileSystemRepository, times(1)).findByNameAndAccountAndOwner(TEST_FILE_SYSTEM_NAME, USER_ACCOUNT, USER_ID);
        verify(authService, times(1)).hasWritePermission(fileSystem);
        verify(fileSystemRepository, times(1)).delete(TEST_FILES_SYSTEM_ID);
    }

    @Test
    public void testDeleteByNameWhenThereIsNoRecordToDeleteWithNameThenExceptionWouldComeAndNothingCatchesIt() {
        when(fileSystemRepository.findByNameAndAccountAndOwner(TEST_FILE_SYSTEM_NAME, USER_ACCOUNT, USER_ID)).thenReturn(null);

        expectedException.expect(NotFoundException.class);
        expectedException.expectMessage(String.format(NOT_FOUND_EXCEPTION_MESSAGE, "name", TEST_FILE_SYSTEM_NAME));

        underTest.delete(TEST_FILE_SYSTEM_NAME, user);

        verify(fileSystemRepository, times(1)).findByNameAndAccountAndOwner(TEST_FILE_SYSTEM_NAME, USER_ACCOUNT, USER_ID);
        verify(fileSystemRepository, times(0)).delete(any(Long.class));
    }

    @Test
    public void testDeleteByNameWhenUserHasNoWriteAccessToDeleteThenAccessDeniedShouldComeAndNothingCatchesIt() {
        when(fileSystemRepository.findByNameAndAccountAndOwner(TEST_FILE_SYSTEM_NAME, USER_ACCOUNT, USER_ID)).thenReturn(fileSystem);
        doThrow(new AccessDeniedException(TEST_ACCESS_DENIED_EXCEPTION_MESSAGE)).when(authService).hasWritePermission(fileSystem);

        expectedException.expect(AccessDeniedException.class);
        expectedException.expectMessage(TEST_ACCESS_DENIED_EXCEPTION_MESSAGE);

        underTest.delete(TEST_FILE_SYSTEM_NAME, user);

        verify(fileSystemRepository, times(1)).findOne(TEST_FILES_SYSTEM_ID);
        verify(authService, times(1)).hasWritePermission(fileSystem);
        verify(fileSystemRepository, times(0)).delete(any(Long.class));
    }

    private FileSystem setIdForCreatedFileSystemEntry(InvocationOnMock invocation) {
        FileSystem fileSystem = invocation.getArgument(0);
        fileSystem.setId(TEST_FILES_SYSTEM_ID);
        return fileSystem;
    }

    private Set<FileSystem> createFileSystems(int quantity) {
        Set<FileSystem> fileSystems = new LinkedHashSet<>(quantity);
        for (int i = 0; i < quantity; i++) {
            fileSystems.add(createFileSystem(String.valueOf(i)));
        }
        return fileSystems;
    }

    private FileSystem createFileSystem() {
        return createFileSystem("1");
    }

    private FileSystem createFileSystem(String namePostFix) {
        FileSystem fileSystem = new FileSystem();
        fileSystem.setDescription("some description");
        fileSystem.setDefaultFs(true);
        fileSystem.setPublicInAccount(true);
        return fileSystem;
    }

}