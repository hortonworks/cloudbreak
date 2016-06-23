package com.sequenceiq.cloudbreak.service.stack.flow

import org.junit.Assert.assertEquals

import java.io.IOException
import java.util.HashSet

import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.runners.MockitoJUnitRunner

import com.sequenceiq.cloudbreak.util.FileReaderUtils

@RunWith(MockitoJUnitRunner::class)
class FingerprintParserUtilTest {


    @Test
    @SuppressWarnings("unchecked")
    @Throws(IOException::class)
    fun parseFingerprintGCP() {
        val expected = HashSet<String>()
        expected.add("db:01:97:98:81:2a:25:a0:05:62:39:0b:3c:65:49:ac")
        expected.add("1b:42:16:09:65:e0:e4:39:31:5d:7d:29:6a:76:2b:87")
        val consoleLog = FileReaderUtils.readFileFromClasspath("com/sequenceiq/cloudbreak/service/stack/flow/gcp-console.txt")
        val fingerprints = FingerprintParserUtil.parseFingerprints(consoleLog)
        assertEquals(expected, fingerprints)
    }

    @Test
    @SuppressWarnings("unchecked")
    @Throws(IOException::class)
    fun parseFingerprintOpenstack() {
        val expected = HashSet<String>()
        expected.add("0d:0f:d8:85:33:f5:e0:73:25:8f:d4:b3:52:ed:89:94")
        expected.add("19:ff:09:ba:e1:72:7c:39:6f:82:0d:f5:21:01:4a:b4")
        val consoleLog = FileReaderUtils.readFileFromClasspath("com/sequenceiq/cloudbreak/service/stack/flow/openstack-console.txt")
        val fingerprints = FingerprintParserUtil.parseFingerprints(consoleLog)
        assertEquals(expected, fingerprints)
    }
}
