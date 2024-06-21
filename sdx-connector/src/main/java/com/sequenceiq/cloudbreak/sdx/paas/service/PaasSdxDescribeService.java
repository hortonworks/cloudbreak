package com.sequenceiq.cloudbreak.sdx.paas.service;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.sdx.common.model.SdxAccessView;
import com.sequenceiq.cloudbreak.sdx.common.model.SdxBasicView;
import com.sequenceiq.cloudbreak.sdx.common.service.PlatformAwareSdxDescribeService;
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

    @Override
    public Optional<String> getRemoteDataContext(String crn) {
        return localRdcSupplier.map(rdcSupplier -> rdcSupplier.getPaasSdxRemoteDataContext(crn))
                .orElseThrow(() -> new CloudbreakServiceException("Cannot provide remote data context!"));
    }

    @Override
    public Set<String> listSdxCrns(String environmentCrn) {
        return localPaasSdxService.map(localPaasSdxService -> localPaasSdxService.listSdxCrns(environmentCrn))
                .orElse(sdxEndpoint.getByEnvCrn(environmentCrn).stream().map(SdxClusterResponse::getCrn).collect(Collectors.toSet()));
    }

    @Override
    public Optional<SdxBasicView> getSdxByEnvironmentCrn(String environmentCrn) {
        return localPaasSdxService.flatMap(localPaasService -> localPaasService.getSdxBasicView(environmentCrn)).or(() ->
                sdxEndpoint.getByEnvCrn(environmentCrn)
                    .stream()
                    .map(sdx -> new SdxBasicView(
                            sdx.getName(),
                            sdx.getCrn(),
                            sdx.getRuntime(),
                            sdx.getRangerRazEnabled(),
                            sdx.getCreated(),
                            sdx.getDatabaseServerCrn(),
                            Optional.empty()))
                    .findFirst());
    }

    @Override
    public Optional<SdxAccessView> getSdxAccessViewByEnvironmentCrn(String environmentCrn) {
        return localPaasSdxService.flatMap(localPaasService -> localPaasService.getSdxAccessView(environmentCrn));
    }

    @Override
    public boolean isSdxClusterHA(String environmentCrn) {
        return sdxEndpoint.getByCrn(environmentCrn).getClusterShape().isHA();
    }

}
