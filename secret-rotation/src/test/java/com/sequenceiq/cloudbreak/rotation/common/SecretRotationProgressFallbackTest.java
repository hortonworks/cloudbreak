package com.sequenceiq.cloudbreak.rotation.common;

import static com.sequenceiq.cloudbreak.rotation.common.TestSecretType.TEST;
import static com.sequenceiq.cloudbreak.rotation.common.TestSecretType.TEST_2;
import static com.sequenceiq.cloudbreak.rotation.common.TestSecretType.TEST_3;
import static com.sequenceiq.cloudbreak.rotation.request.RotationSource.CLOUDBREAK;
import static com.sequenceiq.cloudbreak.rotation.request.RotationSource.FREEIPA;
import static com.sequenceiq.cloudbreak.rotation.request.RotationSource.REDBEAMS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.rotation.SecretType;
import com.sequenceiq.cloudbreak.rotation.request.RotationSource;
import com.sequenceiq.cloudbreak.rotation.request.StepProgressResponse;

class SecretRotationProgressFallbackTest {

    private static Map<RotationSource, SecretType> pollingTypes() {
        Map<RotationSource, SecretType> pollingTypes = new LinkedHashMap<>();
        pollingTypes.put(CLOUDBREAK, TEST);
        pollingTypes.put(FREEIPA, TEST_2);
        pollingTypes.put(REDBEAMS, TEST_3);
        return pollingTypes;
    }

    @Test
    void findFirstProgressReturnsFirstNonNullResponseAndSkipsRemainingSources() {
        StepProgressResponse expected = new StepProgressResponse();
        List<RotationSource> queried = new ArrayList<>();

        Optional<StepProgressResponse> result = SecretRotationProgressFallback.findFirstProgress(pollingTypes(), (source, secretType) -> {
            queried.add(source);
            return source == FREEIPA ? expected : null;
        });

        assertTrue(result.isPresent());
        assertSame(expected, result.get());
        assertEquals(List.of(CLOUDBREAK, FREEIPA), queried);
    }

    @Test
    void findFirstProgressSkipsNotFoundExceptionsAndReturnsLaterMatch() {
        StepProgressResponse expected = new StepProgressResponse();

        Optional<StepProgressResponse> result = SecretRotationProgressFallback.findFirstProgress(pollingTypes(), (source, secretType) -> {
            if (source == CLOUDBREAK) {
                throw new NotFoundException("local miss");
            }
            if (source == FREEIPA) {
                throw new jakarta.ws.rs.NotFoundException("remote miss");
            }
            return expected;
        });

        assertTrue(result.isPresent());
        assertSame(expected, result.get());
    }

    @Test
    void findFirstProgressSwallowsUnexpectedExceptionsAndContinues() {
        StepProgressResponse expected = new StepProgressResponse();

        Optional<StepProgressResponse> result = SecretRotationProgressFallback.findFirstProgress(pollingTypes(), (source, secretType) -> {
            if (source == CLOUDBREAK) {
                throw new IllegalStateException("service unavailable");
            }
            return source == REDBEAMS ? expected : null;
        });

        assertTrue(result.isPresent());
        assertSame(expected, result.get());
    }

    @Test
    void findFirstProgressReturnsEmptyWhenEverySourceYieldsNothing() {
        Optional<StepProgressResponse> result = SecretRotationProgressFallback.findFirstProgress(pollingTypes(), (source, secretType) -> {
            if (source == CLOUDBREAK) {
                throw new NotFoundException("miss");
            }
            return null;
        });

        assertTrue(result.isEmpty());
    }

    @Test
    void findFirstProgressReturnsEmptyForEmptyPollingTypes() {
        Optional<StepProgressResponse> result =
                SecretRotationProgressFallback.findFirstProgress(Map.of(), (source, secretType) -> new StepProgressResponse());

        assertTrue(result.isEmpty());
    }
}
