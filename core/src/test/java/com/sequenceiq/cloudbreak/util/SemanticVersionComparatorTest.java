package com.sequenceiq.cloudbreak.util;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class SemanticVersionComparatorTest {

    @Test
    public void compare() {
        SemanticVersionComparator comparator = new SemanticVersionComparator();

        assertEquals("major desc", 1L, comparator.compare("2.0.0", "1.0.0"));
        assertEquals("minor desc", 1L, comparator.compare("2.1.0", "2.0.0"));
        assertEquals("patch desc", 1L, comparator.compare("2.1.1", "2.1.0"));
        assertEquals("equals", 0L, comparator.compare("2.0.0", "2.0.0"));
        assertEquals("major asc", -1L, comparator.compare("1.0.0", "2.0.0"));
        assertEquals("minor asc", -1L, comparator.compare("2.0.0", "2.1.0"));
        assertEquals("patch asc", -1L, comparator.compare("2.1.0", "2.1.1"));

        assertEquals("dev major desc", 1L, comparator.compare("2.0.0-dev.1", "1.0.0-dev.1"));
        assertEquals("dev minor desc", 1L, comparator.compare("2.1.0-dev.1", "2.0.0-dev.1"));
        assertEquals("dev patch desc", 1L, comparator.compare("2.1.1-dev.1", "2.1.0-dev.1"));
        assertEquals("dev desc", 1L, comparator.compare("2.1.1-dev.2", "2.1.1-dev.1"));
        assertEquals("dev equals", 0L, comparator.compare("2.0.0-dev.1", "2.0.0-dev.1"));
        assertEquals("dev major asc", -1L, comparator.compare("1.0.0-dev.1", "2.0.0-dev.1"));
        assertEquals("dev minor asc", -1L, comparator.compare("2.0.0-dev.1", "2.1.0-dev.1"));
        assertEquals("dev patch asc", -1L, comparator.compare("2.1.0-dev.1", "2.1.1-dev.1"));
        assertEquals("dev asc", -1L, comparator.compare("2.1.1-dev.1", "2.1.1-dev.2"));
    }
}