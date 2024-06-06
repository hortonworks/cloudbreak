package com.sequenceiq.cloudbreak.sdx.cdl.service;


import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto;
import com.sequenceiq.cloudbreak.sdx.cdl.grpc.GrpcSdxCdlClient;
import com.sequenceiq.cloudbreak.sdx.common.polling.PollingResult;
import com.sequenceiq.cloudbreak.sdx.common.service.PlatformAwareSdxDeleteService;

@Service
public class CdlSdxDeleteService extends CdlSdxStatusService implements PlatformAwareSdxDeleteService<CdlCrudProto.StatusType.Value> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CdlSdxDeleteService.class);

    @Inject
    private GrpcSdxCdlClient grpcClient;

    @Override
    public void deleteSdx(String sdxCrn, Boolean force) {
        if (isEnabled(sdxCrn)) {
            try {
                grpcClient.deleteDatalake(sdxCrn, force);
            } catch (RuntimeException exception) {
                LOGGER.info("We are not able to delete CDL with CRN: {}, Exception: {}", sdxCrn, exception.getMessage());
            }
        }
    }

    @Override
    public PollingResult getDeletePollingResultByStatus(CdlCrudProto.StatusType.Value status) {
        return switch (status) {
            case DELETED -> PollingResult.COMPLETED;
            case DELETE_FAILED -> PollingResult.FAILED;
            default -> PollingResult.IN_PROGRESS;
        };
    }
}