package com.sequenceiq.cloudbreak.sdx.common;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import jakarta.inject.Inject;

import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.dyngr.core.AttemptResult;
import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.auth.crn.CrnResourceDescriptor;
import com.sequenceiq.cloudbreak.sdx.TargetPlatform;
import com.sequenceiq.cloudbreak.sdx.common.model.SdxAccessView;
import com.sequenceiq.cloudbreak.sdx.common.model.SdxBasicView;
import com.sequenceiq.cloudbreak.sdx.common.polling.PollingResult;
import com.sequenceiq.cloudbreak.sdx.common.service.PlatformAwareSdxDeleteService;
import com.sequenceiq.cloudbreak.sdx.common.service.PlatformAwareSdxDescribeService;
import com.sequenceiq.cloudbreak.sdx.common.service.PlatformAwareSdxStatusService;
import com.sequenceiq.cloudbreak.sdx.common.status.StatusCheckResult;

@Service
public class PlatformAwareSdxConnector {

    private static final Logger LOGGER = LoggerFactory.getLogger(PlatformAwareSdxConnector.class);

    @Inject
    private Map<TargetPlatform, PlatformAwareSdxStatusService<?>> platformDependentSdxStatusServicesMap;

    @Inject
    private Map<TargetPlatform, PlatformAwareSdxDeleteService<?>> platformDependentSdxDeleteServices;

    @Inject
    private Map<TargetPlatform, PlatformAwareSdxDescribeService> platformDependentSdxDescribeServices;

    public Optional<String> getRemoteDataContext(String sdxCrn) {
        LOGGER.info("Getting remote data context for SDX {}", sdxCrn);
        return platformDependentSdxDescribeServices.get(TargetPlatform.getByCrn(sdxCrn)).getRemoteDataContext(sdxCrn);
    }

    public Map<String, String> getHmsServiceConfig(String sdxCrn) {
        return platformDependentSdxDescribeServices.get(TargetPlatform.getByCrn(sdxCrn)).getHmsServiceConfig(sdxCrn);
    }

    public void delete(String sdxCrn, Boolean force) {
        platformDependentSdxDeleteServices.get(TargetPlatform.getByCrn(sdxCrn)).deleteSdx(sdxCrn, force);
    }

    public AttemptResult<Object> getAttemptResultForDeletion(String environmentCrn, Set<String> sdxCrns) {
        PlatformAwareSdxDeleteService<?> platformAwareSdxDeleteService = platformDependentSdxDeleteServices.get(calculatePlatform(sdxCrns));
        Map<String, PollingResult> pollingResultForDeletion = platformAwareSdxDeleteService.getPollingResultForDeletion(environmentCrn, sdxCrns);
        return platformAwareSdxDeleteService.getAttemptResultForPolling(pollingResultForDeletion, "SDX deletion is failed for these: %s");
    }

    public Set<String> listSdxCrns(String environmentCrn) {
        LOGGER.info("Getting SDX CRN'S for the datalakes in the environment {}", environmentCrn);
        Set<String> paasSdxCrns = platformDependentSdxDescribeServices.get(TargetPlatform.PAAS).listSdxCrns(environmentCrn);
        Set<String> saasSdxCrns = platformDependentSdxDescribeServices.get(TargetPlatform.CDL).listSdxCrns(environmentCrn);
        if (!paasSdxCrns.isEmpty() && !saasSdxCrns.isEmpty()) {
            throw new IllegalStateException(String.format("Environment %s should not have SDX from both PaaS and SaaS platform", environmentCrn));
        }
        return Sets.union(saasSdxCrns, paasSdxCrns);
    }

    public Optional<SdxBasicView> getSdxBasicViewByEnvironmentCrn(String environmentCrn) {
        return platformDependentSdxDescribeServices.get(calculatePlatform(environmentCrn)).getSdxByEnvironmentCrn(environmentCrn);
    }

    public Optional<SdxAccessView> getSdxAccessViewByEnvironmentCrn(String environmentCrn) {
        return platformDependentSdxDescribeServices.get(calculatePlatform(environmentCrn)).getSdxAccessViewByEnvironmentCrn(environmentCrn);
    }

    public Set<Pair<String, StatusCheckResult>> listSdxCrnsWithAvailability(String environmentCrn) {
        Set<String> sdxCrns = listSdxCrns(environmentCrn);
        return platformDependentSdxStatusServicesMap.get(calculatePlatform(sdxCrns)).listSdxCrnStatusCheckPair(environmentCrn, sdxCrns);
    }

    private TargetPlatform calculatePlatform(String environmentCrn) {
        Set<String> sdxCrns = listSdxCrns(environmentCrn);
        return calculatePlatform(sdxCrns);
    }

    private static TargetPlatform calculatePlatform(Set<String> sdxCrns) {
        if (sdxCrns.stream().allMatch(crn -> CrnResourceDescriptor.CDL.checkIfCrnMatches(Crn.safeFromString(crn)))) {
            return TargetPlatform.CDL;
        } else if (sdxCrns.stream().allMatch(crn -> CrnResourceDescriptor.VM_DATALAKE.checkIfCrnMatches(Crn.safeFromString(crn)))) {
            return TargetPlatform.PAAS;
        }
        throw new IllegalStateException("Polling for SDX should be happen only for SaaS or PaaS only at the same time.");
    }
}