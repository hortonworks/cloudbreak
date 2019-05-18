package com.sequenceiq.datalake.service.sdx;


import java.util.List;
import java.util.Optional;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.altus.Crn;
import com.sequenceiq.cloudbreak.client.CloudbreakUserCrnClient;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.exception.NotFoundException;
import com.sequenceiq.datalake.api.endpoint.sdx.SdxClusterRequest;
import com.sequenceiq.datalake.controller.exception.BadRequestException;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.entity.SdxClusterStatus;
import com.sequenceiq.datalake.flow.SdxReactorFlowManager;
import com.sequenceiq.datalake.repository.SdxClusterRepository;

@Service
public class SdxService {

    public static final String SDX_CLUSTER_NAME = "sdx-cluster";

    private static final Logger LOGGER = LoggerFactory.getLogger(SdxService.class);

    @Inject
    private CloudbreakUserCrnClient cloudbreakClient;

    @Inject
    private SdxClusterRepository sdxClusterRepository;

    @Inject
    private SdxReactorFlowManager sdxReactorFlowManager;

    public SdxCluster getById(Long id) {
        Optional<SdxCluster> sdxClusters = sdxClusterRepository.findById(id);
        if (sdxClusters.isPresent()) {
            return sdxClusters.get();
        } else {
            throw new NotFoundException("Sdx cluster not found by ID");
        }
    }

    public SdxCluster getByAccountIdAndEnvName(String userCrn, String env) {
        String accountIdFromCrn = getAccountIdFromCrn(userCrn);
        List<SdxCluster> sdxClusters = sdxClusterRepository.findByAccountIdAndEnvName(accountIdFromCrn, env);
        Optional<SdxCluster> firstSdxCluster = sdxClusters.stream().findFirst();
        if (firstSdxCluster.isPresent()) {
            return firstSdxCluster.get();
        } else {
            throw new NotFoundException("Sdx cluster not found");
        }
    }

    public void updateSdxStatus(Long id, SdxClusterStatus sdxClusterStatus) {
        Optional<SdxCluster> sdxCluster = sdxClusterRepository.findById(id);
        sdxCluster.ifPresentOrElse(sdx -> {
            sdx.setStatus(sdxClusterStatus);
            sdxClusterRepository.save(sdx);
        }, () -> LOGGER.info("Can not update sdx {} to {} status", id, sdxClusterStatus));
    }

    public SdxCluster createSdx(String userCrn, String envName, SdxClusterRequest sdxClusterRequest) {
        SdxCluster sdxCluster = new SdxCluster();
        sdxCluster.setClusterName(getConvolutedClusterName(envName));
        sdxCluster.setAccountId(getAccountIdFromCrn(userCrn));
        sdxCluster.setClusterShape(sdxClusterRequest.getClusterShape());
        sdxCluster.setStatus(SdxClusterStatus.REQUESTED);
        sdxCluster.setAccessCidr(sdxClusterRequest.getAccessCidr());
        sdxCluster.setClusterShape(sdxClusterRequest.getClusterShape());
        try {
            sdxCluster.setTags(new Json(sdxClusterRequest.getTags()));
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Can not convert tags", e);
        }
        sdxCluster.setInitiatorUserCrn(userCrn);
        sdxCluster.setEnvName(envName);

        sdxClusterRepository.findByAccountIdAndEnvName(sdxCluster.getAccountId(), sdxCluster.getEnvName())
                .stream().findFirst().ifPresent(existedSdx -> {
            throw new BadRequestException("SDX cluster exists for environment name");
        });

        sdxCluster = sdxClusterRepository.save(sdxCluster);

        LOGGER.info("trigger SDX creation");
        sdxReactorFlowManager.triggerSdxCreation(sdxCluster.getId());

        return sdxCluster;
    }

    public List<SdxCluster> listSdx(String userCrn, String envName) {
        String accountIdFromCrn = getAccountIdFromCrn(userCrn);
        return sdxClusterRepository.findByAccountIdAndEnvName(accountIdFromCrn, envName);
    }

    public void deleteSdx(String userCrn, String envName) {
        LOGGER.info("Delete sdx");
        String accountIdFromCrn = getAccountIdFromCrn(userCrn);
        SdxCluster sdxCluster = sdxClusterRepository
                .findByAccountIdAndClusterNameAndEnvName(accountIdFromCrn, getConvolutedClusterName(envName), envName);
        if (sdxCluster == null) {
            LOGGER.info("Can not find sdx cluster");
            throw new BadRequestException("Can not find sdx cluster");
        }
        try {
            cloudbreakClient.withCrn(userCrn).stackV4Endpoint().delete(0L, sdxCluster.getClusterName(), false, false);
        } catch (Exception e) {
            LOGGER.info("sdx cannot be deleted form CB side, removing from SDX", e);
        }
        sdxClusterRepository.delete(sdxCluster);
        LOGGER.info("sdx deleted");
    }

    private String getAccountIdFromCrn(String userCrn) {
        Crn crn = Crn.fromString(userCrn);
        if (crn != null) {
            return crn.getAccountId();
        } else {
            throw new BadRequestException("Can not guess account ID from CRN");
        }
    }

    private String getConvolutedClusterName(String envName) {
        return envName + "-" + SDX_CLUSTER_NAME;
    }
}
