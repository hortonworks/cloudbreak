package com.sequenceiq.cloudbreak.util;

import static org.junit.Assert.assertEquals;

import java.io.FileNotFoundException;
import java.io.IOException;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.sequenceiq.cloudbreak.TestUtil;


public class FileReaderUtilsTest {

    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    @Test
    public void readFileInBase64WhenFileExist() throws IOException {
        String result = FileReaderUtils.readFileFromPathBase64(TestUtil.getFilePath(getClass(), "testfile.txt").toString());
        assertEquals("YXBwbGUgYXBwbGUgYXBwbGU=", result);
    }

    @Test
    public void readFileInBase64WhenFileNotExist() throws IOException {
        thrown.expect(IOException.class);
        thrown.expectMessage("File path must not be null");
        FileReaderUtils.readFileFromPath(TestUtil.getFilePath(getClass(), "testfile-not-exist.txt"));
    }

    @Test
    public void readBinaryFileWhenFileExist() throws IOException {
        String result = FileReaderUtils.readBinaryFileFromPath(TestUtil.getFilePath(getClass(), "testfilebin.txt"));
        assertEquals("YXBwbGUgYXBwbGUgYXBwbGU=", result);
    }

    @Test
    public void readBinaryFileWhenFileNotExist() throws IOException {
        thrown.expect(IOException.class);
        thrown.expectMessage("File path must not be null");
        FileReaderUtils.readBinaryFileFromPath(TestUtil.getFilePath(getClass(), "testfilebin-not-exist.txt"));
    }

    @Test
    public void readFileFromPathToStringWhenFileExist() throws IOException {
        String result = FileReaderUtils.readFileFromPath(TestUtil.getFilePath(getClass(), "testfile.txt"));
        assertEquals("apple apple apple", result);
    }

    @Test
    public void readFileFromPathToStringWhenFileNotExist() throws IOException {
        thrown.expect(IOException.class);
        thrown.expectMessage("File path must not be null");
        FileReaderUtils.readFileFromPath(TestUtil.getFilePath(getClass(), "testfile-not-exist.txt"));
    }

    @Test
    public void readFileFromClasspathToStringWhenFileExist() throws IOException {
        String result = FileReaderUtils.readFileFromClasspath("com/sequenceiq/cloudbreak/util/testfile.txt");
        assertEquals("apple apple apple", result);
    }

    @Test
    public void readFileFromClasspathToStringWhenFileNotExist() throws IOException {
        thrown.expect(FileNotFoundException.class);
        thrown.expectMessage("class path resource [com/sequenceiq/cloudbreak/util/testfile-not-exist.txt] cannot be opened because it does not exist");
        FileReaderUtils.readFileFromClasspath("com/sequenceiq/cloudbreak/util/testfile-not-exist.txt");
    }

}