package com.sequenceiq.cloudbreak.service.filesystem;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.MockitoJUnitRunner;

import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.domain.FileSystem;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.repository.FileSystemRepository;

@RunWith(MockitoJUnitRunner.class)
public class FileSystemConfigServiceTest {

    private static final String NOT_FOUND_EXCEPTION_MESSAGE = "File system '%s' not found.";

    private static final Long NOT_EXISTING_ID = Long.MAX_VALUE;

    private static final Long TEST_FILES_SYSTEM_ID = 1L;

    private static final int TEST_QUANTITY = 3;

    @Rule
    public final ExpectedException expectedException = ExpectedException.none();

    @InjectMocks
    private FileSystemConfigService underTest;

    @Mock
    private FileSystemRepository fileSystemRepository;

    @Spy
    private TransactionService transactionService;

    @Test
    public void testGetWhenDatabaseHasEntryWithProvidedIdThenThatactualShouldReturn() {
        FileSystem expected = createFileSystem();
        when(fileSystemRepository.findById(TEST_FILES_SYSTEM_ID)).thenReturn(Optional.of(expected));

        FileSystem actual = underTest.getByIdFromAnyAvailableWorkspace(TEST_FILES_SYSTEM_ID);

        Assert.assertEquals(expected, actual);
        Assert.assertEquals(expected.getId(), actual.getId());
        verify(fileSystemRepository, times(1)).findById(TEST_FILES_SYSTEM_ID);
    }

    @Test
    public void testGetWhenThereIsNoEntryWithGivenIdThenFileSystemConfigExceptionShouldComeInsteadOfNull() {
        when(fileSystemRepository.findById(NOT_EXISTING_ID)).thenReturn(Optional.ofNullable(null));

        expectedException.expect(NotFoundException.class);
        expectedException.expectMessage(String.format(NOT_FOUND_EXCEPTION_MESSAGE, NOT_EXISTING_ID));

        underTest.getByIdFromAnyAvailableWorkspace(NOT_EXISTING_ID);
        verify(fileSystemRepository, times(1)).findById(NOT_EXISTING_ID);
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
        return fileSystem;
    }

}
