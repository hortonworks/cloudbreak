package com.sequenceiq.cloudbreak.sdx.cdl.service;


import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto;
import com.sequenceiq.cloudbreak.sdx.cdl.grpc.GrpcSdxCdlClient;
import com.sequenceiq.cloudbreak.sdx.common.service.PlatformAwareSdxStatusService;
import com.sequenceiq.cloudbreak.sdx.common.status.StatusCheckResult;

@Service
public class CdlSdxStatusService extends AbstractCdlSdxService implements PlatformAwareSdxStatusService<CdlCrudProto.StatusType.Value> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CdlSdxStatusService.class);

    @Inject
    private GrpcSdxCdlClient grpcClient;

    @Override
    public Set<Pair<String, CdlCrudProto.StatusType.Value>> listSdxCrnStatusPair(String environmentCrn) {
        Set<Pair<String, CdlCrudProto.StatusType.Value>> result = new HashSet<>();
        if (isEnabled(environmentCrn)) {
            try {
                CdlCrudProto.DatalakeResponse response = grpcClient.findDatalake(environmentCrn, StringUtils.EMPTY);
                result.add(Pair.of(response.getCrn(), CdlCrudProto.StatusType.Value.valueOf(response.getStatus())));
            } catch (RuntimeException exception) {
                LOGGER.error(String.format("CDL not found for environment. CRN: %s.", environmentCrn), exception);
                return Collections.emptySet();
            }
        }
        return result
                .stream()
                .filter(entry -> !CdlCrudProto.StatusType.Value.DELETED.equals(entry.getValue()))
                .collect(Collectors.toSet());
    }

    @Override
    public StatusCheckResult getAvailabilityStatusCheckResult(CdlCrudProto.StatusType.Value status) {
        return switch (status) {
            case RUNNING, PROVISIONED -> StatusCheckResult.AVAILABLE;
            default -> StatusCheckResult.NOT_AVAILABLE;
        };
    }
}