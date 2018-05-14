package com.sequenceiq.cloudbreak.converter.v2.cli;

import static org.junit.Assert.assertEquals;

import java.util.Collections;
import java.util.Map;

import org.junit.Test;

import com.sequenceiq.cloudbreak.api.model.FileSystemResponse;
import com.sequenceiq.cloudbreak.api.model.FileSystemType;
import com.sequenceiq.cloudbreak.domain.FileSystem;

public class FileSystemToFileSystemResponseConverterTest {

    private static final Long FILE_SYSTEM_ID = 1L;

    private static final String FILE_SYSTEM_NAME = "fsName";

    private static final FileSystemType EXAMPLE_FILE_SYSTEM_TYPE = FileSystemType.GCS;

    private static final boolean EXAMPLE_IS_DEFAULT_FS_VALUE = true;

    private static final Map<String, String> FILE_SYSTEM_PROPERTIES_AS_MAP = Collections.emptyMap();

    private final FileSystemToFileSystemResponseConverter underTest = new FileSystemToFileSystemResponseConverter();

    @Test
    public void testConvertWhenSourceContainsValidDataThenThisShouldBeConvertedIntoResponse() {
        FileSystem fileSystem = createFileSystemSource();
        FileSystemResponse result = underTest.convert(fileSystem);

        assertEquals(FILE_SYSTEM_ID, result.getId());
        assertEquals(FILE_SYSTEM_NAME, result.getName());
        assertEquals(EXAMPLE_FILE_SYSTEM_TYPE, result.getType());
        assertEquals(EXAMPLE_IS_DEFAULT_FS_VALUE, result.isDefaultFs());
        assertEquals(FILE_SYSTEM_PROPERTIES_AS_MAP, result.getProperties());
    }

    private FileSystem createFileSystemSource() {
        FileSystem fileSystem = new FileSystem();
        fileSystem.setId(FILE_SYSTEM_ID);
        fileSystem.setName(FILE_SYSTEM_NAME);
        fileSystem.setType(EXAMPLE_FILE_SYSTEM_TYPE.name());
        fileSystem.setDefaultFs(EXAMPLE_IS_DEFAULT_FS_VALUE);
        fileSystem.setProperties(FILE_SYSTEM_PROPERTIES_AS_MAP);
        return fileSystem;
    }

}