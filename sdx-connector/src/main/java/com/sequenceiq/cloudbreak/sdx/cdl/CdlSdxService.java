package com.sequenceiq.cloudbreak.sdx.cdl;


import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.ENABLE_CONTAINERIZED_DATALKE;
import static org.apache.commons.lang3.StringUtils.isBlank;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.sequenceiq.cloudbreak.auth.altus.model.Entitlement;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.sdx.TargetPlatform;
import com.sequenceiq.cloudbreak.sdx.common.AbstractSdxService;
import com.sequenceiq.cloudbreak.sdx.common.polling.PollingResult;
import com.sequenceiq.cloudbreak.sdx.common.status.StatusCheckResult;

@Service
public class CdlSdxService extends AbstractSdxService<CdlCrudProto.StatusType.Value> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CdlSdxService.class);

    @Inject
    private GrpcSdxCdlClient grpcClient;

    @Inject
    private GrpcServiceDiscoveryClient grpcServiceDiscoveryClient;

    @Override
    public TargetPlatform targetPlatform() {
        return TargetPlatform.CDL;
    }

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
    public void deleteSdx(String sdxCrn, Boolean force) {
        if (isPlatformEntitled(Crn.safeFromString(sdxCrn).getAccountId())) {
            grpcClient.deleteDatalake(sdxCrn);
        }
    }

    @Override
    public Set<String> listSdxCrns(String environmentName, String environmentCrn) {
        if (isPlatformEntitled(Crn.safeFromString(environmentCrn).getAccountId())) {
            CdlCrudProto.DatalakeResponse datalake = grpcClient.findDatalake(isBlank(environmentCrn) ? environmentName : environmentCrn, "");
            return Set.of(datalake.getCrn());
        }
        return Set.of();
    }

    @Override
    public Set<Pair<String, CdlCrudProto.StatusType.Value>> listSdxCrnStatusPair(String environmentCrn, String environmentName, Set<String> sdxCrns) {
        Set<Pair<String, CdlCrudProto.StatusType.Value>> result = new HashSet<>();
        if (isPlatformEntitled(Crn.safeFromString(environmentCrn).getAccountId())) {
            if (CollectionUtils.isNotEmpty(sdxCrns)) {
                sdxCrns.forEach(crn -> {
                            CdlCrudProto.DatalakeResponse response = grpcClient.findDatalake(isBlank(environmentCrn) ? environmentName : environmentCrn, crn);
                            result.add(Pair.of(crn, CdlCrudProto.StatusType.Value.valueOf(response.getStatus())));
                        }
                );
            } else {
                CdlCrudProto.DatalakeResponse response = grpcClient.findDatalake(isBlank(environmentCrn) ? environmentName : environmentCrn, "");
                result.add(Pair.of(response.getCrn(), CdlCrudProto.StatusType.Value.valueOf(response.getStatus())));
            }
        }
        return result;
    }

    @Override
    public PollingResult getDeletePollingResultByStatus(CdlCrudProto.StatusType.Value status) {
        return CdlCrudProto.StatusType.Value.DELETED == status ? PollingResult.COMPLETED : PollingResult.IN_PROGRESS;
    }

    @Override
    public StatusCheckResult getAvailabilityStatusCheckResult(CdlCrudProto.StatusType.Value status) {
        if (CdlCrudProto.StatusType.Value.RUNNING == status || CdlCrudProto.StatusType.Value.PROVISIONED == status) {
            return StatusCheckResult.AVAILABLE;
        }
        return StatusCheckResult.NOT_AVAILABLE;
    }

    @Override
    public boolean isSdxClusterHA(String environmentCrn) {
        throw new UnsupportedOperationException("Currently we cannot decide if a SDX CDL cluster is HA or not.");
    }

    @Override
    public Optional<Entitlement> getEntitlement() {
        return Optional.of(ENABLE_CONTAINERIZED_DATALKE);
    }
}
