package com.sequenceiq.cloudbreak.util;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.junit.Test;

import com.sequenceiq.cloudbreak.TestUtil;


public class FileReaderUtilsTest {

    @Test
    public void readFileInBase64WhenFileExist() throws IOException {
        String result = FileReaderUtils.readFileFromPath(TestUtil.getFilePath(getClass(), "testfile.txt"));
        assertEquals("YXBwbGUgYXBwbGUgYXBwbGU=", result);
    }

    @Test(expected = IOException.class)
    public void readFileInBase64WhenFileNotExist() throws IOException {
        FileReaderUtils.readFileFromPath(TestUtil.getFilePath(getClass(), "testfile-not-exist.txt"));
    }

    @Test
    public void readBinaryFileWhenFileExist() throws IOException {
        String result = FileReaderUtils.readBinaryFileFromPath(TestUtil.getFilePath(getClass(), "testfilebin.txt"));
        assertEquals("YXBwbGUgYXBwbGUgYXBwbGU=", result);
    }

    @Test(expected = IOException.class)
    public void readBinaryFileWhenFileNotExist() throws IOException {
        FileReaderUtils.readBinaryFileFromPath(TestUtil.getFilePath(getClass(), "testfilebin-not-exist.txt"));
    }

    @Test
    public void readFileFromPathToStringWhenFileExist() throws IOException {
        String result = FileReaderUtils.readFileFromPathToString(TestUtil.getFilePath(getClass(), "testfile.txt"));
        assertEquals("apple apple apple", result);
    }

    @Test(expected = IOException.class)
    public void readFileFromPathToStringWhenFileNotExist() throws IOException {
        FileReaderUtils.readFileFromPathToString(TestUtil.getFilePath(getClass(), "testfile-not-exist.txt"));
    }

    @Test
    public void readFileFromClasspathToStringWhenFileExist() throws IOException {
        String result = FileReaderUtils.readFileFromClasspath("com/sequenceiq/cloudbreak/util/testfile.txt");
        assertEquals("apple apple apple", result);
    }

    @Test(expected = IOException.class)
    public void readFileFromClasspathToStringWhenFileNotExist() throws IOException {
        FileReaderUtils.readFileFromClasspath("com/sequenceiq/cloudbreak/util/testfile-not-exist.txt");
    }

}