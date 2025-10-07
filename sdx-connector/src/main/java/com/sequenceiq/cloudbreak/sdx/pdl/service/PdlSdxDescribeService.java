package com.sequenceiq.cloudbreak.sdx.pdl.service;

import java.util.Optional;
import java.util.Set;

import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.cloudera.api.swagger.model.ApiRemoteDataContext;
import com.cloudera.cdp.servicediscovery.model.DescribeDatalakeAsApiRemoteDataContextResponse;
import com.cloudera.cdp.servicediscovery.model.DescribeDatalakeServicesRequest;
import com.cloudera.cdp.servicediscovery.model.DescribeDatalakeServicesResponse;
import com.cloudera.thunderhead.service.environments2api.model.Environment;
import com.cloudera.thunderhead.service.environments2api.model.GetRootCertificateResponse;
import com.cloudera.thunderhead.service.environments2api.model.PrivateDatalakeDetails;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.sdx.RdcView;
import com.sequenceiq.cloudbreak.sdx.TargetPlatform;
import com.sequenceiq.cloudbreak.sdx.common.model.SdxAccessView;
import com.sequenceiq.cloudbreak.sdx.common.model.SdxBasicView;
import com.sequenceiq.cloudbreak.sdx.common.service.PlatformAwareSdxDescribeService;
import com.sequenceiq.cloudbreak.sdx.pdl.util.PdlRdcUtil;
import com.sequenceiq.remoteenvironment.api.v1.environment.model.DescribeRemoteEnvironment;

@Service
public class PdlSdxDescribeService extends AbstractPdlSdxService implements PlatformAwareSdxDescribeService {
    private static final Logger LOGGER = LoggerFactory.getLogger(PdlSdxDescribeService.class);

    @Inject
    private PdlRdcUtil pdlRdcUtil;

    @Override
    public Optional<String> getRemoteDataContext(String crn) {
        try {
            DescribeRemoteEnvironment describeRemoteEnvironment = getRemoteEnvironmentRequest(crn);
            DescribeDatalakeAsApiRemoteDataContextResponse remoteDataContext = getRemoteEnvironmentEndPoint().getRdcByCrn(describeRemoteEnvironment);
            String parsedJson = JsonUtil.writeValueAsString(remoteDataContext.getContext());
            ApiRemoteDataContext apiRemoteDataContext = pdlRdcUtil.parseRemoteDataContext(parsedJson);
            return Optional.of(pdlRdcUtil.remoteDataContextToJson(crn, apiRemoteDataContext));
        } catch (JsonProcessingException e) {
            LOGGER.error("Json processing failed, thus we cannot query remote data context. CRN: {}.", crn, e);
        } catch (RuntimeException exception) {
            LOGGER.error("Not able to fetch the RDC for PDL from Service Discovery. CRN: {}.", crn, exception);
        }
        throw new RuntimeException("Not able to fetch the RDC for PDL from Service Discovery");
    }

    @Override
    public RdcView extendRdcView(RdcView rdcView) {
        DescribeDatalakeServicesRequest request = new DescribeDatalakeServicesRequest().clusterid(rdcView.getStackCrn());
        DescribeDatalakeServicesResponse datalakeServices = getRemoteEnvironmentEndPoint().getDatalakeServicesByCrn(request);
        return pdlRdcUtil.extendRdcView(rdcView, datalakeServices);
    }

    @Override
    public Set<String> listSdxCrns(String environmentCrn) {
        try {
            String pvcCrn = getPrivateCloudEnvCrn(environmentCrn).orElse(null);
            LOGGER.info("Private Cloud CRN is {}", pvcCrn);
            if (StringUtils.isNotBlank(pvcCrn)) {
                return Set.of(pvcCrn);
            }
        } catch (Exception e) {
            LOGGER.info("No Private Cloud environment found with CRN: {}", environmentCrn, e);
        }
        return Set.of();
    }

    @Override
    public Optional<SdxBasicView> getSdxByEnvironmentCrn(String environmentCrn) {
        try {
            Environment environment = getPrivateEnvForPublicEnv(environmentCrn);
            if (environment != null && environment.getPvcEnvironmentDetails() != null
                    && environment.getPvcEnvironmentDetails().getPrivateDatalakeDetails() != null) {
                PrivateDatalakeDetails privateDatalakeDetails = environment.getPvcEnvironmentDetails().getPrivateDatalakeDetails();
                return Optional.of(SdxBasicView.builder()
                        .withName(privateDatalakeDetails.getDatalakeName())
                        .withCrn(environment.getCrn())
                        .withRuntime(StringUtils.substringBefore(environment.getCdpRuntimeVersion(), "-"))
                        .withRazEnabled(privateDatalakeDetails.getEnableRangerRaz())
                        .withCreated(privateDatalakeDetails.getCreationTimeEpochMillis())
                        .withPlatform(TargetPlatform.PDL)
                        .build());
            }
        } catch (RuntimeException exception) {
            LOGGER.error(String.format("Exception while fetching CRN for private datalake with Environment: %s.", environmentCrn), exception);
            if (exception instanceof BadRequestException) {
                throw exception;
            }
        }
        return Optional.empty();
    }

    @Override
    public Optional<SdxAccessView> getSdxAccessViewByEnvironmentCrn(String environmentCrn) {
        Environment environment = getPrivateEnvForPublicEnv(environmentCrn);
        if (environment != null && environment.getPvcEnvironmentDetails() != null
                && environment.getPvcEnvironmentDetails().getPrivateDatalakeDetails() != null) {
            PrivateDatalakeDetails privateDatalakeDetails = environment.getPvcEnvironmentDetails().getPrivateDatalakeDetails();
            return Optional.of(SdxAccessView.builder().withRangerFqdn(
                            StringUtils.isNotBlank(privateDatalakeDetails.getCmFQDN()) ? privateDatalakeDetails.getCmFQDN() : privateDatalakeDetails.getCmIP())
                    .build());
        }
        return Optional.empty();
    }

    @Override
    public Set<String> listSdxCrnsDetachedIncluded(String environmentCrn) {
        return listSdxCrns(environmentCrn);
    }

    @Override
    public Optional<String> getCACertsForEnvironment(String environmentCrn) {
        String pvcCrn = getPrivateCloudEnvCrn(environmentCrn).orElse(null);
        GetRootCertificateResponse response = getRemoteEnvironmentEndPoint().getRootCertificateByCrn(pvcCrn);
        return Optional.ofNullable(response).map(GetRootCertificateResponse::getContents);
    }
}