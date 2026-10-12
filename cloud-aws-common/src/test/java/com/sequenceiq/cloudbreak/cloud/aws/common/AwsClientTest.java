package com.sequenceiq.cloudbreak.cloud.aws.common;

import static com.sequenceiq.cloudbreak.cloud.aws.common.AwsClient.BASE_ICE_BACKOFF_SECONDS_RUN_INSTANCES;
import static com.sequenceiq.cloudbreak.cloud.aws.common.AwsClient.LEGACY_RETRY_TOKEN_BUCKET_SIZE;
import static com.sequenceiq.cloudbreak.cloud.aws.common.AwsClient.LEGACY_RETRY_TOKEN_COST;
import static com.sequenceiq.cloudbreak.cloud.aws.common.AwsClient.MAX_CLIENT_RETRIES;
import static com.sequenceiq.cloudbreak.cloud.aws.common.AwsClient.MAX_ICE_BACKOFF_SECONDS_RUN_INSTANCES;
import static com.sequenceiq.cloudbreak.cloud.aws.common.AwsClient.MAX_ICE_RETRIES_RUN_INSTANCES;
import static com.sequenceiq.cloudbreak.cloud.aws.common.AwsSdkErrorCodes.INSUFFICIENT_INSTANCE_CAPACITY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.retry.RetryPolicy;
import software.amazon.awssdk.core.retry.RetryPolicyContext;
import software.amazon.awssdk.core.retry.conditions.TokenBucketRetryCondition;

class AwsClientTest {

    private static final String REQUEST_LIMIT_EXCEEDED = "RequestLimitExceeded";

    private static final String INVALID_PARAMETER_VALUE = "InvalidParameterValue";

    private static final String INTERNAL_ERROR = "InternalError";

    private static final Duration DEFAULT_MAX_BACKOFF = Duration.ofSeconds(20);

    private static final int BACKOFF_SAMPLES = 50;

    private final RetryPolicy underTest = AwsClient.createRunInstancesRetryPolicy();

    @Test
    void testRunInstancesRetryPolicyKeepsDefaultRetryBudgetAndLegacyTokenBucket() {
        assertEquals(MAX_CLIENT_RETRIES, underTest.numRetries());
        assertThat(underTest.toBuilder().retryCapacityCondition()).isInstanceOf(TokenBucketRetryCondition.class);
        assertEquals(LEGACY_RETRY_TOKEN_BUCKET_SIZE, tokenBucket().tokensAvailable());
    }

    @Test
    void testRunInstancesRetryPolicyStopsInsufficientInstanceCapacityAtLimit() {
        ExecutionAttributes request = new ExecutionAttributes();
        IntStream.range(0, MAX_ICE_RETRIES_RUN_INSTANCES)
                .forEach(retriesAttempted -> assertTrue(shouldRetry(request, INSUFFICIENT_INSTANCE_CAPACITY, 500, retriesAttempted)));
        assertFalse(shouldRetry(request, INSUFFICIENT_INSTANCE_CAPACITY, 500, MAX_ICE_RETRIES_RUN_INSTANCES));
    }

    @Test
    void testRunInstancesRetryPolicyCountsOnlyInsufficientInstanceCapacityRetriesAgainstLimit() {
        ExecutionAttributes request = new ExecutionAttributes();
        IntStream.range(0, MAX_ICE_RETRIES_RUN_INSTANCES)
                .forEach(retriesAttempted -> assertTrue(shouldRetry(request, REQUEST_LIMIT_EXCEEDED, 400, retriesAttempted)));
        IntStream.range(MAX_ICE_RETRIES_RUN_INSTANCES, MAX_ICE_RETRIES_RUN_INSTANCES * 2)
                .forEach(retriesAttempted -> assertTrue(shouldRetry(request, INSUFFICIENT_INSTANCE_CAPACITY, 500, retriesAttempted)));
        assertFalse(shouldRetry(request, INSUFFICIENT_INSTANCE_CAPACITY, 500, MAX_ICE_RETRIES_RUN_INSTANCES * 2));
    }

    @Test
    void testRunInstancesRetryPolicyRetriesThrottlingBeyondInsufficientInstanceCapacityLimit() {
        assertTrue(shouldRetry(new ExecutionAttributes(), REQUEST_LIMIT_EXCEEDED, 400, MAX_ICE_RETRIES_RUN_INSTANCES * 2));
        assertFalse(shouldRetry(new ExecutionAttributes(), REQUEST_LIMIT_EXCEEDED, 400, MAX_CLIENT_RETRIES));
    }

    @Test
    void testRunInstancesRetryPolicyDoesNotRetryClientError() {
        assertFalse(shouldRetry(new ExecutionAttributes(), INVALID_PARAMETER_VALUE, 400, 0));
    }

