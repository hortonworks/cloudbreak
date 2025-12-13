package com.sequenceiq.cloudbreak.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.FileNotFoundException;
import java.io.IOException;

import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.TestUtil;

class FileReaderUtilsTest {

    @Test
    void readFileInBase64WhenFileExist() throws IOException {
        String result = FileReaderUtils.readFileFromPathBase64(TestUtil.getFilePath(getClass(), "testfile.txt").toString());
        assertEquals("YXBwbGUgYXBwbGUgYXBwbGU=", result);
    }

    @Test
    void readFileInBase64WhenFileNotExist() throws IOException {
        assertThrows(IOException.class, () -> FileReaderUtils.readFileFromPath(TestUtil.getFilePath(getClass(), "testfile-not-exist.txt")),
                "File path must not be null");
    }

    @Test
    void readBinaryFileWhenFileExist() throws IOException {
        String result = FileReaderUtils.readBinaryFileFromPath(TestUtil.getFilePath(getClass(), "testfilebin.txt"));
        assertEquals("YXBwbGUgYXBwbGUgYXBwbGU=", result);
    }

    @Test
    void readBinaryFileWhenFileNotExist() throws IOException {
        assertThrows(IOException.class, () -> FileReaderUtils.readBinaryFileFromPath(TestUtil.getFilePath(getClass(), "testfilebin-not-exist.txt")),
                "File path must not be null");
    }

    @Test
    void readFileFromPathToStringWhenFileExist() throws IOException {
        String result = FileReaderUtils.readFileFromPath(TestUtil.getFilePath(getClass(), "testfile.txt"));
        assertEquals("apple apple apple", result);
    }

    @Test
    void readFileFromPathToStringWhenFileNotExist() throws IOException {
        assertThrows(IOException.class, () -> FileReaderUtils.readFileFromPath(TestUtil.getFilePath(getClass(), "testfile-not-exist.txt")),
                "File path must not be null");
    }

    @Test
    void readFileFromClasspathToStringWhenFileExist() throws IOException {
        String result = FileReaderUtils.readFileFromClasspath("com/sequenceiq/cloudbreak/util/testfile.txt");
        assertEquals("apple apple apple", result);
    }

    @Test
    void readFileFromClasspathToStringWhenFileNotExist() throws IOException {
        assertThrows(FileNotFoundException.class, () -> FileReaderUtils.readFileFromClasspath("com/sequenceiq/cloudbreak/util/testfile-not-exist.txt"),
                "class path resource [com/sequenceiq/cloudbreak/util/testfile-not-exist.txt] cannot be opened because it does not exist");
    }

}