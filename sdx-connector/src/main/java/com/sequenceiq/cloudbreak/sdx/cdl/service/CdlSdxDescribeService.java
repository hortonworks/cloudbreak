package com.sequenceiq.cloudbreak.sdx.cdl.service;


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
import com.sequenceiq.cloudbreak.sdx.cdl.grpc.GrpcSdxCdlClient;
import com.sequenceiq.cloudbreak.sdx.cdl.grpc.GrpcServiceDiscoveryClient;
import com.sequenceiq.cloudbreak.sdx.common.model.SdxAccessView;
import com.sequenceiq.cloudbreak.sdx.common.model.SdxBasicView;
import com.sequenceiq.cloudbreak.sdx.common.service.PlatformAwareSdxDescribeService;

@Service
public class CdlSdxDescribeService extends AbstractCdlSdxService implements PlatformAwareSdxDescribeService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CdlSdxDescribeService.class);

    private static final String ACCESS_VIEW_IG_NAME = "sdx";

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
                LOGGER.info("Json processing failed, thus we cannot query remote data context. Crn: {}, Exception message: {}", crn, e.getMessage());
            } catch (RuntimeException exception) {
                LOGGER.info("Not able to fetch the RDC for CDL from Service Discovery. CRN: {}, Exception message: {}", crn, exception.getMessage());
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
                LOGGER.info("Failed at listing CDL. CRN: {}. Exception: {}",
                        environmentCrn, exception.getMessage());
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
                SdxBasicView sdxBasicView = new SdxBasicView(
                        detailedCdl.getName(),
                        detailedCdl.getCrn(),
                        detailedCdl.getShape(),
                        detailedCdl.getRangerRazEnabled(),
                        detailedCdl.getCreated(),
                        detailedCdl.getDatabaseDetails().getCrn(),
                        Optional.empty());
                return Optional.of(sdxBasicView);
            } catch (RuntimeException exception) {
                LOGGER.info("Exception while fetching CRN for containerized datalake with Environment:{} {}",
                    environmentCrn, exception.getMessage());
                return Optional.empty();
            }
        }
        return Optional.empty();
    }

    @Override
    public Optional<SdxAccessView> getSdxAccessViewByEnvironmentCrn(String environmentCrn) {
        CdlCrudProto.DatalakeResponse datalake = grpcClient.findDatalake(environmentCrn, StringUtils.EMPTY);
        CdlCrudProto.DescribeDatalakeResponse detailedCdl = grpcClient.describeDatalake(datalake.getCrn());
        return detailedCdl.getInstanceGroupsList().stream()
                .filter(ig -> StringUtils.equals(ig.getName(), ACCESS_VIEW_IG_NAME))
                .flatMap(ig -> ig.getInstancesList().stream())
                .map(instance -> new SdxAccessView(instance.getDiscoveryFQDN(), instance.getPrivateIP()))
                .findFirst();
    }

    @Override
    public boolean isSdxClusterHA(String environmentCrn) {
        throw new UnsupportedOperationException("Currently we cannot decide if a SDX CDL cluster is HA or not.");
    }
}