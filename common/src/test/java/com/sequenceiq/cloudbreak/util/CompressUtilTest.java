package com.sequenceiq.cloudbreak.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class CompressUtilTest {

    private CompressUtil underTest;

    @BeforeEach
    public void setUp() {
        underTest = new CompressUtil();
    }

    @Test
    public void testUpdateCompressedOutputFolders() throws IOException {
        // GIVEN
        // WHEN
        byte[] originalZipOutput = underTest.generateCompressedOutputFromFolders("test-salt/case1");
        byte[] updatedZipOutput = underTest.updateCompressedOutputFolders(List.of("test-salt/case2"), List.of("/salt/component1"), originalZipOutput);
        Map<String, String> result = getZipEntries(updatedZipOutput);
        // THEN
        assertEquals("myinit1", result.get("/pillar/component1/init.sls"));
        assertEquals("mycomponent1", result.get("/salt/component2/component.sls"));
        assertEquals("mycomponent2", result.get("/salt/component1/component.sls"));
    }

    private Map<String, String> getZipEntries(byte[] bytesContent) throws IOException {
        Map<String, String> result = new HashMap<>();
        try (ByteArrayInputStream bis = new ByteArrayInputStream(bytesContent)) {
            try (ZipInputStream zis = new ZipInputStream(bis)) {
                ZipEntry ze;
                while ((ze = zis.getNextEntry()) != null) {
                    if (!ze.isDirectory()) {
                        String name = ze.getName();
                        try (ByteArrayOutputStream bosForStr = new ByteArrayOutputStream()) {
                            String content = underTest.readBytesAsStringFromZip(zis, bosForStr);
                            result.put(name, content);
                        }
                    }
                }
            }
        }
        return result;
    }
}
