package com.sequenceiq.cloudbreak.sdx.common.service;

import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.sdx.common.status.StatusCheckResult;

public interface PlatformAwareSdxStatusService<S> extends PlatformAwareSdxCommonService {

    Logger LOGGER = LoggerFactory.getLogger(PlatformAwareSdxStatusService.class);

    Set<Pair<String, S>> listSdxCrnStatusPair(String environmentCrn);

    default Set<Pair<String, StatusCheckResult>> listSdxCrnStatusCheckPair(String environmentCrn) {
        return listSdxCrnStatusPair(environmentCrn).stream()
                .map(statusPair -> Pair.of(statusPair.getKey(), getAvailabilityStatusCheckResult(statusPair.getValue())))
                .collect(Collectors.toSet());
    }

    StatusCheckResult getAvailabilityStatusCheckResult(S status);
}
