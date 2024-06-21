package com.sequenceiq.cloudbreak.sdx.common.service;

import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;

import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.sdx.common.polling.PollingResult;

public interface PlatformAwareSdxDeleteService<S> extends PlatformAwareSdxStatusService<S> {

    void deleteSdx(String sdxCrn, Boolean force);

    default Map<String, PollingResult> getPollingResultForDeletion(String environmentCrn) {
        if (isPlatformEntitled(Crn.safeFromString(environmentCrn).getAccountId())) {
            return listSdxCrnStatusPair(environmentCrn).stream()
                    .collect(Collectors.toMap(Pair::getLeft, pair -> getDeletePollingResultByStatus(pair.getRight())));
        }
        return Map.of();
    }

    PollingResult getDeletePollingResultByStatus(S status);
}
