package com.sequenceiq.cloudbreak.cloud.aws.scheduler;

interface CancellationCheck {
    boolean isCancelled();
}
