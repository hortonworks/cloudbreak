package com.sequenceiq.cloudbreak.service.upgrade.sync.operationresult;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class CmSyncOperationSummaryTest {

    private final CmSyncOperationSummary.Builder underTest = CmSyncOperationSummary.builder();

    @Test
    void testBuilderWhenSuccessOnlyThenSuccess() {
        CmSyncOperationSummary cmSyncOperationsummary = underTest.withSuccess("successMessage").build();

        assertTrue(cmSyncOperationsummary.hasSucceeded());
        assertEquals("successMessage", cmSyncOperationsummary.getMessage());
    }

    @Test
    void testBuilderWhenSuccessAndFailureThenFailure() {
        CmSyncOperationSummary cmSyncOperationsummary = underTest.withSuccess("successMessage.").withError("errorMessage.").build();

        assertFalse(cmSyncOperationsummary.hasSucceeded());
        assertEquals("successMessage. errorMessage.", cmSyncOperationsummary.getMessage());
    }

}
