package com.sequenceiq.cloudbreak.cloud.azure.image.copy.parallel;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ParallelImageCopyParametersService {

    @Value("${cb.azure.image.copy.parallel.retry.count}")
    private int retryCount;

    @Value("${cb.azure.image.copy.parallel.retry.backoff.min.seconds}")
    private int backoffMinimumSeconds;

    @Value("${cb.azure.image.copy.parallel.retry.backoff.max.seconds}")
    private int backoffMaximumSeconds;

    @Value("${cb.azure.image.copy.parallel.lease.duration.seconds}")
    private int blobLeaseDurationSeconds;

    @Value("${cb.azure.image.copy.parallel.lease.renew.interval.seconds}")
    private int blobLeaseRenewIntervalSeconds;

    @Value("${cb.azure.image.copy.parallel.timeout.minutes}")
    private int imageCopyTimeoutMinutes;

    @Value("${cb.azure.image.copy.parallel.concurrency}")
    private int concurrency;

    @Value("${cb.azure.image.copy.parallel.prefetch}")
    private int prefetch;

    private Duration backoffMinimum;

    private Duration backoffMaximum;

    private Duration imageCopyTimeout;

    private Duration blobLeaseRenewInterval;

    @PostConstruct
    public void init() {
        backoffMinimum = Duration.of(backoffMinimumSeconds, ChronoUnit.SECONDS);
        backoffMaximum = Duration.of(backoffMaximumSeconds, ChronoUnit.SECONDS);
        imageCopyTimeout = Duration.of(imageCopyTimeoutMinutes, ChronoUnit.MINUTES);
        blobLeaseRenewInterval = Duration.of(blobLeaseRenewIntervalSeconds, ChronoUnit.SECONDS);
    }

    public int getRetryCount() {
        return retryCount;
    }

    public Duration getBackoffMinimum() {
        return backoffMinimum;
    }

    public Duration getBackoffMaximum() {
        return backoffMaximum;
    }

    public int getBlobLeaseDurationSeconds() {
        return blobLeaseDurationSeconds;
    }

    public Duration getImageCopyTimeout() {
        return imageCopyTimeout;
    }

    public Duration getBlobLeaseRenewInterval() {
        return blobLeaseRenewInterval;
    }

    public int getConcurrency() {
        return concurrency;
    }

    public int getPrefetch() {
        return prefetch;
    }
}
