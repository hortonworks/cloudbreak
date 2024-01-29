package com.sequenceiq.cloudbreak.sdx.common;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.auth.altus.model.Entitlement;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.sdx.TargetPlatform;
import com.sequenceiq.cloudbreak.sdx.common.polling.PollingResult;
import com.sequenceiq.cloudbreak.sdx.common.status.StatusCheckResult;

public interface SdxService<S> {

    TargetPlatform targetPlatform();

    Optional<String> getRemoteDataContext(String crn);

    void deleteSdx(String sdxCrn, Boolean force);

    Set<String> listSdxCrns(String environmentName, String environmentCrn);

    Optional<String> getSdxCrnByEnvironmentCrn(String environmentCrn);

    Set<Pair<String, S>> listSdxCrnStatusPair(String environmentCrn, String environmentName, Set<String> sdxCrns);

    default Set<Pair<String, StatusCheckResult>> listSdxCrnStatusCheckPair(String environmentCrn, String environmentName, Set<String> sdxCrns) {
        return listSdxCrnStatusPair(environmentCrn, environmentName, sdxCrns).stream()
                .map(statusPair -> Pair.of(statusPair.getKey(), getAvailabilityStatusCheckResult(statusPair.getValue())))
                .collect(Collectors.toSet());
    }

    default Map<String, PollingResult> getPollingResultForDeletion(String environmentCrn, String environmentName, Set<String> sdxCrns) {
        if (isPlatformEntitled(Crn.safeFromString(environmentCrn).getAccountId())) {
            return listSdxCrnStatusPair(environmentCrn, environmentName, sdxCrns).stream()
                    .collect(Collectors.toMap(Pair::getLeft, pair -> getDeletePollingResultByStatus(pair.getRight())));
        }
        return Map.of();
    }

    PollingResult getDeletePollingResultByStatus(S status);

    StatusCheckResult getAvailabilityStatusCheckResult(S status);

    default Optional<Entitlement> getEntitlement() {
        return Optional.empty();
    }

    EntitlementService getEntitlementService();

    default boolean isPlatformEntitled(String accountId) {
        return getEntitlement().isEmpty() || getEntitlementService().isEntitledFor(accountId, getEntitlement().get());
    }

    boolean isSdxClusterHA(String environmentCrn);
}
