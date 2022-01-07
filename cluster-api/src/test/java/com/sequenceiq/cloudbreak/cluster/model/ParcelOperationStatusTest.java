package com.sequenceiq.cloudbreak.cluster.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.Test;

public class ParcelOperationStatusTest {

    @Test
    public void testMerge() {
        ParcelOperationStatus status1 = new ParcelOperationStatus(Map.of(), Map.of("parcel1", "v1"));
        ParcelOperationStatus status2 = new ParcelOperationStatus(Map.of("parcel2", "v2"), Map.of());
        ParcelOperationStatus status3 = new ParcelOperationStatus(Map.of("parcel3", "v3"), Map.of("parcel2", "v2"));

        ParcelOperationStatus result = status1.merge(status2).merge(status3);

        assertEquals(1, result.getSuccessful().size());
        assertTrue(result.getSuccessful().containsEntry("parcel3", "v3"));
        assertEquals(2, result.getFailed().size());
        assertTrue(result.getFailed().containsEntry("parcel1", "v1"));
        assertTrue(result.getFailed().containsEntry("parcel2", "v2"));
    }
}