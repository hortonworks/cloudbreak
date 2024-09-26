package com.sequenceiq.cloudbreak.sdx.paas.service;

import static com.sequenceiq.sdx.api.model.SdxClusterStatusResponse.DELETE_FAILED;

import java.util.Optional;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.cloudbreak.sdx.common.polling.PollingResult;
import com.sequenceiq.cloudbreak.sdx.common.service.PlatformAwareSdxDeleteService;
import com.sequenceiq.cloudbreak.sdx.paas.LocalPaasRemoteDataContextSupplier;
import com.sequenceiq.cloudbreak.sdx.paas.LocalPaasSdxService;
import com.sequenceiq.sdx.api.endpoint.SdxEndpoint;
import com.sequenceiq.sdx.api.model.SdxClusterStatusResponse;

@Service
public class PaasSdxDeleteService extends PaasSdxStatusService implements PlatformAwareSdxDeleteService<SdxClusterStatusResponse> {

    private static final Logger LOGGER = LoggerFactory.getLogger(PaasSdxDeleteService.class);

    @Inject
    private SdxEndpoint sdxEndpoint;

    @Inject
    private Optional<LocalPaasRemoteDataContextSupplier> localRdcSupplier;

    @Inject
    private Optional<LocalPaasSdxService> localPaasSdxService;

    @Inject
    private RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    @Override
    public void deleteSdx(String sdxCrn, Boolean force) {
        LOGGER.info("Calling delete for SDX PaaS cluster {}", sdxCrn);
        ThreadBasedUserCrnProvider.doAsInternalActor(regionAwareInternalCrnGeneratorFactory.iam().getInternalCrnForServiceAsString(),
                () -> sdxEndpoint.deleteByCrn(sdxCrn, force));
    }

    @Override
    public PollingResult getDeletePollingResultByStatus(SdxClusterStatusResponse status) {
        return DELETE_FAILED.equals(status) ? PollingResult.FAILED : PollingResult.IN_PROGRESS;
    }
}
