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
        Optional<Span> optionalSpan = TracingUtil.initOptionalSpan(tracer, "Vault", operation);
        try (Scope ignored = optionalSpan.map(tracer::activateSpan).orElse(null)) {
            try {
                return action.get();
            } catch (RuntimeException e) {
                optionalSpan.ifPresent(span -> {
                    span.setTag(TracingUtil.ERROR, true);
                    span.setTag(TracingUtil.MESSAGE, e.getLocalizedMessage());
                });
                LOGGER.error("Exception during vault " + operation, e);
                metricService.incrementMetricCounter(metricType);
                throw new Retry.ActionFailedException(e.getMessage());
            }
        } finally {
            optionalSpan.ifPresent(Span::finish);
        }
    }
}
