package com.sequenceiq.cloudbreak.service.filesystem;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import org.mockito.Spy;
import org.mockito.invocation.InvocationOnMock;

import com.sequenceiq.cloudbreak.common.model.user.IdentityUser;
import com.sequenceiq.cloudbreak.controller.exception.NotFoundException;
import com.sequenceiq.cloudbreak.domain.FileSystem;
import com.sequenceiq.cloudbreak.domain.organization.Organization;
import com.sequenceiq.cloudbreak.domain.organization.User;
import com.sequenceiq.cloudbreak.repository.FileSystemRepository;
import com.sequenceiq.cloudbreak.service.AuthenticatedUserService;
import com.sequenceiq.cloudbreak.service.AuthorizationService;
import com.sequenceiq.cloudbreak.service.TransactionService;
import com.sequenceiq.cloudbreak.service.organization.OrganizationService;
import com.sequenceiq.cloudbreak.service.user.UserService;

public class FileSystemConfigServiceTest {

    private static final String NOT_FOUND_EXCEPTION_MESSAGE = "File system '%s' not found.";

    private static final String NO_SUCH_FS_BY_ID_FORMAT_MESSAGE = "There is no such file system with the id of [%s]";

    private static final String TEST_ACCESS_DENIED_EXCEPTION_MESSAGE = "Access denied!";

    private static final String USER_ACCOUNT = "fa431902-74fb-4f61-b643-35003f680f6a";

    private static final String USER_ID = "fa431902-74fb-4f61-b643-35003f680f6a";

    private static final String TEST_FILE_SYSTEM_NAME = "fsName";

    private static final Long NOT_EXISTING_ID = Long.MAX_VALUE;

    private static final Long TEST_FILES_SYSTEM_ID = 1L;

    private static final int TEST_QUANTITY = 3;

    private static final String ORGANIZATION_NAME = "TOP SECRET";

    private static final long ORG_ID = 100L;

    @Rule
    public final ExpectedException expectedException = ExpectedException.none();

    @InjectMocks
    private DefaultFileSystemService underTest;

    @Mock
    private FileSystemRepository fileSystemRepository;

    @Mock
    private AuthorizationService authService;

    @Mock
    private IdentityUser identityUser;

    @Mock
    private FileSystem fileSystem;

    @Mock
    private OrganizationService organizationService;

    @Mock
    private AuthenticatedUserService authenticatedUserService;

    @Mock
    private UserService userService;

    @Mock
    private User user;

    @Spy
    private TransactionService transactionService;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        when(fileSystem.getId()).thenReturn(TEST_FILES_SYSTEM_ID);
        Organization organization = new Organization();
        organization.setName(ORGANIZATION_NAME);
        organization.setId(ORG_ID);
        when(organizationService.get(ORG_ID)).thenReturn(organization);
        when(authenticatedUserService.getCbUser()).thenReturn(identityUser);
        when(userService.getOrCreate(identityUser)).thenReturn(user);
    }

    @Test
    public void testGetWhenDatabaseHasEntryWithProvidedIdThenThatactualShouldReturn() {
        FileSystem expected = createFileSystem();
        fileSystem.setId(TEST_FILES_SYSTEM_ID);
        when(fileSystemRepository.findById(TEST_FILES_SYSTEM_ID)).thenReturn(Optional.of(expected));

        FileSystem actual = underTest.getByIdFromAnyAvailableOrganization(TEST_FILES_SYSTEM_ID);

        Assert.assertEquals(expected, actual);
        Assert.assertEquals(expected.getId(), actual.getId());
        verify(fileSystemRepository, times(1)).findById(TEST_FILES_SYSTEM_ID);
    }

    @Test
    public void testGetWhenThereIsNoEntryWithGivenIdThenFileSystemConfigExceptionShouldComeInsteadOfNull() {
        when(fileSystemRepository.findById(NOT_EXISTING_ID)).thenReturn(Optional.ofNullable(null));

        expectedException.expect(NotFoundException.class);
        expectedException.expectMessage(String.format(NOT_FOUND_EXCEPTION_MESSAGE, NOT_EXISTING_ID));

        underTest.getByIdFromAnyAvailableOrganization(NOT_EXISTING_ID);
        verify(fileSystemRepository, times(1)).findById(NOT_EXISTING_ID);
    }

    @Test
    public void testDeleteByIdWhenUserHasRightToDeleteAndThereIsARecordWithIdThenDeleteOperationWouldBeCalled() {
        when(fileSystemRepository.findById(TEST_FILES_SYSTEM_ID)).thenReturn(Optional.of(fileSystem));
        doNothing().when(fileSystemRepository).deleteById(TEST_FILES_SYSTEM_ID);

        underTest.deleteByIdFromAnyAvailableOrganization(TEST_FILES_SYSTEM_ID);

        verify(fileSystemRepository, times(1)).findById(TEST_FILES_SYSTEM_ID);
        verify(fileSystemRepository, times(1)).delete(any());
    }

    @Test
    public void testDeleteByIdWhenThereIsNoRecordToDeleteWithIdThenExceptionWouldComeAndNothingCatchesIt() {
        when(fileSystemRepository.findById(NOT_EXISTING_ID)).thenReturn(Optional.ofNullable(null));

        expectedException.expect(NotFoundException.class);
        expectedException.expectMessage(String.format(NOT_FOUND_EXCEPTION_MESSAGE, NOT_EXISTING_ID));

        underTest.deleteByIdFromAnyAvailableOrganization(NOT_EXISTING_ID);

        verify(fileSystemRepository, times(1)).findById(NOT_EXISTING_ID);
        verify(fileSystemRepository, times(0)).delete(any());
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