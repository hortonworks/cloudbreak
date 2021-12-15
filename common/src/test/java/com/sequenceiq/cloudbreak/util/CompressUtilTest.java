package com.sequenceiq.cloudbreak.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class CompressUtilTest {

    private CompressUtil underTest;

    @BeforeEach
    public void setUp() {
        underTest = new CompressUtil();
    }

    @Test
    public void testUpdateCompressedOutputFoldersAndCompareResults() throws IOException {
        // GIVEN
        // WHEN
        byte[] originalZipOutput = underTest.generateCompressedOutputFromFolders("test-salt/case1");
        byte[] updatedZipOutput = underTest.updateCompressedOutputFolders(List.of("test-salt/case2"), List.of("/salt/component1"), originalZipOutput);
        Map<String, String> mergedContentMap = underTest.getZipEntries(updatedZipOutput);
        boolean fullContentMatch = underTest.compareCompressedContent(originalZipOutput, updatedZipOutput);
        boolean component1ContentMatch =  underTest.compareCompressedContent(originalZipOutput, updatedZipOutput, List.of("/salt/component1"));
        boolean component2ContentMatch = underTest.compareCompressedContent(originalZipOutput, updatedZipOutput, List.of("/salt/component2"));
        // THEN
        assertEquals("myinit1", mergedContentMap.get("/pillar/component1/init.sls"));
        assertEquals("mycomponent1", mergedContentMap.get("/salt/component2/component.sls"));
        assertEquals("mycomponent2", mergedContentMap.get("/salt/component1/component.sls"));
        assertFalse(fullContentMatch);
        assertFalse(component1ContentMatch);
        assertTrue(component2ContentMatch);
    }
}
