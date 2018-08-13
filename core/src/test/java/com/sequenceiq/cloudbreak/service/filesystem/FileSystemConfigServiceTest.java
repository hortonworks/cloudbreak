package com.sequenceiq.cloudbreak.service.filesystem;

import static com.sequenceiq.cloudbreak.common.model.user.IdentityUserRole.ADMIN;
import static com.sequenceiq.cloudbreak.common.model.user.IdentityUserRole.USER;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Optional;
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

import com.sequenceiq.cloudbreak.common.model.user.IdentityUser;
import com.sequenceiq.cloudbreak.controller.exception.NotFoundException;
import com.sequenceiq.cloudbreak.domain.FileSystem;
import com.sequenceiq.cloudbreak.domain.organization.Organization;
import com.sequenceiq.cloudbreak.repository.FileSystemRepository;
import com.sequenceiq.cloudbreak.service.AuthorizationService;

public class FileSystemConfigServiceTest {

    private static final String NOT_FOUND_EXCEPTION_MESSAGE = "Record '%s' not found.";

    private static final String NO_SUCH_FS_BY_ID_FORMAT_MESSAGE = "There is no such file system with the id of [%s]";

    private static final String TEST_ACCESS_DENIED_EXCEPTION_MESSAGE = "Access denied!";

    private static final String USER_ACCOUNT = "fa431902-74fb-4f61-b643-35003f680f6a";

    private static final String USER_ID = "fa431902-74fb-4f61-b643-35003f680f6a";

    private static final String TEST_FILE_SYSTEM_NAME = "fsName";

    private static final Long NOT_EXISTING_ID = Long.MAX_VALUE;

    private static final Long TEST_FILES_SYSTEM_ID = 1L;

    private static final int TEST_QUANTITY = 3;

    private static final String ORGANIZATION_NAME = "TOP SECRET - FBI";

    @Rule
    public final ExpectedException expectedException = ExpectedException.none();

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
        Organization organization = new Organization();
        organization.setName(ORGANIZATION_NAME);
        when(fileSystemRepository.save(expected)).thenAnswer((Answer<FileSystem>) this::setIdForCreatedFileSystemEntry);

        FileSystem actual = underTest.create(user, expected, organization);

