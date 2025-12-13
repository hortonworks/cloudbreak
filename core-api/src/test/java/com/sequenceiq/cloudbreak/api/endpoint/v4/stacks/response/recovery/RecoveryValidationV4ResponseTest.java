package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.recovery;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class RecoveryValidationV4ResponseTest {

    private static final String RECOVERABLE_MESSAGE_1 = "recoverable message 1";

    private static final String RECOVERABLE_MESSAGE_2 = "recoverable message 2";

    private static final String NON_RECOVERABLE_MESSAGE_1 = "non-recoverable message 1";

    private static final String NON_RECOVERABLE_MESSAGE_2 = "non-recoverable message 2";

    @Test
    void testMergeTwoRecoverableResponses() {
        RecoveryValidationV4Response baseResponse = new RecoveryValidationV4Response(RECOVERABLE_MESSAGE_1, RecoveryStatus.RECOVERABLE);
        RecoveryValidationV4Response otherResponse = new RecoveryValidationV4Response(RECOVERABLE_MESSAGE_2, RecoveryStatus.RECOVERABLE);
        RecoveryValidationV4Response mergedResponse = baseResponse.merge(otherResponse);
        assertEquals("recoverable message 1 recoverable message 2", mergedResponse.getReason());
        assertEquals(RecoveryStatus.RECOVERABLE, mergedResponse.getStatus());
    }

    @Test
    void testMergeTwoNonRecoverableResponses() {
        RecoveryValidationV4Response baseResponse = new RecoveryValidationV4Response(NON_RECOVERABLE_MESSAGE_1, RecoveryStatus.NON_RECOVERABLE);
        RecoveryValidationV4Response otherResponse = new RecoveryValidationV4Response(NON_RECOVERABLE_MESSAGE_2, RecoveryStatus.NON_RECOVERABLE);
        RecoveryValidationV4Response mergedResponse = baseResponse.merge(otherResponse);
        assertEquals("non-recoverable message 1 Next issue: non-recoverable message 2", mergedResponse.getReason());
        assertEquals(RecoveryStatus.NON_RECOVERABLE, mergedResponse.getStatus());
    }

    @Test
    void testMergeRecoverableAndNonRecoverableResponses() {
        RecoveryValidationV4Response baseResponse = new RecoveryValidationV4Response(RECOVERABLE_MESSAGE_1, RecoveryStatus.RECOVERABLE);
        RecoveryValidationV4Response otherResponse = new RecoveryValidationV4Response(NON_RECOVERABLE_MESSAGE_1, RecoveryStatus.NON_RECOVERABLE);
        RecoveryValidationV4Response mergedResponse = baseResponse.merge(otherResponse);
        assertEquals("non-recoverable message 1", mergedResponse.getReason());
        assertEquals(RecoveryStatus.NON_RECOVERABLE, mergedResponse.getStatus());
    }

    @Test
    void testMergeNonrecoverableAndRecoverableResponses() {
        RecoveryValidationV4Response baseResponse = new RecoveryValidationV4Response(NON_RECOVERABLE_MESSAGE_1, RecoveryStatus.NON_RECOVERABLE);
        RecoveryValidationV4Response otherResponse = new RecoveryValidationV4Response(RECOVERABLE_MESSAGE_1, RecoveryStatus.RECOVERABLE);
        RecoveryValidationV4Response mergedResponse = baseResponse.merge(otherResponse);
        assertEquals("non-recoverable message 1", mergedResponse.getReason());
        assertEquals(RecoveryStatus.NON_RECOVERABLE, mergedResponse.getStatus());
    }
}