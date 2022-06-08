package com.sequenceiq.cloudbreak.saas.sdx;

import static com.cloudera.thunderhead.service.sdxsvccommon.SDXSvcCommonProto.InstanceHighLevelStatus.Value.UNHEALTHY;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_SAAS_SDX_INTEGRATION;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.cloudera.thunderhead.service.sdxsvccommon.SDXSvcCommonProto;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.model.Entitlement;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.saas.client.sdx.GrpcSdxSaasClient;
import com.sequenceiq.cloudbreak.saas.client.sdx.GrpcServiceDiscoveryClient;
import com.sequenceiq.cloudbreak.saas.sdx.polling.PollingResult;
import com.sequenceiq.cloudbreak.saas.sdx.status.StatusCheckResult;

@Service
public class SaasSdxService extends AbstractSdxService<SDXSvcCommonProto.InstanceHighLevelStatus.Value> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SaasSdxService.class);

    @Inject
    private GrpcSdxSaasClient grpcSdxSaasClient;

    @Inject
    private GrpcServiceDiscoveryClient grpcServiceDiscoveryClient;

    @Override
    public Optional<String> getRemoteDataContext(String crn) {
        if (isPlatformEntitled(Crn.safeFromString(crn).getAccountId())) {
            try {
                return Optional.of(grpcServiceDiscoveryClient.getRemoteDataContext(crn));
            } catch (JsonProcessingException e) {
                LOGGER.error("Json processing failed, thus we cannot query remote data context.");
            }
        }
        return Optional.empty();
    }

    @Override
    public TargetPlatform targetPlatform() {
        return TargetPlatform.SAAS;
    }

    @Override
    public void deleteSdx(String sdxCrn, Boolean force) {
        if (isPlatformEntitled(Crn.safeFromString(sdxCrn).getAccountId())) {
            LOGGER.info("Calling deleteInstance for SDX SaaS instance {}", sdxCrn);
            grpcSdxSaasClient.deleteInstance(sdxCrn);
        }
    }

    @Override
    public Set<String> listSdxCrns(String environmentName, String environmentCrn) {
        if (isPlatformEntitled(Crn.safeFromString(environmentCrn).getAccountId())) {
            return grpcSdxSaasClient.listInstances(environmentCrn).stream()
                    .map(SDXSvcCommonProto.Instance::getCrn)
                    .collect(Collectors.toSet());
        }
        return Set.of();
    }

    @Override
    public Set<Pair<String, SDXSvcCommonProto.InstanceHighLevelStatus.Value>> listSdxCrnStatusPair(String environmentCrn,
            String environmentName, Set<String> sdxCrns) {
        if (isPlatformEntitled(ThreadBasedUserCrnProvider.getAccountId())) {
            return grpcSdxSaasClient.listInstances(environmentCrn).stream()
                    .filter(instance -> sdxCrns.contains(instance.getCrn()))
                    .map(instance -> Pair.of(instance.getCrn(), instance.getStatus()))
                    .collect(Collectors.toSet());
        }
        return Set.of();
    }

    @Override
    public PollingResult getDeletePollingResultByStatus(SDXSvcCommonProto.InstanceHighLevelStatus.Value status) {
        return UNHEALTHY.equals(status) ? PollingResult.FAILED : PollingResult.IN_PROGRESS;
    }

    @Override
    public StatusCheckResult getAvailabilityStatusCheckResult(SDXSvcCommonProto.InstanceHighLevelStatus.Value status) {
        return SDXSvcCommonProto.InstanceHighLevelStatus.Value.HEALTHY.equals(status) ? StatusCheckResult.AVAILABLE : StatusCheckResult.NOT_AVAILABLE;
    }

    @Override
    public Optional<Entitlement> getEntitlement() {
        return Optional.of(CDP_SAAS_SDX_INTEGRATION);
    }
}