        Assert.assertEquals(TEST_FILES_SYSTEM_ID, actual.getId());
        Assert.assertEquals(USER_ACCOUNT, actual.getAccount());
        Assert.assertEquals(USER_ID, actual.getOwner());
        Assert.assertEquals(ORGANIZATION_NAME, actual.getOrganization().getName());
        verify(fileSystemRepository, times(1)).save(expected);
    }

    @Test
    public void testGetPrivateFileSystemWhenUserHasRightToGetFileSystemThenItShouldReturn() {
        when(fileSystemRepository.findByNameAndOwner(TEST_FILE_SYSTEM_NAME, USER_ID)).thenReturn(fileSystem);

        FileSystem actual = underTest.getPrivateFileSystem(TEST_FILE_SYSTEM_NAME, user);

        Assert.assertEquals(fileSystem, actual);
        verify(fileSystemRepository, times(1)).findByNameAndOwner(TEST_FILE_SYSTEM_NAME, USER_ID);
    }

    @Test
    public void testGetWhenDatabaseHasEntryWithProvidedIdThenThatactualShouldReturn() {
        FileSystem expected = createFileSystem();
        fileSystem.setId(TEST_FILES_SYSTEM_ID);
        when(fileSystemRepository.findById(TEST_FILES_SYSTEM_ID)).thenReturn(Optional.of(expected));

        FileSystem actual = underTest.get(TEST_FILES_SYSTEM_ID);

        Assert.assertEquals(expected, actual);
        Assert.assertEquals(expected.getId(), actual.getId());
        verify(fileSystemRepository, times(1)).findById(TEST_FILES_SYSTEM_ID);
    }

    @Test
    public void testGetWhenThereIsNoEntryWithGivenIdThenFileSystemConfigExceptionShouldComeInsteadOfNull() {
        when(fileSystemRepository.findById(NOT_EXISTING_ID)).thenReturn(Optional.ofNullable(null));

        expectedException.expect(NotFoundException.class);
        expectedException.expectMessage(String.format(NOT_FOUND_EXCEPTION_MESSAGE, NOT_EXISTING_ID));

        underTest.get(NOT_EXISTING_ID);
        verify(fileSystemRepository, times(1)).findById(NOT_EXISTING_ID);
    }

    @Test
    public void testRetrieveAccountFileSystemsWhenUserHasAdminRoleThenSearchWouldBeExecutedByOnlyAccount() {
        Set<FileSystem> expectedFileSystems = createFileSystems();
        when(user.getRoles()).thenReturn(Collections.singleton(ADMIN));
        when(fileSystemRepository.findByAccount(USER_ACCOUNT)).thenReturn(expectedFileSystems);

        Set<FileSystem> actual = underTest.retrieveAccountFileSystems(user);

        Assert.assertEquals(expectedFileSystems, actual);
        verify(fileSystemRepository, times(1)).findByAccount(USER_ACCOUNT);
        verify(fileSystemRepository, times(0)).findByAccountAndOwner(USER_ACCOUNT, USER_ID);
    }

    @Test
    public void testRetrieveAccountFileSystemsWhenUserHasNotAdminRoleThenSearchWouldBeExecutedByAccountAndOwner() {
        Set<FileSystem> expectedFileSystems = createFileSystems();
        when(user.getRoles()).thenReturn(Collections.singleton(USER));
        when(fileSystemRepository.findByAccountAndOwner(USER_ACCOUNT, USER_ID)).thenReturn(expectedFileSystems);

        Set<FileSystem> actual = underTest.retrieveAccountFileSystems(user);

        Assert.assertEquals(expectedFileSystems, actual);
        verify(fileSystemRepository, times(0)).findByAccount(USER_ACCOUNT);
        verify(fileSystemRepository, times(1)).findByAccountAndOwner(USER_ACCOUNT, USER_ID);
    }

    @Test
    public void testDeleteByIdWhenUserHasRightToDeleteAndThereIsARecordWithIdThenDeleteOperationWouldBeCalled() {
        when(fileSystemRepository.findById(TEST_FILES_SYSTEM_ID)).thenReturn(Optional.of(fileSystem));
        doNothing().when(fileSystemRepository).deleteById(TEST_FILES_SYSTEM_ID);

        underTest.delete(TEST_FILES_SYSTEM_ID, user);

        verify(fileSystemRepository, times(1)).findById(TEST_FILES_SYSTEM_ID);
        verify(fileSystemRepository, times(1)).delete(any());
    }

    @Test
    public void testDeleteByIdWhenThereIsNoRecordToDeleteWithIdThenExceptionWouldComeAndNothingCatchesIt() {
        when(fileSystemRepository.findById(NOT_EXISTING_ID)).thenReturn(Optional.ofNullable(null));

        expectedException.expect(NotFoundException.class);
        expectedException.expectMessage(String.format(NOT_FOUND_EXCEPTION_MESSAGE, NOT_EXISTING_ID));

        underTest.delete(NOT_EXISTING_ID, user);

        verify(fileSystemRepository, times(1)).findById(NOT_EXISTING_ID);
        verify(fileSystemRepository, times(0)).delete(any());
    }

    @Test
    public void testDeleteByNameWhenUserHasRightToDeleteAndThereIsARecordWithNameThenDeleteOperationWouldBeCalled() {
        when(fileSystemRepository.findByNameAndAccountAndOwner(TEST_FILE_SYSTEM_NAME, USER_ACCOUNT, USER_ID)).thenReturn(fileSystem);
        doNothing().when(fileSystemRepository).deleteById(TEST_FILES_SYSTEM_ID);

        underTest.delete(TEST_FILE_SYSTEM_NAME, user);

        verify(fileSystemRepository, times(1)).findByNameAndAccountAndOwner(TEST_FILE_SYSTEM_NAME, USER_ACCOUNT, USER_ID);
        verify(fileSystemRepository, times(1)).delete(any());
    }

    @Test
    public void testDeleteByNameWhenThereIsNoRecordToDeleteWithNameThenExceptionWouldComeAndNothingCatchesIt() {
        when(fileSystemRepository.findByNameAndAccountAndOwner(TEST_FILE_SYSTEM_NAME, USER_ACCOUNT, USER_ID)).thenReturn(null);

        expectedException.expect(NotFoundException.class);
        expectedException.expectMessage(String.format(NOT_FOUND_EXCEPTION_MESSAGE, TEST_FILE_SYSTEM_NAME));

        underTest.delete(TEST_FILE_SYSTEM_NAME, user);

        verify(fileSystemRepository, times(1)).findByNameAndAccountAndOwner(TEST_FILE_SYSTEM_NAME, USER_ACCOUNT, USER_ID);
        verify(fileSystemRepository, times(0)).delete(any(FileSystem.class));
    }

    private FileSystem setIdForCreatedFileSystemEntry(InvocationOnMock invocation) {
        FileSystem fileSystem = invocation.getArgument(0);
        fileSystem.setId(TEST_FILES_SYSTEM_ID);
        return fileSystem;
    }

    private Set<FileSystem> createFileSystems() {
        Set<FileSystem> fileSystems = new LinkedHashSet<>(TEST_QUANTITY);
        for (int i = 0; i < TEST_QUANTITY; i++) {
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