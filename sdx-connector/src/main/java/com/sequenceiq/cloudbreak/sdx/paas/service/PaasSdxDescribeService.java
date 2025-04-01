package com.sequenceiq.cloudbreak.sdx.paas.service;

import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.sdx.RdcView;
import com.sequenceiq.cloudbreak.sdx.TargetPlatform;
import com.sequenceiq.cloudbreak.sdx.common.model.SdxAccessView;
import com.sequenceiq.cloudbreak.sdx.common.model.SdxBasicView;
import com.sequenceiq.cloudbreak.sdx.common.service.PlatformAwareSdxDescribeService;
import com.sequenceiq.cloudbreak.sdx.paas.LocalPaasRdcViewExtender;
import com.sequenceiq.cloudbreak.sdx.paas.LocalPaasRemoteDataContextSupplier;
import com.sequenceiq.cloudbreak.sdx.paas.LocalPaasSdxService;
import com.sequenceiq.sdx.api.endpoint.SdxEndpoint;
import com.sequenceiq.sdx.api.model.SdxClusterResponse;

@Service
public class PaasSdxDescribeService extends AbstractPaasSdxService implements PlatformAwareSdxDescribeService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PaasSdxDescribeService.class);

    @Inject
    private SdxEndpoint sdxEndpoint;

    @Inject
    private Optional<LocalPaasRemoteDataContextSupplier> localRdcSupplier;

    @Inject
    private Optional<LocalPaasSdxService> localPaasSdxService;

    @Inject
    private Optional<LocalPaasRdcViewExtender> localPaasSdxContextExtender;

    @Override
    public Optional<String> getRemoteDataContext(String crn) {
        return localRdcSupplier.map(rdcSupplier -> rdcSupplier.getPaasSdxRemoteDataContext(crn))
                .orElseThrow(() -> new CloudbreakServiceException(String.format("Cannot provide remote data context for CRN [%s]!", crn)));
    }

    @Override
    public RdcView extendRdcView(RdcView rdcView) {
        if (localPaasSdxContextExtender.isEmpty()) {
            throw new CloudbreakServiceException(String.format("Cannot extend context for CRN [%s]!", rdcView.getStackCrn()));
        }
        return localPaasSdxContextExtender.get().extendRdcView(rdcView);
    }

    @Override
    public Set<String> listSdxCrns(String environmentCrn) {
        if (localPaasSdxService.isPresent()) {
            return localPaasSdxService.get().listSdxCrns(environmentCrn);
        }
        return ThreadBasedUserCrnProvider.doAsInternalActor(
                () -> sdxEndpoint.getByEnvCrn(environmentCrn, false)).stream().map(SdxClusterResponse::getCrn).collect(Collectors.toSet());
    }

    @Override
    public Optional<SdxBasicView> getSdxByEnvironmentCrn(String environmentCrn) {
        return localPaasSdxService.flatMap(localPaasService -> localPaasService.getSdxBasicView(environmentCrn)).or(() ->
                ThreadBasedUserCrnProvider.doAsInternalActor(
                        () -> sdxEndpoint.getByEnvCrn(environmentCrn, false))
                    .stream()
                    .filter(Predicate.not(SdxClusterResponse::isDetached))
                    .map(sdx -> SdxBasicView.builder()
                            .withName(sdx.getName())
                            .withCrn(sdx.getCrn())
                            .withRuntime(sdx.getRuntime())
                            .withRazEnabled(sdx.getRangerRazEnabled())
                            .withCreated(sdx.getCreated())
                            .withDbServerCrn(sdx.getDatabaseServerCrn())
                            .withPlatform(TargetPlatform.PAAS)
                            .build())
                    .findFirst());
    }

    @Override
    public Optional<SdxAccessView> getSdxAccessViewByEnvironmentCrn(String environmentCrn) {
        return localPaasSdxService.flatMap(localPaasService -> localPaasService.getSdxAccessView(environmentCrn));
    }

    @Override
    public Set<String> listSdxCrnsDetachedIncluded(String environmentCrn) {
        if (localPaasSdxService.isPresent()) {
            return localPaasSdxService.get().listSdxCrns(environmentCrn);
        }
        return ThreadBasedUserCrnProvider.doAsInternalActor(
                () -> sdxEndpoint.getByEnvCrn(environmentCrn, true)).stream().map(SdxClusterResponse::getCrn).collect(Collectors.toSet());
    }
}
