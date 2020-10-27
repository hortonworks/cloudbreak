package com.sequenceiq.cloudbreak.service.secret.service;

import java.util.Optional;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.metrics.MetricService;
import com.sequenceiq.cloudbreak.common.metrics.type.MetricType;
import com.sequenceiq.cloudbreak.service.Retry;
import com.sequenceiq.cloudbreak.tracing.TracingUtil;

import io.opentracing.References;
import io.opentracing.Scope;
import io.opentracing.Span;
import io.opentracing.Tracer;

@Service
public class VaultRetryService {

    private static final Logger LOGGER = LoggerFactory.getLogger(VaultRetryService.class);

    private final MetricService metricService;

    private final Tracer tracer;

    public VaultRetryService(MetricService metricService, Tracer tracer) {
        this.metricService = metricService;
        this.tracer = tracer;
    }

    @Retryable(
            value = Retry.ActionFailedException.class,
            maxAttemptsExpression = "${vault.retry.maxattempt:5}",
            backoff = @Backoff(delayExpression = "${vault.retry.delay:2000}",
                    multiplierExpression = "${vault.retry.multiplier:2}",
                    maxDelayExpression = "${vault.retry.maxdelay:10000}")
    )
    public <T> T tryReadingVault(Supplier<T> action) throws Retry.ActionFailedException {
        return executeVaultOperationWithTrace(action, "read", MetricType.VAULT_READ_FAILED);
    }

    @Retryable(
            value = Retry.ActionFailedException.class,
            maxAttemptsExpression = "${vault.retry.maxattempt:5}",
            backoff = @Backoff(delayExpression = "${vault.retry.delay:2000}",
                    multiplierExpression = "${vault.retry.multiplier:2}",
                    maxDelayExpression = "${vault.retry.maxdelay:10000}")
    )
    public <T> T tryWritingVault(Supplier<T> action) throws Retry.ActionFailedException {
        return executeVaultOperationWithTrace(action, "write", MetricType.VAULT_WRITE_FAILED);
    }

    private <T> T executeVaultOperationWithTrace(Supplier<T> action, String operation, MetricType metricType) {
        Optional<Span> optionalSpan = initSpan(operation);
        return optionalSpan.map(span -> {
            try (Scope ignored = tracer.activateSpan(span)) {
                try {
                    return action.get();
                } catch (RuntimeException e) {
                    span.setTag(TracingUtil.ERROR, true);
                    span.setTag(TracingUtil.MESSAGE, e.getMessage());
                    return handleException(operation, e, metricType);
                }
            } finally {
                span.finish();
            }
        }).orElseGet(() -> {
            try {
                return action.get();
            } catch (RuntimeException e) {
                return handleException(operation, e, metricType);
            }
        });
    }

    private Optional<Span> initSpan(String operationType) {
        return Optional.ofNullable(tracer.activeSpan()).map(activeSpan -> {
            Span span = tracer.buildSpan("Vault - " + operationType)
                    .addReference(References.FOLLOWS_FROM, activeSpan.context())
                    .start();
            span.setTag(TracingUtil.COMPONENT, "Vault");
            return span;
        });
    }

    private <T> T handleException(String operation, RuntimeException e, MetricType metricType) {
        LOGGER.error("Exception during vault " + operation, e);
        metricService.incrementMetricCounter(metricType);
        throw new Retry.ActionFailedException(e.getMessage());
    }
}
