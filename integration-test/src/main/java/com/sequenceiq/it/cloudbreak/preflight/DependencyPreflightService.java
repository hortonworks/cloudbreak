package com.sequenceiq.it.cloudbreak.preflight;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.sequenceiq.it.cloudbreak.exception.TestFailException;

@Service
public class DependencyPreflightService {

    private static final Logger LOG = LoggerFactory.getLogger(DependencyPreflightService.class);

    @Value("${integrationtest.preflight.enabled:false}")
    private boolean enabled;

    @Inject
    private List<PreflightCheck> preflightChecks;

    public void runPreflightChecksOrFail() {
        if (!enabled) {
            LOG.info("Preflight dependency checks are disabled (integrationtest.preflight.enabled=false), skipping.");
            return;
        }
        List<PreflightCheckResult> failures = new ArrayList<>();
        for (PreflightCheck preflightCheck : preflightChecks) {
            PreflightCheckResult result = preflightCheck.check();
            logResult(result);
            if (!result.isSkipped() && !result.isPassed()) {
                failures.add(result);
            }
        }

        if (!failures.isEmpty()) {
            String aggregatedMessage = failures.stream()
                    .map(result -> result.getCheckName() + ": " + result.getMessage())
                    .collect(Collectors.joining("; "));
            throw new TestFailException("Preflight dependency checks failed, aborting before test execution: " + aggregatedMessage);
        }
    }

    private void logResult(PreflightCheckResult result) {
        if (result.isSkipped()) {
            LOG.info("Preflight check [{}] skipped: {}", result.getCheckName(), result.getMessage());
        } else if (result.isPassed()) {
            LOG.info("Preflight check [{}] passed: {}", result.getCheckName(), result.getMessage());
        } else {
            LOG.error("Preflight check [{}] FAILED: {}", result.getCheckName(), result.getMessage());
        }
    }
}
