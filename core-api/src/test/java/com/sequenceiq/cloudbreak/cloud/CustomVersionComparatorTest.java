package com.sequenceiq.cloudbreak.cloud;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class CustomVersionComparatorTest {

    private static final CompareLevel MAINTENANCE_COMPARE_LEVEL = CompareLevel.MAINTENANCE;

    private static final CompareLevel MAJOR_COMPARE_LEVEL = CompareLevel.FULL;

    private static final CompareLevel MINOR_COMPARE_LEVEL = CompareLevel.MINOR;

    private CustomVersionComparator underTest = new CustomVersionComparator();

    @Test
    public void testCompareShouldReturnsMinusOneWhenTheMaintenanceVersionIsGreaterThanTheCurrent() {
        int actual = underTest.compare("7.0.2", "7.0.3", MAINTENANCE_COMPARE_LEVEL);
        assertEquals(-1, actual);
    }

    @Test
    public void testCompareShouldReturnsOneWhenTheMaintenanceVersionIsLowerThanTheCurrent() {
        int actual = underTest.compare("7.0.3", "7.0.2", MAINTENANCE_COMPARE_LEVEL);
        assertEquals(1, actual);
    }

    @Test
    public void testCompareShouldReturnsZeroWhenTheMaintenanceVersionsAreEqual() {
        int actual = underTest.compare("7.0.3", "7.0.3", MAINTENANCE_COMPARE_LEVEL);
        assertEquals(0, actual);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCompareShouldThrowExceptionWhenTheNewVersionIsContainsOtherCharacter() {
        underTest.compare("7.0.3", "7.x.3", MAINTENANCE_COMPARE_LEVEL);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCompareShouldThrowExceptionWhenTheVersionsAreNull() {
        underTest.compare(null, null, MAINTENANCE_COMPARE_LEVEL);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCompareShouldThrowExceptionWhenTheMajorVersionsAreNotEqualsInCaseOfMaintenanceLevelCompare() {
        underTest.compare("8.0.2", "7.0.3", MAINTENANCE_COMPARE_LEVEL);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCompareShouldThrowExceptionWhenTheMinorVersionsAreNotEqualsInCaseOfMaintenanceLevelCompare() {
        underTest.compare("7.1.2", "7.0.3", MAINTENANCE_COMPARE_LEVEL);
    }

    @Test
    public void testCompareShouldReturnsMinusOneWhenTheMajorVersionIsGreaterThanTheCurrent() {
        int actual = underTest.compare("7.0.3", "8.0.3", MAJOR_COMPARE_LEVEL);
        assertEquals(-1, actual);
    }

    @Test
    public void testCompareShouldReturnsOneWhenTheMajorVersionIsLowerThanTheCurrent() {
        int actual = underTest.compare("8.0.3", "7.0.3", MAJOR_COMPARE_LEVEL);
        assertEquals(1, actual);
    }

    @Test
    public void testCompareShouldReturnsZeroWhenTheMajorVersionsAreEqual() {
        int actual = underTest.compare("8.0.3", "8.0.3", MAJOR_COMPARE_LEVEL);
        assertEquals(0, actual);
    }

    @Test
    public void testCompareShouldReturnsMinusOneWhenTheMinorVersionIsGreaterThanTheCurrent() {
        int actual = underTest.compare("7.0.3", "7.1.3", MINOR_COMPARE_LEVEL);
        assertEquals(-1, actual);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCompareShouldThrowExceptionWhenTheNewMajorVersionIsLower() {
        underTest.compare("8.0.3", "7.1.3", MINOR_COMPARE_LEVEL);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCompareShouldThrowExceptionWhenTheNewMajorVersionsIsGreater() {
        underTest.compare("8.0.3", "9.1.3", MINOR_COMPARE_LEVEL);
    }

    @Test
    public void testCompareShouldReturnsOneWhenTheMinorVersionIsLowerThanTheCurrent() {
        int actual = underTest.compare("7.1.3", "7.0.2", MINOR_COMPARE_LEVEL);
        assertEquals(1, actual);
    }

    @Test
    public void testCompareShouldReturnsZeroWhenTheMinorVersionsAreEqual() {
        int actual = underTest.compare("7.1.3", "7.1.3", MINOR_COMPARE_LEVEL);
        assertEquals(0, actual);
    }
}