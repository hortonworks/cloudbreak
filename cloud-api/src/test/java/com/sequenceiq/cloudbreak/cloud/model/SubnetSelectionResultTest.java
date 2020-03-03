package com.sequenceiq.cloudbreak.cloud.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

public class SubnetSelectionResultTest {

    @Test
    public void testWhenCtorWithListWithElementThenHasErrorIsFalse() {
        SubnetSelectionResult subnetSelectionResult = new SubnetSelectionResult(List.of(new CloudSubnet()));

        assertFalse(subnetSelectionResult.hasError());
    }

    @Test
    public void testWhenCtorWithListWithElementThenHasResultIsTrue() {
        SubnetSelectionResult subnetSelectionResult = new SubnetSelectionResult(List.of(new CloudSubnet()));

        assertTrue(subnetSelectionResult.hasResult());
    }

    @Test
    public void testWhenCtorWithEmptyListThenHasResultIsFalse() {
        SubnetSelectionResult subnetSelectionResult = new SubnetSelectionResult(List.of());

        assertFalse(subnetSelectionResult.hasResult());
        assertFalse(subnetSelectionResult.hasError());
    }

    @Test
    public void testWhenCtorWithErrorMessageThenHasErrorIsTrue() {
        SubnetSelectionResult subnetSelectionResult = new SubnetSelectionResult("my error message");

        assertTrue(subnetSelectionResult.hasError());
        assertFalse(subnetSelectionResult.hasResult());
        assertEquals("my error message", subnetSelectionResult.getErrorMessage());
    }

}