    @Test
    void testRunInstancesRetryPolicyInsufficientInstanceCapacityDoesNotDrainTokenBucket() {
        IntStream.range(0, LEGACY_RETRY_TOKEN_BUCKET_SIZE)
                .forEach(i -> assertTrue(shouldRetry(new ExecutionAttributes(), INSUFFICIENT_INSTANCE_CAPACITY, 500, 0)));
        assertEquals(LEGACY_RETRY_TOKEN_BUCKET_SIZE, tokenBucket().tokensAvailable());
    }

    @Test
    void testRunInstancesRetryPolicyInternalErrorDrainsTokenBucket() {
        int allowedRetries = LEGACY_RETRY_TOKEN_BUCKET_SIZE / LEGACY_RETRY_TOKEN_COST;
        IntStream.range(0, allowedRetries).forEach(i -> assertTrue(shouldRetry(new ExecutionAttributes(), INTERNAL_ERROR, 500, 0)));
        assertFalse(shouldRetry(new ExecutionAttributes(), INTERNAL_ERROR, 500, 0));
        assertTrue(shouldRetry(new ExecutionAttributes(), INSUFFICIENT_INSTANCE_CAPACITY, 500, 0));
    }

    @Test
    void testRunInstancesRetryPolicyInsufficientInstanceCapacityBackoffReachesCap() {
        ExecutionAttributes request = new ExecutionAttributes();
        IntStream.range(0, MAX_ICE_RETRIES_RUN_INSTANCES)
                .forEach(retriesAttempted -> shouldRetry(request, INSUFFICIENT_INSTANCE_CAPACITY, 500, retriesAttempted));

        Duration maxDelay = maxBackoff(context(request, INSUFFICIENT_INSTANCE_CAPACITY, 500, MAX_ICE_RETRIES_RUN_INSTANCES - 1));

        assertThat(maxDelay).isGreaterThan(DEFAULT_MAX_BACKOFF).isLessThanOrEqualTo(Duration.ofSeconds(MAX_ICE_BACKOFF_SECONDS_RUN_INSTANCES));
    }

    @Test
    void testRunInstancesRetryPolicyInsufficientInstanceCapacityBackoffIgnoresThrottlingRetries() {
        ExecutionAttributes request = new ExecutionAttributes();
        IntStream.range(0, MAX_ICE_RETRIES_RUN_INSTANCES)
                .forEach(retriesAttempted -> shouldRetry(request, REQUEST_LIMIT_EXCEEDED, 400, retriesAttempted));
        shouldRetry(request, INSUFFICIENT_INSTANCE_CAPACITY, 500, MAX_ICE_RETRIES_RUN_INSTANCES);

        Duration maxDelay = maxBackoff(context(request, INSUFFICIENT_INSTANCE_CAPACITY, 500, MAX_ICE_RETRIES_RUN_INSTANCES));

        assertThat(maxDelay).isLessThanOrEqualTo(Duration.ofSeconds(BASE_ICE_BACKOFF_SECONDS_RUN_INSTANCES));
    }

    @Test
    void testRunInstancesRetryPolicyNonCapacityErrorUsesDefaultBackoff() {
        assertThat(maxBackoff(context(new ExecutionAttributes(), INTERNAL_ERROR, 500, MAX_ICE_RETRIES_RUN_INSTANCES - 1)))
                .isLessThanOrEqualTo(DEFAULT_MAX_BACKOFF);
    }

    private TokenBucketRetryCondition tokenBucket() {
        return (TokenBucketRetryCondition) underTest.toBuilder().retryCapacityCondition();
    }

    private boolean shouldRetry(ExecutionAttributes request, String errorCode, int statusCode, int retriesAttempted) {
        return underTest.aggregateRetryCondition().shouldRetry(context(request, errorCode, statusCode, retriesAttempted));
    }

    private Duration maxBackoff(RetryPolicyContext context) {
        return IntStream.range(0, BACKOFF_SAMPLES)
                .mapToObj(i -> underTest.backoffStrategy().computeDelayBeforeNextRetry(context))
                .max(Duration::compareTo)
                .orElseThrow();
    }

    private RetryPolicyContext context(ExecutionAttributes request, String errorCode, int statusCode, int retriesAttempted) {
        AwsServiceException exception = AwsServiceException.builder()
                .statusCode(statusCode)
                .awsErrorDetails(AwsErrorDetails.builder().errorCode(errorCode).build())
                .build();
        return RetryPolicyContext.builder()
                .exception(exception)
                .httpStatusCode(statusCode)
                .retriesAttempted(retriesAttempted)
                .executionAttributes(request)
                .build();
    }
}
