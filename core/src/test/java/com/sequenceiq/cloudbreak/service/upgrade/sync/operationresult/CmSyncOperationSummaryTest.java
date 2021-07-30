package com.sequenceiq.cloudbreak.service.upgrade.sync.operationresult;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class CmSyncOperationSummaryTest {

    private static final String SUCCESS_MESSAGE = "successMessage";

    private static final String ERROR_MESSAGE = "errorMessage";

    private final CmSyncOperationSummary.Builder underTest = CmSyncOperationSummary.builder();

    @Test
    void testBuilderWhenSuccessOnlyThenSuccess() {
        CmSyncOperationSummary cmSyncOperationsummary = underTest.withSuccess(SUCCESS_MESSAGE).build();

        assertTrue(cmSyncOperationsummary.hasSucceeded());
        assertEquals("successMessage", cmSyncOperationsummary.getMessage());
    }

    @Test
    void testBuilderWhenSuccessAndFailureThenFailure() {
        CmSyncOperationSummary cmSyncOperationsummary = underTest.withSuccess(SUCCESS_MESSAGE).withError(ERROR_MESSAGE).build();

        assertFalse(cmSyncOperationsummary.hasSucceeded());
        assertEquals(String.format("%s %s", SUCCESS_MESSAGE, ERROR_MESSAGE), cmSyncOperationsummary.getMessage());
    }

    @Test
    void testBuilderMergeWhenEmptyWithError() {
        CmSyncOperationSummary.Builder other = CmSyncOperationSummary.builder().withError(ERROR_MESSAGE);

        underTest.merge(other);

        assertThat(underTest.getMessages(), hasSize(1));
        assertThat(underTest.getMessages(), containsInAnyOrder(ERROR_MESSAGE));
        assertFalse(underTest.isSuccess());
    }

    @Test
    void testBuilderMergeWhenThisSuccessWithOtherSuccessThenSuccess() {
        CmSyncOperationSummary.Builder other = CmSyncOperationSummary.builder().withSuccess(SUCCESS_MESSAGE);
        underTest.withSuccess(SUCCESS_MESSAGE);

        underTest.merge(other);

        assertThat(underTest.getMessages(), hasSize(2));
        assertThat(underTest.getMessages(), containsInAnyOrder(SUCCESS_MESSAGE, SUCCESS_MESSAGE));
        assertTrue(underTest.isSuccess());
    }

    @Test
    void testBuilderMergeWhenThisErrorWithOtherSuccessThenError() {
        CmSyncOperationSummary.Builder other = CmSyncOperationSummary.builder().withSuccess(SUCCESS_MESSAGE);
        underTest.withError(ERROR_MESSAGE);

        underTest.merge(other);

        assertThat(underTest.getMessages(), hasSize(2));
        assertThat(underTest.getMessages(), containsInAnyOrder(SUCCESS_MESSAGE, ERROR_MESSAGE));
        assertFalse(underTest.isSuccess());
    }

    @Test
    void testBuilderMergeWhenThisSuccessWithOtherErrorThenError() {
        CmSyncOperationSummary.Builder other = CmSyncOperationSummary.builder().withError(ERROR_MESSAGE);
        underTest.withSuccess(SUCCESS_MESSAGE);

        underTest.merge(other);

        assertThat(underTest.getMessages(), hasSize(2));
        assertThat(underTest.getMessages(), containsInAnyOrder(SUCCESS_MESSAGE, ERROR_MESSAGE));
        assertFalse(underTest.isSuccess());
    }

    @Test
    void testBuilderMergeWhenThisErrorWithOtherErrorThenError() {
        CmSyncOperationSummary.Builder other = CmSyncOperationSummary.builder().withError(ERROR_MESSAGE);
        underTest.withError(ERROR_MESSAGE);

        underTest.merge(other);

        assertThat(underTest.getMessages(), hasSize(2));
        assertThat(underTest.getMessages(), containsInAnyOrder(ERROR_MESSAGE, ERROR_MESSAGE));
        assertFalse(underTest.isSuccess());
    }

}
