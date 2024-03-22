package com.sequenceiq.cloudbreak.sdx.cdl;


import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.ENABLE_CONTAINERIZED_DATALKE;
import static org.apache.commons.lang3.StringUtils.isBlank;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import jakarta.inject.Inject;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.protobuf.InvalidProtocolBufferException;
import com.sequenceiq.cloudbreak.auth.altus.model.Entitlement;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.sdx.TargetPlatform;
import com.sequenceiq.cloudbreak.sdx.common.AbstractSdxService;
import com.sequenceiq.cloudbreak.sdx.common.polling.PollingResult;
import com.sequenceiq.cloudbreak.sdx.common.status.StatusCheckResult;

@Service
public class CdlSdxService extends AbstractSdxService<CdlCrudProto.StatusType.Value> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CdlSdxService.class);

    @Value("${sdx.cdl.enabled:false}")
    private boolean cdlEnabled;

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
        if (isEnabled(crn)) {
            try {
                return Optional.of(grpcServiceDiscoveryClient.getRemoteDataContext(crn));
            } catch (JsonProcessingException | InvalidProtocolBufferException e) {
                LOGGER.info("Json processing failed, thus we cannot query remote data context. Crn: {}, Exception message: {}", crn, e.getMessage());
            } catch (RuntimeException exception) {
                LOGGER.info("Not able to fetch the RDC for CDL from Service Discovery. CRN: {}, Exception message: {}", crn, exception.getMessage());
            }
        }
        return Optional.empty();
    }

    @Override
    public void deleteSdx(String sdxCrn, Boolean force) {
        if (isEnabled(sdxCrn)) {
            try {
                grpcClient.deleteDatalake(sdxCrn);
            } catch (RuntimeException exception) {
                LOGGER.info("We are not able to delete CDL with CRN: {}, Exception: {}", sdxCrn, exception.getMessage());
            }
        }
    }

    @Override
    public Set<String> listSdxCrns(String environmentName, String environmentCrn) {
        if (isEnabled(environmentCrn)) {
            try {
                CdlCrudProto.DatalakeResponse datalake = grpcClient.findDatalake(isBlank(environmentCrn) ? environmentName : environmentCrn, "");
                return Set.of(datalake.getCrn());
            } catch (RuntimeException exception) {
                LOGGER.info("CDL not found for environment. CRN: {}. Name: {}. Exception: {}",
                        environmentCrn, environmentName, exception.getMessage());
                return Collections.emptySet();
            }
        }
        return Set.of();
    }

    @Override
    public Optional<String> getSdxCrnByEnvironmentCrn(String environmentCrn) {
        if  (isEnabled(environmentCrn)) {
            try {
                CdlCrudProto.DatalakeResponse datalake = grpcClient.findDatalake(environmentCrn, "");
                return Optional.of(datalake.getCrn());
            } catch (RuntimeException exception) {
                LOGGER.info("Exception while fetching CRN for containerized datalake with Environment:{} {}",
                    environmentCrn, exception.getMessage());
                return Optional.empty();
            }
        }
        return Optional.empty();
    }

    @Override
    public Set<Pair<String, CdlCrudProto.StatusType.Value>> listSdxCrnStatusPair(String environmentCrn, String environmentName, Set<String> sdxCrns) {
        Set<Pair<String, CdlCrudProto.StatusType.Value>> result = new HashSet<>();
        if (isEnabled(environmentCrn)) {
            try {
                if (CollectionUtils.isNotEmpty(sdxCrns)) {
                    sdxCrns.forEach(crn -> {
                                CdlCrudProto.DatalakeResponse response = grpcClient.findDatalake(
                                        isBlank(environmentCrn) ? environmentName : environmentCrn, crn);
                                result.add(Pair.of(crn, CdlCrudProto.StatusType.Value.valueOf(response.getStatus())));
                            }
                    );
                } else {
                    CdlCrudProto.DatalakeResponse response = grpcClient.findDatalake(isBlank(environmentCrn) ? environmentName : environmentCrn, "");
                    result.add(Pair.of(response.getCrn(), CdlCrudProto.StatusType.Value.valueOf(response.getStatus())));
                }
            } catch (RuntimeException exception) {
                LOGGER.info("CDL not found for environment. CRN: {}. Name: {}", environmentCrn, environmentName);
                return Collections.emptySet();
            }
        }
        return result;
    }

    @Override
    public PollingResult getDeletePollingResultByStatus(CdlCrudProto.StatusType.Value status) {
        return switch (status) {
            case DELETED -> PollingResult.COMPLETED;
            case DELETE_FAILED -> PollingResult.FAILED;
            default -> PollingResult.IN_PROGRESS;
        };
    }

    @Override
    public StatusCheckResult getAvailabilityStatusCheckResult(CdlCrudProto.StatusType.Value status) {
        return switch (status) {
            case RUNNING, PROVISIONED -> StatusCheckResult.AVAILABLE;
            default -> StatusCheckResult.NOT_AVAILABLE;
        };
    }

    @Override
    public boolean isSdxClusterHA(String environmentCrn) {
        throw new UnsupportedOperationException("Currently we cannot decide if a SDX CDL cluster is HA or not.");
    }

    @Override
    public Optional<Entitlement> getEntitlement() {
        return Optional.of(ENABLE_CONTAINERIZED_DATALKE);
    }

    public Map<String, String> getServiceConfiguration(String sdxCrn, String name) {
        if (isEnabled(sdxCrn)) {
            LOGGER.info("Service configuration fetch for {}, in sdx {}", name, sdxCrn);
            return grpcServiceDiscoveryClient.getServiceConfiguration(sdxCrn, name);
        }
        return Collections.emptyMap();
    }

    public boolean isEnabled(String crn) {
        boolean enabled = cdlEnabled && isPlatformEntitled(Crn.safeFromString(crn).getAccountId());
        if (!cdlEnabled) {
            LOGGER.debug("CDL is not enabled. {} {}",
                    cdlEnabled, isPlatformEntitled(Crn.safeFromString(crn).getAccountId()));
        }
        return enabled;
    }

}