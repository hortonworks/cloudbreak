package com.sequenceiq.cloudbreak.service.parcel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ManifestMapperTest {

    private ManifestMapper underTest;

    @BeforeEach
    void setUp() {
        underTest = new ManifestMapper();
    }

    @Test
    void testMapReturnsOpenSearchForSemanticSearch() {
        assertEquals("opensearch", underTest.map("semantic_search"));
    }

    @Test
    void testMapReturnsOriginalKeyWhenNotInMapper() {
        assertEquals("hdfs", underTest.map("hdfs"));
    }

    @Test
    void testMapReturnsOriginalKeyForUnknownService() {
        assertEquals("some_unknown_service", underTest.map("some_unknown_service"));
    }

    @Test
    void testMapReturnsNullForNullInput() {
        assertNull(underTest.map(null));
    }

    @Test
    void testMapReturnsBlankForBlankInput() {
        assertEquals("", underTest.map(""));
    }

    @Test
    void testMapReturnsWhitespaceForWhitespaceInput() {
        assertEquals("   ", underTest.map("   "));
    }

    @Test
    void testMapIsCaseSensitive() {
        assertEquals("SEMANTIC_SEARCH", underTest.map("SEMANTIC_SEARCH"));
    }
}
