package com.sequenceiq.cloudbreak.sdx.pdl.service;

import java.util.Optional;
import java.util.Set;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.cloudera.thunderhead.service.environments2api.model.Environment;
import com.cloudera.thunderhead.service.environments2api.model.PrivateDatalakeDetails;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.protobuf.InvalidProtocolBufferException;
import com.sequenceiq.cloudbreak.sdx.TargetPlatform;
import com.sequenceiq.cloudbreak.sdx.cdl.grpc.GrpcSdxCdlClient;
import com.sequenceiq.cloudbreak.sdx.common.grpc.GrpcServiceDiscoveryClient;
import com.sequenceiq.cloudbreak.sdx.common.model.SdxAccessView;
import com.sequenceiq.cloudbreak.sdx.common.model.SdxBasicView;
import com.sequenceiq.cloudbreak.sdx.common.service.PlatformAwareSdxDescribeService;

@Service
public class PdlSdxDescribeService extends AbstractPdlSdxService implements PlatformAwareSdxDescribeService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PdlSdxDescribeService.class);

    @Inject
    private GrpcSdxCdlClient grpcClient;

    @Inject
    private GrpcServiceDiscoveryClient grpcServiceDiscoveryClient;

    @Override
    public Optional<String> getRemoteDataContext(String crn) {
        if (isEnabled(crn)) {
            try {
                return Optional.of(grpcServiceDiscoveryClient.getRemoteDataContext(crn));
            } catch (JsonProcessingException | InvalidProtocolBufferException e) {
                LOGGER.error(String.format("Json processing failed, thus we cannot query remote data context. CRN: %s.", crn), e);
            } catch (RuntimeException exception) {
                LOGGER.error(String.format("Not able to fetch the RDC for CDL from Service Discovery. CRN: %s.", crn), exception);
            }
        }
        return Optional.empty();
    }

    @Override
    public Set<String> listSdxCrns(String environmentCrn) {
        if (isEnabled(environmentCrn)) {
            String pvcCrn = getPrivateCloudEnvCrn(environmentCrn).orElse(null);
            LOGGER.info("pvcCrn is {}", pvcCrn);
            if (!StringUtils.isEmpty(pvcCrn)) {
                return Set.of(pvcCrn);
            }
        }
        return Set.of();
    }

    @Override
    public Optional<SdxBasicView> getSdxByEnvironmentCrn(String environmentCrn) {
        if (isEnabled(environmentCrn)) {
            try {
                Environment environment = getPrivateEnvForPublicEnv(environmentCrn);
                if (environment != null && environment.getPvcEnvironmentDetails() != null
                        && environment.getPvcEnvironmentDetails().getPrivateDatalakeDetails() != null) {
                    PrivateDatalakeDetails privateDatalakeDetails = environment.getPvcEnvironmentDetails().getPrivateDatalakeDetails();
                    return Optional.of(SdxBasicView.builder()
                            .withName(privateDatalakeDetails.getDatalakeName())
                            .withCrn(environment.getCrn())
                            .withRuntime(environment.getCdpRuntimeVersion())
                            .withRazEnabled(privateDatalakeDetails.getEnableRangerRaz())
                            .withCreated(privateDatalakeDetails.getCreationTimeEpochMillis())
                            .withPlatform(TargetPlatform.PDL)
                            .build());
                }
            } catch (RuntimeException exception) {
                LOGGER.error(String.format("Exception while fetching CRN for private datalake with Environment: %s.", environmentCrn), exception);
            }
        }
        return Optional.empty();
    }

    @Override
    public Optional<SdxAccessView> getSdxAccessViewByEnvironmentCrn(String environmentCrn) {
        if  (isEnabled(environmentCrn)) {
            Environment environment = getPrivateEnvForPublicEnv(environmentCrn);
            if (environment != null && environment.getPvcEnvironmentDetails() != null
                    && environment.getPvcEnvironmentDetails().getPrivateDatalakeDetails() != null) {
                PrivateDatalakeDetails privateDatalakeDetails = environment.getPvcEnvironmentDetails().getPrivateDatalakeDetails();
                return Optional.of(SdxAccessView.builder().withRangerFqdn(
                        !StringUtils.isEmpty(privateDatalakeDetails.getCmFQDN()) ? privateDatalakeDetails.getCmFQDN() : privateDatalakeDetails.getCmIP())
                        .build());
            }
        }
        return Optional.empty();
    }

    @Override
    public Set<String> listSdxCrnsDetachedIncluded(String environmentCrn) {
        return listSdxCrns(environmentCrn);
    }

}