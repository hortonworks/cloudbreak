package com.sequenceiq.cloudbreak.sdx.cdl.service;


import java.net.URI;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.protobuf.InvalidProtocolBufferException;
import com.sequenceiq.cloudbreak.sdx.TargetPlatform;
import com.sequenceiq.cloudbreak.sdx.cdl.grpc.GrpcSdxCdlClient;
import com.sequenceiq.cloudbreak.sdx.cdl.grpc.GrpcServiceDiscoveryClient;
import com.sequenceiq.cloudbreak.sdx.common.model.SdxAccessView;
import com.sequenceiq.cloudbreak.sdx.common.model.SdxBasicView;
import com.sequenceiq.cloudbreak.sdx.common.service.PlatformAwareSdxDescribeService;

@Service
public class CdlSdxDescribeService extends AbstractCdlSdxService implements PlatformAwareSdxDescribeService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CdlSdxDescribeService.class);

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
            Set<String> sdxCrns = new HashSet<>();
            try {
                CdlCrudProto.ListDatalakesResponse datalakes = grpcClient.listDatalakes(environmentCrn, StringUtils.EMPTY);
                for (CdlCrudProto.DatalakeResponse datalakeResponse : datalakes.getDatalakeResponseList()) {
                    sdxCrns.add(datalakeResponse.getCrn());
                }
                return sdxCrns;
            } catch (RuntimeException exception) {
                LOGGER.error(String.format("Failed at listing CDL. CRN: %s.", environmentCrn), exception);
                return Collections.emptySet();
            }
        }
        return Set.of();
    }

    @Override
    public Optional<SdxBasicView> getSdxByEnvironmentCrn(String environmentCrn) {
        if  (isEnabled(environmentCrn)) {
            try {
                CdlCrudProto.DatalakeResponse datalake = grpcClient.findDatalake(environmentCrn, StringUtils.EMPTY);
                CdlCrudProto.DescribeDatalakeResponse detailedCdl = grpcClient.describeDatalake(datalake.getCrn());
                return Optional.of(SdxBasicView.builder()
                        .withName(detailedCdl.getName())
                        .withCrn(detailedCdl.getCrn())
                        .withRuntime(detailedCdl.getRuntimeVersion())
                        .withRazEnabled(detailedCdl.getRangerRazEnabled())
                        .withCreated(detailedCdl.getCreated())
                        .withDbServerCrn(detailedCdl.getDatabaseDetails().getCrn())
                        .withPlatform(TargetPlatform.CDL)
                        .build());
            } catch (RuntimeException exception) {
                LOGGER.error(String.format("Exception while fetching CRN for containerized datalake with Environment: %s.", environmentCrn), exception);
                return Optional.empty();
            }
        }
        return Optional.empty();
    }

    @Override
    public Optional<SdxAccessView> getSdxAccessViewByEnvironmentCrn(String environmentCrn) {
        if  (isEnabled(environmentCrn)) {
            CdlCrudProto.DatalakeResponse datalake = grpcClient.findDatalake(environmentCrn, StringUtils.EMPTY);
            String rangerFqdn = grpcClient.describeDatalakeServices(datalake.getCrn())
                    .getEndpointsList().stream()
                    .filter(endpointInfo -> StringUtils.containsIgnoreCase(endpointInfo.getName(), "ranger"))
                    .filter(endpointInfo -> !endpointInfo.getEndpointHostsList().isEmpty())
                    .map(endpointInfo -> {
                        try {
                            return new URI(endpointInfo.getEndpointHostsList().getFirst().getUri()).getHost();
                        } catch (Exception e) {
                            LOGGER.error("Couldn't parse URI for Ranger endpoint host.", e);
                            return StringUtils.EMPTY;
                        }
                    })
                    .findFirst()
                    .orElse(null);
            // here we are returning only with ranger FQDN since in case of CDL, term "cluster manager" is not applicable
            return Optional.of(SdxAccessView.builder().withRangerFqdn(rangerFqdn).build());
        }
        return Optional.empty();
    }

    @Override
    public boolean isSdxClusterHA(String environmentCrn) {
        throw new UnsupportedOperationException("Currently we cannot decide if CDL cluster is HA or not.");
    }
}