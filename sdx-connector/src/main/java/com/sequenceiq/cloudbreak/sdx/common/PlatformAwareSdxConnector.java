package com.sequenceiq.cloudbreak.sdx.common;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.dyngr.core.AttemptResult;
import com.dyngr.core.AttemptResults;
import com.google.common.base.Joiner;
import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.sdx.TargetPlatform;
import com.sequenceiq.cloudbreak.sdx.common.polling.PollingResult;
import com.sequenceiq.cloudbreak.sdx.common.status.StatusCheckResult;

@Service
public class PlatformAwareSdxConnector {

    private static final Logger LOGGER = LoggerFactory.getLogger(PlatformAwareSdxConnector.class);

    @Inject
    private Map<TargetPlatform, SdxService<?>> platformDependentServiceMap;

    public Optional<String> getRemoteDataContext(String sdxCrn) {
        LOGGER.debug("Getting remote data context for SDX {}", sdxCrn);
        return platformDependentServiceMap.get(TargetPlatform.getByCrn(sdxCrn)).getRemoteDataContext(sdxCrn);
    }

    public void delete(String sdxCrn, Boolean force) {
        platformDependentServiceMap.get(TargetPlatform.getByCrn(sdxCrn)).deleteSdx(sdxCrn, force);
    }

    public AttemptResult<Object> getAttemptResultForDeletion(String environmentCrn, String environmentName, Set<String> sdxCrns) {
        return getAttemptResultForPolling(platformDependentServiceMap.get(calculatePlatform(sdxCrns))
                        .getPollingResultForDeletion(environmentCrn, environmentName, sdxCrns), "SDX deletion is failed for these: %s");
    }

    public Set<String> listSdxCrns(String environmentName, String environmentCrn) {
        Set<String> saasSdxCrns = platformDependentServiceMap.get(TargetPlatform.CDL).listSdxCrns(environmentName, environmentCrn);
        Set<String> paasSdxCrns = platformDependentServiceMap.get(TargetPlatform.PAAS).listSdxCrns(environmentName, environmentCrn);
        if (!paasSdxCrns.isEmpty() && !saasSdxCrns.isEmpty()) {
            throw new IllegalStateException(String.format("Environment %s should not have SDX from both PaaS and SaaS platform", environmentCrn));
        }
        return Sets.union(saasSdxCrns, paasSdxCrns);
    }

    public Set<Pair<String, StatusCheckResult>> listSdxCrnsWithAvailability(String environmentName, String environmentCrn, Set<String> sdxCrns) {
        return platformDependentServiceMap.get(calculatePlatform(sdxCrns)).listSdxCrnStatusCheckPair(environmentCrn, environmentName, sdxCrns);
    }

    private TargetPlatform calculatePlatform(Set<String> sdxCrns) {
        if (sdxCrns.stream().allMatch(crn -> Crn.ResourceType.INSTANCE.equals(Crn.safeFromString(crn).getResourceType()))) {
            return TargetPlatform.CDL;
        } else if (sdxCrns.stream().allMatch(crn -> Crn.ResourceType.DATALAKE.equals(Crn.safeFromString(crn).getResourceType()))) {
            return TargetPlatform.PAAS;
        }
        throw new IllegalStateException("Polling for SDX should be happen only for SaaS or PaaS only at the same time.");
    }

    private AttemptResult<Object> getAttemptResultForPolling(Map<String, PollingResult> pollingResult, String failedPollingErrorMessageTemplate) {
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
}
