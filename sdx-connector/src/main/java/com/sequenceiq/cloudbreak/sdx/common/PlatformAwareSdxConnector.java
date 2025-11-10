package com.sequenceiq.cloudbreak.sdx.common;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.dyngr.core.AttemptResult;
import com.dyngr.core.AttemptResults;
import com.google.common.base.Joiner;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.sdx.RdcConstants;
import com.sequenceiq.cloudbreak.sdx.RdcView;
import com.sequenceiq.cloudbreak.sdx.TargetPlatform;
import com.sequenceiq.cloudbreak.sdx.common.model.SdxAccessView;
import com.sequenceiq.cloudbreak.sdx.common.model.SdxBasicView;
import com.sequenceiq.cloudbreak.sdx.common.polling.PollingResult;
import com.sequenceiq.cloudbreak.sdx.common.service.PlatformAwareSdxDeleteService;
import com.sequenceiq.cloudbreak.sdx.common.service.PlatformAwareSdxDescribeService;
import com.sequenceiq.cloudbreak.sdx.common.service.PlatformAwareSdxDhTearDownService;
import com.sequenceiq.cloudbreak.sdx.common.service.PlatformAwareSdxStartStopService;
import com.sequenceiq.cloudbreak.sdx.common.service.PlatformAwareSdxStatusService;
import com.sequenceiq.cloudbreak.sdx.common.service.RdcViewFactory;
import com.sequenceiq.cloudbreak.sdx.common.status.StatusCheckResult;

@Service
public class PlatformAwareSdxConnector {

    private static final Logger LOGGER = LoggerFactory.getLogger(PlatformAwareSdxConnector.class);

    @Inject
    private RdcViewFactory datalakeContextViewFactory;

    @Inject
    private Map<TargetPlatform, PlatformAwareSdxStatusService<?>> platformDependentSdxStatusServicesMap;

    @Inject
    private Map<TargetPlatform, PlatformAwareSdxDeleteService<?>> platformDependentSdxDeleteServices;

    @Inject
    private Map<TargetPlatform, PlatformAwareSdxStartStopService> platformDependentSdxStartStopServices;

    @Inject
    private Map<TargetPlatform, PlatformAwareSdxDescribeService> platformDependentSdxDescribeServices;

    @Inject
    private Map<TargetPlatform, PlatformAwareSdxDhTearDownService> platformDependentSdxDhTearDownServices;

    @Deprecated
    public Optional<String> getRemoteDataContext(String sdxCrn) {
        LOGGER.info("Getting remote data context for SDX {}", sdxCrn);
        return getRdcView(sdxCrn).getRemoteDataContext();
    }

    @Deprecated
    public Map<String, String> getHmsServiceConfig(String sdxCrn) {
        return getRdcView(sdxCrn).getServiceConfigs(RdcConstants.HIVE_SERVICE);
    }

    public RdcView getRdcView(String sdxCrn) {
        PlatformAwareSdxDescribeService sdxDescribeService = platformDependentSdxDescribeServices.get(TargetPlatform.getByCrn(sdxCrn));
        Optional<String> remoteDataContext = sdxDescribeService.getRemoteDataContext(sdxCrn);
        RdcView rdcView = datalakeContextViewFactory.create(sdxCrn, remoteDataContext);
        return sdxDescribeService.extendRdcView(rdcView);
    }

    public void tearDownDatahub(String sdxCrn, String datahubCrn) {
        platformDependentSdxDhTearDownServices.get(TargetPlatform.getByCrn(sdxCrn)).tearDownDataHub(sdxCrn, datahubCrn);
    }

    public void deleteByEnvironment(String environmentName, Boolean force) {
        platformDependentSdxDescribeServices.values().forEach(describeService ->
                describeService.listSdxCrnsDetachedIncluded(environmentName).forEach(crn ->
                        platformDependentSdxDeleteServices.get(TargetPlatform.getByCrn(crn)).deleteSdx(crn, force)));
    }

    public AttemptResult<Object> getAttemptResultForDeletion(String environmentCrn) {
        Map<PollingResult, String> sdxCrnsByPollingResult = platformDependentSdxDeleteServices.values().stream()
                .map(deleteService -> deleteService.getPollingResultForDeletion(environmentCrn).entrySet())
                .flatMap(Collection::stream)
                .collect(Collectors.groupingBy(Map.Entry::getValue, Collectors.mapping(Map.Entry::getKey, Collectors.joining(","))));
        return getAttemptResultForPolling(sdxCrnsByPollingResult);
    }

    public void startByEnvironment(String environmentCrn) {
        getSdxBasicViewByEnvironmentCrn(environmentCrn)
                .ifPresent(sdx -> platformDependentSdxStartStopServices.get(TargetPlatform.getByCrn(sdx.crn()))
                        .startSdx(sdx.crn()));
    }

