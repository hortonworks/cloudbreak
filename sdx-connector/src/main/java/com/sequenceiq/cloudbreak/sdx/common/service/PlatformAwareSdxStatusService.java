package com.sequenceiq.cloudbreak.sdx.common.service;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.dyngr.core.AttemptResult;
import com.dyngr.core.AttemptResults;
import com.google.common.base.Joiner;
import com.sequenceiq.cloudbreak.sdx.common.polling.PollingResult;
import com.sequenceiq.cloudbreak.sdx.common.status.StatusCheckResult;

public interface PlatformAwareSdxStatusService<S> extends PlatformAwareSdxCommonService {

    Logger LOGGER = LoggerFactory.getLogger(PlatformAwareSdxStatusService.class);

    Set<Pair<String, S>> listSdxCrnStatusPair(String environmentCrn, Set<String> sdxCrns);

    default Set<Pair<String, StatusCheckResult>> listSdxCrnStatusCheckPair(String environmentCrn, Set<String> sdxCrns) {
        return listSdxCrnStatusPair(environmentCrn, sdxCrns).stream()
                .map(statusPair -> Pair.of(statusPair.getKey(), getAvailabilityStatusCheckResult(statusPair.getValue())))
                .collect(Collectors.toSet());
    }

    default AttemptResult<Object> getAttemptResultForPolling(Map<String, PollingResult> pollingResult, String failedPollingErrorMessageTemplate) {
        if (!pollingResult.isEmpty()) {
            Set<String> failedSdxCrns = pollingResult.entrySet().stream()
                    .filter(entry -> PollingResult.FAILED.equals(entry.getValue()))
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toSet());
            if (!failedSdxCrns.isEmpty()) {
                String errorMessage = String.format(failedPollingErrorMessageTemplate, Joiner.on(",").join(failedSdxCrns));
                LOGGER.info(errorMessage);
                return AttemptResults.breakFor(new IllegalStateException(errorMessage));
            }
            return AttemptResults.justContinue();
        }
        return AttemptResults.finishWith(null);
    }

    StatusCheckResult getAvailabilityStatusCheckResult(S status);
}
