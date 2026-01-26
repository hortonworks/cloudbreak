package com.sequenceiq.freeipa.flow.freeipa.common;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;

import java.io.IOException;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.util.FileReaderUtils;
import com.sequenceiq.freeipa.flow.freeipa.rebuild.event.RebuildFailureEvent;

@ExtendWith(MockitoExtension.class)
class FreeIpaFailedFlowAnalyzerTest {

    private final Exception validationException = new Exception("Validation failed. A required field validation check failed.");

    private final Exception nonValidationException = new IllegalArgumentException("General processing error.");

    private final Exception nullException = null;

    @Mock
    private FreeIpaValidationProperties validationProperties;

    @InjectMocks
    private FreeIpaFailedFlowAnalyzer underTest;

    @BeforeEach
    void before() {
        lenient().when(validationProperties.getFailedMessages()).thenReturn(Set.of(
                "Validation failed",
                "Modify its 'disableApiTermination' instance attribute and try again",
                "Testing s3 put",
                "Testing abfs write failed",
                "Please check you connection against",
                "Cloudera DataBus API network connectivity check failed",
                "Failed to upload freeipa backup to object storage",
                "Marketplace purchase eligibilty check returned errors",
                "QuotaExceeded",
                "Unhealthy instances found",
                "The maximum number of addresses has been reached",
                "Your AWS credential is not authorized to perform action",
                "Validating FreeIPA cloud storage permission for backup failed",
                "Quota reaches threshold",
                "You cannot specify tags for Spot instances requests if there are no Spot instances requests being created by the request",
                "Cannot execute method: terminateInstances",
                "Please remove the lock and try again",
                "difference between desired and actual capacity"
        ));
    }

    @Test
    public void testAnalyzeFailure() throws IOException {
        String fileName = String.format("analyzer/validation.txt");
        String[] rows = FileReaderUtils.readFileFromClasspath(fileName).split("\n");
        for (String row : rows) {
            FreeIpaFailureEvent freeIpaFailureEvent = new RebuildFailureEvent(
                    1L,
                    FailureType.ERROR,
                    new RuntimeException(row)
            );
            boolean validationFailedError = underTest.isValidationFailedError(freeIpaFailureEvent);
            assertTrue(validationFailedError, row + " Should be validation error");
        }
    }

    @Test
    void testTypeIsValidationAndExceptionIsValidationShouldBeTrue() {
        FreeIpaFailureEvent payload = createEvent(FailureType.VALIDATION, validationException);
        assertTrue(underTest.isValidationFailedError(payload),
                "Should be true because both FailureType and Exception indicate validation failure.");
    }

    @Test
    void testTypeIsValidationAndExceptionIsNotValidationShouldBeTrue() {
        FreeIpaFailureEvent payload = createEvent(FailureType.VALIDATION, nonValidationException);
        assertTrue(underTest.isValidationFailedError(payload),
                "Should be true because FailureType is VALIDATION, regardless of the exception content.");
    }

    @Test
    void testTypeIsNotValidationAndExceptionIsValidationShouldBeTrue() {
        FreeIpaFailureEvent payload = createEvent(FailureType.ERROR, validationException);
        assertTrue(underTest.isValidationFailedError(payload),
                "Should be true because the Exception's message indicates a validation failure.");
    }

    @Test
    void testCase4TypeIsNotValidationAndExceptionIsNotValidationShouldBeFalse() {
        FreeIpaFailureEvent payload = createEvent(FailureType.ERROR, nonValidationException);
        assertFalse(underTest.isValidationFailedError(payload),
                "Should be false because neither the FailureType nor the Exception indicate validation failure.");
    }

    @Test
    void testCase5TypeIsValidationAndNullExceptionShouldBeTrue() {
        FreeIpaFailureEvent payload = createEvent(FailureType.VALIDATION, nullException);
        assertTrue(underTest.isValidationFailedError(payload),
                "Should be true because FailureType is VALIDATION, even with a null exception.");
    }

    @Test
    void testNotValidationAndNullExceptionShouldBeFalse() {
        FreeIpaFailureEvent payload = createEvent(FailureType.ERROR, nullException);
        assertFalse(underTest.isValidationFailedError(payload),
                "Should be false when FailureType is not VALIDATION and Exception is null.");
    }

    @Test
    void testNonValidationTypeShouldBeFalse() {
        FreeIpaFailureEvent payload = createEvent(FailureType.ERROR, nonValidationException);
        assertFalse(underTest.isValidationFailedError(payload),
                "Should be false for ERROR type and non-validation exception.");
    }

    private FreeIpaFailureEvent createEvent(FailureType type, Exception exception) {
        return new RebuildFailureEvent(1L, type, exception);
    }

}