    public void stopByEnvironment(String environmentCrn) {
        getSdxBasicViewByEnvironmentCrn(environmentCrn)
                .ifPresent(sdx -> platformDependentSdxStartStopServices.get(TargetPlatform.getByCrn(sdx.crn()))
                        .stopSdx(sdx.crn()));
    }

    public Set<String> listSdxCrns(String environmentCrn) {
        LOGGER.info("Getting SDX CRN'S for the datalakes in the environment {}", environmentCrn);
        Set<String> paasSdxCrns = platformDependentSdxDescribeServices.get(TargetPlatform.PAAS).listSdxCrns(environmentCrn);
        if (CollectionUtils.isEmpty(paasSdxCrns)) {
            for (TargetPlatform targetPlatform : List.of(TargetPlatform.CDL, TargetPlatform.PDL)) {
                Set<String> sdxCrns =  platformDependentSdxDescribeServices.get(targetPlatform).listSdxCrns(environmentCrn);
                if (!CollectionUtils.isEmpty(sdxCrns)) {
                    return sdxCrns;
                }
            }
        }
        return paasSdxCrns;
    }

    public Optional<SdxBasicView> getSdxBasicViewByEnvironmentCrn(String environmentCrn) {
        return platformDependentSdxDescribeServices.get(calculatePlatform(environmentCrn)).getSdxByEnvironmentCrn(environmentCrn);
    }

    public Optional<SdxAccessView> getSdxAccessViewByEnvironmentCrn(String environmentCrn) {
        return platformDependentSdxDescribeServices.get(calculatePlatform(environmentCrn)).getSdxAccessViewByEnvironmentCrn(environmentCrn);
    }

    public Set<Pair<String, StatusCheckResult>> listSdxCrnsWithAvailability(String environmentCrn) {
        return platformDependentSdxStatusServicesMap.get(calculatePlatform(environmentCrn)).listSdxCrnStatusCheckPair(environmentCrn);
    }

    public Optional<String> getCACertsForEnvironment(String environmentCrn) {
        return platformDependentSdxDescribeServices.get(calculatePlatform(environmentCrn)).getCACertsForEnvironment(environmentCrn);
    }

    public Set<String> getSdxDomains(String environmentCrn) {
        return platformDependentSdxDescribeServices.get(calculatePlatform(environmentCrn)).getSdxDomains(environmentCrn);
    }

    public void validateIfOtherPlatformsHasSdx(String environmentCrn, TargetPlatform currentPlatform) {
        Set<TargetPlatform> platforms = platformDependentSdxDescribeServices.entrySet().stream()
                .filter(entry -> !currentPlatform.equals(entry.getKey()))
                .filter(entry -> !entry.getValue().listSdxCrns(environmentCrn).isEmpty())
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
        if (!platforms.isEmpty()) {
            throw new BadRequestException(String.format("Platforms [%s] have already SDX cluster!", platforms));
        }
    }

    private TargetPlatform calculatePlatform(String environmentCrn) {
        Set<String> sdxCrns = listSdxCrns(environmentCrn);
        return calculatePlatform(sdxCrns);
    }

    private static TargetPlatform calculatePlatform(Set<String> sdxCrns) {
        Set<TargetPlatform> targetPlatforms = sdxCrns.stream().map(TargetPlatform::getByCrn).collect(Collectors.toSet());
        if (targetPlatforms.isEmpty()) {
            // FIXME CB-30627: workaround for mock tests that use stack api (bypassing distrox's sdx validation)
            return TargetPlatform.PAAS;
        } else if (targetPlatforms.size() > 1) {
            String joinedSdxCrns = Joiner.on(",").join(sdxCrns);
            LOGGER.error("Could not decide target SDX platform based on SDX CRN list [{}].", joinedSdxCrns);
            throw new IllegalStateException(
                    String.format("Could not determine Data Lake platform from CRNs [%s], please contact Cloudera support!", joinedSdxCrns));
        }
        return targetPlatforms.iterator().next();
    }

    private AttemptResult<Object> getAttemptResultForPolling(Map<PollingResult, String> sdxCrnsByPollingResult) {
        if (sdxCrnsByPollingResult.containsKey(PollingResult.FAILED)) {
            String errorMessage = String.format("Data Lake delete failed for %s", sdxCrnsByPollingResult.get(PollingResult.FAILED));
            LOGGER.error(errorMessage);
            return AttemptResults.breakFor(new IllegalStateException(errorMessage));
        }
        if (sdxCrnsByPollingResult.containsKey(PollingResult.IN_PROGRESS)) {
            LOGGER.debug("Data Lake delete is in progress for {}", sdxCrnsByPollingResult.get(PollingResult.IN_PROGRESS));
            return AttemptResults.justContinue();
        }
        LOGGER.debug("Data Lake delete finished for {}", sdxCrnsByPollingResult.get(PollingResult.COMPLETED));
        return AttemptResults.finishWith(null);
    }
}