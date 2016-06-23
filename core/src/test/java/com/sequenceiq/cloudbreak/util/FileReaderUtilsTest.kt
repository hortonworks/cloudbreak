package com.sequenceiq.cloudbreak.util

import org.junit.Assert.assertEquals

import java.io.IOException

import org.junit.Test

import com.sequenceiq.cloudbreak.TestUtil


class FileReaderUtilsTest {

    @Test
    @Throws(IOException::class)
    fun readFileInBase64WhenFileExist() {
        val result = FileReaderUtils.readFileFromPath(TestUtil.getFilePath(javaClass, "testfile.txt"))
        assertEquals("YXBwbGUgYXBwbGUgYXBwbGU=", result)
    }

    @Test(expected = IOException::class)
    @Throws(IOException::class)
    fun readFileInBase64WhenFileNotExist() {
        FileReaderUtils.readFileFromPath(TestUtil.getFilePath(javaClass, "testfile-not-exist.txt"))
    }

    @Test
    @Throws(IOException::class)
    fun readBinaryFileWhenFileExist() {
        val result = FileReaderUtils.readBinaryFileFromPath(TestUtil.getFilePath(javaClass, "testfilebin.txt"))
        assertEquals("YXBwbGUgYXBwbGUgYXBwbGU=", result)
    }

    @Test(expected = IOException::class)
    @Throws(IOException::class)
    fun readBinaryFileWhenFileNotExist() {
        FileReaderUtils.readBinaryFileFromPath(TestUtil.getFilePath(javaClass, "testfilebin-not-exist.txt"))
    }

    @Test
    @Throws(IOException::class)
    fun readFileFromPathToStringWhenFileExist() {
        val result = FileReaderUtils.readFileFromPathToString(TestUtil.getFilePath(javaClass, "testfile.txt"))
        assertEquals("apple apple apple", result)
    }

    @Test(expected = IOException::class)
    @Throws(IOException::class)
    fun readFileFromPathToStringWhenFileNotExist() {
        FileReaderUtils.readFileFromPathToString(TestUtil.getFilePath(javaClass, "testfile-not-exist.txt"))
    }

    @Test
    @Throws(IOException::class)
    fun readFileFromClasspathToStringWhenFileExist() {
        val result = FileReaderUtils.readFileFromClasspath("com/sequenceiq/cloudbreak/util/testfile.txt")
        assertEquals("apple apple apple", result)
    }

    @Test(expected = IOException::class)
    @Throws(IOException::class)
    fun readFileFromClasspathToStringWhenFileNotExist() {
        FileReaderUtils.readFileFromClasspath("com/sequenceiq/cloudbreak/util/testfile-not-exist.txt")
    }

}