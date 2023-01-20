package com.sequenceiq.cloudbreak.cloud.azure.client;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.azure.storage.blob.models.CopyStatusType;

class CopyStateTest {

    @Test
    public void testBigNumberIsParsedCorrectly() {
        CopyState copyState = CopyState.of(CopyStatusType.PENDING, "100000000000000000/200000000000000000");

        assertEquals(CopyStatusType.PENDING, copyState.getCopyStatusType());
        assertEquals(100000000000000000L, copyState.getBytesCopied());
        assertEquals(200000000000000000L, copyState.getTotalBytes());
    }

    @Test
    public void testWhenProgressCannotBe() {
        CopyState copyState = CopyState.of(CopyStatusType.PENDING, "");

        assertEquals(CopyStatusType.PENDING, copyState.getCopyStatusType());
        assertEquals(-1, copyState.getBytesCopied());
        assertEquals(-1, copyState.getTotalBytes());
    }

}